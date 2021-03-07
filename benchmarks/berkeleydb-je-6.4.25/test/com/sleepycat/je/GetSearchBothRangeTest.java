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

package com.sleepycat.je;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Comparator;

import org.junit.Test;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.util.DualTestCase;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;

/**
 * Tests getSearchBothRange when searching for a key that doesn't exist.
 * [#11119]
 */
public class GetSearchBothRangeTest extends DualTestCase {

    private File envHome;
    private Environment env;
    private Database db;
    private boolean dups;

    public GetSearchBothRangeTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    /**
     * Open environment and database.
     */
    private void openEnv()
        throws DatabaseException {

        openEnvWithComparator(null);
    }

    private void openEnvWithComparator(Class comparatorClass)
        throws DatabaseException {

        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);
        //*
        envConfig.setConfigParam
            (EnvironmentParams.ENV_RUN_INCOMPRESSOR.getName(), "false");
        //*/
        env = create(envHome, envConfig);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setSortedDuplicates(dups);
        dbConfig.setAllowCreate(true);

        dbConfig.setBtreeComparator(comparatorClass);

        db = env.openDatabase(null, "GetSearchBothRangeTest", dbConfig);
    }

    /**
     * Close environment and database.
     */
    private void closeEnv()
        throws DatabaseException {

        db.close();
        db = null;
        close(env);
        env = null;
    }

    @Test
    public void testSearchKeyRangeWithDupTree()
        throws Exception {

        dups = true;
        openEnv();

        insert(1, 1);
        insert(1, 2);
        insert(3, 1);

        DatabaseEntry key = entry(2);
        DatabaseEntry data = new DatabaseEntry();

        Transaction txn = null;
        if (DualTestCase.isReplicatedTest(getClass())) {
            txn = env.beginTransaction(null, null);
        }

        Cursor cursor = db.openCursor(txn, null);
        OperationStatus status = cursor.getSearchKeyRange(key, data, null);
        assertEquals(OperationStatus.SUCCESS, status);
        assertEquals(3, val(key));
        assertEquals(1, val(data));
        cursor.close();
        if (txn != null) {
            txn.commit();
        }

        closeEnv();
    }

    @Test
    public void testSearchBothWithNoDupTree()
        throws Exception {

        dups = true;
        openEnv();

        insert(1, 1);

        DatabaseEntry key = entry(1);
        DatabaseEntry data = entry(2);

        Transaction txn = null;
        if (DualTestCase.isReplicatedTest(getClass())) {
            txn = env.beginTransaction(null, null);
        }

        Cursor cursor = db.openCursor(txn, null);
        OperationStatus status = cursor.getSearchBoth(key, data, null);
        assertEquals(OperationStatus.NOTFOUND, status);
        cursor.close();

        key = entry(1);
        data = entry(1);

        cursor = db.openCursor(txn, null);
        status = cursor.getSearchBoth(key, data, null);
        assertEquals(OperationStatus.SUCCESS, status);
        assertEquals(1, val(key));
        assertEquals(1, val(data));
        cursor.close();

        key = entry(1);
        data = entry(0);

        cursor = db.openCursor(txn, null);
        status = cursor.getSearchBothRange(key, data, null);
        assertEquals(OperationStatus.SUCCESS, status);
        assertEquals(1, val(key));
        assertEquals(1, val(data));
        cursor.close();
        if (txn != null) {
            txn.commit();
        }

        closeEnv();
    }

    @Test
    public void testSuccess()
        throws DatabaseException {

        openEnv();
        insert(1, 1);
        insert(3, 1);
        if (dups) {
            insert(1, 2);
            insert(3, 2);
        }

        DatabaseEntry key = entry(3);
        DatabaseEntry data = entry(0);

        Transaction txn = null;
        if (DualTestCase.isReplicatedTest(getClass())) {
            txn = env.beginTransaction(null, null);
        }

        Cursor cursor = db.openCursor(txn, null);
        OperationStatus status = cursor.getSearchBothRange(key, data, null);
        assertEquals(OperationStatus.NOTFOUND, status);

        data = entry(1);
        status = cursor.getSearchBothRange(key, data, null);
        assertEquals(OperationStatus.SUCCESS, status);
        assertEquals(3, val(key));
        assertEquals(1, val(data));
        cursor.close();
        if (txn != null) {
            txn.commit();
        }

        closeEnv();
    }

    @Test
    public void testSuccessDup()
        throws DatabaseException {

        dups = true;

        openEnv();
        insert(1, 1);
        insert(3, 1);
        if (dups) {
            insert(1, 2);
            insert(3, 2);
        }

        DatabaseEntry key = entry(3);
        DatabaseEntry data = entry(0);

        Transaction txn = null;
        if (DualTestCase.isReplicatedTest(getClass())) {
            txn = env.beginTransaction(null, null);
        }

        Cursor cursor = db.openCursor(txn, null);
        OperationStatus status = cursor.getSearchBothRange(key, data, null);
        assertEquals(OperationStatus.SUCCESS, status);
        assertEquals(3, val(key));
        assertEquals(1, val(data));
        cursor.close();
        if (txn != null) {
            txn.commit();
        }

        closeEnv();
    }

    @Test
    public void testNotFound()
        throws DatabaseException {

        openEnv();
        insert(1, 0);
        if (dups) {
            insert(1, 1);
        }

        DatabaseEntry key = entry(2);
        DatabaseEntry data = entry(0);

        Transaction txn = null;
        if (DualTestCase.isReplicatedTest(getClass())) {
            txn = env.beginTransaction(null, null);
        }

        Cursor cursor = db.openCursor(txn, null);
        OperationStatus status = cursor.getSearchBothRange(key, data, null);
        assertEquals(OperationStatus.NOTFOUND, status);
        cursor.close();
        if (txn != null) {
            txn.commit();
        }

        closeEnv();
    }

    @Test
    public void testNotFoundDup()
        throws DatabaseException {

        dups = true;
        testNotFound();
    }

    @Test
    public void testSearchBefore()
        throws DatabaseException {

        dups = true;
        openEnv();
        insert(1, 0);

        DatabaseEntry key = entry(1);
        DatabaseEntry data = entry(2);

        Transaction txn = null;
        if (DualTestCase.isReplicatedTest(getClass())) {
            txn = env.beginTransaction(null, null);
        }

        Cursor cursor = db.openCursor(txn, null);
        OperationStatus status = cursor.getSearchBothRange(key, data, null);
        assertEquals(OperationStatus.NOTFOUND, status);
        cursor.close();
        if (txn != null) {
            txn.commit();
        }

        closeEnv();
    }

    @Test
    public void testSearchBeforeDups()
        throws DatabaseException {

        dups = true;
        openEnv();
        insert(1, 1);
        insert(1, 2);

        DatabaseEntry key = entry(1);
        DatabaseEntry data = entry(0);

        Transaction txn = null;
        if (DualTestCase.isReplicatedTest(getClass())) {
            txn = env.beginTransaction(null, null);
        }

        Cursor cursor = db.openCursor(txn, null);
        OperationStatus status = cursor.getSearchBothRange(key, data, null);
        assertEquals(OperationStatus.SUCCESS, status);
        assertEquals(1, val(key));
        assertEquals(1, val(data));
        cursor.close();
        if (txn != null) {
            txn.commit();
        }

        closeEnv();
    }

    public static class NormalComparator implements Comparator {

        public NormalComparator() {
        }

        public int compare(Object o1, Object o2) {

            DatabaseEntry arg1 = new DatabaseEntry((byte[]) o1);
            DatabaseEntry arg2 = new DatabaseEntry((byte[]) o2);
            int val1 = IntegerBinding.entryToInt(arg1);
            int val2 = IntegerBinding.entryToInt(arg2);

            if (val1 < val2) {
                return -1;
            } else if (val1 > val2) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    @Test
    public void testSearchAfterDups()
        throws DatabaseException {

        dups = true;
        openEnv();
        insert(1, 0);
        insert(1, 1);
        insert(2, 0);
        insert(2, 1);

        DatabaseEntry key = entry(1);
        DatabaseEntry data = entry(2);

        Transaction txn = null;
        if (DualTestCase.isReplicatedTest(getClass())) {
            txn = env.beginTransaction(null, null);
        }

        Cursor cursor = db.openCursor(txn, null);
        OperationStatus status = cursor.getSearchBothRange(key, data, null);
        assertEquals(OperationStatus.NOTFOUND, status);
        cursor.close();
        if (txn != null) {
            txn.commit();
        }

        closeEnv();
    }

    @Test
    public void testSearchAfterDupsWithComparator()
        throws DatabaseException {

        dups = true;
        openEnvWithComparator(NormalComparator.class);
        insert(1, 0);
        insert(1, 1);
        insert(2, 0);
        insert(2, 1);

        DatabaseEntry key = entry(1);
        DatabaseEntry data = entry(2);

        Transaction txn = null;
        if (DualTestCase.isReplicatedTest(getClass())) {
            txn = env.beginTransaction(null, null);
        }

        Cursor cursor = db.openCursor(txn, null);
        OperationStatus status = cursor.getSearchBothRange(key, data, null);
        assertEquals(OperationStatus.NOTFOUND, status);
        cursor.close();
        if (txn != null) {
            txn.commit();
        }

        closeEnv();
    }

    @Test
    public void testSearchAfterDeletedDup()
        throws DatabaseException {

        dups = true;
        openEnv();
        insert(1, 1);
        insert(1, 2);
        insert(1, 3);

        /* Delete {1,3} leaving {1,1} in dup tree. */
        Transaction txn = null;
        txn = env.beginTransaction(null, null);
        Cursor cursor = db.openCursor(txn, null);
        DatabaseEntry key = entry(1);
        DatabaseEntry data = entry(3);
        OperationStatus status = cursor.getSearchBothRange(key, data, null);
        assertEquals(OperationStatus.SUCCESS, status);
        cursor.delete();
        cursor.close();
        env.compress();

        /* Search for {1,3} and expect NOTFOUND. */
        cursor = db.openCursor(txn, null);
        key = entry(1);
        data = entry(3);
        status = cursor.getSearchBothRange(key, data, null);
        assertEquals(OperationStatus.NOTFOUND, status);
        cursor.close();
        txn.commit();

        closeEnv();
    }

    @Test
    public void testSingleDatumBug()
        throws DatabaseException {

        dups = true;
        openEnv();
        insert(1, 1);
        insert(2, 2);

        Transaction txn = null;
        if (DualTestCase.isReplicatedTest(getClass())) {
            txn = env.beginTransaction(null, null);
        }

        /* Search for {1,2} and expect NOTFOUND. */
        Cursor cursor = db.openCursor(txn, null);
        DatabaseEntry key = entry(1);
        DatabaseEntry data = entry(2);
        OperationStatus status = cursor.getSearchBothRange(key, data, null);
        assertEquals(OperationStatus.NOTFOUND, status);
        cursor.close();
        if (txn != null) {
            txn.commit();
        }

        closeEnv();
    }

    private int val(DatabaseEntry entry) {
        return IntegerBinding.entryToInt(entry);
    }

    private DatabaseEntry entry(int val) {
        DatabaseEntry entry = new DatabaseEntry();
        IntegerBinding.intToEntry(val, entry);
        return entry;
    }

    private void insert(int keyVal, int dataVal)
        throws DatabaseException {

        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry data = new DatabaseEntry();
        IntegerBinding.intToEntry(keyVal, key);
        IntegerBinding.intToEntry(dataVal, data);
        OperationStatus status;
        if (dups) {
            status = db.putNoDupData(null, key, data);
        } else {
            status= db.putNoOverwrite(null, key, data);
        }
        assertEquals(OperationStatus.SUCCESS, status);
    }
}
