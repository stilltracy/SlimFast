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
package com.sleepycat.je.rep.dual.trigger;

import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.Environment;
import com.sleepycat.je.trigger.Trigger;

public class ConfigTest extends com.sleepycat.je.trigger.ConfigTest {

    Environment env;

    @Before
    public void setUp() 
       throws Exception {

        super.setUp();
        env = create(envRoot, envConfig);
    }

    @After
    public void tearDown() 
        throws Exception {
        
        close(env);
        super.tearDown();
    }

    @Test
    public void testTriggerConfigOnEnvOpen() {
        dbConfig.setTriggers(Arrays.asList((Trigger) new InvokeTest.DBT("t1"),
                             (Trigger) new InvokeTest.DBT("t2")));

        /* Implementing ReplicatedDatabaseTrigger (RDBT) is expected. */
        try {
            env.openDatabase(null, "db1", dbConfig).close();
            fail("IAE expected");
        } catch (IllegalArgumentException iae) {
            // expected
        }

    }
}
