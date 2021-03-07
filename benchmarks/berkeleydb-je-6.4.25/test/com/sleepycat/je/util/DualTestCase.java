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

package com.sleepycat.je.util;

import java.io.File;

import org.junit.After;
import org.junit.Before;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.util.test.TestBase;

public abstract class DualTestCase extends TestBase {

    /* All environment management APIs are forwarded to this wrapper. */
    private EnvTestWrapper envWrapper;

    /* Helps determine whether setUp()and tearDown() were invoked as a pair */
    private boolean setUpInvoked = false;

    @Before
    public void setUp()
        throws Exception {

        super.setUp();
        setUpInvoked = true;
        if (DualTestCase.isReplicatedTest(getClass())) {
            try {
                /* Load the class dynamically to avoid a dependency */
                Class<?> cl =
                    Class.forName("com.sleepycat.je.rep.util.RepEnvWrapper");
                envWrapper = (EnvTestWrapper) cl.newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            envWrapper = new EnvTestWrapper.LocalEnvWrapper();
        }
    }

    @After
    public void tearDown()
        throws Exception {
        
        if (!setUpInvoked) {
            throw new IllegalStateException
                ("DualTestCase.tearDown was invoked without a corresponding " +
                 "DualTestCase.setUp() call");
        }
    }

    /**
     * Creates the environment to be used by the test case. If the environment
     * already exists on disk, it reuses it. If not, it creates a new
     * environment and returns it.
     */
    protected Environment create(File envHome,
                                 EnvironmentConfig envConfig)
        throws DatabaseException {

        return envWrapper.create(envHome, envConfig);
    }

    /**
     * Closes the environment.
     *
     * @param environment the environment to be closed.
     *
     * @throws DatabaseException
     */
    protected void close(Environment environment)
        throws DatabaseException {

        envWrapper.close(environment);
    }

    protected void resetNodeEqualityCheck() {
        envWrapper.resetNodeEqualityCheck();
    }

    /**
     * Closes the environment without a checkpoint.
     *
     * @param environment the environment to be closed.
     *
     * @throws DatabaseException
     */
    protected void closeNoCheckpoint(Environment environment)
        throws DatabaseException {

        envWrapper.closeNoCheckpoint(environment);
    }

    /**
     * Simulate a crash.
     */
    protected void abnormalClose(Environment environment)
        throws DatabaseException {

        envWrapper.abnormalClose(environment);
    }

    /**
     * Destroys the contents of the test directory used to hold the test
     * environments.
     *
     * @throws Exception
     */
    protected void destroy()
        throws Exception {

        envWrapper.destroy();
    }

    /**
     * Determines whether this test is to be run with a replicated environment.
     * If the test is in the "rep" package it assumes that the test is to be
     * run in a replicated environment.
     *
     * It's used to bypass the specifics of tests that may not be suitable for
     * replication, e.g. non-transactional mode testing.
     *
     * @param testCaseClass the test case class
     * @return true if the test uses a replicated environment, false otherwise.
     */
    public static boolean isReplicatedTest(Class<?> testCaseClass) {
        return testCaseClass.getName().contains(".rep.");
    }

    /* Returns the environment test wrapper. */
    protected EnvTestWrapper getWrapper() {
        return envWrapper;
    }
}
