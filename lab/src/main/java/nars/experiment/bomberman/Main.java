package nars.experiment.bomberman;/*
 * Main.java
 * Created on February 17, 2001, 12:51 AM
 */

import javax.swing.*;

/**
 * This class is the starting proint of the program.
 *
 * @author Sammy Leong
 * @version 1.0
 */
public class Main {
    public static BomberMain bomberMain;

    /**
     * relative path
     */
    public static final String RP = "./";
    /**
     * flag: whether current machine's java runtime is version 2 or not
     */
    public static boolean J2;


    /**
     * Starts Bomberman
     */
    public static void startBomberman() {
        bomberMain = new BomberMain();
    }

    /**
     * Starts the program by creating an instance of MainFrame.
     */
    public static void main(String[] args) {
        boolean bombermanMode = false;
        boolean badArg = false;
        /** default look and feel: metal */
        int lookAndFeel = 1;
        /** check supplied parameters (if any) */
        for (int i = 0; i < args.length; i++) {
            /** if "bomberman" parameter is supplied */
            if ("Bomberman".equals(args[i]) || "bomberman".equals(args[i]))
                bombermanMode = true;
            /** if look and feel parameter is supplied */
            if (args[i].startsWith("-l")) {
                switch (args[i].substring(2)) {
                    case "System":
                        lookAndFeel = 0;
                        break;
                    case "Metal":
                        lookAndFeel = 1;
                        break;
                    case "Windows":
                        lookAndFeel = 2;
                        break;
                    case "Mac":
                        lookAndFeel = 3;
                        break;
                    case "Motif":
                        lookAndFeel = 4;
                        break;
                }
            }
        }
        /** if look and feel isn't default: metal */
        if (lookAndFeel != 1) {
            try {
                /**
                 * available look and feels:
                 * =========================
                 * "javax.swing.plaf.metal.MetalLookAndFeel"
                 * "com.sun.java.swing.plaf.windows.WindowsLookAndFeel"
                 * "com.sun.java.swing.plaf.motif.MotifLookAndFeel"
                 * "javax.swing.plaf.mac.MacLookAndFeel"
                 */
                String laf = "javax.swing.plaf.metal.MetalLookAndFeel";
                switch (lookAndFeel) {
                    case 0:
                        laf = UIManager.getSystemLookAndFeelClassName();
                        break;
                    case 1:
                        laf = "javax.swing.plaf.metal.MetalLookAndFeel";
                        break;
                    case 2:
                        laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
                        break;
                    case 3:
                        laf = "javax.swing.plaf.mac.MacLookAndFeel";
                        break;
                    case 4:
                        laf = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
                        break;
                }
                UIManager.setLookAndFeel(laf);
            } catch (Exception e) {
                new ErrorDialog(e);
            }
        }

        startBomberman();
    }
}