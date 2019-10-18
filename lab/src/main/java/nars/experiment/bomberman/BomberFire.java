package nars.experiment.bomberman;

import java.awt.*;

/**
 * Title:        Bomberman
 * Description:
 * Copyright:    Copyright (c) 2001
 *
 * @author Sammy Leong
 * @version 1.0
 */

public class BomberFire extends Thread {
    /**
     * map object
     */
    private final BomberMap map;
    /**
     * position
     */
    private final int x;
    private final int y;
    /**
     * fire type
     */
    private final int type;
    /**
     * frame count
     */
    private int frame;
    /**
     * owner
     */
    private int owner;
    /**
     * bomb sprite image handles
     */
    private static Image[][] images;
    /**
     * rendering hints
     */
    private static Object hints;

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

    public BomberFire(BomberMap map, int x, int y, int type) {
        this.map = map;
        /**
         * map grid handle
         */
        int[][] grid = map.grid;
        this.x = x;
        this.y = y;
        this.type = type;
        this.owner -= 1;
        images = BomberMap.fireImages;

        if (type == BomberMap.FIRE_BRICK)
            grid[x >> BomberMain.shiftCount][y >> BomberMain.shiftCount] =
                    BomberMap.FIRE_BRICK;
        map.fireGrid[x >> BomberMain.shiftCount][y >> BomberMain.shiftCount] =
                true;

        /** see if there is a bonus in the same spot */
        if (map.bonusGrid[x >> BomberMain.shiftCount]
                [y >> BomberMain.shiftCount] != null) {
            /** if yes then remove it */
            map.removeBonus(x, y);
        }

        setPriority(Thread.MAX_PRIORITY);
        start();
    }

    /**
     * Main loop.
     */
    @Override
    public void run() {
        while (true) {
            /** draw the fire */
            paint();
            /** see if any players are in the way */
            for (int i = 0; i < BomberGame.totalPlayers; i++) {
                /** if there is */
                if ((BomberGame.players[i].x >> BomberMain.shiftCount) ==
                        (x >> BomberMain.shiftCount) && (BomberGame.players[i].y >>
                        BomberMain.shiftCount) == (y >> BomberMain.shiftCount)) {
                    /** then kill it */
                    BomberGame.players[i].kill();
                }
            }
            /** increase frame */
            frame += 1;
            /** sleep for 65 ms */
            try {
                sleep(65);
            } catch (Exception e) {
            }
            /** if frame is greater than 7 then it's finish burning */
            if (frame > 7) break;
        }
        map.grid[x >> BomberMain.shiftCount][y >> BomberMain.shiftCount] =
                BomberMap.NOTHING;
        map.fireGrid[x >> BomberMain.shiftCount][y >> BomberMain.shiftCount] =
                false;
        /** if this is a tail or brick, then it's the last fire in the chain */
        /** then refresh the screen */
        map.paintImmediately(x, y, BomberMain.size, BomberMain.size);
        /** if this was a brick then create a bonus there */
        if (type == BomberMap.FIRE_BRICK) {
            map.createBonus(x, y);
        }
    }

    /**
     * Drawing method.
     */
    public void paint() {
        Graphics g = map.getGraphics();
        /** if java runtime is Java 2 */
        if (Main.J2) {
            paint2D(map.getGraphics());
        }
        /** if java runtime isn't Java 2 */
        else {
            g.drawImage(images[type][frame], x, y,
                    BomberMain.size, BomberMain.size, null);
        }
        g.dispose();
    }

    /**
     * Drawing method for Java 2's Graphics2D
     *
     * @param graphics graphics handle
     */
    public void paint2D(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics;
        /** set the rendering hints */
        g2.setRenderingHints((RenderingHints) hints);
        g2.drawImage(images[type][frame], x, y,
                BomberMain.size, BomberMain.size, null);
    }
}