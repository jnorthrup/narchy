package mcaixictw;

import java.util.ArrayList;
import java.util.List;

import com.gs.collections.api.list.primitive.BooleanList;
import com.gs.collections.api.list.primitive.MutableBooleanList;
import com.gs.collections.impl.list.mutable.primitive.BooleanArrayList;
import mcaixictw.worldmodels.WorldModel;

import static mcaixictw.Util.asInt;

/**
 * Representation of a smart agent.
 * 
 */
public class AIXIModel {

	public AIXIModel(int actions, int obsBits, int rewBits, WorldModel model) {
		this.actions = actions;
		this.obsBits = obsBits;
		this.rewBits = rewBits;
		this.model = model;
		actionBits = Util.bitsNeeded(actions);
		lastUpdatePercept = false;
	}

	private final int actions; // number of actions
	private final int actionBits; // number of bits to represent an action
	private final int obsBits; // number of bits to represent an observation
	private final int rewBits; // number of bits to represent a reward
	private final WorldModel model; // Context Tree representing the agent's beliefs
	private int timeCycle; // How many time cycles the agent has been alive
	private int totalReward; // The total reward received by the agent
	private boolean lastUpdatePercept; // True if the last update was a percept
										// update

	// TODO redesign to avoid violations of the command & query separation principle.
	

	/**
	 * current age of the agent in cycles
	 * 
	 * @return
	 */
	public int age() {
		return timeCycle;
	}

	/**
	 * the total accumulated reward across an agents lifetime
	 * 
	 * @return
	 */
	public int reward() {
		return totalReward;
	}

	/**
	 * the average reward received by the agent at each time step
	 * 
	 * @return
	 */
	public double averageReward() {
		return age() > 0 ? (double) totalReward / (double) age() : 0.0;
	}

	/**
	 * maximum reward in a single time instant
	 */
	public int maxReward() {
		return ((1 << rewBits) - 1);
	}

	/**
	 * minimum reward in a single time instant
	 * 
	 * @return
	 */
	public int minReward() {
		return 0;
	}

	/**
	 * number of distinct actions
	 * 
	 * @return
	 */
	public int numActions() {
		return actions;
	}

	/**
	 * the length of the stored history for an agent
	 * 
	 * @return
	 */
	public int historySize() {
		return model.historySize();
	}

	/**
	 * generate a random action and update the model (in case of an action this
	 * means updating the history but not the tree).
	 * 
	 * @return
	 */
	public BooleanArrayList genRandomActionAndUpdate() {
		int a = Util.randRange(actions);
		BooleanArrayList aa = Util.encode(a, actionBits);
		modelUpdate(aa);
		return aa;
	}

	/**
	 * generate a percept distributed according to our history statistics
	 * 
	 * @return
	 */
	public BooleanArrayList genPercept() {
		return model.genRandomSymbols(obsBits + rewBits);
	}

	/**
	 * generate a percept distributed to our history statistics, and only update
	 * the history but not the context trees.
	 * 
	 * @return
	 */
	public BooleanArrayList genPerceptAndUpdateHistory() {
		assert (!lastUpdatePercept);
		BooleanArrayList r = model.genRandomSymbols(obsBits + rewBits);
		model.update(r);
		int reward = decodeReward(r);
		assert (isRewardOk(reward));
		assert (isObservationOk(r));
		totalReward += reward;
		lastUpdatePercept = true;
		return r;
	}

	/**
	 * generate a percept distributed to our history statistics, and update our
	 * mixture environment model with it. we have to keep in mind that the
	 * percept_t returned here is an observation combined with a reward!
	 * 
	 * @return
	 */
	public BooleanArrayList genPerceptAndUpdate() {
		
		// TODO problem if there was no update
		
		assert (!lastUpdatePercept);
		int numBits = obsBits + rewBits;
		BooleanArrayList list = model.genRandomSymbolsAndUpdate(numBits);
		int reward = decodeReward(list);
		assert (isObservationOk(list));
		assert (isRewardOk(reward));
		totalReward += reward;
		lastUpdatePercept = true;
		return list;
	}

	/**
	 * Update the agent's internal model of the world after receiving a percept
	 * 
	 * @param observation
	 * @param reward
	 */
	public void modelUpdate(int observation, int reward) {
		assert (!lastUpdatePercept);
		model.update(encodePercept(observation, reward));
		// Update other properties
		totalReward += reward;
		lastUpdatePercept = true;
	}
	public void modelUpdate(float[] observation, int bitsPerInput, int reward) {
		assert (!lastUpdatePercept);
		model.update(encodePercept(observation, bitsPerInput, reward));
		// Update other properties
		totalReward += reward;
		lastUpdatePercept = true;
	}

	/**
	 * Update the agent's internal model of the world after performing an action
	 * 
	 * @param action
	 */
	public void modelUpdate(BooleanArrayList action) {
		assert (asInt(action) < actions);
		assert (lastUpdatePercept);

		// Update internal model
		model.updateHistory(action);

		timeCycle++;
		lastUpdatePercept = false;
	}

	/**
	 * revert the history of the agent without touching the CTW model.
	 */
	public void historyRevert(ModelUndo mu) {
		model.revertHistory(mu.getHistorySize());
		assert (model.historySize() == mu.getHistorySize());
		timeCycle = mu.getAge();
		totalReward = mu.getReward();
		lastUpdatePercept = mu.isLastUpdatePercept();
	}

	/**
	 * revert the agent's internal model of the world to that of a previous time
	 * cycle, false on failure.
	 * 
	 * @param mu
	 * @return
	 */
	public boolean modelRevert(ModelUndo mu) {
		// Revert as long we are not in the state defined by 'mu'
		while (mu.getAge() != timeCycle
				|| mu.isLastUpdatePercept() != lastUpdatePercept) {
			// Undo a percept-update
			if (lastUpdatePercept) {
				model.revert(obsBits + rewBits);
				lastUpdatePercept = false;
			} else {
				// Undo a action-update
				model.revertHistory(model.historySize() - actionBits);
				timeCycle--;
				lastUpdatePercept = true;
			}
		}

		// Make sure we correctly reverted the agent.
		assert (mu.getHistorySize() == historySize());
		assert (lastUpdatePercept == mu.isLastUpdatePercept());

		timeCycle = mu.getAge();
		totalReward = mu.getReward();

		return true;
	}

	/**
	 * Just reset the total reward and the age of the agent without resetting
	 * the agent's model of the world.
	 */
	public void resetRemeberHistory() {
		timeCycle = 0;
		totalReward = 0;
	}

	/**
	 * get the agent's probability of receiving a particular percept
	 * 
	 * @param observation
	 * @param reward
	 * @return
	 */
	public double perceptProbability(int observation, int reward) {
		return model.predict(encodePercept(observation, reward));
	}

	/**
	 * reward sanity check
	 */
	private boolean isRewardOk(int reward) {
		return reward >= minReward() && reward <= maxReward();
	}

	/**
	 * observation sanity check
	 * 
	 * @param obs
	 * @return
	 */
	private boolean isObservationOk(BooleanArrayList obs) {
		return obs.size() <= ((1 << obsBits) - 1);
	}



	/**
	 * Encodes a percept (observation, reward) as a list of symbols
	 *
	 * @param observation
	 * @param reward
	 * @return
	 */
	public BooleanArrayList encodePercept(int observation, int reward) {
		BooleanArrayList result = new BooleanArrayList(obsBits + rewBits);
		Util.encode(observation, obsBits, result);
		Util.encode(reward, rewBits, result);
		return result;
	}

	public BooleanArrayList encodePercept(float[] observation, int bitsPerInput, int reward) {
		if (observation.length*bitsPerInput!=obsBits)
			throw new UnsupportedOperationException();

		BooleanArrayList result = new BooleanArrayList(obsBits + rewBits);
		Util.encode(observation, bitsPerInput, result);
		Util.encode(reward, rewBits, result);
		return result;
	}

	/**
	 * Decodes the reward from a list of symbols containing a perception.
	 * 
	 * @param perception
	 * @return
	 */
	public int decodeReward(BooleanArrayList perception) {
		int s = perception.size();
		//BooleanList decodedReward = perception.subList(s - rewBits, s);
		BooleanArrayList decodedReward = new BooleanArrayList(rewBits);
		for (int i = s - rewBits; i < s; i++)
			decodedReward.add(perception.get(i));

		int total = asInt(decodedReward);

		assert (isRewardOk(total));
		return total;
	}

	/**
	 * Decodes the observation from a list of symbols, assuming the perception
	 * is at the beginning of the list.
	 * 
	 * @param symlist
	 * @return
	 */
	public BooleanArrayList decodeObservation(List<Boolean> symlist) {
		return Util.asBitSet(symlist.subList(0, obsBits));
	}

	// Returns the most recent perception
	public BooleanArrayList getLastPercept() {
		int percept_bits = obsBits + rewBits;
		int i, end;
		if (lastUpdatePercept) {
			i = percept_bits - 1;
			end = 0;
		} else {
			i = percept_bits + actionBits - 1;
			end = actionBits;
		}
		List<Boolean> list = new ArrayList<>(end);
		for (; i >= end; i--) {
			list.add(model.nthHistorySymbol(i));
		}
		return Util.asBitSet(list);
	}

	// Returns the most recent action
	public BooleanArrayList getLastAction() {
		int percept_bits = obsBits + rewBits;
		int i, end;
		if (lastUpdatePercept) {
			i = percept_bits + actionBits - 1;
			end = percept_bits;
		} else {
			i = actionBits - 1;
			end = 0;
		}
		List<Boolean> list = new ArrayList<>(end);
		for (; i >= end; i--) {
			list.add(model.nthHistorySymbol(i));
		}
		return Util.asBitSet(list);
	}

	public boolean lastUpdatePercept() {
		return this.lastUpdatePercept;
	}

	public int getActionBits() {
		return actionBits;
	}

	public int getObsBits() {
		return obsBits;
	}

	public int getRewBits() {
		return rewBits;
	}

	public int getPerceptBits() {
		return obsBits + rewBits;
	}

	// Returns a string representation of the agent's model of the world
	public String toString() {
		return model.toString();
	}

	public WorldModel getModel() {
		return model;
	}

}
