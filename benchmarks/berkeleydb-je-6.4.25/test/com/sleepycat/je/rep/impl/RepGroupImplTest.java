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
package com.sleepycat.je.rep.impl;

import static org.junit.Assert.assertEquals;

import java.net.UnknownHostException;

import org.junit.Test;

import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.util.test.TestBase;

public class RepGroupImplTest extends TestBase {

    @Test
    public void testSerializeDeserialize()
        throws UnknownHostException {

        for (int formatVersion = RepGroupImpl.MIN_FORMAT_VERSION;
             formatVersion <= RepGroupImpl.MAX_FORMAT_VERSION;
             formatVersion++) {
            final int electable = 5;
            final int monitor = 1;
            final int secondary =
                (formatVersion < RepGroupImpl.FORMAT_VERSION_3) ?
                0 :
                3;
            RepGroupImpl group =
                RepTestUtils.createTestRepGroup(electable, monitor, secondary);
            String s1 = group.serializeHex(formatVersion);
            String tokens[] = s1.split(TextProtocol.SEPARATOR_REGEXP);
            assertEquals(
                1 +                              /* The Rep group itself */ +
                electable + monitor + secondary, /* the individual nodes. */
                tokens.length);
            RepGroupImpl dgroup = RepGroupImpl.deserializeHex(tokens, 0);
            assertEquals("Version", formatVersion, dgroup.getFormatVersion());
            if (formatVersion == RepGroupImpl.INITIAL_FORMAT_VERSION) {
                assertEquals("Deserialized version " + formatVersion,
                             group, dgroup);
            }
            String s2 = dgroup.serializeHex(formatVersion);
            assertEquals("Reserialized version " + formatVersion, s1, s2);
        }
    }
}
