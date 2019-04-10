package jcog.memoize.byt;

import jcog.Texts;

import java.util.Arrays;

public interface ByteKey  {


    /** of size equal or greater than length() */
    byte[] array();

    int length();


    static boolean equals(ByteKey a, ByteKey o) {
        return a.hashCode() == o.hashCode() && equalsBytes(a, o);
    }

    static boolean equalsBytes(ByteKey a, ByteKey b) {
        int l = a.length();
        return b.length() == l && Arrays.equals(a.array(), 0, l, b.array(), 0, l);
    }


    @Override
    int hashCode();


    static String toString(ByteKey b) {
        return Texts.i(b.array(),0, b.length(), 16) + " [" + Integer.toUnsignedString(b.hashCode(),32) + ']';
    }


}
