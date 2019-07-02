package nars.nal;

import nars.derive.rule.Rules;

import static nars.Op.QUESTION;

/** TODO */
public class NAL1_Guess extends Rules {

    public NAL1_Guess() {

        ////Similarity to Inheritance
        //    (S --> P), (S <-> P),  task("?"),  hasBelief() |- (S --> P),   (Belief:BeliefStructuralIntersection, Punctuation:Belief)
        rule("(S-->P)", "(S<->P)", "(S-->P)")
            .hasBelief()
            .believe(QUESTION,"BeliefStructuralIntersection");
    }


}
