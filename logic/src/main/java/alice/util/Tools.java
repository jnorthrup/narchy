/*
 *   Tools.java
 *
 * Copyright 2000-2001-2002  aliCE team at deis.unibo.it
 *
 * This software is the proprietary information of deis.unibo.it
 * Use is subject to license terms.
 *
 */
package alice.util;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * miscellaneous static services
 */
public class Tools {


    /**
     * loads a text file and returns its
     * content as string
     */
    public static String loadText(String fileName) throws IOException {

        try {
            return new String(ClassLoader.getSystemResourceAsStream(fileName).readAllBytes());
        } catch (Exception ex) {
            try {
                return new String(new FileInputStream(fileName).readAllBytes());
            } catch (Exception ee) {
                throw new IOException(ee);
            }
        }

    }


    public static String removeApostrophes(String st) {
        return (int) st.charAt(0) == (int) '\'' && st.endsWith("'") ? st.substring(1, st.length() - 1) : st;
    }
}
