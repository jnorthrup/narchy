package nars.experiment.mario;


import spacegraph.audio.Audio;
import spacegraph.audio.SoundSource;

import java.awt.*;


public abstract class Scene implements SoundSource {
    public Audio sound;
    public static boolean[] keys = new boolean[16];


    public static boolean key(int key) {
        return keys[key];
    }

    public static boolean key(int key, boolean isPressed) {
        boolean wasPressed = keys[key];
        keys[key] = isPressed;
        return wasPressed;
    }

    public final void setSound(Audio sound) {
        sound.setListener(this);
        this.sound = sound;
    }

    public abstract void init();

    public abstract void tick();

    public abstract void render(Graphics og, float alpha);
}