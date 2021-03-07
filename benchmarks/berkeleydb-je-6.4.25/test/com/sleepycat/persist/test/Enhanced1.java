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

package com.sleepycat.persist.test;

import static com.sleepycat.persist.model.Relationship.MANY_TO_ONE;

import com.sleepycat.persist.impl.Enhanced;
import com.sleepycat.persist.impl.EnhancedAccessor;
import com.sleepycat.persist.impl.EntityInput;
import com.sleepycat.persist.impl.EntityOutput;
import com.sleepycat.persist.impl.Format;
import com.sleepycat.persist.impl.RefreshException;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;

/**
 * For running ASMifier -- adds minimal enhancements.
 */
@Entity
class Enhanced1 implements Enhanced {

    @PrimaryKey
    private String f1;

    @SecondaryKey(relate=MANY_TO_ONE)
    private int f2;
    @SecondaryKey(relate=MANY_TO_ONE)
    private String f3;
    @SecondaryKey(relate=MANY_TO_ONE)
    private String f4;

    private int f5;
    private String f6;
    private String f7;
    private int f8;
    private int f9;
    private int f10;
    private int f11;
    private int f12;

    static {
        EnhancedAccessor.registerClass(null, new Enhanced1());
    }

    public Object bdbNewInstance() {
        return new Enhanced1();
    }

    public Object bdbNewArray(int len) {
        return new Enhanced1[len];
    }

    public boolean bdbIsPriKeyFieldNullOrZero() {
        return f1 == null;
    }

    public void bdbWritePriKeyField(EntityOutput output, Format format)
        throws RefreshException {

        output.writeKeyObject(f1, format);
    }

    public void bdbReadPriKeyField(EntityInput input, Format format)
        throws RefreshException {

        f1 = (String) input.readKeyObject(format);
    }

    public void bdbWriteSecKeyFields(EntityOutput output)
        throws RefreshException {

        /* If primary key is an object: */
        output.registerPriKeyObject(f1);
        /* Always: */
        output.writeInt(f2);
        output.writeObject(f3, null);
        output.writeObject(f4, null);
    }

    public void bdbReadSecKeyFields(EntityInput input,
                                    int startField,
                                    int endField,
                                    int superLevel)
        throws RefreshException {

        /* If primary key is an object: */
        input.registerPriKeyObject(f1);

        if (superLevel <= 0) {
            switch (startField) {
            case 0:
                f2 = input.readInt();
                if (endField == 0) break;
            case 1:
                f3 = (String) input.readObject();
                if (endField == 1) break;
            case 2:
                f4 = (String) input.readObject();
            }
        }
    }

    public void bdbWriteNonKeyFields(EntityOutput output)
        throws RefreshException {

        output.writeInt(f5);
        output.writeObject(f6, null);
        output.writeObject(f7, null);
        output.writeInt(f8);
        output.writeInt(f9);
        output.writeInt(f10);
        output.writeInt(f11);
        output.writeInt(f12);
    }

    public void bdbReadNonKeyFields(EntityInput input,
                                    int startField,
                                    int endField,
                                    int superLevel)
        throws RefreshException {

        if (superLevel <= 0) {
            switch (startField) {
            case 0:
                f5 = input.readInt();
                if (endField == 0) break;
            case 1:
                f6 = (String) input.readObject();
                if (endField == 1) break;
            case 2:
                f7 = (String) input.readObject();
                if (endField == 2) break;
            case 3:
                f8 = input.readInt();
                if (endField == 3) break;
            case 4:
                f9 = input.readInt();
                if (endField == 4) break;
            case 5:
                f10 = input.readInt();
                if (endField == 5) break;
            case 6:
                f11 = input.readInt();
                if (endField == 6) break;
            case 7:
                f12 = input.readInt();
            }
        }
    }

    public void bdbWriteCompositeKeyFields(EntityOutput output,
                                           Format[] formats) {
    }

    public void bdbReadCompositeKeyFields(EntityInput input,
                                          Format[] formats) {
    }

    public boolean bdbNullifyKeyField(Object o,
                                      int field,
                                      int superLevel,
                                      boolean isSecField,
                                      Object keyElement) {
        if (superLevel > 0) {
            return false;
        } else if (isSecField) {
            switch (field) {
            case 1:
                if (f3 != null) {
                    f3 = null;
                    return true;
                } else {
                    return false;
                }
            case 2:
                if (f4 != null) {
                    f4 = null;
                    return true;
                } else {
                    return false;
                }
            default:
                return false;
            }
        } else {
            switch (field) {
            case 1:
                if (f6 != null) {
                    f6 = null;
                    return true;
                } else {
                    return false;
                }
            case 2:
                if (f7 != null) {
                    f7 = null;
                    return true;
                } else {
                    return false;
                }
            default:
                return false;
            }
        }
    }

    public Object bdbGetField(Object o,
                              int field,
                              int superLevel,
                              boolean isSecField) {
        if (superLevel > 0) {
        } else if (isSecField) {
            switch (field) {
            case 0:
                return Integer.valueOf(f2);
            case 1:
                return f3;
            case 2:
                return f4;
            }
        } else {
            switch (field) {
            case 0:
                return Integer.valueOf(f5);
            case 1:
                return f6;
            case 2:
                return f7;
            }
        }
        return null;
    }

    public void bdbSetField(Object o,
                            int field,
                            int superLevel,
                            boolean isSecField,
                            Object value) {
        if (superLevel > 0) {
        } else if (isSecField) {
            switch (field) {
            case 0:
                f2 = ((Integer) value).intValue();
                return;
            case 1:
                f3 = (String) value;
                return;
            case 2:
                f4 = (String) value;
                return;
            }
        } else {
            switch (field) {
            case 0:
                f5 = ((Integer) value).intValue();
                return;
            case 1:
                f6 = (String) value;
                return;
            case 2:
                f7 = (String) value;
                return;
            }
        }
    }
    
    public void bdbSetPriField(Object o, Object value) {
        f1 = (String) value;
    }
}
