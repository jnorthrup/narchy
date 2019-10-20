package net.propero.rdp.tools;

public class HexDump {
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String dumpHexString(byte[] array) {
        return dumpHexString(array, 0, array.length);
    }

    private static String dumpHexString(byte[] array, int offset, int length) {
        StringBuilder result = new StringBuilder();

        result.append("0x");
        result.append(toHexString(offset));

        int lineIndex = 0;
        byte[] line = new byte[16];
        for (int i = offset; i < offset + length; i++) {
            if (lineIndex == 16) {
                result.append(' ');

                for (int j = 0; j < 16; j++) {
                    if ((int) line[j] > (int) ' ' && (int) line[j] < (int) '~') {
                        result.append(new String(line, j, 1));
                    } else {
                        result.append('.');
                    }
                }

                result.append("\n0x");
                result.append(toHexString(i));
                lineIndex = 0;
            }

            byte b = array[i];
            result.append(' ');
            result.append(HEX_DIGITS[((int) b >>> 4) & 0x0F]);
            result.append(HEX_DIGITS[(int) b & 0x0F]);

            line[lineIndex++] = b;
        }

        if (lineIndex != 16) {
            int count = (16 - lineIndex) * 3;
            count++;
            result.append(" ".repeat(Math.max(0, count)));

            for (int i = 0; i < lineIndex; i++) {
                if ((int) line[i] > (int) ' ' && (int) line[i] < (int) '~') {
                    result.append(new String(line, i, 1));
                } else {
                    result.append('.');
                }
            }
        }

        return result.toString();
    }

    public static String toHexString(byte b) {
        return toHexString(toByteArray(b));
    }

    private static String toHexString(byte[] array) {
        return toHexString(array, 0, array.length);
    }

    private static String toHexString(byte[] array, int offset, int length) {
        char[] buf = new char[length * 2];

        int bufIndex = 0;
        for (int i = offset; i < offset + length; i++) {
            byte b = array[i];
            buf[bufIndex++] = HEX_DIGITS[((int) b >>> 4) & 0x0F];
            buf[bufIndex++] = HEX_DIGITS[(int) b & 0x0F];
        }

        return new String(buf);
    }

    private static String toHexString(int i) {
        return toHexString(toByteArray(i));
    }

    private static byte[] toByteArray(byte b) {
        byte[] array = new byte[1];
        array[0] = b;
        return array;
    }

    private static byte[] toByteArray(int i) {
        byte[] array = new byte[4];

        array[3] = (byte) (i & 0xFF);
        array[2] = (byte) ((i >> 8) & 0xFF);
        array[1] = (byte) ((i >> 16) & 0xFF);
        array[0] = (byte) ((i >> 24) & 0xFF);

        return array;
    }

    private static int toByte(char c) {
        if ((int) c >= (int) '0' && (int) c <= (int) '9') return ((int) c - (int) '0');
        if ((int) c >= (int) 'A' && (int) c <= (int) 'F') return ((int) c - (int) 'A' + 10);
        if ((int) c >= (int) 'a' && (int) c <= (int) 'f') return ((int) c - (int) 'a' + 10);

        throw new RuntimeException("Invalid hex char '" + c + '\'');
    }

    public static byte[] hexStringToByteArray(String hexString) {
        int length = hexString.length();
        byte[] buffer = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            buffer[i / 2] = (byte) ((toByte(hexString.charAt(i)) << 4) | toByte(hexString.charAt(i + 1)));
        }

        return buffer;
    }
}
