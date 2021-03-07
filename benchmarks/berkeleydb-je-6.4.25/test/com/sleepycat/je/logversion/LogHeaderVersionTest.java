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

package com.sleepycat.je.logversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Test;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.VersionMismatchException;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * Tests log file header versioning.  This test is used in conjunction with
 * MakeLogHeaderVersionData, a main program that was used once to generate two
 * log files with maximum and minimum valued header version numbers.
 *
 * @see MakeLogHeaderVersionData
 */
public class LogHeaderVersionTest extends TestBase {

    private File envHome;

    public LogHeaderVersionTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    @After
    public void tearDown() {

        envHome = null;
    }

    /**
     * Tests that an exception is thrown when a log header is read with a newer
     * version than the current version.  The maxversion.jdb log file is loaded
     * as a resource by this test and written as a regular log file.  When the
     * environment is opened, we expect a VersionMismatchException.
     */
    @Test
    public void testGreaterVersionNotAllowed()
        throws IOException {

        TestUtils.loadLog(getClass(), Utils.MAX_VERSION_NAME, envHome);

        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setAllowCreate(false);
        envConfig.setTransactional(true);

        try {
            Environment env = new Environment(envHome, envConfig);
            try {
                env.close();
            } catch (Exception ignore) {}
        } catch (VersionMismatchException e) {
            /* Got VersionMismatchException as expected. */
            return;
        }
        fail("Expected VersionMismatchException");
    }

    /**
     * Tests that when a file is opened with a lesser version than the current
     * version, a new log file is started for writing new log entries.  This is
     * important so that the new header version is written even if no new log
     * file is needed.  If the new version were not written, an older version
     * of JE would not recognize that there had been a version change.
     */
    @Test
    public void testLesserVersionNotUpdated()
        throws DatabaseException, IOException {

        TestUtils.loadLog(getClass(), Utils.MIN_VERSION_NAME, envHome);
        File logFile = new File(envHome, TestUtils.LOG_FILE_NAME);
        long origFileSize = logFile.length();

        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setAllowCreate(false);
        envConfig.setTransactional(true);

        Environment env = new Environment(envHome, envConfig);
        env.sync();
        env.close();

        assertEquals(origFileSize, logFile.length());
    }
}
