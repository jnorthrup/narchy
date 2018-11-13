package mcaixictw;

import mcaixictw.environments.CoinFlipEnv;
import mcaixictw.worldmodels.WorldModelSettings;
import mcaixictw.worldmodels.Worldmodel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

public class RunAgentTest {

	private static Logger log = Logger.getLogger(RunAgentTest.class
			.getName());



	@BeforeEach
	public void setUp() {

//		// set up the biased coin environment. The coin lands on one side with a
//		// probability of 0.7.
		double p_biased_coin = 0.9;
		env = new CoinFlipEnv(p_biased_coin);

//		env = new Maze1DEnv(5);

		ctName = env.getClass().getSimpleName();

		WorldModelSettings modelSettings = new WorldModelSettings();
		modelSettings.setFacContextTree(false);
		modelSettings.setDepth(10);

		UCTSettings uctSettings = new UCTSettings();
		uctSettings.setHorizon(2);
		uctSettings.setMcSimulations(4);
		uctSettings.setRecycleUCT(true);

		ControllerSettings controllerSettings = new ControllerSettings();



		Worldmodel model = Worldmodel.getInstance(ctName, modelSettings);
		controller = new AgentController(env, controllerSettings, uctSettings, model);

	}

	private AgentController controller;
	private String ctName;
	private Environment env;
	int n = 64;

	@Test
	public final void test() {


		// A smart agent should learn to always choose the biased side and
		// should come close to an average reward of 0.7
		controller.play(n, false);


	}

}
