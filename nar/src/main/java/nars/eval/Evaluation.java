package nars.eval;

import jcog.data.iterator.CartesianIterator;
import jcog.data.set.ArrayHashSet;
import jcog.math.ShuffledPermutations;
import jcog.util.ArrayUtil;
import nars.NAR;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import nars.term.buffer.Termerator;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.CONJ;
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

	private static void eval(Term x, boolean includeTrues, boolean includeFalses, Evaluator e, Predicate<Term> each) {
		if (evalable(x)) {
			e.eval(each, includeTrues, includeFalses, x);
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


	private boolean termute(Evaluator e, Term y) {

		int before = v.size();

		if (termutes.size() == 1) {
			return termute1(e, y, before);
		} else {
			return termuteN(e, y, before);
		}
	}

	protected static Random random() {
		return ThreadLocalRandom.current();
	}

	private boolean termuteN(Evaluator e, Term y, int start) {

		Iterable<Predicate<Termerator>>[] tt = termutes.toArrayRecycled(Iterable[]::new);
		for (int i = 0, ttLength = tt.length; i < ttLength; i++)
			tt[i] = shuffle(tt[i]);
		ArrayUtil.shuffle(tt, random());

		CartesianIterator<Predicate<Termerator>> ci = new CartesianIterator<>(Predicate[]::new, tt);

		termutes.clear();

		Set<Term> tried = null;
		while (ci.hasNext()) {
			boolean appliedAll = true;
			for (Predicate<Termerator> cc : ci.next()) {
				if (!cc.test(this)) {
					appliedAll = false;
					break;
				}
			}

			if (appliedAll) {
				if (tried == null) tried = new UnifiedSet<>(0);
				if (!recurse(e, y, tried))
					return false;
			}

			v.revert(start);

		}
		return true;
	}


	private boolean termute1(Evaluator e, Term y, int start) {
		Iterable<Predicate<Termerator>> t = shuffle(termutes.remove(0));
		Set<Term> tried = null;
		for (Predicate<Termerator> tt : t) {
			if (tt.test(this)) {
				if (tried == null) tried = new UnifiedSet<>(0);

				if (!recurse(e, y, tried))
					return false;
			}
			v.revert(start);
		}
		return true;
	}

	private static Iterable<Predicate<Termerator>> shuffle(Iterable<Predicate<Termerator>> t) {
		return ShuffledPermutations.shuffle(t, random());
	}

	private boolean recurse(Evaluator e, Term y, Set<Term> tried) {
		Term z = y.replace(subs);
		return y.equals(z) || !tried.add(z) || eval(e, z);  //CUT
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
//			int mutStart;
			//iterate until stable
			main:
			do {
				prev = y;
                for (Term a : clauses) {

                    //run the functor resolver for any new functor terms which may have appeared

                    Term aFunc_Pre = Functor.func(a);
                    if (!(aFunc_Pre instanceof Functor)) {
                        //try resolving
                        Term aa = e.apply(a);
                        if (aa == a) {
                            //no change. no such functor Exception?
                        } else {
                            a = aa;
                        }
                    }

                    final int vStart = now();

					Term b = ((Functor) a.sub(1)).apply(this, a.sub(0).subterms());

                    boolean newSubsts = now() != vStart;

                    if (b instanceof Bool) {
						if (b == True) {
							if (!newSubsts)
								continue;
						} else if (b == Null) {
							y = Null;
							break main;
						} else if (/*b == False &&*/ y.opID() == CONJ.id) {
                            y = x.equals(y) ? False : a.neg();
                            break main;
                        }
                    }

                    Term y0 = y;

                    if (b != null && b != a) {
                        y = y.replace(a, b); //TODO replace only the first?
                        if (!(y instanceof Compound))
                            break main;
                    }

                    if (newSubsts) {
                        y = y.replace(subs);
                        if (!(y instanceof Compound))
                            break main;
                    }

                    if (!y.equals(y0)) {
                        //re-clausify
                        //TODO this may only be helpful if y changes significantly (ex: entire sub-trees get removed, this can eliminate useless evals)
                        if (evalable(y)) {
                            clauses.clear();
                            clauses = e.clauseFind((Compound) y, clauses);
                        } else
                            clauses = null;

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
		if (termutators() > 0)
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

		UnifiedSet<Term> ee = new UnifiedSet<>(0);

		Evaluation.eval(x, includeTrues, includeFalses, resolver, t -> {
			if (t != Null)
				ee.add(t);
			return true;
		});

		return ee.isEmpty() ? Set.of() : ee;
		//java.util.Set.of($.func(Inperience.wonder, x))
	}
	public static Set<Term> eval(Term x, boolean includeTrues, boolean includeFalses, Evaluator e) {

		UnifiedSet ee = new UnifiedSet(0, 0.5f);

		Evaluation.eval(x, includeTrues, includeFalses, e, t -> {
			if (t != Null)
				ee.add(t);
			return true;
		});

		return ee.isEmpty() ? Set.of() : ee;
	}

}
