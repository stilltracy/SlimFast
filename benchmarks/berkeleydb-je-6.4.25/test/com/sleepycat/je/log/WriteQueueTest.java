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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.log.FileManager.LogEndFileDescriptor;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * Test File Manager write queue
 */
public class WriteQueueTest extends TestBase {

    static private int FILE_SIZE = 120;

    private Environment env;
    private FileManager fileManager;
    private final File envHome;

    public WriteQueueTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    @Before
    public void setUp()
        throws Exception {

        /* Remove files to start with a clean slate. */
        super.setUp();
        
        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        DbInternal.disableParameterValidation(envConfig);
        envConfig.setConfigParam(EnvironmentParams.LOG_FILE_MAX.getName(),
                                 new Integer(FILE_SIZE).toString());
        /* Yank the cache size way down. */
        envConfig.setConfigParam
            (EnvironmentParams.LOG_FILE_CACHE_SIZE.getName(), "3");
        envConfig.setAllowCreate(true);

        env = new Environment(envHome, envConfig);
        fileManager = DbInternal.getEnvironmentImpl(env).getFileManager();
    }

    @After
    public void tearDown()
        throws DatabaseException {

        if (env != null) {
            env.close();
            env = null;
        }
    }

    @Test
    public void testReadFromEmptyWriteQueue() {
        if (fileManager.getUseWriteQueue()) {
            LogEndFileDescriptor lefd =
                fileManager.new LogEndFileDescriptor();
            ByteBuffer bb = ByteBuffer.allocate(100);
            assertFalse(lefd.checkWriteCache(bb, 0, 0));
        }
    }

    @Test
    public void testReadFromWriteQueueWithDifferentFileNum() {
        if (fileManager.getUseWriteQueue()) {
            LogEndFileDescriptor lefd =
                fileManager.new LogEndFileDescriptor();
            lefd.setQueueFileNum(1);
            ByteBuffer bb = ByteBuffer.allocate(100);
            assertFalse(lefd.checkWriteCache(bb, 0, 0));
        }
    }

    @Test
    public void testReadFromWriteQueueExactMatch()
        throws Exception {

        if (fileManager.getUseWriteQueue()) {
            LogEndFileDescriptor lefd =
                fileManager.new LogEndFileDescriptor();
            lefd.setQueueFileNum(0);
            lefd.enqueueWrite(0, new byte[] { 0, 1, 2, 3, 4 }, 0, 0, 5);
            ByteBuffer bb = ByteBuffer.allocate(100);
            bb.limit(5);
            assertTrue(lefd.checkWriteCache(bb, 0, 0));
            bb.position(0);
            for (int i = 0; i < 5; i++) {
                byte b = bb.get();
                assertTrue(b == i);
            }
        }
    }

    @Test
    public void testReadFromWriteQueueSubset()
        throws Exception {

        if (fileManager.getUseWriteQueue()) {
            LogEndFileDescriptor lefd =
                fileManager.new LogEndFileDescriptor();
            lefd.setQueueFileNum(0);
            lefd.enqueueWrite(0, new byte[] { 0, 1, 2, 3, 4 }, 0, 0, 5);
            ByteBuffer bb = ByteBuffer.allocate(100);
            bb.limit(3);
            assertTrue(lefd.checkWriteCache(bb, 0, 0));
            bb.position(0);
            for (int i = 0; i < bb.limit(); i++) {
                byte b = bb.get();
                assertTrue(b == i);
            }
        }
    }

    @Test
    public void testReadFromWriteQueueSubsetOffset()
        throws Exception {

        if (fileManager.getUseWriteQueue()) {
            LogEndFileDescriptor lefd =
                fileManager.new LogEndFileDescriptor();
            lefd.setQueueFileNum(0);
            lefd.enqueueWrite(0, new byte[] { 0, 1, 2, 3, 4 }, 0, 0, 5);
            ByteBuffer bb = ByteBuffer.allocate(100);
            bb.limit(3);
            assertTrue(lefd.checkWriteCache(bb, 2, 0));
            bb.position(0);
            for (int i = 2; i < bb.limit() + 2; i++) {
                byte b = bb.get();
                assertTrue(b == i);
            }
        }
    }

    @Test
    public void testReadFromWriteQueueSubsetUnderflow()
        throws Exception {

        if (fileManager.getUseWriteQueue()) {
            LogEndFileDescriptor lefd =
                fileManager.new LogEndFileDescriptor();
            lefd.setQueueFileNum(0);
            lefd.enqueueWrite(0, new byte[] { 0, 1, 2, 3, 4 }, 0, 0, 5);
            ByteBuffer bb = ByteBuffer.allocate(100);
            bb.limit(4);
            assertTrue(lefd.checkWriteCache(bb, 2, 0));
            /* Ensure that buffer was reset to where it belongs. */
            assertEquals(bb.position(), 3);
            bb.flip();
            for (int i = 2; i < 2 + bb.limit(); i++) {
                byte b = bb.get();
                assertTrue(b == i);
            }
        }
    }

    @Test
    public void testReadFromWriteQueueSubsetOverflow()
        throws Exception {

        if (fileManager.getUseWriteQueue()) {
            LogEndFileDescriptor lefd =
                fileManager.new LogEndFileDescriptor();
            lefd.setQueueFileNum(0);
            lefd.enqueueWrite(0, new byte[] { 0, 1, 2, 3, 4 }, 0, 0, 5);
            lefd.enqueueWrite(0, new byte[] { 5, 6, 7, 8, 9 }, 5, 0, 5);

            ByteBuffer bb = ByteBuffer.allocate(100);
            bb.limit(4);
            assertTrue(lefd.checkWriteCache(bb, 2, 0));
            bb.position(0);
            for (int i = 2; i < bb.limit() + 2; i++) {
                byte b = bb.get();
                assertTrue(b == i);
            }
        }
    }

    @Test
    public void testReadFromWriteQueueSubsetOverflow2()
        throws Exception {

        if (fileManager.getUseWriteQueue()) {
            LogEndFileDescriptor lefd =
                fileManager.new LogEndFileDescriptor();
            lefd.setQueueFileNum(0);
            lefd.enqueueWrite(0, new byte[] { 0, 1, 2, 3, 4 }, 0, 0, 5);
            lefd.enqueueWrite(0, new byte[] { 5, 6, 7, 8, 9 }, 5, 0, 5);

            ByteBuffer bb = ByteBuffer.allocate(100);
            bb.limit(8);
            assertTrue(lefd.checkWriteCache(bb, 2, 0));
            bb.position(0);
            for (int i = 2; i < bb.limit() + 2; i++) {
                byte b = bb.get();
                assertTrue(b == i);
            }
        }
    }

    @Test
    public void testReadFromWriteQueueMultipleEntries()
        throws Exception {

        if (fileManager.getUseWriteQueue()) {
            LogEndFileDescriptor lefd =
                fileManager.new LogEndFileDescriptor();
            lefd.setQueueFileNum(0);
            lefd.enqueueWrite(0, new byte[] { 0, 1, 2, 3, 4 }, 0, 0, 5);
            lefd.enqueueWrite(0, new byte[] { 5, 6, 7, 8, 9 }, 5, 0, 5);
            lefd.enqueueWrite(0, new byte[] { 10, 11, 12, 13, 14 }, 10, 0, 5);

            ByteBuffer bb = ByteBuffer.allocate(100);
            bb.limit(9);
            assertTrue(lefd.checkWriteCache(bb, 2, 0));
            bb.position(0);
            for (int i = 2; i < bb.limit() + 2; i++) {
                byte b = bb.get();
                assertTrue(b == i);
            }
        }
    }

    @Test
    public void testReadFromWriteQueueLast2EntriesOnly()
        throws Exception {

        if (fileManager.getUseWriteQueue()) {
            LogEndFileDescriptor lefd =
                fileManager.new LogEndFileDescriptor();
            lefd.setQueueFileNum(0);
            lefd.enqueueWrite(0, new byte[] { 0, 1, 2, 3, 4 }, 0, 0, 5);
            lefd.enqueueWrite(0, new byte[] { 5, 6, 7, 8, 9 }, 5, 0, 5);
            lefd.enqueueWrite(0, new byte[] { 10, 11, 12, 13, 14 }, 10, 0, 5);

            ByteBuffer bb = ByteBuffer.allocate(100);
            bb.limit(9);
            assertTrue(lefd.checkWriteCache(bb, 6, 0));
            bb.position(0);
            for (int i = 6; i < bb.limit() + 6; i++) {
                byte b = bb.get();
                assertTrue(b == i);
            }
        }
    }

    @Test
    public void testReadFromWriteQueueLastEntryOnly()
        throws Exception {

        if (fileManager.getUseWriteQueue()) {
            LogEndFileDescriptor lefd =
                fileManager.new LogEndFileDescriptor();
            lefd.setQueueFileNum(0);
            lefd.enqueueWrite(0, new byte[] { 0, 1, 2, 3, 4 }, 0, 0, 5);
            lefd.enqueueWrite(0, new byte[] { 5, 6, 7, 8, 9 }, 5, 0, 5);
            lefd.enqueueWrite(0, new byte[] { 10, 11, 12, 13, 14 }, 10, 0, 5);

            ByteBuffer bb = ByteBuffer.allocate(100);
            bb.limit(5);
            assertTrue(lefd.checkWriteCache(bb, 10, 0));
            bb.position(0);
            for (int i = 10; i < bb.limit() + 10; i++) {
                byte b = bb.get();
                assertTrue(b == i);
            }
        }
    }
}
