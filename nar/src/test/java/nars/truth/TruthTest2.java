package nars.truth;

import jcog.Texts;
import nars.NAR;
import nars.NARS;
import nars.task.util.Revision;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static nars.$.t;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TruthTest2 {
	@Test
	void testTruthRevision() {
		Truth d = Revision.revise(new DiscreteTruth(1f, 0.1f), new DiscreteTruth(1f, 0.1f));
		assertEquals(1f, d.freq(), 0.01f);
		assertEquals(0.18f, d.conf(), 0.01f);

		Truth a = Revision.revise(new DiscreteTruth(1f, 0.3f), new DiscreteTruth(1f, 0.3f));
		assertEquals(1f, a.freq(), 0.01f);
		assertEquals(0.46f, a.conf(), 0.01f);

		Truth b = Revision.revise(new DiscreteTruth(0f, 0.3f), new DiscreteTruth(1f, 0.3f));
		assertEquals(0.5f, b.freq(), 0.01f);
		assertEquals(0.46f, b.conf(), 0.01f);

		Truth c = Revision.revise(new DiscreteTruth(1f, 0.9f), new DiscreteTruth(1f, 0.9f));
		assertEquals(1f, c.freq(), 0.01f);
		assertEquals(0.95f, c.conf(), 0.01f);
	}

	@Test void testDitheringSanity() {
		NAR n = NARS.shell();
		n.confResolution.set(0.1f);
		int steps = 99;
		float step = 0.01f;
		int subSteps = 9;
		float subStep = 0.001f;
		for (int i = 1; i < steps; i++) {
			for (int j = 1; j < subSteps; j++) {
				float c = step * i + (subStep) * j;
				@Nullable PreciseTruth p = t(1f, c).dither(n);
				if (p!=null)
					System.out.println(p + "\t" + Texts.n2(c) + '\t' + Texts.n4(c)+ '\t' + c );
			}
		}
	}


}
