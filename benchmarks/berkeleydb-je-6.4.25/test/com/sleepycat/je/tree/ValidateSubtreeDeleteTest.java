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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

public class ValidateSubtreeDeleteTest extends TestBase {

    private final File envHome;
    private Environment env;
    private Database testDb;

    public ValidateSubtreeDeleteTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    @Before
    public void setUp()
        throws Exception {

        super.setUp();
        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setTransactional(true);
        envConfig.setConfigParam(EnvironmentParams.ENV_RUN_INCOMPRESSOR.getName(),
                                 "false");
        envConfig.setConfigParam(EnvironmentParams.NODE_MAX.getName(), "6");
        envConfig.setAllowCreate(true);
        env = new Environment(envHome, envConfig);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(true);
        testDb = env.openDatabase(null, "Test", dbConfig);
    }

    @After
    public void tearDown() 
        throws Exception {
        
        testDb.close();
        if (env != null) {
            try {
                env.close();
            } catch (DatabaseException E) {
            }
        }
    }

    @Test
    public void testBasic()
        throws Exception  {
        try {
            /* Make a 3 level tree full of data */
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry data = new DatabaseEntry();
            byte[] testData = new byte[1];
            testData[0] = 1;
            data.setData(testData);

            Transaction txn = env.beginTransaction(null, null);
            for (int i = 0; i < 15; i ++) {
                key.setData(TestUtils.getTestArray(i));
                testDb.put(txn, key, data);
            }

            /* Should not be able to delete any of it */
            assertFalse(DbInternal.getDatabaseImpl(testDb).getTree().validateDelete(0));
            assertFalse(DbInternal.getDatabaseImpl(testDb).getTree().validateDelete(1));

            /*
             * Should be able to delete both, the txn is aborted and the data
             * isn't there.
             */
            txn.abort();
            assertTrue(DbInternal.getDatabaseImpl(testDb).getTree().validateDelete(0));
            assertTrue(DbInternal.getDatabaseImpl(testDb).getTree().validateDelete(1));

            /*
             * Try explicit deletes.
             */
            txn = env.beginTransaction(null, null);
            for (int i = 0; i < 15; i ++) {
                key.setData(TestUtils.getTestArray(i));
                testDb.put(txn, key, data);
            }
            for (int i = 0; i < 15; i ++) {
                key.setData(TestUtils.getTestArray(i));
                testDb.delete(txn, key);
            }
            assertFalse(DbInternal.getDatabaseImpl(testDb).getTree().validateDelete(0));
            assertFalse(DbInternal.getDatabaseImpl(testDb).getTree().validateDelete(1));

            // XXX, now commit the delete and compress and test that the
            // subtree is deletable. Not finished yet! Also must test deletes.
            txn.abort();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testDuplicates()
        throws Exception  {
        try {
            /* Make a 3 level tree full of data */
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry data = new DatabaseEntry();
            byte[] testData = new byte[1];
            testData[0] = 1;
            key.setData(testData);

            Transaction txn = env.beginTransaction(null, null);
            for (int i = 0; i < 4; i ++) {
                data.setData(TestUtils.getTestArray(i));
                testDb.put(txn, key, data);
            }

            /* Should not be able to delete any of it */
            Tree tree = DbInternal.getDatabaseImpl(testDb).getTree();
            assertFalse(tree.validateDelete(0));

            /*
             * Should be able to delete, the txn is aborted and the data
             * isn't there.
             */
            txn.abort();
            assertTrue(tree.validateDelete(0));

            /*
             * Try explicit deletes.
             */
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
