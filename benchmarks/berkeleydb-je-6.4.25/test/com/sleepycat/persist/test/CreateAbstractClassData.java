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

import java.io.File;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;

/* 
 * Create a database which stores abstract entity classes. This database will be 
 * used in the unit test c.s.persist.test.AddNewSecKeyToAbstractClassTest.
 */
public class CreateAbstractClassData {
    private Environment env;
    private EntityStore store;
    private PrimaryIndex<Long, AbstractEntity1> primary1;
    private PrimaryIndex<Long, AbstractEntity2> primary2;
    
    public static void main(String args[]) {
        CreateAbstractClassData epc = new CreateAbstractClassData();
        epc.open();
        epc.writeData();
        epc.close();
    }
    
    private void writeData() {
        primary1.put(null, new EntityData1(1));
        primary2.put(null, new EntityData2(1));
    }
    
    private void close() {
        store.close();
        store = null;

        env.close();
        env = null;
    }

    private void open() {

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        File envHome = new File("./");
        env = new Environment(envHome, envConfig);
        StoreConfig config = new StoreConfig();
        config.setAllowCreate(envConfig.getAllowCreate());
        config.setTransactional(envConfig.getTransactional());
        store = new EntityStore(env, "test", config);
        primary1 = store.getPrimaryIndex(Long.class, AbstractEntity1.class);
        primary2 = store.getPrimaryIndex(Long.class, AbstractEntity2.class);
    }
    
    @Entity
    static abstract class AbstractEntity1 {
        AbstractEntity1(Long i) {
            this.id = i;
        }
        
        private AbstractEntity1(){}
        
        @PrimaryKey
        private Long id;
    }
    
    @Persistent
    static class EntityData1 extends AbstractEntity1{
        private int f1;
        
        private EntityData1(){}
        
        EntityData1(int i) {
            super(Long.valueOf(i));
            this.f1 = i;
        }
    }
    
    @Entity
    static abstract class AbstractEntity2 {
        AbstractEntity2(Long i) {
            this.id = i;
        }
        
        private AbstractEntity2(){}
        
        @PrimaryKey
        private Long id;
    }
    
    @Persistent
    static class EntityData2 extends AbstractEntity2{
        private int f1;
        
        private EntityData2(){}
        
        EntityData2(int i) {
            super(Long.valueOf(i));
            this.f1 = i;
        }
    }
}
