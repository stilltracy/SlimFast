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
package com.sleepycat.je.trigger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.rep.dual.trigger.InvokeTest;

public class ConfigTest extends TestBase {

    @Test
    public void testConflictingTypes() {
        DatabaseConfig dc = new DatabaseConfig();
        try {
            dc.setTriggers(Arrays.asList((Trigger) new DBT("t1"),
                           (Trigger) new InvokeTest.RDBT("t2")));
            fail("IAE expected");
        } catch (IllegalArgumentException iae) {
            // Expected
        }
    }

    @Test
    public void testConflictingNames() {
        DatabaseConfig dc = new DatabaseConfig();
        try {
            dc.setTriggers(Arrays.asList((Trigger) new DBT("t1"),
                           (Trigger) new DBT("t1")));
            fail("IAE expected");
        } catch (IllegalArgumentException iae) {
            // Expected
        }
    }

    @Test
    public void testSecondaryConfig() {
        SecondaryConfig sc = new SecondaryConfig();

        try {
            sc.setTriggers(Arrays.asList((Trigger) new DBT("t1"),
                           (Trigger) new DBT("t2")));
            fail("IAE expected");
        } catch (IllegalArgumentException iae) {
            // Expected
        }

        try {
            sc.setOverrideTriggers(true);
            fail("IAE expected");
        } catch (IllegalArgumentException iae) {
            // Expected
        }

        assertEquals(0,sc.getTriggers().size());
        assertFalse(sc.getOverrideTriggers());
    }
}
