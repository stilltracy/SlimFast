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

package com.sleepycat.je.jmx;

import java.io.File;

import javax.management.DynamicMBean;

import junit.framework.TestCase;

import com.sleepycat.je.Environment;
import com.sleepycat.je.util.TestUtils;

/**
 * @excludeDualMode
 *
 * Instantiate and exercise the JEMBeanHelper.
 */
public class MBeanTest extends TestCase {

    private static final boolean DEBUG = false;
    private File envHome;
    private String environmentDir;

    public MBeanTest() {
        environmentDir = System.getProperty(TestUtils.DEST_DIR);
        envHome = new File(environmentDir);
    }

    public void setUp() {

        TestUtils.removeLogFiles("Setup", envHome, false);
    }

    public void tearDown()
        throws Exception {

        TestUtils.removeLogFiles("tearDown", envHome, true);
    }

    /**
     * MBean which can configure and open an environment.
     */
    public void testOpenableBean()
        throws Throwable {

        Environment env = null;
        try {
            /* Environment is not open, and we can open. */
            env = MBeanTestUtils.openTxnalEnv(false, envHome);
            env.close();

            DynamicMBean mbean = new JEApplicationMBean(environmentDir);
            MBeanTestUtils.validateGetters(mbean, 5, DEBUG);
            MBeanTestUtils.validateMBeanOperations
                (mbean, 1, false, null, null, DEBUG);

            /* Open the environment. */
            mbean.invoke(JEApplicationMBean.OP_OPEN, null, null);

            MBeanTestUtils.validateGetters(mbean, 7, DEBUG);
            MBeanTestUtils.validateMBeanOperations
                (mbean, 7, true, null, null, DEBUG);

            /*
             * The last call to validateOperations ended up closing the
             * environment.
             */
            MBeanTestUtils.validateGetters(mbean, 5, DEBUG);
            MBeanTestUtils.validateMBeanOperations
                (mbean, 1, false, null, null, DEBUG);

            /* Should be no open handles. */
            MBeanTestUtils.checkForNoOpenHandles(environmentDir);
        } catch (Throwable t) {
            t.printStackTrace();

            if (env != null) {
                env.close();
            }
            throw t;
        }
    }

    /**
     * Checks that all parameters and return values are Serializable to
     * support JMX over RMI.
     */
    public void testSerializable()
        throws Exception {

        /* Create and close the environment. */
        Environment env = MBeanTestUtils.openTxnalEnv(false, envHome);
        env.close();

        /* Test without an open environment. */
        DynamicMBean mbean = new JEApplicationMBean(environmentDir);
        MBeanTestUtils.doTestSerializable(mbean);

        /* Test with an open environment. */
        mbean.invoke(JEApplicationMBean.OP_OPEN, null, null);
        MBeanTestUtils.doTestSerializable(mbean);

        /* Close. */
        mbean.invoke(JEApplicationMBean.OP_CLOSE, null, null);
    }
}
