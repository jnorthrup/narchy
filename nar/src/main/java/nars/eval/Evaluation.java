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
import nars.term.atom.theBool;
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
import static nars.term.atom.theBool.*;

/**
 * see: https://www.swi-prolog.org/pldoc/man?section=preddesc
 */
public class Evaluation extends Termerator {

	private final Predicate<Term> each;

	public static void eval(Term x, Predicate<Term> each, Function<Atom, Functor> resolver) {
		if (evalable(x))
			new Evaluation(each).evalTry((Compound) x, new Evaluator(resolver), false);
	}

	private static void eval(Term x, boolean includeTrues, boolean includeFalses, Predicate<Term> each, Function<Atom, Functor> resolver) {

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

		var before = v.size();

		return termutes.size() == 1 ? termute1(e, y, before) : termuteN(e, y, before);
	}

	protected static Random random() {
		return ThreadLocalRandom.current();
	}

	private boolean termuteN(Evaluator e, Term y, int start) {

		var tt = termutes.toArrayRecycled(Iterable[]::new);
		for (int i = 0, ttLength = tt.length; i < ttLength; i++)
			tt[i] = shuffle(tt[i]);
		ArrayUtil.shuffle(tt, random());

		CartesianIterator<Predicate<Termerator>> ci = new CartesianIterator<>(Predicate[]::new, tt);

		termutes.clear();

		Set<Term> tried = null;
		while (ci.hasNext()) {
			var appliedAll = Arrays.stream(ci.next()).allMatch(cc -> cc.test(this));

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
		var t = shuffle(termutes.remove(0));
		Set<Term> tried = null;
		for (var tt : t) {
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
		var z = y.replace(subs);
		return y.equals(z) || !tried.add(z) || eval(e, z);  //CUT
	}


	public boolean eval(Evaluator e, Term x) {
		return eval(e, x, x instanceof Compound ? e.clauses((Compound) x, this) : null);
	}

	/**
	 * fails fast if no known functors apply
	 */
	protected boolean evalTry(Compound x, Evaluator e, boolean includeOriginal) {
		@Nullable var c = e.clauses(x, this);

		if ((c == null || c.isEmpty()) && (termutes == null || termutes.isEmpty())) {
			if (includeOriginal)
				each.test(x);
			return true;
		}

		return eval(e, x, c);
	}

	private boolean eval(Evaluator e, Term x, @Nullable ArrayHashSet<Term> clauses) {

		var y = x;

		if (clauses != null && !clauses.isEmpty()) {

			Term prev;
//			int mutStart;
			//iterate until stable
			main:
			do {
				prev = y;
                for (var a : clauses) {

                    //run the functor resolver for any new functor terms which may have appeared

                    Term aFunc_Pre = Functor.func(a);
                    if (!(aFunc_Pre instanceof Functor)) {
                        //try resolving
						var aa = e.apply(a);
                        if (aa == a) {
                            //no change. no such functor Exception?
                        } else {
                            a = aa;
                        }
                    }

					var vStart = now();

					var b = ((Functor) a.sub(1)).apply(this, a.sub(0).subterms());

					var newSubsts = now() != vStart;

                    if (b instanceof theBool) {
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

					var y0 = y;

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

		if (y instanceof theBool)
			return each.test(bool(x, (theBool) y)); //Terminal Result

		//if termutators, collect all results. otherwise 'cur' is the only result to return
		//Transformed Result (possibly same)
		return termutators() > 0 ? termute(e, y) : each.test(y);
	}

	protected Term bool(Term x, theBool b) {
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
		var y = new Term[1];
		Evaluation.eval(x, true, true, (what) -> {
			if (what instanceof theBool) {
				if (y[0] != null)
					return true; //ignore and continue try to find a non-bool solution
			}
			y[0] = what;
			return false;
		}, axioms);
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

		var ee = new UnifiedSet<Term>(0);

		Evaluation.eval(x, includeTrues, includeFalses, t -> {
			if (t != Null)
				ee.add(t);
			return true;
		}, resolver);

		return ee.isEmpty() ? Set.of() : ee;
		//java.util.Set.of($.func(Inperience.wonder, x))
	}
	public static Set<Term> eval(Term x, boolean includeTrues, boolean includeFalses, Evaluator e) {

		var ee = new UnifiedSet(0, 0.5f);

		Evaluation.eval(x, includeTrues, includeFalses, e, t -> {
			if (t != Null)
				ee.add(t);
			return true;
		});

		return ee.isEmpty() ? Set.of() : ee;
	}

}
