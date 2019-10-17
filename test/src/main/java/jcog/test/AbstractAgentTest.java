package jcog.test;

import jcog.learn.Agent;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.junit.jupiter.api.TestReporter;

/**
 * Generic MDP test abstraction
 *
 * https://github.com/openai/gym/tree/master/gym/envs
 * https://github.com/jmacglashan/burlap/tree/master/src/main/java/burlap/domain/singleagent
 */
public abstract class AbstractAgentTest {

//    public void before(ExtensionContext context) {
//
//    }

    protected abstract void test(IntIntToObjectFunction<Agent> agentBuilder);

    public void after(TestReporter context) {
//        String message = String.format(
//                "%s '%s' took %d ms.",
//                unit, context.getDisplayName(), elapsedTime);
        //context.publishEntry("test" ,"x");

    }

}