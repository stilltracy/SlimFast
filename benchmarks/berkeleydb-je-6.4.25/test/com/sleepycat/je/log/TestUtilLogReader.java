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

package com.sleepycat.je.log;

import java.nio.ByteBuffer;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.log.entry.LogEntry;
import com.sleepycat.je.utilint.DbLsn;

/**
 * Instantiates all log entries using the shared log entry instances.
 */
public class TestUtilLogReader extends FileReader {

    private LogEntryType entryType;
    private LogEntry entry;

    public TestUtilLogReader(EnvironmentImpl env)
        throws DatabaseException {

        super(env,
              4096,
              true,
              DbLsn.NULL_LSN,
              null,
              DbLsn.NULL_LSN,
              DbLsn.NULL_LSN);
    }

    public TestUtilLogReader(EnvironmentImpl env,
                             int readBufferSize,
                             boolean forward,
                             long startLsn,
                             Long singleFileNumber,
                             long endOfFileLsn,
                             long finishLsn)
        throws DatabaseException {

        super(env,
              readBufferSize,
              forward,
              startLsn,
              singleFileNumber,
              endOfFileLsn,
              finishLsn);
    }

    public LogEntryType getEntryType() {
        return entryType;
    }

    public int getEntryVersion() {
        return currentEntryHeader.getVersion();
    }

    public LogEntry getEntry() {
        return entry;
    }

    protected boolean isTargetEntry() {
        return true;
    }

    protected boolean processEntry(ByteBuffer entryBuffer)
        throws DatabaseException {

        entryType = LogEntryType.findType(currentEntryHeader.getType());
        entry = entryType.getSharedLogEntry();
        entry.readEntry(envImpl, currentEntryHeader, entryBuffer);
        return true;
    }
}
