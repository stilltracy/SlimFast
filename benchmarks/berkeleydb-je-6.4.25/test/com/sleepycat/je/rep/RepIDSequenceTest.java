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
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.rep.impl.RepImpl;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;
import com.sleepycat.je.utilint.VLSN;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

public class RepIDSequenceTest extends TestBase {
    private final File envRoot;
    private RepEnvInfo[] repEnvInfo;
    private final int DB_NUM = 5;
    private final int DB_SIZE = 100;
    private final String DB_NAME = "test";
    private final boolean verbose = Boolean.getBoolean("verbose");

    public RepIDSequenceTest() {
        envRoot = SharedTestUtils.getTestDir();
    }

    /* Verify that id generation is in sequence after recovery. */
    @Test
    public void testIDSequenceAfterRecovery()
        throws Exception {

        repEnvInfo = RepTestUtils.setupEnvInfos(envRoot, 1);
        /* Get configurations so that they can be used when reopened.*/
        ReplicatedEnvironment master = RepTestUtils.joinGroup(repEnvInfo);
        ReplicationConfig repConfig = master.getRepConfig();
        EnvironmentConfig envConfig = master.getConfig();
        File envHome = master.getHome();
        /* Do some operations. */
        doOperations(master, 1, DB_NUM);
        /* Do a sync to make sure the ids are written to the log. */
        master.sync();
        RepImpl repImpl = RepInternal.getRepImpl(master);
        long lastRepNodeId =
            repImpl.getNodeSequence().getLastReplicatedNodeId();
        long lastDbId = repImpl.getDbTree().getLastReplicatedDbId();
        long lastTxnId = repImpl.getTxnManager().getLastReplicatedTxnId();
        /* Make sure that replicated ids are negative ids. */
        assertTrue(lastRepNodeId < 0);
        assertTrue(lastDbId < 0);
        assertTrue(lastTxnId < 0);
        master.close();

        master = new ReplicatedEnvironment(envHome, repConfig, envConfig);
        assertTrue(master.getState().isMaster());
        repImpl = RepInternal.getRepImpl(master);

        /*
         * The node id, db id and txn id should be the same as before, since
         * the test doesn't do any operations during this time.
         */
        assertEquals(lastRepNodeId,
                     repImpl.getNodeSequence().getLastReplicatedNodeId());
        assertEquals(lastDbId,
                     repImpl.getDbTree().getLastReplicatedDbId());
        /*
         * The - 1 (replicated txn ids are represented by the series, -1, -2,
         * -3, ...) below is to account for the additional transaction that is
         * used to update the CBVLSN of the master when it is opened above.
         */
        assertEquals(lastTxnId - 1,
                     repImpl.getTxnManager().getLastReplicatedTxnId());
        doOperations(master, DB_NUM + 1, DB_NUM + 1);

        /*
         * The db id and txn id should be smaller than before, since test
         * creats a new database, db id should be 1 smaller than before.
         */
        assertTrue
            (repImpl.getDbTree().getLastReplicatedDbId() == lastDbId - 1);
        assertTrue
            (repImpl.getTxnManager().getLastReplicatedTxnId() < lastTxnId);
        master.close();
    }

    /* Verify that ids are in sequence after fail over. */
    @Test
    public void testIDSequenceAfterFailOver()
        throws Exception {

        repEnvInfo = RepTestUtils.setupEnvInfos(envRoot, 3);
        ReplicatedEnvironment master = RepTestUtils.joinGroup(repEnvInfo);
        doOperations(master, 1, DB_NUM);
        master.sync();
        RepImpl repImpl = RepInternal.getRepImpl(master);
        long lastRepNodeId =
            repImpl.getNodeSequence().getLastReplicatedNodeId();
        long lastDbId = repImpl.getDbTree().getLastReplicatedDbId();
        long lastTxnId = repImpl.getTxnManager().getLastReplicatedTxnId();
        /* Make sure that replicated ids are negative ids. */
        assertTrue(lastRepNodeId < 0);
        assertTrue(lastDbId < 0);
        assertTrue(lastTxnId < 0);
        checkEquality(repEnvInfo);
        master.close();

        master = RepTestUtils.openRepEnvsJoin(repEnvInfo);
        assertTrue(master.getState().isMaster());
        repImpl = RepInternal.getRepImpl(master);
        assertEquals(lastRepNodeId,
                     repImpl.getNodeSequence().getLastReplicatedNodeId());
        assertEquals(lastDbId,
                     repImpl.getDbTree().getLastReplicatedDbId());

        /*
         * The replication group needs to do elections to select a master, so
         * the txn id should be different as before.
         */
        assertTrue
            (repImpl.getTxnManager().getLastReplicatedTxnId() <= lastTxnId);
        /* Create a new database. */
        doOperations(master, DB_NUM + 1, DB_NUM + 1);
        assertTrue
            (repImpl.getDbTree().getLastReplicatedDbId() == lastDbId - 1);
        assertTrue
            (repImpl.getTxnManager().getLastReplicatedTxnId() < lastTxnId);
        checkEquality(RepTestUtils.getOpenRepEnvs(repEnvInfo));
    }

    private void doOperations(ReplicatedEnvironment master,
                              int beginId,
                              int endId)
        throws Exception {

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(true);
        dbConfig.setSortedDuplicates(false);

        for (int i = beginId; i <= endId; i++) {
            Database db = master.openDatabase
                (null, DB_NAME + new Integer(i).toString(), dbConfig);
            insertData(db);
            db.close();
        }
    }

    private void insertData(Database db)
        throws Exception {

        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry data = new DatabaseEntry();
        for (int i = 1; i <= DB_SIZE; i++) {
            IntegerBinding.intToEntry(i, key);
            StringBinding.stringToEntry("herococo", data);
            db.put(null, key, data);
        }
    }

    private void checkEquality(RepEnvInfo[] repInfoArray)
        throws Exception {

        VLSN vlsn = RepTestUtils.syncGroupToLastCommit(repInfoArray,
                                                       repInfoArray.length);
        RepTestUtils.checkNodeEquality(vlsn, verbose, repInfoArray);
    }
}
