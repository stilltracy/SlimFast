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
import java.io.IOException;
import java.io.FileInputStream;

/**
 * Simple ClassLoader to load class files from a given directory.  Does not
 * support jar files or multiple directories.
 */
public class SimpleClassLoader extends ClassLoader {
    
    private final File classPath;

    public SimpleClassLoader(ClassLoader parentLoader, File classPath) {
        super(parentLoader);
        this.classPath = classPath;
    }

    @Override
    public Class findClass(String className)
        throws ClassNotFoundException {

        try {
            final String fileName = className.replace('.', '/') + ".class";
            final File file = new File(classPath, fileName);
            final byte[] data = new byte[(int) file.length()];
            final FileInputStream fis = new FileInputStream(file);
            try {
                fis.read(data);
            } finally {
                fis.close();
            }
            return defineClass(className, data, 0, data.length);
        } catch (IOException e) {
            throw new ClassNotFoundException
                ("Class: " + className + " could not be loaded from: " +
                 classPath, e);
        }
    }
}
