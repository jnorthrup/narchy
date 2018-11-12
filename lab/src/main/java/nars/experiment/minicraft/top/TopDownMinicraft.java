package nars.experiment.minicraft.top;

import jcog.Util;
import nars.experiment.minicraft.top.entity.Player;
import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.gfx.Font;
import nars.experiment.minicraft.top.gfx.Screen;
import nars.experiment.minicraft.top.gfx.SpriteSheet;
import nars.experiment.minicraft.top.level.Level;
import nars.experiment.minicraft.top.level.tile.Tile;
import nars.experiment.minicraft.top.screen.LevelTransitionMenu;
import nars.experiment.minicraft.top.screen.Menu;
import nars.experiment.minicraft.top.screen.TitleMenu;
import nars.experiment.minicraft.top.screen.WonMenu;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;

public class TopDownMinicraft extends Canvas implements Runnable {

    public static final String NAME = "Minicraft";
    public static final int HEIGHT = 120;
    public static final int WIDTH = 160;
    private static final int SCALE = 6;

    public final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    private boolean running;
    private Screen screen;
    private Screen lightScreen;
    public final InputHandler input = new InputHandler(this);

    static final int[] colors = new int[256];

    static {
        int pp = 0;
        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    float rr = (r * 255f / 5f);
                    float gg = (g * 255f / 5f);
                    float bb = (b * 255f / 5f);
                    float mid = (rr * 30 + gg * 59 + bb * 11) / 100;

                    int r1 = Math.round(((rr + mid * 1) / 2) * 230f / 255f + 10f);
                    int g1 = Math.round(((gg + mid * 1) / 2) * 230f / 255f + 10f);
                    int b1 = Math.round(((bb + mid * 1) / 2) * 230f / 255f + 10f);
                    colors[pp++] = r1 << 16 | g1 << 8 | b1;

                }
            }
        }
    }

    public TopDownMinicraft() {
        player = new Player(this, input);

    }

    private int tickCount;
    public int gameTime;

    private Level level;
    private Level[] levels = new Level[5];
    private int currentLevel = 3;
    public Player player;

    public Menu menu;
    private int playerDeadTime;
    private int pendingLevelChange;
    private int wonTimer;
    public boolean hasWon;


    public void setMenu(Menu menu) {
        this.menu = menu;
        if (menu != null) menu.init(this, input);
    }

    public void start() {
        running = true;


        try {
            screen = new Screen(WIDTH, HEIGHT, new SpriteSheet(ImageIO.read(TopDownMinicraft.class.getResource("icons.png"))));
            lightScreen = new Screen(WIDTH, HEIGHT, new SpriteSheet(ImageIO.read(TopDownMinicraft.class.getResource("icons.png"))));
        } catch (IOException e) {
            e.printStackTrace();
        }

        resetGame();

    }

    public void stop() {
        running = false;
    }

    public void resetGame() {
        playerDeadTime = 0;
        wonTimer = 0;
        gameTime = 0;
        hasWon = false;

        levels = new Level[5];
        currentLevel =
                3;

        levels[4] = new Level(128, 128, 1, null);
        levels[3] = new Level(128, 128, 0, levels[4]);
        levels[2] = new Level(128, 128, -1, levels[3]);
        levels[1] = new Level(128, 128, -2, levels[2]);
        levels[0] = new Level(128, 128, -3, levels[1]);

        level = levels[currentLevel];
        player = new Player(this, input);
        player.findStartPos(level);

        level.add(player);

        for (int i = 0; i < 5; i++) {
            levels[i].trySpawn(5000);
        }
    }

    int frames;
    int ticks;
    double unprocessed;

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60;
        long lastTimer1 = System.currentTimeMillis();


        while (running) {
            long now = System.nanoTime();
            unprocessed += (now - lastTime) / nsPerTick;
            lastTime = now;
            frame();

            Util.sleepMS(2);

            int fpsIntervalMS = 10000;
            if (System.currentTimeMillis() - lastTimer1 > fpsIntervalMS) {
                lastTimer1 += fpsIntervalMS;
                System.out.println(ticks + " ticks, " + frames + " fps");
                frames = 0;
                ticks = 0;
            }
        }
    }

    public void frame() {
        boolean shouldRender = true;
        while (unprocessed >= 1) {
            ticks++;
            tick();
            unprocessed -= 1;
            shouldRender = true;
        }

        if (shouldRender) {
            frames++;
            render();
        }
    }

    public int frameImmediate() {
        ticks++;
        tick();

        frames++;
        render();


        return player.score;
    }

    public void tick() {
        tickCount++;
        /*if (!hasFocus()) {
            input.releaseAll();
        } else */
        if (!player.removed && !hasWon) gameTime++;

        input.tick();
        if (menu != null) {
            menu.tick();
        } else {
            if (player.removed) {
                playerDeadTime++;
                if (playerDeadTime > 60) {
                    die();

                }
            } else {
                if (pendingLevelChange != 0) {
                    setMenu(new LevelTransitionMenu(pendingLevelChange));
                    pendingLevelChange = 0;
                }
            }
            if (wonTimer > 0) {
                if (--wonTimer == 0) {
                    win();
                }
            }
            if (level!=null)
                level.tick();
            Tile.tickCount++;
        }


    }

    public void win() {
        setMenu(new WonMenu());
    }

    public void die() {

        setMenu(new TitleMenu());
    }

    public void changeLevel(int dir) {
        level.remove(player);
        currentLevel += dir;
        level = levels[currentLevel];
        player.x = (player.x >> 4) * 16 + 8;
        player.y = (player.y >> 4) * 16 + 8;
        level.add(player);

    }

    public void render() {
        BufferStrategy bs = getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(3);
            requestFocus();
            return;
        }

        int xScroll = player.x - screen.w / 2;
        int yScroll = player.y - (screen.h - 8) / 2;
        if (xScroll < 16) xScroll = 16;
        if (yScroll < 16) yScroll = 16;
        if (xScroll > level.w * 16 - screen.w - 16) xScroll = level.w * 16 - screen.w - 16;
        if (yScroll > level.h * 16 - screen.h - 16) yScroll = level.h * 16 - screen.h - 16;
        if (currentLevel > 3) {
            int col = Color.get(20, 20, 121, 121);
            for (int y = 0; y < 14; y++)
                for (int x = 0; x < 24; x++) {
                    screen.render(x * 8 - ((xScroll / 4) & 7), y * 8 - ((yScroll / 4) & 7), 0, col, 0);
                }
        }

        level.renderBackground(screen, xScroll, yScroll);
        level.renderSprites(screen, xScroll, yScroll);

        if (currentLevel < 3) {
            lightScreen.clear(0);
            level.renderLight(lightScreen, xScroll, yScroll);
            screen.overlay(lightScreen, xScroll, yScroll);
        }

        renderGui();


        for (int y = 0; y < screen.h; y++) {
            for (int x = 0; x < screen.w; x++) {
                int cc = screen.pixels[x + y * screen.w];
                if (cc < 255) pixels[x + y * WIDTH] = colors[cc];
            }
        }

        Graphics g = bs.getDrawGraphics();
        g.fillRect(0, 0, getWidth(), getHeight());

        int ww = WIDTH * SCALE;
        int hh = HEIGHT * SCALE;
        int xo = (getWidth() - ww) / 2;
        int yo = (getHeight() - hh) / 2;
        g.drawImage(image, xo, yo, ww, hh, null);
        g.dispose();
        bs.show();
    }

    private void renderGui() {
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 20; x++) {
                screen.render(x * 8, screen.h - 16 + y * 8, 0 + 12 * 32, Color.get(000, 000, 000, 000), 0);
            }
        }

        for (int i = 0; i < 10; i++) {
            if (i < player.health)
                screen.render(i * 8, screen.h - 16, 0 + 12 * 32, Color.get(000, 200, 500, 533), 0);
            else
                screen.render(i * 8, screen.h - 16, 0 + 12 * 32, Color.get(000, 100, 000, 000), 0);

            if (player.staminaRechargeDelay > 0) {
                if (player.staminaRechargeDelay / 4 % 2 == 0)
                    screen.render(i * 8, screen.h - 8, 1 + 12 * 32, Color.get(000, 555, 000, 000), 0);
                else
                    screen.render(i * 8, screen.h - 8, 1 + 12 * 32, Color.get(000, 110, 000, 000), 0);
            } else {
                if (i < player.stamina)
                    screen.render(i * 8, screen.h - 8, 1 + 12 * 32, Color.get(000, 220, 550, 553), 0);
                else
                    screen.render(i * 8, screen.h - 8, 1 + 12 * 32, Color.get(000, 110, 000, 000), 0);
            }
        }
        if (player.activeItem != null) {
            player.activeItem.renderInventory(screen, 10 * 8, screen.h - 16);
        }

        if (menu != null) {
            menu.render(screen);
        }
    }

    private void renderFocusNagger() {
        String msg = "Click to focus!";
        int xx = (WIDTH - msg.length() * 8) / 2;
        int yy = (HEIGHT - 8) / 2;
        int w = msg.length();
        int h = 1;

        screen.render(xx - 8, yy - 8, 0 + 13 * 32, Color.get(-1, 1, 5, 445), 0);
        screen.render(xx + w * 8, yy - 8, 0 + 13 * 32, Color.get(-1, 1, 5, 445), 1);
        screen.render(xx - 8, yy + 8, 0 + 13 * 32, Color.get(-1, 1, 5, 445), 2);
        screen.render(xx + w * 8, yy + 8, 0 + 13 * 32, Color.get(-1, 1, 5, 445), 3);
        for (int x = 0; x < w; x++) {
            screen.render(xx + x * 8, yy - 8, 1 + 13 * 32, Color.get(-1, 1, 5, 445), 0);
            screen.render(xx + x * 8, yy + 8, 1 + 13 * 32, Color.get(-1, 1, 5, 445), 2);
        }
        for (int y = 0; y < h; y++) {
            screen.render(xx - 8, yy + y * 8, 2 + 13 * 32, Color.get(-1, 1, 5, 445), 0);
            screen.render(xx + w * 8, yy + y * 8, 2 + 13 * 32, Color.get(-1, 1, 5, 445), 1);
        }

        if ((tickCount / 20) % 2 == 0) {
            Font.draw(msg, screen, xx, yy, Color.get(5, 333, 333, 333));
        } else {
            Font.draw(msg, screen, xx, yy, Color.get(5, 555, 555, 555));
        }
    }

    public void scheduleLevelChange(int dir) {
        pendingLevelChange = dir;
    }

    public static void main(String[] args) {
        TopDownMinicraft game = new TopDownMinicraft();
        start(game);
    }

    public static void start(TopDownMinicraft game) {
        game.setMinimumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        game.setMaximumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        game.setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));

        JFrame frame = new JFrame(TopDownMinicraft.NAME);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(game, BorderLayout.CENTER);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        game.start();


    }

    public void won() {
        wonTimer = 60 * 3;
        hasWon = true;
    }
}