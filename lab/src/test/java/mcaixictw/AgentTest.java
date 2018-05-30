package mcaixictw;

import mcaixictw.worldmodels.WorldModelSettings;
import mcaixictw.worldmodels.Worldmodel;
import org.junit.jupiter.api.*;

abstract public class AgentTest {

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	public static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	public void setUp() throws Exception {

		

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

	@AfterEach
	public void tearDown() throws Exception {
	}

	
	@Test
	public final void testEncodePercept() {

		int observation = 165;
		int reward = 4;

		BooleanArrayList result = Util.encode(observation, obsBits);
		result.addAll(Util.encode(reward, rewBits));

		
		Assertions.assertTrue(result.get(0) == true);
		Assertions.assertTrue(result.get(1) == false);
		Assertions.assertTrue(result.get(2) == true);
		Assertions.assertTrue(result.get(3) == false);
		Assertions.assertTrue(result.get(4) == false);
		Assertions.assertTrue(result.get(5) == true);
		Assertions.assertTrue(result.get(6) == false);
		Assertions.assertTrue(result.get(7) == true);
		Assertions.assertTrue(result.get(8) == true);
		Assertions.assertTrue(result.get(9) == false);
		Assertions.assertTrue(result.get(10) == false);

		result = agent.encodePercept(observation, reward);
		
		Assertions.assertTrue(result.get(0));
		Assertions.assertFalse(result.get(1));
		Assertions.assertTrue(result.get(2) == true);
		Assertions.assertTrue(result.get(3) == false);
		Assertions.assertTrue(result.get(4) == false);
		Assertions.assertTrue(result.get(5) == true);
		Assertions.assertTrue(result.get(6) == false);
		Assertions.assertTrue(result.get(7) == true);
		Assertions.assertTrue(result.get(8) == true);
		Assertions.assertTrue(result.get(9) == false);
		Assertions.assertTrue(result.get(10) == false);
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
		Assertions.assertTrue(perception == lastPercept);
		agent.genRandomActionAndUpdate();
		lastPercept = agent.getLastPercept();
		Assertions.assertTrue(perception == lastPercept);
	}

	@Test
	public final void testGetLastAction() {
		agent.modelUpdate(0,0);
		agent.modelUpdate(0);
		agent.genPerceptAndUpdate();
		int a = agent.genRandomActionAndUpdate();
		Assertions.assertTrue(a == agent.getLastAction());
		agent.genPerceptAndUpdate();
		Assertions.assertTrue(a == agent.getLastAction());
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

		

		Assertions.assertTrue(Math.abs(p2 - p) < eps);
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
		agent.historyRevert(mu);
		Assertions.assertTrue(agent.perceptProbability(obs, rew) - p < eps);
	}
}
