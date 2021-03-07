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

package com.sleepycat.je.dbi;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.je.utilint.LoggerUtils;
import com.sleepycat.util.test.TestBase;

/**
 * Basic verification of environment startup tracking. Verification is really
 * via inspection.
 */
public class StartupTrackerTest extends TestBase {

    private Environment env;
    private final File envHome;

    public StartupTrackerTest() {
        envHome = new File(System.getProperty(TestUtils.DEST_DIR));
    }

    @Before
    public void setUp()
        throws Exception {

        TestUtils.removeLogFiles("Setup", envHome, false);
        super.setUp();
    }

    @After
    public void tearDown() {

        /*
         * Close down environments in case the unit test failed so that the log
         * files can be removed.
         */
        try {
            if (env != null) {
                env.close();
                env = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // TestUtils.removeLogFiles("TearDown", envHome, false);
    }

    /*
     */
    @Test
    public void testEnvRecovery() {

        Logger logger = LoggerUtils.getLoggerFixedPrefix(this.getClass(),
                                                         "test");
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream p = new PrintStream(baos);

            EnvironmentConfig envConfig = TestUtils.initEnvConfig();
            envConfig.setAllowCreate(true);
            envConfig.setConfigParam("je.env.startupThreshold", "0");
            env = new Environment(envHome, envConfig);
            env.printStartupInfo(p);

            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setAllowCreate(true);
            Database db = env.openDatabase(null, "foo", dbConfig);

            DatabaseEntry key = new DatabaseEntry(new byte[1000]);
            DatabaseEntry data = new DatabaseEntry(new byte[1000]);
            for (int i = 0; i < 10; i += 1) {
                db.put(null, key, data);
            }
            db.close();
            env.close();

            env = new Environment(envHome, envConfig);
            env.printStartupInfo(p);
            logger.fine(baos.toString());
            env.close();
            env = null;
        } catch (Exception e) {
            fail("This test succeeds as long as the printing of the report " +
                 "does not cause a problem. Any exception is a failure. " );
        }
    }
}

