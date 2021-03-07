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

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.util.DualTestCase;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.KeyField;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestEnv;

/**
 * @author Mark Hayes
 */
public class SequenceTest extends DualTestCase {

    private File envHome;
    private Environment env;

    @Before
    public void setUp()
        throws Exception {

        super.setUp();
        envHome = SharedTestUtils.getTestDir();
    }

    @After
    public void tearDown()
        throws Exception {

        super.tearDown();
        envHome = null;
        env = null;
    }

    @Test
    public void testSequenceKeys()
        throws Exception {

        Class[] classes = {
            SequenceEntity_Long.class,
            SequenceEntity_Integer.class,
            SequenceEntity_Short.class,
            SequenceEntity_Byte.class,
            SequenceEntity_tlong.class,
            SequenceEntity_tint.class,
            SequenceEntity_tshort.class,
            SequenceEntity_tbyte.class,
            SequenceEntity_Long_composite.class,
            SequenceEntity_Integer_composite.class,
            SequenceEntity_Short_composite.class,
            SequenceEntity_Byte_composite.class,
            SequenceEntity_tlong_composite.class,
            SequenceEntity_tint_composite.class,
            SequenceEntity_tshort_composite.class,
            SequenceEntity_tbyte_composite.class,
        };

        EnvironmentConfig envConfig = TestEnv.TXN.getConfig();
        envConfig.setAllowCreate(true);
        env = create(envHome, envConfig);

        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(true);
        EntityStore store = new EntityStore(env, "foo", storeConfig);

        long seq = 0;

        for (int i = 0; i < classes.length; i += 1) {
            Class entityCls = classes[i];
            SequenceEntity entity = (SequenceEntity) entityCls.newInstance();
            Class keyCls = entity.getKeyClass();

            PrimaryIndex<Object, SequenceEntity> index =
                store.getPrimaryIndex(keyCls, entityCls);
            index.putNoReturn(entity);
            seq += 1;
            assertEquals(seq, entity.getKey());

            index.putNoReturn(entity);
            assertEquals(seq, entity.getKey());

            entity.nullifyKey();
            index.putNoReturn(entity);
            seq += 1;
            assertEquals(seq, entity.getKey());
        }

        store.close();
        close(env);
        env = null;
    }

    interface SequenceEntity {
        Class getKeyClass();
        long getKey();
        void nullifyKey();
    }

    @Entity
    static class SequenceEntity_Long implements SequenceEntity {

        @PrimaryKey(sequence="X")
        Long priKey;

        public Class getKeyClass() {
            return Long.class;
        }

        public long getKey() {
            return priKey;
        }

        public void nullifyKey() {
            priKey = null;
        }
    }

    @Entity
    static class SequenceEntity_Integer implements SequenceEntity {

        @PrimaryKey(sequence="X")
        Integer priKey;

        public Class getKeyClass() {
            return Integer.class;
        }

        public long getKey() {
            return priKey;
        }

        public void nullifyKey() {
            priKey = null;
        }
    }

    @Entity
    static class SequenceEntity_Short implements SequenceEntity {

        @PrimaryKey(sequence="X")
        Short priKey;

        public Class getKeyClass() {
            return Short.class;
        }

        public long getKey() {
            return priKey;
        }

        public void nullifyKey() {
            priKey = null;
        }
    }

    @Entity
    static class SequenceEntity_Byte implements SequenceEntity {

        @PrimaryKey(sequence="X")
        Byte priKey;

        public Class getKeyClass() {
            return Byte.class;
        }

        public long getKey() {
            return priKey;
        }

        public void nullifyKey() {
            priKey = null;
        }
    }

    @Entity
    static class SequenceEntity_tlong implements SequenceEntity {

        @PrimaryKey(sequence="X")
        long priKey;

        public Class getKeyClass() {
            return Long.class;
        }

        public long getKey() {
            return priKey;
        }

        public void nullifyKey() {
            priKey = 0;
        }
    }

    @Entity
    static class SequenceEntity_tint implements SequenceEntity {

        @PrimaryKey(sequence="X")
        int priKey;

        public Class getKeyClass() {
            return Integer.class;
        }

        public long getKey() {
            return priKey;
        }

        public void nullifyKey() {
            priKey = 0;
        }
    }

    @Entity
    static class SequenceEntity_tshort implements SequenceEntity {

        @PrimaryKey(sequence="X")
        short priKey;

        public Class getKeyClass() {
            return Short.class;
        }

        public long getKey() {
            return priKey;
        }

        public void nullifyKey() {
            priKey = 0;
        }
    }

    @Entity
    static class SequenceEntity_tbyte implements SequenceEntity {

        @PrimaryKey(sequence="X")
        byte priKey;

        public Class getKeyClass() {
            return Byte.class;
        }

        public long getKey() {
            return priKey;
        }

        public void nullifyKey() {
            priKey = 0;
        }
    }

    @Entity
    static class SequenceEntity_Long_composite implements SequenceEntity {

        @PrimaryKey(sequence="X")
        Key priKey;

        @Persistent
        static class Key {
            @KeyField(1)
            Long priKey;
        }

        public Class getKeyClass() {
            return Key.class;
        }

        public long getKey() {
            return priKey.priKey;
        }

        public void nullifyKey() {
            priKey = null;
        }
    }

    @Entity
    static class SequenceEntity_Integer_composite implements SequenceEntity {

        @PrimaryKey(sequence="X")
        Key priKey;

        @Persistent
        static class Key {
            @KeyField(1)
            Integer priKey;
        }

        public Class getKeyClass() {
            return Key.class;
        }

        public long getKey() {
            return priKey.priKey;
        }

        public void nullifyKey() {
            priKey = null;
        }
    }

    @Entity
    static class SequenceEntity_Short_composite implements SequenceEntity {

        @PrimaryKey(sequence="X")
        Key priKey;

        @Persistent
        static class Key {
            @KeyField(1)
            Short priKey;
        }

        public Class getKeyClass() {
            return Key.class;
        }

        public long getKey() {
            return priKey.priKey;
        }

        public void nullifyKey() {
            priKey = null;
        }
    }

    @Entity
    static class SequenceEntity_Byte_composite implements SequenceEntity {

        @PrimaryKey(sequence="X")
        Key priKey;

        @Persistent
        static class Key {
            @KeyField(1)
            Byte priKey;
        }

        public Class getKeyClass() {
            return Key.class;
        }

        public long getKey() {
            return priKey.priKey;
        }

        public void nullifyKey() {
            priKey = null;
        }
    }

    @Entity
    static class SequenceEntity_tlong_composite implements SequenceEntity {

        @PrimaryKey(sequence="X")
        Key priKey;

        @Persistent
        static class Key {
            @KeyField(1)
            long priKey;
        }

        public Class getKeyClass() {
            return Key.class;
        }

        public long getKey() {
            return priKey.priKey;
        }

        public void nullifyKey() {
            priKey = null;
        }
    }

    @Entity
    static class SequenceEntity_tint_composite implements SequenceEntity {

        @PrimaryKey(sequence="X")
        Key priKey;

        @Persistent
        static class Key {
            @KeyField(1)
            int priKey;
        }

        public Class getKeyClass() {
            return Key.class;
        }

        public long getKey() {
            return priKey.priKey;
        }

        public void nullifyKey() {
            priKey = null;
        }
    }

    @Entity
    static class SequenceEntity_tshort_composite implements SequenceEntity {

        @PrimaryKey(sequence="X")
        Key priKey;

        @Persistent
        static class Key {
            @KeyField(1)
            short priKey;
        }

        public Class getKeyClass() {
            return Key.class;
        }

        public long getKey() {
            return priKey.priKey;
        }

        public void nullifyKey() {
            priKey = null;
        }
    }

    @Entity
    static class SequenceEntity_tbyte_composite implements SequenceEntity {

        @PrimaryKey(sequence="X")
        Key priKey;

        @Persistent
        static class Key {
            @KeyField(1)
            byte priKey;
        }

        public Class getKeyClass() {
            return Key.class;
        }

        public long getKey() {
            return priKey.priKey;
        }

        public void nullifyKey() {
            priKey = null;
        }
    }
}
