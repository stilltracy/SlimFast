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
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import org.junit.Test;

import com.sleepycat.je.CommitToken;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

public class CommitTokenTest extends TestBase {

    private final File envRoot;

    public CommitTokenTest() {
        envRoot = SharedTestUtils.getTestDir();
    }

    @Test
    public void testBasic() 
        throws IOException, ClassNotFoundException {

        UUID repenvUUID = UUID.randomUUID();

        CommitToken t1 = new CommitToken(repenvUUID, 1);
        CommitToken t2 = new CommitToken(repenvUUID, 2);
        CommitToken t3 = new CommitToken(repenvUUID, 3);

        assertTrue((t1.compareTo(t2) < 0) && (t2.compareTo(t1) > 0));
        assertTrue((t2.compareTo(t3) < 0) && (t3.compareTo(t2) > 0));
        assertTrue((t1.compareTo(t3) < 0) && (t3.compareTo(t1) > 0));

        assertEquals(t1, new CommitToken(repenvUUID, 1));
        assertEquals(0, t1.compareTo(new CommitToken(repenvUUID, 1)));

        try {
            t1.compareTo(new CommitToken(UUID.randomUUID(), 1));
            fail("Expected exception");
        } catch (IllegalArgumentException ie) {
            // expected
        }

        /* test serialization/de-serialization. */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(t1);
        ByteArrayInputStream bais =
            new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        CommitToken t11 = (CommitToken)ois.readObject();

        assertEquals(t1, t11);
    }

    /**
     * Make sure that we only return a commit token when we've done real work.
     */
    @Test
    public void testCommitTokenFailures() 
        throws IOException {

        RepEnvInfo[] repEnvInfo = RepTestUtils.setupEnvInfos(envRoot, 1);
        ReplicatedEnvironment master = RepTestUtils.joinGroup(repEnvInfo);

        /* It's illegal to get a commit token before it has closed. */
        Transaction txn = master.beginTransaction(null, null);
        try {
            txn.getCommitToken();
            fail("Should have gotten IllegalStateException");
        } catch (IllegalStateException expected) {
            /* expected outcome. */
        }

        /* 
         * Now abort and try again. Simce this transaction has done no writing
         * the commit token should be null.
         */
        txn.abort();
        CommitToken token = txn.getCommitToken();
        assertTrue(token == null);

        /* 
         * A committed txn that has done no writing should also return a null
         * commit token.
         */
        txn = master.beginTransaction(null, null);
        txn.commit();
        token = txn.getCommitToken();
        assertTrue(token == null);

        /* 
         * A committed txn that has done a write should return a non-null
         * token.
         */
        txn = master.beginTransaction(null, null);
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        Database db = master.openDatabase(txn, "foo", dbConfig);
        db.close();
        txn.commit();
        token = txn.getCommitToken();
        assertTrue(token != null);

        RepTestUtils.shutdownRepEnvs(repEnvInfo);
    }
}
