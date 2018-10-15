package jcog.learn;

import jcog.learn.ql.*;
import jcog.random.XoRoShiRo128PlusRandom;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentTest {


    @Test
    void testHaiQ() {
        testAgentObviousChoice( new HaiQ(1, 2) );
        testAgentTwoToTwoBoolean( new HaiQ(2, 2) );
    }
    @Test
    void testDQN2() {
        testAgentObviousChoice( new DQN2(1, 2) );
        testAgentTwoToTwoBoolean( new DQN2(2, 2) );
    }

    @Test
    void testHaiQAgent() {
        testAgentObviousChoice( new HaiQae(1, 2) );
        testAgentTwoToTwoBoolean( new HaiQae(2, 2) );

    }

    @Disabled
    @Test
    void testDPGAgent() {
        testAgentObviousChoice( new DPG(1, 2) );
    }


    @Disabled @Test
    void testDQNAgent() {
        testAgentTwoToTwoBoolean( new DQN(2, 2) );
        testAgentObviousChoice( new DQN(1, 2) );
    }

    private static void testAgentObviousChoice(Agent agent) {

        assert(agent.inputs >= 1);
        assert(agent.actions == 2);

        final float minRatio = 2f;

        int cycles = 100;

        float nextReward = 0;
        IntIntHashMap acts = new IntIntHashMap();
        for (int i = 0; i < cycles; i++) {
            int action = agent.act(nextReward, new float[] { (float)Math.random() } );
            
            acts.addToValue(action, 1);
            switch (action) {
                case 0: nextReward = -1.0f; break;
                case 1: nextReward = +1.0f; break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
        System.out.println(agent.getClass() + " " + agent.summary() + "\n" + acts);
        assertTrue(acts.get(1) > acts.get(0));
        assertTrue(acts.get(1)/ minRatio > acts.get(0)); 
    }
    private static void testAgentTwoToTwoBoolean(Agent agent) {

        assert(agent.inputs >= 2);
        assert(agent.actions == 2);

        int cycles = 500;

        float nextReward = 0;
        IntIntHashMap acts = new IntIntHashMap();
        Random rng = new XoRoShiRo128PlusRandom(1);
        float rewardSum = 0;
        for (int i = 0; i < cycles; i++) {

            boolean which = rng.nextBoolean();

            int action = agent.act(nextReward, new float[] { which ? 1 : 0, which ? 0 : 1 } );

            acts.addToValue(action, 1);

            nextReward = (which ? action==1 : action==0) ? +1 : -1;
            rewardSum += nextReward;
        }
        float rewardMean = rewardSum / cycles;
        System.out.println(agent.getClass() + " " + agent.summary() + "\n" + acts + " "+ rewardMean);
        assertTrue(rewardMean > 0);
    }

}