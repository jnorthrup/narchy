package mcaixictw;

import mcaixictw.worldmodels.WorldModelSettings;
import mcaixictw.worldmodels.Worldmodel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class AgentTest {




    @BeforeEach
    public void setUp() throws Exception {

        // 5, 5, false, 3, 8, 3

        int actions = 5;
        modelSettings = new WorldModelSettings();
        modelSettings.setDepth(5);

        Worldmodel model = Worldmodel.getInstance("AgentTestModel",
                modelSettings);

        agent = new Agent(actions, obsBits, rewBits, model);
    }

    Agent agent;
    double eps = 0.01;
    int obsBits = 8;
    int rewBits = 3;
    WorldModelSettings modelSettings;


    // Encodes a percept (observation, reward) as a list of symbols
    @Test
    public final void testEncodePercept() {

        int observation = 165;
        int reward = 4;

        BooleanArrayList result = McAIXIUtil.encode(observation, obsBits);
        result.addAll(McAIXIUtil.encode(reward, rewBits));

        // 10100101101
        assertTrue(result.get(0));
        assertTrue(!result.get(1));
        assertTrue(result.get(2));
        assertTrue(!result.get(3));
        assertTrue(!result.get(4));
        assertTrue(result.get(5));
        assertTrue(!result.get(6));
        assertTrue(result.get(7));
        assertTrue(result.get(8));
        assertTrue(!result.get(9));
        assertTrue(!result.get(10));

        result = agent.encodePercept(observation, reward);
        // 10100101101
        assertTrue(result.get(0));
        assertFalse(result.get(1));
        assertTrue(result.get(2));
        assertTrue(!result.get(3));
        assertTrue(!result.get(4));
        assertTrue(result.get(5));
        assertTrue(!result.get(6));
        assertTrue(result.get(7));
        assertTrue(result.get(8));
        assertTrue(!result.get(9));
        assertTrue(!result.get(10));
    }

    @Test
    public final void testGenPerceptAndUpdate() {
        System.out.println(modelSettings);
        System.out.println("history size: " + agent.getModel().historySize());
        agent.modelUpdate(0,0);
        agent.modelUpdate(0);
        agent.genPerceptAndUpdate();
    }

    @Test
    public final void testGetLastPercept() {
        int obs = 77;
        int rew = 2;
        int perception = (obs << rewBits) | rew;
        agent.modelUpdate(obs, rew);
        int lastPercept = agent.getLastPercept();
        assertTrue(perception == lastPercept);
        agent.genRandomActionAndUpdate();
        lastPercept = agent.getLastPercept();
        assertTrue(perception == lastPercept);
    }

    @Test
    public final void testGetLastAction() {
        agent.modelUpdate(0,0);
        agent.modelUpdate(0);
        agent.genPerceptAndUpdate();
        int a = agent.genRandomActionAndUpdate();
        assertTrue(a == agent.getLastAction());
        agent.genPerceptAndUpdate();
        assertTrue(a == agent.getLastAction());
    }

    @Test
    public final void testModelRevert() {
        agent.modelUpdate(0,0);
        int obs = 1;
        int rew = 1;
        agent.modelUpdate(0);
        double p = agent.perceptProbability(obs, rew);
        ModelUndo mu = new ModelUndo(agent);

        for (int i = 0; i < 10; i++) {
            agent.genPerceptAndUpdate();
            agent.genRandomActionAndUpdate();
        }
        agent.modelRevert(mu);
        double p2 = agent.perceptProbability(obs, rew);

        // System.out.println("p: " + p + " p2: " + p2 + " p2-p: " + (p2 - p));

        assertTrue(Math.abs(p2 - p) < eps);
    }

    @Test
    public final void testHistoryRevert() {
        agent.modelUpdate(0,0);
        agent.modelUpdate(0);
        int obs = 1;
        int rew = 1;
        double p = agent.perceptProbability(obs, rew);
        ModelUndo mu = new ModelUndo(agent);
        for (int i = 0; i < 10; i++) {
            agent.genPerceptAndUpdateHistory();
            agent.genRandomActionAndUpdate();
        }
        //agent.historyRevert(mu);
        assertTrue(agent.perceptProbability(obs, rew) - p < eps);
    }
}
