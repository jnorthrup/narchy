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
public final class PostCondition implements Serializable 
{

    public final Term pattern;

    @Nullable
    public final Term beliefTruth;
    @Nullable
    public final Term goalTruth;


    protected PostCondition(@NotNull Term pattern, @Nullable Term beliefTruth, @Nullable Term goalTruth, byte puncOverride) {
        this.pattern = pattern;
        this.beliefTruth = beliefTruth;
        this.goalTruth = goalTruth;
        this.puncOverride = puncOverride;
    }


    static final ImmutableSet<Atomic> reservedMetaInfoCategories = Sets.immutable.of(
            Atomic.the("Belief"),
            Atomic.the("Goal"),
            Atomic.the("Punctuation"),
            Atomic.the("Time")
    );







    /**
     * if puncOverride == 0 (unspecified), then the default punctuation rule determines the
     * derived task's punctuation.  otherwise, its punctuation will be set to puncOverride's value
     */
    final transient byte puncOverride;


    protected boolean modifiesPunctuation() {
        return puncOverride > 0;
    }


    @NotNull
    @Override
    public String toString() {
        return "PostCondition{" +
                "term=" + pattern +
                
                ", beliefTruth=" + beliefTruth +
                ", goalTruth=" + goalTruth +
                ", puncOverride=" + puncOverride +
                '}';
    }


}
