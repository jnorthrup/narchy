package nars.experiment.pacman;

import nars.experiment.pacman.entities.Ghost;
import nars.experiment.pacman.entities.Player;
import nars.experiment.pacman.maze.Maze;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.concurrent.CopyOnWriteArrayList;

public class PacmanGame {

    public static final double GHOST_SPEED_SCARED = 0.3;
    public static final float GHOST_SPEED = 0.2f;
    public static final int UPDATES = -25;
    public static int periodMS = 50;
    private final float playerSpeed = GHOST_SPEED*2;

    public void resetGhosts() {

        ghosts = new Ghost[]{
                new Ghost(maze, maze.playerStart().x, maze.playerStart().y - 3, Color.red)


        };

    }

    static final boolean running = true;
    public final PacComponent view;
    Maze maze;
    boolean doubled;
    Player player;
    public boolean[] keys;
    boolean started = true;
    Ghost[] ghosts;
    int updates;
    public String text;
    public int score;
    private int previousDotCount;
    int ghostEatCount;
    CopyOnWriteArrayList<PacComponent.SplashModel> splashes;
    int fruitTime;

    public PacmanGame() {

        updates = UPDATES;
        maze = Maze.create(13, 13);
        player = new Player(maze, maze.playerStart().x, maze.playerStart().y, playerSpeed);
        keys = new boolean[4];
        splashes = new CopyOnWriteArrayList<>();
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Pacman");
        frame.setVisible(true);
        frame.setResizable(false);

        frame.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {

                processRelease(e);

            }

            @Override
            public void keyPressed(KeyEvent e) {
                processPress(e);
            }
        });

        view = new PacComponent(this);
        frame.add(view);
        frame.pack();

        restart();

    }

    public void restart() {
        resetGhosts();
        text = "";
        score = 0;
        previousDotCount = maze.dotCount;
    }


    public void update() {
        view.repaint();

        if (started) {

            if (updates == -99)
                text = "";
            if (updates == -50)
                text = "Ready?";
            if (updates == -25)
                text = "";

            updates++;

            if (updates < 0)
                return;

            ghosts[0].target(new Point((int) player.x, (int) player.y));

            if (ghosts.length > 1) {
                switch (player.dir) {

                    case up:
                        ghosts[1].target(new Point((int) player.x, (int) player.y - 4));
                        break;

                    case down:
                        ghosts[1].target(new Point((int) player.x, (int) player.y + 4));
                        break;

                    case left:
                        ghosts[1].target(new Point((int) player.x - 4, (int) player.y));
                        break;

                    case right:
                        ghosts[1].target(new Point((int) player.x + 4, (int) player.y));
                        break;

                }

                ghosts[2].target(new Point((int) (3 * player.x - 2 * ghosts[0].x), (int) (3 * player.x - 2 * ghosts[0].x)));
                ghosts[3].random = true;
            }

            if (player.power == 0)
                ghostEatCount = 0;

            for (Ghost g : ghosts) {

                if (updates >= 3500 && updates % 3500 == 0) {

                    g.dir = g.dir.opposite();
                    g.relaxed = true;

                }

                if (updates >= 3500 && updates % 3500 == 500) {

                    g.dir = g.dir.opposite();
                    g.relaxed = false;

                }


                if (Math.abs(g.x - player.x) + Math.abs(g.y - player.y) < 0.5) {

                    if (g.scared) {

                        ghostEatCount++;
                        score += 4;
                        splashes.add(new PacComponent.SplashModel("" + 100 * Math.pow(2, ghostEatCount), g.x, g.y, Color.white));
                        g.reset();

                    } else {

                        if (player.deathAnimation()) {

                            splashes.add(new PacComponent.SplashModel("-100", player.x, player.y, Color.red));
                            score -= 10;

                            maze.fruit = Maze.Fruit.none;
                            fruitTime = 0;

                            resetGhosts();
                            updates = UPDATES;
                            if (player.die()) {
                                lose();
                            }

                        } else {
                            return;
                        }

                    }


                }

                if (g.scared)
                    g.scared = player.power > 0;
                else
                    g.scared = player.power > Player.MAX_POWER - 5;

                if (g.scared) g.target(new Point((int) player.x, (int) player.y));

                g.update();

            }

            if (maze.fruit != Maze.Fruit.none) {

                if (Math.abs(player.x - maze.playerStart().x) < 1 && Math.abs(player.y - maze.playerStart().y) < 1) {

                    switch (maze.fruit) {

                        case red:
                            score += 5;
                            splashes.add(new PacComponent.SplashModel("500", player.x, player.y, Color.white));
                            break;

                        case yellow:
                            score += 10;
                            splashes.add(new PacComponent.SplashModel("1000", player.x, player.y, Color.white));
                            break;

                        case blue:
                            score += 20;
                            splashes.add(new PacComponent.SplashModel("5000", player.x, player.y, Color.cyan));
                            break;

                        default:
                            break;

                    }

                    fruitTime = 0;
                    maze.fruit = Maze.Fruit.none;

                } else if (fruitTime > (1500 + Math.random() * 30000)) {

                    maze.fruit = Maze.Fruit.none;
                    fruitTime = 0;

                }

            } else {

                if (fruitTime > 500) {

                    if (Math.random() < 0.0005) {

                        maze.fruit = Maze.Fruit.red;
                        fruitTime = 0;

                    }

                    if (Math.random() < 0.0003) {

                        maze.fruit = Maze.Fruit.yellow;
                        fruitTime = 0;

                    }

                    if (Math.random() < 0.0001) {

                        maze.fruit = Maze.Fruit.blue;
                        fruitTime = 0;

                    }

                }

            }
            fruitTime++;

            if (maze.dotCount != previousDotCount) {

                if (maze.dotCount == 0) {

                    win();

                }

                previousDotCount = maze.dotCount;
                score += 1;

            }

            if (keys[0]) player.turn(Maze.Direction.left);
            if (keys[1]) player.turn(Maze.Direction.right);
            if (keys[2]) player.turn(Maze.Direction.up);
            if (keys[3]) player.turn(Maze.Direction.down);
            player.update();

            if (updates == 100) ghosts[0].free = true;
            if (ghosts.length > 1) {
                if (updates == 500) ghosts[1].free = true;
                if (updates == 1000) ghosts[2].free = true;
                if (updates == 1500) ghosts[3].free = true;
            }

        } else {

            text = "Press Space to Start";

        }

    }

    public void processPress(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_SPACE && !started) started = true;

        if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) keys[0] = true;
        if (e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) keys[1] = true;
        if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) keys[2] = true;
        if (e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN) keys[3] = true;

    }

    public void processRelease(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) keys[0] = false;
        if (e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) keys[1] = false;
        if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) keys[2] = false;
        if (e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN) keys[3] = false;

    }

    public static void main(String[] args) {

        PacmanGame game = new PacmanGame();


    }

    public void win() {

        text = "WIN";
        restart(); //TODO check this works

        //System.out.println("You Win!");



    }

    public void lose() {
        text = "LOSE";
        //System.out.println("You Lose!");


    }

}
