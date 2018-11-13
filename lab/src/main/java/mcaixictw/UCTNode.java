package mcaixictw;

import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

/**
 * one node of the upper confidence bound applied to trees algorithm.
 */
public class UCTNode {

    public UCTNode(boolean isChanceNode) {
        this.isChanceNode = isChanceNode;
        visits = 0;
        mean = 0;
        children = new IntObjectHashMap();
    }

    private final IntObjectHashMap<UCTNode> children; 
    private final boolean isChanceNode; 
    private double mean; 
    private int visits; 
    private static final double explorationRatio = 1.41;
    

    /**
     * Returns the action with the highest expected reward.
     *
     * @return
     */
    public int bestAction() {
        final int[] bestAction = {0};
        final double[] maxReward = {Double.NEGATIVE_INFINITY};
        //TODO fair
        children.forEachWithIndex((UCTNode curr, int i)->{
            double expectedReward = curr.mean;
            if (expectedReward > maxReward[0]) {
                maxReward[0] = expectedReward;
                bestAction[0] = i;
            }
        });
        return bestAction[0];
    }

    /**
     * determine the next action to play
     *
     * @param agent
     * @param dfr
     * @return
     */
    private int actionSelect(Agent agent, int dfr) {
        assert (agent.numActions() >= children.size());

        final double[] maxValue = {Double.NEGATIVE_INFINITY};
        final int[] selectedAction = {0};

        
        
        if (children.size() < agent.numActions()) {
            IntArrayList unexplored = new IntArrayList();
            for (int a = 0; a < agent.numActions(); a++) {
                if (!children.containsKey(a)) {
                    unexplored.add(a);
                }
            }
            selectedAction[0] = unexplored.get(McAIXIUtil.randRange(unexplored.size()));
        } else {
            
            
            

            children.forEachKeyValue((i, currNode) -> {

                double value = 1.0
                        / (double) (dfr * (agent.maxReward() - agent
                        .minReward()))
                        * currNode.expectation()
                        + explorationRatio
                        * Math.sqrt(Math.log((double) visits)
                        / (double) currNode.visits);

                if (value > maxValue[0]) {
                    maxValue[0] = value;
                    selectedAction[0] = i;
                }
            });
        }


        return selectedAction[0];

    }

    /**
     * the expected reward of this node.
     *
     * @return expected reward
     */
    double expectation() {
        return mean;
    }

    /**
     * perform a sample run through this node and it's m_children, returning the
     * accumulated reward from this sample run.
     *
     * @param agent
     * @param m        remaining horizon
     * @param ctUpdate update CTW
     * @return accumulated reward
     */
    public double sample(Agent agent, int m) {



        double futureTotalReward;

        if (m == 0) {
            
            return agent.reward();
        }

        ModelUndo undo = new ModelUndo(agent);
        if (isChanceNode) {
            int p = agent.genPerceptAndUpdate();

            futureTotalReward = children.getIfAbsentPut(p, () -> new UCTNode(false)).sample(agent, m - 1);
        } else if (visits == 0) {
            futureTotalReward = rollout(agent, m);
        } else {
            int a = actionSelect(agent, m);

            UCTNode next = children.getIfAbsentPut(a, () -> new UCTNode(true));

            agent.modelUpdate(a);
            futureTotalReward = next.sample(agent, m);
        }

        
        double reward = futureTotalReward - undo.reward();

        
        mean = 1.0 / (double) (visits + 1) * (reward + (double) visits * mean);

        visits++;

        
        
        
        

        agent.modelRevert(undo);

        assert (undo.age() == agent.age());
        assert (undo.getHistorySize() == agent.historySize());
        assert (undo.reward() == agent.reward());
        assert (undo.isLastUpdatePercept() == agent.lastUpdatePercept());

        return futureTotalReward;
    }

    /**
     * number of times the search node has been visited
     */
    int visits() {
        return visits;
    }

    /**
     * simulate a path through a hypothetical future for the agent within it's
     * internal model of the world, returning the accumulated reward.
     *
     * @param agent
     * @param rolloutLength
     * @param ctUpdate
     * @return accumulated reward
     */
    private double rollout(Agent agent, int rolloutLength) {
        assert (!isChanceNode);
        assert (rolloutLength > 0);
        for (int i = 0; i < rolloutLength; i++) {
            agent.genRandomActionAndUpdate();
            agent.genPerceptAndUpdate();
        }
        return agent.reward();
    }

    /**
     * returns the subtree rooted at [root,action,percept]. If that subtree does
     * not exist the tree is cleared and a new search tree is returned.
     *
     * @param action
     * @param percept
     * @return
     */
    public UCTNode getSubtree(int action, int percept) {
        assert (!isChanceNode);
//        assert (children.containsKey(action));

        UCTNode p = children.get(action).children.get(percept);
        if (p!=null) {
            return p;
        } else {
            return new UCTNode(false);
        }
        /*
         *
		 * NodeSearch afterAction, afterPercept; boolean found = false;
		 * afterAction = child(action); if(afterAction != NULL) { afterPercept =
		 * afterAction->child(percept); if(afterPercept != NULL) { found = true;
		 * } }
		 * 
		 * if(found) { afterAction->m_children[percept] = 0; delete root; return
		 * afterPercept; } else { delete root; return new NodeSearch(false); }
		 */
    }
}