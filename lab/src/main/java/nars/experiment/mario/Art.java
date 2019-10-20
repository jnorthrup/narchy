package nars.experiment.mario;


import spacegraph.audio.sample.SoundSample;

import javax.imageio.ImageIO;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;


public class Art {
    public static final int SAMPLE_BREAK_BLOCK = 0;
    public static final int SAMPLE_GET_COIN = 1;
    public static final int SAMPLE_MARIO_JUMP = 2;
    public static final int SAMPLE_MARIO_STOMP = 3;
    public static final int SAMPLE_MARIO_KICK = 4;
    public static final int SAMPLE_MARIO_POWER_UP = 5;
    public static final int SAMPLE_MARIO_POWER_DOWN = 6;
    public static final int SAMPLE_MARIO_DEATH = 7;
    public static final int SAMPLE_ITEM_SPROUT = 8;
    public static final int SAMPLE_CANNON_FIRE = 9;
    public static final int SAMPLE_SHELL_BUMP = 10;
    public static final int SAMPLE_LEVEL_EXIT = 11;
    public static final int SAMPLE_MARIO_1UP = 12;
    public static final int SAMPLE_MARIO_FIREBALL = 13;

    public static Image[][] mario;
    public static Image[][] smallMario;
    public static Image[][] fireMario;
    public static Image[][] enemies;
    public static Image[][] items;
    public static Image[][] level;
    public static Image[][] particles;
    public static Image[][] font;
    public static Image[][] bg;
    public static Image[][] map;
    public static Image[][] endScene;
    public static Image[][] gameOver;
    public static Image logo;
    public static Image titleScreen;

    public static SoundSample[] samples = new SoundSample[100];

    private static final Sequence[] songs = new Sequence[10];
    private static Sequencer sequencer;


    public static void init(GraphicsConfiguration gc) {
        try {
            mario = cutImage(gc, "mariosheet.png", 32, 32);
            smallMario = cutImage(gc, "smallmariosheet.png", 16, 16);
            fireMario = cutImage(gc, "firemariosheet.png", 32, 32);
            enemies = cutImage(gc, "enemysheet.png", 16, 32);
            items = cutImage(gc, "itemsheet.png", 16, 16);
            level = cutImage(gc, "mapsheet.png", 16, 16);
            map = cutImage(gc, "worldmap.png", 16, 16);
            particles = cutImage(gc, "particlesheet.png", 8, 8);
            bg = cutImage(gc, "bgsheet.png", 32, 32);
            logo = getImage(gc, "logo.gif");
            titleScreen = getImage(gc, "title.gif");
            font = cutImage(gc, "font.gif", 8, 8);
            endScene = cutImage(gc, "endscene.gif", 96, 96);
            gameOver = cutImage(gc, "gameovergost.gif", 96, 64);


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static Image getImage(GraphicsConfiguration gc, String imageName) throws IOException {
        BufferedImage source = ImageIO.read(Art.class.getResourceAsStream(imageName));
        Image image = gc.createCompatibleImage(source.getWidth(), source.getHeight(), Transparency.BITMASK);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return image;
    }

    private static Image[][] cutImage(GraphicsConfiguration gc, String imageName, int xSize, int ySize) throws IOException {
        Image source = getImage(gc, imageName);
        Image[][] images = new Image[source.getWidth(null) / xSize][source.getHeight(null) / ySize];
        for (int x = 0; x < source.getWidth(null) / xSize; x++) {
            for (int y = 0; y < source.getHeight(null) / ySize; y++) {
                Image image = gc.createCompatibleImage(xSize, ySize, Transparency.BITMASK);
                Graphics2D g = (Graphics2D) image.getGraphics();
                g.setComposite(AlphaComposite.Src);
                g.drawImage(source, -x * xSize, -y * ySize, null);
                g.dispose();
                images[x][y] = image;
            }
        }

        return images;
    }

    public static void startMusic(int song) {
        stopMusic();


    }

    public static void stopMusic() {
        if (sequencer != null) {
            try {
                sequencer.stop();
                sequencer.close();
            } catch (Exception e) {
            }
        }
    }
}