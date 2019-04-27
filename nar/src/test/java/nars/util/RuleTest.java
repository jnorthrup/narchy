package nars.util;

import nars.Narsese;
import nars.Task;
import nars.test.TestNAR;
import org.jetbrains.annotations.NotNull;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * test an invididual premise
 */
public interface RuleTest {
























	@NotNull
	static TestNAR get(@NotNull TestNAR test, @NotNull String task, @NotNull String belief, @NotNull String result, float minFreq,
					   float maxFreq, float minConf, float maxConf) {
		test(
				
				test, task, belief, result, minFreq, maxFreq, minConf,
				maxConf);
		return test;
	}

	

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
		
		test.mustBelieve(25, result, minFreq, maxFreq, minConf, maxConf);

	}


}
