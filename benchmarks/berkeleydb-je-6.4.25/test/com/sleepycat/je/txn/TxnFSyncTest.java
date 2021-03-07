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

package com.sleepycat.je.txn;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.dbi.DbEnvPool;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.TxnTestCase;
import com.sleepycat.utilint.StringUtils;

/*
 * Make sure that transactions sync to disk. Mimic a crash by failing to
 * close the environment and explicitly flush the log manager. If we haven't
 * properly written and synced data to disk, we'll have unflushed data and
 * we won't find the expected data in the log.
 *
 * Note that this test is run with the TxnTestCase framework and will
 * be exercised with app-created and autocommit txns.
 */
@RunWith(Parameterized.class)
public class TxnFSyncTest extends TxnTestCase {

    private static final int NUM_RECS = 5;

    private static EnvironmentConfig envConfig = TestUtils.initEnvConfig();
    static {
        envConfig.setAllowCreate(true);
        setupEnvConfig(envConfig);
    }

    private static void setupEnvConfig(EnvironmentConfig envConfig) {
        envConfig.setTransactional(true);
        envConfig.setConfigParam(
            EnvironmentParams.ENV_RUN_CHECKPOINTER.getName(), "false");
    }

    @Parameters
    public static List<Object[]> genParams() {
        return getTxnParams(
            new String[] {TxnTestCase.TXN_USER, TxnTestCase.TXN_AUTO}, false);
    }
    
    public TxnFSyncTest(String type){
        super.envConfig = envConfig;
        txnType = type;
        isTransactional = (txnType != TXN_NULL);
        customName = txnType;
    }
    

    @Test
    public void testFSyncButNoClose()
        throws Exception {

        try {
            /* Create a database. */
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setTransactional(isTransactional);
            dbConfig.setAllowCreate(true);
            Transaction txn = txnBegin();
            Database db = env.openDatabase(txn, "foo", dbConfig);

            /* Insert data. */
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry data = new DatabaseEntry();
            for (int i = 0; i < NUM_RECS; i++) {
                Integer val = new Integer(i);
                key.setData(StringUtils.toUTF8(val.toString()));
                data.setData(StringUtils.toUTF8(val.toString()));

                assertEquals(OperationStatus.SUCCESS,
                             db.putNoOverwrite(txn, key, data));
            }
            txnCommit(txn);

            /*
             * Now throw away this environment WITHOUT flushing the log
             * manager. We do need to release the environment file lock
             * and all file handles so we can recover in this test and
             * run repeated test cases within this one test program.
             */
            EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);
            envImpl.getFileManager().clear(); // release file handles
            envImpl.getFileManager().close(); // release file lock
            envImpl.closeHandlers();  // release logging files
            env = null;
            DbEnvPool.getInstance().clear();

            /*
             * Open the environment and database again. The database should
             * exist.
             */
            EnvironmentConfig envConfig2 = TestUtils.initEnvConfig();
            setupEnvConfig(envConfig2);
            env = create(envHome, envConfig2);
            dbConfig.setAllowCreate(false);
            db = env.openDatabase(null, "foo", dbConfig);

            /* Read all the data. */
            for (int i = 0; i < NUM_RECS; i++) {
                Integer val = new Integer(i);
                key.setData(StringUtils.toUTF8(val.toString()));

                assertEquals(OperationStatus.SUCCESS,
                             db.get(null, key, data, LockMode.DEFAULT));
                /* add test of data. */
            }
            db.close();
            env.close();
            env = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (env != null) {
                env.close();
            }
        }
    }
}
