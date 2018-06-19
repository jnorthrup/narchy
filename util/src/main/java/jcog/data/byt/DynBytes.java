package jcog.data.byt;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import org.apache.commons.lang3.ArrayUtils;
import org.iq80.snappy.Snappy;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.util.Arrays;

/**
 * dynamic byte array with mostly append-oriented functionality
 */
public class DynBytes implements ByteArrayDataOutput, Appendable, AbstractBytes {

    static final int MIN_GROWTH_BYTES = 64;
    /**
     * must remain final for global consistency
     * might as well be 1.0, if it's already compressed to discover what this is, just keep it
     */
    private final static float minCompressionRatio = 1f;
    /**
     * must remain final for global consistency
     */
    private final static int MIN_COMPRESSION_BYTES = 64;
    public int len;
    protected byte[] bytes;

    public DynBytes(int bufferSize) {
        this.bytes = new byte[bufferSize];
    }

    public DynBytes(byte[] zeroCopy) {
        this(zeroCopy, zeroCopy.length);
    }

    public DynBytes(byte[] zeroCopy, int len) {
        this.bytes = zeroCopy;
        this.len = len;
    }

    public int compress() {
        return compress(0);
    }


    /**
     * return length of the compressed region (not including the from offset).
     * or -1 if compression was not applied
     */
    public int compress(int from) {

        

        int to = length();
        int len = to - from;
        if (len < MIN_COMPRESSION_BYTES) {
            return -1;
        }


        int bufferLen = from + Snappy.maxCompressedLength(len);
        

        byte[] compressed = new byte[bufferLen]; 

        int compressedLength = Snappy.compress(
                this.bytes, from, len,
                compressed, from);


        if (compressedLength < (len * minCompressionRatio)) {

            if (from > 0)
                System.arraycopy(this.bytes, 0, compressed, 0, from); 
            

            this.bytes = compressed;
            this.len = from + compressedLength;
            return compressedLength;
        } else {
            return -1;
            
        }


    }


    @Override
    public int hashCode() {
        return Util.hash(bytes);
    }

    public int hash(int from, int to) {
        return Util.hash(bytes, from, to);
    }





    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        return equivalent((DynBytes) obj);
    }

    public boolean equivalent(DynBytes d) {
        int len = this.len;
        return d.len == len && Arrays.equals(bytes, 0, len, d.bytes, 0, len);
    }

    @Override
    public final int length() {
        return len;
    }

    public DynBytes rewind(int num) {
        if (num > 0) {
            len -= num;
            if (len < 0) throw new BufferUnderflowException();
        }
        return this;
    }

    @Override
    public final byte at(int index) {
        return bytes[index];
    }

    @Override
    public final AbstractBytes subSequence(int start, int end) {
        if (end - start == 1)
            return new OneByteSeq(at(start));

        if (start == 0 && end == length())
            return this; 

        return new ArrayBytes(bytes, start, end); //not window since this is mutable
    }

    @Override
    public final void write(int v) {
        writeByte(v);
    }


    @Override
    public final void writeByte(int v) {
        ensureSized(1);
        this.bytes[this.len++] = (byte) v;
    }

    /**
     * combo: (byte, int)
     */
    public final void write(byte b, int v) {
        int s = ensureSized(1 + 4);
        byte[] e = this.bytes;
        e[s++] = b;
        e[s++] = (byte) (v >> 24);
        e[s++] = (byte) (v >> 16);
        e[s++] = (byte) (v >> 8);
        e[s++] = (byte) v;
        this.len = s;
    }

    public final void fillBytes(byte b, int next) {
        int start = this.len;
        this.len += next;
        int end = this.len;
        Arrays.fill(bytes, start, end, b);
    }


    @Override
    public final void write(byte[] bytes) {
        this.write(bytes, 0, bytes.length);
    }

    @Override
    public final void write(byte[] bytes, int off, int len) {
        int position = ensureSized(len);
        System.arraycopy(bytes, off, this.bytes, position, len);
        this.len = position + len;
    }

    private int ensureSized(int extra) {
        byte[] b = this.bytes;
        int space = b.length;
        int p = this.len;
        if (space - p <= extra) {
            this.bytes = Arrays.copyOf(b, space + Math.max(extra, MIN_GROWTH_BYTES));
        }
        return p;
    }

    @Override public final byte[] array() {
        compact();
        return arrayDirect();
    }

    public final byte[] arrayDirect() {
        return bytes;
    }


    public final byte[] arrayCopy() {
        return array().clone();
    }

    public byte[] compact() {
        return compact(false);
    }

    public final byte[] compact(boolean force) {
        return compact(null, force);
    }

    public final byte[] compact(byte[] forceIfSameAs, boolean force) {
        int l = this.len;
        if (l > 0) {
            byte[] b = this.bytes;
            if (force || b.length != l || forceIfSameAs == bytes)
                return this.bytes = Arrays.copyOfRange(b, 0, l);
        } else {
            return this.bytes = ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        return bytes;
    }


    @Override
    public final void toArray(byte[] c, int offset) {
        System.arraycopy(bytes, 0, c, offset, length());
    }

    @Override
    public String toString() {
        return Arrays.toString(ArrayUtils.subarray(bytes, 0, length()));
    }

    public String toStringFromBytes() {
        return new String(bytes, 0, length());
    }


    @Override
    public final void writeBoolean(boolean v) {
        ensureSized(1);
        byte[] e = this.bytes;
        e[this.len++] = (byte) (v ? 1 : 0);
    }

    @Override
    public final void writeShort(int v) {
        int s = ensureSized(2);
        byte[] e = this.bytes;
        e[s] = (byte) (v >> 8);
        e[s + 1] = (byte) v;
        this.len += 2;
    }

    @Override
    public final void writeChar(int v) {

        int s = ensureSized(2);
        byte[] e = this.bytes;
        e[s] = (byte) (v >> 8);
        e[s + 1] = (byte) v;
        this.len += 2;

    }

    @Override
    public final void writeInt(int v) {

        int s = ensureSized(4);
        byte[] e = this.bytes;
        e[s] = (byte) (v >> 24);
        e[s + 1] = (byte) (v >> 16);
        e[s + 2] = (byte) (v >> 8);
        e[s + 3] = (byte) v;
        this.len += 4;
    }

    @Override
    public final void writeLong(long v) {

        int s = ensureSized(8);
        this.len += 8;
        byte[] e = this.bytes;
        e[s] = (byte) ((int) (v >> 56));
        e[s + 1] = (byte) ((int) (v >> 48));
        e[s + 2] = (byte) ((int) (v >> 40));
        e[s + 3] = (byte) ((int) (v >> 32));
        e[s + 4] = (byte) ((int) (v >> 24));
        e[s + 5] = (byte) ((int) (v >> 16));
        e[s + 6] = (byte) ((int) (v >> 8));
        e[s + 7] = (byte) ((int) v);
    }

    @Override
    public final void writeFloat(float v) {

        int s = ensureSized(4);
        byte[] e = this.bytes;
        this.len += 4;
        int bits = Float.floatToIntBits(v);
        e[s] = (byte) (bits >> 24);
        e[s + 1] = (byte) (bits >> 16);
        e[s + 2] = (byte) (bits >> 8);
        e[s + 3] = (byte) bits;
    }

    @Override
    public final void writeDouble(double v) {
        throw new UnsupportedOperationException("yet");




























    }

    @Override
    public void writeBytes(String s) {





        throw new UnsupportedOperationException("TODO");

    }


    @Override
    @Deprecated
    public byte[] toByteArray() {
        return bytes;
    }

    @Override
    public void writeChars(String s) {





        throw new UnsupportedOperationException("TODO");

    }

    //final UTF8Writer utf8 = new UTF8Writer();
    @Override
    public void writeUTF(String s) {

        write(s.getBytes());

    }

    @Override
    public Appendable append(CharSequence csq) {
        return append(csq, 0, csq.length());
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) {
        for (int i = start; i < end; i++) {
            writeChar(csq.charAt(i)); 
        }
        return this;
    }

    @Override
    public Appendable append(char c) {
        writeChar(c);
        return this;
    }

    public void appendTo(DataOutput out) throws IOException {
        out.write(bytes, 0, len);
    }

    public void writeUnsignedByte(int i) {
        writeByte(i & 0xff);
    }

    public void clear() {
        len = 0;
    }

    public RawBytes rawCopy() {
        return new RawBytes(compact(true));
    }


    public void appendTo(OutputStream o) throws IOException {
        o.write(bytes, 0, len);
    }

















































}
