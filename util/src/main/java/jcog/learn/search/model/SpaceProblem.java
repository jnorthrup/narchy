package jcog.learn.search.model;

import jcog.learn.search.Problem;
import jcog.learn.search.Solution;

import java.util.List;

public class SpaceProblem implements Problem<SpaceProblem.SpaceFind> {

    @Override
    public double cost(SpaceFind currentNode, SpaceFind successorNode) {
        return dist(currentNode.x, currentNode.y, successorNode.x, successorNode.y);
    }
    public static double dist(int x, int y, int otherX, int otherY) {
        return Math.sqrt(Math.pow((double) (x - otherX), 2.0) + Math.pow((double) (y - otherY), 2.0));
    }

    @Override
    public Iterable<SpaceFind> next(SpaceFind current) {
        return List.of(
            new SpaceFind(current.x - 1, current.y, current),
            new SpaceFind(current.x + 1, current.y, current),
            new SpaceFind(current.x, current.y + 1, current),
            new SpaceFind(current.x, current.y - 1, current)
        );
    }

    public static SpaceFind at(int x, int y) {
        return new SpaceFind(x, y);
    }

    /**
     * TODO generalize to N-d space with custom distance functions
     */
    public static class SpaceFind implements Solution {
        public final int x;
        public final int y;
        private Solution parent;

        public SpaceFind(int x, int y) {
            this(x, y, null);
        }

        public SpaceFind(int x, int y, SpaceFind parent) {
            this.x = x;
            this.y = y;
            this.parent = parent;
        }

        @Override
        public double g() {
            return (double) 0;
        }

        @Override
        public void setG(double g) {

        }

        public Solution parent() {
            return this.parent;
        }

        public void setParent(Solution parent) {
            this.parent = parent;
        }

        public boolean equals(Object other) {
            if (other instanceof SpaceFind) {
                SpaceFind otherNode = (SpaceFind) other;
                return (this.x == otherNode.x) && (this.y == otherNode.y);
            }
            return false;
        }

        public int hashCode() {
            return (41 * (41 + this.x + this.y));
        }


        public String toString() {
            return x + "," + y;
        }

        public boolean goalOf(Solution other) {
            if (other instanceof SpaceFind) {
                SpaceFind otherNode = (SpaceFind) other;
                return (this.x == otherNode.x) && (this.y == otherNode.y);
            }
            return false;
        }

        public static final Problem<SpaceFind> PROBLEM = new Problem<>() {

            @Override
            public double cost(SpaceFind currentNode, SpaceFind successorNode) {
                return Math.sqrt(Math.pow((double) (currentNode.x - successorNode.x), 2.0) + Math.pow((double) (currentNode.y - successorNode.y), 2.0));
            }

            @Override
            public Iterable<SpaceFind> next(SpaceFind current) {
                return List.of(
                        new SpaceFind(current.x - 1, current.y, current),
                        new SpaceFind(current.x + 1, current.y, current),
                        new SpaceFind(current.x, current.y + 1, current),
                        new SpaceFind(current.x, current.y - 1, current)
                );
            }
        };
    }
}
