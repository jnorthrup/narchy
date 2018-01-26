package astar;

import astar.impl.ClosedSet;
import astar.impl.IClosedSet;
import astar.impl.IOpenSet;
import astar.impl.OpenSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Uses the A* Algorithm to find the shortest path from
 * an initial to a goal node.
 */
public class AStarGoalFind<F extends Solution> {



    // Amount of debug output 0,1,2
//    private int verbose;
    public Solution bestNodeAfterSearch;
    // The maximum number of completed nodes. After that number the algorithm returns null.
    // If negative, the search will run until the goal node is found.
    private int maxSteps = -1;
    //number of search steps the AStar will perform before null is returned
    private int iteration;

    /**
     * the shortest Path from a start node to an end node according to
     * the A* heuristics (h must not overestimate). initialNode and last found node included.
     */
    public final List<Solution> plan;

    public AStarGoalFind(Problem<F> problem, F initialNode, F goalNode) {

        F endNode = this.search(problem, initialNode, goalNode);

        this.plan = endNode!=null ? AStarGoalFind.path(endNode) : null;

    }

    /**
     * returns path from the earliest ancestor to the node in the argument
     * if the parents are set via AStar search, it will return the path found.
     * This is the shortest shortest path, if the heuristic h does not overestimate
     * the true remaining costs
     *
     * @param node node from which the parents are to be found. Parents of the node should
     *             have been properly set in preprocessing (f.e. AStar.search)
     * @return path to the node in the argument
     */
    public static <F extends Solution> List<Solution> path(F node) {
        List<Solution> path = new ArrayList<>();
        path.add(node);
        Solution currentNode = node;
        while (currentNode.parent() != null) {
            Solution parent = currentNode.parent();
            path.add(0, parent);
            currentNode = parent;
        }
        return path;
    }



    /**
     *
     * @param problem
     * @param initialNode start of the search
     * @param goalNode    end of the search
     * @return goal node from which you can reconstruct the path
     */
    F search(Problem<F> problem,  F initialNode, F goalNode) {

        final Comparator<F> SEARCH_COMPARATOR = Comparator.comparingDouble((x)->
                x.g() + problem.cost(x, goalNode));

        IOpenSet<F> openSet = new OpenSet(SEARCH_COMPARATOR);
        openSet.add(initialNode);

        IClosedSet<F> closedSet = new ClosedSet(SEARCH_COMPARATOR);

        this.iteration = 0;

        while (openSet.size() > 0 && (maxSteps < 0 || this.iteration < maxSteps)) {
            //get element with the least sum of costs from the initial node
            //and heuristic costs to the goal
            F currentNode = openSet.poll();

//            //debug output according to verbose
//            System.out.println((verbose > 1 ? "Open set: " + openSet + "\n" : "")
//                    + (verbose > 0 ? "Current node: " + currentNode + "\n" : "")
//                    + (verbose > 1 ? "Closed set: " + closedSet : ""));

            if (goalNode.goalOf(currentNode)) {
                //we know the shortest path to the goal node, done
                this.bestNodeAfterSearch = currentNode;
                return currentNode;
            }
            //get successor nodes
            Iterable<F> successorNodes = problem.next(currentNode);
            for (F successorNode : successorNodes) {
                boolean inOpenSet;
                if (closedSet.contains(successorNode))
                    continue;
                /* Special rule for nodes that are generated within other nodes:
                 * We need to ensure that we use the node and
                 * its g value from the openSet if its already discovered
                 */
                F discSuccessorNode = openSet.getNode(successorNode);
                if (discSuccessorNode != null) {
                    successorNode = discSuccessorNode;
                    inOpenSet = true;
                } else {
                    inOpenSet = false;
                }
                //compute tentativeG
                double tentativeG = currentNode.g() + problem.cost(currentNode,successorNode);
                //node was already discovered and this path is worse than the last one
                if (inOpenSet && tentativeG >= successorNode.g())
                    continue;
                successorNode.setParent(currentNode);
                if (inOpenSet) {
                    // if successorNode is already in data structure it has to be inserted again to
                    // regain the order
                    openSet.remove(successorNode);
                    successorNode.setG(tentativeG);
                    openSet.add(successorNode);
                } else {
                    successorNode.setG(tentativeG);
                    openSet.add(successorNode);
                }
            }
            closedSet.add(currentNode);
            this.iteration += 1;
        }

        this.bestNodeAfterSearch = closedSet.min();
        return null;
    }

    public int numSearchSteps() {
        return this.iteration;
    }

    public Solution bestNodeAfterSearch() {
        return this.bestNodeAfterSearch;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }


}
