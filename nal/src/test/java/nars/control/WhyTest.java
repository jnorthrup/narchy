package nars.control;

import org.junit.jupiter.api.Test;

import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WhyTest {


	@Test
	void testMerge() {
		assertEq("{1,2}", Why.why(Why.why((short) 1), Why.why((short) 2), 3));
		assertEq("{{2,3},1}", Why.why(Why.why((short) 1), Why.why(new short[]{(short) 2, (short) 3}, 5), 5));
	}
	@Test
	void testForceLinearize() {
		assertEq("{1,2,3}", Why.why(Why.why((short) 1), Why.why(new short[]{(short) 2, (short) 3}, 4), 4));
	}

	@Test
	void testForceSample() {
		assertEquals(3, Why.why(Why.why((short)1), Why.why(new short[]{(short)2, (short)3}, 3), 3).volume());
	}
}