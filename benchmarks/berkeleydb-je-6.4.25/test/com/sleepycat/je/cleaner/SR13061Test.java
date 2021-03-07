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

package com.sleepycat.je.cleaner;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.tree.FileSummaryLN;
import com.sleepycat.utilint.StringUtils;

/**
 * Tests that a FileSummaryLN with an old style string key can be read.  When
 * we relied solely on log entry version to determine whether an LN had a
 * string key, we could fail when an old style LN was migrated by the cleaner.
 * In that case the key was still a string key but the log entry version was
 * updated to something greater than zero.  See FileSummaryLN.hasStringKey for
 * details of how we now guard against this.
 */
@RunWith(Parameterized.class)
public class SR13061Test extends CleanerTestBase {

    public SR13061Test(boolean multiSubDir) {
        envMultiSubDir = multiSubDir;
        customName = envMultiSubDir ? "multi-sub-dir" : null ;
    }
    
    @Parameters
    public static List<Object[]> genParams() {
        
        return getEnv(new boolean[] {false, true});
    }

    @Test
    public void testSR13061()
        throws DatabaseException {

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        if (envMultiSubDir) {
            envConfig.setConfigParam(EnvironmentConfig.LOG_N_DATA_DIRECTORIES,
                                     DATA_DIRS + "");
        }
        env = new Environment(envHome, envConfig);

        FileSummaryLN ln = new FileSummaryLN(new FileSummary());

        /*
         * All of these tests failed before checking that the byte array must
         * be eight bytes for integer keys.
         */
        assertTrue(ln.hasStringKey(stringKey(0)));
        assertTrue(ln.hasStringKey(stringKey(1)));
        assertTrue(ln.hasStringKey(stringKey(12)));
        assertTrue(ln.hasStringKey(stringKey(123)));
        assertTrue(ln.hasStringKey(stringKey(1234)));
        assertTrue(ln.hasStringKey(stringKey(12345)));
        assertTrue(ln.hasStringKey(stringKey(123456)));
        assertTrue(ln.hasStringKey(stringKey(1234567)));
        assertTrue(ln.hasStringKey(stringKey(123456789)));
        assertTrue(ln.hasStringKey(stringKey(1234567890)));

        /*
         * These tests failed before checking that the first byte of the
         * sequence number (in an eight byte key) must not be '0' to '9' for
         * integer keys.
         */
        assertTrue(ln.hasStringKey(stringKey(12345678)));
        assertTrue(ln.hasStringKey(stringKey(12340000)));

        /* These tests are just for good measure. */
        assertTrue(!ln.hasStringKey(intKey(0, 1)));
        assertTrue(!ln.hasStringKey(intKey(1, 1)));
        assertTrue(!ln.hasStringKey(intKey(12, 1)));
        assertTrue(!ln.hasStringKey(intKey(123, 1)));
        assertTrue(!ln.hasStringKey(intKey(1234, 1)));
        assertTrue(!ln.hasStringKey(intKey(12345, 1)));
        assertTrue(!ln.hasStringKey(intKey(123456, 1)));
        assertTrue(!ln.hasStringKey(intKey(1234567, 1)));
        assertTrue(!ln.hasStringKey(intKey(12345678, 1)));
        assertTrue(!ln.hasStringKey(intKey(123456789, 1)));
        assertTrue(!ln.hasStringKey(intKey(1234567890, 1)));
    }

    private byte[] stringKey(long fileNum) {

        try {
            return StringUtils.toUTF8(String.valueOf(fileNum));
        } catch (Exception e) {
            fail(e.toString());
            return null;
        }
    }

    private byte[] intKey(long fileNum, long seqNum) {

        TupleOutput out = new TupleOutput();
        out.writeUnsignedInt(fileNum);
        out.writeUnsignedInt(seqNum);
        return out.toByteArray();
    }
}
