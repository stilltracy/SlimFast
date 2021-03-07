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

package com.sleepycat.je.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.log.DumpFileReader;
import com.sleepycat.je.log.LogEntryType;
import com.sleepycat.je.log.entry.LogEntry;
import com.sleepycat.je.utilint.DbLsn;

/**
 * TestDumper is a sample custom dumper, of the kind that can be specified by
 * DbPrintLog -c <custom dumper>.
 * 
 * To try the custom dumper:
 * java -cp "build/test/classes;build/classes" com.sleepycat.je.util.DbPrintLog
 *            -h <env> -c com.sleepycat.je.util.TestDumper
 * TestDumper will list a count of log entries, the log entry type, and the lsn.
 */
public class TestDumper extends DumpFileReader {
    public static final String SAVE_INFO_FILE = "dumpLog";

    private int counter = 0;

    public TestDumper(EnvironmentImpl env,
                      Integer readBufferSize,
                      Long startLsn,
                      Long finishLsn,
                      Long endOfFileLsn,
                      String entryTypes,
                      String txnIds,
                      Boolean verbose,
                      Boolean repEntriesOnly,
                      Boolean forwards) 
        throws DatabaseException {

        super(env, readBufferSize, startLsn, finishLsn, endOfFileLsn, 
              entryTypes, txnIds, verbose,  repEntriesOnly, forwards);
    }

    @Override
    protected boolean processEntry(ByteBuffer entryBuffer)
        throws DatabaseException {

        /* Figure out what kind of log entry this is */
        byte curType = currentEntryHeader.getType();
        LogEntryType lastEntryType = LogEntryType.findType(curType);

        /* Read the entry and dump it into a string buffer. */
        LogEntry entry = lastEntryType.getSharedLogEntry();
        entry.readEntry(envImpl, currentEntryHeader, entryBuffer); 

        writePrintInfo((lastEntryType + " lsn=" +
                       DbLsn.getNoFormatString(getLastLsn())));

        return true;
    }

    private void writePrintInfo(String message) {
        PrintWriter out = null;
        try {
            File savedFile = 
                new File(envImpl.getEnvironmentHome(), SAVE_INFO_FILE);
            out = new PrintWriter
                (new BufferedWriter(new FileWriter(savedFile, true)));
            out.println(message);
        } catch (Exception e) {
            throw new IllegalStateException("Exception happens while " +
                                            "writing information into the " + 
                                            "info file " + e.getMessage());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    throw new IllegalStateException("Run into exception " +
                            "while closing the BufferedWriter: " + 
                            e.getMessage());
                }
            }
        }
    }
}
