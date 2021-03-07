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

package com.sleepycat.je.rep.persist.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Test;

import com.sleepycat.je.Durability;
import com.sleepycat.je.rep.ReplicaWriteException;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.ReplicationConfig;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * Simple tests using the DPL and HA.
 */
public class SimpleTest extends TestBase {

    private File envRoot;
    private RepEnvInfo[] repEnvInfo;
    private ReplicatedEnvironment masterEnv;
    private ReplicatedEnvironment replicaEnv;

    public SimpleTest() {
        envRoot = SharedTestUtils.getTestDir();
    }

    @After
    public void tearDown() {
        if (repEnvInfo != null) {

            /*
             * close() was not called, test failed. Do cleanup to allow more
             * tests to run, but leave log files for debugging this test case.
             */
            try {
                close(false /*normalShutdown*/);
            } catch (Exception ignored) {
                /* This secondary exception is just noise. */
            }
        }
    }

    /**
     * Create a 2 node group.
     *
     * ReplicaAckPolicy.ALL is used to ensure that when a master operation is
     * committed, the change is immediately available on the replica for
     * testing -- no waiting in the test is needed.
     */
    private void open()
        throws IOException {

        repEnvInfo = RepTestUtils.setupEnvInfos
            (envRoot, 2,
             RepTestUtils.createEnvConfig
                 (new Durability(Durability.SyncPolicy.WRITE_NO_SYNC,
                                 Durability.SyncPolicy.WRITE_NO_SYNC,
                                 Durability.ReplicaAckPolicy.ALL)),
             new ReplicationConfig());
        masterEnv = RepTestUtils.joinGroup(repEnvInfo);
        replicaEnv = repEnvInfo[1].getEnv();
        assertNotNull(masterEnv);
        assertNotNull(replicaEnv);
        assertNotSame(masterEnv, replicaEnv);
    }

    private void close() {
        close(true /*normalShutdown*/);
    }

    private void close(boolean normalShutdown) {
        try {
            if (normalShutdown) {
                RepTestUtils.shutdownRepEnvs(repEnvInfo);
            } else {
                for (RepEnvInfo info : repEnvInfo) {
                    info.abnormalCloseEnv();
                }
            }
        } finally {
            repEnvInfo = null;
            masterEnv = null;
            replicaEnv = null;
        }
    }

    /**
     * Test that the following sequence works.
     *  + Open EntityStore on Master and Replica.
     *  + Open PrimaryIndex on Master.
     *  + Open PrimaryIndex on Replica.
     *
     * This requires that a refresh of DPL metadata be performed on the Replica
     * when the PrimaryIndex is opened, since the Replica will have stale
     * metadata that does not include information for the index.
     *
     * Note that this is not the normal/expected usage mode.  Normally, all
     * indexes are opened immediately after opening the EntityStore.
     *
     * [#18594]
     */
    @Test
    public void testDeferOpenIndex()
        throws IOException {

        open();

        final EntityStore masterStore = new EntityStore
            (masterEnv, "foo",
             new StoreConfig().setAllowCreate(true).setTransactional(true));
        final EntityStore replicaStore = new EntityStore
            (replicaEnv, "foo",
             new StoreConfig().setTransactional(true));

        final PrimaryIndex<Integer, SimpleEntity> masterIndex =
            masterStore.getPrimaryIndex(Integer.class, SimpleEntity.class);

        /* Before the [#18594] fix, this threw RefreshException. */
        final PrimaryIndex<Integer, SimpleEntity> replicaIndex =
            replicaStore.getPrimaryIndex(Integer.class, SimpleEntity.class);

        replicaStore.close();
        masterStore.close();

        close();
    }

    /**
     * Test that the following sequence works.
     *  + Open EntityStore on Master and Replica.
     *  + Open PrimaryIndex A, with a sequence, on Master.
     *  + Open PrimaryIndex B, with a sequence, on Replica.
     *
     * This tickled a bug where the transaction config for the sequence was
     * not being properly initialized.
     *
     * Note that this is not the normal/expected usage mode.  Normally, all
     * indexes are opened immediately after opening the EntityStore.
     *
     * [#18594]
     */
    @Test
    public void testOpenSecondNonExistentSequenceOnReplica()
        throws IOException {

        open();

        final EntityStore masterStore = new EntityStore
            (masterEnv, "foo",
             new StoreConfig().setAllowCreate(true).setTransactional(true));
        final EntityStore replicaStore = new EntityStore
            (replicaEnv, "foo",
             new StoreConfig().setTransactional(true));

        masterStore.getPrimaryIndex(Integer.class, SimpleEntity.class);

        /* Note: An IndexNotAvailableException may be thrown in the future. */
        try {
            replicaStore.getPrimaryIndex(Integer.class, SimpleEntity2.class);
            fail();
        } catch (ReplicaWriteException expected) {
        }

        replicaStore.close();
        masterStore.close();

        close();
    }

    @Entity
    static class SimpleEntity {

        @PrimaryKey(sequence="ID")
        int id;

        String data = "data";
    }

    @Entity
    static class SimpleEntity2 {

        @PrimaryKey(sequence="ID2")
        int id;

        String data = "data";
    }
}
