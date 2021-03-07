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

package je;

import java.lang.Thread;
import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentMutableConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

/**
 * SimpleExample creates a database environment, a database, and a database
 * cursor, inserts and retrieves data.
 */
class SimpleExample {
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_FAILURE = 1;

    private int numRecords;   // num records to insert or retrieve
    private int offset;       // where we want to start inserting
    private boolean doInsert; // if true, insert, else retrieve
    private File envDir;

    public SimpleExample(int numRecords,
                         boolean doInsert,
                         File envDir,
                         int offset) {
        this.numRecords = numRecords;
        this.doInsert = doInsert;
        this.envDir = envDir;
        this.offset = offset;
    }

    /**
     * Usage string
     */
    public static void usage() {
        System.out.println("usage: java " +
                           "je.SimpleExample " +
                           "<dbEnvHomeDirectory> " +
                           "<insert|retrieve> <numRecords> [offset]");
        System.exit(EXIT_FAILURE);
    }

    /**
     * Main
     */
    public static void main(String argv[]) {

        if (argv.length < 2) {
            usage();
            return;
        }
        File envHomeDirectory = new File(argv[0]);

        boolean doInsertArg = false;
        if (argv[1].equalsIgnoreCase("insert")) {
            doInsertArg = true;
        } else if (argv[1].equalsIgnoreCase("retrieve")) {
            doInsertArg = false;
        } else {
            usage();
        }

        int startOffset = 0;
        int numRecordsVal = 0;

        if (argv.length > 2) {
            numRecordsVal = Integer.parseInt(argv[2]);
        } else {
            usage();
            return;
        }

        if (doInsertArg) {

            if (argv.length > 3) {
                startOffset = Integer.parseInt(argv[3]);
            }
        }

        try {
            SimpleExample app = new SimpleExample(numRecordsVal,
                                                  doInsertArg,
                                                  envHomeDirectory,
                                                  startOffset);
            app.run();
        } catch (DatabaseException e) {
            e.printStackTrace();
            System.exit(EXIT_FAILURE);
        }
        System.exit(EXIT_SUCCESS);
    }

    /**
     * Insert or retrieve data
     */
    public void run() throws DatabaseException {
        /* Create a new, transactional database environment */
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);

        envConfig.setCachePercent(90 /*percent*/);

        Environment exampleEnv = new Environment(envDir, envConfig);

        /*
         * Make a database within that environment
         *
         * Notice that we use an explicit transaction to
         * perform this database open, and that we
         * immediately commit the transaction once the
         * database is opened. This is required if we
         * want transactional support for the database.
         * However, we could have used autocommit to
         * perform the same thing by simply passing a
         * null txn handle to openDatabase().
         */
        Transaction txn = null; //exampleEnv.beginTransaction(null, null);
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(true);
        dbConfig.setDeferredWrite(false);
        Database exampleDb = exampleEnv.openDatabase(null, "simpleDb", dbConfig);
        //txn.commit();

        System.out.println(" *** foobarbaz: db bench starting up!");

        /*
         * Insert or retrieve data. In our example, database records are
         * integer pairs.
         */

        /* DatabaseEntry represents the key and data of each record */
        DatabaseEntry keyEntry = new DatabaseEntry();
        //DatabaseEntry dataEntry = new DatabaseEntry();
        DatabaseEntry dataEntry = new DatabaseEntry(new byte[1 << 14]);

        if (doInsert) {

            txn = null;
            /* put some data in */
            for (int i = offset; i < numRecords + offset; i++) {
                /*
                 * Note that autocommit mode, described in the Getting
                 * Started Guide, is an alternative to explicitly
                 * creating the transaction object.
                 */
                //txn = exampleEnv.beginTransaction(null, null);

                /* Use a binding to convert the int into a DatabaseEntry. */

                IntegerBinding.intToEntry(i, keyEntry);
                //IntegerBinding.intToEntry(i+1, dataEntry);
                OperationStatus status = exampleDb.put(txn, keyEntry, dataEntry);

                /*
                 * Note that put will throw a DatabaseException when
                 * error conditions are found such as deadlock.
                 * However, the status return conveys a variety of
                 * information. For example, the put might succeed,
                 * or it might not succeed if the record alread exists
                 * and the database was not configured for duplicate
                 * records.
                 */
                if (status != OperationStatus.SUCCESS) {
                    throw new RuntimeException("Data insertion got status " +
                                               status);
                }
                //txn.commit();

                /*
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {}
                */
                
                if ( i > 0 && 0 == i % 1000 ) System.out.println(" *** foobarbaz: Inserted datum "+i); // jld
            }
        } else {
            /* retrieve the data */
            Cursor cursor = exampleDb.openCursor(null, null);
            System.out.println(" *** foobarbaz: Retrieving data...");

            for (int i = 0; i < numRecords; i++) {
                int r = ThreadLocalRandom.current().nextInt(0, 2000);
                IntegerBinding.intToEntry(r, keyEntry);
                OperationStatus status = cursor.getSearchKey(keyEntry, dataEntry, null);

                if (status != OperationStatus.SUCCESS) {
                    /*
                    System.out.println("key=" +
                                       IntegerBinding.entryToInt(keyEntry) +
                                       " data=" +
                                       IntegerBinding.entryToInt(dataEntry));
                    */
                    System.out.println(" *** foobarbaz: Read error: " + status + " key:" + r);
                }

                if ( i > 0 && 0 == i % 10 ) System.out.println(" *** foobarbaz: Completed search "+i); // jld
            }
            cursor.close();
        }

        exampleDb.close();
        exampleEnv.close();

    }
}
