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

package com.sleepycat.je.rep.jmx;

import java.io.File;

import javax.management.DynamicMBean;

import org.junit.After;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;

/**
 * Test RepJEMonitor.
 */
public class RepJEMonitorTest extends com.sleepycat.je.jmx.JEMonitorTest {
    private static final boolean DEBUG = false;
    private final File envRoot;
    private RepEnvInfo[] repEnvInfo;

    public RepJEMonitorTest() {
        envRoot = SharedTestUtils.getTestDir();
    }

    @After
    public void tearDown()
        throws Exception {

        RepTestUtils.shutdownRepEnvs(repEnvInfo);
    }

    @Override
    protected DynamicMBean createMBean(Environment env) {
        return new RepJEMonitor(env);
    }

    @Override
    protected Environment openEnv(boolean openTransactionally)
        throws Exception {

        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(openTransactionally);

        repEnvInfo = RepTestUtils.setupEnvInfos(envRoot, 2, envConfig);
        ReplicatedEnvironment master = RepTestUtils.joinGroup(repEnvInfo);

        return master;
    }
}
