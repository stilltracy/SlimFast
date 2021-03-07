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

package com.sleepycat.je.rep.util.ldiff;

import java.io.File;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class LDiffTestUtils {
    private static final byte[] keyArr = {0, 0, 0, 0};
    public static final byte[] dataArr =
        { (byte) 0xdb, (byte) 0xdb, (byte) 0xdb, (byte) 0xdb };

    /* Delete a directory. */
    public static void deleteDir(File dir) {
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                f.delete();
            }
            dir.delete();
        }
    }

    /* Create an Environment. */
    public static Environment createEnvironment(File envHome) 
        throws Exception {

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);

        return new Environment(envHome, envConfig);
    }

    /* Create a database. */
    public static Database createDatabase(Environment env, String dbName) 
        throws Exception {

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);

        return env.openDatabase(null, dbName, dbConfig);
    }

    public static byte[] insertRecords(Database db,
                                       int iters)
        throws Exception {

        return insertRecords(db, iters, keyArr, dataArr);
    }

    public static byte[] insertRecords(Database db,
                                       int iters,
                                       byte[] data,
                                       boolean key) 
        throws Exception {

        if (key) {
            return insertRecords(db, iters, data, dataArr);
        } else {
            return insertRecords(db, iters, keyArr, data);
        }
    }

    /* Insert records. */
    public static byte[] insertRecords(Database db, 
                                       int iters, 
                                       byte[] usedKeys,
                                       byte[] usedData) 
        throws Exception {

        byte[] keys = new byte[usedKeys.length];
        System.arraycopy(usedKeys, 0, keys, 0, usedKeys.length);
        for (int i = 0; i < iters; i++) {
            db.put(null, new DatabaseEntry(keys), new DatabaseEntry(usedData));
            if (++keys[3] == 0) 
                if (++keys[2] == 0) 
                    if (++keys[1] == 0) 
                        keys[0]++;
        }

        return keys;
    }

    /* Insert records, let a database has some additional blocks. */
    public static void insertAdditionalRecords(Database db1,
                                               Database db2)
        throws Exception {

        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry data = new DatabaseEntry();
        for (int i = 101; i <= 200; i++) {
            IntegerBinding.intToEntry(i, key);
            StringBinding.stringToEntry("herococo", data);
            if (i <= 150 || i >= 171) {
                db1.put(null, key, data);
            }
        }

        for (int i = 1; i <= 296; i++) {
            IntegerBinding.intToEntry(i, key);
            StringBinding.stringToEntry("herococo", data);
            db2.put(null, key, data);
        }
    }

    /* Insert different records into two databases. */
    public static void insertDifferentRecords(Database db1,
                                              Database db2)
        throws Exception {

        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry data = new DatabaseEntry();
        for (int i = 1; i <= 296; i++) {
            IntegerBinding.intToEntry(i, key);
            StringBinding.stringToEntry("herococo", data);
            db1.put(null, key, data);
        }

        for (int i = 1; i <= 290; i++) {
            IntegerBinding.intToEntry(i, key);
            StringBinding.stringToEntry("herococo", data);
            if (i <= 5) {
                StringBinding.stringToEntry("hero-coco", data);
            }
            if (i >= 55 && i <= 80) {
                StringBinding.stringToEntry("hero-coco", data);
            }
            if (i == 100) {
                StringBinding.stringToEntry("hero-coco", data);
            }
            if (i >= 271 && i <= 290) {
                StringBinding.stringToEntry("hero-coco", data);
            }
            db2.put(null, key, data);
        }
    }


    /* Insert the records with keys which are different from keyArr. */
    public static void insertWithDiffKeys(Database db,
                                          int iters,
                                          byte[] usedKeys)
        throws Exception {

        byte[] keys = new byte[usedKeys.length];
        System.arraycopy(usedKeys, 0, keys, 0, usedKeys.length);
        for (int i = 0; i < iters; i++) {
            db.put(null, new DatabaseEntry(keys), new DatabaseEntry(dataArr));
            if (--keys[3] == 0)
                if (--keys[2] == 0)
                    if (--keys[1] == 0)
                        keys[0]--;
        }
    }
}
