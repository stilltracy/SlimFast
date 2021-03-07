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

package com.sleepycat.je.cleaner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import com.sleepycat.bind.tuple.IntegerBinding;
import org.junit.After;
import org.junit.Test;

import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentStats;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * Checks that the cleaner wakes up at certain times even when there is no
 * logging:
 *  - startup
 *  - change to minUtilization
 *  - DB remove/truncate
 */
public class WakeupTest extends TestBase {

    private static final int FILE_SIZE = 1000000;
    private static final String DB_NAME = "WakeupTest";

    private final File envHome;
    private Environment env;
    private Database db;
    
    public WakeupTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    private void open(final boolean runCleaner) {
        final EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setAllowCreate(true);
        envConfig.setConfigParam(
            EnvironmentConfig.LOG_FILE_MAX, Integer.toString(FILE_SIZE));
        envConfig.setConfigParam(
            EnvironmentConfig.CLEANER_MIN_UTILIZATION, "50");
        envConfig.setConfigParam(
            EnvironmentConfig.CLEANER_MIN_FILE_UTILIZATION, "0");
        envConfig.setConfigParam(
            EnvironmentConfig.ENV_RUN_CLEANER, runCleaner ? "true" : "false");
        envConfig.setConfigParam(
            EnvironmentConfig.ENV_RUN_CHECKPOINTER, "false");
        env = new Environment(envHome, envConfig);

        final DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        db = env.openDatabase(null, DB_NAME, dbConfig);
    }

    private void close() {
        if (db != null) {
            db.close();
            db = null;
        }
        if (env != null) {
            env.close();
            env = null;
        }
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (env != null) {
            try {
                env.close();
            } finally {
                env = null;
            }
        }
    }

    @Test
    public void testCleanAtStartup() {
        open(false /*runCleaner*/);
        writeFiles(0 /*nActive*/, 10 /*nObsolete*/);
        close();
        open(true /*runCleaner*/);
        expectBackgroundCleaning();
        close();
    }

    @Test
    public void testCleanAfterMinUtilizationChange() {
        open(true /*runCleaner*/);
        writeFiles(4 /*nActive*/, 3 /*nObsolete*/);
        expectNothingToClean();
        final EnvironmentConfig envConfig = env.getConfig();
        envConfig.setConfigParam(
            EnvironmentConfig.CLEANER_MIN_UTILIZATION, "90");
        env.setMutableConfig(envConfig);
        expectBackgroundCleaning();
        close();
    }

    @Test
    public void testCleanAfterDbRemoval() {
        open(true /*runCleaner*/);
        writeFiles(5 /*nActive*/, 0 /*nObsolete*/);
        expectNothingToClean();
        db.close();
        db = null;
        env.removeDatabase(null, DB_NAME);
        expectBackgroundCleaning();
        close();
    }

    @Test
    public void testCleanAfterDbTruncate() {
        open(true /*runCleaner*/);
        writeFiles(5 /*nActive*/, 0 /*nObsolete*/);
        expectNothingToClean();
        db.close();
        db = null;
        env.truncateDatabase(null, DB_NAME, false);
        expectBackgroundCleaning();
        close();
    }

    private void expectNothingToClean() {
        env.cleanLog();
        final EnvironmentStats stats = env.getStats(null);
        final String msg = String.format("%d probes, %d non-probes",
            stats.getNCleanerProbeRuns(), stats.getNCleanerRuns());
        assertEquals(msg, 0,
            stats.getNCleanerRuns() - stats.getNCleanerProbeRuns());
    }

    private void expectBackgroundCleaning() {
        final long endTime = System.currentTimeMillis() + (30 * 1000);
        while (System.currentTimeMillis() < endTime) {
            final EnvironmentStats stats = env.getStats(null);
            if (stats.getNCleanerRuns() > 0) {
                return;
            }
        }
        close();
        fail("Cleaner did not run");
    }

    private void writeFiles(final int nActive, final int nObsolete) {
        int key = 0;
        final DatabaseEntry keyEntry = new DatabaseEntry();
        final DatabaseEntry dataEntry = new DatabaseEntry(new byte[FILE_SIZE]);
        for (int i = 0; i < nActive; i += 1) {
            IntegerBinding.intToEntry(key, keyEntry);
            db.put(null, keyEntry, dataEntry);
            key += 1;
        }
        IntegerBinding.intToEntry(key, keyEntry);
        for (int i = 0; i <= nObsolete; i += 1) {
            db.put(null, keyEntry, dataEntry);
        }
        env.checkpoint(new CheckpointConfig().setForce(true));
    }
}
