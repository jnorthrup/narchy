package nars.experiment.pacman.entities;

import nars.experiment.pacman.maze.Maze;
import nars.experiment.pacman.maze.Maze.Direction;

abstract class Entity {

    public double x;
    public double y;
    public Direction dir;
    double speed;
    double lead;
    Maze maze;

    public Entity(Maze maze, int x, int y) {

        this.x = (double) x;
        this.y = (double) y;
        speed = 0.1;
        lead = 0.1;
        dir = Direction.right;
        this.maze = maze;

    }

    public void update() {

        move(dir, speed);

        if (dir == Direction.left && x <= -1.0) x = (double) maze.width;
        if (dir == Direction.right && x >= (double) maze.width) x = -1.0;
        if (dir == Direction.up && y <= -1.0) y = (double) maze.height;
        if (dir == Direction.down && y >= (double) maze.height) y = -1.0;

        if (this.dir == Direction.up || this.dir == Direction.down) {

            if ((double) Math.round(x) > x) {

                x += 0.1;

            }

            if ((double) Math.round(x) < x) {

                x -= 0.1;

            }

        }

        if (this.dir == Direction.left || this.dir == Direction.right) {

            if ((double) Math.round(y) > y) {

                y += 0.1;

            }

            if ((double) Math.round(y) < y) {

                y -= 0.1;

            }

        }

    }

    public void move(Direction dir, double dist) {

        if (dir == null) return;

        switch (dir) {

            case up:

                if (y < 1.0 - dist) {

                    y -= dist;
                    break;

                }

                if (Maze.isWall(maze.tiles[(int) Math.floor(x + lead)][(int) Math.ceil(y) - 1], Direction.up) ||
                        Maze.isWall(maze.tiles[(int) Math.ceil(x - lead)][(int) Math.ceil(y) - 1], Direction.up)) {

                    y = Math.ceil(y);

                } else {

                    y -= dist;

                }
                break;

            case down:

                if (y > (double) (maze.height - 2) + dist) {

                    y += dist;
                    break;

                }

                if (Maze.isWall(maze.tiles[(int) Math.floor(x + lead)][(int) Math.floor(y) + 1], Direction.down) ||
                        Maze.isWall(maze.tiles[(int) Math.ceil(x - lead)][(int) Math.floor(y) + 1], Direction.down)) {

                    y = Math.floor(y);

                } else {

                    y += dist;

                }
                break;

            case left:

                if (x < 1.0 - dist) {

                    x -= dist;
                    break;

                }

                if (Maze.isWall(maze.tiles[(int) Math.ceil(x) - 1][(int) Math.floor(y + lead)], Direction.left) ||
                        Maze.isWall(maze.tiles[(int) Math.ceil(x) - 1][(int) Math.ceil(y - lead)], Direction.left)) {

                    x = Math.ceil(x);

                } else {

                    x -= dist;

                }
                break;

            case right:

                if (x > (double) (maze.width - 2) + dist) {

                    x += dist;
                    break;

                }

                if (Maze.isWall(maze.tiles[(int) Math.floor(x) + 1][(int) Math.floor(y + lead)], Direction.right) ||
                        Maze.isWall(maze.tiles[(int) Math.floor(x) + 1][(int) Math.ceil(y - lead)], Direction.right)) {

                    x = Math.floor(x);

                } else {

                    x += dist;

                }
                break;

        }

    }

    public boolean walled(Direction dir) {

        try {

            switch (dir) {

                case left:
                    if (Maze.isWall(maze.tiles[(int) Math.round(x) - 1][(int) Math.floor(y + lead)], dir)) return true;
                    if (Maze.isWall(maze.tiles[(int) Math.round(x) - 1][(int) Math.ceil(y - lead)], dir)) return true;
                    break;

                case right:
                    if (Maze.isWall(maze.tiles[(int) Math.round(x) + 1][(int) Math.floor(y + lead)], dir)) return true;
                    if (Maze.isWall(maze.tiles[(int) Math.round(x) + 1][(int) Math.ceil(y - lead)], dir)) return true;
                    break;

                case up:
                    if (Maze.isWall(maze.tiles[(int) Math.floor(x + lead)][(int) Math.round(y) - 1], dir)) return true;
                    if (Maze.isWall(maze.tiles[(int) Math.ceil(x - lead)][(int) Math.round(y) - 1], dir)) return true;
                    break;

                case down:
                    if (Maze.isWall(maze.tiles[(int) Math.floor(x + lead)][(int) Math.round(y) + 1], dir)) return true;
                    if (Maze.isWall(maze.tiles[(int) Math.ceil(x - lead)][(int) Math.round(y) + 1], dir)) return true;
                    break;


            }

        } catch (ArrayIndexOutOfBoundsException e) {

            return true;

        }

        return false;

    }

}
