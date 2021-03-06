package nars.experiment.mario;

import nars.experiment.mario.level.BgLevelGenerator;
import nars.experiment.mario.level.Level;
import nars.experiment.mario.level.LevelGenerator;
import nars.experiment.mario.level.SpriteTemplate;
import nars.experiment.mario.sprites.*;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class LevelScene extends Scene implements SpriteContext {

    private final List<Sprite> sprites = new ArrayList<>();
    private final List<Sprite> spritesToAdd = new ArrayList<>();
    private final List<Sprite> spritesToRemove = new ArrayList<>();

    public Level level;
    public final Mario mario;
    public float xCam;
    public float yCam;
    public float xCamO;
    public float yCamO;
    private int tick;

    public LevelRenderer layer;
    private final BgRenderer[] bgLayer = new BgRenderer[2];

    private final GraphicsConfiguration graphicsConfiguration;

    public boolean paused;
    public int startTime;
    private int timeLeft;


    private final long levelSeed;
    private final MarioComponent renderer;
    private final int levelType;
    private final int levelDifficulty;

    public LevelScene(GraphicsConfiguration graphicsConfiguration, MarioComponent renderer, long seed, int levelDifficulty, int type) {
        this.graphicsConfiguration = graphicsConfiguration;
        this.levelSeed = seed;
        this.renderer = renderer;
        this.levelDifficulty = levelDifficulty;
        this.levelType = type;
        mario = new Mario(this);

    }

    @Override
    public void init() {

        try {
            Level.loadBehaviors(new DataInputStream(LevelScene.class.getResourceAsStream("tiles.dat")));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        /*        if (replayer!=null)
         {
         level = LevelGenerator.createLevel(2048, 15, replayer.nextLong());
         }
         else
         {*/

        level = LevelGenerator.createLevel(320, 15, levelSeed, levelDifficulty, levelType);
        

        /*        if (recorder != null)
         {
         recorder.addLong(LevelGenerator.lastSeed);
         }*/

        switch (levelType) {
            case LevelGenerator.TYPE_OVERGROUND:
                Art.startMusic(1);
                break;
            case LevelGenerator.TYPE_UNDERGROUND:
                Art.startMusic(2);
                break;
            case LevelGenerator.TYPE_CASTLE:
                Art.startMusic(3);
                break;
        }


        paused = false;
        Sprite.spriteContext = this;
        sprites.clear();
        layer = new LevelRenderer(level, graphicsConfiguration, 320, 240);
        for (int i = 0; i < 2; i++) {
            int scrollSpeed = 4 >> i;
            int w = ((level.width * 16) - 320) / scrollSpeed + 320;
            int h = ((level.height * 16) - 240) / scrollSpeed + 240;
            Level bgLevel = BgLevelGenerator.createLevel(w / 32 + 1, h / 32 + 1, i == 0, levelType);
            bgLayer[i] = new BgRenderer(bgLevel, graphicsConfiguration, 320, 240, scrollSpeed);
        }
        sprites.add(mario);
        startTime = 1;

        timeLeft = 200 * 15;

        tick = 0;
    }

    public int fireballsOnScreen;

    List<Shell> shellsToCheck = new ArrayList<>();

    public void checkShellCollide(Shell shell) {
        shellsToCheck.add(shell);
    }

    List<Fireball> fireballsToCheck = new ArrayList<>();

    public void checkFireballCollide(Fireball fireball) {
        fireballsToCheck.add(fireball);
    }

    @Override
    public synchronized void tick() {

        if (--timeLeft == 0) {
            mario.die();
        }
        xCamO = xCam;
        yCamO = yCam;

        if (startTime > 0) {
            startTime++;
        }

        float targetXCam = mario.x - 160.0F;

        xCam = targetXCam;

        if (level == null)
            return;

        if (xCam < (float) 0) xCam = (float) 0;
        if (xCam > (float) (level.width * 16 - 320)) xCam = (float) (level.width * 16 - 320);

        /*      if (recorder != null)
         {
         recorder.addTick(mario.getKeyMask());
         }
         
         if (replayer!=null)
         {
         mario.setKeys(replayer.nextTick());
         }*/

        fireballsOnScreen = 0;

        for (Sprite sprite : sprites) {
            if (sprite != mario) {
                float xd = sprite.x - xCam;
                float yd = sprite.y - yCam;
                if (xd < -64.0F || xd > (float) (320 + 64) || yd < -64.0F || yd > (float) (240 + 64)) {
                    removeSprite(sprite);
                } else {
                    if (sprite instanceof Fireball) {
                        fireballsOnScreen++;
                    }
                }
            }
        }

        if (paused) {
            for (Sprite sprite : sprites) {
                if (sprite == mario) {
                    sprite.tick();
                } else {
                    sprite.tickNoMove();
                }
            }
        } else {
            tick++;
            level.tick();

            boolean hasShotCannon = false;
            int xCannon = 0;

//            if (layer == null)
//                return;

            for (int x = (int) xCam / 16 - 1; x <= (int) (xCam + (float) layer.width) / 16 + 1; x++)
                for (int y = (int) yCam / 16 - 1; y <= (int) (yCam + (float) layer.height) / 16 + 1; y++) {
                    int dir = 0;

                    if ((float) (x * 16 + 8) > mario.x + 16.0F) dir = -1;
                    if ((float) (x * 16 + 8) < mario.x - 16.0F) dir = 1;

                    SpriteTemplate st = level.getSpriteTemplate(x, y);

                    if (st != null) {
                        if (st.lastVisibleTick != tick - 1) {
                            if (st.sprite == null || !sprites.contains(st.sprite)) {
                                st.spawn(this, x, y, dir);
                            }
                        }

                        st.lastVisibleTick = tick;
                    }

                    if (dir != 0) {
                        byte b = level.getBlock(x, y);
                        if (((int) (Level.TILE_BEHAVIORS[(int) b & 0xff]) & Level.BIT_ANIMATED) != 0) {
                            if (((int) b % 16) / 4 == 3 && (int) b / 16 == 0) {
                                if ((tick - x * 2) % 100 == 0) {
                                    xCannon = x;
                                    for (int i = 0; i < 8; i++) {
                                        addSprite(new Sparkle(x * 16 + 8, y * 16 + (int) (Math.random() * 16.0), (float) Math.random() * (float) dir, (float) 0, 0, 1, 5));
                                    }
                                    addSprite(new BulletBill(this, (float) (x * 16 + 8 + dir * 8), (float) (y * 16 + 15), dir));
                                    hasShotCannon = true;
                                }
                            }
                        }
                    }
                }

            if (hasShotCannon) {

            }

            for (Sprite sprite : sprites) {
                sprite.tick();
            }

            for (Sprite sprite : sprites) {
                sprite.collideCheck();
            }

            for (Shell shell : shellsToCheck) {
                for (Sprite sprite : sprites) {
                    if (sprite != shell && !shell.dead) {
                        if (sprite.shellCollideCheck(shell)) {
                            if (mario.carried == shell && !shell.dead) {
                                mario.carried = null;
                                shell.die();
                            }
                        }
                    }
                }
            }
            shellsToCheck.clear();

            for (Fireball fireball : fireballsToCheck) {
                for (Sprite sprite : sprites) {
                    if (sprite != fireball && !fireball.dead) {
                        if (sprite.fireballCollideCheck(fireball)) {
                            fireball.die();
                        }
                    }
                }
            }
            fireballsToCheck.clear();
        }

        sprites.addAll(0, spritesToAdd);
        spritesToAdd.clear();
        sprites.removeAll(spritesToRemove);
        spritesToRemove.clear();
    }

    private final DecimalFormat df = new DecimalFormat("00");
    private final DecimalFormat df2 = new DecimalFormat("000");

    @Override
    public void render(Graphics g, float alpha) {
        LevelRenderer layer = this.layer;
//        if (level == null || layer == null)
//            return;

        int xCam = (int) (mario.xOld + (mario.x - mario.xOld) * alpha) - 160;
        int yCam = (int) (mario.yOld + (mario.y - mario.yOld) * alpha) - 120;


        if (xCam < 0) xCam = 0;
        if (yCam < 0) yCam = 0;
        if (xCam > level.width * 16 - 320) xCam = level.width * 16 - 320;
        if (yCam > level.height * 16 - 240) yCam = level.height * 16 - 240;


        boolean renderBackground = false;
        if (renderBackground) {
            for (int i = 0; i < 2; i++) {
                bgLayer[i].setCam(xCam, yCam);
                bgLayer[i].render(g, tick, alpha);
            }
        }

        g.translate(-xCam, -yCam);
        for (Sprite sprite : sprites) {
            if (sprite.layer == 0) sprite.render(g, alpha);
        }
        g.translate(xCam, yCam);

        layer.setCam(xCam, yCam);
        layer.render(g, tick, paused ? (float) 0 : alpha);
        layer.renderExit0(g, tick, paused ? (float) 0 : alpha, mario.winTime == 0);

        g.translate(-xCam, -yCam);
        for (Sprite sprite : sprites) {
            if (sprite.layer == 1) sprite.render(g, alpha);
        }
        g.translate(xCam, yCam);
        g.setColor(Color.BLACK);
        layer.renderExit1(g, tick, paused ? (float) 0 : alpha);

        drawStringDropShadow(g, "MARIO " + df.format((long) Mario.lives), 0, 0, 7);
        drawStringDropShadow(g, "00000000", 0, 1, 7);

        drawStringDropShadow(g, "COIN", 14, 0, 7);
        drawStringDropShadow(g, " " + df.format((long) Mario.coins), 14, 1, 7);

        drawStringDropShadow(g, "WORLD", 24, 0, 7);
        drawStringDropShadow(g, " " + Mario.levelString, 24, 1, 7);

        drawStringDropShadow(g, "TIME", 35, 0, 7);
        int time = (timeLeft + 15 - 1) / 15;
        if (time < 0) time = 0;
        drawStringDropShadow(g, " " + df2.format((long) time), 35, 1, 7);


        if (startTime > 0) {
            float t = (float) startTime + alpha - 2.0F;
            t = t * t * 0.6f;
//            renderBlackout(g, 160, 120, (int) (t));
        }

        if (mario.winTime > 0) {
            float t = (float) mario.winTime + alpha;
            t = t * t * 0.2f;

            if (t > 900.0F) {
                renderer.levelWon();


            }

//            renderBlackout(g, mario.xDeathPos - xCam, mario.yDeathPos - yCam, (int) (320 - t));
        }

        if (mario.deathTime > 0) {
            if (mario.deathTime-- > 2) {
                //pause
            } else {
                mario.deathTime = 0;
                renderer.levelFailed();
            }


        }
    }

    private static void drawStringDropShadow(Graphics g, String text, int x, int y, int c) {
        drawString(g, text, x * 8 + 5, y * 8 + 5, 0);
        drawString(g, text, x * 8 + 4, y * 8 + 4, c);
    }

    private static void drawString(Graphics g, String text, int x, int y, int c) {
        char[] ch = text.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            g.drawImage(Art.font[(int) ch[i] - 32][c], x + i * 8, y, null);
        }
    }

    private static void renderBlackout(Graphics g, int x, int y, int radius) {
        if (radius > 320) return;

        int[] xp = new int[20];
        int[] yp = new int[20];
        for (int i = 0; i < 16; i++) {
            xp[i] = x + (int) (Math.cos((double) i * Math.PI / 15.0) * (double) radius);
            yp[i] = y + (int) (Math.sin((double) i * Math.PI / 15.0) * (double) radius);
        }
        xp[16] = 320;
        yp[16] = y;
        xp[17] = 320;
        yp[17] = 240;
        xp[18] = 0;
        yp[18] = 240;
        xp[19] = 0;
        yp[19] = y;
        g.fillPolygon(xp, yp, xp.length);

        for (int i = 0; i < 16; i++) {
            xp[i] = x - (int) (Math.cos((double) i * Math.PI / 15.0) * (double) radius);
            yp[i] = y - (int) (Math.sin((double) i * Math.PI / 15.0) * (double) radius);
        }
        xp[16] = 320;
        yp[16] = y;
        xp[17] = 320;
        yp[17] = 0;
        xp[18] = 0;
        yp[18] = 0;
        xp[19] = 0;
        yp[19] = y;

        g.fillPolygon(xp, yp, xp.length);
    }


    @Override
    public void addSprite(Sprite sprite) {
        spritesToAdd.add(sprite);
        sprite.tick();
    }

    @Override
    public void removeSprite(Sprite sprite) {
        spritesToRemove.add(sprite);
    }

    @Override
    public float getX(float alpha) {
        int xCam = (int) (mario.xOld + (mario.x - mario.xOld) * alpha) - 160;


        if (xCam < 0) xCam = 0;


        return (float) (xCam + 160);
    }

    @Override
    public float getY(float alpha) {
        return (float) 0;
    }

    public void bump(int x, int y, boolean canBreakBricks) {
        byte block = level.getBlock(x, y);

        if (((int) Level.TILE_BEHAVIORS[(int) block & 0xff] & Level.BIT_BUMPABLE) > 0) {
            bumpInto(x, y - 1);
            level.setBlock(x, y, (byte) 4);
            level.setBlockData(x, y, (byte) 4);

            if (((int) (Level.TILE_BEHAVIORS[(int) block & 0xff]) & Level.BIT_SPECIAL) != 0) {

                if (!Mario.large) {
                    addSprite(new Mushroom(this, x * 16 + 8, y * 16 + 8));
                } else {
                    addSprite(new FireFlower(this, x * 16 + 8, y * 16 + 8));
                }
            } else {
                Mario.getCoin();

                addSprite(new CoinAnim(x, y));
            }
        }

        if (((int) Level.TILE_BEHAVIORS[(int) block & 0xff] & Level.BIT_BREAKABLE) != 0) {
            bumpInto(x, y - 1);
            if (canBreakBricks) {

                level.setBlock(x, y, (byte) 0);
                for (int xx = 0; xx < 2; xx++)
                    for (int yy = 0; yy < 2; yy++)
                        addSprite(new Particle(x * 16 + xx * 8 + 4, y * 16 + yy * 8 + 4, (float) ((xx * 2 - 1) * 4), (float) ((yy * 2 - 1) * 4 - 8)));
            } else {
                level.setBlockData(x, y, (byte) 4);
            }
        }
    }

    public void bumpInto(int x, int y) {
        byte block = level.getBlock(x, y);
        if (((int) (Level.TILE_BEHAVIORS[(int) block & 0xff]) & Level.BIT_PICKUPABLE) != 0) {
            Mario.getCoin();

            level.setBlock(x, y, (byte) 0);
            addSprite(new CoinAnim(x, y + 1));
        }

        for (Sprite sprite : sprites) {
            sprite.bumpCheck(x, y);
        }
    }
}