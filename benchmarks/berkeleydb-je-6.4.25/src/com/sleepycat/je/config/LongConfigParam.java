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

package com.sleepycat.je.config;

/**
 * A JE configuration parameter with an integer value.
 */
public class LongConfigParam extends ConfigParam {

    private static final String DEBUG_NAME = LongConfigParam.class.getName();

    private Long min;
    private Long max;

    public LongConfigParam(String configName,
                           Long minVal,
                           Long maxVal,
                           Long defaultValue,
                           boolean mutable,
                           boolean forReplication) {

        /* defaultValue must not be null. */
        super(configName, defaultValue.toString(), mutable, forReplication);
        min = minVal;
        max = maxVal;
    }

    /**
     * Self validate. Check mins and maxs
     */
    private void validate(Long value)
        throws IllegalArgumentException {

        if (value != null) {
            if (min != null) {
                if (value.compareTo(min) < 0) {
                    throw new IllegalArgumentException
                        (DEBUG_NAME + ":" +
                         " param " + name +
                         " doesn't validate, " +
                         value +
                         " is less than min of "
                         + min);
                }
            }
            if (max != null) {
                if (value.compareTo(max) > 0) {
                    throw new IllegalArgumentException
                        (DEBUG_NAME + ":" +
                         " param " + name +
                         " doesn't validate, " +
                         value +
                         " is greater than max "+
                         " of " +  max);
                }
            }
        }
    }

    @Override
    public void validateValue(String value)
        throws IllegalArgumentException {

        try {
            validate(new Long(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException
                (DEBUG_NAME + ": " +  value + " not valid value for " + name);
        }
    }
}
