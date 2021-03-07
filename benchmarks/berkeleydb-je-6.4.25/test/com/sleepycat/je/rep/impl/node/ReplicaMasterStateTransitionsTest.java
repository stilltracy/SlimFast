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

package com.sleepycat.je.rep.impl.node;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.sleepycat.je.CommitToken;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;
import com.sleepycat.je.rep.CommitPointConsistencyPolicy;
import com.sleepycat.je.rep.RepInternal;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.impl.RepTestBase;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.util.FileHandler;
import com.sleepycat.je.utilint.PollCondition;

public class ReplicaMasterStateTransitionsTest extends RepTestBase {

    /*
     * This test was motivated by SR 18212. In this test node 1 starts out as a
     * master, relinquishes mastership to node 2, and then tries to resume as a
     * replica with node 2 as the master.
     */
    @Test
    public void testMasterReplicaTransition()
        throws Throwable {

        FileHandler.STIFLE_DEFAULT_ERROR_MANAGER = true;
        createGroup();
        ReplicatedEnvironment renv1 = repEnvInfo[0].getEnv();
        assertTrue(renv1.getState().isMaster());
        {
            Transaction txn =
                renv1.beginTransaction(null, RepTestUtils.SYNC_SYNC_ALL_TC);
            renv1.openDatabase(txn, "db1", dbconfig).close();
            txn.commit();
        }
        final ReplicatedEnvironment renv2 = repEnvInfo[1].getEnv();
        final RepNode rn2 =  RepInternal.getRepImpl(renv2).getRepNode();

        assertFalse(renv2.getState().isMaster());
        rn2.forceMaster(true);
        /* Verify handle has transitioned to master state. */
        assertTrue(new PollCondition(100, 60000) {

            @Override
            protected boolean condition() {
                return renv2.getState().isMaster();
            }
        }.await());

        /* Wait for replicas to join up prior to seeking ALL acks. */
        findMasterAndWaitForReplicas(60000, repEnvInfo.length - 1, repEnvInfo);

        renv1 = repEnvInfo[0].getEnv();

        CommitToken db2CommitToken = null;
        {
            Transaction txn =
                renv2.beginTransaction(null, RepTestUtils.SYNC_SYNC_ALL_TC);
            renv2.openDatabase(txn, "db2", dbconfig).close();
            txn.commit();
            db2CommitToken = txn.getCommitToken();
        }

        /*
         * Verify that the change was replayed at the replica via the
         * replication stream.
         */
        {
            TransactionConfig txnConfig = new TransactionConfig();
            txnConfig.setConsistencyPolicy
                (new CommitPointConsistencyPolicy
                 (db2CommitToken, 60, TimeUnit.SECONDS));
            Transaction txn = renv1.beginTransaction(null, txnConfig);
            assertTrue(renv1.getDatabaseNames().contains("db2"));
            txn.commit();
        }
    }
}
