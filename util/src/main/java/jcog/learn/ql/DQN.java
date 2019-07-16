//package jcog.learn.ql;
//
///**
// * Created by me on 5/3/16.
// */
//public class DQN extends ReinforceJSAgent {
//
//    @Deprecated private final float alpha = 0.01f;
//    @Deprecated private final float epsilon = 0.1f;
//
//    public DQN(int inputs, int actions) {
//        super(inputs, actions);
//    }
//
//    @Override
//    String getAgentInitCode(int inputs, int actions) {
//        int hiddens = 2 * inputs * actions;
//        String spec = "{ alpha: " + alpha + ", epsilon: " + epsilon + ", num_hidden_units: " + hiddens + " }";
//        return "var agent = new RL.DQNAgent(env, " + spec + "); ";
//    }
//
//
//
//}
