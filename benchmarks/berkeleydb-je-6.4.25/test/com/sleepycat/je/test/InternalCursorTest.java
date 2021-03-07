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

package com.sleepycat.je.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.txn.BasicLocker;
import com.sleepycat.je.txn.Locker;
import com.sleepycat.util.test.TxnTestCase;

/**
 * Tests the use of the Cursor class for internal operations where
 * DbInternal.makeCursor is called instead of Database.openCursor.  The
 * makeCursor method calls Cursor.setNonCloning(true), so this tests the
 * NonCloning feature.  The NonCloning feature is not available for public API
 * Cursors.
 */
@RunWith(Parameterized.class)
public class InternalCursorTest extends TxnTestCase {

    
    @Parameters
    public static List<Object[]> genParams() {
        return getTxnParams(null, false);
    }
    
    public InternalCursorTest(String type){
        initEnvConfig();
        txnType = type;
        isTransactional = (txnType != TXN_NULL);
        customName = txnType;
    }

    /**
     * Ensures that a Cursor is removed from the current BIN when Cursor
     * methods such as put() and search() are called. These methods pass false
     * for the samePosition parameter of beginMoveCursor.  Previously the
     * CursorImpl was not reset when cloning was disabled, which caused Cursors
     * to accumulate in BINs.  This test goes along new assertions in
     * CursorImpl.setBIN/setDupBIN which check for residual cursors.  [#16280]
     */
    @Test
    public void testAddCursorFix() {
        final Database db = openDb("foo", false /*duplicates*/);
        final DatabaseEntry key = new DatabaseEntry();
        final DatabaseEntry data = new DatabaseEntry();
        IntegerBinding.intToEntry(123, data);

        final Transaction txn = txnBeginCursor();
        final Locker locker = (txn != null) ?
            DbInternal.getLocker(txn) :
            BasicLocker.createBasicLocker(DbInternal.getEnvironmentImpl(env));
        /* Create a non-sticky Cursor. */
        final Cursor cursor = DbInternal.makeCursor
            (DbInternal.getDatabaseImpl(db), locker, null);

        /* Add records to create 2 BINs. */
        OperationStatus status;
        for (int i = 1; i <= 200; i += 1) {
            IntegerBinding.intToEntry(i, key);
            status = cursor.put(key, data);
            assertSame(OperationStatus.SUCCESS, status);
        }

        /* Move to first BIN. */
        status = cursor.getFirst(key, data, null);
        assertSame(OperationStatus.SUCCESS, status);

        /* Put in second BIN. */
        IntegerBinding.intToEntry(200, key);
        status = cursor.put(key, data);
        assertSame(OperationStatus.SUCCESS, status);

        /* Search in first BIN. */
        IntegerBinding.intToEntry(1, key);
        status = cursor.getSearchKey(key, data, null);
        assertSame(OperationStatus.SUCCESS, status);

        /* Put in second BIN. */
        IntegerBinding.intToEntry(200, key);
        status = cursor.put(key, data);
        assertSame(OperationStatus.SUCCESS, status);

        /* Traverse all records. */
        status = cursor.getFirst(key, data, null);
        assertSame(OperationStatus.SUCCESS, status);
        for (int i = 1; i <= 200; i += 1) {
            assertEquals(i, IntegerBinding.entryToInt(key));
            status = cursor.getNext(key, data, null);
            assertSame((i == 200) ?
                        OperationStatus.NOTFOUND :
                        OperationStatus.SUCCESS,
                        status);
        }

        /* Put in first BIN. */
        IntegerBinding.intToEntry(1, key);
        status = cursor.put(key, data);
        assertSame(OperationStatus.SUCCESS, status);

        cursor.close();
        if (txn != null) {
            txnCommit(txn);
        } else {
            locker.operationEnd(true);
        }

        db.close();
    }

    private Database openDb(String name, boolean duplicates) {

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(isTransactional);
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(duplicates);

        Transaction txn = txnBegin();
        try {
            return env.openDatabase(txn, name, dbConfig);
        } finally {
            txnCommit(txn);
        }
    }
}
