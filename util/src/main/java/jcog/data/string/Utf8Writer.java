package jcog.data.string;


import java.io.DataOutput;
import java.io.IOException;

/**
 * <p>This class is used to write a stream of chars as a stream of
 * bytes using the UTF8 encoding. It assumes that the underlying
 * output stream is buffered or does not need additional buffering.</p>
 *
 * <p>It is more efficient than using a <code>java.io.OutputStreamWriter</code>
 * because it does not need to be wrapped in a
 * <code>java.io.BufferedWriter</code>. Creating multiple instances
 * of <code>java.io.BufferedWriter</code> has been shown to be very
 * expensive in JAX-WS.</p>
 *
 * @author Santiago.PericasGeertsen@sun.com
 */
public final class Utf8Writer  {



    /**
     * Undelying output stream. This class assumes that this
     * output stream does not need buffering.
     */
    private final DataOutput out;

    /**
     * Java represents chars that are not in the Basic Multilingual
     * Plane (BMP) in UTF-16. This int stores the first code unit
     * for a code point encoded in two UTF-16 code units.
     */
    private int lastUTF16CodePoint;

    public Utf8Writer(DataOutput out) {
        this.out = out;
    }


    static boolean isHighSurrogate(int c) {
        return (0xD800 <= c && c <= 0xDBFF);
    }
    static boolean isLowSurrogate(int c) {
        return (0xDC00 <= c && c <= 0xDFFF);
    }

    public void write(int c) throws IOException {
        
        if (lastUTF16CodePoint != 0) {
            final int uc =
                (((lastUTF16CodePoint & 0x3ff) << 10) | (c & 0x3ff)) + 0x10000;

            if (uc < 0 || uc >= 0x200000) {
                throw new IOException("Atttempting to write invalid Unicode code point '" + uc + '\'');
            }

            out.write(0xF0 | (uc >> 18));
            out.write(0x80 | ((uc >> 12) & 0x3F));
            out.write(0x80 | ((uc >> 6) & 0x3F));
            out.write(0x80 | (uc & 0x3F));

            lastUTF16CodePoint = 0;
            return;
        }

        
        if (c < 0x80) {
            
            out.write(c);
        }
        else if (c < 0x800) {
            
            out.write(0xC0 | (c >> 6));    
            out.write(0x80 | (c & 0x3F));  
        }
        else if (c <= '\uFFFF') {
            if (!isHighSurrogate(c) && !isLowSurrogate(c)) {
                
                out.write(0xE0 | (c >> 12));   
                out.write(0x80 | ((c >> 6) & 0x3F));  
                out.write(0x80 | (c & 0x3F));  
            }
            else {
                lastUTF16CodePoint = c;
            }
        }
    }

    public void write(char[] cbuf) throws IOException {
        for (int i = 0; i < cbuf.length; i++) {
            write(cbuf[i]);
        }
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            write(cbuf[off + i]);
        }
    }

    public void write(String str) throws IOException {
        final int len = str.length();
        for (int i = 0; i < len; i++) {
            write(str.charAt(i));
        }
    }

    public void write(String str, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            write(str.charAt(off + i));
        }
    }


}
