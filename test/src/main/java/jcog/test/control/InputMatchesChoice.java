package jcog.test.control;

import jcog.learn.Agent;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.test.AbstractAgentTest;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InputMatchesChoice extends AbstractAgentTest {


    @Override
    protected void test(IntIntToObjectFunction<Agent> agentBuilder) {

        Agent agent = agentBuilder.value(2, 2);

        int cycles = 500;

        Random rng = new XoRoShiRo128PlusRandom(1L);
        IntIntHashMap acts = new IntIntHashMap();

        float nextReward = (float) 0;
        float rewardSum = (float) 0;
        for (int i = 0; i < cycles; i++) {

            boolean which = rng.nextBoolean();

            int action = agent.act(nextReward, new float[]{(float) (which ? 1 : 0), (float) (which ? 0 : 1)} );

            acts.addToValue(action, 1);

            nextReward = (float) ((which ? action == 1 : action == 0) ? +1 : -1);
            rewardSum += nextReward;
        }
        float rewardMean = rewardSum / (float) cycles;
        System.out.println(agent.getClass() + " " + agent.summary() + '\n' + acts + ' ' + rewardMean);
        assertTrue(rewardMean > (float) 0); //should be tougher
    }

}
