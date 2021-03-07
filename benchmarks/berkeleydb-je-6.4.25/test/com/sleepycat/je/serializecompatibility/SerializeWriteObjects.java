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

package com.sleepycat.je.serializecompatibility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

import com.sleepycat.je.JEVersion;

public class SerializeWriteObjects {
    private File outputDir;

    public SerializeWriteObjects(String dirName) {
        outputDir = new File(dirName);
    }

    /* Delete the existed directory for serialized outputs. */
    private void deleteExistDir(File fileDir) {
        if (!fileDir.exists())
            return;
        if (fileDir.isFile()) {
            fileDir.delete();
            return;
        }

        File[] files = fileDir.listFiles();
        for (int i = 0; i < files.length; i++)
            deleteExistDir(files[i]);
        
        fileDir.delete();
    }

    /* 
     * If the directory doesn't exist, create a new one;
     * Or delete it and make a fresh one.
     */
    private void createHome() {
        if (outputDir.exists()) {
            deleteExistDir(outputDir);
        }

        outputDir.mkdirs();
    }

    /*
     * Generate a directory of .out files representing the serialized versions
     * of all serializable classes for this JE version. The directory will be 
     * named with JE version number, and each file will be named 
     * <classname>.out. These files will be used by SerializedReadObjectsTest.
     */
    public void writeObjects()
        throws IOException {

        createHome();
        ObjectOutputStream out;
        for (Map.Entry<String, Object> entry : 
             SerializeUtils.getSerializedSet().entrySet()) {
            out = new ObjectOutputStream
                (new FileOutputStream
                 (outputDir.getPath() + System.getProperty("file.separator") +
                  entry.getValue().getClass().getName() + ".out"));
            out.writeObject(entry.getValue());
            out.close();
        }
    }

    /* 
     * When do the test, it will create a sub process to run this main program
     * to call the writeObjects() method to generate serialized outputs.
     */
    public static void main(String args[]) 
        throws IOException {

        String dirName = args[0] + System.getProperty("file.separator") + 
                         JEVersion.CURRENT_VERSION.toString();
        SerializeWriteObjects writeTest = new SerializeWriteObjects(dirName);
        writeTest.writeObjects();
    }
}
