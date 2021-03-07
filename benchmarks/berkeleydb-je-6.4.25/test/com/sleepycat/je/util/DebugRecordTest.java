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

package com.sleepycat.je.util;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;

import org.junit.Test;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.log.LogEntryType;
import com.sleepycat.je.log.SearchFileReader;
import com.sleepycat.je.log.Trace;
import com.sleepycat.je.utilint.DbLsn;
import com.sleepycat.je.utilint.LoggerUtils;
import com.sleepycat.je.utilint.TracerFormatter;
import com.sleepycat.util.test.SharedTestUtils;

/**
 * This test originally ran in dual mode. After changes were made that started
 * making replication recovery run an different path, this expected entries
 * in the log began to differ substantially enough between a replicated
 * and non-replicated environment, and the dual mode version was removed.
 * If more test cases are eventually added to this test, we may want to
 * return to dual mode.
 */
public class DebugRecordTest extends DualTestCase {
    private File envHome;
    private Environment env;

    public DebugRecordTest() {
        envHome = SharedTestUtils.getTestDir();
        env = null;
    }

    @Test
    public void testDebugLogging()
        throws DatabaseException, IOException {

        try {

            /*
             * Turn on the txt file and db log logging, turn off the console.
             */
            EnvironmentConfig envConfig = TestUtils.initEnvConfig();
            envConfig.setConfigParam
                (EnvironmentParams.NODE_MAX.getName(), "6");
            envConfig.setAllowCreate(true);
            /* Disable noisy UtilizationProfile database creation. */
            DbInternal.setCreateUP(envConfig, false);
            /* Don't run the cleaner without a UtilizationProfile. */
            envConfig.setConfigParam
                (EnvironmentParams.ENV_RUN_CLEANER.getName(), "false");
            envConfig.setTransactional(true);
        
            env = create(envHome, envConfig);
        
            EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);

            List<Trace> expectedDbLogRecords = new ArrayList<Trace>();
            List<Trace> expectedFileRecords = new ArrayList<Trace>();

            /* Log a message. */
            Trace.trace(envImpl, "hi there");
            expectedDbLogRecords.add(new Trace("hi there"));

            /* 
             * Log an exception. The je.info file defaults to SEVERE, and will
             * only hold exceptions.
             */
            RuntimeException e = new RuntimeException("fake exception");
            LoggerUtils.traceAndLogException(envImpl, "DebugRecordTest", 
                                     "testException", "foo", e);
            Trace exceptionTrace = new Trace("foo\n" + 
                                             LoggerUtils.getStackTrace(e));
            expectedDbLogRecords.add(exceptionTrace);

            /* Log a split and flush the log to disk. */
            envImpl.getLogManager().flush();
            envImpl.getFileManager().clear();

            /* Verify. */
            checkDatabaseLog(expectedDbLogRecords);
            checkTextFile(expectedFileRecords);

        } finally {
            if (env != null) {
                close(env);
            }
        }
    }

    /**
     * Check what's in the database log.
     */
    private void checkDatabaseLog(List<Trace> expectedList)
        throws DatabaseException {

        SearchFileReader searcher =
            new SearchFileReader(DbInternal.getEnvironmentImpl(env), 
                    1000, true, DbLsn.NULL_LSN,
                                 DbLsn.NULL_LSN, LogEntryType.LOG_TRACE);

        int numSeen = 0;
        while (searcher.readNextEntry()) {
            Trace dRec = (Trace) searcher.getLastObject();
            assertEquals("Should see this as " + numSeen + " record: ",
                         expectedList.get(numSeen).getMessage(),
                         dRec.getMessage());
            numSeen++;
        }

        assertEquals("Should see this many debug records",
                     expectedList.size(), numSeen);
    }

    /**
     * Check what's in the text file.
     */
    private void checkTextFile(List<Trace> expectedList)
        throws IOException {

        FileReader fr = null;
        BufferedReader br = null;
        try {
            String textFileName = 
                DbInternal.getEnvironmentImpl(env).getEnvironmentHome() + 
                File.separator + "je.info.0";
            fr = new FileReader(textFileName);
            br = new BufferedReader(fr);

            String line = br.readLine();
            int numSeen = 0;

            /*
             * Read the file, checking only lines that start with valid Levels.
             */
            while (line != null) {
                try {
                    /* The line should start with a valid date. */
                    ParsePosition pp = new ParsePosition(0);
                    DateFormat ff = TracerFormatter.makeDateFormat();
                    ff.parse(line, pp);

                    /* There should be a java.util.logging.level next. */
                    int dateEnd = pp.getIndex();
                    int levelEnd = line.indexOf(" ", dateEnd + 1);
                    String possibleLevel = line.substring(dateEnd + 1,
                                                          levelEnd);
                    Level.parse(possibleLevel);

                    String expected =
                        expectedList.get(numSeen).getMessage();
                    StringBuilder seen = new StringBuilder();
                    seen.append(line.substring(levelEnd + 1));
                    /*
                     * Assemble the log message by reading the right number
                     * of lines
                     */
                    StringTokenizer st =
                        new StringTokenizer(expected,
                                            Character.toString('\n'), false);

                    for (int i = 1; i < st.countTokens(); i++) {
                        seen.append('\n');
                        String l = br.readLine();
                        seen.append(l);
                        if (i == (st.countTokens() -1)) {
                            seen.append('\n');
                        }
                    }
                    /* XXX, diff of multiline stuff isn't right yet. */
                    
                    /*
                     * The formatters for rep test and non-rep test 
                     * different, so ignore this check here.
                     */
                    if (!isReplicatedTest(getClass())) {
                        if (st.countTokens() == 1) {
                            assertEquals("Line " + numSeen +
                                         " should be the same",
                                         expected, seen.toString());
                        }
                    }
                    numSeen++;
                } catch (Exception e) {
                    /* Skip this line, not a message. */
                }
                line = br.readLine();
            }
            assertEquals("Should see this many debug records",
                         expectedList.size(), numSeen);
        } finally {
            if (br != null) {
                br.close();
            }

            if (fr != null) {
                fr.close();
            }
        }
    }
}
