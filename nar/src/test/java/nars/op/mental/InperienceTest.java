package nars.op.mental;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.util.TermTest.assertEq;

class InperienceTest {

	@Test
	void testReifyBelief() throws Narsese.NarseseException {
		NAR n = NARS.shell();

		assertEq(
			"believe(I,x)",
			Inperience.reifyBeliefOrGoal(n.inputTask("x. |"), $$("I"))
		);

		//negation is maintained internally to the overall positive belief
		assertEq(
			"believe(I,(--,x))",
			Inperience.reifyBeliefOrGoal(n.inputTask("--x. |"), $$("I"))
		);
	}
}