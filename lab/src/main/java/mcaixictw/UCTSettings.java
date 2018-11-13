package mcaixictw;

import java.io.File;

public class UCTSettings extends Settings {

	public UCTSettings() {
		super();
	}

	public UCTSettings(String path) {
		super(path);
	}

	public String toString() {
		String result = "";
		result += "horizon: " + horizon + '\n';
		result += "recycleUCT: " + recycleUCT + '\n';
		result += "mcSimulations: " + mcSimulations + '\n';
		return result;
	}

	@Override
	public void parseSettings(File file) {
		horizon = parseInt("horizon", horizon, file);
		recycleUCT = parseBoolean("recycle-uct", recycleUCT, file);
		
		mcSimulations = parseInt("mc-simulations", mcSimulations, file);
	}

	@Override
    public void loadDefaultSettings() {
		horizon = 1;
		mcSimulations = 100;
		recycleUCT = true;
		
	}

	@Override
    public UCTSettings clone() {
		UCTSettings s = new UCTSettings();
		s.setHorizon(horizon);
		s.setMcSimulations(mcSimulations);
		s.setRecycleUCT(recycleUCT);
		
		return s;
	}

	/*
	 * depth of the UCT
	 */
	private int horizon;

	/*
	 * number of sample runs
	 */
	private int mcSimulations;

	/*
	 * recycle the UCT from previous search
	 */
	private boolean recycleUCT;


	public int getHorizon() {
		return horizon;
	}

	public void setHorizon(int horizon) {
		this.horizon = horizon;
	}

	public int getMcSimulations() {
		return mcSimulations;
	}

	public void setMcSimulations(int mcSimulations) {
		this.mcSimulations = mcSimulations;
	}

	public boolean isRecycleUCT() {
		return recycleUCT;
	}

	public void setRecycleUCT(boolean recycleUCT) {
		this.recycleUCT = recycleUCT;
	}

//	public static UCTSettings KUHN_POKER_TRAINING() {
//		UCTSettings s = new UCTSettings();
//		s.setHorizon(16);
//		s.setMcSimulations(100);
//		s.setRecycleUCT(true);
//
//		return s;
//	}

}
