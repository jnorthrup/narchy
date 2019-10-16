package nars.nal.nal1;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.GOAL;

public class NAL1GoalTest extends NALTest {

	private final int cycles = 50;

	@Override
	protected NAR nar() {
		return NARS.tmp(1);
	}

	@Test
	void questFromInhSiblingGoal_ext() {
		test
			.input("(a-->c)!")
			.input("(b-->c).")
			.mustQuest(cycles, "(b-->c)")
		;
	}


	@Test
	void deductionPositiveGoalNegativeBelief() {
		test
			.input("(a-->b)!")
			.input("(b-->c). %0.2%")
			.mustGoal(cycles, "(a-->c)", 1f, 0.16f);//1f, 0.08f)
		;
	}


	@Test
	void deductionNegativeGoalPositiveBelief() {
		test
			.input("--(nars --> stupid)!")
			.input("(stupid --> dangerous).")
			.mustGoal(cycles, "(nars-->dangerous)", 0f, 0.81f)
		;
	}

	@Disabled
	@Test
	void deductionNegativeGoalPositiveBeliefSwap() {
		//(B --> C), (A --> B), neqRCom(A,C)    |- (A --> C), (Belief:DeductionX)
		test
			.input("--(nars --> stupid)!")
			.input("(derivation --> nars).")
			.mustGoal(cycles, "(derivation-->stupid)", 0f, 0.81f)
			.mustNotOutput(cycles, "(stupid-->derivation)", GOAL, 0, 1, 0.5f, 1, (t) -> true)
		;
	}

	@Test
	void abductionNegativeGoalPositiveBelief() {
		test
			.goal("--(nars --> stupid)")
			.believe("(human --> stupid)")
			.mustGoal(cycles, "(nars --> human)", 0f, 0.45f)
			.mustGoal(cycles, "(human --> nars)", 0f, 0.45f);
	}

	@Test
	void inductionNegativeGoalPositiveBelief() {
		test
			.goal("--(human --> stupid)")
			.believe("(nars --> stupid)")
			.mustGoal(cycles, "(nars --> human)", 0f, 0.45f)
			.mustGoal(cycles, "(human --> nars)", 0f, 0.45f);
	}

	@Test
	void similarityGoalPosBeliefPos() {
		test
			.input("(a-->c)!")
			.input("(c-->a).")
			.mustGoal(cycles, "(a<->c)", 1f, 0.81f)
		;
	}

	@Test
	void similarityGoalPosBeliefNeg() {
		test
			.input("(a-->c)!")
			.input("(c-->a). %0.25;0.90%")
			.mustGoal(cycles, "(a<->c)", 0.25f, 0.2f)
		;
	}

	@Test
	void similarityGoalNegBeliefNeg() {
		test
			.input("--(c-->a)!")
			.input("--(a-->c).")
			.mustGoal(cycles, "(a<->c)", 0f, 0.81f)
		;
	}


}
