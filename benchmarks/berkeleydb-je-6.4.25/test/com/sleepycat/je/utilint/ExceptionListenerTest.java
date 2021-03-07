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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.ExceptionEvent;
import com.sleepycat.je.ExceptionListener;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

public class ExceptionListenerTest extends TestBase {

    private final File envHome;

    private volatile boolean exceptionThrownCalled = false;

    private DaemonThread dt = null;

    public ExceptionListenerTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    @Test
    public void testExceptionListener()
        throws Exception {

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setExceptionListener(new MyExceptionListener());
        envConfig.setAllowCreate(true);
        Environment env = new Environment(envHome, envConfig);
        EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);

        assertSame(envConfig.getExceptionListener(),
                   envImpl.getExceptionListener());

        dt = new MyDaemonThread(0, Environment.CLEANER_NAME, envImpl);
        DaemonThread.stifleExceptionChatter = true;
        dt.runOrPause(true);
        long startTime = System.currentTimeMillis();
        while (!dt.isShutdownRequested() &&
               System.currentTimeMillis() - startTime < 10 * 10000) {
            Thread.yield();
        }
        assertTrue("ExceptionListener apparently not called",
                   exceptionThrownCalled);

        /* Change the exception listener. */
        envConfig = env.getConfig();
        exceptionThrownCalled = false;
        envConfig.setExceptionListener(new MyExceptionListener());
        env.setMutableConfig(envConfig);

        assertSame(envConfig.getExceptionListener(),
                   envImpl.getExceptionListener());

        dt = new MyDaemonThread(0, Environment.CLEANER_NAME, envImpl);
        dt.stifleExceptionChatter = true;
        dt.runOrPause(true);
        startTime = System.currentTimeMillis();
        while (!dt.isShutdownRequested() &&
               System.currentTimeMillis() - startTime < 10 * 10000) {
            Thread.yield();
        }
        assertTrue("ExceptionListener apparently not called",
                   exceptionThrownCalled);
    }

    private class MyDaemonThread extends DaemonThread {
        MyDaemonThread(long waitTime, String name, EnvironmentImpl envImpl) {
            super(waitTime, name, envImpl);
        }

        @Override
        protected void onWakeup() {
            throw new RuntimeException("test exception listener");
        }
    }

    private class MyExceptionListener implements ExceptionListener {
        public void exceptionThrown(ExceptionEvent event) {
            assertEquals("daemonName should be CLEANER_NAME",
                         Environment.CLEANER_NAME,
                         event.getThreadName());

	    /*
	     * Be sure to set the exceptionThrownFlag before calling
	     * shutdown, so the main test thread will see the right
	     * value of the flag when it comes out of the loop in
	     * testExceptionList that waits for the daemon thread to
	     * finish.
	     */
            exceptionThrownCalled = true;
            dt.requestShutdown();
        }
    }
}
