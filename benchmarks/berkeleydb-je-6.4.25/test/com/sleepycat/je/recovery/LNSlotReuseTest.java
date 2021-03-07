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

package com.sleepycat.je.recovery;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * Test recovery redo of a LN, when the redo provokes slot reuse.
 */
public class LNSlotReuseTest extends TestBase {
    private final File envHome;

    public LNSlotReuseTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    /**
     * This test was motivated by SR [#17770], which had to do with the
     * fact recovery redos were not appropriately clearing the known deleted
     * and pending deleted fields in the BIN. When a slot is reused, those
     * bits must be initialized properly so the LN does not erroneously seem
     * be deleted.
     * 
     * This unit test is trying to generate the following log sequence:
     * 100 LNA (key xxx) is inserted
     * 110 txn commit for insert of LNA
     * 120 LNA is deleted
     * 125 checkpoint start
     * 130 BIN for key xxx, pending deleted bit for LNA is set, slot 
     *     points to lsn 120.
     * 135 checkpoint end
     * 140 txn commit for delete of LNA (in memory, BIN's known deleted bit 
           is set, but it's not set in the log)
     * 150 LNB (key xxx) is inserted, goes into slot for LNA.
     * 160 txn commit for LNB.
     *
     * The goal is to provoke a recovery that runs from lsn 125->160. LNB is 
     * committed, but goes into a slot previously occupied by LNA. Since LNB's
     * pending deleted state is incorrectly set, a call to Database.count()
     * skips over the slot.
     */
    @Test
    public void testLNSlotReuse() 
        throws Exception {

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        envConfig.setDurability(Durability.COMMIT_WRITE_NO_SYNC);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(true);

        Environment env = null;
        Database db = null;

        try {
            env = new Environment(envHome, envConfig);
            db = env.openDatabase(null, "testDB", dbConfig);

            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry data = new DatabaseEntry();
            IntegerBinding.intToEntry(1024, key);
            StringBinding.stringToEntry("herococo", data);
        
            /* 
             * Insert and delete a record, so our BIN will have a slot with
             * pending deleted set.
             */
            Transaction txn = env.beginTransaction(null, null);
            db.put(txn, key, data); // insert record A
            txn.commit();
            txn = env.beginTransaction(null, null);
            db.delete(txn, key);  // delete record A

            /* Checkpoint to flush our target BIN out to disk. */
            CheckpointConfig ckptConfig = new CheckpointConfig();
            ckptConfig.setForce(true);
            env.checkpoint(ckptConfig);

            /* 
             * Committing the deletion after the checkpoint means the BIN will
             * go out with Pending Deleted set. If we commit before the
             * checkpoint, the BIN will be compressed, the slot will be
             * deleted, and we won't exercise slot reuse.
             */
            txn.commit();         

            /* Insert record B and  reuse the slot previously held by A */
            txn = env.beginTransaction(null, null);
            db.put(txn, key, data); 
            txn.commit();
            db.close();

            /* Simulate a crash. */
            EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);
            envImpl.close(false);
            
            /* Do a recovery */
            env = new Environment(envHome, envConfig);
            db = env.openDatabase(null, "testDB", dbConfig);

            /* 
             * Compare counts obtained via a cursor traveral to a count from
             * Database.count() The expected value is 1.
             */
            Cursor cursor = db.openCursor(null, null);
            int counter = 0;
            while (OperationStatus.SUCCESS == 
                   cursor.getNext(key, data, null)) {
                counter++;
            }
            cursor.close();

            /* 
             * We expect the count to be 1, and we expect the two methods to
             * be equal.
             */
            assertEquals(1, counter);
            assertEquals(counter, db.count());
        } finally {
            if (db != null) {
                db.close();
            }

            if (env != null) {
                env.close();
            }
        }
    }
}
