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

package com.sleepycat.je.junit;

import java.lang.reflect.Method;

/**
 * A JUnitThread whose testBody calls a given TestCase method.
 */
public class JUnitMethodThread extends JUnitThread {

    private Object testCase;
    private Method method;
    private Object param;

    public JUnitMethodThread(String threadName, String methodName,
                             Object testCase)
        throws NoSuchMethodException {

        this(threadName, methodName, testCase, null);
    }

    public JUnitMethodThread(String threadName, String methodName,
                             Object testCase, Object param)
        throws NoSuchMethodException {

        super(threadName);
        this.testCase = testCase;
        this.param = param;
        method = testCase.getClass().getMethod(methodName, new Class[0]);
    }

    public void testBody()
        throws Exception {

        if (param != null) {
            method.invoke(testCase, new Object[] { param });
        } else {
            method.invoke(testCase, new Object[0]);
        }
    }
}
