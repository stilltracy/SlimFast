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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;

import com.sleepycat.je.CacheMode;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.dbi.DatabaseImpl;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

public class INEntryTestBase extends TestBase {

    File envHome = SharedTestUtils.getTestDir();

    EnvironmentConfig envConfig;

    int nodeMaxEntries;

    short compactMaxKeyLength = 0;

    CacheMode cacheMode = CacheMode.DEFAULT;

    Environment env = null;

    protected static String DB_NAME = "TestDb";

    @Before
    public void setUp()  
        throws Exception {

        super.setUp();
        envConfig = TestUtils.initEnvConfig();
        envConfig.setAllowCreate(true);
        envConfig.setConfigParam(EnvironmentConfig.ENV_RUN_CLEANER,
                                 "false");
        envConfig.setConfigParam(EnvironmentConfig.ENV_RUN_CHECKPOINTER,
                                 "false");
        envConfig.setConfigParam(EnvironmentConfig.TREE_COMPACT_MAX_KEY_LENGTH,
                                 String.valueOf(compactMaxKeyLength));
        nodeMaxEntries = Integer.parseInt
            (envConfig.getConfigParam(EnvironmentConfig.NODE_MAX_ENTRIES));
        env = new Environment(envHome, envConfig);
    }

    @After
    public void tearDown() {
        env.close();
    }

    /* Assumes the test creates just one IN node. */
    protected void verifyINMemorySize(DatabaseImpl dbImpl) {
        BIN in = (BIN)(dbImpl.getTree().getFirstNode(cacheMode));
        in.releaseLatch();

        final IN lastNode = dbImpl.getTree().getLastNode(cacheMode);
        assertEquals(in, lastNode);
        assertTrue(in.verifyMemorySize());

        in.releaseLatch();
        TestUtils.validateNodeMemUsage(dbImpl.getEnv(), true);
    }

    protected Database createDb(String dbName,
                                int keySize,
                                int count,
                                boolean keyPrefixingEnabled) {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(false);
        dbConfig.setKeyPrefixing(keyPrefixingEnabled);

        Database db = env.openDatabase(null, dbName, dbConfig);
        final DatabaseImpl dbImpl = DbInternal.getDatabaseImpl(db);

        DatabaseEntry key = new DatabaseEntry();

        for (int i=0; i < count; i++) {
            key.setData(createByteVal(i, keySize));
            db.put(null, key, key);
            verifyINMemorySize(dbImpl);
        }
        return db;
    }

    protected Database createDb(String dbName,
                                int keySize,
                                int count) {
        return createDb(dbName, keySize, count, false);
    }

    protected Database createDupDb(String dbName,
                                   int keySize,
                                   int count) {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(true);

        Database db = env.openDatabase(null, dbName, dbConfig);
        final DatabaseImpl dbImpl = DbInternal.getDatabaseImpl(db);

        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry data = new DatabaseEntry();

        for (int i = 0; i < count; i++) {
            key.setData(new byte[0]);
            data.setData(createByteVal(i, keySize));
            db.put(null, key, data);
            verifyINMemorySize(dbImpl);
        }
        return db;
    }

    protected byte[] createByteVal(int val, int arrayLength) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(arrayLength);
        if (arrayLength >= 4) {
            byteBuffer.putInt(val);
        } else if (arrayLength >= 2) {
            byteBuffer.putShort((short) val);
        } else {
            byteBuffer.put((byte) val);
        }
        return byteBuffer.array();
    }

    /* Dummy test IN. */
    class TestIN extends IN {
        private int maxEntries;

        TestIN(int capacity) {
            maxEntries = capacity;
        }

        @Override
        protected int getCompactMaxKeyLength() {
            return compactMaxKeyLength;
        }

        @Override
        public int getMaxEntries() {
            return maxEntries;
        }
    }
}
