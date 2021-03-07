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

import java.io.File;
import java.io.FilenameFilter;
import java.util.StringTokenizer;

public class InfoFileFilter implements FilenameFilter {

    /**
     * Accept files of this format:
     * je.info.#
     */
    public boolean accept(File dir, String name) {
        boolean ok = false;
        StringTokenizer tokenizer = new StringTokenizer(name, ".");
        // there should be two parts
        if (tokenizer.countTokens() == 3) {
            String filePrefix = tokenizer.nextToken();
            String fileSuffix = tokenizer.nextToken();
            String repeat = tokenizer.nextToken();

            // check the length and the suffix
            if (filePrefix.equals("je") && fileSuffix.equals("info")) {
                // The last part should be a number
                try {
                    Integer.parseInt(repeat);
                    ok = true;
                } catch (NumberFormatException e) {
                    ok = false;
                }
            }
        }

        return ok;
    }
}
