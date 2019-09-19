package nars.derive.rule;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import jcog.TODO;
import jcog.util.ArrayUtil;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.PreDerivation;
import nars.derive.action.PatternPremiseAction;
import nars.derive.op.*;
import nars.derive.util.PremiseTermAccessor;
import nars.subterm.Subterms;
import nars.subterm.util.SubtermCondition;
import nars.term.Variable;
import nars.term.*;
import nars.term.control.PREDICATE;
import nars.term.control.TermMatch;
import nars.term.util.TermException;
import nars.term.var.UnnormalizedVariable;
import nars.term.var.VarPattern;
import nars.unify.constraint.*;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.derive.premise.PatternTermBuilder.patternify;
import static nars.subterm.util.SubtermCondition.*;
import static nars.term.atom.Bool.Null;
import static nars.term.control.AbstractTermMatchPred.cost;
import static nars.term.control.PREDICATE.sortByCostIncreasing;
import static nars.unify.constraint.RelationConstraint.*;

/** base class providing all facilities for constructing a trie conditionally filtered rule,
 * but oblivious to the action implementation */
public abstract class ConditionalPremiseRuleBuilder extends PremiseRuleBuilder {

	public static final VarPattern TheTask = $.varPattern(1);
	static final Variable ANY_TERM =
		new UnnormalizedVariable(Op.VAR_PATTERN, "_");

	public Term taskPattern = ANY_TERM, beliefPattern = ANY_TERM;


	protected transient UnifyConstraint<Derivation>[] CONSTRAINTS;

	/**
	 * conditions which can be tested before unification
	 */

	public final MutableSet<UnifyConstraint<Derivation>> constraints = new UnifiedSet<>();


	@Deprecated transient protected boolean forceDouble = false;

	public void taskPattern(String x)  {
		try {
			taskPattern(Narsese.term(x, false));
		} catch (Narsese.NarseseException e) {
			throw new RuntimeException("invalid task pattern", e);
		}
	}

	public void beliefPattern(String x)  {
		try {
			beliefPattern(Narsese.term(x, false));
		} catch (Narsese.NarseseException e) {
			throw new RuntimeException("invalid belief pattern", e);
		}
	}

	/** single premise, matching anything */
	public final void single() {
		single(TheTask);
	}

	public final void single(Term taskPattern) {
		single(taskPattern, null);
	}

	/** single premise */
	public final void single(Term taskPattern, @Nullable Term beliefPattern) {
		taskPattern(taskPattern);
		if (beliefPattern!=null)
			beliefPattern(beliefPattern);
		else
			this.beliefPattern = this.taskPattern;

		taskPunc(true,true,true,true,false);
		hasBelief(false);

	}

//	/** double premise */
//	public final void doubl(Term taskPattern, Term beliefPattern) {
//		taskPattern(taskPattern);
//		beliefPattern(beliefPattern);
//	}

	public void taskPattern(Term x) {

		this.taskPattern = patternify(x);

		if (taskPattern instanceof Neg)
			throw new TermException("task pattern can never be NEG", taskPattern);
		if (taskPattern.op() != VAR_PATTERN && !taskPattern.op().taskable)
			throw new TermException("task pattern is not taskable", taskPattern);
	}

	public void beliefPattern(Term x) {

		this.beliefPattern = patternify(x);

		if (beliefPattern instanceof Neg)
			throw new TermException("belief pattern can never be NEG", beliefPattern);
//		if (beliefPattern.op() == Op.ATOM)
//			throw new TermException("belief target must contain no atoms", beliefPattern);
	}

	public void taskPunc(boolean belief, boolean goal, boolean question, boolean quest) {
		taskPunc(belief, goal, question, quest, false);
	}
	public void taskPunc(byte... puncs) {
		if (puncs==null || puncs.length == 0)
			return; //no filtering

		taskPunc(
			ArrayUtil.indexOf(puncs, BELIEF)!=-1,
			ArrayUtil.indexOf(puncs, GOAL)!=-1,
			ArrayUtil.indexOf(puncs, QUESTION)!=-1,
			ArrayUtil.indexOf(puncs, QUEST)!=-1
		);
	}


	public void taskPunc(boolean belief, boolean goal, boolean question, boolean quest, boolean command) {
		byte accepts = PuncMap.TRUE;
		pre.add(new PuncMap(belief ? accepts : 0, goal ? accepts : 0, question ? accepts : 0, quest ? accepts : 0, command ? accepts : 0));
	}

	/** adds a condition opcode
	 *  interpreted from the Term according to Meta NAL semantics
	 */
	public void cond(Term o) {

		boolean negated = o instanceof Neg;
		if (negated)
			o = o.unneg();

		Term name = Functor.func(o);
		if (name == Null)
			throw new TermException("invalid " + getClass().getSimpleName() + " precondition", o);

		String pred = name.toString();
		Subterms args = Functor.args(o);
		int an = args.subs();
		Term X = an > 0 ? args.sub(0) : null;
		Term Y = an > 1 ? args.sub(1) : null;

		nars.term.Variable XX = X instanceof Variable ? (Variable) X : null;
		Variable YY = Y instanceof Variable ? (Variable) Y : null;

		/** variables sorted */
		Variable Xv, Yv;
		if (XX==null || YY ==null) {
			Xv = Yv = null;
		} else if (XX.compareTo(YY) <= 0) {
			Xv = XX;
			Yv = YY;
		} else {
			Xv = YY;
			Yv = XX;
		}

		boolean[] negationApplied = new boolean[1];

		cond(o, negated, negationApplied, pred, X, Y, XX, YY, Xv, Yv);

		if (negationApplied[0] != negated)
			throw new RuntimeException("unhandled negation: " + o);

	}

	protected void cond(Term o, boolean negated, boolean[] _negationApplied, String pred, Term x, Term y, Variable Xv, Variable Yv, Variable Xvs, Variable Yvs) {

		boolean negationApplied = _negationApplied[0]; //safety check to make sure semantic of negation was applied by the handler

		switch (pred) {


			case "neq":
				if (Yvs!=null)
					neq(Xvs, Yvs);
				else
					neq(Xv, y); //???
				break;

			case "neqPN":
				constraints.add(new NotEqualPosNegConstraint(Xvs, Yvs));
				break;

			case "neqRoot":
				neqRoot(Xvs, Yvs);
				break;

			case "subCountEqual":
				constraints.add(new NotEqualConstraint.SubCountEqual(Xvs, Yvs));
				break;

			case "eqPN":
				constraints.add((UnifyConstraint)(new EqualPosOrNeg(Xvs, Yvs).negIf(negated)));
				if (negated) negationApplied = true;
				break;

			case "eqNeg":
				//TODO special predicate: either (but not both) is Neg
				neq(Xvs, Yvs);
				constraints.add(new EqualNegConstraint(Xvs, Yvs));
				break;


			case "neqRCom":
				neq(Xvs, Yvs);
				constraints.add(new NotEqualConstraint.NotEqualAndNotRecursiveSubtermOf(Xvs, Yvs));
				break;

			case "setsIntersect":
				constraints.add(new NotEqualConstraint.SetsIntersect(Xvs, Yvs));
				break;

			case "notSetsOrDifferentSets": {
				constraints.add(new NotEqualConstraint.NotSetsOrDifferentSets(Xvs, Yvs));
				break;
			}

			case "subOf":
			case "subOfPN": {

				SubtermCondition mode;

//                    if (pred.startsWith("sub"))

				if (y instanceof Neg) {
					Yv = (Variable) (y = y.unneg());
					mode = SubtermNeg;
				} else
					mode = Subterm;
//                    else {
//
//                        if (Y instanceof Neg) {
//                            YY = (Variable) (Y = Y.unneg());
//                            mode = SubsectNeg;
//                        } else
//                            mode = Subsect;
//
//                        if (!negated)
//                            is(XX, Op.CONJ);
//                        else
//                            throw new TODO();
//
//                    }

				if (!negated) {

					neq(Xv, y);
					if (y instanceof Variable)
						bigger(Xv, Yv);
				}

				int polarity;
				if (pred.endsWith("PN")) {
					polarity = 0;
					assert (y.op() != NEG);
				} else {
					polarity = 1; //negation already handled
				}

				if (y instanceof Variable) {

					SubOfConstraint c = new SubOfConstraint(Xv, (Variable) y, mode, polarity);
					constraints.add((UnifyConstraint)(c.negIf(negated)));
				} else {
					if (polarity == 0)
						throw new TODO(); //TODO contains(Y) || contains(--Y)
					match(Xv, new TermMatcher.Contains(y), !negated);
				}

				if (negated)
					negationApplied = true;
				break;
			}


			case "in":
				neq(Xv, y.unneg());
				bigger(Xvs,Yvs);
				constraints.add(new SubOfConstraint<>(Xv, ((Variable) y.unneg()), Recursive, y instanceof Neg ? -1 : +1));
				break;

			case "hasBelief":
				//TODO test negated
				hasBelief(!negated);  negationApplied = negated;
				break;

			case "conjParallel":
			case "conjSequence":
				if (!negated) is(x, CONJ);
				match(x, pred.equals("conjSequence") ? TermMatcher.ConjSequence.the : TermMatcher.ConjParallel.the, !negated);
				if (negated) negationApplied = true;
				break;

			case "eventOf":
			case "eventOfNeg": {

				boolean yNeg = pred.endsWith("Neg");

				if (y instanceof Neg) {
					y = y.unneg();
					Yv = (Variable) y;
					yNeg = !yNeg;
				}

				constraints.add((UnifyConstraint)(new SubOfConstraint(Xv, Yv, Event, yNeg ? -1 : +1).negIf(negated)));

				if (!negated) {
					bigger(Xv, Yv);
					is(Xv, CONJ);
//                        if (yNeg &&
//                                (YY.equals(taskPattern) ||
//                                        (YY.equals(beliefPattern) ||
//                                                (XX.containsRecursively(YY) && !XX.containsRecursively(YY.neg())
//                                                )))) {
//                            hasAny(XX, NEG); //taskPattern and beliefPattern themselves will always be unneg so it is safe to expect a negation
//                        }
					eventable(Yv);
				}

				if (negated) {
					negationApplied = true;
				}
				break;
			}

//                case "eventFirstOf":
////                case "eventFirstOfNeg":
//                case "eventLastOf":
////                case "eventLastOfNeg":
//                {
//                    match(X, new TermMatcher.Is(CONJ));
//                    neq(constraints, XX, YY);
//                    boolean yNeg = pred.contains("Neg");
//                    constraints.add(new SubOfConstraint(XX, YY, pred.contains("First") ? EventFirst : EventLast, yNeg ? -1 : +1).negIf(negated));
//
//                    eventable(YY);
//
//                    if (negated) {
//                        negationApplied = true;
//                    }
//                    break;
//                }

			case "eventOfPN":
				is(x, CONJ);
				neq(Xv, Yv);
				bigger(Xv, Yv);

				eventable(Yv);

				constraints.add(new SubOfConstraint(Xv, Yv, Event, 0));
				break;

//                /** one or more events contained */
//                case "eventsOf":
//                    neq(constraints, XX, YY);
//                    match(X, new TermMatch.Is(CONJ));
//
//                    constraints.addAt(new SubOfConstraint(XX, YY, EventsAny, 1));
//                    break;

			//case "eventsOfNeg":

			case "eventCommon":
				is(x, CONJ);
				is(y, CONJ);
				constraints.add(new CommonSubEventConstraint(Xv, Yv));

				break;


			case "subsMin":
				match(x, new TermMatcher.SubsMin((short) $.intValue(y)));
				break;

			case "is": {
				int struct;
				if (y.op() == SETe) {
					struct = 0;
					for (Term yy : y.subterms()) {
						struct |= Op.the($.unquote(yy)).bit;
					}
				} else {
					//TEMPORARY
					if (y.toString().equals("\"||\"")) {
						isUnneg(x, CONJ, negated);
						if (negated) negationApplied = true;
						break;
					}
					struct = Op.the($.unquote(y)).bit;
				}
				is(x, struct, negated);
				if (negated) negationApplied = true;
				break;
			}
			case "isVar": {
				is(x, Op.Variable, negated);
				if (negated)
					negationApplied = true;
				break;
			}

			case "hasVar": {
				match(x, new TermMatcher.Has(Op.Variable, true, 2), !negated);
				if (negated)
					negationApplied = true;
				break;
			}

			case "isUnneg": {
				Op oo = Op.the($.unquote(y));
				isUnneg(x, oo, negated);
				if (negated) negationApplied = true;
				break;
			}


			case "has": {
				//hasAny
				hasAny(x, Op.the($.unquote(y)), !negated);
				if (negated) negationApplied = true;
				break;
			}


			case "task":
				if (this instanceof PatternPremiseAction) {
					//HACK ignore; handled in subclass
				} else
					throw new UnsupportedOperationException();
				break;

			default:
				throw new RuntimeException("unhandled postcondition: " + pred + " in " + this);

		}

		_negationApplied[0] = negationApplied;
	}

	public void hasBelief(boolean trueOrFalse) {
		pre.add(new SingleOrDoublePremise(!trueOrFalse));
	}

	protected void guardOpVolStruct(PremiseTermAccessor r, Term root) {
		if (root.op() == VAR_PATTERN)
			return;

		guardOpVolStruct(r, root, null);
	}

	protected void guardOpVolStruct(PremiseTermAccessor r, Term root, @Nullable ByteArrayList p) {
		Term t;
		int depth;
		byte[] pp;
		if (p == null) {
			pp = ArrayUtil.EMPTY_BYTE_ARRAY;
			t = root;
			depth = 0;
		} else {
			pp = p.toByteArray();
			t = root.subPath(pp);
			depth = pp.length;
		}

		Op o = t.op();
		if (o == Op.VAR_PATTERN)
			return;

		Function<PreDerivation, Term> rr = depth == 0 ? r : r.path(pp);

		pre.add(new TermMatch<>(TermMatcher.matchTerm(t, depth), rr, depth));

		int n = t.subs();
		if (!o.commutative || (n == 1 && o!=CONJ)) {
			if (p == null)
				p = new ByteArrayList(8);

			for (byte i = 0; i < n; i++) {
				p.add(i);
				guardOpVolStruct(r, root, p);
				p.popByte();
			}
		}
	}

	public void neq(Variable x, Term y) {

		if (y instanceof Neg && y.unneg() instanceof Variable) {
			constraints.add(new EqualNegConstraint(x, (Variable) (y.unneg())).neg());
		} else if (y instanceof Variable) {
			constraints.add(new NotEqualConstraint(x, (Variable) y));
		} else {
			match(x, new TermMatcher.Equals(y), false);
		}
	}

	public void neqRoot(Variable x, Variable y) {
		constraints.add(new NotEqualConstraint.NotEqualRootConstraint(x, y));
	}

	public void bigger(Variable x, Variable y) {
		constraints.add(new VolumeCompare(x, y, false, +1 /* X > Y */));
	}

	public void biggerIffConstant(Variable x, Variable y) {
		//TODO dangerous, check before using
		constraints.add(new VolumeCompare(x, y, true, +1));
	}

	public void match(boolean taskOrBelief, byte[] path, TermMatcher m) {
		match(taskOrBelief, path, m, true);
	}

	public void match(boolean taskOrBelief, byte[] path, TermMatcher m, boolean trueOrFalse) {
		pre.add(new TermMatch(m, trueOrFalse, TaskOrBelief(taskOrBelief).path(path), cost(path.length)));
	}

	protected void match(Term x,
					   BiConsumer<byte[], byte[]> preDerivationExactFilter,
					   BiConsumer<Boolean, Boolean> preDerivationSuperFilter /* task,belief*/
	) {

		//boolean isTask = taskPattern.equals(x);
		//boolean isBelief = beliefPattern.equals(x);
		byte[] pt = //(isTask || !taskPattern.ORrecurse(s -> s instanceof Ellipsislike)) ?
			Terms.pathConstant(taskPattern, x);
		byte[] pb = //(isBelief || !beliefPattern.ORrecurse(s -> s instanceof Ellipsislike)) ?
			Terms.pathConstant(beliefPattern, x);// : null;
		if (pt != null || pb != null) {


			if ((pt != null) && (pb != null)) {
				//only need to test one. use shortest path
				if (pb.length < pt.length)
					pt = null;
				else
					pb = null;
			}

			preDerivationExactFilter.accept(pt, pb);
		} else {
			//assert(!isTask && !isBelief);
			//non-exact filter
			boolean inTask = taskPattern.containsRecursively(x);
			boolean inBelief = beliefPattern.containsRecursively(x);

			if (inTask && inBelief) {
				//only need to test one. use smallest volume
				if (beliefPattern.volume() < taskPattern.volume())
					inTask = false;
				else
					inBelief = false;
			}
			preDerivationSuperFilter.accept(inTask, inBelief);
		}
	}

	public void match(Term x, TermMatcher m) {
		match(x, m, true);
	}

	public void match(Term x, TermMatcher m, boolean trueOrFalse) {
		match(x, (pathInTask, pathInBelief) -> {


				if (pathInTask != null)
					match(true, pathInTask, m, trueOrFalse);
				if (pathInBelief != null)
					match(false, pathInBelief, m, trueOrFalse);

			}, (inTask, inBelief) -> {

//                    if (trueOrFalse /*|| m instanceof TermMatch.TermMatchEliminatesFalseSuper*/) { //positive only (absence of evidence / evidence of absence)
//                        if (inTask)
//                            matchSuper(true, m, trueOrFalse);
//                        if (inBelief)
//                            matchSuper(false, m, trueOrFalse);
//                    }

				constraints.add(m.constraint((Variable) x, trueOrFalse));
			}
		);
	}

	private static PremiseTermAccessor TaskOrBelief(boolean taskOrBelief) {
		return taskOrBelief ? PremiseRuleBuilder.TaskTerm : PremiseRuleBuilder.BeliefTerm;
	}

	public static Term pathTerm(@Nullable byte[] path) {
		return path == null ? $.the(-1) /* null */ : $.p(path);
	}

	/** cost-sorted array of constraint enable procedures, bundled by common term via CompoundConstraint */
	private UnifyConstraint<Derivation>[] constraints(MutableSet<UnifyConstraint<Derivation>> constraints) {
		return UnifyConstraint.the(constraints.stream().<UnifyConstraint<Derivation>>flatMap(c -> {
//            PREDICATE<Derivation> post = cc.postFilter();
//            if (post!=null) {
//            }

			PREDICATE<PreDerivation> cc = preFilter(c, taskPattern, beliefPattern);
			if (cc != null) {
				pre.add(cc);
				return Stream.empty();
			}
			if (c instanceof RelationConstraint) {
				RelationConstraint<Derivation> m = ((RelationConstraint<Derivation>) c).mirror();
				if (m != null) {
					//isnt possible:
//                    PREDICATE<Unify> mm = preFilter(m, taskPattern, beliefPattern);
//                    if (mm != null) {
//                        pre.add(mm);
//                        return Stream.empty();
//                    }

					return Stream.of(c, m);
				}
			}

			return Stream.of(c);
		}));

		//return theInterned(uu); //AFTER .. constraints can be added to in conclusion()
	}

	public void isUnneg(Term x, Op o, boolean negated) {
		match(x, new TermMatcher.IsUnneg(o), !negated);
	}

	private static PREDICATE<PreDerivation> preFilter(UnifyConstraint cc, Term taskPattern, Term beliefPattern) {

		Variable x = cc.x;

		if (cc instanceof NegRelationConstraint) {
			PREDICATE p = preFilter(((NegRelationConstraint) cc).r, taskPattern, beliefPattern);
			return p != null ? p.neg() : null;
		} else if (cc instanceof RelationConstraint) {

			Variable y = ((RelationConstraint) cc).y;
			byte[] xInT = Terms.pathConstant(taskPattern, x);
			byte[] xInB = Terms.pathConstant(beliefPattern, x);
			if (xInT != null || xInB != null) {
				byte[] yInT = Terms.pathConstant(taskPattern, y);
				byte[] yInB = Terms.pathConstant(beliefPattern, y);
				if ((yInT != null || yInB != null)) {
					if (xInT != null && xInB != null) {
						if (xInB.length < xInT.length) xInT = null;
						else xInB = null;
					}
					if (yInT != null && yInB != null) {
						if (yInB.length < yInT.length) yInT = null;
						else yInB = null;
					}
					return ConstraintAsPremisePredicate.the(cc, x, y, xInT, xInB, yInT, yInB);
				}
			}


		} else if (cc instanceof UnaryConstraint) {
			byte[] xInT = Terms.pathConstant(taskPattern, x);
			byte[] xInB = Terms.pathConstant(beliefPattern, x);
			if (xInT != null || xInB != null) {
				if (xInT != null && xInB != null) {
					if (xInB.length < xInT.length) xInT = null;
					else xInB = null;
				}
				return ConstraintAsPremisePredicate.the(cc, x, null, xInT, xInB, null, null);
			}

		}

		return null;
	}

	public void is(Term x, Op o) {
		is(x, o.bit);
	}
	public void isNot(Term x, Op o) {
		isNot(x, o.bit);
	}

	public void is(Term x, int struct) {
		is(x, struct, false);
	}
	public void isNot(Term x, int struct) {
		is(x, struct, true);
	}

	//new CustomConcurrentHashMap<>(STRONG, EQUALS, WEAK, EQUALS, 1024);

	public void is(Term x, int struct, boolean negated) {
		match(x, new TermMatcher.Is(struct), !negated);
		//constraints.add(new TermMatcher.Is(struct).constraint((Variable)x, !negated));
	}

	public void eventable(Variable YY) {
		constraints.add(TermMatcher.Eventable.the.constraint(YY, true));
	}

	public void hasAny(Term x, Op o) {
		hasAny(x, o, true);
	}

	public void hasAny(Term x, Op o, boolean trueOrFalse) {
		if (o == INT)
			throw new RuntimeException("Premise Key memoizing erases Integers"); //HACK TODO

		match(x, new TermMatcher.Has(o, true), trueOrFalse);
	}

	@Override protected PREDICATE<PreDerivation>[] conditions() {
		commit();



		/** constraints must be computed BEFORE preconditions as some constraints may be transformed into preconditions */
		CONSTRAINTS = constraints(constraints);

		int rules = pre.size();
		PREDICATE[] PRE = pre.toArray(new PREDICATE[rules]);

		if (rules > 1) {
			Arrays.sort(PRE, 0, rules, sortByCostIncreasing);
			assert (PRE[0].cost() <= PRE[rules - 2].cost()); //increasing cost
		}

		//not working yet:
//        for (int i = 0, preLength = PRE.length; i < preLength; i++) {
//            PRE[i] = INDEX.intern(PRE[i]);
//        }

		return PRE;
	}

	protected void commit() {


		CommutativeConstantPreFilter.tryFilter(true, taskPattern, beliefPattern, pre);
		guardOpVolStruct(TaskTerm, taskPattern);

		CommutativeConstantPreFilter.tryFilter(false, taskPattern, beliefPattern, pre);
		guardOpVolStruct(BeliefTerm, beliefPattern);

		if (taskPattern.equals(beliefPattern)) {
			pre.add(TaskBeliefTermsEqual.the);
		}


	}
}
