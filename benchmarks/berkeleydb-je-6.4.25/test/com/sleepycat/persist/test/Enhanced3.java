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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import com.sleepycat.persist.impl.Enhanced;
import com.sleepycat.persist.impl.EnhancedAccessor;
import com.sleepycat.persist.impl.EntityInput;
import com.sleepycat.persist.impl.EntityOutput;
import com.sleepycat.persist.impl.Format;
import com.sleepycat.persist.impl.RefreshException;
import com.sleepycat.persist.model.KeyField;
import com.sleepycat.persist.model.Persistent;

/**
 * For running ASMifier -- a composite key class using all simple data types,
 * does not follow from previous EnhancedN.java files
 */
@Persistent
class Enhanced3 implements Enhanced {

    enum MyEnum { ONE, TWO };

    @KeyField(1) boolean z;
    @KeyField(2) char c;
    @KeyField(3) byte b;
    @KeyField(4) short s;
    @KeyField(5) int i;
    @KeyField(6) long l;
    @KeyField(7) float f;
    @KeyField(8) double d;

    @KeyField(9) Boolean zw;
    @KeyField(10) Character cw;
    @KeyField(11) Byte bw;
    @KeyField(12) Short sw;
    @KeyField(13) Integer iw;
    @KeyField(14) Long lw;
    @KeyField(15) Float fw;
    @KeyField(16) Double dw;

    @KeyField(17) Date date;
    @KeyField(18) String str;
    @KeyField(19) MyEnum e;
    @KeyField(20) BigInteger bigint;
    @KeyField(21) BigDecimal bigdec;

    static {
        EnhancedAccessor.registerClass(null, new Enhanced3());
    }

    public Object bdbNewInstance() {
        return new Enhanced3();
    }

    public Object bdbNewArray(int len) {
        return new Enhanced3[len];
    }

    public boolean bdbIsPriKeyFieldNullOrZero() {
        return false;
    }

    public void bdbWritePriKeyField(EntityOutput output, Format format) {
    }

    public void bdbReadPriKeyField(EntityInput input, Format format) {
    }

    public void bdbWriteSecKeyFields(EntityOutput output) {
    }

    public void bdbReadSecKeyFields(EntityInput input,
                                    int startField,
                                    int endField,
                                    int superLevel) {
    }

    public void bdbWriteNonKeyFields(EntityOutput output) {
    }

    public void bdbReadNonKeyFields(EntityInput input,
                                    int startField,
                                    int endField,
                                    int superLevel) {
    }

    public void bdbWriteCompositeKeyFields(EntityOutput output,
                                           Format[] formats)
        throws RefreshException {

        output.writeBoolean(z);
        output.writeChar(c);
        output.writeByte(b);
        output.writeShort(s);
        output.writeInt(i);
        output.writeLong(l);
        output.writeSortedFloat(f);
        output.writeSortedDouble(d);

        output.writeBoolean(zw.booleanValue());
        output.writeChar(cw.charValue());
        output.writeByte(bw.byteValue());
        output.writeShort(sw.shortValue());
        output.writeInt(iw.intValue());
        output.writeLong(lw.longValue());
        output.writeSortedFloat(fw.floatValue());
        output.writeSortedDouble(dw.doubleValue());

        output.writeLong(date.getTime());
        output.writeString(str);
        output.writeKeyObject(e, formats[18]);
        output.writeBigInteger(bigint);
        output.writeSortedBigDecimal(bigdec);
    }

    public void bdbReadCompositeKeyFields(EntityInput input,
                                          Format[] formats)
        throws RefreshException {

        z = input.readBoolean();
        c = input.readChar();
        b = input.readByte();
        s = input.readShort();
        i = input.readInt();
        l = input.readLong();
        f = input.readSortedFloat();
        d = input.readSortedDouble();

        zw = Boolean.valueOf(input.readBoolean());
        cw = Character.valueOf(input.readChar());
        bw = Byte.valueOf(input.readByte());
        sw = Short.valueOf(input.readShort());
        iw = Integer.valueOf(input.readInt());
        lw = Long.valueOf(input.readLong());
        fw = Float.valueOf(input.readSortedFloat());
        dw = Double.valueOf(input.readSortedDouble());

        date = new Date(input.readLong());
        str = input.readString();
        e = (MyEnum) input.readKeyObject(formats[18]);
        bigint = input.readBigInteger();
        bigdec = input.readSortedBigDecimal();
    }

    public boolean bdbNullifyKeyField(Object o,
                                      int field,
                                      int superLevel,
                                      boolean isSecField,
                                      Object keyElement) {
        // Didn't bother with this one.
        return false;
    }

    public Object bdbGetField(Object o,
                              int field,
                              int superLevel,
                              boolean isSecField) {
        // Didn't bother with this one.
        return null;
    }

    public void bdbSetField(Object o,
                            int field,
                            int superLevel,
                            boolean isSecField,
                            Object value) {
        // Didn't bother with this one.
    }
    
    public void bdbSetPriField(Object o, Object value) {
        // Didn't bother with this one.
    }
}
