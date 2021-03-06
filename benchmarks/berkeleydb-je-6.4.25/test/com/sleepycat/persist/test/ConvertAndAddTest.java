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

package com.sleepycat.persist.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.evolve.Conversion;
import com.sleepycat.persist.evolve.Converter;
import com.sleepycat.persist.evolve.Mutations;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.EntityModel;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;
import com.sleepycat.util.test.TestEnv;

/**
 * Test a bug fix where an IndexOutOfBoundsException occurs when adding a field
 * and converting another field, where the latter field is alphabetically
 * higher than the former.  This is also tested by
 * EvolveClasses.FieldAddAndConvert, but that class does not test evolving an
 * entity that was created by catalog version 0.  [#15797]
 *
 * A modified version of this program was run manually with JE 3.2.30 to
 * produce a log, which is the result of the testSetup() test.  The sole log
 * file was renamed from 00000000.jdb to ConvertAndAddTest.jdb and added to CVS
 * in this directory.  When that log file is opened here, the bug is
 * reproduced.  The modifications to this program for 3.2.30 are:
 *
 *  + X in testSetup
 *  + X out testConvertAndAddField
 *  + don't remove log files in tearDown
 *  + @Entity version is 0
 *  + removed field MyEntity.a
 *
 * This test should be excluded from the BDB build because it uses a stored JE
 * log file and it tests a fix for a bug that was never present in BDB.
 *
 * @author Mark Hayes
 */
public class ConvertAndAddTest extends TestBase {

    private static final String STORE_NAME = "test";

    private File envHome;
    private Environment env;

    @Before
    public void setUp() 
        throws Exception {
        
        envHome = SharedTestUtils.getTestDir();
        super.setUp();
    }

    @After
    public void tearDown() {
        if (env != null) {
            try {
                env.close();
            } catch (DatabaseException e) {
                System.out.println("During tearDown: " + e);
            }
        }
        envHome = null;
        env = null;
    }

    private EntityStore open(boolean addConverter)
        throws DatabaseException {

        EnvironmentConfig envConfig = TestEnv.BDB.getConfig();
        envConfig.setAllowCreate(true);
        env = new Environment(envHome, envConfig);

        Mutations mutations = new Mutations();
        mutations.addConverter(new Converter
            (MyEntity.class.getName(), 0, "b", new MyConversion()));

        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        storeConfig.setMutations(mutations);
        return new EntityStore(env, "foo", storeConfig);
    }

    private void close(EntityStore store)
        throws DatabaseException {

        store.close();
        env.close();
        env = null;
    }

    @Test
    public void testConvertAndAddField()
        throws DatabaseException, IOException {

        /* Copy log file resource to log file zero. */
        TestUtils.loadLog(getClass(), "ConvertAndAddTest.jdb", envHome);

        EntityStore store = open(true /*addConverter*/);

        PrimaryIndex<Long, MyEntity> index =
            store.getPrimaryIndex(Long.class, MyEntity.class);

        MyEntity entity = index.get(1L);
        assertNotNull(entity);
        assertEquals(123, entity.b);

        close(store);
    }

    public void xtestSetup()
        throws DatabaseException {

        EntityStore store = open(false /*addConverter*/);

        PrimaryIndex<Long, MyEntity> index =
            store.getPrimaryIndex(Long.class, MyEntity.class);

        MyEntity entity = new MyEntity();
        entity.key = 1;
        entity.b = 123;
        index.put(entity);

        close(store);
    }

    @Entity(version=1)
    static class MyEntity {

        @PrimaryKey
        long key;

        int a; // added in version 1
        int b;

        private MyEntity() {}
    }

    @SuppressWarnings("serial")
    public static class MyConversion implements Conversion {

        public void initialize(EntityModel model) {
        }

        public Object convert(Object fromValue) {
            return fromValue;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof MyConversion;
        }
    }
}
