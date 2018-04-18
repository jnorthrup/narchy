package nars.derive.premise;

import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Describes a derivation postcondition
 * Immutable
 */
public final class PostCondition implements Serializable //since there can be multiple tasks derived per rule
{

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
            Atomic.the("Belief"),
            Atomic.the("Stamp"),
            Atomic.the("Goal"),
            Atomic.the("Order"),
            Atomic.the("Permute"),
            Atomic.the("Info"),
            Atomic.the("Event"),
            Atomic.the("Punctuation")
    );


//    private static final Atomic
//            swap = the("Swap");
//    private static final Atomic backward = the("Backward");


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
    public static PostCondition the(PremiseDeriverProto rule, Term pattern,
                                    byte puncOverride,
                                    Term beliefTruth, Term goalTruth) throws RuntimeException {



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
