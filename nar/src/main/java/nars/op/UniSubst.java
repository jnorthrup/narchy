package nars.op;

import nars.$;
import nars.NAL;
import nars.Op;
import nars.derive.Derivation;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.util.TermException;
import nars.term.util.transform.InlineFunctor;
import nars.unify.UnifyTransform;

import java.util.Map;

import static nars.Op.VAR_DEP;
import static nars.Op.VAR_INDEP;
import static nars.term.atom.IdempotentBool.Null;

/**
 * substituteIfUnifies....(target, varFrom, varTo)
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
 * <sseehh> my dep/indep introducer is general because it isnt built from any specific rule but operates on any target
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
public class UniSubst extends Functor implements InlineFunctor<Evaluation> {


    /** must involve a variable substitution, deriving a new term */
    public static final Term NOVEL = Atomic.the("novel");

    public static final Term INDEP_VAR = $.INSTANCE.quote("$");
    public static final Term DEP_VAR = $.INSTANCE.quote("#");
    public static final Atom unisubst = Atomic.atom("unisubst");

    public final MyUnifyTransform u; //TODO find what state is being held that contaminated repeated use of this

    private final Derivation parent;

    public UniSubst(Derivation parent) {
        super(unisubst);
        this.parent = parent;
        u = new MyUnifyTransform();
    }

    @Override
    public Term apply(Evaluation e,  Subterms a) {

        int pp = a.subs();
        if (pp < 3)
            throw new TermException(UniSubst.class.getSimpleName() + " argument underflow", a);

        //TODO cache these in compiled unisubst instances
        boolean strict = false;
        int var = Op.VAR_DEP.bit | VAR_INDEP.bit;
        for (int p = 3; p < pp; p++) {
            Term ap = a.sub(p);
            if (ap instanceof Atom) {
                if (ap.equals(NOVEL))
                    strict = true;
                else if (ap.equals(INDEP_VAR))
                    var = VAR_INDEP.bit;
                else if (ap.equals(DEP_VAR))
                    var = VAR_DEP.bit;
                else
                    throw new UnsupportedOperationException("unrecognized parameter: " + ap);
            }
        }

        /** target being transformed if x unifies with y */

        return apply(a, var, strict);
    }

    private Term apply(Subterms a, int var, boolean strict) {
        Term c = a.sub(0);
        Term x = a.sub(1);
        Term y = a.sub(2);

        if (x.equals(y))
            return strict ? Null : c;

        boolean hasVar = x.hasAny(var) || y.hasAny(var);
        //boolean hasXternal = x.hasXternal() || y.hasXternal();
        boolean tryUnify = hasVar;// || hasXternal;

        if (!tryUnify)
            return Null;

        //prefilters:
        boolean xv = x instanceof Variable, yv = y instanceof Variable;
        Term cc;
        if (xv && !yv)
            cc = c.replace(x, y); //result can be determined by substitution
        else if (yv && !xv)
            cc = c.replace(y, x); //result can be determined by substitution
        else if (!xv && x.opID()!=y.opID())
            cc = null; //impossible TODO check if this is always the case, and whether Terms.possiblyUnifiable(x, y) hel
        else
            cc = unify(c, x, y, var, strict, NAL.derive.TTL_UNISUBST);

        return cc == null || (strict && c.equals(cc)) ? Null : cc;
    }

    private Term unify(Term c, Term x, Term y, int var, boolean strict, int subTTL) {



        u.setTTL(subTTL);

        return u.unifySubst(x, y, c, var, strict);


    }


    public final class MyUnifyTransform extends UnifyTransform {
        public MyUnifyTransform() {
            super(null);
        }

        @Override
        protected boolean accept(Term result) {
            if (super.accept(result)) {
                commitSubst();
                return true;
            }
            return false;
        }

        /** commit substitutions to the premise */
        public void commitSubst() {
            Map<Term, Term> termTermMap = parent.retransform;
            for (Map.Entry<Variable, Term> entry : this.xy.entrySet()) {
                Variable key = entry.getKey();
                Term value = entry.getValue();
                termTermMap.put(key, value);
            }
        }

    }


}
