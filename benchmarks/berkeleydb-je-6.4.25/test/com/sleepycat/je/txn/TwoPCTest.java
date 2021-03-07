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

package com.sleepycat.je.txn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import javax.transaction.xa.XAResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionStats;
import com.sleepycat.je.XAEnvironment;
import com.sleepycat.je.log.LogUtils.XidImpl;
import com.sleepycat.je.util.StringDbt;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;
import com.sleepycat.utilint.StringUtils;

/*
 * Simple 2PC transaction testing.
 */
public class TwoPCTest extends TestBase {
    private final File envHome;
    private XAEnvironment env;
    private Database db;

    public TwoPCTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    @Before
    public void setUp()
        throws Exception {

        super.setUp();
        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);
        env = new XAEnvironment(envHome, envConfig);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        db = env.openDatabase(null, "foo", dbConfig);
    }

    @After
    public void tearDown()
        throws Exception {

        db.close();
        env.close();
    }

    /**
     * Basic Two Phase Commit calls.
     */
    @Test
    public void testBasic2PC() {
        try {
        TransactionStats stats =
            env.getTransactionStats(TestUtils.FAST_STATS);
        /*
         * 2 commits for setting up XA env, opening db.
         */
        int numBegins = 2;
        int numCommits = 2;
        int numXAPrepares = 0;
        int numXACommits = 0;
        assertEquals(numBegins, stats.getNBegins());
        assertEquals(numCommits, stats.getNCommits());
        assertEquals(numXAPrepares, stats.getNXAPrepares());
        assertEquals(numXACommits, stats.getNXACommits());

        Transaction txn = env.beginTransaction(null, null);
        stats = env.getTransactionStats(TestUtils.FAST_STATS);
        numBegins++;
        assertEquals(numBegins, stats.getNBegins());
        assertEquals(numCommits, stats.getNCommits());
        assertEquals(numXAPrepares, stats.getNXAPrepares());
        assertEquals(numXACommits, stats.getNXACommits());
        assertEquals(1, stats.getNActive());

        XidImpl xid = new XidImpl(1, StringUtils.toUTF8("TwoPCTest1"), null);
        env.setXATransaction(xid, txn);
        stats = env.getTransactionStats(TestUtils.FAST_STATS);
        assertEquals(numBegins, stats.getNBegins());
        assertEquals(numCommits, stats.getNCommits());
        assertEquals(numXAPrepares, stats.getNXAPrepares());
        assertEquals(numXACommits, stats.getNXACommits());
        assertEquals(1, stats.getNActive());

        StringDbt key = new StringDbt("key");
        StringDbt data = new StringDbt("data");
        db.put(txn, key, data);
        stats = env.getTransactionStats(TestUtils.FAST_STATS);
        assertEquals(numBegins, stats.getNBegins());
        assertEquals(numCommits, stats.getNCommits());
        assertEquals(numXAPrepares, stats.getNXAPrepares());
        assertEquals(numXACommits, stats.getNXACommits());
        assertEquals(1, stats.getNActive());

        env.prepare(xid);
        numXAPrepares++;
        stats = env.getTransactionStats(TestUtils.FAST_STATS);
        assertEquals(numBegins, stats.getNBegins());
        assertEquals(numCommits, stats.getNCommits());
        assertEquals(numXAPrepares, stats.getNXAPrepares());
        assertEquals(numXACommits, stats.getNXACommits());
        assertEquals(1, stats.getNActive());

        env.commit(xid, false);
        numCommits++;
        numXACommits++;
        stats = env.getTransactionStats(TestUtils.FAST_STATS);
        assertEquals(numBegins, stats.getNBegins());
        assertEquals(numCommits, stats.getNCommits());
        assertEquals(numXAPrepares, stats.getNXAPrepares());
        assertEquals(numXACommits, stats.getNXACommits());
        assertEquals(0, stats.getNActive());
        } catch (Exception E) {
            System.out.println("caught " + E);
        }
    }

    /**
     * Basic readonly-prepare.
     */
    @Test
    public void testROPrepare() {
        try {
            Transaction txn = env.beginTransaction(null, null);
            XidImpl xid = new XidImpl(1, StringUtils.toUTF8("TwoPCTest1"), null);
            env.setXATransaction(xid, txn);

            assertEquals(XAResource.XA_RDONLY, env.prepare(xid));
        } catch (Exception E) {
            System.out.println("caught " + E);
        }
    }

    /**
     * Test calling prepare twice (should throw exception).
     */
    @Test
    public void testTwicePreparedTransaction()
        throws Throwable {

        Transaction txn = env.beginTransaction(null, null);
        XidImpl xid = new XidImpl(1, StringUtils.toUTF8("TwoPCTest2"), null);
        env.setXATransaction(xid, txn);
        StringDbt key = new StringDbt("key");
        StringDbt data = new StringDbt("data");
        db.put(txn, key, data);

        try {
            env.prepare(xid);
            env.prepare(xid);
            fail("should not be able to prepare twice");
        } catch (Exception E) {
            env.commit(xid, false);
        }
    }

    /**
     * Test calling rollback(xid) on an unregistered xa txn.
     */
    @Test
    public void testRollbackNonExistent()
        throws Throwable {

        Transaction txn = env.beginTransaction(null, null);
        StringDbt key = new StringDbt("key");
        StringDbt data = new StringDbt("data");
        db.put(txn, key, data);
        XidImpl xid = new XidImpl(1, StringUtils.toUTF8("TwoPCTest2"), null);

        try {
            env.rollback(xid);
            fail("should not be able to call rollback on an unknown xid");
        } catch (Exception E) {
        }
        txn.abort();
    }

    /**
     * Test calling commit(xid) on an unregistered xa txn.
     */
    @Test
    public void testCommitNonExistent()
        throws Throwable {

        Transaction txn = env.beginTransaction(null, null);
        StringDbt key = new StringDbt("key");
        StringDbt data = new StringDbt("data");
        db.put(txn, key, data);
        XidImpl xid = new XidImpl(1, StringUtils.toUTF8("TwoPCTest2"), null);

        try {
            env.commit(xid, false);
            fail("should not be able to call commit on an unknown xid");
        } catch (Exception E) {
        }
        txn.abort();
    }
}
