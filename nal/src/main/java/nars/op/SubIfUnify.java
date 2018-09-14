package nars.op;

import nars.$;
import nars.Op;
import nars.Param;
import nars.derive.Derivation;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.util.Image;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static nars.Op.VAR_DEP;
import static nars.term.atom.Bool.Null;

/**
 * substituteIfUnifies....(term, varFrom, varTo)
 * <p>
 * <patham9_> for A ==> B, D it is valid to unify both dependent and independent ones ( unify(A,D) )
 * <patham9_> same for B ==> A, D     ( unify(A,D) )
 * <patham9_> same for <=> and all temporal variations
 * <patham9_> is this the general solution you are searching for?
 * <patham9_> for (&&,...) there are only dep-vars anyway so there its easy
 * <sseehh> i found a solution for one which would be to make it do both dep and indep var
 * <sseehh> like you said
 * <sseehh> which im working now, making it accept either
 * <patham9_> ah I see
 * <sseehh> i dont know if the others are solved by this or not
 * <patham9_> we also allow both in 2.0.x here
 * <sseehh> in all cases?
 * <sseehh> no this isnt the general solution i imagined would be necessary. it may need just special cases always, i dunno
 * <sseehh> my dep/indep introducer is general because it isnt built from any specific rule but operates on any term
 * <patham9_> the cases I mentioned above, are there cases that are not captured here?
 * <sseehh> i dont know i have to look at them all
 * <sseehh> i jus tknow that currently each one is either dep or indep
 * <sseehh> and im making the first one which is both
 * <sseehh> and if this works then ill see if the others benefit from it
 * <patham9_> yes it should allow both here anyway
 * <sseehh> i hope its the case that they all can be either
 * <patham9_> unify("$") also allows unify("#") but not vice versa
 * <patham9_> thats what we also had in 1.7.0
 * <sseehh> so you're syaing anywhree i have substituteIfUnifiesDep i can not make both, but anywhere that is substituteIfUnifiesIndep i can?
 * <sseehh> or that they both can
 * <patham9_> yes thats what I'm saying
 * <sseehh> k
 * <patham9_> substituteIfUnifiesIndep  is always used on conditional rules like the ones above, this is why unifying dep here is also fine here
 * <patham9_> for substituteIfUnifiesDep there has to be a dependent variable that was unified, else the rule application leads to a redundant and weaker result
 * <patham9_> imagine this case: (&&,<tim --> cat>,<#1 --> animal>).   <tim --> cat>.   would lead to <#1 --> animal>  Truth:AnonymousAnalogy altough no anonymous analogy was attempted here
 * <patham9_> which itself is weaker than:  <#1 --> animal>  as it would have come from deduction rule alone here already
 * <sseehh> i think this is why i tried something like subtituteOnlyIfUnifiesDep but it probably needed this condition instead
 * <sseehh> but i had since removed that
 * <patham9_> I see
 * <patham9_> yes dep-var unification needs a dep-var that was unified. while the cases where ind-var unification is used, it doesnt matter if there is a variable at all
 * <sseehh> ok that clarifies it ill add your notes here as comments
 * <sseehh> coding this now, carefly
 * <sseehh> carefuly
 * <patham9_> also i can't think of a case where dep-var unification would need the ability to also unify ind-vars, if you find such a case i don't see an issue with allowing it, as long as it requires one dep-var to be unified it should work
 * <patham9_> hm not it would be wrong to allow ind-var-unification for dep-var unification, reason: (&&,<$1 --> #1> ==> <$1 --> blub>,<cat --> #1>) could derive <cat --> #1> from a more specific case such as <tim --> #1> ==> <tim --> blub>>
 * <patham9_> *no
 * <patham9_> so its really this:
 * <patham9_> allow dep-var unify on ind-var unify, but not vice versa.
 * <patham9_> and require at least one dep-var to be unified in dep-var unification.
 * <patham9_> in principle the restriction to have at least one dep-var unified could be skipped, but the additional weaker result doesn't add any value to the system
 */
public class SubIfUnify extends Functor implements Functor.InlineFunctor {


    public final static Term STRICT = Atomic.the("strict");
    public final static Term INDEP_VAR = $.quote("$");
    public final static Term DEP_VAR = $.quote("#");
    public static final Atom SubIfUnify = (Atom) Atomic.the("subIfUnifiesAny");

    private final Derivation parent;

    public SubIfUnify(Derivation parent) {
        super(SubIfUnify);
        this.parent = parent;
    }

    @Override
    public Term apply(Evaluation e, /*@NotNull*/ Subterms a) {


        boolean strict = false;
        int op = Op.VAR_DEP.bit | Op.VAR_INDEP.bit;
        //@Nullable Op op = null;


        int pp = a.subs();
        for (int p = 3; p < pp; p++) {
            Term ai = a.sub(p);
            if (ai.equals(STRICT))
                strict = true;
            else if (ai.equals(INDEP_VAR)) {
                //HACK is this ignored?
            } else if (ai.equals(DEP_VAR)) {
                op = VAR_DEP.bit;
            } else
                throw new UnsupportedOperationException("unrecognized parameter: " + ai);
        }

        /** term being transformed if x unifies with y */
        Term c = a.sub(0);


        Term x = Image.imageNormalize(a.sub(1));


        Term y = Image.imageNormalize(a.sub(2));


        if (x.equals(y)) {
            return strict ? Null : c;
        }

        Term output;
        if (c.equals(x)) {

            output = y;
        } else {

            boolean tryUnify = x.hasAny(op);

            if (!tryUnify) {
                output = null;
            } else {
                int ttl = parent.nar.subUnifyTTLMax.intValue();
                SubUnify u = new MySubUnify(op, strict);
                output = u.tryMatch(c, x, y, ttl);
                parent.use(ttl - u.ttl);
            }

            if (output == null) {

                return Null;


            }

        }

        return (strict && c.equals(output)) ? Null : output;
    }

    private class MySubUnify extends SubUnify {
        private final boolean strict;

        MySubUnify(@Nullable int varBits, boolean strict) {
            super(parent.random, varBits);
            this.strict = strict;
        }

        @Override
        protected boolean tryMatch(Term result) {
            if (!strict || !result.equals(transformed)) {


                this.xy.forEach(parent.untransform::put);


                return true;
            }
            return false;
        }
    }


    /**
     * Less powerful one-match only unification
     */
    public static class SubUnify extends Unify {

        @Nullable
        protected Term transformed;


        @Nullable
        private Term result;


        public SubUnify(Random rng, int varBits) {
            super(varBits, rng, Param.UnificationStackMax);
            symmetric = false;
        }

        /**
         * terminate after the first match
         */
        @Override
        public void tryMatch() {

            if (transformed != null) {
                Term result = transform(transformed);
                if (result != null && tryMatch(result)) {


                    this.result = result;
                    stop();


                }
            }
        }


        @Nullable
        public Term tryMatch(@Nullable Term transformed, Term x, Term y, int ttl) {
            this.transformed = transformed;
            this.result = null;
            setTTL(ttl); assert(ttl > 0);

            unify(x, y);

            return result;
        }

        protected boolean tryMatch(Term result) {
            return true;
        }

    }
}
