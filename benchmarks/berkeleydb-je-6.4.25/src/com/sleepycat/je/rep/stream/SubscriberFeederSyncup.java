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
package com.sleepycat.je.rep.stream;


import java.io.IOException;
import java.util.logging.Logger;

import com.sleepycat.je.rep.impl.RepImpl;
import com.sleepycat.je.rep.stream.Protocol.AlternateMatchpoint;
import com.sleepycat.je.rep.stream.Protocol.Entry;
import com.sleepycat.je.rep.stream.Protocol.EntryNotFound;
import com.sleepycat.je.rep.utilint.BinaryProtocol.Message;
import com.sleepycat.je.rep.utilint.NamedChannel;
import com.sleepycat.je.utilint.InternalException;
import com.sleepycat.je.utilint.LoggerUtils;
import com.sleepycat.je.utilint.VLSN;

/**
 * Object to sync-up the Feeder and Subscriber to establish the VLSN from
 * which subscriber should should start stream log entries from feeder.
 */
public class SubscriberFeederSyncup {

    private final Logger logger;
    private final RepImpl repImpl;
    private final NamedChannel namedChannel;
    private final Protocol protocol;
    private final FeederFilter filter;

    public SubscriberFeederSyncup(NamedChannel namedChannel,
                                  Protocol protocol,
                                  FeederFilter filter,
                                  RepImpl repImpl,
                                  Logger logger) {
        this.namedChannel = namedChannel;
        this.protocol = protocol;
        this.filter = filter;
        this.repImpl = repImpl;
        this.logger = logger;
    }

    /**
     * Execute sync-up to the Feeder.  Request Feeder to start a replication
     * stream from a start VLSN, if it is available. Otherwise return NULL
     * VLSN to subscriber.
     *
     * @param reqVLSN  VLSN requested by subscriber to stream log entries
     *
     * @return start VLSN from subscribe can stream log entries
     * @throws InternalException
     */
    public VLSN execute(VLSN reqVLSN) throws InternalException {

        final long startTime = System.currentTimeMillis();

        LoggerUtils.info(logger, repImpl,
                         "Subscriber-Feeder " + namedChannel.getNameIdPair() +
                         " syncup started.");

        try {
            /* first query the start VLSN from feeder */
            final VLSN startVLSN = getStartVLSNFromFeeder(reqVLSN);
            if (!startVLSN.equals(VLSN.NULL_VLSN)) {
                LoggerUtils.info(logger, repImpl,
                                 "Response from feeder  " +
                                 namedChannel.getNameIdPair() +
                                 ": the start VLSN " + startVLSN +
                                 ", the requested VLSN " + reqVLSN +
                                 ", send startStream request with filter.");

                /* start streaming from feeder if valid start VLSN */
                protocol.write(protocol.new StartStream(startVLSN, filter),
                               namedChannel);
            } else {
                LoggerUtils.info(logger, repImpl,
                                 "Unable to stream from Feeder " +
                                 namedChannel.getNameIdPair() +
                                 " from requested VLSN " + reqVLSN);
            }
            return startVLSN;
        } catch (IllegalStateException e) {
            throw new InternalException(e.getMessage());
        } catch (IOException e) {
            throw new InternalException(e.getMessage());
        } finally {
            LoggerUtils.info(logger, repImpl,
                             String.format("Subscriber to feeder " +
                                           namedChannel.getNameIdPair() +
                                           " sync-up done, elapsed time: %,dms",
                                           System.currentTimeMillis() -
                                           startTime));
        }
    }

    /**
     * Request a start VLSN from feeder. The feeder will return a valid
     * start VLSN, which can be equal to or earlier than the request VLSN,
     * or null if feeder is unable to service the requested VLSN.
     *
     * @param requestVLSN start VLSN requested by subscriber
     *
     * @return VLSN a valid start VLSN from feeder, or null if it unavailable
     * at the feeder
     * @throws IOException if unable to read message from channel
     * @throws IllegalStateException if the feeder sends an unexpected message
     */
    private VLSN getStartVLSNFromFeeder(VLSN requestVLSN)
            throws IOException, IllegalStateException {

        LoggerUtils.fine(logger, repImpl,
                         "Subscriber send requested VLSN " + requestVLSN + 
                         " to feeder " + namedChannel.getNameIdPair());

        /* ask the feeder for the requested VLSN. */
        protocol.write(protocol.new EntryRequest(requestVLSN), namedChannel);

        /*
         * expect one of following:
         *  a) the feeder returns the requested VLSN
         *  b) the feeder returns an non-null VLSN earlier than requested VLSN
         *  c) the feeder returns not_found
         */
        final Message message = protocol.read(namedChannel);
        final VLSN vlsn;
        if (message instanceof Entry) {
            vlsn = ((Entry) message).getWireRecord().getVLSN();

            assert(vlsn.equals(requestVLSN));
            LoggerUtils.finest(logger, repImpl,
                               "Subscriber successfully requested VLSN " + 
                               requestVLSN + " from feeder " + 
                               namedChannel.getNameIdPair());
        } else if (message instanceof AlternateMatchpoint) {
            vlsn = ((AlternateMatchpoint) message).getAlternateWireRecord()
                                                  .getVLSN();
            /* must be an earlier VLSN */
            assert(vlsn.compareTo(requestVLSN) < 0);
            LoggerUtils.finest(logger, repImpl, 
                               "Feeder " + namedChannel.getNameIdPair() + 
                               " returns a valid start VLSN" + vlsn + 
                               " but earlier than requested one " +
                               requestVLSN);
        } else  if (message instanceof EntryNotFound) {
            vlsn = VLSN.NULL_VLSN;
            LoggerUtils.finest(logger, repImpl,
                               "Feeder " + namedChannel.getNameIdPair() + 
                               " is unable to service the request vlsn " +
                               requestVLSN );
        } else {
            /* unexpected response from feeder */
            String msg = "Receive unexpected response " + message.toString() +
                         "from feeder " + namedChannel.getNameIdPair();
            LoggerUtils.warning(logger, repImpl, msg);
            throw new IllegalStateException(msg);
        }

        return vlsn;
    }
}
