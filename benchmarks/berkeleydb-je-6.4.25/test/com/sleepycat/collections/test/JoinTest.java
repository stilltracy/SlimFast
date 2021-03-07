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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.serial.test.MarshalledObject;
import com.sleepycat.collections.StoredCollection;
import com.sleepycat.collections.StoredContainer;
import com.sleepycat.collections.StoredIterator;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.collections.TransactionRunner;
import com.sleepycat.collections.TransactionWorker;
import com.sleepycat.collections.TupleSerialFactory;
import com.sleepycat.compat.DbCompat;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;
import com.sleepycat.util.test.TestEnv;

/**
 * @author Mark Hayes
 */
public class JoinTest extends TestBase
    implements TransactionWorker {

    private static final String MATCH_DATA = "d4"; // matches both keys = "yes"
    private static final String MATCH_KEY  = "k4"; // matches both keys = "yes"
    private static final String[] VALUES = {"yes", "yes"};

    private Environment env;
    private TransactionRunner runner;
    private StoredClassCatalog catalog;
    private TupleSerialFactory factory;
    private Database store;
    private SecondaryDatabase index1;
    private SecondaryDatabase index2;
    private StoredMap storeMap;
    private StoredMap indexMap1;
    private StoredMap indexMap2;

    public JoinTest() {
        customName = "JoinTest";
    }

    @Before
    public void setUp()
        throws Exception {

        SharedTestUtils.printTestName(customName);
        env = TestEnv.TXN.open(customName);
        runner = new TransactionRunner(env);
        createDatabase();
    }

    @After
    public void tearDown() {

        try {
            if (index1 != null) {
                index1.close();
            }
            if (index2 != null) {
                index2.close();
            }
            if (store != null) {
                store.close();
            }
            if (catalog != null) {
                catalog.close();
            }
            if (env != null) {
                env.close();
            }
        } catch (Exception e) {
            System.out.println("Ignored exception during tearDown: " + e);
        } finally {
            /* Ensure that GC can cleanup. */
            index1 = null;
            index2 = null;
            store = null;
            catalog = null;
            env = null;
            runner = null;
            factory = null;
            storeMap = null;
            indexMap1 = null;
            indexMap2 = null;
        }
    }

    @Test
    public void runTest()
        throws Exception {

        runner.run(this);
    }

    public void doWork() {
        createViews();
        writeAndRead();
    }

    private void createDatabase()
        throws Exception {

        catalog = new StoredClassCatalog(openDb("catalog.db"));
        factory = new TupleSerialFactory(catalog);
        assertSame(catalog, factory.getCatalog());

        store = openDb("store.db");
        index1 = openSecondaryDb(store, "index1.db", "1");
        index2 = openSecondaryDb(store, "index2.db", "2");
    }

    private Database openDb(String file)
        throws Exception {

        DatabaseConfig config = new DatabaseConfig();
        DbCompat.setTypeBtree(config);
        config.setTransactional(true);
        config.setAllowCreate(true);

        return DbCompat.testOpenDatabase(env, null, file, null, config);
    }

    private SecondaryDatabase openSecondaryDb(Database primary,
                                              String file,
                                              String keyName)
        throws Exception {

        SecondaryConfig secConfig = new SecondaryConfig();
        DbCompat.setTypeBtree(secConfig);
        secConfig.setTransactional(true);
        secConfig.setAllowCreate(true);
        DbCompat.setSortedDuplicates(secConfig, true);
        secConfig.setKeyCreator(factory.getKeyCreator(MarshalledObject.class,
                                                      keyName));

        return DbCompat.testOpenSecondaryDatabase
            (env, null, file, null, primary, secConfig);
    }

    private void createViews() {
        storeMap = factory.newMap(store, String.class,
                                         MarshalledObject.class, true);
        indexMap1 = factory.newMap(index1, String.class,
                                           MarshalledObject.class, true);
        indexMap2 = factory.newMap(index2, String.class,
                                           MarshalledObject.class, true);
    }

    private void writeAndRead() {
        // write records: Data, PrimaryKey, IndexKey1, IndexKey2
        assertNull(storeMap.put(null,
            new MarshalledObject("d1", "k1", "no",  "yes")));
        assertNull(storeMap.put(null,
            new MarshalledObject("d2", "k2", "no",  "no")));
        assertNull(storeMap.put(null,
            new MarshalledObject("d3", "k3", "no",  "yes")));
        assertNull(storeMap.put(null,
            new MarshalledObject("d4", "k4", "yes", "yes")));
        assertNull(storeMap.put(null,
            new MarshalledObject("d5", "k5", "yes", "no")));

        Object o;
        Map.Entry e;

        // join values with index maps
        o = doJoin((StoredCollection) storeMap.values());
        assertEquals(MATCH_DATA, ((MarshalledObject) o).getData());

        // join keySet with index maps
        o = doJoin((StoredCollection) storeMap.keySet());
        assertEquals(MATCH_KEY, o);

        // join entrySet with index maps
        o = doJoin((StoredCollection) storeMap.entrySet());
        e = (Map.Entry) o;
        assertEquals(MATCH_KEY, e.getKey());
        assertEquals(MATCH_DATA, ((MarshalledObject) e.getValue()).getData());
    }

    private Object doJoin(StoredCollection coll) {

        StoredContainer[] indices = { indexMap1, indexMap2 };
        StoredIterator i = coll.join(indices, VALUES, null);
        try {
            assertTrue(i.hasNext());
            Object result = i.next();
            assertNotNull(result);
            assertFalse(i.hasNext());
            return result;
        } finally { i.close(); }
    }
}
