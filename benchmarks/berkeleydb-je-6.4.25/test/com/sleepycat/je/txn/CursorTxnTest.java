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

import static com.sleepycat.je.txn.LockStatDefinition.LOCK_READ_LOCKS;
import static com.sleepycat.je.txn.LockStatDefinition.LOCK_WRITE_LOCKS;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.Cursor;
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
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.dbi.CursorImpl;
import com.sleepycat.je.dbi.DbEnvPool;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.je.utilint.StatGroup;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

public class CursorTxnTest extends TestBase {
    private final File envHome;
    private Environment env;
    private Database myDb;
    private int initialEnvReadLocks;
    private int initialEnvWriteLocks;
    private boolean noLocking;

    public CursorTxnTest() {
        envHome = SharedTestUtils.getTestDir();
        DbEnvPool.getInstance().clear();
    }

    @Before
    public void setUp()
        throws Exception {

        super.setUp();
        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        DbInternal.setLoadPropertyFile(envConfig, false);
        envConfig.setConfigParam(EnvironmentParams.NODE_MAX.getName(), "6");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_INCOMPRESSOR.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_EVICTOR.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_CHECKPOINTER.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_CLEANER.getName(), "false");
        envConfig.setAllowCreate(true);
        env = new Environment(envHome, envConfig);

        EnvironmentConfig envConfigAsSet = env.getConfig();
        noLocking = !(envConfigAsSet.getLocking());

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(true);
        myDb = env.openDatabase(null, "test", dbConfig);
    }

    @After
    public void tearDown() {
        try {
            myDb.close();
        } catch (DatabaseException ignored) {}
        try {
            env.close();
        } catch (DatabaseException ignored) {}
    }

    /**
     * Create a cursor with a null transaction.
     */
    @Test
    public void testNullTxnLockRelease()
        throws DatabaseException {

        getInitialEnvStats();
        Cursor cursor = myDb.openCursor(null, null);

        /* First put() holds a write lock on the non-duplicate entry. */
        insertData(cursor, 10, 1);
        checkReadWriteLockCounts(cursor, 0, 1);

        // Check that count does not add more locks
        int count = cursor.count();
        assertEquals(1, count);
        checkReadWriteLockCounts(cursor, 0, 1);

        /*
         * Second put() holds a single write lock, now that we no longer create
         * a DIN/DBIN tree.
         */
        insertData(cursor, 10, 2);
        checkReadWriteLockCounts(cursor, 0, 1);

        /* Check that count does not add more locks. */
        count = cursor.count();
        assertEquals(2, count);
        checkReadWriteLockCounts(cursor, 0, 1);

        /*
         * Third put() holds one write lock.
         */
        insertData(cursor, 10, 3);
        checkReadWriteLockCounts(cursor, 0, 1);

        DatabaseEntry foundKey = new DatabaseEntry();
        DatabaseEntry foundData = new DatabaseEntry();

        /* Check that read locks are held on forward traversal. */
        OperationStatus status =
            cursor.getFirst(foundKey, foundData, LockMode.DEFAULT);
        checkReadWriteLockCounts(cursor, 1, 0);
        int numSeen = 0;
        while (status == OperationStatus.SUCCESS) {
            numSeen++;
            status = cursor.getNext(foundKey, foundData, LockMode.DEFAULT);
            checkReadWriteLockCounts(cursor, 1, 0);
            if (status != OperationStatus.SUCCESS) {
                break;
            }

            status = cursor.getCurrent(foundKey, foundData,
                                       LockMode.DEFAULT);
            checkReadWriteLockCounts(cursor, 1, 0);
        }
        assertEquals(30, numSeen);

        /* Check that read locks are held on backwards traversal and count. */
        status = cursor.getLast(foundKey, foundData, LockMode.DEFAULT);
        checkReadWriteLockCounts(cursor, 1, 0);

        while (status == OperationStatus.SUCCESS) {
            count = cursor.count();
            assertEquals("For key " +
                         TestUtils.dumpByteArray(foundKey.getData()),
                         3, count);
            status = cursor.getPrev(foundKey, foundData, LockMode.DEFAULT);
            checkReadWriteLockCounts(cursor, 1, 0);
        }

        /* Check that delete holds a write lock. */
        status = cursor.getFirst(foundKey, foundData, LockMode.DEFAULT);
        while (status == OperationStatus.SUCCESS) {
            assertEquals("For key " +
                         TestUtils.dumpByteArray(foundKey.getData()),
                         OperationStatus.SUCCESS, cursor.delete());
            /* Two write locks (old/new LSNs) on deleted LN. */
            checkReadWriteLockCounts(cursor, 0, 2);
            status = cursor.getNext(foundKey, foundData, LockMode.DEFAULT);
            if (status == OperationStatus.SUCCESS) {
                checkReadWriteLockCounts(cursor, 1, 0);
            } else {
                checkReadWriteLockCounts(cursor, 0, 2);
            }
        }

        /* Check that count does not add more locks. */
        count = cursor.count();
        assertEquals(0, count);
        checkReadWriteLockCounts(cursor, 0, 2);

        cursor.close();
    }

    private void checkReadWriteLockCounts(Cursor cursor,
                                          int expectReadLocks,
                                          int expectWriteLocks)
        throws DatabaseException {

        if (noLocking) {
            expectReadLocks = expectWriteLocks = 0;
        }

        CursorImpl cursorImpl = DbTestProxy.dbcGetCursorImpl(cursor);
        StatGroup cursorStats = cursorImpl.getLockStats();
        assertEquals(expectReadLocks, cursorStats.getInt(LOCK_READ_LOCKS));
        assertEquals(expectWriteLocks, cursorStats.getInt(LOCK_WRITE_LOCKS));

        EnvironmentStats lockStats = env.getStats(null);
        assertEquals(initialEnvReadLocks + expectReadLocks,
                     lockStats.getNReadLocks());
        assertEquals(initialEnvWriteLocks + expectWriteLocks,
                     lockStats.getNWriteLocks());
    }

    private void getInitialEnvStats()
        throws DatabaseException {

        EnvironmentStats lockStats = env.getStats(null);
        initialEnvReadLocks = lockStats.getNReadLocks();
        initialEnvWriteLocks = lockStats.getNWriteLocks();
    }

    private void insertData(Cursor cursor, int numRecords, int dataVal)
        throws DatabaseException {

        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry data = new DatabaseEntry();

        for (int i = 0; i < numRecords; i++) {
            byte[] keyData = TestUtils.getTestArray(i);
            byte[] dataData = new byte[1];
            dataData[0] = (byte) dataVal;
            key.setData(keyData);
            data.setData(dataData);
            OperationStatus status = cursor.putNoDupData(key, data);
            assertEquals(OperationStatus.SUCCESS, status);
        }
    }
}
