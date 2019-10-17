package nars.experiment.bomberman;

/**
 * File:         BomberBonus
 * Copyright:    Copyright (c) 2001
 *
 * @author Sammy Leong
 * @version 1.0
 */

import java.awt.*;

/**
 * This class creates the bonuses in the game.
 */
public class BomberBonus extends Thread {
    /** map object */
    private final BomberMap map;
    /** position */
    private final int x;
    private final int y;
    /** frame count */
    private int frame;
    /** alive flag */
    private boolean alive = true;
    /** bonus type */
    private final int type;
    /** bomb sprite image handles */
    private final Image[] images;
    /** rendering hints */
    private static Object hints;

    private static final int FIRE = 0;
    private static final int BOMB = 1;

    static {
        /** if java runtime is Java 2 */
        if (Main.J2) {
            /** create the rendering hints for better graphics output */
            RenderingHints h = new RenderingHints(null);
            h.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            h.put(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            h.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            h.put(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            h.put(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            hints = h;
        }
    }

    /**
     * Constructs a bonus.
     * @param map map object
     * @param x x-coordinate
     * @param y y-coordinage
     * @param type bonus type;
     */
    public BomberBonus(BomberMap map, int x, int y, int type) {
        this.map = map;
        this.x = x;
        this.y = y;
        this.type = type;
        this.images = BomberMap.bonusImages[type];

        setPriority(Thread.MAX_PRIORITY);
        start();
    }

    /**
     * Main loop.
     */
    @Override
    public synchronized void run() {
        while (alive) {
            /** draw the bonus */
            map.paintImmediately(x, y, BomberMain.size, BomberMain.size);
            /** rotate frame */
            frame = (frame + 1) % 2;
            /** sleep for 130 ms */
            try {
                sleep(130);
            } catch (Exception e) {
            }
            if (frame == 10) break;
        }
        /** remove it from the grid */
        map.removeBonus(x, y);
    }

    /**
     * Gives this bonus to a user then removes it.
     */
    public void giveToPlayer(int player) {
        BomberMain.sndEffectPlayer.playSound("Bonus");
        /** if it's a fire bonus */
        if (type == FIRE) /** then increase the fire length by 1 */
            BomberGame.players[player - 1].fireLength += 1;
        /** if it's a bomb bonus */
        else if (type == BOMB) /** then increase the bomb count by 1 */
            BomberGame.players[player - 1].totalBombs += 1;
        kill();
    }

    /**
     * Kills the object along with the thread
     */
    public void kill() {
        alive = false;
        interrupt();
    }

    /**
     * Drawing method.
     */
    public void paint(Graphics g) {
        /** if java runtime is Java 2 */
        if (Main.J2) {
            paint2D(g);
        }
        /** if java runtime isn't Java 2 */
        else {
            g.drawImage(images[frame], x, y,
                    BomberMain.size, BomberMain.size, null);
        }
    }

    /**
     * Drawing method for Java 2's Graphics2D
     * @param graphics graphics handle
     */
    public void paint2D(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics;
        /** set the rendering hints */
        g2.setRenderingHints((RenderingHints) hints);
        g2.drawImage(images[frame], x, y,
                BomberMain.size, BomberMain.size, null);
    }
}