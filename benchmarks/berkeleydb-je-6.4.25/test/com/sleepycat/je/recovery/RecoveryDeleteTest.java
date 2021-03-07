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

package com.sleepycat.je.recovery;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.config.EnvironmentParams;

public class RecoveryDeleteTest extends RecoveryTestBase {

    @Override
    protected void setExtraProperties() {
        envConfig.setConfigParam(
                      EnvironmentParams.ENV_RUN_INCOMPRESSOR.getName(),
                      "false");
    }

    /* Make sure that we can recover after the entire tree is compressed away. */
    @Test
    public void testDeleteAllAndCompress()
        throws Throwable {

        createEnvAndDbs(1 << 20, false, NUM_DBS);
        int numRecs = 10;

        try {
            // Set up an repository of expected data
            Map<TestData, Set<TestData>> expectedData =
                new HashMap<TestData, Set<TestData>>();

            // insert all the data
            Transaction txn = env.beginTransaction(null, null);
            insertData(txn, 0, numRecs -1 , expectedData, 1, true, NUM_DBS);
            txn.commit();

            /*
             * Do two checkpoints here so that the INs that make up this new
             * tree are not in the redo part of the log.
             */
            CheckpointConfig ckptConfig = new CheckpointConfig();
            ckptConfig.setForce(true);
            env.checkpoint(ckptConfig);
            env.checkpoint(ckptConfig);
            txn = env.beginTransaction(null, null);
            insertData(txn, numRecs, numRecs + 1, expectedData, 1, true, NUM_DBS);
            txn.commit();

            /* delete all */
            txn = env.beginTransaction(null, null);
            deleteData(txn, expectedData, true, true, NUM_DBS);
            txn.commit();

            /* This will remove the root. */
            env.compress();

            closeEnv();
            recoverAndVerify(expectedData, NUM_DBS);
        } catch (Throwable t) {
            // print stacktrace before trying to clean up files
            t.printStackTrace();
            throw t;
        }
    }
}
