package nars.experiment.bomberman;

import java.io.File;

/**
 * File:         BomberBGM
 * Copyright:    Copyright (c) 2001
 * @author Sammy Leong
 * @version 1.0
 */

/**
 * This class plays the background music.
 */
public class BomberSndEffect extends Thread {
    public BomberSndEffect() {
        start();
    }

    public void playSound(String str) {
        /** if Java 2 is available */
        if (Main.J2) {
            SoundPlayer sound = null;
//            try {
//                /** create sound player */
//                sound = new SoundPlayer(
//                new File(BomberMain.RP +
//                "Sounds/BomberSndEffects/" +  str + ".mid").
//                getCanonicalPath());
//            }
//            catch (Exception e) { }
//            /** open file */
//            sound.open();
//            /** then play sound */
//            sound.change(0, false);
        }
    }
}