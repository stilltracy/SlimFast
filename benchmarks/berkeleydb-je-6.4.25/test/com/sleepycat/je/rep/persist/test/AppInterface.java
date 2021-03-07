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

package com.sleepycat.je.rep.persist.test;

import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.persist.EntityStore;

public interface AppInterface {
    public void setVersion(final int label);
    public void setInitDuringOpen(final boolean doInit);
    public void open(final ReplicatedEnvironment env);
    public void close();
    public void writeData(final int key);
    public void writeDataA(final int key);
    public void writeDataB(final int key);
    public void writeDataC(final int key);
    public void writeData2(final int key);
    public void readData(final int key);
    public void readDataA(final int key);
    public void readDataB(final int key);
    public void readDataC(final int key);
    public void readData2(final int key);
    public void adopt(AppInterface other);
    public int getVersion();
    public ReplicatedEnvironment getEnv();
    public EntityStore getStore();
    /* For testRefreshBeforeWrite. */
    public void insertNullAnimal();
    public void readNullAnimal();
    public void insertDogAnimal();
    public void readDogAnimal();
    public void insertCatAnimal();
    public void readCatAnimal();
}
