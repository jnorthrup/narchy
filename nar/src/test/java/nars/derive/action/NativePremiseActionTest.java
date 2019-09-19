package nars.derive.action;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.rule.PremiseRuleSet;
import org.junit.jupiter.api.Test;

class NativePremiseActionTest {

	@Test
	void testNativeDeriveAction_combined_with_MetaNAL() throws Narsese.NarseseException {

		NAR n = NARS.shell();
		PremiseRuleSet d = new PremiseRuleSet(n)
			.add(
				new NativePremiseAction() {

					{
						taskPattern("(%A,%B,%C)");
					}

					@Override
					protected void run(Derivation d) {
						System.out.println("match: " + d);
					}

					@Override
					protected float pri(Derivation d) {
						return 0.5f;
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