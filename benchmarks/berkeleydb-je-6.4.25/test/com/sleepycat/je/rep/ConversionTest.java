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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;
import com.sleepycat.je.utilint.CmdUtil;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * Standalone environments can be converted once to be replicated environments.
 * Replicated environments can't be opened in standalone mode.
 */
public class ConversionTest extends TestBase {

    private final File envRoot;

    public ConversionTest() {
        envRoot = SharedTestUtils.getTestDir();
    }

    /**
     * Check that an environment which is opened for replication cannot be
     * re-opened as a standalone environment in r/w mode
     */
    @Test
    public void testNoStandaloneReopen()
        throws DatabaseException, IOException {

        RepEnvInfo[] repEnvInfo = initialOpenWithReplication();

        /* Try to re-open standalone r/w, should fail. */
        try {
            EnvironmentConfig reopenConfig = new EnvironmentConfig();
            reopenConfig.setTransactional(true);
            @SuppressWarnings("unused")
            Environment unused = new Environment(repEnvInfo[0].getEnvHome(),
                                                 reopenConfig);
            fail("Should have thrown an exception.");
        } catch (UnsupportedOperationException ignore) {
            /* throw a more specific exception? */
        }
    }

    /**
     * Check that an environment which is opened for replication can
     * also be opened as a standalone r/o environment.
     */
    @Test
    public void testStandaloneRO()
        throws DatabaseException, IOException {

        RepEnvInfo[] repEnvInfo = initialOpenWithReplication();

        /* Try to re-open standalone r/o, should succeed */
        try {
            EnvironmentConfig reopenConfig = new EnvironmentConfig();
            reopenConfig.setTransactional(true);
            reopenConfig.setReadOnly(true);
            Environment env = new Environment(repEnvInfo[0].getEnvHome(),
                                              reopenConfig);
            env.close();
        } catch (DatabaseException e) {
            fail("Should be successful" + e);
        }
    }

    @Test
    public void testStandaloneUtility()
        throws DatabaseException, IOException {

        RepEnvInfo[] repEnvInfo = initialOpenWithReplication();

        /* Try to re-open as a read/only utility, should succeed */
        try {
            EnvironmentConfig reopenConfig = new EnvironmentConfig();
            reopenConfig.setTransactional(true);
            reopenConfig.setReadOnly(true);
            EnvironmentImpl envImpl =
                CmdUtil.makeUtilityEnvironment(repEnvInfo[0].getEnvHome(),
                                           true /* readOnly */);
            envImpl.close();
        } catch (DatabaseException e) {
            fail("Should be successful" + e);
        }
    }

    private RepEnvInfo[] initialOpenWithReplication()
        throws DatabaseException, IOException {
     
        RepEnvInfo[] repEnvInfo = RepTestUtils.setupEnvInfos(envRoot, 2);
        RepTestUtils.joinGroup(repEnvInfo);
        for (RepEnvInfo repi : repEnvInfo) {
            repi.getEnv().close();
        }
        return repEnvInfo;
    }
}
