package jurls.reinforcementlearning.domains.wander.brain;

import java.io.Serializable;

/**
 * Each instance of this class is responsible for executing
 * one specified action of the Agent. 
 * @author Elser http:
 */
public interface Action extends Serializable {
	/**
	 * Here you implement what the agent should do,
	 * when performing the action. 
	 */
    void execute();
}
