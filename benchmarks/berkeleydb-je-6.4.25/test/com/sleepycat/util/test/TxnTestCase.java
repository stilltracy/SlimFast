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

package com.sleepycat.util.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;

import com.sleepycat.compat.DbCompat;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;
import com.sleepycat.je.util.DualTestCase;

/**
 * Permutes test cases over three transaction types: null (non-transactional),
 * auto-commit, and user (explicit).
 *
 * <p>Overrides runTest, setUp and tearDown to open/close the environment and
 * to set up protected members for use by test cases.</p>
 *
 * <p>If a subclass needs to override setUp or tearDown, the overridden method
 * should call super.setUp or super.tearDown.</p>
 *
 * <p>When writing a test case based on this class, write it as if a user txn
 * were always used: call txnBegin, txnCommit and txnAbort for all write
 * operations.  Use the isTransactional protected field for setup of a database
 * config.</p>
 */
public abstract class TxnTestCase extends DualTestCase {

    public static final String TXN_NULL = "txn-null";
    public static final String TXN_AUTO = "txn-auto";
    public static final String TXN_USER = "txn-user";
    public static final String TXN_CDB = "txn-cdb";

    protected File envHome;
    protected Environment env;
    protected EnvironmentConfig envConfig;
    protected String txnType;
    protected boolean isTransactional;

    public static List<Object[]> getTxnParams(String[] txnTypes, boolean rep) {
        final List<Object[]> list = new ArrayList<Object[]>();
        for (final String type : getTxnTypes(txnTypes, rep)) {
            list.add(new Object[] {type});
        }
        return list;
    }

    public static String[] getTxnTypes(String[] txnTypes, boolean rep) {
        if (txnTypes == null) {
            if (rep) {
                txnTypes = new String[] { // Skip non-transactional tests
                                          TxnTestCase.TXN_USER,
                                          TxnTestCase.TXN_AUTO };
            } else if (!DbCompat.CDB) {
                txnTypes = new String[] { TxnTestCase.TXN_NULL,
                                          TxnTestCase.TXN_USER,
                                          TxnTestCase.TXN_AUTO };
            } else {
                txnTypes = new String[] { TxnTestCase.TXN_NULL,
                                          TxnTestCase.TXN_USER,
                                          TxnTestCase.TXN_AUTO, 
                                          TxnTestCase.TXN_CDB };
            }
        } else {
            if (!DbCompat.CDB) {
                /* Remove TxnTestCase.TXN_CDB, if there is any. */
                final ArrayList<String> tmp =
                    new ArrayList<String>(Arrays.asList(txnTypes));
                tmp.remove(TxnTestCase.TXN_CDB);
                txnTypes = new String[tmp.size()];
                tmp.toArray(txnTypes);
            }
        }
        return txnTypes;
    }

    @Before
    public void setUp()
        throws Exception {

        super.setUp();
        envHome = SharedTestUtils.getNewDir();
        openEnv();
    }

    @After
    public void tearDown()
        throws Exception {

        super.tearDown();
        closeEnv();
        env = null;
    }
    
    protected void initEnvConfig() {
        if (envConfig == null) {
            envConfig = new EnvironmentConfig();
            envConfig.setAllowCreate(true);
            
            /* Always use write-no-sync (by default) to speed up tests. */
            if (!envConfig.getTxnNoSync() && !envConfig.getTxnWriteNoSync()) {
                envConfig.setTxnWriteNoSync(true);
            }
        }
    }

    /**
     * Closes the environment and sets the env field to null.
     * Used for closing and reopening the environment.
     */
    public void closeEnv()
        throws DatabaseException {

        if (env != null) {
            close(env);
            env = null;
        }
    }

    /**
     * Opens the environment based on the txnType for this test case.
     * Used for closing and reopening the environment.
     */
    public void openEnv()
        throws DatabaseException {

        if (txnType == TXN_NULL) {
            TestEnv.BDB.copyConfig(envConfig);
            env = create(envHome, envConfig);
        } else if (txnType == TXN_AUTO) {
            TestEnv.TXN.copyConfig(envConfig);
            env = create(envHome, envConfig);
        } else if (txnType == TXN_USER) {
            TestEnv.TXN.copyConfig(envConfig);
            env = create(envHome, envConfig);
        } else if (txnType == TXN_CDB) {
            TestEnv.CDB.copyConfig(envConfig);
            env = create(envHome, envConfig);
        } else {
            assert false;
        }
    }

    /**
     * Begin a txn if in TXN_USER mode; otherwise return null;
     */
    protected Transaction txnBegin()
        throws DatabaseException {

        return txnBegin(null, null);
    }

    /**
     * Begin a txn if in TXN_USER mode; otherwise return null;
     */
    protected Transaction txnBegin(Transaction parentTxn,
                                   TransactionConfig config)
        throws DatabaseException {

        if (txnType == TXN_USER) {
            return env.beginTransaction(parentTxn, config);
        }
        return null;
    }

    /**
     * Begin a txn if in TXN_USER or TXN_AUTO mode; otherwise return null;
     */
    protected Transaction txnBeginCursor()
        throws DatabaseException {

        return txnBeginCursor(null, null);
    }

    /**
     * Begin a txn if in TXN_USER or TXN_AUTO mode; otherwise return null;
     */
    protected Transaction txnBeginCursor(Transaction parentTxn,
                                         TransactionConfig config)
        throws DatabaseException {

        if (txnType == TXN_USER || txnType == TXN_AUTO) {
            return env.beginTransaction(parentTxn, config);
        } else {
            return null;
        }
    }
    
    /**
     * Create a write cursor config;
     */
    public CursorConfig getWriteCursorConfig() {
        if (txnType != TXN_CDB) {
            return null;
        }
        final CursorConfig config = new CursorConfig();
        DbCompat.setWriteCursor(config, true);
        return config;
    } 

    /**
     * Commit a txn if non-null.
     */
    protected void txnCommit(Transaction txn)
        throws DatabaseException {

        if (txn != null) {
            txn.commit();
        }
    }

    /**
     * Commit a txn if non-null.
     */
    protected void txnAbort(Transaction txn)
        throws DatabaseException {

        if (txn != null) {
            txn.abort();
        }
    }
}
