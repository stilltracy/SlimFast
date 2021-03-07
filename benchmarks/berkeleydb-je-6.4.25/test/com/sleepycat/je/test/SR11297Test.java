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

package com.sleepycat.je.test;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * Fix for SR11297.  When the first BIN in database was empty,
 * CursorImpl.positionFirstOrLast(true, null) was returning false, causing
 * Cursor.getFirst to return NOTFOUND.  This test reproduces that problem by
 * creating a database with the first BIN empty and the second BIN non-empty.
 *
 * <p>A specific sequence where partial compression takes place is necessary to
 * reproduce the problem.  A duplicate is added as the first entry in the first
 * BIN, then that BIN is filled and one entry is added to the next BIN.  Then
 * all records in the first BIN are deleted.  compress() is called once, which
 * deletes the duplicate tree and all entries in the first BIN, but the first
 * BIN will not be deleted until the next compression.  At that point in time,
 * getFirst failed to find the record in the second BIN.</p>
 */
public class SR11297Test extends TestBase {

    /* Minimum child entries per BIN. */
    private static int N_ENTRIES = 4;

    private static CheckpointConfig forceCheckpoint = new CheckpointConfig();
    static {
        forceCheckpoint.setForce(true);
    }

    private File envHome;
    private Environment env;

    public SR11297Test() {
        envHome = SharedTestUtils.getTestDir();
    }

    @After
    public void tearDown() {
        try {
            if (env != null) {
                env.close();
            }
        } catch (Throwable e) {
            System.out.println("tearDown: " + e);
        }

        envHome = null;
        env = null;
    }

    private void openEnv()
        throws DatabaseException {

        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        DbInternal.disableParameterValidation(envConfig);
        envConfig.setAllowCreate(true);
        /* Make as small a log as possible to save space in CVS. */
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_INCOMPRESSOR.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_CLEANER.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_EVICTOR.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_CHECKPOINTER.getName(), "false");
        /* Use a 100 MB log file size to ensure only one file is written. */
        envConfig.setConfigParam(EnvironmentParams.LOG_FILE_MAX.getName(),
                                 Integer.toString(100 * (1 << 20)));
        /* Force BIN-delta. */
        envConfig.setConfigParam
            (EnvironmentParams.BIN_DELTA_PERCENT.getName(),
             Integer.toString(75));
        /* Force INDelete. */
        envConfig.setConfigParam
            (EnvironmentParams.NODE_MAX.getName(),
             Integer.toString(N_ENTRIES));
        env = new Environment(envHome, envConfig);
    }

    private void closeEnv()
        throws DatabaseException {

        env.close();
        env = null;
    }

    @Test
    public void test11297()
        throws DatabaseException {

        openEnv();

        /* Write db0 and db1. */
        for (int i = 0; i < 2; i += 1) {
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setAllowCreate(true);
            dbConfig.setSortedDuplicates(true);
            Database db = env.openDatabase(null, "db" + i, dbConfig);

            /* Write: {0, 0}, {0, 1}, {1, 0}, {2, 0}, {3, 0} */
            for (int j = 0; j < N_ENTRIES; j += 1) {
                db.put(null, entry(j), entry(0));
            }
            db.put(null, entry(0), entry(1));

            /* Delete everything but the last record. */
            for (int j = 0; j < N_ENTRIES - 1; j += 1) {
                db.delete(null, entry(j));
            }

            db.close();
        }

        checkFirstRecord();
        env.compress();
        checkFirstRecord();

        closeEnv();
    }

    /**
     * First and only record in db1 should be {3,0}.
     */
    private void checkFirstRecord()
        throws DatabaseException {

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(false);
        dbConfig.setReadOnly(true);
        dbConfig.setSortedDuplicates(true);
        Database db = env.openDatabase(null, "db1", dbConfig);
        Cursor cursor = db.openCursor(null, null);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry data = new DatabaseEntry();
        OperationStatus status = cursor.getFirst(key, data, null);
        assertEquals(OperationStatus.SUCCESS, status);
        assertEquals(3, value(key));
        assertEquals(0, value(data));
        cursor.close();
        db.close();
    }

    static DatabaseEntry entry(int val) {

        byte[] data = new byte[] { (byte) val };
        return new DatabaseEntry(data);
    }

    static int value(DatabaseEntry entry) {

        byte[] data = entry.getData();
        if (data.length != 1) {
            throw new IllegalStateException("len=" + data.length);
        }
        return data[0];
    }
}
