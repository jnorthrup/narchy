package nars.rl;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;



public abstract class RLAgent implements AgentInterface {

    protected Action action;
    TaskSpec problem = null;

    public RLAgent() {
    }

    public void agent_cleanup() {
    }

    public void agent_end(double arg0) {
    }

    public void agent_freeze() {
    }

    public void agent_init(String taskSpec) {
        problem = new TaskSpec(taskSpec);
        
        
        
        
        
        
        
        
        
        
        
        
        action = new Action(problem.getNumDiscreteActionDims(), problem.getNumContinuousActionDims());
    }

    public String agent_message(String arg0) {
        return null;
    }

    abstract public Action agent_start(Observation o);

    public abstract Action agent_step(double reward, Observation o);
}