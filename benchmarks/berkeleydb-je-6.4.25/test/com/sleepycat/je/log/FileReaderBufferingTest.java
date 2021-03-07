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

package com.sleepycat.je.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.Test;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.je.utilint.DbLsn;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;
import com.sleepycat.utilint.StringUtils;

/**
 * Check our ability to adjust the file reader buffer size.
 */
public class FileReaderBufferingTest extends TestBase {

    private final File envHome;
    private Environment env;
    private EnvironmentImpl envImpl;
    private ArrayList<Long> expectedLsns;
    private ArrayList<String> expectedVals;

    public FileReaderBufferingTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    /**
     * Should overflow once and then grow.
     */
    @Test
    public void testBasic()
        throws Exception {

        readLog(1050,   // starting size of object in entry
                0,      // object growth increment
                100,    // starting read buffer size
                "3000", // max read buffer size
                0);     // expected number of overflows.
    }

    /**
     * Should overflow once and then grow.
     */
    @Test
    public void testCantGrow()
        throws Exception {

        readLog(2000,   // starting size of object in entry
                0,      // object growth increment
                100,    // starting read buffer size
                "1000", // max read buffer size
                10);    // expected number of overflows.
    }

    /**
     * Should overflow, grow, and then reach the max.
     */
    @Test
    public void testReachMax()
        throws Exception {

        readLog(1000,   // size of object in entry
                1000,      // object growth increment
                100,    // starting read buffer size
                "3500", // max read buffer size
                7);     // expected number of overflows.
    }
    /**
     *
     */
    private void readLog(int entrySize,
                         int entrySizeIncrement,
                         int readBufferSize,
                         String bufferMaxSize,
                         int expectedOverflows)
        throws Exception {

        try {

            EnvironmentConfig envConfig = TestUtils.initEnvConfig();
            envConfig.setAllowCreate(true);
            envConfig.setConfigParam
                (EnvironmentParams.LOG_ITERATOR_MAX_SIZE.getName(),
                 bufferMaxSize);
            env = new Environment(envHome, envConfig);

            envImpl = DbInternal.getEnvironmentImpl(env);

            /* Make a log file */
            createLogFile(10, entrySize, entrySizeIncrement);
            SearchFileReader reader =
                new SearchFileReader(envImpl,
                                     readBufferSize,
                                     true,
                                     DbLsn.longToLsn
                                     (expectedLsns.get(0)),
                                     DbLsn.NULL_LSN,
                                     LogEntryType.LOG_TRACE);

            Iterator<Long> lsnIter = expectedLsns.iterator();
            Iterator<String> valIter = expectedVals.iterator();
            while (reader.readNextEntry()) {
                Trace rec = (Trace) reader.getLastObject();
                assertTrue(lsnIter.hasNext());
                assertEquals(reader.getLastLsn(),
                             DbLsn.longToLsn(lsnIter.next()));
                assertEquals(valIter.next(), rec.getMessage());
            }
            assertEquals(10, reader.getNumRead());
            assertEquals(expectedOverflows, reader.getNRepeatIteratorReads());

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            env.close();
        }
    }

    /**
     * Write a logfile of entries, put the entries that we expect to
     * read into a list for later verification.
     * @return end of file LSN.
     */
    private void createLogFile(int numItems, int size, int sizeIncrement)
        throws IOException, DatabaseException {

        LogManager logManager = envImpl.getLogManager();
        expectedLsns = new ArrayList<Long>();
        expectedVals = new ArrayList<String>();

        for (int i = 0; i < numItems; i++) {
            /* Add a debug record just to be filler. */
            int recordSize = size + (i * sizeIncrement);
            byte[] filler = new byte[recordSize];
            Arrays.fill(filler, (byte)i);
            String val = StringUtils.fromUTF8(filler);

            Trace rec = new Trace(val);
            long lsn = rec.trace(envImpl, rec);
            expectedLsns.add(new Long(lsn));
            expectedVals.add(val);
        }

        logManager.flush();
        envImpl.getFileManager().clear();
    }
}
