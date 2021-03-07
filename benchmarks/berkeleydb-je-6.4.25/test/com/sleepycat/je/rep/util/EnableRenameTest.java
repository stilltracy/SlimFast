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

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.dbi.DbConfigManager;
import com.sleepycat.je.rep.InsufficientLogException;
import com.sleepycat.je.rep.NetworkRestore;
import com.sleepycat.je.rep.NetworkRestoreConfig;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.ReplicationConfig;
import com.sleepycat.je.rep.impl.RepTestBase;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;

/**
 * Regression test for SR #21537, which was first reported on OTN forum.
 */
public class EnableRenameTest extends RepTestBase {
    public static final String NEW_DB_NAME = TEST_DB_NAME + "2";

    @Before
    public void setUp() 
        throws Exception {
        
        groupSize = 2;
        super.setUp();
    }

    @Test
    public void testEnableWithRename() throws Exception {
        
        RepEnvInfo master = repEnvInfo[0];
        Properties temp = new Properties();
        DbConfigManager.applyFileConfig(repEnvInfo[0].getEnvHome(), 
                                        temp, true);
        if ("true".equals
                (temp.get("je.rep.preserveRecordVersion"))) {
             // TODO: enable this and fix the JE bug
             return;
        } 
        
        Environment env = new Environment(master.getEnvHome(),
                                          master.getEnvConfig());
        Database db = env.openDatabase(null, TEST_DB_NAME, dbconfig);
        db.close();
        env.close();

        ReplicationConfig masterConf =
            master.getRepConfig();
        DbEnableReplication enabler =
            new DbEnableReplication(master.getEnvHome(),
                                    masterConf.getGroupName(),
                                    masterConf.getNodeName(),
                                    masterConf.getNodeHostPort());
        enabler.convert();

        restartNodes(master);
        ReplicatedEnvironment masterEnv = master.getEnv();
        masterEnv.renameDatabase(null, TEST_DB_NAME, NEW_DB_NAME);

        ReplicatedEnvironment replicaEnv = null;
        try {
            replicaEnv = openRepEnv();
        } catch (InsufficientLogException ile) {
            NetworkRestore restore = new NetworkRestore();
            restore.execute(ile, new NetworkRestoreConfig());
            replicaEnv = openRepEnv();
        }
        DatabaseConfig dc2 = new DatabaseConfig();
        dc2.setReadOnly(true);
        dc2.setTransactional(true);

        try {
            db = replicaEnv.openDatabase(null, NEW_DB_NAME, dc2);
            db.close();
        } finally {
            replicaEnv.close();
        }
    }

    @Test
    public void testWorkaround() throws Exception {
        // same as above, except start new replica before doing the rename
        RepEnvInfo master = repEnvInfo[0];
        
        Properties temp = new Properties();
        DbConfigManager.applyFileConfig(repEnvInfo[0].getEnvHome(), 
                                        temp, true);
        if ("true".equals
                (temp.get("je.rep.preserveRecordVersion"))) {
             // TODO: enable this and fix the JE bug
             return;
        }
        
        Environment env = new Environment(master.getEnvHome(),
                                          master.getEnvConfig());
        Database db = env.openDatabase(null, TEST_DB_NAME, dbconfig);
        db.close();
        env.close();

        ReplicationConfig masterConf =
            master.getRepConfig();
        DbEnableReplication enabler =
            new DbEnableReplication(master.getEnvHome(),
                                    masterConf.getGroupName(),
                                    masterConf.getNodeName(),
                                    masterConf.getNodeHostPort());
        enabler.convert();

        restartNodes(master);
        ReplicatedEnvironment replicaEnv = null;
        try {
            replicaEnv = openRepEnv();
        } catch (InsufficientLogException ile) {
            NetworkRestore restore = new NetworkRestore();
            restore.execute(ile, new NetworkRestoreConfig());
            replicaEnv = openRepEnv();
        }

        ReplicatedEnvironment masterEnv = master.getEnv();
        masterEnv.renameDatabase(null, TEST_DB_NAME, NEW_DB_NAME);

        DatabaseConfig dc2 = new DatabaseConfig();
        dc2.setReadOnly(true);
        dc2.setTransactional(true);
        db = replicaEnv.openDatabase(null, NEW_DB_NAME, dc2);

        db.close();
        replicaEnv.close();
    }

    private ReplicatedEnvironment openRepEnv() throws Exception {
        RepEnvInfo replica = repEnvInfo[1];
        return new ReplicatedEnvironment(replica.getEnvHome(),
                                         replica.getRepConfig(),
                                         replica.getEnvConfig());
    }
}
