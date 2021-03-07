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

package com.sleepycat.je.cleaner;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import junit.framework.TestCase;

import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.util.TestUtils;

/**
 * Checks that backlog alert messages are logged when the cleaner is not able
 * to make forward progress.  [#21111]
 */
public class BacklogAlertTest extends TestCase {

    private final File envHome;
    private Environment env;
    private long nextFile = 100;
    private boolean gotAlert;

    public BacklogAlertTest() {
        envHome = new File(System.getProperty(TestUtils.DEST_DIR));
    }

    @Override
    public void setUp()
        throws Exception {

        TestUtils.removeLogFiles("Setup", envHome, false);
        open();
    }

    @Override
    public void tearDown()
        throws Exception {

        close();
    }

    private void open() {
        final EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setAllowCreate(true);
        envConfig.setLoggingHandler(new LoggingHandler());
        envConfig.setConfigParam(EnvironmentConfig.ENV_RUN_CLEANER, "false");
        env = new Environment(envHome, envConfig);
    }

    private void close()
        throws Exception {

        try {
            TestUtils.closeAll(env);
        } finally {
            env = null;
        }
    }

    public void testAlerts() {

        /*
         * The first alert fires after the backlog changes BACKLOG_ALERT_COUNT
         * times, and it is increasing, and the backlog is at least 
         */
        for (int i = 0; i < Cleaner.BACKLOG_ALERT_COUNT; i += 1) {
            produceAlert(0, false);
        }
        produceAlert(Cleaner.BACKLOG_ALERT_FLOOR, true);

        /* Alerts continue to fire as the backlog grows. */
        produceAlert(1, true);
        produceAlert(1, true);

        /* When the backlog doesn't grow, no alert fires. */
        produceAlert(0, false);
        produceAlert(0, false);

        /* More alerts fire when the backlog grows again. */
        produceAlert(1, true);
        produceAlert(1, true);
    }

    public void testNoAlertWithLowNumberOfChanges() {
        for (int i = 0; i < Cleaner.BACKLOG_ALERT_COUNT; i += 1) {
            produceAlert(2 * Cleaner.BACKLOG_ALERT_FLOOR, false);
        }
    }

    public void testNoAlertWithLowBacklogValue() {
        for (int i = 0; i < 2 * Cleaner.BACKLOG_ALERT_COUNT; i += 1) {
            produceAlert(0, false);
        }
        for (int i = 1; i < Cleaner.BACKLOG_ALERT_FLOOR; i += 1) {
            produceAlert(1, false);
        }
    }

    private void produceAlert(int nFilesToAddToBacklog, boolean expectAlert) {
        final Cleaner cleaner =
            DbInternal.getEnvironmentImpl(env).getCleaner();
        final FileSelector fileSelector = cleaner.getFileSelector();

        int nAdded = 0;
        while (nAdded < nFilesToAddToBacklog) {
            fileSelector.injectFileForCleaning(nextFile);
            nextFile += 1;
            nAdded += 1;
        }

        gotAlert = false;
        cleaner.checkBacklogGrowth();
        if (expectAlert) {
            assertTrue("expected alert, but it did not fire", gotAlert);
        } else {
            assertFalse("expected no alert, but it fired", gotAlert);
        }
    }

    /**
     * A handler that is called when JE logs a message.  If the message is a
     * backlog alert, our handler sets the gotAlert field to true.
     */
    private class LoggingHandler extends Handler {

        @Override
        public void publish(LogRecord record) {
            if (record.getMessage().contains("backlog has grown")) {
                assertEquals(Level.SEVERE, record.getLevel());
                assertFalse("alert fired multiple times", gotAlert);
                gotAlert = true;
            }
        }

        @Override
        public void flush() {
            /* Nothing to do */
        }

        @Override
        public void close() {
            /* Nothing to do */
        }
    }
}
