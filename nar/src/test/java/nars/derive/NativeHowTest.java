package nars.derive;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.derive.action.NativeHow;
import nars.derive.rule.PremiseRuleSet;
import nars.derive.rule.RuleCause;
import org.junit.jupiter.api.Test;

class NativeHowTest {

	@Test
	void testNativeDeriveAction_combined_with_MetaNAL() throws Narsese.NarseseException {

		NAR n = NARS.shell();
		PremiseRuleSet d = new PremiseRuleSet(n)
			.add(
				new NativeHow() {

					{
						taskPattern("(%A,%B,%C)");
					}

					@Override
					protected void run(RuleCause why, Derivation d) {
						System.out.println("match: " + d);
					}

                }
			)
			.add(
				"(A,B),(A,B) |- B, (Punctuation:Ask)"
//                "X,Y |- (X&&Y), (Belief:Intersection)",
//                "{X},Y |- (X&&Y), (Belief:Deduction)",
//                "{X},Y |- (||,X,Y), (Belief:Union)"
			);

		new Deriver(d);

		n.log();
		n.believe("(x,y)");
		n.believe("(x,y,z)");
		n.run(1000);
	}
}