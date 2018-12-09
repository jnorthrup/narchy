package nars.experiment.mario;

import jcog.Util;
import nars.experiment.mario.level.LevelGenerator;
import nars.experiment.mario.sprites.Mario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


public class MarioComponent extends JComponent implements Runnable, KeyListener, FocusListener {

    int fps = 24;

    public void startGame() {


        int levelType = ThreadLocalRandom.current().nextFloat()  > 0.5f ?
                LevelGenerator.TYPE_UNDERGROUND
                :
                LevelGenerator.TYPE_OVERGROUND;

        startLevel((int) (Math.random() * 50000), 1,
                levelType
        );
    }

    private static final long serialVersionUID = 739318775993206607L;

    private boolean running;
    private final int width;
    private final int height;
    private GraphicsConfiguration graphicsConfiguration;
    public Scene scene;

    @SuppressWarnings("unused")
    private boolean focused;
    public MapScene mapScene;
    int delay;
    public BufferedImage image;

    public MarioComponent(int width, int height) {
        this.setFocusable(true);
        this.setEnabled(true);
        this.width = width;
        this.height = height;

        Dimension size = new Dimension(width, height);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);


        setFocusable(true);
    }

    private void toggleKey(int keyCode, boolean isPressed) {
        if (keyCode == KeyEvent.VK_LEFT) {
            scene.key(Mario.KEY_LEFT, isPressed);
        }
        if (keyCode == KeyEvent.VK_RIGHT) {
            scene.key(Mario.KEY_RIGHT, isPressed);
        }
        if (keyCode == KeyEvent.VK_DOWN) {
            scene.key(Mario.KEY_DOWN, isPressed);
        }
        if (keyCode == KeyEvent.VK_UP) {
            scene.key(Mario.KEY_UP, isPressed);
        }
        if (keyCode == KeyEvent.VK_A) {
            scene.key(Mario.KEY_SPEED, isPressed);
        }
        if (keyCode == KeyEvent.VK_S) {
            scene.key(Mario.KEY_JUMP, isPressed);
        }
        if (isPressed && keyCode == KeyEvent.VK_ESCAPE) {


            toTitle();


        }
    }

    @Override
    public void paint(Graphics g) {
    }

    @Override
    public void update(Graphics g) {
    }

    public void start() {
        if (!running) {
            running = true;
            new Thread(this, "Game Thread").start();
        }
    }

    public void stop() {
        Art.stopMusic();
        running = false;
    }

    @Override
    public void run() {
        graphicsConfiguration = getGraphicsConfiguration();

        mapScene = new MapScene(graphicsConfiguration, this, new Random().nextLong());
        scene = mapScene;



        Art.init(graphicsConfiguration);

        image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        image.setAccelerationPriority(1f);
        Graphics g = getGraphics();
        Graphics og = image.getGraphics();

        double time = System.nanoTime() / 1000000000.0;
        double now = time;
        long tm = System.currentTimeMillis();

        addKeyListener(this);
        addFocusListener(this);

        toTitle();
        adjustFPS();


        while (running) {
            scene.tick();

//            long nextTM = System.currentTimeMillis();
//            float alpha = (float) (System.currentTimeMillis() - tm);


            @SuppressWarnings("unused")
            int x = (int) (Math.sin(now) * 16 + 160);
            @SuppressWarnings("unused")
            int y = (int) (Math.cos(now) * 16 + 120);

            og.setColor(Color.BLACK);
            og.fillRect(0, 0, 320, 240);

            float alpha = 0;
            scene.render(og, alpha);

//            if (lTick / 4 % 2 == 0 && (scene instanceof TitleScene)) {
//                String msg = "INSERT COIN";
//
//
//
//            }

            if (width != 320 || height != 240) {
                g.drawImage(image, 0, 0, width, height, null);
            } else {
                g.drawImage(image, 0, 0, null);
            }

            if (delay > 0)
                tm += delay;
            Util.sleepMS(Math.max(0, tm - System.currentTimeMillis()));

        }

        Art.stopMusic();
    }

    private void drawString(Graphics g, String text, int x, int y, int c) {
        char[] ch = text.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            g.drawImage(Art.font[ch[i] - 32][c], x + i * 8, y, null);
        }
    }

    @Override
    public void keyPressed(KeyEvent arg0) {
        toggleKey(arg0.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent arg0) {
        toggleKey(arg0.getKeyCode(), false);
    }

    public void startLevel(long seed, int difficulty, int type) {
        scene = new LevelScene(graphicsConfiguration, this, seed, difficulty, type);

        scene.init();
    }

    public void levelFailed() {
        startGame();




    }

    @Override
    public void keyTyped(KeyEvent arg0) {
    }

    @Override
    public void focusGained(FocusEvent arg0) {
        focused = true;
    }

    @Override
    public void focusLost(FocusEvent arg0) {
        focused = false;
    }

    public void levelWon() {
        scene = mapScene;
        mapScene.startMusic();
        mapScene.levelWon();
    }

    public void win() {
        scene = new WinScene(this);

        scene.init();
    }

    public void toTitle() {
        Mario.resetStatic();


        startGame();
    }

    public void lose() {
        scene = new LoseScene(this);

        scene.init();
    }


    void adjustFPS() {

        delay = (fps > 0) ? (fps >= 100) ? 0 : (1000 / fps) : 100;

    }
}