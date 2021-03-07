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

import java.io.File;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.util.TestUtils;

/**
 * This standalone command line program creates a single 00000000.jdb log file.
 * It was used to generate maxversion.jdb and minversion.jdb, and although it
 * may never need to be used again, below are instructions.
 *
 * <p>Before running this program change LogEntryType.LOG_VERSION to
 * Integer.MAX_VALUE or one temporarily, just for creating a file with the
 * maximum or minimum version number.  A single command line argument is
 * required for the home directory.  After running this program rename the
 * 00000000.jdb file to maxversion.jdb or minversion.jdb file in the directory
 * of this source package.  When adding it to CVS make sure to use -kb since it
 * is a binary file.  Don't forget to change LogEntryType.LOG_VERSION back to
 * the correct value.</p>
 *
 * @see LogHeaderVersionTest
 */
public class MakeLogHeaderVersionData {

    private MakeLogHeaderVersionData() {
    }

    public static void main(String[] args)
        throws Exception {

        if (args.length != 1) {
            throw new Exception("Home directory arg is required.");
        }

        File homeDir = new File(args[0]);
        File logFile = new File(homeDir, TestUtils.LOG_FILE_NAME);

        if (logFile.exists()) {
            throw new Exception("Home directory must be empty of log files.");
        }

        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        /* Make as small a log as possible to save space in CVS. */
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_INCOMPRESSOR.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_CLEANER.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_EVICTOR.getName(), "false");
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_CHECKPOINTER.getName(), "false");

        Environment env = new Environment(homeDir, envConfig);
        env.close();

        if (!logFile.exists()) {
            throw new Exception("Home directory does not contain: " + logFile);
        }

        System.out.println("Sucessfully created: " + logFile);
    }
}
