package jcog.test.control;

import jcog.learn.Agent;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.test.AbstractAgentTest;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OneObviousChoice extends AbstractAgentTest {

    @Override
    protected void test(IntIntToObjectFunction<Agent> agentBuilder) {

        Agent agent = agentBuilder.value(1, 2);
        assert (agent.inputs >= 1);
        assert (agent.actions == 2);

        final float minRatio = 2f;

        int cycles = 100;

        Random r = new XoRoShiRo128PlusRandom(1);
        float nextReward = 0;
        IntIntHashMap acts = new IntIntHashMap();
        for (int i = 0; i < cycles; i++) {
            int action = agent.act(nextReward, new float[]{r.nextFloat()});

            acts.addToValue(action, 1);
            switch (action) {
                case 0:
                    nextReward = -1.0f;
                    break;
                case 1:
                    nextReward = +1.0f;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
        System.out.println(this.getClass().getSimpleName() + '\t' + agent.getClass() + ' ' + agent.summary() + '\n' + acts);
        assertTrue(acts.get(1) > acts.get(0));
        assertTrue(acts.get(1) / minRatio > acts.get(0));

    }
}
