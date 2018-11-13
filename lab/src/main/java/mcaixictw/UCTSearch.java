package mcaixictw;

/**
 * provides ability to perform monte carlo UCT searches.
 */
public class UCTSearch {

	private UCTNode uctRoot;

	/**
	 * determine the best action by searching ahead using MCTS
	 * 
	 * @param agent
	 * @return
	 */
	public int search(Agent agent, UCTSettings settings) {

		if (uctRoot == null) {
			uctRoot = new UCTNode(false);
		} else {
			uctRoot = uctRoot.getSubtree(agent.getLastAction(), agent.getLastPercept());
		}

		int horizon = settings.getHorizon();
		for (int i = 0; i < settings.getMcSimulations(); i++) {
			
			uctRoot.sample(agent, horizon);
		}
		
		
		int bestAction = uctRoot.bestAction();
		System.out.println("bestAction=" +  bestAction);
		
		if (!settings.isRecycleUCT()) {
			uctRoot = null;
		}
		return bestAction;
	}
}
