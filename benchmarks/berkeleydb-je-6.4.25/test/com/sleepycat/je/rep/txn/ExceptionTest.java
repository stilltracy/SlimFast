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

package com.sleepycat.je.rep.txn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.ReplicatedEnvironment.State;
import com.sleepycat.je.rep.ReplicationConfig;
import com.sleepycat.je.rep.impl.RepTestBase;
import com.sleepycat.je.rep.utilint.RepTestUtils;

public class ExceptionTest extends RepTestBase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test for SR23970.
     *
     * A transaction begin should not invalidate the environment if it is
     * stalled in a beginTransaction while trying to get a quorum of replicas.
     * The transaction will fail later, without invalidating the environment,
     * when it tries to acquire locks and discovers that the environment has
     * been closed.
     */
    @Test
    public void test() throws InterruptedException {

        repEnvInfo[0].getRepConfig().
            setConfigParam(ReplicationConfig.INSUFFICIENT_REPLICAS_TIMEOUT,
                           "60 s");
        createGroup(2);
        final ReplicatedEnvironment menv = repEnvInfo[0].getEnv();
        assertEquals(menv.getState(), State.MASTER);

        /* Shutdown the second RN so that the beginTransaction() below stalls */
        repEnvInfo[1].closeEnv();

        final AtomicReference<Exception> te = new AtomicReference<Exception>();

        final Thread t = new Thread () {
            @Override
            public void run() {
                Transaction txn = null;
                try {
                    txn = menv.beginTransaction(null,
                                                RepTestUtils.SYNC_SYNC_ALL_TC);
                } catch (Exception e) {
                    /* Test failed if there is an exception on this path. */
                    te.set(e);
                } finally {
                    if (txn != null) {
                        try {
                            txn.abort();
                        } catch (Exception e) {
                            /* Ignore it */
                        }
                    }
                }
            }
        };

        t.start();

        /* Let it get to the beginTransaction. */
        Thread.sleep(2000);
        final EnvironmentImpl menvImpl = DbInternal.getEnvironmentImpl(menv);

        try {
            menv.close();
        } catch (Exception e) {
            /*
             * The abort above may not execute in time leaving an unclosed txn.
             * Ignore the resulting exception.
             */
        }

        t.join(10000);

        assertTrue(menvImpl.getInvalidatingException() == null);
    }
}
