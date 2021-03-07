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

package com.sleepycat.util.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Test;

import com.sleepycat.util.ExceptionUnwrapper;
import com.sleepycat.util.IOExceptionWrapper;
import com.sleepycat.util.RuntimeExceptionWrapper;

/**
 * @author Mark Hayes
 */
public class ExceptionWrapperTest extends TestBase {

    @Test
    public void testIOWrapper() {
        try {
            throw new IOExceptionWrapper(new RuntimeException("msg"));
        } catch (IOException e) {
            Exception ee = ExceptionUnwrapper.unwrap(e);
            assertTrue(ee instanceof RuntimeException);
            assertEquals("msg", ee.getMessage());

            Throwable t = ExceptionUnwrapper.unwrapAny(e);
            assertTrue(t instanceof RuntimeException);
            assertEquals("msg", t.getMessage());
        }
    }

    @Test
    public void testRuntimeWrapper() {
        try {
            throw new RuntimeExceptionWrapper(new IOException("msg"));
        } catch (RuntimeException e) {
            Exception ee = ExceptionUnwrapper.unwrap(e);
            assertTrue(ee instanceof IOException);
            assertEquals("msg", ee.getMessage());

            Throwable t = ExceptionUnwrapper.unwrapAny(e);
            assertTrue(t instanceof IOException);
            assertEquals("msg", t.getMessage());
        }
    }

    @Test
    public void testErrorWrapper() {
        try {
            throw new RuntimeExceptionWrapper(new Error("msg"));
        } catch (RuntimeException e) {
            try {
                ExceptionUnwrapper.unwrap(e);
                fail();
            } catch (Error ee) {
                assertTrue(ee instanceof Error);
                assertEquals("msg", ee.getMessage());
            }

            Throwable t = ExceptionUnwrapper.unwrapAny(e);
            assertTrue(t instanceof Error);
            assertEquals("msg", t.getMessage());
        }
    }

    /**
     * Generates a stack trace for a nested exception and checks the output
     * for the nested exception.
     */
    @Test
    public void testStackTrace() {

        /* Nested stack traces are not avilable in Java 1.3. */
        String version = System.getProperty("java.version");
        if (version.startsWith("1.3.")) {
            return;
        }

        Exception ex = new Exception("some exception");
        String causedBy = "Caused by: java.lang.Exception: some exception";

        try {
            throw new RuntimeExceptionWrapper(ex);
        } catch (RuntimeException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String s = sw.toString();
            assertTrue(s.indexOf(causedBy) != -1);
        }

        try {
            throw new IOExceptionWrapper(ex);
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String s = sw.toString();
            assertTrue(s.indexOf(causedBy) != -1);
        }
    }
}
