package jcog.learn.ql;

/**
 * Deterministic Policy Gradient
 */
public class DPG extends ReinforceJSAgent {

    @Deprecated private final float alpha = 0.03f;

    public DPG(int inputs, int actions) {
        super(inputs, actions);
    }

    @Override
    String getAgentInitCode(int inputs, int actions) {
        int hiddens = 3 * inputs * actions; 
        return "var spec =  { alpha: + " + alpha + ", num_hidden_units: " + hiddens + " }; " +
                "var agent = new RL.DPGAgent(env, spec); ";
    }



}
