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

import java.io.File;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;

/**
 * @see ReadOnlyLockingTest
 */
public class ReadOnlyProcess {

    public static void main(String[] args) {

        /*
         * Don't write to System.out in this process because the parent
         * process only reads System.err.
         */
        try {
            EnvironmentConfig envConfig = TestUtils.initEnvConfig();
            envConfig.setTransactional(true);
            envConfig.setReadOnly(true);
            if (args[0].equals("true")) {
                envConfig.setConfigParam
                    (EnvironmentConfig.LOG_N_DATA_DIRECTORIES, args[1]);
            }

            File envHome = SharedTestUtils.getTestDir();
            Environment env = new Environment(envHome, envConfig);

            //System.err.println("Opened read-only: " + envHome);
            //System.err.println(System.getProperty("java.class.path"));

            /* Notify the test that this process has opened the environment. */
            ReadOnlyLockingTest.createProcessFile();

            /* Sleep until the parent process kills me. */
            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {

            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
