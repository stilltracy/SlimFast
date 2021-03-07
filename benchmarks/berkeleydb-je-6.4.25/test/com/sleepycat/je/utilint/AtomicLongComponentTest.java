/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2002, 2014 Oracle and/or its affiliates.  All rights reserved.
 *
 */

package com.sleepycat.je.utilint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sleepycat.util.test.TestBase;

/** Test the AtomicLongComponent class */
public class AtomicLongComponentTest extends TestBase {

    @Test
    public void testConstructor() {
        AtomicLongComponent comp = new AtomicLongComponent();
        assertEquals(Long.valueOf(0), comp.get());
    }

    @Test
    public void testSet() {
        AtomicLongComponent comp = new AtomicLongComponent();
        comp.set(72);
        assertEquals(Long.valueOf(72), comp.get());
    }

    @Test
    public void testClear() {
        AtomicLongComponent comp = new AtomicLongComponent();
        comp.set(37);
        comp.clear();
        assertEquals(Long.valueOf(0), comp.get());
    }

    @Test
    public void testCopy() {
        AtomicLongComponent comp = new AtomicLongComponent();
        comp.set(70);
        AtomicLongComponent copy = comp.copy();
        comp.clear();
        assertEquals(Long.valueOf(70), copy.get());
        copy.set(75);
        assertEquals(Long.valueOf(0), comp.get());
    }

    @Test
    public void testGetFormattedValue() {
        AtomicLongComponent comp = new AtomicLongComponent();
        comp.set(123456789);
        assertEquals("123,456,789", comp.getFormattedValue(true));
        assertEquals("123456789", comp.getFormattedValue(false));
    }

    @Test
    public void testIsNotSet() {
        AtomicLongComponent comp = new AtomicLongComponent();
        assertTrue(comp.isNotSet());
        comp.set(3);
        assertFalse(comp.isNotSet());
        comp.clear();
        assertTrue(comp.isNotSet());
    }

    @Test
    public void testToString() {
        AtomicLongComponent comp = new AtomicLongComponent();
        comp.set(987654321);
        assertEquals("987654321", comp.toString());
    }
}
