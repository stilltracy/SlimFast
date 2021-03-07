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

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * Test out-of-memory fix to DaemonThread [#10504].
 */
public class MultiEnvOpenCloseTest extends TestBase {

    private File envHome;

    @Before
    public void setUp() 
        throws Exception {

        envHome = SharedTestUtils.getTestDir();
        super.setUp();
    }

    @Test
    public void testMultiOpenClose()
        throws Exception {

        /*
         * Before fixing the bug in DaemonThread [#10504] this test would run
         * out of memory after 7 iterations.  The bug was, if we open an
         * environment read-only we won't start certain daemon threads, they
         * will not be GC'ed because they are part of a thread group, and they
         * will retain a reference to the Environment.  The fix was to not
         * create the threads until we need to start them.
         */
        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setAllowCreate(true);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);

        final int DATA_SIZE = 1024 * 10;
        final int N_RECORDS = 1000;
        final int N_ITERS = 30;

        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry data = new DatabaseEntry(new byte[DATA_SIZE]);

        Environment env = new Environment(envHome, envConfig);
        Database db = env.openDatabase(null, "MultiEnvOpenCloseTest",
                                       dbConfig);
        for (int i = 0; i < N_RECORDS; i += 1) {
            IntegerBinding.intToEntry(i, key);
            db.put(null, key, data);
        }

        db.close();
        env.close();

        envConfig.setAllowCreate(false);
        envConfig.setReadOnly(true);
        dbConfig.setAllowCreate(false);
        dbConfig.setReadOnly(true);

        for (int i = 1; i <= N_ITERS; i += 1) {
            //System.out.println("MultiEnvOpenCloseTest iteration # " + i);
            env = new Environment(envHome, envConfig);
            db = env.openDatabase(null, "MultiEnvOpenCloseTest", dbConfig);
            for (int j = 0; j < N_RECORDS; j += 1) {
                IntegerBinding.intToEntry(j, key);
                db.get(null, key, data, null);
            }
            db.close();
            env.close();
        }
    }
}
