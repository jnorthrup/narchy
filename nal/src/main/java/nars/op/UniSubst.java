package nars.op;

import nars.$;
import nars.Op;
import nars.derive.Derivation;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.util.Image;

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
public class UniSubst extends Functor implements Functor.InlineFunctor {


    public final static Term STRICT = Atomic.the("strict");
    public final static Term INDEP_VAR = $.quote("$");
    public final static Term DEP_VAR = $.quote("#");
    public static final Atom unisubst = (Atom) Atomic.the("unisubst");

    public final MySubUnify u; //TODO find what state is being held that contaminated repeated use of this

    private final Derivation parent;

    public UniSubst(Derivation parent) {
        super(unisubst);
        this.parent = parent;
        u = new MySubUnify();
    }

    @Override
    public Term apply(Evaluation e, /*@NotNull*/ Subterms a) {

//        if (a.hasAny(Op.VAR_PATTERN))
//            return Null; //result not fully unified; though there may be a chance it could be eliminated here without even having an assigned value

        boolean strict = false;
        int var = Op.VAR_DEP.bit | Op.VAR_INDEP.bit;
        //@Nullable Op op = null;


        int pp = a.subs();
        for (int p = 3; p < pp; p++) {
            Term ai = a.sub(p);
            if (ai.equals(STRICT))
                strict = true;
            else if (ai.equals(INDEP_VAR)) {
                //HACK is this ignored?
            } else if (ai.equals(DEP_VAR)) {
                var = VAR_DEP.bit;
            } else
                throw new UnsupportedOperationException("unrecognized parameter: " + ai);
        }

        /** term being transformed if x unifies with y */
        Term c = a.sub(0);


        Term x = a.sub(1);


        Term y = a.sub(2);


        if (x.equals(y)) {
            return strict ? Null : c;
        }

        Term output;
//        if (c.equals(x)) {
//
//            output = y;
//
//        } else {

        boolean tryUnify = (x.hasAny(var) || y.hasAny(var));

        if (tryUnify) {
            int ttl = Math.max(1, parent.nar.subUnifyTTLMax.intValue() - 1);
            x = Image.imageNormalize(x);
            y = Image.imageNormalize(y);
            output = u.reset(var, strict).tryMatch(c, x, y, ttl);
            parent.use(1 + ttl - u.ttl);
        } else {
            output = null;
        }

//        }

        return (output == null || (strict && c.equals(output))) ? Null : output;
    }

    public final class MySubUnify extends SubUnify {

        private boolean strict;

        MySubUnify() {
            super(null, Op.Variable);
        }

        private MySubUnify reset(int varBits, boolean strict) {
            this.random = parent.random;
            this.strict = strict;
            setVarBits(varBits);
            this.result = this.transformed = null;
            clear();
            return this;
        }


        @Override
        protected boolean tryMatch(Term result) {

            if (!strict || (!result.equals(transformed) && !result.normalize().equals(transformed.normalize()))) { //dont actually normalize it ; could destroy common variables since they arent Anon and in the derivation's Anon map

                //this.xy.forEach(parent.retransform::put);
                this.xy.forEach(parent.xy::set);

                return true;
            }
            return false;
        }
    }




}
