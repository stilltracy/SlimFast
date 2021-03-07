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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import com.sleepycat.je.CacheMode;
import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.evictor.Evictor;
import com.sleepycat.je.evictor.Evictor.EvictionSource;
import com.sleepycat.je.log.FileManager;
import com.sleepycat.je.tree.BIN;
import com.sleepycat.je.tree.IN;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.je.utilint.DbLsn;
import com.sleepycat.je.utilint.TestHook;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;
import org.junit.After;
import org.junit.Test;

/**
 * Tests coordination of eviction and checkpointing.
 */
public class CkptEvictCoordTest extends TestBase {

    private static final boolean DEBUG = false;

    private File envHome;
    private Environment env;
    private EnvironmentImpl envImpl;

    public CkptEvictCoordTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    @After
    public void tearDown() {
        
        try {
            if (env != null) {
                env.close();
            }
        } catch (Throwable e) {
            System.out.println("tearDown: " + e);
        }

        env = null;
        envImpl = null;
        envHome = null;
    }

    /**
     * Opens the environment.
     */
    private void openEnv() {

        EnvironmentConfig config = TestUtils.initEnvConfig();
        config.setAllowCreate(true);

        /* Do not run the daemons. */
        config.setConfigParam(
            EnvironmentConfig.ENV_RUN_CLEANER, "false");
        config.setConfigParam(
            EnvironmentConfig.ENV_RUN_EVICTOR, "false");
        config.setConfigParam(
            EnvironmentConfig.ENV_RUN_OFFHEAP_EVICTOR, "false");
        config.setConfigParam(
            EnvironmentConfig.ENV_RUN_CHECKPOINTER, "false");
        config.setConfigParam(
            EnvironmentConfig.ENV_RUN_IN_COMPRESSOR, "false");
        /* Set max batch files to one, for exact control over cleaning. */
        config.setConfigParam(
            EnvironmentConfig.CLEANER_MAX_BATCH_FILES, "1");
        /* Use a tiny log file size to write one IN per file. */
        DbInternal.disableParameterValidation(config);
        config.setConfigParam(
            EnvironmentConfig.LOG_FILE_MAX, Integer.toString(64));

        /*
         * Disable critical eviction, we want to test under controlled
         * circumstances.
         */
        config.setConfigParam(
            EnvironmentConfig.EVICTOR_CRITICAL_PERCENTAGE, "1000");

        env = new Environment(envHome, config);
        envImpl = DbInternal.getEnvironmentImpl(env);
    }

    /**
     * Closes the environment.
     */
    private void closeEnv() {

        if (env != null) {
            env.close();
            env = null;
        }
    }

    /**
     * Verifies a fix for a LogFileNotFound issue that was introduced in JE 4.1
     * by removing synchronization of eviction.  Eviction and the construction
     * of the checkpointer dirty map were previously synchronized and could not
     * execute concurrently.  Rather than synchronize them again, the fix is to
     * make eviction log provisionally during construction of the dirty map,
     * and add the dirty parent of the evicted IN to the dirty map.
     *
     * This test creates the scenario described here:
     * https://sleepycat.oracle.com/trac/ticket/19346#comment:16
     *
     * [#19346]
     */
    @Test
    public void testEvictionDuringDirtyMapCreation() {

        openEnv();

        /* Start with nothing dirty. */
        env.sync();

        /*
         * We use the FileSummaryDB because it just so happens that when we
         * open a new environment, it has a single BIN and parent IN, and they
         * are iterated by the INList in the order required to reproduce the
         * problem: parent followed by child.  See the SR for details.
         */
        final long DB_ID = 2L; /* ID of FileSummaryDB is always 2. */

        /*
         * Find parent IN and child BIN.  Check that IN precedes BIN when
         * iterating the INList, which is necessary to reproduce the bug.
         */
        IN child = null;
        IN parent = null;

        for (IN in : envImpl.getInMemoryINs()) {
            if (in.getDatabase().getId().getId() == DB_ID) {
                if (in instanceof BIN) {
                    assertNull("Expect only one BIN", child);
                    child = in;
                } else {
                    if (child != null) {
                        System.out.println
                            ("WARNING: Test cannot be performed because IN " +
                             "parent does not precede child BIN");
                        closeEnv();
                        return;
                    }
                    assertNull("Expect only one IN", parent);
                    parent = in;
                }
            }
        }
        assertNotNull(child);
        assertNotNull(parent);

        /* We use tiny log files so that each IN is in a different file. */
        assertTrue(DbLsn.getFileNumber(child.getLastLoggedLsn()) !=
                   DbLsn.getFileNumber(parent.getLastLoggedLsn()));

        if (DEBUG) {
            System.out.println("child node=" + child.getNodeId() + " LSN=" +
                               DbLsn.getNoFormatString
                               (child.getLastLoggedLsn()) +
                               " dirty=" + child.getDirty());
            System.out.println("parent node=" + parent.getNodeId() + " LSN=" +
                               DbLsn.getNoFormatString
                               (parent.getLastLoggedLsn()) +
                               " dirty=" + parent.getDirty());
        }

        /*
         * Clean the log file containing the child BIN.  Because we set
         * CLEANER_MAX_BATCH_FILES to 1, this will clean only the single file
         * that we inject below.
         */
        final long fileNum = DbLsn.getFileNumber(child.getLastLoggedLsn());
        envImpl.getCleaner().getFileSelector().injectFileForCleaning(fileNum);

        final boolean filesCleaned = env.cleanLogFile();
        assertTrue(filesCleaned);

        /* Parent must not be dirty.  Child must be dirty after cleaning. */
        assertFalse(parent.getDirty());
        assertTrue(child.getDirty());

        final IN useChild = child;
        final IN useParent = parent;

        /* Hook called after examining each IN during dirty map creation. */
        class DirtyMapHook implements TestHook<IN> {
            boolean sawChild;
            boolean sawParent;

            public void doHook(IN in) {

                /*
                 * The parent IN is iterated first, before the child BIN.  It
                 * is not dirty, so it will not be added to the dirty map.  We
                 * evict the child BIN at this time, so that the child will not
                 * be added to the dirty map.
                 *
                 * The eviction creates the condition for the bug described in
                 * the SR, which is that the child is logged in the checkpoint
                 * interval but the parent is not, and the child is logged
                 * non-provisionally.  With the bug fix, the child is logged
                 * provisionally by the evictor and the parent is added to the
                 * dirty map at that time, so that it will be logged by the
                 * checkpoint.
                 *
                 * Ideally, to simulate real world conditions, this test should
                 * do the eviction in a separate thread.  However, because
                 * there was no synchronization between checkpointer and
                 * evictor, the effect of doing it in the same thread is the
                 * same.  Even with the bug fix, there is still no
                 * synchronization between checkpointer and evictor at the time
                 * the hook is called.
                 */
                if (in == useParent) {
                    assertFalse(sawChild);
                    assertFalse(sawParent);
                    assertFalse(in.getDirty());
                    sawParent = true;

                    Evictor evictor = envImpl.getEvictor();

                    /*
                     * First eviction strips LNs. Second evicts IN, if the old
                     * evictor is used; otherwise, it moves the node to the
                     * dirty/priority-2 LRUSet. As a result, one more eviction
                     * is needed with the new evictor.
                     */
                    useChild.latch(CacheMode.UNCHANGED);
                    evictor.doTestEvict(useChild, EvictionSource.MANUAL);
                    if (useChild.getInListResident()) {
                        useChild.latch(CacheMode.UNCHANGED);
                        evictor.doTestEvict(useChild, EvictionSource.MANUAL);
                        if (useChild.getInListResident()) {
                            useChild.latch(CacheMode.UNCHANGED);
                            evictor.doTestEvict(useChild, EvictionSource.MANUAL);
                            assertFalse(useChild.getInListResident());
                        }
                    }
                }

                /*
                 * We shouldn't see the child BIN because it was evicted, but
                 * if we do see it then it should not be dirty.
                 */
                if (in == useChild) {
                    assertFalse(sawChild);
                    assertTrue(sawParent);
                    sawChild = true;
                    assertFalse(in.getDirty());
                }
            }

            /* Unused methods. */
            public void doHook() {
                throw new UnsupportedOperationException();
            }
            public IN getHookValue() {
                throw new UnsupportedOperationException();
            }
            public void doIOHook() {
                throw new UnsupportedOperationException();
            }
            public void hookSetup() {
                throw new UnsupportedOperationException();
            }
        };

        /*
         * Perform checkpoint and perform eviction during construction of the
         * dirty map, using the hook above.
         */
        final DirtyMapHook hook = new DirtyMapHook();
        Checkpointer.examineINForCheckpointHook = hook;
        try {
            env.checkpoint(new CheckpointConfig().setForce(true));
        } finally {
            Checkpointer.examineINForCheckpointHook = null;
        }
        assertTrue(hook.sawParent);

        /* Checkpoint should have deleted the file. */
        final String fileName = envImpl.getFileManager().getFullFileName
            (fileNum, FileManager.JE_SUFFIX);
        assertFalse(fileName, new File(fileName).exists());

        /* Crash and recover. */
        envImpl.abnormalClose();
        envImpl = null;
        /* Before the bug fix, this recovery caused LogFileNotFound. */
        openEnv();
        closeEnv();
    }
}
