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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import com.sleepycat.je.log.entry.BINDeltaLogEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.dbi.CursorImpl;
import com.sleepycat.je.log.LogManager;
import com.sleepycat.je.log.entry.LogEntry;
import com.sleepycat.je.tree.Key.DumpType;
import com.sleepycat.je.txn.BasicLocker;
import com.sleepycat.je.txn.Locker;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.je.utilint.DbLsn;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * Exercise the delta based BIN logging.
 */
public class BinDeltaTest extends TestBase {
    private static final String DB_NAME = "test";
    private static final boolean DEBUG = false;
    private Environment env;
    private final File envHome;
    private Database db;
    private LogManager logManager;

    public BinDeltaTest() {
        envHome = SharedTestUtils.getTestDir();

        /* Print keys as numbers */
        Key.DUMP_TYPE = DumpType.BINARY;
    }

    @Before
    public void setUp() 
        throws Exception {
        
        /*
         * Properties for creating an environment.  Disable the evictor for
         * this test, use larger BINS.
         */
        super.setUp();
        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setTransactional(true);
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_EVICTOR.getName(), "true");
        envConfig.setConfigParam
            (EnvironmentParams.NODE_MAX.getName(), "50");
        envConfig.setConfigParam
            (EnvironmentParams.BIN_DELTA_PERCENT.getName(), "50");
        envConfig.setAllowCreate(true);
        env = new Environment(envHome, envConfig);
        logManager = DbInternal.getEnvironmentImpl(env).getLogManager();
    }

    @After
    public void tearDown() {
        if (env != null) {
            try {
                env.close();
            } catch (DatabaseException E) {
            }
        }
    }

    /**
     * Create a db, fill with numRecords, return the first BIN.
     */
    private BIN initDb(int start, int end)
        throws DatabaseException {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        db = env.openDatabase(null, DB_NAME, dbConfig);

        addRecords(start, end);

        /* Now reach into the tree and get the first BIN */
        Locker txn = BasicLocker.
            createBasicLocker(DbInternal.getEnvironmentImpl(env));
        CursorImpl internalCursor =
            new CursorImpl(DbInternal.getDatabaseImpl(db), txn);
        assertTrue(internalCursor.positionFirstOrLast(true));
        BIN firstBIN = internalCursor.getBIN();
        firstBIN.releaseLatch();
        internalCursor.close();
        txn.operationEnd();
        return firstBIN;
    }

    /**
     * Modify the data, just to dirty the BIN.
     */
    private void modifyRecords(int start, int end, int increment)
        throws DatabaseException {

        Transaction txn = env.beginTransaction(null, null);
        Cursor cursor = db.openCursor(txn, null);
        DatabaseEntry searchKey = new DatabaseEntry();
        DatabaseEntry foundData = new DatabaseEntry();
        DatabaseEntry newData = new DatabaseEntry();

        for (int i = start; i <= end; i++) {
            searchKey.setData(TestUtils.getTestArray(i));
            assertEquals(OperationStatus.SUCCESS,
                         cursor.getSearchKey(searchKey, foundData,
                                             LockMode.DEFAULT));
            newData.setData(TestUtils.getTestArray(i+increment));
            cursor.putCurrent(newData);
        }
        cursor.close();
        txn.commit();
    }

    /*
     * Add the specified records.
     */
    private void addRecords(int start, int end)
        throws DatabaseException {

        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry data = new DatabaseEntry();
        for (int i = start;  i < end; i++) {
            byte[] keyData = TestUtils.getTestArray(i);
            byte[] dataData = TestUtils.byteArrayCopy(keyData);
            key.setData(keyData);
            data.setData(dataData);
            db.put(null, key, data);
        }
    }

    /**
     * Simple test, delta a BIN several times, reconstruct.
     */
    @Test
    public void testSimple()
            throws Throwable {

        try {
            /* Create a db, insert records value 10 - 30, get the first BIN */
            BIN bin = initDb(10, 30);

            /* Log a full version. */
            bin.latch();
            long fullLsn = bin.log(true, false, false, false, null);
            bin.releaseLatch();
            assertTrue(fullLsn != DbLsn.NULL_LSN);
            assertEquals(fullLsn, bin.getLastFullLsn());
            assertEquals(fullLsn, bin.getLastLoggedLsn());
            assertEquals(DbLsn.NULL_LSN, bin.getLastDeltaLsn());
            assertFalse(bin.getDirty());

            if (DEBUG) {
                System.out.println("Start");
                System.out.println(bin.dumpString(0, true));
            }

            /* Modify some of the data, add data so the BIN is changed. */
            modifyRecords(11,13,10);
            addRecords(1,3);
            logAndCheck(bin);

            /* Modify more of the data, so the BIN is changed. */
            modifyRecords(14,15,10);
            logAndCheck(bin);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            db.close();
        }
    }

    /**
     * Test that a delta is correctly generated when there are entries
     * that have been aborted and rolled back.
     *
     * The case we're trying to test, (that was in error before)
     *  - a record is deleted
     *  - a full version of BIN x is written to the log, reflecting that
     *    deletion.
     *  - the deleting txn is aborted, so the record is restored. Now the
     *    BIN has an entry where the child LSN is less than the last full
     *    BIN version LSN.
     *  - generate a delta, make sure that the restoration of the record is
     *    present.
     */
    @Test
    public void testUndo()
        throws Throwable {

        try {
            /* Create a db, insert records value 10 - 30, get the first BIN */
            BIN bin = initDb(10, 30);

            /* Delete the first record, then abort the delete. */
            Transaction txn = env.beginTransaction(null, null);
            Cursor cursor = db.openCursor(txn, null);
            DatabaseEntry firstKey = new DatabaseEntry();
            DatabaseEntry foundData = new DatabaseEntry();
            OperationStatus status = cursor.getFirst(firstKey, foundData,
                                                     LockMode.DEFAULT);
            assertEquals(OperationStatus.SUCCESS, status);
            status = cursor.delete();
            assertEquals(OperationStatus.SUCCESS, status);
            cursor.close();

            /* Log a full version. This will reflect the delete. */
            bin.latch();
            long fullLsn = bin.log(true, false, false, false, null);
            bin.releaseLatch();
            assertTrue(fullLsn != DbLsn.NULL_LSN);

            /*
             * Roll back the deletion. Now the full version of the LSN is out
             * of date.
             */
            txn.abort();

            /*
             * Make sure a delta reflect the abort, even though the abort
             * returns an older LSN back into the BIN.
             */
            logAndCheck(bin);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            db.close();
        }
    }

    /* Check if full is logged when percent > max */
    /* Check that max deltas works. */
    /* check knownDelete. */

    /**
     * Log the targetBIN, then read it back from the log and make sure
     * the recreated BIN matches the in memory BIN.
     */
    private void logAndCheck(BIN targetBIN)
        throws IOException, DatabaseException {

        /*
         * Log it as a delta.
         */
        final long fullLsn = targetBIN.getLastFullLsn();
        final long deltaLsn;
        targetBIN.latch();
        try {
            deltaLsn = targetBIN.log(true, false, false, false, null);
        } finally {
            targetBIN.releaseLatch();
        }
        assertEquals(deltaLsn, targetBIN.getLastDeltaLsn());
        assertEquals(deltaLsn, targetBIN.getLastLoggedLsn());
        assertEquals(fullLsn, targetBIN.getLastFullLsn());
        assertFalse(targetBIN.getDirty());

        /* Read the delta back. */
        LogEntry partial =
            logManager.getLogEntry(targetBIN.getLastDeltaLsn());

        /* Make sure that this is was a delta entry. */
        assertTrue(partial instanceof BINDeltaLogEntry);
        BIN delta = ((BINDeltaLogEntry) partial).getMainItem();
        assertTrue(delta.isBINDelta(false));
        delta.setDatabase(targetBIN.getDatabase());

        /* Compare to the current version. */
        BIN createdBIN =
            delta.reconstituteBIN(targetBIN.getDatabase());
        if (DEBUG) {
            System.out.println("created");
            System.out.println(createdBIN.dumpString(0, true));
        }

        assertEquals(targetBIN.getClass().getName(),
                     createdBIN.getClass().getName());
        assertEquals(targetBIN.getNEntries(), createdBIN.getNEntries());

        for (int i = 0; i < createdBIN.getNEntries(); i++) {
            assertEquals("LSN " + i, targetBIN.getLsn(i),
                         createdBIN.getLsn(i));
        }
        assertFalse(createdBIN.getDirty());
    }
}
