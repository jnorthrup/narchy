package nars.experiment.pacman.maze;

import nars.experiment.pacman.maze.Maze.Direction;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class GenCursor {

    public int x;
    public int y;
    Maze maze;
    public Rectangle area;
    ArrayList<Direction> path;
    Random rand = new Random();
    boolean trigger;

    public boolean complete;
    public boolean complTrig;

    public GenCursor(Maze m, int x, int y) {

        this.maze = m;
        this.x = x;
        this.y = y;

        this.area = new Rectangle(0, 0, maze.width, maze.height);

        path = new ArrayList<>();

    }

    public boolean advance() {

        Direction[] openDirs = maze.getUngeneratedDirections(new Point(this.x, this.y), area);

        if (openDirs.length > 0) {

            trigger = true;

            Direction move = openDirs[rand.nextInt(openDirs.length)];
            maze.tiles[motion(move, 1).x][motion(move, 1).y] = 1;
            this.x = motion(move, 2).x;
            this.y = motion(move, 2).y;
            path.add(move);

        } else {

            if (trigger) {

                Direction[] dirs = getPunchDirections();

                for (int x = 0; x < dirs.length; x++)
                    maze.tiles[motion(dirs[x], 1).x][motion(dirs[x], 1).y] = 1;

                trigger = false;

            }

            if (path.size() > 0) {

                Direction move = path.get(path.size() - 1).opposite();
                this.x = motion(move, 2).x;
                this.y = motion(move, 2).y;

                path.remove(path.size() - 1);

            } else {

                complTrig = !complete;

                complete = true;
                return false;

            }

        }

        return true;

    }

    public Point motion(Direction dir, int dist) {

        switch (dir) {

            case up:
                return new Point(this.x, this.y - dist);

            case down:
                return new Point(this.x, this.y + dist);

            case left:
                return new Point(this.x - dist, this.y);

            case right:
                return new Point(this.x + dist, this.y);

            default:
                return null;

        }

    }

    public Direction[] getPunchDirections() {

        Direction past = null;
        if (path.size() > 2)
            past = path.get(path.size() - 2);

        ArrayList<Direction> openDirs = new ArrayList<>();
        if (past != Direction.right)
            if (maze.isWall(x - 1, y) && x > area.x)
                openDirs.add(Direction.left);
        if (past != Direction.left)
            if (maze.isWall(x + 1, y) && x < area.x + area.width - 1)
                openDirs.add(Direction.right);
        if (past != Direction.down)
            if (maze.isWall(x, y - 1) && y > area.y + 2)
                openDirs.add(Direction.up);
            else if (maze.isWall(x, y - 1) && y > area.y && x > area.x + 1 && x < area.width - 2)
                openDirs.add(Direction.up);
        if (past != Direction.up)
            if (maze.isWall(x, y + 1) && y < area.y + area.height - 2)
                openDirs.add(Direction.down);
            else if (maze.isWall(x, y + 1) && y < area.y + area.height - 1 && x > area.x + 1 && x < area.width - 2)
                openDirs.add(Direction.down);

        if (openDirs.size() > 1) {
            if (path.size() > 1)
                openDirs.remove(path.get(path.size() - 1));
        }

        Direction[] returnedDirs = new Direction[openDirs.size()];
        openDirs.toArray(returnedDirs);
        return returnedDirs;

    }

}