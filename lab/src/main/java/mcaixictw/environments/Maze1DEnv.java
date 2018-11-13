package mcaixictw.environments;

import mcaixictw.Environment;
import mcaixictw.McAIXIUtil;


/**
 * 1d-maze. The 1d-maze is a simple problem from the work of Cassandra,
 * Kaelbling, and Littman (1994). The agent begins at a random, non-goal
 * location within a 1 × 4 maze. There is a choice of two actions: left or
 * right. Each action transfers the agent to the adjacent cell if it exists,
 * otherwise it has no effect. If the agent reaches the third cell from the
 * left, it receives a reward of 1. Otherwise it receives a reward of 0. The
 * distinguishing feature of this problem is that the observations are
 * uninformative; every observation is the same regardless of the agent’s actual
 * location.
 */
public class Maze1DEnv extends Environment {
	private final int size;
	private final int rewardPosition;
	private int reward;
	private final int observation;
	private int position;

	public Maze1DEnv(int size) {
		this.size = size;
		reward = 0;
		
		observation = 0;
		rewardPosition = (int) Math.floor(Math.random()*size);
		randomizePosition();
	}

	private void randomizePosition() {
		
		do {
			position = McAIXIUtil.randRange(size);
		} while (position == rewardPosition);
	}

	@Override
	public void performAction(int action) {
		if (action == 0) {
			
			if (position > 0) {
				position--;
			}
		} else {
			
			if (position < size) {
				position++;
			}
		}

		
		
		if (position == rewardPosition) {
			reward = 1;
			randomizePosition();
		} else {
			reward = 0;
		}
	}

	@Override
	public int getObservation() {
		return observation;
	}

	@Override
	public int getReward() {
		return reward;
	}

	@Override
	public int numActions() {
		return 2;
	}

//	@Override
//	public double neutralReward() {
//		return 0;
//	}

	@Override
	public boolean isFinished() {
		return false;
	}

	@Override
	public String environmentName() {
		return "1d-Maze";
	}

	@Override
	public int observationBits() {
		return 1;
	}

	@Override
	public int rewardBits() {
		return 1;
	}
}