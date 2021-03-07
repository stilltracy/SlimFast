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
package com.sleepycat.je.recovery;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.log.Trace;
import com.sleepycat.je.recovery.stepwise.TestData;
import com.sleepycat.je.util.TestUtils;

/**
 * The split aunt problem [#14424] is described in LevelRecorder.
 * Also see [#23990] and [#24663].
 */
public class CheckSplitAuntTest extends CheckBase {

    private static final String DB_NAME = "simpleDB";

    @Test
    public void testSplitAunt()
        throws Throwable {

        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        turnOffEnvDaemons(envConfig);
        envConfig.setConfigParam(EnvironmentParams.NODE_MAX.getName(),
                                 "4");
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(true);

        EnvironmentConfig restartConfig = TestUtils.initEnvConfig();
        turnOffEnvDaemons(envConfig);
        envConfig.setConfigParam(EnvironmentParams.NODE_MAX.getName(),
                                 "4");
        envConfig.setTransactional(true);

        testOneCase(DB_NAME,
                    envConfig,
                    dbConfig,
                    new TestGenerator(true){
                        void generateData(Database db)
                            throws DatabaseException {
                            setupSplitData(db);
                        }
                    },
                    restartConfig,
                    new DatabaseConfig());

        /*
         * Now run the test in a stepwise loop, truncate after each
         * log entry. We start the steps before the inserts, so the base
         * expected set is empty.
         */
        HashSet<TestData> currentExpected = new HashSet<TestData>();
        stepwiseLoop(DB_NAME, envConfig, dbConfig, currentExpected,  0);
    }

    private void setupSplitData(Database db)
        throws DatabaseException {

        setStepwiseStart();

        int max = 26;

        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry data = new DatabaseEntry();

        /* Populate a tree so it grows to 4 levels, then checkpoint. */
        for (int i = 0; i < max; i ++) {
            IntegerBinding.intToEntry(i*10, key);
            IntegerBinding.intToEntry(i*10, data);
            assertEquals(OperationStatus.SUCCESS, db.put(null, key, data));
        }

        CheckpointConfig ckptConfig = new CheckpointConfig();
        Trace.trace(DbInternal.getEnvironmentImpl(env), "First sync");
        env.sync();

        Trace.trace(DbInternal.getEnvironmentImpl(env), "Second sync");
        env.sync();

        Trace.trace(DbInternal.getEnvironmentImpl(env), "Third sync");
        env.sync();

        Trace.trace(DbInternal.getEnvironmentImpl(env), "Fourth sync");
        env.sync();

        Trace.trace(DbInternal.getEnvironmentImpl(env), "Fifth sync");
        env.sync();

        Trace.trace(DbInternal.getEnvironmentImpl(env), "Sync6");
        env.sync();

        Trace.trace(DbInternal.getEnvironmentImpl(env), "After sync");

        /*
         * Add a key to dirty the left hand branch. 4 levels are needed to
         * create the problem scenario, because the single key added here,
         * followed by a checkpoint, will always cause at least 2 levels to be
         * logged -- that's the smallest maxFlushLevel for any checkpoint. And
         * we must not dirty the root, so 3 levels is not enough.
         */
        IntegerBinding.intToEntry(5, key);
        IntegerBinding.intToEntry(5, data);
        assertEquals(OperationStatus.SUCCESS, db.put(null, key, data));
        Trace.trace
            (DbInternal.getEnvironmentImpl(env), "After single key insert");

        /*
         * A normal checkpoint should log the BIN and its parent IN, but no
         * higher than level 2. The level 3 parent will be left dirty, but
         * level 4 (the root) will not be dirtied.
         */
        ckptConfig.setForce(true);
        env.checkpoint(ckptConfig);

        Trace.trace(DbInternal.getEnvironmentImpl(env), "before split");

        /* Add enough keys to split the right hand branch. */
        for (int i = max*10; i < max*10 + 7; i ++) {
            IntegerBinding.intToEntry(i, key);
            IntegerBinding.intToEntry(i, data);
            assertEquals(OperationStatus.SUCCESS, db.put(null, key, data));
        }

        Trace.trace(DbInternal.getEnvironmentImpl(env), "after split");
    }
}
