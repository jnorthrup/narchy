package nars.op;

import nars.term.Term;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static nars.op.DepIndepVarIntroduction.ConjOrStatementBits;

/**
 * a generalized variable introduction model that can transform tasks
 */
public abstract class VarIntroduction {

//    static final int maxSubstitutions = 1;


    public Pair<Term, Map<Term, Term>> accept(Term x, Random r) {

        if (x.volume() < 2 || !x.hasAny(ConjOrStatementBits))
            return null;

        boolean inputWasNormalized = x.isNormalized();


//        Map<Term, Term> substs = new HashMap<>(1, 0.9f);
//
        int varOffset = x.vars(); //ensure the variables dont collide with existing variables
//        boolean found = false;
//        for (int i = 0, selectionsSize = selections.size(); i < selectionsSize; i++) {
            Term u = next(x, r);
            if (u == null)
                return null;
            Term v = next(x, u, ++varOffset);
//            if (v != null) {
//                substs.put(u, v);
//                found = true;
//            }
//        }
//        if (!found)
//            return null;


        Term y = x.replace(u, v);
        if (y!=null && !y.equals(x)) {
            if (inputWasNormalized) {
                y = y.normalize();
                if (y == null) return null;
            }

            return Tuples.pair(y, Map.of(u,v));
        } else {
            return null;
        }

//        while (selections.hasNext()) {
//
//            Term s = selections.next();
//
//
//
//            if (dd != null) {
//                int replacements = dd.length;
//                if (replacements > 0) {
//
//                    if (replacements > 1)
//                        throw new UnsupportedOperationException("TODO"); //choose one randomly
//
//                    Term d = dd[0];
//
//
//                }
//            }
//        }
    }


    abstract protected Term next(Term input, Random shuffle);


    /**
     * provides the next terms that will be substituted in separate permutations; return null to prevent introduction
     */
    abstract protected Term next(Term input, Term selection, int order);
    /*{
        return $.varQuery("c" + iteration);
    }*/


//    @Nullable
//    protected void input(@NotNull Task original, @NotNull Term newContent) {
//
//        Compound c = nar.normalize((Compound) newContent);
//        if (c != null && !c.equals(original.term())) {
//
//            Task derived = clone(original, c);
//
//            nar.inputLater(derived);
//
//        }
//
//    }

//    @NotNull protected Task clone(@NotNull Task original, @NotNull Compound c) {
//        MutableTask t = new VarIntroducedTask(c, original)
//            .time(original.creation(), original.occurrence())
//            .evidence(Stamp.cyclic(original.evidence()))
//            .budgetSafe(original.budget());
//
////            if (Param.DEBUG)
////                t.log(tag + ":\n" + (original.log() != null ? Joiner.on("\t\n").join(original.log()) : ""));
////            else
//        t.log(tag);
//
//
//        return t;
//    }

//    @NotNull
//    public VarIntroduction each(@NotNull NAR nar) {
//        nar.onTask(this::accept);
//        return this;
//    }


//    public static final class VarIntroducedTask extends GeneratedTask {
//
//        @Nullable private volatile transient Task original;
//
//        public VarIntroducedTask(@NotNull Compound c, @NotNull Task original) {
//            super(c, original.punc(), original.truth());
//            this.original = original;
//
//        }
//
//        /** if input was successful, crosslink to the original */
//        @Override public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, @NotNull NAR nar) {
//            //if the original was deleted already before this feedback was applied, delete this task too
//            @Nullable Task orig = this.original;
//
//            if (orig == null || orig.isDeleted()) {
//                delete();
//                return;
//            }
//
//            if (deltaConfidence==deltaConfidence /* wasn't deleted, even for questions */) {
//                @Nullable Concept thisConcept = concept(nar);
//                Concept other = thisConcept.crossLink(this, orig, isBeliefOrGoal() ? conf() : qua(), nar);
//
//                if (original instanceof Abbreviation.AbbreviationTask) {
//                    //share abbreviation meta; prevents some cases of recursive abbreviation
//                    thisConcept.put(Abbreviation.class, other.get(Abbreviation.class));
//                }
//            }
//
//            super.feedback(delta, deltaConfidence, deltaSatisfaction, nar);
//        }
//
//        @Override
//        public boolean delete() {
//            if (super.delete()) {
//                if (!Param.DEBUG) original = null; //unlink
//                return true;
//            }
//            return false;
//        }
//
////        @Override
////        public @NotNull Task log(@Nullable List historyToCopy) {
////            return original!= null ? nuloriginal.log(historyToCopy) : null;
////        }
//    }
}
