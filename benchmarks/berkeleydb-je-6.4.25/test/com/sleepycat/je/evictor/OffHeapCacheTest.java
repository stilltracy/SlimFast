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

package com.sleepycat.je.evictor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentStats;
import com.sleepycat.je.tree.BIN;
import com.sleepycat.je.tree.IN;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;
import org.junit.After;
import org.junit.Test;

public class OffHeapCacheTest extends TestBase {

    private File envHome;
    private Environment env;
    private Database db;
    private OffHeapCache ohCache;
    private OffHeapAllocator allocator;

    public OffHeapCacheTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    @After
    public void tearDown() {

        try {
            if (env != null) {
                env.close();
            }
        } finally {
            env = null;
            db = null;
            ohCache = null;
            allocator = null;
        }

        TestUtils.removeLogFiles("TearDown", envHome, false);
    }

    private void open() {

        final EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setOffHeapCacheSize(1024 * 1024);

        envConfig.setConfigParam(
            EnvironmentConfig.ENV_RUN_CLEANER, "false");
        envConfig.setConfigParam(
            EnvironmentConfig.ENV_RUN_CHECKPOINTER, "false");
        envConfig.setConfigParam(
            EnvironmentConfig.ENV_RUN_IN_COMPRESSOR, "false");
        envConfig.setConfigParam(
            EnvironmentConfig.ENV_RUN_EVICTOR, "false");

        env = new Environment(envHome, envConfig);

        final DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);

        db = env.openDatabase(null, "foo", dbConfig);

        ohCache =
            DbInternal.getEnvironmentImpl(env).getOffHeapCache();

        allocator = ohCache.getAllocator();
    }

    private void close() {
        db.close();
        env.close();
    }

    @Test
    public void testBINSerialization() throws Exception {

        open();

        final BIN bin = new BIN(
            DbInternal.getDatabaseImpl(db),
            new byte[] { 1, 2, 3 },
            128, IN.BIN_LEVEL);

        /* Avoid assertions setting LN memIds. */
        bin.setOffHeapLruId(1);

        bin.latch();
        try {
            checkBINSerialization(bin);
            checkBINSerialization(bin, 0);
            checkBINSerialization(bin, 100);
            checkBINSerialization(bin, 0, 0);
            checkBINSerialization(bin, 100, 0);
            checkBINSerialization(bin, 0, 101);
            checkBINSerialization(bin, 100, 101);
            checkBINSerialization(bin, 0, 0, 0);
            checkBINSerialization(bin, 0, 0, 102);
            checkBINSerialization(bin, 0, 101, 0);
            checkBINSerialization(bin, 0, 101, 102);
            checkBINSerialization(bin, 100, 0, 0);
            checkBINSerialization(bin, 100, 0, 102);
            checkBINSerialization(bin, 100, 101, 0);
            checkBINSerialization(bin, 100, 101, 102);
            checkBINSerialization(bin, 0, 0, 0, 0);
            checkBINSerialization(bin, 0, 0, 0, 103);
            checkBINSerialization(bin, 0, 0, 102, 0);
            checkBINSerialization(bin, 0, 0, 102, 103);
            checkBINSerialization(bin, 100, 101, 0, 0);
            checkBINSerialization(bin, 100, 101, 0, 103);
            checkBINSerialization(bin, 100, 101, 102, 0);
            checkBINSerialization(bin, 100, 101, 102, 103);
        } finally {
            bin.releaseLatch();
        }

        close();
    }

    private void checkBINSerialization(BIN bin, int... memIds) {

        assertTrue(memIds.length >= bin.getNEntries());

        for (int i = 0; i < memIds.length; i += 1) {

            if (i >= bin.getNEntries()) {
                assertTrue(bin.insertEntry(null, new byte[] {(byte) i}, 0));
            }

            bin.setOffHeapLNId(i, memIds[i]);
        }

        final long memId = ohCache.serializeBIN(bin, bin.isBINDelta());
        final byte[] bytes = new byte[allocator.size(memId)];
        allocator.copy(memId, 0, bytes, 0, bytes.length);
        allocator.free(memId);

        final BIN bin2 = ohCache.materializeBIN(bin.getEnv(), bytes);
        bin2.setDatabase(DbInternal.getDatabaseImpl(db));

        /* Avoid assertions setting LN memIds. */
        bin2.setOffHeapLruId(1);

        bin2.latch();
        try {
            assertEquals(bin.isBINDelta(), bin2.isBINDelta());
            assertEquals(bin.getNEntries(), bin2.getNEntries());

            for (int i = 0; i < bin.getNEntries(); i += 1) {
                assertEquals(bin.getOffHeapLNId(i), bin2.getOffHeapLNId(i));
            }

            /*
             * We don't bother to check all BIN fields, since writeToLog and
             * readFromLog are used for serialization and tested in many places.
             */
        } finally {
            bin2.releaseLatch();
        }
    }

    /**
     * Makes a call to each getter to make sure it doesn't throw an exception.
     */
    @Test
    public void testStatGetters() throws Exception {

        open();

        final EnvironmentStats stats = env.getStats(null);

        stats.getOffHeapAllocFailures();
        stats.getOffHeapAllocOverflows();
        stats.getOffHeapThreadUnavailable();
        stats.getOffHeapNodesTargeted();
        stats.getOffHeapNodesEvicted();
        stats.getOffHeapDirtyNodesEvicted();
        stats.getOffHeapNodesStripped();
        stats.getOffHeapNodesMutated();
        stats.getOffHeapNodesSkipped();
        stats.getOffHeapLNsEvicted();
        stats.getOffHeapLNsLoaded();
        stats.getOffHeapLNsStored();
        stats.getOffHeapBINsLoaded();
        stats.getOffHeapBINsStored();
        stats.getOffHeapCachedLNs();
        stats.getOffHeapCachedBINs();
        stats.getOffHeapCachedBINDeltas();
        stats.getOffHeapTotalBytes();
        stats.getOffHeapTotalBlocks();
        stats.getOffHeapLRUSize();

        close();
    }
}
