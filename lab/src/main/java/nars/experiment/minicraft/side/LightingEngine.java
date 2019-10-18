package nars.experiment.minicraft.side;

import nars.$;

import java.io.Serializable;
import java.util.*;

public class LightingEngine implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Direction {
        RIGHT, UP_RIGHT, UP, UP_LEFT, LEFT, DOWN_LEFT, DOWN, DOWN_RIGHT, SOURCE, WELL, UNKNOWN
    }

    public final Direction[][] lightFlow;

    private final int[][] lightValues;
    private final int width;
    private final int height;
    private final Tile[][] tiles;

    private final boolean isSun;

    public LightingEngine(int width, int height, Tile[][] tiles, boolean isSun) {
        this.width = width;
        this.height = height;
        this.tiles = tiles;
        this.isSun = isSun;
        lightValues = new int[width][height];
        lightFlow = new Direction[width][height];
        init();
    }

    private void init() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                lightValues[x][y] = 0;
                lightFlow[x][y] = Direction.UNKNOWN;
            }
        }
        if (isSun) {
            List<LightingPoint> sources = $.newArrayList(width);
            for (int x = 0; x < width; x++) {
                getSunSources(x, sources);
            }
            spreadLightingDijkstra(sources);
        } else {
            for (int x = 0; x < width; x++) {
                Tile[] tx = tiles[x];
                Direction[] fx = lightFlow[x];
                int[] lx = lightValues[x];

                for (int y = 0; y < height; y++) {
                    if (tx[y].type.lightEmitting > 0) {
                        fx[y] = Direction.SOURCE;
                        lx[y] = tx[y].type.lightEmitting;
                    }

                }
            }
        }

    }

    public int getLightValue(int x, int y) {
        return lightValues[x][y];
    }

    public void removedTile(int x, int y) {
        if (!isSun && lightFlow[x][y] == Direction.SOURCE) {
            lightFlow[x][y] = Direction.UNKNOWN;
            resetLighting(x, y);
            return;
        }
        lightFlow[x][y] = Direction.UNKNOWN;
        if (isSun) {
            spreadLightingDijkstra(getSunSources(x, $.newArrayList()));
        }
        spreadLightingDijkstra(new LightingPoint(x, y, Direction.UNKNOWN, lightValues[x][y])
                .getNeighbors(true, width, height));
    }

    public void addedTile(int x, int y) {
        lightFlow[x][y] = Direction.UNKNOWN;
        if (isSun) {

            boolean sun = true;
            for (int i = 0; i < height; i++) {
                if (tiles[x][i].type.lightBlocking != 0) {
                    sun = false;
                    break;
                }
                if (sun) {
                    lightFlow[x][i] = Direction.SOURCE;
                } else {
                    lightFlow[x][i] = Direction.UNKNOWN;
                }
            }
        } else if (tiles[x][y].type.lightEmitting > 0) {
            lightValues[x][y] = tiles[x][y].type.lightEmitting;
            lightFlow[x][y] = Direction.SOURCE;
        }
        resetLighting(x, y);
    }

    public List<LightingPoint> getSunSources(int column, List<LightingPoint> sources) {

        for (int y = 0; y < height - 1; y++) {
            if (tiles[column][y].type.lightBlocking != 0) {
                break;
            }
            sources.add(new LightingPoint(column, y, Direction.SOURCE, Constants.LIGHT_VALUE_SUN));
        }
        return sources;
    }

    public void resetLighting(int x, int y) {
        int left = Math.max(x - Constants.LIGHT_VALUE_SUN, 0);
        int right = Math.min(x + Constants.LIGHT_VALUE_SUN, width - 1);
        int top = Math.max(y - Constants.LIGHT_VALUE_SUN, 0);
        List<LightingPoint> sources = new LinkedList<>();


        boolean bufferLeft = (left > 0);
        boolean bufferRight = (right < width - 1);
        boolean bufferTop = (top > 0);
        if (bufferTop) {
            if (bufferLeft) {
                sources.add(getLightingPoint(left - 1, top - 1));
                zeroLightValue(left - 1, top - 1);
            }
            if (bufferRight) {
                sources.add(getLightingPoint(right + 1, top - 1));
                zeroLightValue(right + 1, top - 1);
            }
            for (int i = left; i <= right; i++) {
                sources.add(getLightingPoint(i, top - 1));
                zeroLightValue(i, top - 1);
            }
        }
        int bottom = Math.min(y + Constants.LIGHT_VALUE_SUN, height - 1);
        boolean bufferBottom = (bottom < height - 1);
        if (bufferBottom) {
            if (bufferLeft) {
                sources.add(getLightingPoint(left - 1, bottom + 1));
                zeroLightValue(left - 1, bottom + 1);
            }
            if (bufferRight) {
                sources.add(getLightingPoint(right + 1, bottom + 1));
                zeroLightValue(right + 1, bottom + 1);
            }
            for (int i = left; i <= right; i++) {
                sources.add(getLightingPoint(i, bottom + 1));
                zeroLightValue(i, bottom + 1);
            }
        }
        if (bufferLeft) {
            for (int i = top; i <= bottom; i++) {
                sources.add(getLightingPoint(left - 1, i));
                zeroLightValue(left - 1, i);
            }
        }
        if (bufferRight) {
            for (int i = top; i <= bottom; i++) {
                sources.add(getLightingPoint(right + 1, i));
                zeroLightValue(right + 1, i);
            }
        }
        for (int i = left; i <= right; i++) {
            for (int j = top; j <= bottom; j++) {
                if (lightFlow[i][j] == Direction.SOURCE) {
                    sources.add(getLightingPoint(i, j));
                }
                lightValues[i][j] = 0;
            }
        }
        spreadLightingDijkstra(sources);
    }

    private void zeroLightValue(int x, int y) {
        lightValues[x][y] = 0;
    }

    private LightingPoint getLightingPoint(int x, int y) {
        return new LightingPoint(x, y, lightFlow[x][y], lightValues[x][y]);
    }

    public class LightingPoint {

        public final int x;
        public final int y;
        public final int lightValue;
        public final Direction flow;


        public LightingPoint(int x, int y, Direction flow, int lightValue) {
            this.x = x;
            this.y = y;
            this.flow = flow;
            this.lightValue = lightValue;
        }

        @Override
        public boolean equals(Object o) {
            LightingPoint other = (LightingPoint) o;
            return other.x == this.x && other.y == this.y;
        }

        public List<LightingPoint> getNeighbors(boolean sun, int width, int height) {
            List<LightingPoint> neighbors = new LinkedList<>();
            if (tiles[x][y].type.lightBlocking == Constants.LIGHT_VALUE_OPAQUE) {
                return neighbors;
            }
            int newValue = lightValue - 1 - tiles[x][y].type.lightBlocking;
            neighbors = getExactNeighbors(width, height, newValue);

            return neighbors;
        }

        public List<LightingPoint> getExactNeighbors(int width, int height, int lightingValue) {
            LinkedList<LightingPoint> neighbors = new LinkedList<>();

            boolean bufferRight = (x < width - 1);
            boolean bufferUp = (y > 0);
            boolean bufferDown = (y < height - 1);

            if (bufferRight) {
                neighbors.add(new LightingPoint(x + 1, y, Direction.RIGHT, lightingValue));
                if (bufferUp) {
                    neighbors
                            .add(new LightingPoint(x + 1, y - 1, Direction.UP_RIGHT, lightingValue));
                }
                if (bufferDown) {
                    neighbors.add(new LightingPoint(x + 1, y + 1, Direction.DOWN_RIGHT,
                            lightingValue));
                }
            }
            boolean bufferLeft = (x > 0);
            if (bufferLeft) {
                neighbors.add(new LightingPoint(x - 1, y, Direction.LEFT, lightingValue));
                if (bufferUp) {
                    neighbors
                            .add(new LightingPoint(x - 1, y - 1, Direction.UP_LEFT, lightingValue));
                }
                if (bufferDown) {
                    neighbors
                            .add(new LightingPoint(x - 1, y + 1, Direction.UP_LEFT, lightingValue));
                }
            }
            if (bufferDown) {
                neighbors.add(new LightingPoint(x, y + 1, Direction.DOWN, lightingValue));
            }
            if (bufferUp) {
                neighbors.add(new LightingPoint(x, y - 1, Direction.UP, lightingValue));
            }
            return neighbors;
        }

        @Override
        public int hashCode() {
            return x * 13 + y * 17;

        }
    }

    public static class LightValueComparator implements Comparator<LightingPoint> {
        @Override
        public int compare(LightingPoint arg0, LightingPoint arg1) {
            return Integer.compare(arg1.lightValue, arg0.lightValue);
        }
    }

    private void spreadLightingDijkstra(Collection<LightingPoint> sources) {
        if (sources.isEmpty())
            return;
        Set<LightingPoint> out = new HashSet<>(sources.size());
        PriorityQueue<LightingPoint> in = new PriorityQueue<>(sources.size(),
                new LightValueComparator());


        out.addAll(sources);

        in.addAll(sources);
        while (!in.isEmpty()) {
            LightingPoint current = in.poll();
            out.add(current);

            if (current.lightValue <= lightValues[current.x][current.y] || current.lightValue < 0) {
                continue;
            }
            lightValues[current.x][current.y] = current.lightValue;
            lightFlow[current.x][current.y] = current.flow;
            if (lightFlow[current.x][current.y] == Direction.SOURCE
                    && current.flow != Direction.SOURCE) {
                System.out.println("There's a bug in the source map!");
            }
            List<LightingPoint> neighbors = current.getNeighbors(isSun, width, height);
            for (LightingPoint next : neighbors) {
                if (out.contains(next)) {
                    continue;
                }
                in.add(next);
            }
        }
    }

    public static Direction oppositeDirection(Direction direction) {
        switch (direction) {
            case RIGHT:
                return Direction.LEFT;
            case LEFT:
                return Direction.RIGHT;
            case UP:
                return Direction.DOWN;
            case DOWN:
                return Direction.UP;
            case UP_RIGHT:
                return Direction.DOWN_LEFT;
            case UP_LEFT:
                return Direction.DOWN_RIGHT;
            case DOWN_RIGHT:
                return Direction.UP_LEFT;
            case DOWN_LEFT:
                return Direction.UP_RIGHT;
            default:
                return Direction.UNKNOWN;
        }
    }

    public static Int2 followDirection(int x, int y, Direction direction) {
        switch (direction) {
            case RIGHT:
                return new Int2(x + 1, y);
            case LEFT:
                return new Int2(x - 1, y);
            case UP:
                return new Int2(x, y - 1);
            case DOWN:
                return new Int2(x, y + 1);
            case UP_RIGHT:
                return new Int2(x + 1, y - 1);
            case UP_LEFT:
                return new Int2(x - 1, y - 1);
            case DOWN_RIGHT:
                return new Int2(x + 1, y + 1);
            case DOWN_LEFT:
                return new Int2(x - 1, y + 1);
            default:
                return null;
        }
    }
}
