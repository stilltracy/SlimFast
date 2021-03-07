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

package com.sleepycat.collections.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.bind.EntityBinding;
import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.serial.TupleSerialBinding;
import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.collections.TransactionRunner;
import com.sleepycat.collections.TransactionWorker;
import com.sleepycat.compat.DbCompat;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;
import com.sleepycat.util.test.TestEnv;

/**
 * Unit test for [#19085]. The collections API supports storing and retrieving
 * null values, as long as the value binding supports null values.
 */
public class NullValueTest extends TestBase
    implements TransactionWorker {
    
    private Environment env;
    private ClassCatalog catalog;
    private Database db;
    private TransactionRunner runner;
    
    public NullValueTest() {

        customName = "NullValueTest";
    }
    
    @Before
    public void setUp()
        throws Exception {

        SharedTestUtils.printTestName(customName);
        env = TestEnv.TXN.open(customName);
        runner = new TransactionRunner(env);
        open();
    }
    
    @After
    public void tearDown() {
        if (catalog != null) {
            try {
                catalog.close();
            } catch (DatabaseException e) {
                System.out.println("During tearDown: " + e);
            }
        }
        if (db != null) {
            try {
                db.close();
            } catch (DatabaseException e) {
                System.out.println("During tearDown: " + e);
            }
        }
        if (env != null) {
            try {
                env.close();
            } catch (DatabaseException e) {
                System.out.println("During tearDown: " + e);
            }
        }
        catalog = null;
        db = null;
        env = null;
    }
    
    private void open()
        throws Exception {
    
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        DbCompat.setTypeBtree(dbConfig);

        Database catalogDb = DbCompat.testOpenDatabase(env, null, "catalog",
                                                       null, dbConfig);
        catalog = new StoredClassCatalog(catalogDb);
    
        db = DbCompat.testOpenDatabase(env, null, "test", null, dbConfig);
    }
    
    @Test
    public void runTest()
        throws Exception {

        runner.run(this);
    }
    
    public void doWork() {
        expectSuccessWithBindingThatDoesSupportNull();
        expectExceptionWithBindingThatDoesNotSupportNull();
        expectExceptionWithWithEntityBinding();
    }
    
    private void expectSuccessWithBindingThatDoesSupportNull() {

        /* Pass null for baseClass to support null values. */
        final EntryBinding<String> dataBinding =
            new SerialBinding<String>(catalog, null /*baseClass*/);

        final StoredMap<Integer, String> map = new StoredMap<Integer, String>
            (db, new IntegerBinding(), dataBinding, true);
        
        /* Store a null value.*/
        map.put(1, null);

        /* Get the null value. */
        assertNull(map.get(1));

        for (String value : map.values()) {
            assertNull(value);
        }

        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            assertEquals(Integer.valueOf(1), entry.getKey());
            assertNull(entry.getValue());
        }

        map.remove(1);
    }

    private void expectExceptionWithBindingThatDoesNotSupportNull() {

        /* Pass non-null for baseClass, null values will not be allowed. */
        final EntryBinding<String> dataBinding =
            new SerialBinding<String>(catalog, String.class /*baseClass*/);

        final StoredMap<Integer, String> map = new StoredMap<Integer, String>
            (db, new IntegerBinding(), dataBinding, true);

        try {
            map.put(1, null);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    public void expectExceptionWithWithEntityBinding() {

        final EntityBinding<MyEntity> entityBinding =
            new MyEntityBinding(catalog);

        final StoredMap<Integer, MyEntity> map =
            new StoredMap<Integer, MyEntity>
                (db, new IntegerBinding(), entityBinding, true);

        try {
            map.put(1, null);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    static class MyEntity {
        int key;
        String data;
    }

    static class MyEntityBinding extends TupleSerialBinding<String, MyEntity> {

        MyEntityBinding(ClassCatalog catalog) {
            super(catalog, String.class);
        }

        public MyEntity entryToObject(TupleInput keyInput, String data) {
            final MyEntity entity = new MyEntity();
            entity.key = keyInput.readInt();
            entity.data = data;
            return entity;
        }

        public void objectToKey(MyEntity entity, TupleOutput keyOutput) {
            keyOutput.writeInt(entity.key);
        }

        public String objectToData(MyEntity entity) {
            return entity.data;
        }
    }
}
