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

package com.sleepycat.je.rep.stream;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.ReplicationConfig;
import com.sleepycat.je.rep.impl.RepParams;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;
import com.sleepycat.je.utilint.VLSN;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * [#18882] Exercise a bug in the interaction between the FeederReader and
 * the write queue.
 * 
 * Unlike any other FileReader, the FeederReader must operate on log files
 * that are concurrently growing in size. Log entries may be in the log buffers,
 * in the write queue, or on disk.
 *
 * The FeederReader starts out by looking for additional log contents in the
 * log buffers. If the log buffers don't have the desired data, the
 * FeederReader assumes the data must be somewhere on disk. In addition, the
 * FeederReader is not reading on lsn boundaries. It's just reading sections of
 * the log files, based on offsets. Because of that, the FeederReader must
 * deduce when a log file has ended, and it needs to switch to the next file.
 *
 * The bug was in this transition. The FeederReader was incorrectly deducing
 * the end of a given log file by checking the size of the log file. However,
 * it's possible that data can exist in the write queue only, and not be on
 * disk. In this case, the FeederReader was incorrectly skipping to the next
 * file, which resulted in an attempt to access a non existent file.
 * 
 * For example, suppose a log entry goes through these stages:
 * 1. In write log buffer
 * 2. Log buffer flushed to LogManager, but is queued in write queue
 *    instead of going to disk.
 * 3. Queued writes are written to disk, log entry is removed from queue, exists
 *    on disk only.
 *
 * The bug was that during stage 2, the FeederReader checked the size of the
 * current log file, found that it had read everything in that file already,
 * and incorrectly decided to jump to the next file. That next file does not
 * exist.
 */
public class FeederWriteQueueTest extends TestBase {
    private boolean verbose = Boolean.getBoolean("verbose");
    
    private final File envRoot;
    private RepEnvInfo[] repEnvInfo;
    private ReplicatedEnvironment master;
    private EntityStore store;
    private PrimaryIndex<Integer, AppData> primaryIndex; 
    private final int numThreads = 5;
    private final int numRecords = 500;
    private final int nNodes = 2;

    public FeederWriteQueueTest() {
        envRoot = SharedTestUtils.getTestDir();
    }

    /**
     * [#18882] Before this bug fix, this test would result in a
     * java.io.FileNotFoundException out of FeederReader$SwitchWindow.fillNext.
     */
    @Test
    public void testDataInWriteQueue() 
        throws Exception {

        openGroup();
        
        ExecutorService appThreads = Executors.newFixedThreadPool(numThreads);
        int opsPerThread = numRecords/numThreads;
        for (int i = 0; i < numThreads; i++) {
            appThreads.execute(new AppWork(i, opsPerThread));
        }

        appThreads.shutdown();
        appThreads.awaitTermination(6000, TimeUnit.SECONDS);
        
        VLSN vlsn = RepTestUtils.syncGroupToLastCommit(repEnvInfo,
                                                       repEnvInfo.length);
        RepTestUtils.checkNodeEquality(vlsn, verbose, repEnvInfo);
        closeGroup();
    }

    private void openGroup() 
        throws IOException {
        
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);

        ReplicationConfig repConfig = new ReplicationConfig();
        repConfig.setConfigParam(RepParams.VLSN_LOG_CACHE_SIZE.getName(),
                                 "2");

        repEnvInfo = RepTestUtils.setupEnvInfos(envRoot, nNodes, envConfig, 
                                                repConfig);
        master = RepTestUtils.joinGroup(repEnvInfo);

        StoreConfig config = new StoreConfig();
        config.setAllowCreate(true);
        config.setTransactional(true);
        store = new EntityStore(master, "test", config);
        primaryIndex = store.getPrimaryIndex(Integer.class, AppData.class);
    }

    private void closeGroup() {
        store.close();
        RepTestUtils.shutdownRepEnvs(repEnvInfo);
    }

    /*
     * Test data
     */ 
    @Entity
    static class AppData {
        public AppData() { }
        AppData(int key) {
            this.key = key;
        }
            
        @SuppressWarnings("unused")
        @PrimaryKey
        private int key;
        @SuppressWarnings("unused")
        private String data = "abcdefghijklmnopqrstuv";

    }

    /**
     * Each thread does some inserts and deletes. 
     */
    private class AppWork implements Runnable {
        private final Random random;
        private final int numOperations;
        private final int whichThread;

        AppWork(int whichThread, int numOperations) {
            this.whichThread = whichThread;
            random = new Random(whichThread);
            this.numOperations = numOperations;
        }
        
        public void run() {
            for (int i = 0; i < numOperations; i++) {
                Transaction txn = master.beginTransaction(null, null);
                int keyVal = random.nextInt();
                AppData data = new AppData(keyVal);
                primaryIndex.put(txn, data);
                txn.commitSync();

                txn = master.beginTransaction(null, null);
                primaryIndex.delete(txn, keyVal);
                txn.commitSync();
            }
        }
    }
}
