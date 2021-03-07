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
package com.sleepycat.je.rep.node.replica;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.sleepycat.je.CommitToken;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;
import com.sleepycat.je.rep.CommitPointConsistencyPolicy;
import com.sleepycat.je.rep.DatabasePreemptedException;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.impl.RepTestBase;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;
import com.sleepycat.je.utilint.VLSN;

public class ReplayTest extends RepTestBase {

    static final String dbName = "ReplayTestDB";

    /*
     * Tests that a Replica correctly replays a transaction that was resumed
     * after a syncup operation.
     */
    @Test
    public void testResumedTransaction() {
        ReplicatedEnvironment master = RepTestUtils.joinGroup(repEnvInfo);
        assertTrue(repEnvInfo[1].getEnv().getState().isReplica());
        ReplicatedEnvironment replica = repEnvInfo[1].getEnv();
        Transaction mt1 = master.beginTransaction(null, null);
        String dbName1 = "DB1";
        Database db1m = master.openDatabase(mt1, dbName1, dbconfig);

        /* Leave the transaction open. */

        /* Start a new transaction and get its commit token. */
        Transaction mt2 = master.beginTransaction(null, null);
        String dbName2 = "DB2";
        Database db2 = master.openDatabase(mt1, dbName2, dbconfig);
        db2.put(mt2, key, data);
        db2.close();
        mt2.commit();
        CommitToken ct2 = mt2.getCommitToken();
        db1m.put(mt1, key, data);

        /* Sync replica to mt2, it contains the put of mt1 as well. */
        TransactionConfig rconfig = new TransactionConfig();
        rconfig.setConsistencyPolicy
            (new CommitPointConsistencyPolicy(ct2, 60, TimeUnit.SECONDS));
        Transaction rt1 = replica.beginTransaction(null, rconfig);
        rt1.commit();

        /* Now shut down the replica, with mt1 still open. */
        repEnvInfo[1].closeEnv();

        /* Reopen forcing a sync, rt1 must be resurrected */
        replica = repEnvInfo[1].openEnv();
        db1m.close();
        mt1.commit();
        CommitToken ct1 = mt1.getCommitToken();
        rconfig.setConsistencyPolicy
            (new CommitPointConsistencyPolicy(ct1, 60, TimeUnit.SECONDS));

        Transaction rt2 = null;

        rt2 = replica.beginTransaction(null, rconfig);

        DatabaseConfig dbrconfig = new DatabaseConfig();
        dbrconfig.setAllowCreate(false);
        dbrconfig.setTransactional(true);
        dbrconfig.setSortedDuplicates(false);
        /* Check that rt1 came through and created the DB1 on the replica. */
        Database db1r = replica.openDatabase(rt2, dbName1, dbrconfig);
        db1r.close();
        rt2.commit();
    }

    @Test
    public void testBasicDatabaseOperations()
        throws Exception {

        ReplicatedEnvironment master = RepTestUtils.joinGroup(repEnvInfo);
        Environment menv = master;

        String truncDbName = "ReplayTestDBTrunc";
        String origDbName = "ReplayTestDBOrig";
        String newDbName = "ReplayTestDBNew";
        String removeDbName = "ReplayTestDBRemove";

        // Create database

        menv.openDatabase(null, truncDbName, dbconfig).close();
        menv.openDatabase(null, origDbName, dbconfig).close();
        menv.openDatabase(null, removeDbName, dbconfig).close();

        RepTestUtils.syncGroupToLastCommit(repEnvInfo, repEnvInfo.length);

        /* make sure they have all showed up. */
        dbconfig.setAllowCreate(false);
        for (RepEnvInfo repi : repEnvInfo) {
            ReplicatedEnvironment rep = repi.getEnv();
            Environment renv = rep;
            renv.openDatabase(null, truncDbName, dbconfig).close();
            renv.openDatabase(null, origDbName, dbconfig).close();
            renv.openDatabase(null, removeDbName, dbconfig).close();
        }
        // Perform the operations on the master.
        menv.truncateDatabase(null, truncDbName, false);
        menv.renameDatabase(null, origDbName, newDbName);
        menv.removeDatabase(null, removeDbName);

        VLSN commitVLSN =
            RepTestUtils.syncGroupToLastCommit(repEnvInfo, repEnvInfo.length);

        /* Verify the changes on the replicators. */
        for (RepEnvInfo repi : repEnvInfo) {
            Environment renv = repi.getEnv();
            // the database should be found
            renv.openDatabase(null, truncDbName, dbconfig).close();
            try {
                renv.openDatabase(null, origDbName, dbconfig).close();
                fail("Expected DatabaseNotFoundException");
            } catch (DatabaseNotFoundException e) {
                // expected
            }
            // renamed db should be found
            renv.openDatabase(null, newDbName, dbconfig).close();
            try {
                renv.openDatabase(null, removeDbName, dbconfig);
                fail("Expected DatabaseNotFoundException");
            } catch (DatabaseNotFoundException e) {
                // expected
            }
        }
        RepTestUtils.checkNodeEquality(commitVLSN, false, repEnvInfo);
    }

    @Test
    public void testDatabaseOpContention()
        throws Exception {

        ReplicatedEnvironment master = RepTestUtils.joinGroup(repEnvInfo);
        Environment menv = master;
        Environment renv = repEnvInfo[1].getEnv();

        Database mdb = menv.openDatabase(null, dbName, dbconfig);
        mdb.close();
        RepTestUtils.syncGroupToLastCommit(repEnvInfo, repEnvInfo.length);
        Database rdb = renv.openDatabase(null, dbName, dbconfig);
        menv.removeDatabase(null, dbName);
        RepTestUtils.syncGroupToLastCommit(repEnvInfo, repEnvInfo.length);
        try {
            rdb.count();
            fail("Expected exception. Handle should have been invalidated");
        } catch (DatabasePreemptedException e) {
            // expected
        }
    }
}
