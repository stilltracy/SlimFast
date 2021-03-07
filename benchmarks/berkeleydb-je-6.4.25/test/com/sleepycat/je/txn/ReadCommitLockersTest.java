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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.DbTestProxy;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentStats;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.dbi.CursorImpl;
import com.sleepycat.je.dbi.DbEnvPool;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.je.utilint.StatGroup;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/*
 * This is a test for SR #23783. Before fixing the bug described there, the
 * following scenario would cause a deadlock, when no real deadlock exists. 
 *
 * 1. Cursor C1 in thread T1 reads a record R using Txn X1. C1 creates a
 *  ReadCommittedLocker L1, with X1 as its buddy. L1 locks R.
 *
 * 2. Cursor C2 in thread T2 tries to write-lock R, using another Txn X2.
 * X2 waits for L1 (==> T2 waits for T1).
 *
 * 3. Cursor C3 in thread T1 tries to read R using X1. C3 creates a
 * ReadCommittedLocker L3, with X1 as its buddy. L3 tries to lock R. L1 and
 * L3 are not recognized as buddies, so L3 waits for X2 (==> T1 waits for T2) 
 */
public class ReadCommitLockersTest extends TestBase {

    File envHome;
    Environment env;
    Database db;

    DatabaseEntry key;

    Object synchronizer1 = new Object();
    Object synchronizer2 = new Object();

    boolean wait1 = true;
    boolean wait2 = true;

    public static DatabaseEntry createDatabaseEntry(String key) {

        DatabaseEntry keyDBEntry = null;
        try {
           keyDBEntry = new DatabaseEntry(key.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.out.println("Unexpected UnsupportedEncodingException");
        }

      return keyDBEntry;
    }


    public ReadCommitLockersTest() {

        envHome = SharedTestUtils.getTestDir();
        DbEnvPool.getInstance().clear();

        key = createDatabaseEntry("key");
    }

    @Before
    public void setUp() throws Exception {

        super.setUp();
        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);

        env = new Environment(envHome, envConfig);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(true);

        db = env.openDatabase(null, "test", dbConfig);
    }

    @After
    public void tearDown() {
        try {
            db.close();
        } catch (DatabaseException ignored) {}
        try {
            env.close();
        } catch (DatabaseException ignored) {}
    }

    @Test
    public void runTest()
        throws DatabaseException,
               UnsupportedEncodingException,
               InterruptedException {

        /*
         * Insert a record R in the DB
         */
        Transaction txn = env.beginTransaction(null, null);
        Cursor cursor = db.openCursor(txn, null);

        DatabaseEntry data = createDatabaseEntry("data");

        assertEquals(OperationStatus.SUCCESS, cursor.put(key, data));

        cursor.close();
        txn.commit();

        /*
         * Start thread T1 and then wait until it reads record R via a
         * read-comitted cursor C1. 
         */
        Thread T1 = new TestThread1();
        T1.start();

        synchronized (synchronizer1) {
            while (wait1) {
                synchronizer1.wait();
            }
        }

        /*
         * Start thread T2. In the mentime, T1 is waiting on synchronizer2.
         */
        Thread T2 = new TestThread2();
        T2.start();

        /*
         * Sleep for a while to allow T2 to block on its lock request. 
         */
        while (T2.getState() == Thread.State.RUNNABLE ||
               T2.getState() == Thread.State.NEW) {
            Thread.currentThread().sleep(10);
        }

        /*
         * Let thread T1 continue with another search on R via another
         * read-comitted cursor C2. 
         */
        synchronized (synchronizer2) {
            wait2 = false;
            synchronizer2.notify();
        }

        /*
         * Wait for both T1 and T2 to terminate.
         */
        T1.join();
        T2.join();

        data = new DatabaseEntry();

        cursor = db.openCursor(null, null);

        try {
            OperationStatus status = cursor.getSearchKey(key, data, null);
            assertTrue(status == OperationStatus.SUCCESS);

            String dataStr = new String(data.getData());
            System.out.println(dataStr);
            assertTrue(dataStr.equals("newdata"));
        } finally {
            cursor.close();
        }
    }

    private class TestThread1 extends Thread
    {
        Environment env;
        Database db;

        DatabaseEntry key;

        Object synchronizer1;
        Object synchronizer2;

        public TestThread1() {
            super("Thread-1");
            this.env = ReadCommitLockersTest.this.env;
            this.db = ReadCommitLockersTest.this.db;
            this.synchronizer1 = ReadCommitLockersTest.this.synchronizer1;
            this.synchronizer2 = ReadCommitLockersTest.this.synchronizer2;
            this.key = ReadCommitLockersTest.this.key;
        }

        @Override
        public void run() {

            OperationStatus status;
            CursorConfig config = new CursorConfig();
            config.setReadCommitted(true);

            DatabaseEntry data = new DatabaseEntry();
            data.setPartial(true);

            Transaction X1 = env.beginTransaction(null, null);

            /* Do a read-committed search for R via cursor C1 and txn X1. */
            Cursor C1 = db.openCursor(X1, config);
            status = C1.getSearchKey(key, data, LockMode.DEFAULT);
            assertTrue(status == OperationStatus.SUCCESS);

            /* Wake up the main thread so that it will start thread T2. */
            synchronized (synchronizer1) {
                ReadCommitLockersTest.this.wait1 = false;
                synchronizer1.notify();
            }

            /* Wait until thread T2 blocks trying to write-lock R. */
            synchronized (synchronizer2) {
                while (ReadCommitLockersTest.this.wait2) {
                    try {
                        synchronizer2.wait();
                    } catch (InterruptedException e) {
                        System.out.println("Unexpected InterruptedException");
                    }
                }
            }
            
            /* Do a read-committed search for R via cursor C3 and txn X1. */
            Cursor C3 = db.openCursor(X1, config);
            status = C3.getSearchKey(key, data, LockMode.DEFAULT);
            assertTrue(status == OperationStatus.SUCCESS);
            
            C1.close();
            C3.close();
            X1.commit();
        }
    }


    private class TestThread2 extends Thread
    {
        Environment env;
        Database db;

        DatabaseEntry key;

        public TestThread2() {
            super("Thread-2");
            this.env = ReadCommitLockersTest.this.env;
            this.db = ReadCommitLockersTest.this.db;
            this.key = ReadCommitLockersTest.this.key;
        }

        @Override
        public void run() {

            boolean success = false;
            OperationStatus status;
            DatabaseEntry data = createDatabaseEntry("newdata");

            Transaction X2 = env.beginTransaction(null, null);

            /* Update R via cursor C2 and txn X2. */
            Cursor C2 = db.openCursor(X2, null);

            try {
                status = C2.put(key, data);
                assertTrue(status == OperationStatus.SUCCESS);
                success = true;
            } finally {
                C2.close();
                if (success) {
                    X2.commit();
                } else {
                    X2.abort();
                }
            }
        }
    }
}
