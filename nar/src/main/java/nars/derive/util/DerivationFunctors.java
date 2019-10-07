package nars.derive.util;

import nars.Builtin;
import nars.NAL;
import nars.NAR;
import nars.derive.Derivation;
import nars.op.Cmp;
import nars.op.Equal;
import nars.op.SetFunc;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.functor.AbstractInlineFunctor1;
import nars.term.functor.AbstractInlineFunctor2;
import nars.term.util.conj.ConjMatch;
import nars.term.util.transform.InlineFunctor;
import nars.util.var.DepIndepVarIntroduction;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;

import static nars.term.atom.Bool.Null;
import static nars.term.util.conj.ConjMatch.CONJ_WITHOUT_UNIFY;

public enum DerivationFunctors {
	;

	public static final Atom Task = Atomic.atom("task");
	public static final Atom Belief = Atomic.atom("belief");
	public static final Atom TaskTerm = Atomic.atom("taskTerm");
	public static final Atom BeliefTerm = Atomic.atom("beliefTerm");

	public static ImmutableMap<Atomic, Term> get(Derivation d) {

		Map<Atomic, Term> m = new UnifiedMap<>(32, 1f);


		for (Term s : Builtin.statik)
			if (s instanceof InlineFunctor)
				add(m, s);

		NAR nar = d.nar();


		Functor[] derivationFunctors = new Functor[]{
			d.uniSubstFunctor,
			d.substituteFunctor,
			d.polarizeTask,
			d.polarizeBelief,
			d.polarizeRandom,

			SetFunc.union,
			SetFunc.interSect,
			SetFunc.unionSect,
			SetFunc.differ,
			SetFunc.intersect,
			Equal.equal, Cmp.cmp,

			(Functor) nar.concept("unneg"),
			(Functor) nar.concept("negateEvents"),
			//(Functor) nar.concept("eventOf"),
			(Functor) nar.concept("chooseAnySubEvent"),
			(Functor) nar.concept("chooseUnifiableSubEvent"),

			(Functor) nar.concept("conjWithout"), (Functor) nar.concept("conjWithoutPN"),
			(Functor) nar.concept("without"), (Functor) nar.concept("withoutPN"),
			//(Functor) nar.concept("withoutPNRepolarized"),
//                (Functor) nar.concept("unsect"),(Functor) nar.concept("unsectPN"),(Functor) nar.concept("unsectPNRepolarized"),

			new AbstractInlineFunctor2(ConjMatch.BEFORE) {
				@Override
				protected Term apply(Term conj, Term event) {
					return ConjMatch.beforeOrAfter(conj, event, true, false, false, d.uniSubstFunctor.u, NAL.derive.TTL_CONJ_BEFORE_AFTER);
				}
			},
			new AbstractInlineFunctor2(ConjMatch.AFTER) {
				@Override
				protected Term apply(Term conj, Term event) {
					return ConjMatch.beforeOrAfter(conj, event, false, false, true, d.uniSubstFunctor.u, NAL.derive.TTL_CONJ_BEFORE_AFTER);
				}
			},

			/** similar to without() but for any (but not necessarily ALL) (possibly-recursive) CONJ sub-events. removes all instances of the positive event */
			new AbstractInlineFunctor2(CONJ_WITHOUT_UNIFY) {
				@Override
				protected Term apply(Term conj, Term event) {
					return ConjMatch.beforeOrAfter(conj, event, true, false, true, d.uniSubstFunctor.u, NAL.derive.TTL_CONJ_BEFORE_AFTER);
				}
			},

			/** applies # dep and $ indep variable introduction if possible. returns the input term otherwise  */
			Functor.f1Inline("varIntro", x -> {
				Term y = DepIndepVarIntroduction.the.apply(x, nar.random(), d.retransform);
				return y == null ? Null : y;
			}),

			new AbstractInlineFunctor1("negateRandomSubterm") {

				@Override
				protected Term apply1(Term _arg) {

					boolean neg = _arg instanceof Neg;
					Term arg = neg ? _arg.unneg() : _arg;

					if (!(arg instanceof Compound))
						return Null;

					Subterms x = arg.subterms();
					int n = x.subs();
					if (n == 0)
						return Null;

					int which = d.random.nextInt(n);
					Subterms y = x.transformSub(which, Term::neg);
					if (x != y)
						return arg.op().the(y).negIf(neg);

					return Null;
				}
			},
		};

		for (Term x : derivationFunctors) //override any statik's
			add(m, x);

		m.put(TaskTerm, TaskTerm); //to be dynamically resolved
		m.put(BeliefTerm, BeliefTerm); //to be dynamically resolved

//        MetalBloomFilter<Atomic> pre = new MetalBloomFilter<>(fastAtomHash, m.size()*2, 2);
//        for (Atomic x : m.keySet()) {
//            pre.addAt(x);
//        }

		return Maps.immutable.ofMap(m);

		//x -> pre.contains(x) ? m.get(x) : null;
	}

	private static void add(Map<Atomic, Term> m, Term x) {
		m.put((Atomic) x.term(), x);
	}


}
