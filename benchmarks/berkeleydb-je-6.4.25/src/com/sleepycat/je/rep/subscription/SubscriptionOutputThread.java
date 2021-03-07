/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2002, 2014 Oracle and/or its affiliates.  All rights reserved.
 *
 */
package com.sleepycat.je.rep.subscription;

import com.sleepycat.je.rep.impl.RepImpl;
import com.sleepycat.je.rep.impl.node.ReplicaOutputThreadBase;
import com.sleepycat.je.rep.net.DataChannel;
import com.sleepycat.je.rep.stream.Protocol;
import com.sleepycat.je.rep.stream.Protocol.HeartbeatResponse;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * Object of the output thread created by subscription to respond the
 * heartbeat ping from feeder
 */
class SubscriptionOutputThread extends ReplicaOutputThreadBase {

    /* handle to statistics */
    private final SubscriptionStat stats;

    public SubscriptionOutputThread(RepImpl repImpl,
                                    BlockingQueue<Long> outputQueue,
                                    Protocol protocol,
                                    DataChannel replicaFeederChannel,
                                    SubscriptionStat stats) {
        super(repImpl, repImpl.getRepNode(), outputQueue, protocol,
              replicaFeederChannel);
        this.stats = stats;
    }

    /**
     * Implement the heartbeat response for output thread
     *
     * @param txnId  txn id
     * @throws IOException
     */
    @Override
    public void writeHeartbeat(Long txnId) throws IOException {

        /* report the most recently received VLSN to feeder */
        HeartbeatResponse response =
            protocol.new HeartbeatResponse(stats.getHighVLSN(),
                                           stats.getHighVLSN());

        protocol.write(response, replicaFeederChannel);
        stats.getNumMsgResponded().increment();
    }
}
