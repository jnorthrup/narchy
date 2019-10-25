package nars.term;

import nars.$;
import nars.Narsese;
import nars.term.atom.Atomic;
import nars.term.util.transform.VariableTransform;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 8/28/15.
 */
public class VariableTest {


	private static void testVariableSorting(String a, String b) {
		Compound A = raw(a);
		Compound B = raw(b);


		assertNotEquals(A, B);


		Term NA = A.normalize();
		Term NB = B.normalize();
		System.out.println(A + "\t" + B);
		System.out.println(NA + "\t" + NB);

		assertEquals(NA, NB);
	}

	private static Compound raw(String a) {
		try {
			return (Compound) Narsese.term(a, false);
		} catch (Narsese.NarseseException e) {
			fail(e);
			return null;
		}
	}

	@Test
	void testPatternVarVolume() throws Narsese.NarseseException {

		assertEquals(0, Narsese.term("$x").complexity());
		assertEquals(1, Narsese.term("$x").volume());

		assertEquals(0, Narsese.term("%x").complexity());
		assertEquals(1, Narsese.term("%x").volume());

		assertEquals(Narsese.term("<x --> y>").volume(),
			Narsese.term("<%x --> %y>").volume());

	}

	@Test
	void testNumVars() throws Narsese.NarseseException {
		assertEquals(1, Narsese.term("$x").vars());
		assertEquals(1, Narsese.term("#x").vars());
		assertEquals(1, Narsese.term("?x").vars());
		assertEquals(1, Narsese.term("%x").vars());

		assertEquals(2, INSTANCE.$("<$x <-> %y>").vars());
	}

	@Test
	void testBooleanReductionViaHasPatternVar() throws Narsese.NarseseException {
		Compound d = INSTANCE.$("<a <-> <$1 --> b>>");
		assertEquals(0, d.varPattern());

		Compound c = INSTANCE.$("<a <-> <%1 --> b>>");
		assertEquals(1, c.varPattern());

		Compound e = INSTANCE.$("<%2 <-> <%1 --> b>>");
		assertEquals(2, e.varPattern());

	}

	@Test
	void testNormalizeUnitCompound() {
		assertEq("(#1)", INSTANCE.$$("(#2)").normalize());
		assertEq("(#1)", INSTANCE.$$("(#1)").normalize());
	}

	@Test
	void testNormalizeNegs() {
		assertEq("(--,#1)", INSTANCE.$$("(--,#2)").normalize());
		assertEq("a((--,#1))", INSTANCE.$$("a((--,#2))").normalize());
		assertEq("(--,#1)", INSTANCE.$$("(--,#1)").normalize());
		assertEq("(--,#1)", INSTANCE.$$("(--,#x)").normalize());

		assertEq("((--,#1)&&(--,#2))", INSTANCE.$$("((--,#3) && (--,#2))").normalize());
	}

	/**
	 * tests target sort order consistency
	 */
	@Test
	void testVariableSubtermSortAffect0() {

		assertEquals(-1, $.INSTANCE.varIndep(1).compareTo($.INSTANCE.varIndep(2)));


		Compound k1 = $.INSTANCE.inh($.INSTANCE.varIndep(1), Atomic.the("key"));
		Compound k2 = $.INSTANCE.inh($.INSTANCE.varIndep(2), Atomic.the("key"));
		Compound l1 = $.INSTANCE.inh($.INSTANCE.varIndep(1), Atomic.the("lock"));
		Compound l2 = $.INSTANCE.inh($.INSTANCE.varIndep(2), Atomic.the("lock"));
		assertEquals(-1, k1.compareTo(k2));
		assertEquals(+1, k2.compareTo(k1));
		assertEquals(-1, l1.compareTo(l2));
		assertEquals(+1, l2.compareTo(l1));

		assertEquals(l1.compareTo(k1), -k1.compareTo(l1));
		assertEquals(l2.compareTo(k2), -k2.compareTo(l2));


		assertEquals(-1, k1.compareTo(l2));
		assertEquals(-1, k2.compareTo(l1));
		assertEquals(+1, l2.compareTo(k1));
		assertEquals(+1, l1.compareTo(k2));


		testVariableSorting("(($1-->key)&&($2-->lock))", "(($1-->lock)&&($2-->key))");

	}

	/**
	 * tests target sort order consistency
	 */
	@Test
	void testVariableSubtermSortAffectNonComm() {

		testVariableSorting("(($1-->key),($2-->lock))", "(($2-->key),($1-->lock))");

	}

	/**
	 * tests target sort order consistency
	 */
	@Test
	void testVariableSubtermSortAffect1() {

		testVariableSorting(
			"((($1-->lock)&&($2-->key))==>open($2,$1))",
			"((($1-->key)&&($2-->lock))==>open($1,$2))"
		);
		testVariableSorting(
			"((($1-->key)&&($2-->lock))==>open($1,$2))",
			"((($1-->lock)&&($2-->key))==>open($2,$1))"

		);

	}

	@Test
	void VariableTransform1() {
		Term y = INSTANCE.$$("(_3(#1,_1) &&+1 _2(#1,$2))").transform(VariableTransform.indepToQueryVar);
		assertEq("(_3(#1,_1) &&+1 _2(#1,?$2))", y);
		assertEq("(_3(#1,_1) &&+1 _2(#1,?2))", y.normalize());
	}

	@Test
	void NormalizationComplex() {
		assertEq("(#1,$2)", INSTANCE.$$("(#1,$1)").normalize());
		assertEq("(#1,?2)", INSTANCE.$$("(#1,?1)").normalize());
		assertEq("(#1,($2==>($2)))", INSTANCE.$$("(#1,($1==>($1)))").normalize());
		assertEq("(($1==>($1)),#2)", INSTANCE.$$("(($1==>($1)),#1)").normalize());
	}

	@Test
	void NormalizationComplex2() {
		Term x = INSTANCE.$$$("((($1-->Investor)==>(possesses($1,#2)&&({#2}-->Investment))) ==>+- ({#1}-->Investment))");
		assertFalse(x.isNormalized());
		assertEq("((($1-->Investor)==>(possesses($1,#2)&&({#2}-->Investment))) ==>+- ({#3}-->Investment))", x.normalize());
	}

	@Test
	void testInhMultipleDep() {

		String s = "(x(#1)-->x(#2))";
		assertEq(s, s);


		String r = "(x(#y)-->x(#z))";
		assertEq(s, r);

	}
}
