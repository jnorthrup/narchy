package nars.derive.premise;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

import static nars.term.atom.Atomic.the;

/**
 * Describes a derivation postcondition
 * Immutable
 */
public class PostCondition implements Serializable //since there can be multiple tasks derived per rule
{

    @NotNull
    public final Term pattern;

    @Nullable
    public final Term beliefTruth;
    @Nullable
    public final Term goalTruth;


    private PostCondition(@NotNull Term pattern, @Nullable Term beliefTruth, @Nullable Term goalTruth, byte puncOverride) {
        this.pattern = pattern;
        this.beliefTruth = beliefTruth;
        this.goalTruth = goalTruth;
        this.puncOverride = puncOverride;
    }


    public static final ImmutableSet<Atomic> reservedMetaInfoCategories = Sets.immutable.of(
            the("Belief"),
            the("Stamp"),
            the("Goal"),
            the("Order"),
            the("Permute"),
            the("Info"),
            the("Event"),
            the("Punctuation")
    );


    private static final Atomic
            swap = the("Swap");
    private static final Atomic backward = the("Backward");


    /**
     * if puncOverride == 0 (unspecified), then the default punctuation rule determines the
     * derived task's punctuation.  otherwise, its punctuation will be set to puncOverride's value
     */
    public final transient byte puncOverride;


    /**
     * @param rule      rule which contains and is constructing this postcondition
     * @param pattern
     * @param modifiers
     * @throws RuntimeException
     */
    @NotNull
    public static PostCondition make(PremiseDeriverProto rule, Term pattern,
                                     @NotNull Term... modifiers) throws RuntimeException {


        Term beliefTruth = null, goalTruth = null;

        //boolean negate = false;
        byte puncOverride = 0;

        for (Term m : modifiers) {
            if (m.op() != Op.INH)
                throw new RuntimeException("Unknown postcondition format: " + m);

            Term type = m.sub(1);
            Term which = m.sub(0);

            switch (type.toString()) {

                case "Punctuation":
                    switch (which.toString()) {
                        case "Question":
                            puncOverride = Op.QUESTION;
                            break;
                        case "Goal":
                            puncOverride = Op.GOAL;
                            break;
                        case "Belief":
                            puncOverride = Op.BELIEF;
                            break;
                        case "Quest":
                            puncOverride = Op.QUEST;
                            break;

                        default:
                            throw new RuntimeException("unknown punctuation: " + which);
                    }
                    break;

//                case "Truth":
//                    throw new UnsupportedOperationException("Use Belief:.. or Goal:..");

                case "Belief":
                    beliefTruth = which;
                    break;

                case "Goal":
                    goalTruth = which;
                    break;

//                case "Permute":
//                    if (which.equals(PostCondition.backward)) {
//                        rule.permuteBackward = true;
//                    } else if (which.equals(PostCondition.swap)) {
//                        rule.permuteForward = true;
//                    } else
//                        throw new RuntimeException("illegal Permute opcode: " + which);
//                    break;

//                case "Order":
//                    //ignore, because this only affects at TaskRule construction
//                    break;
//
//                case "Event":
//                    if (which.equals(PostCondition.anticipate)) {
//                        //IGNORED
//                        //rule.anticipate = true;
//                    }
//                    break;
//
//                case "Eternalize":
//                    if (which.equals(PostCondition.immediate)) {
//                        rule.eternalize = true;
//                    }
//                    break;

//                case "SequenceIntervals":
//                    //IGNORED
////                    if (which.equals(PostCondition.fromBelief)) {
////                        rule.sequenceIntervalsFromBelief = true;
////                    } else if (which.equals(PostCondition.fromTask)) {
////                        rule.sequenceIntervalsFromTask = true;
////                    }
//                    break;

                default:
                    throw new RuntimeException("Unhandled postcondition: " + type + ':' + which);
            }

        }

        PostCondition pc = new PostCondition(pattern, beliefTruth, goalTruth, puncOverride);

        if (!pc.modifiesPunctuation() && pattern instanceof Compound) {
            assert !rule.getTask().equals(pattern) :
                    "punctuation not modified yet rule task equals pattern: " + rule;
            assert !rule.getBelief().equals(pattern) :
                    "punctuation not modified yet rule belief equals pattern: " + rule + "\n\t" + rule.getBelief() + "\n\t" + pattern;
        }

        return pc;
    }


    private boolean modifiesPunctuation() {
        return puncOverride > 0;
    }


    @NotNull
    @Override
    public String toString() {
        return "PostCondition{" +
                "term=" + pattern +
                //", modifiers=" + Arrays.toString(modifiers) +
                ", beliefTruth=" + beliefTruth +
                ", goalTruth=" + goalTruth +
                ", puncOverride=" + puncOverride +
                '}';
    }


}
