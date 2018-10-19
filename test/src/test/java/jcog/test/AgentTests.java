package jcog.test;

import com.google.common.collect.Sets;
import jcog.learn.Agent;
import jcog.learn.ql.DQN2;
import jcog.learn.ql.HaiQ;
import jcog.learn.ql.HaiQae;
import nars.agent.EmbeddedNAgent;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;


class AgentTests implements AfterAllCallback {

    static final IntIntToObjectFunction<?/* Agent */>[] AGENTS = {
        HaiQ::new, HaiQae::new, DQN2::new, EmbeddedNAgent::new
    };

    static final Class<? /*AbstractAgentTest*/>[] ENVS = {
        OneObviousChoice.class, InputMatchesChoice.class, CliffWalking.class
    };

    private AbstractAgentTest env;

    @MethodSource("args")
    @ParameterizedTest() void testAll(Class<? extends AbstractAgentTest> envClass, IntIntToObjectFunction<Agent> agent) {
        try {
            env = envClass.getConstructor().newInstance();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        env.test(agent);
    }

    private static Stream<Arguments> args() {
        return Sets.cartesianProduct( Set.of(ENVS),  Set.of(AGENTS) ).stream()
                .map(x -> Arguments.of(x.toArray()) );
    }

    @AfterEach
    public void afterEach(TestReporter testInfo) throws Exception {
//        if (!shouldBeBenchmarked(context))
//            return;
//
//        long launchTime = loadLaunchTime(context, LaunchTimeKey.TEST);
//        long elapsedTime = currentTimeMillis() - launchTime;
//        report("Test", context, elapsedTime);
        env.after(testInfo);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {

    }
}
