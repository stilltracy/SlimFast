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

package com.sleepycat.je.rep;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.serializecompatibility.SerializeUtils;

/**
 * Verify that all classes marked as being serializable, can be serialized and
 * deserialized.
 */
public class SerializationTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Verifies that the clases identified by SerializeUtils.getSerializedSet()
     * can be serialized and deserialized.
     *
     * The test does not currently verify that structural equality is preserved
     * across serialization/deserialization.
     */
    @Test
    public void test()
        throws IOException, ClassNotFoundException {

        for (Map.Entry<String, Object> entry :
            SerializeUtils.getSerializedSet().entrySet()) {
            final String className = entry.getKey();

            try {
                final ByteArrayOutputStream baos =
                    new ByteArrayOutputStream(1024);
                final ObjectOutputStream out = new ObjectOutputStream(baos);
                final Object o1 = entry.getValue();
                out.writeObject(o1);
                out.close();

                final ByteArrayInputStream bais =
                    new ByteArrayInputStream(baos.toByteArray());
                final ObjectInputStream in = new ObjectInputStream(bais);
                @SuppressWarnings("unused")
                Object o2 = in.readObject();
                in.close();

                // Equality checking -- a future SR
            } catch (NotSerializableException nse) {
                nse.printStackTrace(System.err);
                fail(className + "  " + nse.getMessage());
            }
        }
    }
}
