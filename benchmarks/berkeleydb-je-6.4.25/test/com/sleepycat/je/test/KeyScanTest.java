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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentStats;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.PreloadConfig;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

public class KeyScanTest extends TestBase {

    private File envHome;
    private Environment env;

    public KeyScanTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    @After
    public void tearDown() {
        try {
            closeEnv();
        } catch (Throwable e) {
            System.out.println("tearDown: " + e);
        }

        envHome = null;
        env = null;
    }

    private void openEnv()
        throws DatabaseException {

        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setAllowCreate(true);
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_INCOMPRESSOR.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_CLEANER.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_EVICTOR.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_CHECKPOINTER.getName(), "false");
        env = new Environment(envHome, envConfig);
    }

    private void closeEnv()
        throws DatabaseException {

        if (env != null) {
            env.close();
            env = null;
        }
    }

    @Test
    public void testKeyScan() {
        doKeyScan(false /*dups*/);
    }

    @Test
    public void testKeyScanDup() {
        doKeyScan(true /*dups*/);
    }

    private void doKeyScan(final boolean dups) {
        final DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(dups);
        final DatabaseEntry key = new DatabaseEntry();
        final DatabaseEntry data = new DatabaseEntry();
        final int RECORD_COUNT = 3 * 500;
        OperationStatus status;

        /* Open env, write data, close. */
        openEnv();
        Database db = env.openDatabase(null, "foo", dbConfig);
        for (int i = 0; i < RECORD_COUNT; i += 1) {
            IntegerBinding.intToEntry(i, key);
            IntegerBinding.intToEntry(1, data);
            status = db.putNoOverwrite(null, key, data);
            assertSame(OperationStatus.SUCCESS, status);
            if (dups && ((i % 2) == 1)) {
                IntegerBinding.intToEntry(2, data);
                status = db.putNoDupData(null, key, data);
                assertSame(OperationStatus.SUCCESS, status);
            }
        }
        db.close();
        closeEnv();

        /* Open env, preload without loading LNs. */
        openEnv();
        dbConfig.setAllowCreate(false);
        db = env.openDatabase(null, "foo", dbConfig);
        db.preload(new PreloadConfig());

        /* Clear stats. */
        final StatsConfig statsConfig = new StatsConfig();
        statsConfig.setClear(true);
        EnvironmentStats stats = env.getStats(statsConfig);

        /* Key scan with dirty read. */
        for (int variant = 0; variant < 2; variant += 1) {
            LockMode lockMode = null;
            CursorConfig cursorConfig = null;
            switch (variant) {
                case 0:
                    lockMode = LockMode.READ_UNCOMMITTED;
                    break;
                case 1:
                    cursorConfig = CursorConfig.READ_UNCOMMITTED;
                    break;
                default:
                    fail();
            }
            data.setPartial(0, 0, true);
            Cursor c = db.openCursor(null, cursorConfig);
            int count = 0;
            int expectKey = 0;
            if (dups) {
                while (c.getNextNoDup(key, data, lockMode) ==
                       OperationStatus.SUCCESS) {
                    assertEquals(count, IntegerBinding.entryToInt(key));
                    count += 1;
                }
            } else {
                while (c.getNext(key, data, lockMode) ==
                       OperationStatus.SUCCESS) {
                    assertEquals(count, IntegerBinding.entryToInt(key));
                    count += 1;
                }
            }
            assertEquals(RECORD_COUNT, count);

            /* Try other misc operations. */
            status = c.getFirst(key, data, lockMode);
            assertSame(OperationStatus.SUCCESS, status);
            assertEquals(0, IntegerBinding.entryToInt(key));

            status = c.getLast(key, data, lockMode);
            assertSame(OperationStatus.SUCCESS, status);
            assertEquals(RECORD_COUNT - 1, IntegerBinding.entryToInt(key));

            IntegerBinding.intToEntry(RECORD_COUNT / 2, key);
            status = c.getSearchKey(key, data, lockMode);
            assertSame(OperationStatus.SUCCESS, status);
            assertEquals(RECORD_COUNT / 2, IntegerBinding.entryToInt(key));

            IntegerBinding.intToEntry(RECORD_COUNT / 2, key);
            status = c.getSearchKeyRange(key, data, lockMode);
            assertSame(OperationStatus.SUCCESS, status);
            assertEquals(RECORD_COUNT / 2, IntegerBinding.entryToInt(key));

            c.close();

            /* Expect no cache misses. */
            stats = env.getStats(statsConfig);
            assertEquals(0, stats.getNCacheMiss());
            assertEquals(0, stats.getNNotResident());
        }

        db.close();
        closeEnv();
    }
}
