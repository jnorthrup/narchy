package nars.experiment.pacman;

import nars.experiment.pacman.entities.Ghost;
import nars.experiment.pacman.maze.Maze;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

import static java.awt.RenderingHints.*;

public class PacComponent extends JComponent {

    /**
     *
     */
    PacmanGame game;
    int size;
    java.util.List<Splash> splashText;

    static final Font _font = new Font("Arial", Font.BOLD, 1);

    public PacComponent(PacmanGame g) {

        splashText = new ArrayList<>();

        this.game = g;


        this.setPreferredSize(new Dimension(350, 400));
        size = Math.min(Math.round((getWidth()) / (game.maze.width + 3)), Math.round((getHeight()) / (game.maze.height + 5)));

    }

    @Override
    public synchronized void paintComponent(Graphics g) {

        int mWidth = game.maze.width;
        int mHeight = game.maze.height;
        size = Math.min(Math.round((getWidth()) / (mWidth + 0f)),
                Math.round((getHeight()) / (mHeight + 0f)));
        Point offset =
                new Point(0, 0);


        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        g2d.setColor(Color.black);
        g2d.fill(g2d.getClip());

        Shape clip = g2d.getClip();

        g2d.setClip(new Rectangle(offset.x, offset.y, mWidth * size, mHeight * size));

        g2d.setColor(Color.black.darker().darker());
        g2d.fill(g2d.getClip());

        for (int x = 0; x < mWidth; x++) {
            for (int y = 0; y < mHeight; y++) {

                Rectangle tile = getTileBounds(x, y, offset);

                if (game.maze.tiles[x][y] == 3) {

                    g2d.setColor(Color.lightGray);
                    g2d.fillRect(tile.x, tile.y + tile.height * 1 / 3, tile.width, tile.height * 1 / 3);

                } else if (Maze.isWall(game.maze.tiles[x][y])) {

//                    g2d.setColor(Color.darkGray);
//                    g2d.fill(tile);
                    g2d.setColor(Color.darkGray);
                    g2d.fillRect(tile.x , tile.y , tile.width, tile.height);

                }

                if ((x * y) % 2 == 1 && game.maze.dots[x / 2][y / 2]) {


                    if (game.maze.isBigFood(x, y)) {
                        g2d.setColor(Color.green.brighter());
                    } else{
                        g2d.setColor(Color.green);
                    }
                    g2d.fillOval(tile.x, tile.y, tile.width, tile.height);

                }

            }
        }

        if (game.maze.fruit != Maze.Fruit.none) {

//            switch (game.maze.fruit) {
//
//                case red:
                    g2d.setColor(Color.GREEN);
//                    break;

//                case blue:
//                    g2d.setColor(Color.CYAN.darker().darker());
//                    break;
//
//                case yellow:
//                    g2d.setColor(Color.orange);
//                    break;
//
//                default:
//                    break;

//            }

            Rectangle fruit = getTileBounds(game.maze.playerStart().x, game.maze.playerStart().y, offset);
            g2d.fillOval(fruit.x, fruit.y, fruit.width, fruit.height);

        }

        g2d.setStroke(new BasicStroke(0));

        for (Ghost ghost : game.ghosts) {

            Polygon ghostShape = new Polygon();

            for (double[] coords : Ghost.ghostShape) {

                ghostShape.addPoint((int) (coords[0] * size) + offset.x + (int) (ghost.x * size),
                        (int) (coords[1] * size) + offset.y + (int) (ghost.y * size));

            }

            if (ghost.scared && (game.player.power > 99 || game.player.power % 25 < 12))
                g2d.setColor(Color.green.brighter());
            else if (ghost.scared)
                g2d.setColor(Color.green.brighter());
            else
                g2d.setColor(ghost.color);
            g2d.fill(ghostShape);

        }

        Rectangle pac = getTileBounds(game.player.x, game.player.y, offset);
        g2d.setColor(Color.blue.brighter());

        if (game.player.mouthAngle < 180)
            switch (game.player.dir) {

                case up:
                    g2d.fillArc(pac.x, pac.y, pac.width, pac.height, 90 + game.player.mouthAngle, 360 - 2 * game.player.mouthAngle);
                    g2d.setColor(Color.black);
                    g2d.fillOval(pac.x + pac.width * 4 / 9, pac.y + pac.height * 5 / 9, pac.width * 2 / 9, pac.height * 2 / 9);
                    break;

                case right:
                    g2d.fillArc(pac.x, pac.y, pac.width, pac.height, game.player.mouthAngle, 360 - 2 * game.player.mouthAngle);
                    g2d.setColor(Color.black);
                    g2d.fillOval(pac.x + pac.width * 3 / 9, pac.y + pac.height * 4 / 9, pac.width * 2 / 9, pac.height * 2 / 9);
                    break;

                case down:
                    g2d.fillArc(pac.x, pac.y, pac.width, pac.height, 270 + game.player.mouthAngle, 360 - 2 * game.player.mouthAngle);
                    g2d.setColor(Color.black);
                    g2d.fillOval(pac.x + pac.width * 4 / 9, pac.y + pac.height * 3 / 9, pac.width * 2 / 9, pac.height * 2 / 9);
                    break;

                case left:
                    g2d.fillArc(pac.x, pac.y, pac.width, pac.height, 180 + game.player.mouthAngle, 360 - 2 * game.player.mouthAngle);
                    g2d.setColor(Color.black);
                    g2d.fillOval(pac.x + pac.width * 5 / 9, pac.y + pac.height * 4 / 9, pac.width * 2 / 9, pac.height * 2 / 9);
                    break;

                default:
                    g2d.fillOval(pac.x, pac.y, pac.width, pac.height);
                    g2d.setColor(Color.black);
                    g2d.fillOval(pac.x + pac.width * 4 / 9, pac.y + pac.height * 4 / 9, pac.width * 2 / 9, pac.height * 2 / 9);
                    break;

            }

        g2d.setColor(Color.white);
        g2d.setFont(_font.deriveFont((int) (size * 1.4)));
        g2d.drawString(game.text, getWidth() / 2 - g2d.getFontMetrics().stringWidth(game.text) / 2, getHeight() / 2);

        g2d.setClip(clip);


        g2d.setColor(Color.white);
        g2d.setFont(_font.deriveFont((int) (size * 0.7f)));
        Rectangle r = getTileBounds(0, game.maze.height + 1, offset);


        for (SplashModel s : game.splashes) {
            this.new Splash(s.text, s.x, s.y, s.color);
            game.splashes.remove(s);
        }

        for (int i = 0; i < this.splashText.size(); i++) {

            Splash s = this.splashText.get(i);
            g2d.setColor(new Color(s.color.getRed() / 255f, s.color.getGreen() / 255f, s.color.getBlue() / 255f, s.time / (float) Splash.TIME));
            g2d.setFont(s.font);
            Rectangle bounds = getTileBounds(s.x, s.y, offset);
            g2d.drawString(s.text, bounds.x, (int) (bounds.y + Math.sqrt(s.time)));
            s.update();

            if (s.time <= 0)
                splashText.remove(i);
        }




    }

    Rectangle getTileBounds(double x, double y, Point offset) {

        Rectangle tile = new Rectangle(offset.x + (int) Math.round(x * size), offset.y + (int) Math.round(y * size), size, size);

        return tile;

    }

    public static class SplashModel {

        public static final int TIME = 150;

        String text;
        double x, y;
        int time;
        final Font font;

        Color color;

        public SplashModel(String text, double x, double y, int size, Color color) {

            this.text = text;
            this.time = TIME;
            this.x = x;
            this.y = y;
            this.font = _font.deriveFont(size);
            this.color = color;

        }

        public SplashModel(String text, double x, double y, Color color) {

            this(text, x, y, 15, color);

        }

        public void update() {

            if (time > 0)
                time--;

        }

    }

    public class Splash extends SplashModel {

        public Splash(String text, double x, double y, int size, Color color) {

            super(text, x, y, size, color);
            PacComponent.this.splashText.add(this);

        }

        public Splash(String text, double x, double y, Color color) {

            this(text, x, y, (int) (PacComponent.this.size * .75), color);

        }

    }

}