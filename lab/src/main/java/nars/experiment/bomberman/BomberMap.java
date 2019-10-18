package nars.experiment.bomberman;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Vector;

/**
 * File:         BomberMap.java
 * Copyright:    Copyright (c) 2001
 *
 * @author Sammy Leong
 * @version 1.0
 */

/**
 * This class draws the map and handles things like bonuses and bombs.
 */
public class BomberMap extends JPanel {
    /** game over flag */
    private boolean gameOver;
    /** background color */
    private final Color backgroundColor;
    /** the map grid array */
    public int[][] grid;
    /** fire grid */
    public boolean[][] fireGrid;
    /** bomb grid */
    public BomberBomb[][] bombGrid;
    /** bonus grid */
    public BomberBonus[][] bonusGrid;
    /** bombs */
    private final Vector bombs;
    /** bonuses */
    private final Vector bonuses;

    /**
     * Bomb info class
     */
    private class Bomb {
        public Bomb(int x, int y) {
            r = (x >> BomberMain.shiftCount);
            c = (y >> BomberMain.shiftCount);
        }

        public int r;
        public int c;
    }

    /**
     * Bonus info class
     */
    private class Bonus {
        public Bonus(int x, int y) {
            r = (x >> BomberMain.shiftCount);
            c = (y >> BomberMain.shiftCount);
        }

        public int r;
        public int c;
    }

    /** image handles for the map images */
    private static final Image[][] mapImages;
    /** bomb images */
    public static Image[] bombImages;
    /** fire images */
    public static Image[][] fireImages;
    /** fire brick images */
    public static Image[][] fireBrickImages;
    /** bonus images */
    public static Image[][] bonusImages;
    /** fire type enumerations */
    public static final int FIRE_CENTER = 0;
    public static final int FIRE_VERTICAL = 1;
    public static final int FIRE_HORIZONTAL = 2;
    public static final int FIRE_NORTH = 3;
    public static final int FIRE_SOUTH = 4;
    public static final int FIRE_EAST = 5;
    public static final int FIRE_WEST = 6;
    public static final int FIRE_BRICK = 7;
    /** grid slot type enumerations */
    public static final int BONUS_FIRE = -4;
    public static final int BONUS_BOMB = -3;
    public static final int NOTHING = -1;
    public static final int WALL = 0;
    public static final int BRICK = 1;
    public static final int BOMB = 3;
    /** random level generator */
    private static final BomberRandInt levelRand;
    /** random bonus generator */
    private static final BomberRandInt bonusRand;
    /** current level */
    public static int level;
    /** rendering hints */
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

        /** create the level random generator */
        levelRand = new BomberRandInt(0, 100);
        /** create the bonus random generator */
        bonusRand = new BomberRandInt(0, 7);
        /** creat the image objects array */
        mapImages = new Image[3][3];
        /** create the bomb objects array */
        bombImages = new Image[2];
        /** create the fire objects array */
        fireImages = new Image[8][8];
        /** create the fire brick objects array */
        fireBrickImages = new Image[3][8];
        /** create the bonus image objects array */
        bonusImages = new Image[2][2];

        try {
            String[] strs = new String[3];
            /** load the map images */
            for (int i = 0; i < 2; i++) {
                strs[0] = BomberMain.RP + "Images/BomberWalls/" + (i + 1);
                strs[1] = BomberMain.RP + "Images/BomberBricks/" + (i + 1);
                strs[2] = BomberMain.RP + "Images/BomberFloors/" + (i + 1);
                for (int j = 0; j < 3; j++) {
                    if (i == 0) strs[j] += ".jpg";
                    else strs[j] += ".gif";
                }
                mapImages[i][0] = Toolkit.getDefaultToolkit().getImage(
                        new File(strs[0]).getCanonicalPath());
                mapImages[i][1] = Toolkit.getDefaultToolkit().getImage(
                        new File(strs[1]).getCanonicalPath());
                if (i == 0) mapImages[0][2] = null;
                else
                    mapImages[i][2] = Toolkit.getDefaultToolkit().getImage(
                            new File(strs[2]).getCanonicalPath());
            }

            String str = null;
            /** load the bomb images */
            for (int i = 0; i < 2; i++) {
                str = BomberMain.RP + "Images/BomberBombs/" + (i + 1) + ".gif";
                bombImages[i] = Toolkit.getDefaultToolkit().getImage(
                        new File(str).getCanonicalPath());
            }

            /** load the fire images */
            for (int t = 0; t < 7; t++)
                for (int i = 0; i < 8; i++) {
                    str = BomberMain.RP + "Images/BomberFires/";
                    if (t == FIRE_CENTER) str += "C";
                    else if (t == FIRE_VERTICAL) str += "V";
                    else if (t == FIRE_NORTH) str += "N";
                    else if (t == FIRE_HORIZONTAL) str += "H";
                    else if (t == FIRE_EAST) str += "E";
                    else if (t == FIRE_WEST) str += "W";
                    else if (t == FIRE_SOUTH) str += "S";
                    if (t == FIRE_BRICK) fireImages[FIRE_BRICK][i] = null;
                    else {
                        str += (i + 1) + ".gif";
                        fireImages[t][i] = Toolkit.getDefaultToolkit().getImage(
                                new File(str).getCanonicalPath());
                    }
                }

            int f = 0;
            /** load the fire brick images */
            for (int i = 0; i < 2; i++)
                for (f = 0; f < 8; f++) {
                    str = BomberMain.RP + "Images/BomberFireBricks/" +
                            (i + 1) + (f + 1) + ".gif";
                    fireBrickImages[i][f] = Toolkit.getDefaultToolkit().getImage(
                            new File(str).getCanonicalPath());
                }

            /** load the bonus image sprites */
            for (int i = 0; i < 2; i++)
                for (f = 0; f < 2; f++) {
                    str = BomberMain.RP + "Images/BomberBonuses/" +
                            (i == 0 ? "F" : "B") + (f + 1) + ".gif";
                    bonusImages[i][f] = Toolkit.getDefaultToolkit().getImage(
                            new File(str).getCanonicalPath());
                }
        } catch (Exception e) {
            new ErrorDialog(e);
        }
    }

    public BomberMap(BomberMain main) {
        /** frame object */
        /** generator random level */
        level = levelRand.draw() % 2;
        MediaTracker tracker = new MediaTracker(this);
        /** prepare the images */
        try {
            int counter = 0;
            /** load the map images */
            for (int i = 0; i < 2; i++)
                for (int j = 0; j < 3; j++) {
                    if (mapImages[i][j] != null) {
                        tracker.addImage(mapImages[i][j], counter++);
                    }
                }
            /** load the bomb images */
            for (int i = 0; i < 2; i++)
                tracker.addImage(bombImages[i], counter++);
            /** load the fire brick images */
            System.arraycopy(fireBrickImages[level], 0, fireImages[FIRE_BRICK], 0, 8);
            /** load the fire images */
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++)
                    tracker.addImage(fireImages[i][j], counter++);

            /** wait for images to finish loading */
            tracker.waitForAll();
        } catch (Exception e) {
            new ErrorDialog(e);
        }

        bombs = new Vector();
        bonuses = new Vector();
        /** create the fire grid */
        fireGrid = new boolean[17][17];
        /** create the bomb grid */
        bombGrid = new BomberBomb[17][17];
        /** create the bonus grid */
        bonusGrid = new BomberBonus[17][17];
        /** create the map grid */
        grid = new int[17][17];
        /** fill the map with walls by alternating r by c */
        for (int r = 0; r < 17; r++)
            for (int c = 0; c < 17; c++) {
                /** if it's the edge */
                if (r == 0 || c == 0 || r == 16 || c == 16) grid[r][c] = WALL;
                else if ((r & 1) == 0 && (c & 1) == 0) grid[r][c] = WALL;
                else grid[r][c] = NOTHING;
                fireGrid[r][c] = false;
                bombGrid[r][c] = null;
                bonusGrid[r][c] = null;
            }

        int x, y;
        BomberRandInt ri = new BomberRandInt(1, 15);
        /** generate random bricks */
        for (int i = 0; i < 192 * 2; i++) {
            x = ri.draw();
            y = ri.draw();
            if (grid[x][y] == NOTHING)
                grid[x][y] = BRICK;
        }

        /** clear corners so players can stand there */
        grid[1][1] = grid[2][1] = grid[1][2] =
                grid[1][15] = grid[2][15] = grid[1][14] =
                        grid[15][1] = grid[14][1] = grid[15][2] =
                                grid[15][15] = grid[15][14] = grid[14][15] = NOTHING;

        /** create background color */
        backgroundColor = new Color(52, 108, 108);
        /** set panel size */
        setPreferredSize(new Dimension(17 << BomberMain.shiftCount,
                17 << BomberMain.shiftCount));
        /** double buffer on */
        setDoubleBuffered(true);

        setBounds(0, 0, 17 << BomberMain.shiftCount, 17 << BomberMain.shiftCount);
        setOpaque(false);
        /** add the map to the bottom layer */
        main.getLayeredPane().add(this, 1000);
    }

    /**
     * Sets game over flag on
     */
    public void setGameOver() {
        gameOver = true;
        paintImmediately(0, 0,
                17 << BomberMain.shiftCount, 17 << BomberMain.shiftCount);
    }

    /**
     * Creates a bonus.
     * @param x x-coordinate
     * @param y y-coordinate
     * @param owner owner
     */
    public synchronized void createBonus(int x, int y) {
        int _x = (x >> BomberMain.shiftCount) << BomberMain.shiftCount;
        int _y = (y >> BomberMain.shiftCount) << BomberMain.shiftCount;
        int type = bonusRand.draw();
        /** create bonus : 0 = fire; 1 = bomb */
        if (type == 0 || type == 1) {
            bonusGrid[_x >> BomberMain.shiftCount][_y >> BomberMain.shiftCount] =
                    new BomberBonus(this, _x, _y, type);
            bonuses.addElement(new Bonus(_x, _y));
        }
    }

    /**
     * Removes a bonus.
     * @param x x-coordinate
     * @param y y-coordinate
     */
    public synchronized void removeBonus(int x, int y) {
        int i = 0, k = bonuses.size();
        int r = (x >> BomberMain.shiftCount);
        int c = (y >> BomberMain.shiftCount);
        Bonus b = null;
        while (i < k) {
            b = (Bonus) bonuses.elementAt(i);
            if (b.r == r && b.c == c) {
                bonuses.removeElementAt(i);
                bonusGrid[b.r][b.c].kill();
                bonusGrid[b.r][b.c] = null;
                paintImmediately(b.r << BomberMain.shiftCount,
                        b.c << BomberMain.shiftCount, BomberMain.size,
                        BomberMain.size);
                break;
            }
            i += 1;
            k = bonuses.size();
        }
    }

    /**
     * Creates a bomb.
     * @param x x-coordinate
     * @param y y-coordinate
     * @param owner owner
     */
    public synchronized void createBomb(int x, int y, int owner) {
        int _x = (x >> BomberMain.shiftCount) << BomberMain.shiftCount;
        int _y = (y >> BomberMain.shiftCount) << BomberMain.shiftCount;
        bombGrid[_x >> BomberMain.shiftCount][_y >> BomberMain.shiftCount] =
                new BomberBomb(this, _x, _y, owner);
        bombs.addElement(new Bomb(_x, _y));
    }

    /**
     * Removes a bomb.
     * @param x x-coordinate
     * @param y y-coordinate
     */
    public synchronized void removeBomb(int x, int y) {
        int i = 0, k = bombs.size();
        int r = (x >> BomberMain.shiftCount);
        int c = (y >> BomberMain.shiftCount);
        Bomb b = null;
        while (i < k) {
            b = (Bomb) bombs.elementAt(i);
            if (b.r == r & b.c == c) {
                bombs.removeElementAt(i);
                break;
            }
            i += 1;
            k = bombs.size();
        }
    }

    /**
     * Creates a fire.
     * @param x x-coordinate
     * @param y y-coordinate
     * @param owner owner
     * @param type fire type
     */
    public void createFire(int x, int y, int owner, int type) {
        int _x = (x >> BomberMain.shiftCount) << BomberMain.shiftCount;
        int _y = (y >> BomberMain.shiftCount) << BomberMain.shiftCount;
        boolean createFire = false;
        /** if there's a bomb here */
        if (grid[_x >> BomberMain.shiftCount][_y >> BomberMain.shiftCount] ==
                BOMB) {
            /** then short the bomb */
            if (bombGrid[_x >> BomberMain.shiftCount][_y
                    >> BomberMain.shiftCount] != null)
                bombGrid[_x >> BomberMain.shiftCount][_y
                        >> BomberMain.shiftCount].shortBomb();
        }
        /** if there's no fire there already */
        else if (!fireGrid[_x >>
                BomberMain.shiftCount][_y >> BomberMain.shiftCount]) {
            createFire = true;
            /** create a fire there */
            BomberFire f = new BomberFire(this, _x, _y, type);
        }
        /** if this is a center */
        if (createFire && type == FIRE_CENTER) {
            int shiftCount = BomberMain.shiftCount;
            int size = BomberMain.size;
            /** then create a chain of fire */
            int northStop = 0, southStop = 0, westStop = 0, eastStop = 0,
                    northBlocks = 0, southBlocks = 0, westBlocks = 0, eastBlocks = 0;
            /** see how long the fire can be */
            for (int i = 1; i <= BomberGame.players[owner].fireLength; i++) {
                /** if it can still go south */
                if (southStop == 0) {
                    if (((_y >> shiftCount) + i) < 17) {
                        /** if there isnt't a wall there */
                        if (grid[_x >> shiftCount][(_y >> shiftCount) + i] != WALL) {
                            /** if there's something there though */
                            if (grid[_x >> shiftCount][(_y >> shiftCount) + i]
                                    != NOTHING)
                            /** then create a tail fire there */ {
                                southStop = grid[_x >> shiftCount]
                                        [(_y >> shiftCount) + i];
                            }
                            /** increase fire chain */
                            southBlocks += 1;
                        } else southStop = -1;
                    }
                }
                /** if it can still go north */
                if (northStop == 0) {
                    if (((_y >> shiftCount) - 1) >= 0) {
                        /** if there isn't a wall there */
                        if (grid[_x >> shiftCount][(_y >> shiftCount) - i] != WALL) {
                            /** if there's something there though */
                            if (grid[_x >> shiftCount][(_y >> shiftCount) - i]
                                    != NOTHING)
                            /** then create a tail fire there */ {
                                northStop = grid[_x >> shiftCount]
                                        [(_y >> shiftCount) - i];
                            }
                            /** increaes fire chain */
                            northBlocks += 1;
                        } else northStop = -1;
                    }
                }
                /** if it can still go east */
                if (eastStop == 0) {
                    if (((_x >> shiftCount) + i) < 17) {
                        /** if there isn't a wall there */
                        if (grid[(_x >> shiftCount) + i][_y >> shiftCount] != WALL) {
                            /** if there's somethign there though */
                            if (grid[(_x >> shiftCount) + i][_y >> shiftCount]
                                    != NOTHING)
                            /** then create a tail fire there */ {
                                eastStop = grid[(_x >> shiftCount) + i]
                                        [_y >> shiftCount];
                            }
                            /** increase fire chain */
                            eastBlocks += 1;
                        } else eastStop = -1;
                    }
                }
                /** if it can still go west */
                if (westStop == 0) {
                    if (((_x >> shiftCount) - i) >= 0) {
                        /** if there isn't a wall there */
                        if (grid[(_x >> shiftCount) - i][_y >> shiftCount] != WALL) {
                            /** if there's something there through */
                            if (grid[(_x >> shiftCount) - i]
                                    [_y >> shiftCount] != NOTHING)
                            /** then create a tail fire there */ {
                                westStop = grid[(_x >> shiftCount) - i]
                                        [_y >> shiftCount];
                            }
                            /** increase fire chain */
                            westBlocks += 1;
                        } else westStop = -1;
                    }
                }
            }
            /** create the north chain */
            for (int i = 1; i <= northBlocks; i++) {
                /** if this is a tail */
                if (i == northBlocks) {
                    /** if there's a brick */
                    if (northStop == BRICK)
                    /** then create a burning brick sprite */
                        createFire(_x, _y - (i * size), owner, FIRE_BRICK);
                    /** if it's not a brick then create a tail */
                    else createFire(_x, _y - (i * size), owner, FIRE_NORTH);
                }
                /** if it's not a tail then create a normal fire */
                else createFire(_x, _y - (i * size), owner, FIRE_VERTICAL);
            }
            for (int i = 1; i <= southBlocks; i++) {
                /** if this is a tail */
                if (i == southBlocks) {
                    /** if there's a brick */
                    if (southStop == BRICK)
                    /** then create a burning brick sprite */
                        createFire(_x, _y + (i * size), owner, FIRE_BRICK);
                    /** if it's not a brick then create a tail */
                    else createFire(_x, _y + (i * size), owner, FIRE_SOUTH);
                }
                /** if it's not a tail then create a normal fire */
                else createFire(_x, _y + (i * size), owner, FIRE_VERTICAL);
            }
            for (int i = 1; i <= eastBlocks; i++) {
                /** if this is a tail */
                if (i == eastBlocks) {
                    /** if there's a brick */
                    if (eastStop == BRICK)
                    /** then create a burning brick sprite */
                        createFire(_x + (i * size), _y, owner, FIRE_BRICK);
                    /** if it's not a brick then create a tail */
                    else createFire(_x + (i * size), _y, owner, FIRE_EAST);
                }
                /** if it's not a tail then create a normal fire */
                else createFire(_x + (i * size), _y, owner, FIRE_HORIZONTAL);
            }
            for (int i = 1; i <= westBlocks; i++) {
                /** if this is a tail */
                if (i == westBlocks) {
                    /** if there's a brick */
                    if (westStop == BRICK)
                    /** then create a burning brick sprite */
                        createFire(_x - (i * size), _y, owner, FIRE_BRICK);
                    /** if it's not a brick then create a tail */
                    else createFire(_x - (i * size), _y, owner, FIRE_WEST);
                }
                /** if it's not a tail then create a normal fire */
                else createFire(_x - (i * size), _y, owner, FIRE_HORIZONTAL);
            }
        }
    }

    /**
     * Drawing method.
     * @param graphics graphics handle
     */
    @Override
    public synchronized void paint(Graphics graphics) {
        Graphics g = graphics;
        /** if java runtime is Java 2 */
        if (Main.J2) {
            paint2D(graphics);
        }
        /** if java runtime isn't Java 2 */
        else {
            /** if game is over */
            if (gameOver) {
                /** fill the screen with black color */
                g.setColor(Color.black);
                g.fillRect(0, 0, 17 << BomberMain.shiftCount,
                        17 << BomberMain.shiftCount);
            }
            /** if game isn't over yet */
            else {
                /** fill window with background color */
                g.setColor(backgroundColor);
                g.fillRect(0, 0, 17 << BomberMain.shiftCount,
                        17 << BomberMain.shiftCount);
                /** draw the map */
                for (int r = 0; r < 17; r++)
                    for (int c = 0; c < 17; c++) {
                        /** if there's something in the block */
                        if (grid[r][c] > NOTHING &&
                                grid[r][c] != BOMB && grid[r][c] != FIRE_BRICK &&
                                mapImages[level][grid[r][c]] != null) {
                            g.drawImage(mapImages[level][grid[r][c]],
                                    r << BomberMain.shiftCount, c << BomberMain.shiftCount,
                                    BomberMain.size, BomberMain.size, null);
                        }
                        /** if the block is empty */
                        else {
                            if (mapImages[level][2] != null) {
                                /** draw the floor */
                                g.drawImage(mapImages[level][2],
                                        r << BomberMain.shiftCount, c <<
                                                BomberMain.shiftCount, BomberMain.size,
                                        BomberMain.size, null);
                            }
                        }
                    }
            }
        }
        if (!gameOver) {
            /** draw the bonuses */
            Bonus bb = null;
            int i = 0, k = bonuses.size();
            while (i < k) {
                bb = (Bonus) bonuses.elementAt(i);
                if (bonusGrid[bb.r][bb.c] != null)
                    bonusGrid[bb.r][bb.c].paint(g);
                i += 1;
                k = bonuses.size();
            }
            /** draw the bombs */
            Bomb b = null;
            i = 0;
            k = bombs.size();
            while (i < k) {
                b = (Bomb) bombs.elementAt(i);
                if (bombGrid[b.r][b.c] != null)
                    bombGrid[b.r][b.c].paint(g);
                i += 1;
                k = bombs.size();
            }
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
        /** if game is over */
        if (gameOver) {
            /** fill the screen with black color */
            g2.setColor(Color.black);
            g2.fillRect(0, 0, 17 << BomberMain.shiftCount,
                    17 << BomberMain.shiftCount);
        }
        /** if game isn't over yet */
        else {
            /** fill window with background color */
            g2.setColor(backgroundColor);
            g2.fillRect(0, 0, 17 << BomberMain.shiftCount,
                    17 << BomberMain.shiftCount);
            /** draw the map */
            for (int r = 0; r < 17; r++)
                for (int c = 0; c < 17; c++) {
                    /** if there's something in the block */
                    if (grid[r][c] > NOTHING &&
                            grid[r][c] != BOMB && grid[r][c] != FIRE_BRICK &&
                            mapImages[level][grid[r][c]] != null) {
                        g2.drawImage(mapImages[level][grid[r][c]],
                                r << BomberMain.shiftCount, c << BomberMain.shiftCount,
                                BomberMain.size, BomberMain.size, null);
                    }
                    /** if the block is empty */
                    else {
                        if (mapImages[level][2] != null) {
                            /** draw the floor */
                            g2.drawImage(mapImages[level][2],
                                    r << BomberMain.shiftCount, c <<
                                            BomberMain.shiftCount, BomberMain.size,
                                    BomberMain.size, null);
                        }
                    }
                }
        }
    }
}