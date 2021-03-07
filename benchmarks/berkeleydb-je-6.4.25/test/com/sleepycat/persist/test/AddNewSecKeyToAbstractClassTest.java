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

import static com.sleepycat.persist.model.Relationship.MANY_TO_ONE;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;
import com.sleepycat.util.test.TestEnv;

/*
 * A unit test for testing adding a new seconday key to an abstract entity 
 * class. There are two test cases, one is to add a null seconday key, the 
 * other is to add a non-null secondary key. [#19385]
 */ 

public class AddNewSecKeyToAbstractClassTest extends TestBase {

    private static final String STORE_NAME = "test";

    private File envHome;
    private Environment env;
    private EntityStore store;
    private static boolean notNull = true;

    
   
    @Before
    public void setUp() 
        throws Exception {

        envHome = SharedTestUtils.getTestDir();
        super.setUp();
    }

    @After
    public void tearDown() {
        if (store != null) {
            try {
                store.close();
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
        envHome = null;
        env = null;
        store = null;
    }

    private void open()
        throws DatabaseException {

        EnvironmentConfig envConfig = TestEnv.BDB.getConfig();
        envConfig.setAllowCreate(true);
        env = new Environment(envHome, envConfig);

        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        store = new EntityStore(env, STORE_NAME, storeConfig);
    }

    private void close()
        throws DatabaseException {

        if (store != null) {
            store.close();
            store = null;
        }
        if (env != null) {
            env.close();
            env = null;
        }
    }

    @Test
    public void testAddNullSecKeyToAbstractClass()
        throws IOException {
        
        /* Copy log file resource to log file zero. */
        TestUtils.loadLog(getClass(), "je-4.0.103_AbstractClassData.jdb",
                          envHome);
        notNull = false;
        open();
        
        /* 
         * Adding a new but null secondary key into an abstract class is 
         * allowed.
         */
        PrimaryIndex<Long, AbstractEntity1> primary = 
            store.getPrimaryIndex(Long.class, AbstractEntity1.class);     
        AbstractEntity1 entity = primary.put(null, new EntityData1(1));
        assertNotNull(entity);
        entity = primary.put(null, new EntityData1(2));
        assertTrue(entity == null);
        close();
    }
    
    public void xxtestAddNotNullSecKeyToAbstractClass()
        throws IOException {
    
        /* Copy log file resource to log file zero. */
        TestUtils.loadLog(getClass(), "je-4.0.103_AbstractClassData.jdb",
                          envHome);
        notNull = true;
        
        try {
            open();
            PrimaryIndex<Long, AbstractEntity2> primary = 
                store.getPrimaryIndex(Long.class, AbstractEntity2.class);     
            fail();
        } catch(Exception e) {
            
            /* 
             * Expected exception. Adding a new but not null secondary key into
             * an abstract class is not allowed.
             */
             close();
        }
    }
    
    @Entity(version = 1)
    static abstract class AbstractEntity1 {
        AbstractEntity1(Long i) {
            this.id = i;
        }
        
        private AbstractEntity1(){}
        
        @PrimaryKey
        private Long id;
        
        // Adding a null SecondaryKey.
        @SecondaryKey( relate = MANY_TO_ONE )
        private Long sk1;
    }
    
    @Persistent(version = 1)
    static class EntityData1 extends AbstractEntity1{
        private int f1;
        
        private EntityData1(){}
        
        EntityData1(int i) {
            super(Long.valueOf(i));
            this.f1 = i;
        }
    }
    
    @Entity(version = 1)
    static abstract class AbstractEntity2 {
        AbstractEntity2(Long i) {
            this.id = i;
        }
        
        private AbstractEntity2(){}
        
        @PrimaryKey
        private Long id;
        
        // Adding a non-null SecondaryKey.
        @SecondaryKey( relate = MANY_TO_ONE )
        private final Long sk1 = notNull ? 1L : null;
    }
    
    @Persistent(version = 1)
    static class EntityData2 extends AbstractEntity2{
        private int f1;
        
        private EntityData2(){}
        
        EntityData2(int i) {
            super(Long.valueOf(i));
            this.f1 = i;
        }
    }
}
