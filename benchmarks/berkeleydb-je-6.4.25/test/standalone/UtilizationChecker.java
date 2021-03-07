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

import java.io.File;

import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;

/**
 * Run RepTestUtils.checkUtilizationProfile on a rep group.
 */
public class UtilizationChecker {

    private RepEnvInfo[] repEnvInfo;

    /* Environment home root for whole replication group. */
    private File envRoot;
    private int nNodes = 5;
    private int subDir = 0;

    public static void main(String args[]) {
        try {
            UtilizationChecker utilizationChecker = new UtilizationChecker();
            utilizationChecker.parseArgs(args);
            utilizationChecker.setup();
            utilizationChecker.check();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /**
     * Grow the data store to the appropriate size for the steady state
     * portion of the test.
     */
    private void setup()
        throws Exception {

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setConfigParam(EnvironmentConfig.ENV_RUN_CLEANER, "false");
        if (subDir > 0) {
            envConfig.setConfigParam
                (EnvironmentConfig.LOG_N_DATA_DIRECTORIES, subDir + "");
        }

        /*
         * We have a lot of environments open in a single process, so reduce
         * the cache size lest we run out of file descriptors.
         */
        envConfig.setConfigParam("je.log.fileCacheSize", "30");

        repEnvInfo = RepTestUtils.setupEnvInfos(envRoot, nNodes, envConfig);
    }

    private void check() {

        System.out.println("Check starting");

        RepTestUtils.restartGroup(repEnvInfo);
	RepTestUtils.checkUtilizationProfile(System.out, repEnvInfo);
        System.out.println("Check finishing");
        for (RepEnvInfo info : repEnvInfo) {
            info.closeEnv();
        }
    }

    private void parseArgs(String args[])
        throws Exception {

        for (int i = 0; i < args.length; i++) {
            boolean moreArgs = i < args.length - 1;
            if (args[i].equals("-h") && moreArgs) {
                envRoot = new File(args[++i]);
            } else if (args[i].equals("-repGroupSize") && moreArgs) {
                nNodes = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-subDir") && moreArgs) {
                subDir = Integer.parseInt(args[++i]);
            } else {
                usage("Unknown arg: " + args[i]);
            }
        }
    }

    private void usage(String error) {
        if (error != null) {
            System.err.println(error);
        }

        System.err.println
            ("java " + getClass().getName() + "\n" +
             "     [-h <replication group Environment home dir>]\n" +
             "     [-repGroupSize <replication group size>]\n" +
             "     [-subDir <num directories to use with je.log.nDataDirectories");
        System.exit(2);
    }
}
