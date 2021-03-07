package com.sleepycat.util;

/** System.arraycopy() is implemented in native code. We provide a Pure-Java
 * version here so that RoadRunner will instrument it. We provide type-specific
 * methods here for simplicity.
 */
public class ArrayCopy {

    public static void copy(byte[] src, int srcPos,
                            byte[] dest, int destPos, int length) {

        //System.arraycopy(src, srcPos, dest, destPos, length);

        // NB: detect errors here because the docs require that exns be thrown
        // without dest being modified
        if (src == null || dest == null) {
            throw new NullPointerException();
        }

        if (srcPos < 0 || destPos < 0 || length < 0 || 
            srcPos+length > src.length ||
            destPos+length > dest.length) {
            throw new IndexOutOfBoundsException();
        }

        if (0 == length) return;

        if (src == dest) { // need to use a temporary array
            byte[] tmp = new byte[length];
            for (int i = 0; i < length; i++) {
                tmp[i] = src[srcPos+i];
            }
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = tmp[i];
            }

        } else {
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = src[srcPos+i];
            }
        }
    }

    public static void copy(boolean[] src, int srcPos,
                            boolean[] dest, int destPos, int length) {

        //System.arraycopy(src, srcPos, dest, destPos, length);

        // NB: detect errors here because the docs require that exns be thrown
        // without dest being modified
        if (src == null || dest == null) {
            throw new NullPointerException();
        }

        if (srcPos < 0 || destPos < 0 || length < 0 || 
            srcPos+length > src.length ||
            destPos+length > dest.length) {
            throw new IndexOutOfBoundsException();
        }

        if (0 == length) return;

        if (src == dest) { // need to use a temporary array
            boolean[] tmp = new boolean[length];
            for (int i = 0; i < length; i++) {
                tmp[i] = src[srcPos+i];
            }
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = tmp[i];
            }

        } else {
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = src[srcPos+i];
            }
        }
    }

    public static void copy(char[] src, int srcPos,
                            char[] dest, int destPos, int length) {

        //System.arraycopy(src, srcPos, dest, destPos, length);

        // NB: detect errors here because the docs require that exns be thrown
        // without dest being modified
        if (src == null || dest == null) {
            throw new NullPointerException();
        }

        if (srcPos < 0 || destPos < 0 || length < 0 || 
            srcPos+length > src.length ||
            destPos+length > dest.length) {
            throw new IndexOutOfBoundsException();
        }

        if (0 == length) return;

        if (src == dest) { // need to use a temporary array
            char[] tmp = new char[length];
            for (int i = 0; i < length; i++) {
                tmp[i] = src[srcPos+i];
            }
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = tmp[i];
            }

        } else {
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = src[srcPos+i];
            }
        }
    }

    public static void copy(short[] src, int srcPos,
                            short[] dest, int destPos, int length) {

        //System.arraycopy(src, srcPos, dest, destPos, length);

        // NB: detect errors here because the docs require that exns be thrown
        // without dest being modified
        if (src == null || dest == null) {
            throw new NullPointerException();
        }

        if (srcPos < 0 || destPos < 0 || length < 0 || 
            srcPos+length > src.length ||
            destPos+length > dest.length) {
            throw new IndexOutOfBoundsException();
        }

        if (0 == length) return;

        if (src == dest) { // need to use a temporary array
            short[] tmp = new short[length];
            for (int i = 0; i < length; i++) {
                tmp[i] = src[srcPos+i];
            }
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = tmp[i];
            }

        } else {
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = src[srcPos+i];
            }
        }
    }

    public static void copy(int[] src, int srcPos,
                            int[] dest, int destPos, int length) {

        //System.arraycopy(src, srcPos, dest, destPos, length);

        // NB: detect errors here because the docs require that exns be thrown
        // without dest being modified
        if (src == null || dest == null) {
            throw new NullPointerException();
        }

        if (srcPos < 0 || destPos < 0 || length < 0 || 
            srcPos+length > src.length ||
            destPos+length > dest.length) {
            throw new IndexOutOfBoundsException();
        }

        if (0 == length) return;

        if (src == dest) { // need to use a temporary array
            int[] tmp = new int[length];
            for (int i = 0; i < length; i++) {
                tmp[i] = src[srcPos+i];
            }
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = tmp[i];
            }

        } else {
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = src[srcPos+i];
            }
        }
    }

    public static void copy(long[] src, int srcPos,
                            long[] dest, int destPos, int length) {

        //System.arraycopy(src, srcPos, dest, destPos, length);

        // NB: detect errors here because the docs require that exns be thrown
        // without dest being modified
        if (src == null || dest == null) {
            throw new NullPointerException();
        }

        if (srcPos < 0 || destPos < 0 || length < 0 || 
            srcPos+length > src.length ||
            destPos+length > dest.length) {
            throw new IndexOutOfBoundsException();
        }

        if (0 == length) return;

        if (src == dest) { // need to use a temporary array
            long[] tmp = new long[length];
            for (int i = 0; i < length; i++) {
                tmp[i] = src[srcPos+i];
            }
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = tmp[i];
            }

        } else {
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = src[srcPos+i];
            }
        }
    }

    public static void copy(float[] src, int srcPos,
                            float[] dest, int destPos, int length) {

        //System.arraycopy(src, srcPos, dest, destPos, length);

        // NB: detect errors here because the docs require that exns be thrown
        // without dest being modified
        if (src == null || dest == null) {
            throw new NullPointerException();
        }

        if (srcPos < 0 || destPos < 0 || length < 0 || 
            srcPos+length > src.length ||
            destPos+length > dest.length) {
            throw new IndexOutOfBoundsException();
        }

        if (0 == length) return;

        if (src == dest) { // need to use a temporary array
            float[] tmp = new float[length];
            for (int i = 0; i < length; i++) {
                tmp[i] = src[srcPos+i];
            }
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = tmp[i];
            }

        } else {
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = src[srcPos+i];
            }
        }
    }

    public static void copy(double[] src, int srcPos,
                            double[] dest, int destPos, int length) {

        //System.arraycopy(src, srcPos, dest, destPos, length);

        // NB: detect errors here because the docs require that exns be thrown
        // without dest being modified
        if (src == null || dest == null) {
            throw new NullPointerException();
        }

        if (srcPos < 0 || destPos < 0 || length < 0 || 
            srcPos+length > src.length ||
            destPos+length > dest.length) {
            throw new IndexOutOfBoundsException();
        }

        if (0 == length) return;

        if (src == dest) { // need to use a temporary array
            double[] tmp = new double[length];
            for (int i = 0; i < length; i++) {
                tmp[i] = src[srcPos+i];
            }
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = tmp[i];
            }

        } else {
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = src[srcPos+i];
            }
        }
    }

    public static void copy(Object[] src, int srcPos,
                            Object[] dest, int destPos, int length) {

        //System.arraycopy(src, srcPos, dest, destPos, length);

        // NB: detect errors here because the docs require that exns be thrown
        // without dest being modified
        if (src == null || dest == null) {
            throw new NullPointerException();
        }

        if (srcPos < 0 || destPos < 0 || length < 0 || 
            srcPos+length > src.length ||
            destPos+length > dest.length) {
            throw new IndexOutOfBoundsException();
        }

        if (0 == length) return;

        // ArrayStoreException will be thrown by the runtime if src[] elements
        // are incompatible with dest[] elements

        if (src == dest) { // need to use a temporary array
            Object[] tmp = new Object[length];
            for (int i = 0; i < length; i++) {
                tmp[i] = src[srcPos+i];
            }
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = tmp[i];
            }

        } else {
            for (int i = 0; i < length; i++) {
                dest[destPos+i] = src[srcPos+i];
            }
        }
    }

}
