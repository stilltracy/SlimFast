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

package com.sleepycat.je.dbi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

public class MemoryBudgetTest extends TestBase {
    private final File envHome;

    public MemoryBudgetTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    @Test
    public void testDefaults()
        throws Exception {

        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setAllowCreate(true);
        Environment env = new Environment(envHome, envConfig);
        EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);
        MemoryBudget testBudget = envImpl.getMemoryBudget();

        /*
        System.out.println("max=    " + testBudget.getMaxMemory());
        System.out.println("log=    " + testBudget.getLogBufferBudget());
        System.out.println("thresh= " + testBudget.getEvictorCheckThreshold());
        */

        assertTrue(testBudget.getMaxMemory() > 0);
        assertTrue(testBudget.getLogBufferBudget() > 0);

        assertTrue(testBudget.getMaxMemory() <=
                   MemoryBudget.getRuntimeMaxMemory());

        env.close();
    }

    /* Verify that the proportionally based setting works. */
    @Test
    public void testCacheSizing()
        throws Exception {

        long jvmMemory = MemoryBudget.getRuntimeMaxMemory();

        /*
         * Runtime.maxMemory() may return Long.MAX_VALUE if there is no
         * inherent limit.
         */
        if (jvmMemory == Long.MAX_VALUE) {
            jvmMemory = 1 << 26;
        }

        /* The default cache size ought to be percentage based. */
        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setAllowCreate(true);
        Environment env = new Environment(envHome, envConfig);
        EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);
        long percentConfig = envImpl.getConfigManager().
            getInt(EnvironmentParams.MAX_MEMORY_PERCENT);

        EnvironmentConfig c = env.getConfig();
        long expectedMem = (jvmMemory * percentConfig) / 100;
        assertEquals(expectedMem, c.getCacheSize());
        assertEquals(expectedMem, envImpl.getMemoryBudget().getMaxMemory());
        env.close();

        /* Try setting the percentage.*/
        expectedMem = (jvmMemory * 30) / 100;
        envConfig = TestUtils.initEnvConfig();
        envConfig.setCachePercent(30);
        env = new Environment(envHome, envConfig);
        envImpl = DbInternal.getEnvironmentImpl(env);
        c = env.getConfig();
        assertEquals(expectedMem, c.getCacheSize());
        assertEquals(expectedMem, envImpl.getMemoryBudget().getMaxMemory());
        env.close();

        /* Try overriding */
        envConfig = TestUtils.initEnvConfig();
        envConfig.setCacheSize(MemoryBudget.MIN_MAX_MEMORY_SIZE + 10);
        env = new Environment(envHome, envConfig);
        envImpl = DbInternal.getEnvironmentImpl(env);
        c = env.getConfig();
        assertEquals(MemoryBudget.MIN_MAX_MEMORY_SIZE + 10, c.getCacheSize());
        assertEquals(MemoryBudget.MIN_MAX_MEMORY_SIZE + 10,
                     envImpl.getMemoryBudget().getMaxMemory());
        env.close();
    }
}
