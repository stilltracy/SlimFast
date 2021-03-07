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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import com.sleepycat.je.utilint.LoggerUtils;

/**
 * Perform a test action in another thread, providing methods to wait for
 * completion, and to check for success and failure.
 */
public abstract class TestAction extends Thread {

    /** Logger for this class. */
    protected final Logger logger =
        LoggerUtils.getLoggerFixedPrefix(getClass(), "Test");

    /** The exception thrown by the action or null. */
    public volatile Throwable exception;

    /** The action */
    protected abstract void action()
        throws Exception;

    /**
     * Assert that the action completed, either by succeeding or throwing an
     * exception, within the specified number of milliseconds.
     *
     * @param timeout the number of milliseconds to wait
     * @throws InterruptedException if waiting is interrupted
     */
    public void assertCompleted(final long timeout)
        throws InterruptedException {

        join(timeout);
        assertTrue("Thread should have completed", !isAlive());
    }

    /**
     * Assert that the action completed successfully within the specified
     * number of milliseconds.
     *
     * @param timeout the number of milliseconds to wait
     * @throws InterruptedException if waiting is interrupted
     */
    public void assertSucceeded(final long timeout)
        throws InterruptedException {

        assertCompleted(timeout);
        if (exception != null) {
            final AssertionError err =
                new AssertionError("Unexpected exception: " + exception);
            err.initCause(exception);
            throw err;
        }
    }

    /**
     * Assert that the action failed with an exception of the specified class,
     * or a subclass of it, within the specified number of milliseconds.
     *
     * @param timeout the number of milliseconds to wait
     * @param exceptionClass the exception class
     * @throws InterruptedException if waiting is interrupted
     */
    public void assertException(
        final long timeout,
        final Class<? extends Throwable> exceptionClass)
        throws InterruptedException {

        assertCompleted(timeout);
        assertNotNull("Expected exception", exception);
        if (!exceptionClass.isInstance(exception)) {
            final AssertionError err =
                new AssertionError("Unexpected exception: " + exception);
            err.initCause(exception);
            throw err;
        }
        logger.info("Got expected exception: " + exception);
    }

    /**
     * Call {@link #action} and catch any thrown exceptions.
     */
    @Override
    public void run() {
        try {
            action();
        } catch (Throwable t) {
            exception = t;
        }
    }
}
