package nars.nal.nal5;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class ConversionAndContrapositionTest extends NAL5Test {

	@Test
	void testConversion() {

		test
			.termVolMax(3)
			.input("(x==>y)?")
			.input("(y==>x).")
			.mustBelieve(cycles, "(x==>y)", 1.0f, 0.47f)
		;
	}

	@Test
	void testConversionNeg() {

		test
			.termVolMax(4).confMin(0.45f)
			.input("(x ==> y)?")
			.input("(--y ==> x).")
			.mustBelieve(cycles, "(x ==> y)", 0.0f, 0.47f)
		;
	}


	@Test
	void testConversionNeg3() {
		test
			.termVolMax(4).confMin(0.45f)
			.input("(--x ==> y)?")
			.input("(y ==> --x).")
			.mustBelieve(cycles, "(--x ==> y)", 1f, 0.47f)
		;
	}
	@Test
	void contraposition() {
		test.termVolMax(10);
		test.believe("(--(robin --> bird) ==> (robin --> [flying]))", 0.1f, 0.9f);
		test.mustBelieve(cycles, " (--(robin --> [flying]) ==> (robin --> bird))",
			0.1f, 0.42f /*0.36f*/);
		//0f, 0.45f);
	}

	@Test
	void contrapositionPos() {
		test.termVolMax(9);
		test
			.believe("(--B ==> A)", 0.9f, 0.9f)
			.mustBelieve(cycles, " (--A ==> B)",
				0.9f, 0.42f /*0.36f*/);
		//0.1f, 0.36f);
		//0f, 0.08f);
	}

	@Test
	void contrapositionNeg() {
		test
			.confMin(0.4f)
			.believe("(--B ==> A)", 0.1f, 0.9f)
			.mustBelieve(cycles, " (--A ==> B)",
				0.1f, 0.42f /*0.36f*/);

	}


}
