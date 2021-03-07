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

package com.sleepycat.je.rep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.rep.ReplicatedEnvironment.State;
import com.sleepycat.je.rep.impl.RepTestBase;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;

public class NodePriorityTest extends RepTestBase {

    /*
     * Check that a NZ prio node is elected a master even when there are other
     * zero prio nodes with more current log files.
     */
    @Test
    public void testNZObsoletLogfiles() throws InterruptedException {

        createGroup();
        closeNodes(repEnvInfo);

        /* Only the last node has non-zero priority */
        for (int i = 0; i <= repEnvInfo.length - 2; i++) {
            repEnvInfo[i].getRepConfig().setNodePriority(0);
        }

        /* Last node should always be elected the master */
        RepEnvInfo minfo = restartNodes(repEnvInfo);
        assertEquals(1, minfo.getRepConfig().getNodePriority());
        assertEquals(repEnvInfo[repEnvInfo.length-1].getRepNode().getNodeId(),
                     minfo.getRepNode().getNodeId());

        /* shutdown second last node. */
        final int secondLast = repEnvInfo.length - 2;
        final RepEnvInfo secondLastEnv = repEnvInfo[secondLast];
        secondLastEnv.closeEnv();
        /* It will come up with a NZ prio. */
        secondLastEnv.getRepConfig().setNodePriority(1);

        /* Make changes, obsoleting the logs on secondLast  since it's down */
        ReplicatedEnvironment menv = minfo.getEnv();
        Transaction txn = menv.beginTransaction(null, null);
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(true);

        /*
         * Make updates to ensure that some zero prio nodes have more up to
         * date files relative to secondLastEnv.
         */
        Database db1 = menv.openDatabase(txn, "db1", dbConfig);
        txn.commit(); /* commit with simple majority. */
        db1.close();

        /* Now shutdown the only node with NZ priority, the current master */
        minfo.closeEnv();

        /* Bring up the NZ prio node with the obsolete logs. */
        secondLastEnv.openEnv();
        assertTrue(secondLastEnv.getEnv().getState().isMaster());

        for (int i = 0; i < secondLast; i++) {
            /* Explicitly close invalid (RollbackException) environments. */
            repEnvInfo[i].closeEnv();
        }
    }

    /* Simple API test. */
    @Test
    public void testPriorityBasic() {
        ReplicationConfig repConfig = new ReplicationConfig();
        int prio = repConfig.getNodePriority();
        assertEquals(1, prio); /* Verify default priority. */
        repConfig.setNodePriority(++prio);
        assertEquals(prio, repConfig.getNodePriority());

        try {
            repConfig.setNodePriority(-1);
            fail("expected exception");
        } catch (IllegalArgumentException iae) {
            // Expected
        }
    }

    /*
     * Test failure/restart of a solitary NZ prio node in a group.
     */
    @Test
    public void testNZFailoverAndRestore() throws InterruptedException {

        createGroup();
        closeNodes(repEnvInfo);

        final int last = repEnvInfo.length - 1;

        /* Only the last node has non-zero priority */
        for (int i = 0; i < last; i++) {
            repEnvInfo[i].getRepConfig().setNodePriority(0);
        }

        /* Last node should always be elected the master */
        RepEnvInfo minfo = restartNodes(repEnvInfo);
        assertTrue(minfo.getRepConfig().getNodePriority() > 0);

        ReplicatedEnvironment listenEnv = repEnvInfo[0].getEnv();
        ElectionListener listener = new ElectionListener();
        listenEnv.setStateChangeListener(listener);

        /* Now shutdown the only node with NZ priority. */
        minfo.closeEnv();

        /* We should not be able to conclude an election. */
        boolean ok = listener.electionLatch.await(10, TimeUnit.SECONDS);
        assertFalse(ok);

        /*
         * Now bring the NZ prio node back up again, so it's selected the
         * master, since it's the only choice.
         */
        minfo.openEnv();
        ok = listener.electionLatch.await(10, TimeUnit.SECONDS);
        assertTrue(ok);
    }


    /**
     * test that elections only pick nodes with NZ priority. Kill masters
     * checking each new master that's elected to make sure it has NZ
     * priority.
     */
    @Test
    public void testOnlyNZMasters() throws InterruptedException {

        createGroup();
        closeNodes(repEnvInfo);

        final int majority = (repEnvInfo.length/2) - 1;

        /* Set less than a simple majority of nodes to have prio zero. */
        for (int i = 0; i < majority; i++) {
            repEnvInfo[i].getRepConfig().setNodePriority(0);
        }

        /* Last node should always be elected the master */
        RepEnvInfo minfo = restartNodes(repEnvInfo);
        assertTrue(minfo.getRepConfig().getNodePriority() > 0);

        for (int i=0; i < majority; i++) {
            minfo = findMaster(repEnvInfo);
            assertNotNull(minfo);
            assertTrue(minfo.getRepConfig().getNodePriority() > 0);

            // wait for new master to emerge
            ReplicatedEnvironment listenEnv = repEnvInfo[0].getEnv();
            ElectionListener listener = new ElectionListener();
            listenEnv.setStateChangeListener(listener);
            minfo.closeEnv();
            /* Verify that election has been concluded. */
            boolean ok = listener.electionLatch.await(10, TimeUnit.SECONDS);
            assertTrue(ok);
        }
    }

    /* Listen for an election to be concluded. */
    class ElectionListener implements StateChangeListener {

        final CountDownLatch electionLatch;
        State prevState;
        State newState;

        ElectionListener() {
            electionLatch = new CountDownLatch(1);
        }

        public void stateChange(StateChangeEvent stateChangeEvent)
            throws RuntimeException {
            if (prevState == null) {
                prevState = stateChangeEvent.getState();
                /* Ignore the first immediate synchronous callback. */
                return;
            }

            newState = stateChangeEvent.getState();

            if (newState.isMaster() || newState.isReplica()) {
                electionLatch.countDown();
            }
        }
    }
}
