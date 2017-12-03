package jcog.data.byt;

import com.google.common.io.ByteArrayDataOutput;
import jcog.util.ArrayPool;
import org.apache.commons.lang3.ArrayUtils;
import org.iq80.snappy.Snappy;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 * dynamic byte array with mostly append-oriented functionality
 */
public class DynBytes implements ByteArrayDataOutput, Appendable, AbstractBytes {

    /**
     * must remain final for global consistency
     * might as well be 1.0, if it's already compressed to discover what this is, just keep it
     */
    private final static float minCompressionRatio = 1f;

    /**
     * must remain final for global consistency
     */
    private final static int MIN_COMPRESSION_BYTES = 72;

    static final int MIN_GROWTH_BYTES = 64;

    protected byte[] bytes;
    public int len;

    protected DynBytes() {
        this.bytes = null;
    }

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

        //TODO add parameter for from..to range compresion, currently this will only skip a prefix

        int to = length();
        int len = to - from;
        if (len < MIN_COMPRESSION_BYTES) {
            return -1;
        }


        int bufferLen = from + Snappy.maxCompressedLength(len);
        //byte[] compressed = new byte[bufferLen];
//        ArrayPool<byte[]> bb = ArrayPool.bytes();
        byte[] compressed = new byte[bufferLen]; //bb.getMin(bufferLen);

        int compressedLength = Snappy.compress(
                this.bytes, from, len,
                compressed, from);


        if (compressedLength < (len * minCompressionRatio)) {

            if (from > 0)
                System.arraycopy(this.bytes, 0, compressed, 0, from); //copy prefix
            //TODO copy suffix

            this.bytes = compressed;
            this.len = from + compressedLength;
            return compressedLength;
        } else {
            return -1;
            //bb.release(compressed);
        }

//        return -1;
    }


    @Override
    public int hashCode() {
        return hash(0, len);
    }

    public int hash(int from, int to) {
        return AbstractBytes.hash(bytes, from, to);
    }

    public long hash64(int from, int to) {
        return AbstractBytes.hash64(bytes, from, to);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        DynBytes d = (DynBytes) obj;
        int len = this.len;
        return d.len == len && Arrays.equals(bytes, 0, len, d.bytes, 0, len);
    }

    @Override
    public final int length() {
        return len;
//        return Integer.MAX_VALUE;
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
            return this; //no change

        return new WindowBytes(bytes, start, end);
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
    public final void write(@NotNull byte[] bytes) {
        this.write(bytes, 0, bytes.length);
    }

    @Override
    public final void write(@NotNull byte[] bytes, int off, int len) {
        int position = ensureSized(len);
        System.arraycopy(bytes, off, this.bytes, position, len);
        this.len = position + len;
    }

    private int ensureSized(int extra) {
        byte[] b = this.bytes;
        int space = b != null ? b.length : 0;
        int p = this.len;
        if (space - p <= extra) {
            this.bytes =
                    b != null ?
                            Arrays.copyOf(this.bytes, space + Math.max(extra, MIN_GROWTH_BYTES)) :
                            new byte[extra];
        }
        return p;
    }

    @Override
    public final byte[] array() {
        compact();
        return bytes;
    }

    public void compact() {
        compact(false);
    }

    public final void compact(boolean force) {
        compact(null, force);
    }

    public final void compact(byte[] forceIfSameAs, boolean force) {
        int l = this.len;
        if (l > 0) {
            byte[] b = this.bytes;
            if (force || b.length != l || forceIfSameAs == bytes)
                this.bytes = Arrays.copyOfRange(b, 0, l);
        }
    }


    @Override
    public final void toArray(byte[] c, int offset) {
        System.arraycopy(bytes, 0, c, offset, length());
    }

    @Override
    public String toString() {
        return Arrays.toString(ArrayUtils.subarray(bytes, 0, length()));
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
//        long bits = Double.doubleToLongBits(v);
//
//        int e = this.buffer.length - this.position;
//        if (e < 8) {
//
//            this.buffer[0] = (byte) ((int) (bits >> 56));
//            this.buffer[1] = (byte) ((int) (bits >> 48));
//            this.buffer[2] = (byte) ((int) (bits >> 40));
//            this.buffer[3] = (byte) ((int) (bits >> 32));
//            this.buffer[4] = (byte) ((int) (bits >> 24));
//            this.buffer[5] = (byte) ((int) (bits >> 16));
//            this.buffer[6] = (byte) ((int) (bits >> 8));
//            this.buffer[7] = (byte) ((int) bits);
//            this.position = 8;
//        } else {
//            int s = this.position;
//            this.position = s + 8;
//            this.buffer[s] = (byte) ((int) (bits >> 56));
//            this.buffer[s + 1] = (byte) ((int) (bits >> 48));
//            this.buffer[s + 2] = (byte) ((int) (bits >> 40));
//            this.buffer[s + 3] = (byte) ((int) (bits >> 32));
//            this.buffer[s + 4] = (byte) ((int) (bits >> 24));
//            this.buffer[s + 5] = (byte) ((int) (bits >> 16));
//            this.buffer[s + 6] = (byte) ((int) (bits >> 8));
//            this.buffer[s + 7] = (byte) ((int) bits);
//        }
//

    }

    @Override
    public void writeBytes(String s) {
//        int len = s.length();
//
//        for (int i = 0; i < len; ++i) {
//            this.write(s.charAt(i));
//        }
        throw new UnsupportedOperationException("TODO");

    }


    @Override
    @Deprecated
    public byte[] toByteArray() {
        return bytes;
    }

    @Override
    public void writeChars(String s) {
//        int len = s.length();
//
//        for (int i = 0; i < len; ++i) {
//            this.writeChar(s.charAt(i));
//        }
        throw new UnsupportedOperationException("TODO");

    }

    @Override
    public void writeUTF(@NotNull String s) {

        throw new UnsupportedOperationException("yet");

        //WARNING this isnt UTF8
//        this.write(strToBytes(s));
//        this.writeByte(0); //null-terminated

        //IO.writeWithPreLen(s, this);


//        byte[] ss = Hack.bytes(s);
//        this.writeShort(ss.length);
//        this.write(ss);

//        UTF8Writer
//        UTFUtils.writeUTFBytes(this, s);
    }

    @Override
    public Appendable append(CharSequence csq) {
        return append(csq, 0, csq.length());
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) {
        for (int i = start; i < end; i++) {
            writeChar(csq.charAt(i)); //TODO optimize
        }
        return this;
    }

    @Override
    public Appendable append(char c) {
        writeChar(c);
        return this;
    }

    public void appendTo(@NotNull DataOutput out) throws IOException {
        out.write(bytes, 0, len);
    }

    public void writeUnsignedByte(int i) {
        writeByte(i & 0xff);
    }

//    @Override
//    public void flush()  {
//        int pos = this.position;
//        ByteOutput byteOutput = this.byteOutput;
//        if (byteOutput != null) {
//            if (pos > 0) {
//                byteOutput.write(this.buffer, 0, pos);
//            }
//
//            this.position = 0;
//            byteOutput.flush();
//        }
//
//    }

//    protected void shallowFlush()  {
//        int pos = this.position;
//        ByteOutput byteOutput = this.byteOutput;
//        if (byteOutput != null) {
//            if (pos > 0) {
//                byteOutput.write(this.buffer, 0, pos);
//            }
//
//            this.position = 0;
//        }
//
//    }

//    protected void start(ByteOutput byteOutput)  {
//        this.byteOutput = byteOutput;
//        this.buffer = new byte[this.bufferSize];
//    }

//    protected void finish()  {
//        try {
//            
//        } finally {
//            this.buffer = null;
//            this.byteOutput = null;
//        }
//
//    }
//
//    @Override
//    public void close()  {
//        
//        this.byteOutput.close();
//    }
}
