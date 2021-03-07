/*-
 *
 *  This file is part of Oracle Berkeley DB Java Edition
 *  Copyright (C) 2002, 2015 Oracle and/or its affiliates.  All rights reserved.
 *
 *  Oracle Berkeley DB Java Edition is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation, version 3.
 *
 *  Oracle Berkeley DB Java Edition is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License in
 *  the LICENSE file along with Oracle Berkeley DB Java Edition.  If not, see
 *  <http://www.gnu.org/licenses/>.
 *
 *  An active Oracle commercial licensing agreement for this product
 *  supercedes this license.
 *
 *  For more information please contact:
 *
 *  Vice President Legal, Development
 *  Oracle America, Inc.
 *  5OP-10
 *  500 Oracle Parkway
 *  Redwood Shores, CA 94065
 *
 *  or
 *
 *  berkeleydb-info_us@oracle.com
 *
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  EOF
 *
 */
package com.sleepycat.je.rep.subscription;

import com.sleepycat.je.log.entry.LNLogEntry;
import com.sleepycat.je.log.entry.LogEntry;
import com.sleepycat.je.rep.GroupShutdownException;
import com.sleepycat.je.rep.impl.RepImpl;
import com.sleepycat.je.rep.stream.InputWireRecord;
import com.sleepycat.je.rep.stream.Protocol;
import com.sleepycat.je.utilint.StoppableThread;
import com.sleepycat.je.utilint.VLSN;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.sleepycat.je.log.LogEntryType.LOG_TXN_ABORT;
import static com.sleepycat.je.log.LogEntryType.LOG_TXN_COMMIT;

/**
 * Object to represent the thread created by Subscription to process messages
 * received from feeder.
 */
class SubscriptionProcessMessageThread extends StoppableThread {

    /* handle to stats */
    private final SubscriptionStat stats;
    /* configuration */
    private final SubscriptionConfig config;
    /* input queue from which to consume messages */
    private final BlockingQueue<Object> queue;
    /* logger */
    private final Logger logger;

    /* exit flag to specify exit type */
    private volatile ExitType exitRequest;

    /**
     * Construct a subscription thread to process messages
     *
     * @param impl   RepImpl of the RN where thread is running
     * @param queue  Input queue from which to consume messages
     * @param config Subscription configuration
     * @param logger Logger
     */
    SubscriptionProcessMessageThread(RepImpl impl,
                                     BlockingQueue<Object> queue,
                                     SubscriptionConfig config,
                                     SubscriptionStat stats,
                                     Logger logger) {
        super(impl, "SubscriptionProcessMessageThread");
        this.logger = logger;
        this.config = config;
        this.queue = queue;
        this.stats = stats;

        exitRequest = ExitType.NONE;
        stats.setHighVLSN(VLSN.NULL_VLSN);
    }

    /**
     * Shut down input thread immediately, regardless of the state of queue
     */
    public void shutdown() {
        exitRequest = ExitType.IMMEDIATE;
    }

    /**
     * Implement a soft shutdown. The thread will exist after all messages in
     * the queue are consumed and processed.
     *
     * @return the amount of time in ms that the shutdownThread method will
     * wait for the thread to exit. A -ve value means that the method will not
     * wait. A zero value means it will wait indefinitely.
     */
    @Override
    public int initiateSoftShutdown() {
        exitRequest = ExitType.IMMEDIATE;

        return 0;
    }

    /**
     * Implement thread run() method. Dequeue message from the queue and
     * process it via the callback.
     *
     */
    @Override
    public void run() {

        /* callback provided by client to process each message in input queue */
        final SubscriptionCallback callBack = config.getCallBack();

        logger.info("Input thread started. Message queue size:" +
                    queue.remainingCapacity());

        /* loop to process each message in the queue */
        try {
            while (true) {
                if (exitRequest == ExitType.IMMEDIATE) {
                    /*
                     * if immediate exit is requested,  exit without
                     * consuming any message in the queue
                     */
                    break;
                } else {

                    /* fetch next message from queue */
                    final Object message =
                        queue.poll(SubscriptionConfig.QUEUE_POLL_INTERVAL_MS,
                                   TimeUnit.MILLISECONDS);

                    if (message == null) {
                        /*
                         * No message to consume, continue and wait for the
                         * next message.
                         */
                        continue;

                    }  else if (message instanceof Exception) {
                        
                        callBack.processException((Exception) message);
                        
                        /* exits if shutdown message from feeder */
                        if (message instanceof GroupShutdownException) {
                            exitRequest = ExitType.IMMEDIATE;
                            GroupShutdownException gse = 
                                (GroupShutdownException) message;
                            logger.info("Received shutdown message from " +
                                        config.getFeederHost() +
                                        " at VLSN " + gse.getShutdownVLSN());
                            break;
                        }
                    } else {

                        /* use different callbacks depending on entry type */
                        final InputWireRecord wireRecord =
                            ((Protocol.Entry) message).getWireRecord();
                        final VLSN vlsn = wireRecord.getVLSN();
                        final byte type = wireRecord.getEntryType();
                        final LogEntry entry = wireRecord.getLogEntry();
                        final long txnId = entry.getTransactionId();

                        stats.setHighVLSN(vlsn);
                        stats.getNumOpsProcessed().increment();

                        /* call different proc depending on entry type */
                        if (LOG_TXN_COMMIT.equalsType(type)) {
                            stats.getNumTxnCommitted().increment();
                            callBack.processCommit(vlsn, txnId);
                            continue;
                        }

                        if (LOG_TXN_ABORT.equalsType(type)) {
                            stats.getNumTxnAborted().increment();
                            callBack.processAbort(vlsn, txnId);
                            continue;
                        }

                        if (entry instanceof LNLogEntry) {

                            /* receive a LNLogEntry from Feeder */
                            final LNLogEntry<?> lnEntry = (LNLogEntry<?>)entry;

                            /*
                             * We have to call postFetchInit to avoid EFE. The
                             * function will reformat the key/data if entry is
                             * from a dup DB. The default feeder filter would
                             * filter out all dup db entries for us.
                             *
                             * TODO:
                             * Note today we temporarily disabled user-defined
                             * feeder filter and thus users are unable to
                             * replace the default feeder filter with their own.
                             * So here it is safe to assume no dup db entry.
                             *
                             * We will have to address the dup db entry issue
                             * in future to make the Subscription API public,
                             * in which users will be allowed to use their own
                             * feeder filter.
                             */
                            lnEntry.postFetchInit(false);

                            if (lnEntry.getLN().isDeleted()) {
                                callBack.processDel(vlsn, lnEntry.getKey(),
                                                    txnId);
                            } else {
                                callBack.processPut(vlsn, lnEntry.getKey(),
                                                    lnEntry.getData(), txnId);
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            logger.info("input thread receives exception " + e.getMessage() +
                        ", process the exception in callback, clear queue " +
                        "and exit.");

            exitRequest = ExitType.IMMEDIATE;
        } finally {
            queue.clear();
            logger.info("message queue cleared, thread exits with type: " +
                        exitRequest);
        }
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    /* types of exits  */
    private enum ExitType {
        NONE,      /* No exit requested */
        IMMEDIATE, /* An immediate exit; ignore queued requests. */
        SOFT       /* Process pending requests in queue, then exit */
    }
}
