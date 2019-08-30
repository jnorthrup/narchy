package nars.op;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.eval.Evaluation;
import nars.term.Compound;
import nars.test.TestNAR;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MemberTest {

	private final NAR n = NARS.shell();

	@Test
	void Member1_true() {
		Assertions.assertEquals(
			Set.of($$("member(a,{a,b})")),
			Evaluation.eval($$("member(a,{a,b})"), n));
	}

	@Disabled
	@Test void Member1_product_arg() {
		Assertions.assertEquals(
			Set.of($$("member(a,(a,b))")),
			Evaluation.eval($$("member(a,(a,b))"), n));

	}

	@Test void Member1_false() {
		assertEquals(
			Set.of($$("--member(c,{a,b})")),
			Evaluation.eval($$("member(c,{a,b})"), true, true, n));

	}

	@Test void Member1_generator() {
		Assertions.assertEquals(
			Set.of($$("member(a,{a,b})"), $$("member(b,{a,b})")),
			Evaluation.eval($$("member(#x,{a,b})"), n));
	}

	@Test void member_unwrap1() {
		Assertions.assertEquals(
			Set.of($$("a"), $$("b")),
			Evaluation.eval($$("(member(#x,{a,b}) && #x)"), n));
	}
	@Test void member_unwrap2() {
		Assertions.assertEquals(
			Set.of($$("(a)"),$$("(b)")),
			Evaluation.eval($$("(member(#x,{a,b}) && (#x))"), n));
	}

	@Test void member_can_not_unwrap_due_to_var() {
		Assertions.assertEquals(
			Set.of($$("(member(f(#x),{a,b}) && (#x))")),
			Evaluation.eval($$("(member(f(#x),{a,b}) && (#x))"), n));
	}

	@Test void MultiTermute1() {
		nars.term.Term s = $.$$("(((--,(tetris-->((--,#2)&&left))) &&+4710 (member(#1,{right,#2})&&(tetris-->#1))) &&+4000 member(#1,{2,3}))");
		Assertions.assertEquals(
			Set.of(
				$$("((--,(tetris-->((--,2)&&left))) &&+4710 (tetris-->2))"),
				$$("((--,(tetris-->((--,3)&&left))) &&+4710 (tetris-->3))")
			),
			Evaluation.eval((Compound)s, n));
	}


	@Test void Member_Combine_Rule() {
		NAR n = NARS.shell();
		new Deriver(Derivers.files(n, "nal2.member.nal"));
		TestNAR t = new TestNAR(n);
		t.believe("(member(#1,{a,b}) && (x(#1), y(#1)))");
		t.believe("(member(#1,{c,d}) && (x(#1), y(#1)))");
		int cycles = 500;
		t.mustBelieve(cycles, "(member(#1,{a,b,c,d}) && (x(#1), y(#1)))", 1, 0.81f);
		t.run(cycles);
	}
	@Test void Member_Diff_Rule() {
		NAR n = NARS.shell();
		new Deriver(Derivers.files(n, "nal2.member.nal"));
		TestNAR t = new TestNAR(n);
		t.believe("  (member(#1,{a,b,c}) && (x(#1), y(#1)))");
		t.believe("--(member(#1,{b,d  }) && (x(#1), y(#1)))");
		int cycles = 500;
		t.mustBelieve(cycles, "(member(#1,{a,c}) && (x(#1), y(#1)))", 1, 0.81f);
		t.run(cycles);
	}

	@ParameterizedTest
 	@ValueSource(strings = {"&&", "==>"}) void member_Budgeting(String op) {
		NAR n = NARS.shell();
		TestNAR t = new TestNAR(n);

		int cycles = 2;
		final double initPri = 0.5f;
		final double[] priSum = {0};
		n.onTask((x)->{
			priSum[0] += x.pri();
		});

		String belief = "(member(#x,{a,b}) " + op + " #x)";

		//t.mustBelieve(cycles,belief,1f,0.9f); //the belief itself
		t.mustBelieve(cycles,"a",1f,0.9f);
		t.mustBelieve(cycles,"b",1f,0.9f);

		t.input("$" + initPri + " " + belief + ".");


		t.run(cycles);

		assertEquals(initPri, priSum[0], 0.01);
	}

}
