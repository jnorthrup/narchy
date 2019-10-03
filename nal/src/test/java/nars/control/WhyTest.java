package nars.control;

import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.SETe;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WhyTest {

	//TODO move to TermTest
	@Test void embeddedSetDontFlatten() {
		Term a = $$("1");
		Term b = $$("2");
		Term c = $$("3");
		assertEq("{{1,2},3}", SETe.the(SETe.the(new Term[] {a, b}), c));
	}

	@Test
	void testMerge() {
		assertEq("{1,2}", Why.why(Why.why((short)1), Why.why((short)2), 3));
		assertEq("{{2,3},1}", Why.why(Why.why((short)1), Why.why(new short[]{(short)2, (short)3}, 5), 5));
		assertEq("{1,2,3}", Why.why(Why.why((short)1), Why.why(new short[]{(short)2, (short)3}, 4), 4));
		assertEquals(3, Why.why(Why.why((short)1), Why.why(new short[]{(short)2, (short)3}, 3), 3).volume());
	}
}