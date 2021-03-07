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

package com.sleepycat.je.rep.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.rep.RepInternal;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;
import com.sleepycat.je.util.EnvTestWrapper;
import com.sleepycat.je.utilint.VLSN;

/**
 * An environment wrapper for replicated tests.
 */
public class RepEnvWrapper extends EnvTestWrapper {

    // TODO: a standard way to parameterize all replication tests.
    final int repNodes = 3;

    private boolean doNodeEqualityCheck = true;

    private final Map<File, RepEnvInfo[]> dirRepEnvInfoMap =
        new HashMap<File, RepEnvInfo[]>();

    @Override
    public Environment create(File envRootDir, EnvironmentConfig envConfig)
        throws DatabaseException {

        adjustEnvConfig(envConfig);

        RepEnvInfo[] repEnvInfo = dirRepEnvInfoMap.get(envRootDir);
        Environment env = null;
        if (repEnvInfo != null) {
            /* An existing environment */
            try {
                repEnvInfo = RepTestUtils.setupEnvInfos
                    (envRootDir, repNodes, envConfig);
            } catch (Exception e1) {
                TestCase.fail(e1.getMessage());
            }
            env = restartGroup(repEnvInfo);
        } else {
            /* Eliminate detritus from earlier failed tests. */
            RepTestUtils.removeRepEnvironments(envRootDir);
            try {
                repEnvInfo =
                    RepTestUtils.setupEnvInfos(envRootDir,
                                               repNodes,
                                               envConfig);
                RepTestUtils.joinGroup(repEnvInfo);
                TestCase.assertTrue(repEnvInfo[0].getEnv().
                                    getState().isMaster());
                env = repEnvInfo[0].getEnv();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        dirRepEnvInfoMap.put(envRootDir, repEnvInfo);
        return env;
    }

    /**
     * Modifies the config to use a shared cache and replace use of obsolete
     * sync apis with the Durability api to avoid mixed mode exceptions from
     * Environment.checkTxnConfig.
     */
    @SuppressWarnings("deprecation")
    private void adjustEnvConfig(EnvironmentConfig envConfig)
        throws IllegalArgumentException {

        /*
         * Replicated tests use multiple environments, configure shared cache
         * to reduce the memory consumption (unless this property was set by
         * the test earlier).
         */
        if (!envConfig.isConfigParamSet(EnvironmentConfig.SHARED_CACHE)) {
            envConfig.setSharedCache(true);
        }

        boolean sync = false;
        boolean writeNoSync = envConfig.getTxnWriteNoSync();
        boolean noSync = envConfig.getTxnNoSync();
        envConfig.setTxnWriteNoSync(false);
        envConfig.setTxnNoSync(false);
        envConfig.setDurability(getDurability(sync, writeNoSync, noSync));
    }

    /**
     * Restarts a group associated with an existing environment on disk.
     * Returns the environment associated with the master.
     */
    private Environment restartGroup(RepEnvInfo[] repEnvInfo) {
        return RepTestUtils.restartGroup(repEnvInfo);

    }

    private void closeInternal(Environment env, boolean doCheckpoint) {

        File envRootDir = env.getHome().getParentFile();

        RepEnvInfo[] repEnvInfo = dirRepEnvInfoMap.get(envRootDir);
        try {
            ReplicatedEnvironment master = null;
            for (RepEnvInfo ri : repEnvInfo) {
                ReplicatedEnvironment r = ri.getEnv();
                if (r == null) {
                    continue;
                }
                if (r.getState() == ReplicatedEnvironment.State.MASTER) {
                    master = r;
                }
            }
            TestCase.assertNotNull(master);
            VLSN lastVLSN = RepInternal.getRepImpl(master).getRepNode().
                getVLSNIndex().getRange().getLast();
            RepTestUtils.syncGroupToVLSN(repEnvInfo, repNodes, lastVLSN);

            if (doNodeEqualityCheck) {
                RepTestUtils.checkNodeEquality(lastVLSN, false, repEnvInfo);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        RepTestUtils.shutdownRepEnvs(repEnvInfo, doCheckpoint);
    }

    /* Close environment with a checkpoint. */
    @Override
    public void close(Environment env)
        throws DatabaseException {

        closeInternal(env, true);
    }

    /* Close environment without a checkpoint. */
    @Override
    public void closeNoCheckpoint(Environment env)
        throws DatabaseException {

        closeInternal(env, false);
    }

    /* Simulate a crash. */
    @Override
    public void abnormalClose(Environment env) {
        File envRootDir = env.getHome().getParentFile();
        RepEnvInfo[] repEnvInfo = dirRepEnvInfoMap.get(envRootDir);
        for (RepEnvInfo info : repEnvInfo) {
            info.abnormalCloseEnv();
        }
    }

    @Override
    public void destroy() {
        for (File f : dirRepEnvInfoMap.keySet()) {
            RepTestUtils.removeRepEnvironments(f);
        }
        dirRepEnvInfoMap.clear();
    }

    public void syncGroup(File envRootDir) {
        RepEnvInfo[] repEnvInfo = dirRepEnvInfoMap.get(envRootDir);
        RepTestUtils.syncGroup(repEnvInfo);
    }

    public RepEnvInfo[] getRepEnvInfo(File envRootDir) {
        return dirRepEnvInfoMap.get(envRootDir);
    }

    /**
     * Convert old style sync into Durability as defined in TxnConfig.
     *
     * @see com.sleepycat.je.TransactionConfig#getDurabilityFromSync
     */
    private Durability getDurability(boolean sync,
                                     boolean writeNoSync,
                                     boolean noSync) {
        if (sync) {
            return Durability.COMMIT_SYNC;
        } else if (writeNoSync) {
            return Durability.COMMIT_WRITE_NO_SYNC;
        } else if (noSync) {
            return Durability.COMMIT_NO_SYNC;
        }
        return Durability.COMMIT_SYNC;
    }

    @Override
    public void resetNodeEqualityCheck() {
        doNodeEqualityCheck = !doNodeEqualityCheck;
    }
}
