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

package com.sleepycat.je.rep.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.CommitToken;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.ReplicationConfig;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;
import com.sleepycat.je.rep.utilint.WaitForMasterListener;
import com.sleepycat.je.rep.utilint.WaitForReplicaListener;

public class NetworkPartitionHealingTest extends RepTestBase {

    /* (non-Javadoc)
     * @see com.sleepycat.je.rep.impl.RepTestBase#setUp()
     */
    @Override
    @Before
    public void setUp()
        throws Exception {

        groupSize = 3;
        super.setUp();
    }

    /**
     * This test captures the problem described in SR 20572 and related
     * SR 20258.
     *
     * Simulates a network partition test where a 3 node group (A, B, C) is
     * split into two: (A) and (B,C), resulting in two masters: an A and and a
     * newly elected B.
     *
     * The majority side (B,C) continues to make progress and performs durable
     * writes.
     *
     * The master on the majority side B goes down. There is now no master on
     * the (B,C) side since there is no quorum.
     *
     * The partition is healed. This should result in a master being elected on
     * the "majority" (B,C) side of the partition thus ensuring that
     * transactions are not lost.
     */
    @Test
    public void testPostNetworkPartitionMaster()
        throws DatabaseException, InterruptedException {

        /* Turn off master rebroadcasts via the large broadcast period */
        createPartitionedGroup("1000000 s");

        /* perform durable writes. */
        final RepEnvInfo rei2 = repEnvInfo[1];
        ReplicatedEnvironment env2 = rei2.getEnv();
        Transaction txn = env2.beginTransaction(null, null);
        Database db = env2.openDatabase(txn, "test", dbconfig);
        txn.commit();
        db.close();

        // TODO: SAM: why is this test closing this env?
        rei2.closeEnv();

        WaitForReplicaListener replicaWaiter = new WaitForReplicaListener();

        final RepEnvInfo rei1 = repEnvInfo[0];
        final RepEnvInfo rei3 = repEnvInfo[2];
        ReplicatedEnvironment env1 = rei1.getEnv();
        ReplicatedEnvironment env3 = rei3.getEnv();
        rei1.getEnv().setStateChangeListener(replicaWaiter);

        healPartition();

        rei2.openEnv();
        env2 = rei2.getEnv();

        /* Node 1 should become a replica */
        assertTrue(replicaWaiter.awaitReplica());

        /*
         * The master must be on the majority partition's side. Either node
         * 2 or node 3 could have become a master. The previous env handles
         * should still be valid, as master->replica transition does not
         * require a recovery.
         */
        assertTrue(env1.isValid());
        assertTrue(env2.isValid());
        assertTrue(env3.isValid());
        assertTrue(env2.getState().isMaster() || env3.getState().isMaster());
        assertTrue(env1.getState().isReplica());
    }

    /**
     * Verifies that a unique master is re-established in the rep group after a
     * network partition involving a split where the master is on the minority
     * side of the network split has been resolved.
     *
     * Simulates a network partition with a master on the minority side and
     * then heals it. The obsolete master environment becomes a replica as
     * result.
     *
     * 1) Start a 3 node RG. node 1 is master.
     *
     * 2) Disable Acceptor/Learner/Feeder for node 1. Simulating a network
     *    partition.
     *
     * 3) Force node 2 to be master. We now have 2 masters. With node 1 not
     *    able to process durable writes and node 2 the true master.
     *
     * 4) Heal the network partition.
     *
     * 5) Verify that node1 is informed of the new master and becomes a replica.
     */
    @Test
    public void testPostNetworkPartition()
        throws DatabaseException, InterruptedException {

        final RepEnvInfo rei1 = repEnvInfo[0];

        createPartitionedGroup("1 s");

        ReplicatedEnvironment env1 = rei1.getEnv();
        WaitForReplicaListener replicaWaiter = new WaitForReplicaListener();
        env1.setStateChangeListener(replicaWaiter);

        /*
         * Sleep a multiple of the 1s period above. To ensure that the master
         * is broadcasting repeatedly.
         */
        Thread.sleep(10000);

        healPartition();

        assertTrue(replicaWaiter.awaitReplica());
        assertTrue(env1.isValid());
        assertEquals(ReplicatedEnvironment.State.REPLICA, env1.getState());

        rei1.closeEnv();
    }

    /**
     * This test demonstrates SR24615
     *
     * 1) Create a 3 node group.
     *
     * 2) Simulate a network partition with [node1(master)] and [node2(master,
     * node3(replica)]
     *
     * 3) 10 records are written to the majority partition and 100
     * (unacknowledged) records are written to the minority partition.
     *
     * 4) The nodes are shutdown and brought back up, with the network
     * functioning normally.
     *
     * 5) The nodes come up and hold an election choosing node1 as the master.
     *
     * 6) When nodes node2 and node3 join the master node1, they perform hard
     * rollbacks overwriting the previously acknowledged 10 records.
     */
    @Test
    public void testDemonstrateIncorrectMasterSelection()
        throws DatabaseException, InterruptedException {

        createPartitionedGroup("1000000 s");
        final RepEnvInfo rei1 = repEnvInfo[0];
        final RepEnvInfo rei2 = repEnvInfo[1];
        final RepEnvInfo rei3 = repEnvInfo[2];

        CommitToken ctMinority =
            populateDB(rei1.getEnv(), TEST_DB_NAME, 0, 100,
                                      RepTestUtils.SYNC_SYNC_NONE_TC);
        rei1.closeEnv();

        CommitToken ctMajority =
            populateDB(rei2.getEnv(), TEST_DB_NAME, 0, 10,
                       new TransactionConfig().
                       setDurability(RepTestUtils.DEFAULT_DURABILITY));

        /* Ensure Minority master has larger VLSN */
        assertTrue(ctMinority.compareTo(ctMajority) > 0);
        rei1.closeEnv();
        rei2.closeEnv();
        rei3.closeEnv();

        RepEnvInfo minfo = restartNodes(repEnvInfo);
        assertEquals(rei1, minfo);
    }

    /**
     * Simulates a network partitioned group with node 1 (the master) on one
     * side and nodes 2 an 3 on the other side, with node 2 being the master.
     *
     * It does so by disabling the Learner and Acceptor agents, as well as the
     * feeder service on node 1 and forcing node 2 to be the master, so that
     * node 1 is not informed that node 2 is the new master.
     */
    private void createPartitionedGroup(String rebroadcastPeriod)
        throws DatabaseException, InterruptedException {

        final RepEnvInfo rei1 = repEnvInfo[0];
        final RepEnvInfo rei2 = repEnvInfo[1];

        for (int i=0; i < groupSize; i++) {
            repEnvInfo[i].getRepConfig().setConfigParam
            (ReplicationConfig.ELECTIONS_REBROADCAST_PERIOD,
             rebroadcastPeriod);
        }

        createGroup();

        assertTrue(rei1.getEnv().getState().isMaster());

        logger.info("Simulating partition");

        RepTestUtils.disableServices(rei1);

        WaitForMasterListener masterWaiter = new WaitForMasterListener();
        rei2.getEnv().setStateChangeListener(masterWaiter);
        rei2.getRepNode().forceMaster(true);

        masterWaiter.awaitMastership();

        /* Two masters in group. */
        assertTrue(rei1.getEnv().getState().isMaster());
        assertTrue(rei2.getEnv().getState().isMaster());

        logger.info("Simulated partition");
    }

    private void healPartition() {
        logger.info("healed partition");

        final RepEnvInfo rei1 = repEnvInfo[0];
        RepTestUtils.reenableServices(rei1);
    }
}
