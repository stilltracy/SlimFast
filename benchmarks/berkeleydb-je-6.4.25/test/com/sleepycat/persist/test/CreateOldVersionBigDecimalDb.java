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
import java.math.BigDecimal;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.AnnotationModel;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.EntityModel;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PersistentProxy;
import com.sleepycat.persist.model.PrimaryKey;

/* 
 * Create an old version (before je-4.1) database, which stores BigDecimal data
 * using a proxy class. This database will be used in the unit test 
 * com.sleepycat.persist.test.ProxyToSimpleTypeTest.
 */
public class CreateOldVersionBigDecimalDb {
    
    private Environment env;
    private EntityStore store;
    private PrimaryIndex<Integer, BigDecimalData> primary;
    
    public static void main(String args[]) {
        CreateOldVersionBigDecimalDb sbd = new CreateOldVersionBigDecimalDb();
        sbd.open();
        sbd.writeData();
        sbd.close();
        sbd.open();
        sbd.getData();
        sbd.close();
    }
    
    private void writeData() {
        primary.put(null, 
                    new BigDecimalData (1, new BigDecimal("123.1234000")));
    }
    
    private void getData() {
        BigDecimalData data = primary.get(1);
        System.out.println(data.getF1());
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
        EntityModel model = new AnnotationModel();
        model.registerClass(BigDecimalProxy.class);
        StoreConfig config = new StoreConfig();
        config.setAllowCreate(envConfig.getAllowCreate());
        config.setTransactional(envConfig.getTransactional());
        config.setModel(model);
        store = new EntityStore(env, "test", config);
        primary = store.getPrimaryIndex(Integer.class, BigDecimalData.class);
    }

    @Entity
    static class BigDecimalData {
        @PrimaryKey
        private int id;
        private BigDecimal f1;
        
        BigDecimalData() { }
        
        BigDecimalData(int id, BigDecimal f1) {
            this.id = id;
            this.f1 = f1;
        }
        
        int getId() {
            return id;
        }
        
        BigDecimal getF1() {
            return f1;
        }
    }
    
    @Persistent(proxyFor=BigDecimal.class)
    static class BigDecimalProxy
        implements PersistentProxy<BigDecimal> {

        private String rep;
        private BigDecimalProxy() {}
        public BigDecimal convertProxy() {
            return new BigDecimal(rep);
        }

        public void initializeProxy(BigDecimal o) {
            rep = o.toString();
        }
    }
}