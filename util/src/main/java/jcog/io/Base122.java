package jcog.io;

import jcog.data.bit.MetalBitSet;
import org.eclipse.collections.api.map.primitive.ImmutableByteByteMap;
import org.eclipse.collections.impl.map.mutable.primitive.ByteByteHashMap;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** https://github.com/patrickfav/base122-java */
public enum Base122 { ;
    private static final byte kShortened = (byte) 0b111; // Uses the illegal index to signify the last two-byte char encodes <= 7 bits.

    private static final MetalBitSet illegal = MetalBitSet.bits(128);
    private static ImmutableByteByteMap illegalFwd;
    private static ImmutableByteByteMap illegalRev;

    static {
        ByteByteHashMap ILLEGAL_BYTES_fwd = new ByteByteHashMap(16), ILLEGAL_BYTES_rev = new ByteByteHashMap(16);
        for (byte b : new byte[]{
                (byte) 0  //null
            , (byte) 10 // newline
            , (byte) 13 // carriage return
            , (byte) 34 // double quote
            , (byte) 38 // ampersand
            , (byte) 92 // backslash
        }) {
          illegal.set((int) b);
            byte bi = (byte) ILLEGAL_BYTES_fwd.size();
            ILLEGAL_BYTES_fwd.put(b, bi);
            ILLEGAL_BYTES_rev.put(bi, b);
        }
        Base122.illegalFwd = ILLEGAL_BYTES_fwd.toImmutable();
        Base122.illegalRev = ILLEGAL_BYTES_rev.toImmutable();
    }

    public static String encode(byte[] data) {
        return new Encoder(data).encode();
    }

    public static byte[] decode(String encodedBase122) {
        return new Decoder().decode(encodedBase122);
    }
    public static byte[] decode(byte[] encodedBase122) {
        return new Decoder().decode(encodedBase122);
    }

    static class Encoder {
        private static final byte STOP_BYTE = (byte) 0b1000_0000;
        private int curIndex = 0;
        private int curBit = 0;
        private byte[] rawData;

        Encoder(byte[] rawData) {
            this.rawData = rawData;
        }

        byte next7Bit() {
            try {
                if (curIndex >= rawData.length) {
                    return STOP_BYTE;
                }

                // Shift, mask, unshift to get first part.
                byte firstByte = rawData[curIndex];
                int firstPart = ((0b11111110 >>> curBit) & (int) firstByte) << curBit;
                // Align it to a seven bit chunk.
                firstPart >>>= 1;
                // Check if we need to go to the next byte for more bits.
                curBit += 7;
                if (curBit < 8) return (byte) firstPart; // Do not need next byte.
                curBit -= 8;
                curIndex++;
                // Now we want bits [0..curBit] of the next byte if it exists.
                if (curIndex >= rawData.length) return (byte) firstPart;
                byte secondByte = rawData[curIndex];
                int secondPart = ((0xFF00 >>> curBit) & (int) secondByte) & 0xFF;
                // Align it.
                secondPart >>>= 8 - curBit;
                return (byte) (firstPart | secondPart);
            } finally {
                //System.out.println("curByte: " + curIndex + ", curBit: " + curBit);
            }
        }


        String encode() {
            byte sevenBits;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(rawData.length + (rawData.length / 8) + 1);
            while ((int) (sevenBits = next7Bit()) != (int) STOP_BYTE) {
                int illegalIndex;
                if ((illegalIndex = isIllegalCharacter(sevenBits)) != -1) {
                    // Since this will be a two-byte character, get the next chunk of seven bits.
                    byte nextSevenBits = next7Bit();

                    byte b1 = (byte) 0b11000010;
                    if ((int) nextSevenBits == (int) STOP_BYTE) {
                        b1 = (byte) ((int) b1 | (0b111 & (int) kShortened) << 2);
                        nextSevenBits = sevenBits; // Encode these bits after the shortened signifier.
                    } else {
                        b1 = (byte) ((int) b1 | (0b111 & illegalIndex) << 2);
                    }

                    // Push first bit onto first byte, remaining 6 onto second.
                    byte firstBit = (byte) (((int) nextSevenBits & 0b01000000) > 0 ? 1 : 0);
                    b1 = (byte) ((int) b1 | (int) firstBit);
                    byte b2 = (byte) 0b10000000;
                    b2 = (byte) ((int) b2 | (int) nextSevenBits & 0b00111111);
                    outputStream.write((int) b1);
                    outputStream.write((int) b2);
                } else {
                    outputStream.write((int) sevenBits);
                }
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }


    }

    private static int isIllegalCharacter(byte sevenBits) {
        return (int) (illegal.get((int) sevenBits) ? illegalFwd.get(sevenBits) : (byte) -1);
    }

    static final class Decoder {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private byte curByte = (byte) 0;
        private byte bitOfByte = (byte) 0;

        void pushNext7(int nextElement) {
            nextElement <<= 1;
            // Align this byte to offset for current byte.
            curByte = (byte) ((int) curByte | (nextElement >>> (int) bitOfByte));
            bitOfByte = (byte) ((int) bitOfByte + 7);
            if ((int) bitOfByte >= 8) {
                outputStream.write((int) curByte);
                bitOfByte = (byte) ((int) bitOfByte - 8);
                // Now, take the remainder, left shift by what has been taken.
                curByte = (byte) ((nextElement << (7 - (int) bitOfByte)) & 255);
            }
        }

        byte[] decode(String base122Data) {
            byte[] utf8Bytes = base122Data.getBytes(StandardCharsets.UTF_8);
            return decode(utf8Bytes);
        }

        byte[] decode(byte[] utf8Bytes) {
            for (int i = 0; i < utf8Bytes.length; i++) {
                // Check if this is a two-byte character.
                if ((int) utf8Bytes[i] > 127) {
                    // Note, the charCodeAt will give the codePoint, thus
                    // 0b110xxxxx 0b10yyyyyy will give => xxxxxyyyyyy
                    int illegalIndex = ((int) utf8Bytes[i] >>> 8) & 7; // 7 = 0b111.
                    // We have to first check if this is a shortened two-byte character, i.e. if it only
                    // encodes <= 7 bits.
                    if (illegalIndex != (int) kShortened) pushNext7((int) illegalRev.get((byte) illegalIndex));
                    // Always push the rest.
                    pushNext7((int) utf8Bytes[i] & 127);
                } else {
                    // One byte characters can be pushed directly.
                    pushNext7((int) utf8Bytes[i]);
                }
            }
            return outputStream.toByteArray();
        }
    }

}
