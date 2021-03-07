/*-
 *
 *  This file is part of Oracle NoSQL Database
 *  Copyright (C) 2011, 2015 Oracle and/or its affiliates.  All rights reserved.
 *
 *  Oracle NoSQL Database is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation, version 3.
 *
 *  Oracle NoSQL Database is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public
 *  License in the LICENSE file along with Oracle NoSQL Database.  If not,
 *  see <http://www.gnu.org/licenses/>.
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

import com.sleepycat.je.utilint.LongStat;
import com.sleepycat.je.utilint.StatGroup;
import com.sleepycat.je.utilint.VLSN;

/**
 * Object to represent subscription statistics
 */
public class SubscriptionStat {

    /*
     * VLSN from which feeder agrees to stream log entries, it is returned from
     * the feeder and can be equal to or earlier than the VLSN requested by the
     * client, which is specified in subscription configuration.
     */
    private VLSN startVLSN;

    /* the last VLSN that has been processed */
    private VLSN highVLSN;

    /* used by main thread: # of retries to insert msgs into input queue */
    private final LongStat nReplayQueueOverflow;
    /* used by main thread: # of msgs received from feeder */
    private final LongStat nMsgReceived;
    /* used by main thread: max # of items pending in input queue */
    private final LongStat maxPendingInput;
    /* used by output thread: # of acks sent to feeder */
    private final LongStat nMsgResponded;
    /* used by input thread: # of data ops processed */
    private final LongStat nOpsProcessed;
    /* used by input thread: # of txn aborted and committed */
    private final LongStat nTxnAborted;
    private final LongStat nTxnCommitted;

    SubscriptionStat() {

        startVLSN = VLSN.NULL_VLSN;

        /* initialize statistics */
        StatGroup stats = new StatGroup("subscription",
                                        "subscription " + "statistics");
        nReplayQueueOverflow = new LongStat(stats,
                SubscriptionStatDefinition.SUB_N_REPLAY_QUEUE_OVERFLOW, 0L);
        nMsgReceived = new LongStat(stats,
                SubscriptionStatDefinition.SUB_MSG_RECEIVED, 0L);
        nMsgResponded = new LongStat(stats,
                SubscriptionStatDefinition.SUB_MSG_RESPONDED, 0L);
        maxPendingInput = new LongStat(stats,
                SubscriptionStatDefinition.SUB_MAX_PENDING_INPUT, 0L);

        nOpsProcessed = new LongStat(stats,
                SubscriptionStatDefinition.SUB_OPS_PROCESSED, 0L);
        nTxnAborted = new LongStat(stats,
                SubscriptionStatDefinition.SUB_TXN_ABORTED, 0L);
        nTxnCommitted = new LongStat(stats,
                SubscriptionStatDefinition.SUB_TXN_COMMITTED, 0L);
        
    }

    /*--------------*/
    /*-  Getters   -*/
    /*--------------*/
    public synchronized LongStat getNumReplayQueueOverflow() {
        return nReplayQueueOverflow;
    }
    
    public synchronized LongStat getMaxPendingInput() {
        return maxPendingInput;
    }
    
    public synchronized LongStat getNumMsgResponded() {
        return nMsgResponded;
    }

    public synchronized LongStat getNumMsgReceived() {
        return nMsgReceived;
    }

    public synchronized LongStat getNumOpsProcessed() {
        return nOpsProcessed;
    }

    public synchronized LongStat getNumTxnAborted() {
        return nTxnAborted;
    }

    public synchronized LongStat getNumTxnCommitted() {
        return nTxnCommitted;
    }

    public synchronized VLSN getStartVLSN() {
        return startVLSN;
    }

    public synchronized VLSN getHighVLSN() {
        return highVLSN;
    }

    /*--------------*/
    /*-  Setters   -*/
    /*--------------*/
    public synchronized void setStartVLSN(VLSN vlsn) {
        startVLSN = vlsn;
    }

    public synchronized void setHighVLSN(VLSN vlsn) {
        highVLSN = vlsn;
    }
}
