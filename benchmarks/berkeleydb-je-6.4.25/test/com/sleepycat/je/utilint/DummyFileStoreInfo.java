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

package com.sleepycat.je.utilint;

import java.io.IOException;

/**
 * Define a FileStoreInfo implementation that can be controlled by the test,
 * both to isolate the test from current file system free space conditions, and
 * to permit testing with specific free space conditions.
 */
public class DummyFileStoreInfo extends FileStoreInfo
        implements FileStoreInfo.Factory {

    public static DummyFileStoreInfo INSTANCE = new DummyFileStoreInfo();

    protected DummyFileStoreInfo() { }

    /* Implement Factory */

    @Override
    public void factoryCheckSupported() { }

    @Override
    public FileStoreInfo factoryGetInfo(final String file)
        throws IOException {

        factoryCheckSupported();
        return this;
    }

    /* Implement FileStoreInfo */

    @Override
    public long getTotalSpace()
        throws IOException {

        return Long.MAX_VALUE;
    }

    @Override
    public long getUsableSpace()
        throws IOException {

        return Long.MAX_VALUE;
    }

    /* Object methods */

    @Override
    public boolean equals(final Object o) {
        return getClass().isInstance(o);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
