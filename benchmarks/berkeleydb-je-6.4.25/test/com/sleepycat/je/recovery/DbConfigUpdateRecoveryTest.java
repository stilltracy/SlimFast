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
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.log.LNFileReader;
import com.sleepycat.je.log.LogEntryType;
import com.sleepycat.je.log.LogManager;
import com.sleepycat.je.log.ReplicationContext;
import com.sleepycat.je.log.Trace;
import com.sleepycat.je.log.entry.DbOperationType;
import com.sleepycat.je.log.entry.LogEntry;
import com.sleepycat.je.log.entry.NameLNLogEntry;
import com.sleepycat.je.log.entry.TraceLogEntry;
import com.sleepycat.je.util.DbTruncateLog;
import com.sleepycat.je.utilint.DbLsn;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

public class DbConfigUpdateRecoveryTest extends TestBase {
   private static final String DB_NAME = "testDb";

   private final File envHome;
   private Environment env;
   private Database db;

   public DbConfigUpdateRecoveryTest() {
       envHome = SharedTestUtils.getTestDir();
   }

   @Override
   @After
   public void tearDown() {
       
       try {
           if (db != null) {
               db.close();
           }
           if (env != null) {
               env.close();
           }
       } catch (Exception e) {
           e.printStackTrace();
       }
   }

   /* Test in a transactional Environment. */
   @Test
   public void testTransactional()
       throws Exception {

       doTest(true);
   }

   /* Test in a non-transactional Environment. */
   @Test
   public void testNonTransactional()
       throws Exception {

       doTest(false);
   }

   /*
    * This test is exercising the following recovery scenario, discussed in SR
    * [#18262]. An update of a database configuration results in the logging 
    * of a NameLN, followed by a MapLN. Since MapLNs are always 
    * non-transactional, there is nothing that links the NameLN and the MapLN.
    * If the MapLN is not flushed to disk, and the NameLN alone is within the
    * recovery processing part of the log, we expect recovery to succeed, and
    * that the configuration change will not be persisted.
    *
    * This test will do the following things:
    * 1. Open a database and do database config updates.
    * 2. Write a tracer entry right after the updates so that the file is 
    *    flipped, and we can be sure that there is no checkpoint after the 
    *    logged MapLN, and that the NameLN will be in the recovery period.
    * 3. Use the LNFileReader to read the updated NameLNLogEntry, and calculate
    *    the lsn of the entry right after the NameLNLogEntry.
    * 4. Close the environment without doing a checkpoint.
    * 5. Use DbTruncateLog to truncate log entries after the updated 
    *    NameLNLogEntry.
    * 6. Open the Environment again to see if the database config has updated.
    */
   private void doTest(boolean transactional) 
       throws Exception {

       EnvironmentConfig envConfig = new EnvironmentConfig();
       envConfig.setAllowCreate(true);
       envConfig.setTransactional(transactional);

       env = new Environment(envHome, envConfig);

       DatabaseConfig dbConfig = new DatabaseConfig();
       dbConfig.setAllowCreate(true);
       dbConfig.setTransactional(transactional);

       /* Open a database. */
       db = env.openDatabase(null, DB_NAME, dbConfig);
       db.close();

       /* Update the DatabaseConfig. */
       dbConfig.setNodeMaxEntries(512);
       db = env.openDatabase(null, DB_NAME, dbConfig);
       assertEquals(512, db.getConfig().getNodeMaxEntries());
       db.close();

       /* Flush the updated NameLN and MapLN. */
       EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);
       LogManager logManager = envImpl.getLogManager();
       Trace tracer = new Trace("test message");
       LogEntry tracerEntry = new TraceLogEntry(tracer);
       logManager.logForceFlush
           (tracerEntry, false, ReplicationContext.NO_REPLICATE);

       /* Use FileReader to get the start lsn for the deleted entry. */
       LNFileReader reader = new LNFileReader(envImpl, 
                                              1000, 
                                              DbLsn.NULL_LSN,
                                              true,
                                              DbLsn.NULL_LSN,
                                              DbLsn.NULL_LSN,
                                              null,
                                              DbLsn.NULL_LSN);
       reader.addTargetType(LogEntryType.LOG_NAMELN_TRANSACTIONAL);
       reader.addTargetType(LogEntryType.LOG_NAMELN);

       /* Get the truncation start lsn. */
       long deleteLsn = 0;
       while (reader.readNextEntry()) {
           NameLNLogEntry entry = (NameLNLogEntry) reader.getLNLogEntry();
           if (entry.getOperationType() == DbOperationType.UPDATE_CONFIG) {
               deleteLsn = reader.getLastLsn() + reader.getLastEntrySize();
           }
       }

       assertTrue(deleteLsn > 0);

       /* Close the Environment without doing a checkpoint. */
       envImpl.close(false);

       /* If not delete, the updated config can be recovered. */
       env = new Environment(envHome, envConfig);
       
       dbConfig = new DatabaseConfig();
       dbConfig.setTransactional(transactional);
       dbConfig.setUseExistingConfig(true);

       db = env.openDatabase(null, DB_NAME, dbConfig);
       assertEquals(512, db.getConfig().getNodeMaxEntries());

       /* 
        * Close the database and Environment, because the DbTruncateLog needs 
        * to open an Environment. 
        */
       db.close();
       env.close();

       /* Use DbTruncateLog deletes entries right after the updated NameLN. */
       DbTruncateLog truncate = new DbTruncateLog();
       truncate.truncateLog(envHome, 
                            DbLsn.getFileNumber(deleteLsn), 
                            DbLsn.getFileOffset(deleteLsn));

       /* Open the Environment and database to see the updates are lost. */
       env = new Environment(envHome, envConfig);

       dbConfig = new DatabaseConfig();
       dbConfig.setUseExistingConfig(true);

       db = env.openDatabase(null, DB_NAME, dbConfig);
       assertTrue(db.getConfig().getNodeMaxEntries() != 512);
   }
}
