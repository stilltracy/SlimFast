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

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.cleaner.VerifyUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

public class RecoveryUtilizationTest extends TestBase {

    private final File envRoot;

    public RecoveryUtilizationTest() {
        envRoot = SharedTestUtils.getTestDir();
    }

    /**
     * DupCountLNs are strange hybrid beasts. They are not replicated, because
     * they are incremented as a side effect of applying LNs. At recovery time,
     * any DupCountLNs that are part of a replicated txn must be recovered and
     * resurrected just like other replicated LNs though.
     *
     * This test is trying to create a log has a DupCountLN followed by its DIN
     * parent, like this:
     *
     * 100 DupCountLN for uncommitted, replicated txn
     * 200 DIN that refers to DupCountLN
     *
     * where the log entries are within a checkpoint interval that will be 
     * processed at recovery. The bug is that DupCountLN, which is not a 
     * replicated log entry, was being undone by recovery because it was not 
     * committed. Then it was redone by recovery because it is in a replicated
     * txn. Although the logical outcome was correct -- the DIN parent 
     * continued to point to DupCountLN 100, the utilization was wrong.
     * 
     * @throws InterruptedException 
     */
    @Test
    public void testDupCountRecoverySR17879() 
        throws IOException, InterruptedException {

        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry data = new DatabaseEntry();

        /* Make a two node group. */
        RepEnvInfo[] repEnvInfo = RepTestUtils.setupEnvInfos(envRoot, 2);
        ReplicatedEnvironment master = RepTestUtils.joinGroup(repEnvInfo);
        RepEnvInfo replicaInfo = null;
        ReplicatedEnvironment replica = null;
        for (RepEnvInfo info: repEnvInfo) {
            if (info.getEnv() != master) {
                replicaInfo = info;
                replica = replicaInfo.getEnv();
            }
        }

        Database db = openDb(master);
        Transaction txnA = null;
        try {
            /* Create a dup tree of k=1/d=1, k=1/d=2, and commit it. */
            IntegerBinding.intToEntry(1, key);
            IntegerBinding.intToEntry(1, data);
            assertEquals(OperationStatus.SUCCESS, db.put(null, key, data));
            IntegerBinding.intToEntry(2, data);
            assertEquals(OperationStatus.SUCCESS, db.put(null, key, data));

            /* 
             * Now begin what will be an uncommitted txn, that will result in
             * the update of the dup tree, thereby logging a DupCountLN to the
             * log.
             */
            txnA = master.beginTransaction(null, null);
            IntegerBinding.intToEntry(3, data);
            assertEquals(OperationStatus.SUCCESS, db.put(txnA, key, data));

            /* 
             * Now ensure that the replica receives the changes from txnA.  To
             * do that, make and commit an unrelated change, purely as a way of
             * ensuring that the replica proceeds to a known spot in the
             * replication stream.
             */
            Transaction txnB = master.beginTransaction(null, null);
            IntegerBinding.intToEntry(10, key);
            IntegerBinding.intToEntry(10, data);
            assertEquals(OperationStatus.SUCCESS, db.put(txnB, key, data));    
            txnB.commit();
            RepTestUtils.syncGroupToLastCommit(repEnvInfo, 2);

            /*
             * Do a checkpoint on the replica to ensure that the DIN parent of
             * the problem DupCountLN goes to disk.
             */
            CheckpointConfig ckptConfig = new CheckpointConfig();
            ckptConfig.setForce(true);
            replica.checkpoint(ckptConfig);

            /* Crash the replica, then recovery it. */
            replicaInfo.abnormalCloseEnv();
            replicaInfo.openEnv();

            /*
             * Check that the utilization offsets match those in the tree. 
             * When the bug is in effect, this will fail.
             */
            Database repDb = openDb(replicaInfo.getEnv());
            try {
                VerifyUtils.checkLsns(repDb);
            } finally {
        	repDb.close();
            }

        } finally {
            txnA.abort();
            db.close();
            RepTestUtils.shutdownRepEnvs(repEnvInfo);
        }
    }

    private Database openDb(Environment env) {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(true);
        dbConfig.setSortedDuplicates(true);

        return env.openDatabase(null, "foo", dbConfig);
    }
}
