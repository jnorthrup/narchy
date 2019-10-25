package nars.term.util.transform;

import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static nars.$.*;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertSame;

class MapSubstTest {

	@Test
	void interlock_InNonCommutve_2() {
		assertEq("(b,a)", INSTANCE.$$("(a,b)").replace(Map.of(
			INSTANCE.$$("a"), INSTANCE.$$("b"),
			INSTANCE.$$("b"), INSTANCE.$$("a"))));
	}
	@Test
	void interlock_InNonCommutve_2_3() {
		assertEq("(b,a,c)", INSTANCE.$$("(a,b,c)").replace(Map.of(
			INSTANCE.$$("a"), INSTANCE.$$("b"),
			INSTANCE.$$("b"), INSTANCE.$$("a"))));
	}

	@Test
	void interlock_identical_InCommutve_2() {
		Term aAndB = INSTANCE.$$("(a&&b)");
		assertSame(aAndB, aAndB.replace(Map.of(
			INSTANCE.$$("a"), INSTANCE.$$("b"),
			INSTANCE.$$("b"), INSTANCE.$$("a"))));
	}
	@Test
	void interlock_identical_InCommutve_2_3() {
		Term aAndB = INSTANCE.$$("(&&,a,b,c)");
		assertSame(aAndB, aAndB.replace(Map.of(
			INSTANCE.$$("a"), INSTANCE.$$("b"),
			INSTANCE.$$("b"), INSTANCE.$$("a"))));
	}
}