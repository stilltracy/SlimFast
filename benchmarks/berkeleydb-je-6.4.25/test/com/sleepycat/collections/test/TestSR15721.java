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

package com.sleepycat.collections.test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.collections.CurrentTransaction;
import com.sleepycat.je.Environment;
import com.sleepycat.util.test.TestBase;
import com.sleepycat.util.test.TestEnv;

/**
 * @author Chao Huang
 */
public class TestSR15721 extends TestBase {

    private Environment env;
    private CurrentTransaction currentTxn;

    @Before
    public void setUp()
        throws Exception {

        env = TestEnv.TXN.open("TestSR15721");
        currentTxn = CurrentTransaction.getInstance(env);
    }

    @After
    public void tearDown() {
        try {
            if (env != null) {
                env.close();
            }
        } catch (Exception e) {
            System.out.println("Ignored exception during tearDown: " + e);
        } finally {
            /* Ensure that GC can cleanup. */
            env = null;
            currentTxn = null;
        }
    }

    /**
     * Tests that the CurrentTransaction instance doesn't indeed allow GC to
     * reclaim while attached environment is open. [#15721]
     */
    @Test
    public void testSR15721Fix()
        throws Exception {

        int hash = currentTxn.hashCode();
        int hash2 = -1;

        currentTxn = CurrentTransaction.getInstance(env);
        hash2 = currentTxn.hashCode();
        assertTrue(hash == hash2);

        currentTxn.beginTransaction(null);
        currentTxn = null;
        hash2 = -1;

        for (int i = 0; i < 10; i += 1) {
            byte[] x = null;
            try {
                 x = new byte[Integer.MAX_VALUE - 1];
                 fail();
            } catch (OutOfMemoryError expected) {
            }
            assertNull(x);

            System.gc();
        }

        currentTxn = CurrentTransaction.getInstance(env);
        hash2 = currentTxn.hashCode();
        currentTxn.commitTransaction();

        assertTrue(hash == hash2);
    }
}
