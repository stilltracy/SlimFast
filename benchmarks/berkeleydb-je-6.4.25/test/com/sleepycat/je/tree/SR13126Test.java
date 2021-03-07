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

package com.sleepycat.je.tree;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import com.sleepycat.collections.CurrentTransaction;
import com.sleepycat.collections.TransactionRunner;
import com.sleepycat.collections.TransactionWorker;
import com.sleepycat.compat.DbCompat;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.RunRecoveryException;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.dbi.MemoryBudget;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 */
public class SR13126Test extends TestBase {

    private final File envHome;
    private Environment env;
    private Database db;
    private long maxMem;

    public SR13126Test() {
        envHome = SharedTestUtils.getTestDir();
    }

    @After
    public void tearDown() {
        try {
            if (env != null) {
                env.close();
            }
        } catch (Exception e) {
            System.out.println("During tearDown: " + e);
        }

        env = null;
        db = null;
    }

    private boolean open()
        throws DatabaseException {

        maxMem = MemoryBudget.getRuntimeMaxMemory();
        if (maxMem == -1) {
            System.out.println
                ("*** Warning: not able to run this test because the JVM " +
                 "heap size is not available");
            return false;
        }

        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        /* Do not run the daemons to avoid timing considerations. */
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_CLEANER.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_EVICTOR.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_CHECKPOINTER.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_INCOMPRESSOR.getName(), "false");
        env = new Environment(envHome, envConfig);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(true);
        db = env.openDatabase(null, "foo", dbConfig);

        return true;
    }

    private void close()
        throws DatabaseException {

        db.close();
        db = null;

        env.close();
        env = null;
    }

    @Test
    public void testSR13126()
        throws DatabaseException {

        if (!open()) {
            return;
        }

        Transaction txn = env.beginTransaction(null, null);

        try {
            insertUntilOutOfMemory(txn);
            fail("Expected OutOfMemoryError");
        } catch (OutOfMemoryError expected) {
        } catch (RunRecoveryException expected) {
        }

        verifyDataAndClose();
    }

    @Test
    public void testTransactionRunner()
        throws Exception {

        if (!open()) {
            return;
        }

        final CurrentTransaction currentTxn =
            CurrentTransaction.getInstance(env);

        TransactionRunner runner = new TransactionRunner(env);
        /* Don't print exception stack traces during test runs. */
        DbCompat.TRANSACTION_RUNNER_PRINT_STACK_TRACES = false;
        try {
            runner.run(new TransactionWorker() {
                public void doWork()
                    throws Exception {

                    insertUntilOutOfMemory(currentTxn.getTransaction());
                }
            });
            fail("Expected OutOfMemoryError");
        } catch (OutOfMemoryError expected) {
        } catch (RunRecoveryException expected) {
        }

        /*
         * If TransactionRunner does not abort the transaction, this thread
         * will be left with a transaction attached.
         */
        assertNull(currentTxn.getTransaction());

        verifyDataAndClose();
    }

    private void insertUntilOutOfMemory(Transaction txn)
        throws DatabaseException, OutOfMemoryError {

        DatabaseEntry key = new DatabaseEntry(new byte[1]);
        DatabaseEntry data = new DatabaseEntry();

        int startMem = (int) (maxMem / 3);
        int bumpMem = (int) ((maxMem - maxMem / 3) / 5);

        /* Insert larger and larger LNs until an OutOfMemoryError occurs. */
        for (int memSize = startMem;; memSize += bumpMem) {

            /*
             * If the memory error occurs when we do "new byte[]" below, this
             * is not a test of the bug in question, so the test fails.
             */
            data.setData(new byte[memSize]);
            try {
                db.put(null, key, data);
            } catch (OutOfMemoryError e) {
                //System.err.println("Error during write " + memSize);
                throw e;
            }
        }
    }

    private void verifyDataAndClose()
        throws DatabaseException {

        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry data = new DatabaseEntry();

        /*
         * If a NULL_LSN is present in a BIN entry because of an incomplete
         * insert, an assertion will fire during the checkpoint when writing
         * the BIN.
         */
        env.close();
        env = null;

        /*
         * If the NULL_LSN was written above because assertions are disabled,
         * check that we don't get an exception when fetching it.
         */
        open();
        Cursor c = db.openCursor(null, null);
        while (c.getNext(key, data, null) == OperationStatus.SUCCESS) {}
        c.close();
        close();
    }
}
