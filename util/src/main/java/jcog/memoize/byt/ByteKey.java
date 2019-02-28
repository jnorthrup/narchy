package jcog.memoize.byt;

import jcog.Texts;
import jcog.pri.UnitPri;

import java.util.Arrays;

abstract public class ByteKey extends UnitPri {


    protected ByteKey() {
        super();
    }

    /** of size equal or greater than length() */
    abstract public byte[] array();

    abstract public int length();

    @Override
    public final boolean equals(Object o) {
        ByteKey that = (ByteKey) o;
        if (hashCode() == that.hashCode()) {
            return equalsBytes(that);
        }
        return false;
    }

    protected final boolean equalsBytes(ByteKey that) {
        int l = length();
        return that.length() == l && Arrays.equals(array(), 0, l, that.array(), 0, l);
    }


    @Override
    abstract public int hashCode();

    @Override
    public String toString() {
        return Texts.i(array(),0, length(), 16) + " [" + Integer.toUnsignedString(hashCode(),32) + "]";
    }


}
