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

package persist;

import java.io.File;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.IndexNotAvailableException;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.EntityModel;
import com.sleepycat.persist.raw.RawObject;
import com.sleepycat.persist.raw.RawStore;
import com.sleepycat.persist.raw.RawType;

/**
 * Dumps a store or all stores to standard output in raw XML format.  This
 * sample is intended to be modifed to dump in application specific ways.
 * @see #usage
 */
public class DplDump {

    private File envHome;
    private String storeName;
    private boolean dumpMetadata;
    private Environment env;

    public static void main(String[] args) {
        try {
            DplDump dump = new DplDump(args);
            dump.open();
            dump.execute();
            dump.close();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private DplDump(String[] args) {

        for (int i = 0; i < args.length; i += 1) {
            String name = args[i];
            String val = null;
            if (i < args.length - 1 && !args[i + 1].startsWith("-")) {
                i += 1;
                val = args[i];
            }
            if (name.equals("-h")) {
                if (val == null) {
                    usage("No value after -h");
                }
                envHome = new File(val);
            } else if (name.equals("-s")) {
                if (val == null) {
                    usage("No value after -s");
                }
                storeName = val;
            } else if (name.equals("-meta")) {
                dumpMetadata = true;
            } else {
                usage("Unknown arg: " + name);
            }
        }

        if (envHome == null) {
            usage("-h not specified");
        }
    }

    private void usage(String msg) {

        if (msg != null) {
            System.out.println(msg);
        }

        System.out.println
            ("usage:" +
             "\njava "  + DplDump.class.getName() +
             "\n   -h <envHome>" +
             "\n      # Environment home directory" +
             "\n  [-meta]" +
             "\n      # Dump metadata; default: false" +
             "\n  [-s <storeName>]" +
             "\n      # Store to dump; default: dump all stores");

        System.exit(2);
    }

    private void open()
        throws DatabaseException {

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setReadOnly(true);
        env = new Environment(envHome, envConfig);
    }

    private void close()
        throws DatabaseException {

        env.close();
    }

    private void execute()
        throws DatabaseException {

        if (storeName != null) {
            dump();
        } else {
            for (String name : EntityStore.getStoreNames(env)) {
                storeName = name;
                dump();
            }
        }
    }

    private void dump()
        throws DatabaseException {

        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setReadOnly(true);
        RawStore store = new RawStore(env, storeName, storeConfig);

        EntityModel model = store.getModel();
        if (dumpMetadata) {
            for (RawType type : model.getAllRawTypes()) {
                System.out.println(type);
            }
        } else {
            for (String clsName : model.getKnownClasses()) {
                if (model.getEntityMetadata(clsName) != null) {
                    final PrimaryIndex<Object,RawObject> index;
                    try {
                        index = store.getPrimaryIndex(clsName);
                    } catch (IndexNotAvailableException e) {
                        System.err.println("Skipping primary index that is " +
                                           "not yet available: " + clsName);
                        continue;
                    }
                    EntityCursor<RawObject> entities = index.entities();
                    for (RawObject entity : entities) {
                        System.out.println(entity);
                    }
                    entities.close();
                }
            }
        }

        store.close();
    }
}
