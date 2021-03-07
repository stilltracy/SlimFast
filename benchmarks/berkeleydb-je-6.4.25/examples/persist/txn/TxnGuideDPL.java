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

// File TxnGuideDPL.java

package persist.txn;

import java.io.File;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;

public class TxnGuideDPL {

    private static String myEnvPath = "./";
    private static String storeName = "exampleStore";

    // Handles
    private static EntityStore myStore = null;
    private static Environment myEnv = null;

    private static final int NUMTHREADS = 5;

    private static void usage() {
        System.out.println("TxnGuideDPL [-h <env directory>]");
        System.exit(-1);
    }

    public static void main(String args[]) {
        try {
            // Parse the arguments list
            parseArgs(args);
            // Open the environment and store
            openEnv();

            // Start the threads
            StoreWriter[] threadArray;
            threadArray = new StoreWriter[NUMTHREADS];
            for (int i = 0; i < NUMTHREADS; i++) {
                threadArray[i] = new StoreWriter(myEnv, myStore);
                threadArray[i].start();
            }

            for (int i = 0; i < NUMTHREADS; i++) {
                threadArray[i].join();
            }
        } catch (Exception e) {
            System.err.println("TxnGuideDPL: " + e.toString());
            e.printStackTrace();
        } finally {
            closeEnv();
        }
        System.out.println("All done.");
    }

    private static void openEnv() throws DatabaseException {
        System.out.println("opening env and store");

        // Set up the environment.
        EnvironmentConfig myEnvConfig = new EnvironmentConfig();
        myEnvConfig.setAllowCreate(true);
        myEnvConfig.setTransactional(true);
        //  Environment handles are free-threaded by default in JE,
        // so we do not have to do anything to cause the
        // environment handle to be free-threaded.

        // Set up the entity store
        StoreConfig myStoreConfig = new StoreConfig();
        myStoreConfig.setAllowCreate(true);
        myStoreConfig.setTransactional(true);

        // Open the environment
        myEnv = new Environment(new File(myEnvPath),    // Env home
                                    myEnvConfig);

        // Open the store
        myStore = new EntityStore(myEnv, storeName, myStoreConfig);

    }

    private static void closeEnv() {
        System.out.println("Closing env and store");
        if (myStore != null ) {
            try {
                myStore.close();
            } catch (DatabaseException e) {
                System.err.println("closeEnv: myStore: " + 
                    e.toString());
                e.printStackTrace();
            }
        }

        if (myEnv != null ) {
            try {
                myEnv.close();
            } catch (DatabaseException e) {
                System.err.println("closeEnv: " + e.toString());
                e.printStackTrace();
            }
        }
    }

    private TxnGuideDPL() {}

    private static void parseArgs(String args[]) {
        int nArgs = args.length;
        for(int i = 0; i < args.length; ++i) {
            if (args[i].startsWith("-")) {
                switch(args[i].charAt(1)) {
                    case 'h':
                        if (i < nArgs - 1) {
                            myEnvPath = new String(args[++i]);
                        }
                    break;
                    default:
                        usage();
                }
            }
        }
    }
}
