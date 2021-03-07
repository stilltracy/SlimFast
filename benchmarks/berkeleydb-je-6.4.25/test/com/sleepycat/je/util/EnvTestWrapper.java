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
import java.util.HashMap;
import java.util.Map;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

/**
 * This wrapper encapsulates environment operations used while running unit
 * tests. The encapsulation permits creation of standalone or replicated
 * environments as needed behind the wrapper, so that the test does not have
 * to deal with the mechanics of environment management in each case and can
 * focus on just the test logic itself.
 *
 *  It provides the following API needed to:
 *
 * 1) create new environments
 *
 * 2) Close open environments.
 *
 * 3) Clear out the test directory used to create environments for a test
 *
 */
public abstract class EnvTestWrapper  {

    /**
     * Creates the environment to be used by the test case. If the environment
     * already exists on disk, it's reused. If not, it creates a new
     * environment and returns it.
     */
    public abstract Environment create(File envHome,
                                       EnvironmentConfig envConfig)
        throws DatabaseException;

    /**
     * Closes the environment.
     *
     * @param environment the environment to be closed.
     *
     * @throws DatabaseException
     */
    public abstract void close(Environment environment)
        throws DatabaseException;

    public abstract void closeNoCheckpoint(Environment environment)
        throws DatabaseException;

    public abstract void abnormalClose(Environment env);

    public abstract void resetNodeEqualityCheck();

    /**
     * Destroys the contents of the test directory used to hold the test
     * environments.
     *
     * @throws Exception
     */
    public abstract void destroy()
        throws Exception;

    /**
     * A wrapper for local tests.
     */
    public static class LocalEnvWrapper extends EnvTestWrapper {
        private File envDir;
        private final Map<File, Environment> dirEnvMap =
            new HashMap<File, Environment>();

        @Override
        public Environment create(File envHome,
                                  EnvironmentConfig envConfig)
            throws DatabaseException {

            this.envDir = envHome;
            Environment env = new Environment(envHome, envConfig);
            dirEnvMap.put(envHome, env);
            return env;
        }

        @Override
        public void close(Environment env)
            throws DatabaseException {
            
            env.close();
        }

        @Override
        public void resetNodeEqualityCheck() {
            throw new UnsupportedOperationException
                ("This opertaion is not supported by base environment.");
        }

        /* Provide the utility for closing without a checkpoint. */
        @Override
        public void closeNoCheckpoint(Environment env) 
            throws DatabaseException {

            DbInternal.getEnvironmentImpl(env).close(false);
        }

        @Override
        public void abnormalClose(Environment env) {
            DbInternal.getEnvironmentImpl(env).abnormalClose();
        }

        @Override
        public void destroy() {
            if (dirEnvMap == null) {
                return;
            }
            for (Environment env : dirEnvMap.values()) {
                try {
                    /* Close in case we hit an exception and didn't close */
                    env.close();
                } catch (RuntimeException e) {
                    /* OK if already closed */
                }
            }
            dirEnvMap.clear();
            if (envDir != null) {
                TestUtils.removeLogFiles("TearDown", envDir, false);
            }
            envDir = null;
        }
    }
}
