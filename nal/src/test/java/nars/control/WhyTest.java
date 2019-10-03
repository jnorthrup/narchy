package nars.control;

import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WhyTest {

	static final Term x = Why.why(Why.why((short) 1), Why.why(new short[]{(short) 2, (short) 3}, 5), 5);

	@Test
	void testMerge() {
		assertEq("1", Why.why((short) 1));
		assertEq("{1,2}", Why.why(Why.why((short) 1), Why.why((short) 2), 3));

		assertEq("{{2,3},1}", x);
	}
	@Test
	void testForceLinearize() {
		assertEq("{1,2,3}", Why.why(Why.why((short) 1), Why.why(new short[]{(short) 2, (short) 3}, 4), 4));
	}

	@Test
	void testForceSample() {
		assertEquals(3, Why.why(Why.why((short)1), Why.why(new short[]{(short)2, (short)3}, 3), 3).volume());
	}

	@Test void testEval() {
		Map m = new HashMap();
		Why.eval(x, 1, m::put);
		assertEquals("{1=0.5, 2=0.25, 3=0.25}", m.toString());
	}

}