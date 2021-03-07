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

package com.sleepycat.je.rep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * Make sure the unadvertised per-db replication config setting works.
 */
public class PerDbReplicationTest extends TestBase {

    private static final String TEST_DB = "testdb";
    private final File envRoot;
    private Environment env;
    private Database db;

    public PerDbReplicationTest() {
        envRoot = SharedTestUtils.getTestDir();
    }

    /**
     * A database in a replicated environment should replicate by default.
     */
    @Test
    public void testDefault() {
//        Replicator[] replicators = RepTestUtils.startGroup(envRoot,
//                                                           1,
//                                                           false /* verbose */);
//        try {
//            env = replicators[0].getEnvironment();
//            DatabaseConfig config = new DatabaseConfig();
//            config.setAllowCreate(true);
//            config.setTransactional(true);
//
//            validate(config, true /* replicated */);
//        } finally {
//            if (db != null) {
//                db.close();
//            }
//
//            for (Replicator rep: replicators) {
//                rep.close();
//            }
//        }
    }

    /**
     * Check that a database in a replicated environment which is configured to
     * not replicate is properly saved.
     * (Not a public feature yet).
     */
    @Test
    public void testNotReplicated() {
//        Replicator[] replicators = RepTestUtils.startGroup(envRoot,
//                                                           1,
//                                                           false /* verbose*/);
//        try {
//            env = replicators[0].getEnvironment();
//            DatabaseConfig config = new DatabaseConfig();
//            config.setAllowCreate(true);
//            config.setTransactional(true);
//            config.setReplicated(false);
//
//            validate(config, false /* replicated */);
//        } finally {
//            if (db != null) {
//                db.close();
//            }
//
//            for (Replicator rep: replicators) {
//                rep.close();
//            }
//        }
    }

    /**
     * A database in a standalone environment should not be replicated.
     */
    @Test
    public void testStandalone()
        throws DatabaseException {

        try {
            EnvironmentConfig envConfig = new EnvironmentConfig();
            envConfig.setAllowCreate(true);
            env = new Environment(envRoot, envConfig);
            DatabaseConfig config = new DatabaseConfig();
            config.setAllowCreate(true);

            validate(config, false /* replicated */);
        } finally {
            if (db != null) {
                db.close();
            }

            if (env != null) {
                env.close();
            }
        }
    }

    /*
     * Check that the notReplicate attribute is properly immutable and
     * persistent.
     */
    private void validate(DatabaseConfig config,
                          boolean replicated)
            throws DatabaseException {

        /* Create the database -- is its config what we expect? */
        db = env.openDatabase(null, TEST_DB, config);
        DatabaseConfig inUseConfig = db.getConfig();
        assertEquals(replicated, inUseConfig.getReplicated());

        /* Close, re-open. */
        db.close();
        db = null;
        db = env.openDatabase(null, TEST_DB, inUseConfig);
        assertEquals(replicated, db.getConfig().getReplicated());

        /*
         * Close, re-open w/inappropriate value for the replicated bit. This is
         * only checked for replicated environments.
         */
        db.close();
        db = null;
        if (DbInternal.getEnvironmentImpl(env).isReplicated()) {
            inUseConfig.setReplicated(!replicated);
            try {
                db = env.openDatabase(null, TEST_DB, inUseConfig);
                fail("Should have caught config mismatch");
            } catch (IllegalArgumentException expected) {
            }
        }
    }
}
