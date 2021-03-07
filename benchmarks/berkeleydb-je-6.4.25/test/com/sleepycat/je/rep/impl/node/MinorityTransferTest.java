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

package com.sleepycat.je.rep.impl.node;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.sleepycat.je.Transaction;
import org.junit.Test;

import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.rep.ReplicaWriteException;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.ReplicationConfig;
import com.sleepycat.je.rep.impl.RepTestBase;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;
import com.sleepycat.je.rep.UnknownMasterException;

/**
 * Master Transfer tests that run with only a group minority.
 */
public class MinorityTransferTest extends RepTestBase {
    private RepEnvInfo master;
    private ReplicatedEnvironment masterEnv;
    private RepEnvInfo replica;
    private String replicaName;

    @Override
    public void setUp() 
        throws Exception {

        super.setUp();
        master = repEnvInfo[0];
        master.getRepConfig().
            setConfigParam(ReplicationConfig.INSUFFICIENT_REPLICAS_TIMEOUT,
                           "30 s");
        createGroup();
        masterEnv = master.getEnv();
        replica = repEnvInfo[1];
        replicaName = replica.getEnv().getNodeName();
        closeNodes(repEnvInfo[2], repEnvInfo[3], repEnvInfo[4]);
    }

    @Override
    public void tearDown() 
        throws Exception {
        
        restartNodes(repEnvInfo[0], 
            repEnvInfo[2], repEnvInfo[3], repEnvInfo[4]);
        super.tearDown();
    }

    /**
     * Ensures that a thread waiting in {@code beginTransaction()} for
     * sufficient replicas gets a proper {@code
     * UnknownMasterException} upon a Master Transfer.
     */
    @Test
    public void testBeginWaiterException() throws Exception {
        ResultEvaluator expected =
            new ResultEvaluator() {
                @Override
                public boolean isExpected(Throwable t) {
                    return ((t instanceof UnknownMasterException) ||
                           (t instanceof ReplicaWriteException));
                }
            };
        TxnRunner runner = new TxnRunner(expected);
        Thread thread = new Thread(runner);
        thread.start();
        Thread.sleep(5 * MasterTransferTest.TICK);

        Set<String> replicas = new HashSet<String>();
        replicas.add(replicaName);
        masterEnv.transferMaster(replicas, 10, TimeUnit.SECONDS);
        thread.join(10000);
        assertFalse(thread.isAlive());
        assertTrue(runner.isOK());

        MasterTransferTest.awaitSettle(master, replica);
        master.closeEnv();
    }

    @Test
    public void testEnvClose() throws Exception {
        ResultEvaluator expected =
            new ResultEvaluator() {
                @Override
                public boolean isExpected(Throwable t) {
                    return t instanceof EnvironmentFailureException &&
                        t.getCause() instanceof IllegalStateException;
                }
            };
        TxnRunner runner = new TxnRunner(expected);
        Thread thread = new Thread(runner);
        thread.start();
        Thread.sleep(5 * MasterTransferTest.TICK);

        /*
         * Depending on thread timing, close() may either succeed or throw this
         * exception.
         */
        try {
            master.closeEnv();
        } catch (EnvironmentFailureException efe) {
            if (!expected.isExpected(efe.getCause())) {
                efe.printStackTrace();
                fail();
            }
        }
        thread.join(10000);
        assertFalse(thread.isAlive());
        assertTrue(runner.isOK());
    }

    class TxnRunner implements Runnable {
        private final ResultEvaluator evaluator;
        private boolean ok;
        
        TxnRunner(ResultEvaluator evaluator) {
            this.evaluator = evaluator;
        }
        
        @Override
        public void run() {
            try {
                Transaction txn = masterEnv.beginTransaction(null, null);
                txn.abort();
                ok = true;
            } catch (Throwable t) {
                if (evaluator.isExpected(t)) {
                    ok = true;
                } else {
                    t.printStackTrace();
                }
            }
        }
        
        boolean isOK() {
            return ok;
        }
    }

    interface ResultEvaluator {
        public boolean isExpected(Throwable exception);
    }
}
