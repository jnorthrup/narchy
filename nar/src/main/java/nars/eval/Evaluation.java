package nars.eval;

import jcog.data.iterator.CartesianIterator;
import jcog.data.set.ArrayHashSet;
import jcog.math.ShuffledPermutations;
import jcog.util.ArrayUtil;
import nars.NAR;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import nars.term.buffer.Termerator;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.term.atom.Bool.*;

/**
 * see: https://www.swi-prolog.org/pldoc/man?section=preddesc
 */
public class Evaluation extends Termerator {

	private final Predicate<Term> each;

	public static void eval(Term x, Function<Atom, Functor> resolver, Predicate<Term> each) {
		if (evalable(x))
			new Evaluation(each).evalTry((Compound) x, new Evaluator(resolver), false);
	}

	private static void eval(Term x, boolean includeTrues, boolean includeFalses, Function<Atom, Functor> resolver, Predicate<Term> each) {

		if (evalable(x)) {
			new Evaluator(resolver).eval(each, includeTrues, includeFalses, x);
		} else {
			each.test(x); //didnt need evaluating, just input
		}
	}

	protected Evaluation() {
		this.each = (Predicate<Term>) this;
	}

	Evaluation(Predicate<Term> each) {
		this.each = each;
	}

	@Deprecated
	private boolean termute(Evaluator e, Term y) {

		int before = v.size();

		if (termutes.size() == 1) {
			return termute1(e, y, before);
		} else {
			return termuteN(e, y, before);
		}
	}

	protected Random random() {
		return ThreadLocalRandom.current();
	}

	private boolean termuteN(Evaluator e, Term y, int start) {

		Iterable<Predicate<Termerator>>[] tt = termutes.toArrayRecycled(Iterable[]::new);
		for (int i = 0, ttLength = tt.length; i < ttLength; i++)
			tt[i] = shuffle(tt[i]);
		ArrayUtil.shuffle(tt, random());

		CartesianIterator<Predicate<Termerator>> ci = new CartesianIterator(Predicate[]::new, tt);

		termutes.clear();

		nextProduct:
		while (ci.hasNext()) {

			v.revert(start);

			Predicate/*<VersionMap<Term,Term>>*/[] c = ci.next();

			for (Predicate<Termerator> cc : c) {
//                if (cc == null)
//                    break; //null target list
				if (!cc.test(this))
					continue nextProduct;
			}

			//all components applied successfully

			if (!recurse(e, y))
				return false;

		}
		return true;
	}


	private boolean termute1(Evaluator e, Term y, int start) {
		Iterable<Predicate<Termerator>> t = shuffle(termutes.remove(0));
		for (Predicate<Termerator> tt : t) {
			if (tt.test(this)) {
				if (!recurse(e, y))
					return false;
			}
			v.revert(start);
		}
		return true;
	}

	private Iterable<Predicate<Termerator>> shuffle(Iterable<Predicate<Termerator>> t) {
		return ShuffledPermutations.shuffle(t, random());
	}

	private boolean recurse(Evaluator e, Term y) {
		Term z = y.replace(subs);
		return y.equals(z) || eval(e, z);  //CUT
	}


	public boolean eval(Evaluator e, final Term x) {
		return eval(e, x, x instanceof Compound ? e.clauses((Compound) x, this) : null);
	}

	/**
	 * fails fast if no known functors apply
	 */
	protected boolean evalTry(Compound x, Evaluator e, boolean includeOriginal) {
		@Nullable ArrayHashSet<Term> c = e.clauses(x, this);

		if ((c == null || c.isEmpty()) && (termutes == null || termutes.isEmpty())) {
			if (includeOriginal)
				each.test(x);
			return true;
		}

		return eval(e, x, c);
	}

	private boolean eval(Evaluator e, final Term x, @Nullable ArrayHashSet<Term> clauses) {

		Term y = x;

		if (clauses != null && !clauses.isEmpty()) {

			Term prev;
			int mutStart;
			//iterate until stable
			main:
			do {
				prev = y;
				mutStart = termutators();
				Iterator<Term> ii = clauses.iterator();
				while (ii.hasNext()) {

					Term a = ii.next();

					//run the functor resolver for any new functor terms which may have appeared

					Term aFunc_Pre = Functor.func(a);
					if (!(aFunc_Pre instanceof Functor)) {
						//try resolving
						Term aa = e.apply(a);
						if (aa == a) {
							//no change. no such functor
						} else {
							a = aa;
						}
					}


					final int vStart = now();

					Functor aFunc = (Functor) a.sub(1);
					Subterms aArgs = a.sub(0).subterms();
					Term b = aFunc.apply(this, aArgs);

					boolean newSubs = now() != vStart;

					if (b instanceof Bool && !newSubs) {
						if (b == True) {
							//continue
						} else if (b == False) {
							y = x.equals(y) ? False : a.neg();
							break main;
						} else {
							y = Null;
							break main;
						}
					}

					Term y0 = y;

					if (b != null && b != a) {
						y = y.replace(a, b); //TODO replace only the first?
						if (!(y instanceof Compound))
							break main;
					}

					if (newSubs) {
						y = y.replace(subs);
						if (!(y instanceof Compound))
							break main;
					}

					if (!y.equals(y0)) {
						//re-clausify
                        //TODO this may only be helpful if y changes significantly (ex: entire sub-trees get removed, this can eliminate useless evals)
						clauses.clear();
						clauses = e.clauseFind((Compound) y, clauses);
						if (clauses == null || clauses.isEmpty())
							break main; //done

                        continue main;
					}


				}

			} while (!y.equals(prev));
		}

		assert (y != null);

		if (y instanceof Bool)
			return each.test(bool(x, (Bool) y)); //Terminal Result

		//if termutators, collect all results. otherwise 'cur' is the only result to return
		else if (termutators() > 0)
			return termute(e, y);
		else
			return each.test(y); //Transformed Result (possibly same)
	}

	protected Term bool(Term x, Bool b) {
		if (b == True) {
			return boolTrue(x);
		} else if (b == False) {
			return boolFalse(x);
		} else {
			return Null;
		}
	}

	Term boolTrue(Term x) {
		return x;
	}

	Term boolFalse(Term x) {
		return x.neg();
	}

	private int termutators() {
		return termutes != null ? termutes.size() : 0;
	}

	private int now() {
		return v != null ? v.size() : 0;
	}


	public static Term solveFirst(Term x, Function<Atom, Functor> axioms) {
		Term[] y = new Term[1];
		Evaluation.eval(x, true, true, axioms, (what) -> {
			if (what instanceof Bool) {
				if (y[0] != null)
					return true; //ignore and continue try to find a non-bool solution
			}
			y[0] = what;
			return false;
		});
		return y[0];
	}


	@Deprecated
	public static Set<Term> eval(Compound x, NAR n) {
		return eval(x, true, false, n);
	}

	@Deprecated
	public static Set<Term> eval(Compound x, boolean includeTrues, boolean includeFalses, NAR n) {
		return eval(x, includeTrues, includeFalses, n::axioms);
	}

	/**
	 * gathers results from one truth setAt, ex: +1 (true)
	 * TODO add limit
	 */
	public static Set<Term> eval(Term x, boolean includeTrues, boolean includeFalses, Function<Atom, Functor> resolver) {

		UnifiedSet ee = new UnifiedSet(0, 0.5f);

		Evaluation.eval(x, includeTrues, includeFalses, resolver, t -> {
			if (t != Null)
				ee.add(t);
			return true;
		});

		if (ee.isEmpty()) {
			//java.util.Set.of($.func(Inperience.wonder, x))
			return Set.of();
		} else
			return ee;
	}


}
