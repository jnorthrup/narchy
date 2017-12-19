package nars.util.signal;

import nars.Narsese;
import nars.Task;
import nars.test.TestNAR;
import org.jetbrains.annotations.NotNull;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * test an invididual premise
 */
public interface RuleTest {

//	@NotNull
//	public static RuleTest from(@NotNull PremiseRule r) {
//		// TODO eventually make this handle all of r's postconditions and
//		// modifiers, etc
//		String task = r.task().toString();
//		task = task.replace("%", "p"); // TODO do proper term replacement
//
//		String belief = r.belief().toString();
//		belief = belief.replace("%", "p"); // TODO do proper term replacement
//
//		String conc = r.getConclusion().term(0).toString();
//		conc = conc.replace("%", "p");
//		char concPunc = '.';
//
//		char beliefPunc = '.';
//		char taskPunc = '.';
//		return RuleTest(new TestNAR(new Default()), task + taskPunc, belief + beliefPunc, conc
//				+ concPunc);
//	}

//	public RuleTest(@NotNull String task, @NotNull String belief, String result) {
//		this(task, belief, result, 0, 1, 0, 1);
//	}
	@NotNull
	static TestNAR get(@NotNull TestNAR test, @NotNull String task, @NotNull String belief, @NotNull String result, float minFreq,
					   float maxFreq, float minConf, float maxConf) {
		test(
				// new SingleStepNAR(),
				test, task, belief, result, minFreq, maxFreq, minConf,
				maxConf);
		return test;
	}

	//private static final Narsese p = Narsese.the();

	static void test(@NotNull TestNAR test, @NotNull String task, @NotNull String belief, @NotNull String result,
					 float minFreq, float maxFreq, float minConf, float maxConf) {
		try {
            test(test, Narsese.the().task(task, test.nar), Narsese.the().task(belief, test.nar), result, minFreq, maxFreq,
                    minConf, maxConf);
		} catch (Narsese.NarseseException e) {
			e.printStackTrace();
            fail(e);
		}

	}
	static void test(@NotNull TestNAR test, @NotNull Task task, @NotNull Task belief, @NotNull String result,
                     float minFreq, float maxFreq, float minConf, float maxConf) {

		test.nar.input(task, belief);
		//test.log();
		test.mustBelieve(25, result, minFreq, maxFreq, minConf, maxConf);

	}


}
