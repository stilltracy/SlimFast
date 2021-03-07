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

package com.sleepycat.je.utilint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

import static org.hamcrest.core.IsNull.nullValue;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.junit.Test;

import com.sleepycat.je.utilint.LoggerUtils;

/** Test {@link FileStoreInfo}. */
public class FileStoreInfoTest {

    private final Logger logger =
        LoggerUtils.getLoggerFixedPrefix(getClass(), "Test");

    /** Test when running on Java 6. */
    @Test
    public void testJava6()
        throws Exception {

        try {
            Class.forName(FileStoreInfo.FILE_STORE_CLASS);
            assumeThat("Skip when running Java 7 or later", nullValue());
        } catch (ClassNotFoundException e) {
        }

        try {
            FileStoreInfo.checkSupported();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            logger.info("Got expected unsupported exception for Java 6: " + e);
        }

        try {
            FileStoreInfo.getInfo(System.getProperty("user.dir"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            logger.info("Got expected exception for Java 6: " + e);
        }
    }

    /** Test when running on Java 7 or later. */
    @Test
    public void testJava7()
        throws Exception {

        try {
            Class.forName(FileStoreInfo.FILE_STORE_CLASS);
        } catch (ClassNotFoundException e) {
            assumeThat("Skip when running Java 6", nullValue());
        }

        FileStoreInfo.checkSupported();

        final File file1 = File.createTempFile("file1", null);
        file1.deleteOnExit();
        final FileStoreInfo info1 = FileStoreInfo.getInfo(file1.getPath());

        assertFalse(info1.equals(null));
        assertFalse(info1.equals(Boolean.TRUE));
        assertEquals(info1, info1);
        assertEquals(info1, FileStoreInfo.getInfo(file1.getPath()));

        assertTrue("Total space greater than zero",
                   info1.getTotalSpace() > 0);
        assertTrue("Usable space greater than zero",
                   info1.getUsableSpace() > 0);

        final File file2 = File.createTempFile("file2", null);
        file2.deleteOnExit();
        final FileStoreInfo info2 = FileStoreInfo.getInfo(file2.getPath());

        assertEquals("Equal file store info for files in same directory",
                     info1, info2);

        file2.delete();
        try {
            FileStoreInfo.getInfo(file2.getPath()).getTotalSpace();
            fail("Expected IOException");
        } catch (IOException e) {
            logger.info("Got expected exception for deleted file: " + e);
        }
   }
}
