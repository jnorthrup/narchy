package nars.term.compound;

import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayUnenforcedSortedSet;
import jcog.data.set.MetalTreeSet;
import jcog.data.sexpression.Pair;
import jcog.decide.Roulette;
import nars.Op;
import nars.The;
import nars.io.TermAppender;
import nars.subterm.RemappedSubterms;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.*;
import nars.term.anon.Anon;
import nars.term.util.TermTransformException;
import nars.term.util.builder.TermBuilder;
import nars.term.util.conj.Conj;
import nars.term.util.conj.ConjSeq;
import nars.term.util.conj.ConjUnify;
import nars.term.util.transform.MapSubst;
import nars.term.util.transform.TermTransform;
import nars.unify.Unify;
import nars.unify.UnifyAny;
import nars.unify.UnifyFirst;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import static nars.Op.CONJ;
import static nars.Op.EmptyTermArray;
import static nars.subterm.Subterms.different;
import static nars.time.Tense.*;

/** delegates certain methods to a specific impl */
public abstract class SeparateSubtermsCompound implements Compound {

    private final int hash;

//    protected SeparateSubtermsCompound(Op o, Subterms x) {
//        this(x.hashWith(o.id));
//    }

    SeparateSubtermsCompound(byte op, Subterms x) {
        this(x.hashWith(op));
    }

    SeparateSubtermsCompound(int hash) {
        this.hash = hash;
    }

    @Override
    public boolean isNormalized() {
        return subterms().isNormalized();
    }

    public final int subStructure() {
        return subterms().structure();
    }


    @Override
    public String toString() {
        return Compound.toString(this);
    }

    @Override
    public final boolean equals(Object obj) {
        return Compound.equals(this, obj,true);
    }

    @Override
    public final int hashCode() {
        //return Compound.hashCode(this);
        return hash;
    }

    @Override
    public final int hashCodeSubterms() {
        return subterms().hashCodeSubterms();
    }

    @Override
    public boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound superterm) {
        return !inSuperCompound.test(this) ||
                whileTrue.test(this) && subterms().recurseTerms(inSuperCompound, whileTrue, this);
    }

    @Override
    public boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        return !inSuperCompound.test(this) ||
                whileTrue.test(this) &&
                subterms().recurseTermsOrdered(inSuperCompound, whileTrue, this);
    }

    @Override
    public boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, @Nullable Compound superterm) {
        return !aSuperCompoundMust.test(this) ||
                whileTrue.test(this, superterm) && subterms().recurseTerms(aSuperCompoundMust, whileTrue, this);
    }

    public boolean subtermsContainsRecursively(Term x, boolean root, Predicate<Term> inSubtermsOf) {
        return subterms().containsRecursively(x, root, inSubtermsOf);
    }


    /*@NotNull*/
    @Override
    public Term[] arrayClone() {
        return subterms().arrayClone();
    }

    @Override
    public Term[] arrayShared() {
        return subterms().arrayShared();
    }

    @Override
    public Term[] arrayClone(Term[] x, int from, int to) {
        return subterms().arrayClone(x, from, to);
    }

    @Override
    public final int subs() {
        return subterms().subs();
    }

    @Override
    public final Term sub(int i) {
        return subterms().sub(i);
    }

    @Override
    public final Term sub(int i, Term ifOutOfBounds) {
        return subterms().sub(i, ifOutOfBounds);
    }

    @Override
    public final boolean subIs(int i, Op o) {
        return subterms().subIs(i, o);
    }

    @Override
    public final boolean subIsOrOOB(int i, Op o) {
        return subterms().subIsOrOOB(i, o);
    }

    @Override
    public @Nullable Term subPath(int start, int end, byte... path) {
        return end==start ? this : subterms().subSub(start, end, path);
    }

    @Override
    public Iterator<Term> iterator() {
        return subterms().iterator();
    }

    @Override
    public final Stream<Term> subStream() {
        return subterms().subStream();
    }

    @Override
    public final int count(Op matchingOp) {
        return subterms().count(matchingOp);
    }

    @Override
    public final int count(Predicate<Term> match) {
        return subterms().count(match);
    }

    @Override
    public final void forEach(/*@NotNull*/ Consumer<? super Term> c) {
        for (Term term : subterms())
            c.accept(term);
    }
    @Override
    public final void forEach(/*@NotNull*/ Consumer<? super Term> action, int start, int stop) {
        subterms().forEach(action, start, stop);
    }
    @Override
    public final void forEachI(ObjectIntProcedure<Term> t) {
        subterms().forEachI(t);
    }

    @Override
    public final <X> void forEachWith(BiConsumer<Term, X> t, X argConst) {
        subterms().forEachWith(t, argConst);
    }

    @Override
    public int addAllTo(Term[] t, int offset) {
        return subterms().addAllTo(t, offset);
    }

    @Override
    public void addAllTo(Collection target) {
        subterms().addAllTo(target);
    }

    @Override
    public void addAllTo(FasterList target) {
        subterms().addAllTo(target);
    }

    @Override
    public int indexOf(Term t, int after) {
        return subterms().indexOf(t, after);
    }

    @Override
    public final boolean contains(Term t) {
        return subterms().contains(t);
    }

    @Override
    public final boolean containsNeg(Term x) {
        return subterms().containsNeg(x);
    }

    @Override
    public boolean hasXternal() {
        return dt()==XTERNAL || subterms().hasXternal();
    }

    @Override
    public int structure() {
        return subStructure() | opBit();
    }

    @Override
    public int complexity() {
        return subterms().complexity();
    }

    @Override
    public int volume() {
        return subterms().volume();
    }

    @Override
    public int varQuery() {
        return subterms().varQuery();
    }

    @Override
    public int varPattern() {
        return subterms().varPattern();
    }
    @Override
    public int varDep() {
        return subterms().varDep();
    }

    @Override
    public int varIndep() {
        return subterms().varIndep();
    }

    @Override
    public int vars() {
        return subterms().vars();
    }

    @Override
    public final int intifyRecurse(int v, IntObjectToIntFunction<Term> reduce) {
        return subterms().intifyRecurse(v, reduce);
    }

    @Override
    public final int intifyShallow(int v, IntObjectToIntFunction<Term> reduce) {
        return subterms().intifyShallow(v, reduce);
    }

    @Override
    public final @Nullable Term subSub(int start, int end, byte[] path) {
        return subterms().subSub(start, end, path);
    }

    @Override
    public final @Nullable Term subSub(byte[] path) {
        return subterms().subSub(path);
    }

    @Override
    public final @Nullable Term subSubUnsafe(int start, int end, byte[] path) {
        return subterms().subSubUnsafe(start, end, path);
    }

    @Override
    public final @Nullable Subterms transformSubs(UnaryOperator<Term> f, Op superOp) {
        return subterms().transformSubs(f, superOp);
    }

    @Override
    public boolean OR(/*@NotNull*/ Predicate<Term> p) {
        return subterms().OR(p);
    }

    @Override
    public boolean AND(/*@NotNull*/ Predicate<Term> p) {
        return subterms().AND(p);
    }

    @Override
    public <X> boolean ORwith(BiPredicate<Term, X> p, X param) {
        return subterms().ORwith(p, param);
    }

    @Override
    public <X> boolean ANDwith(BiPredicate<Term, X> p, X param) {
        return subterms().ANDwith(p, param);
    }

    @Override
    public <X> boolean ANDwithOrdered(BiPredicate<Term, X> p, X param) {
        return subterms().ANDwithOrdered(p, param);
    }


    @Override
    public boolean ANDi(ObjectIntPredicate<Term> p) {
        return subterms().ANDi(p);
    }
    @Override
    public boolean ORi(ObjectIntPredicate<Term> p) {
        return subterms().ORi(p);
    }

    @Override
    public boolean unifiesRecursively(Term x) {
        return unifiesRecursively(x, (y)->true);
    }

    @Override
    public boolean unifiesRecursively(Term x, Predicate<Term> preFilter) {

        if (x instanceof Compound) {
//            int xv = x.volume();
            if (!hasAny(Op.Variable) /*&& xv > volume()*/)
                return false; //TODO check

            UnifyAny u = new UnifyAny();

            //if (u.unifies(this, x)) return true;

            int xOp = x.opID();
            return !subterms().recurseTerms(s->s.hasAny(1<<xOp)/*t->t.volume()>=xv*/, s->{
                if (s instanceof Compound && s.opID()==xOp) {
                    return !preFilter.test(s) || !x.unify(s, u);
                }
                return true;
            }, this);
        } else {
            return x instanceof Variable || containsRecursively(x);
        }
    }

    @Override
    public /* final */ boolean containsRecursively(Term x, boolean root, @Nullable Predicate<Term> inSubtermsOf) {
        return (inSubtermsOf == null || inSubtermsOf.test(this)) && subtermsContainsRecursively(x, root, inSubtermsOf);
    }

    @Override
    public abstract Subterms subterms();

    @Override
    public Subterms subtermsDirect() {
        return subterms();
    }

    @Override
    public boolean the() {
        return this instanceof The && subterms().these();
    }

    @Override
    public boolean these() {
        return this.the();
    }

    @Override
    public Term anon() {
        return new Anon(/* TODO size estimate */).put(this);
    }

    @Override
    public boolean ORrecurse(Predicate<Term> p) {
        boolean result = false;
        Subterms terms1 = subterms();
        int s = terms1.subs();
        Term prev = null;
        for (int i = 0; i < s; i++) {
            Term next = terms1.sub(i);
            if (different(prev, next)) {
                if (p.test(next) || (next instanceof Compound && ((Compound) next).ORrecurse(p))) {
                    result = true;
                    break;
                }
                prev = next;
            }
        }
        return p.test(this) || result;
    }

    @Override
    public boolean ANDrecurse(Predicate<Term> p) {
        boolean result = true;
        Subterms terms1 = subterms();
        int s = terms1.subs();
        Term prev = null;
        for (int i = 0; i < s; i++) {
            Term next = terms1.sub(i);
            if (different(prev, next)) {
                if (!p.test(next) || (next instanceof Compound && !((Compound) next).ANDrecurse(p))) {
                    result = false;
                    break;
                }
                prev = next;
            }
        }
        return p.test(this) && result;
    }

    //    @Override
//    default boolean unify(/*@NotNull*/ Term y, /*@NotNull*/ Unify u) {
//        return (this == y) || unifyForward(y, u) || ((y instanceof Variable) && y.unify(this, u));
//
//    }
    @Override
    public boolean unify(Term y, Unify u) {
        return (this == y)
                ||
                (y instanceof UnifyFirst && y.unify(this, u))
                ||
                (y instanceof Compound && (equals(y) || (opID() == y.opID() && unifySubterms((Compound)y, u))))

                ;
    }

    @Override
    public boolean unifySubterms(Compound y, Unify u) {

        Compound x = this;

        Op o = op();
        if (o.temporal && !u.unifyDT(x, y))
            return false;

        Subterms xx = x.subterms(), yy = y.subterms();

        if (o.temporal) {

            if (xx.equals(yy))
                return true; //compound equality would have been true if non-temporal

            if (o == CONJ)
                return ConjUnify.unifyConj(x, y, xx, yy, u);
        }

        int xs = xx.subs();
        if (xs != yy.subs())
            return false;

        if (xs == 1)
            return xx.sub(0).unify(y.sub(0), u);

        if (!Subterms.possiblyUnifiableAssumingNotEqual(xx, yy, u.varBits))
            return false;

        /* subs>1 */
        return o.commutative ?
            Subterms.unifyCommute(xx, yy, u) :
            Subterms.unifyLinear(xx, yy, u);
    }

    @Override
    public void appendTo(/*@NotNull*/ Appendable p) throws IOException {
        TermAppender.append(this, p);
    }

    @Override
    @Nullable
    public Object _car() {
        return sub(0);
    }

    @Override
    @Nullable
    public Object _cdr() {
        int len = subs();
        switch (len) {
            case 1:
                throw new RuntimeException("Pair fault");
            case 2:
                return sub(1);
            case 3:
                return new Pair(sub(1), sub(2));
            case 4:
                return new Pair(sub(1), new Pair(sub(2), sub(3)));
        }


        Pair p = null;
        for (int i = len - 2; i >= 0; i--) {
            p = new Pair(sub(i), p == null ? sub(i + 1) : p);
        }
        return p;
    }

    /*@NotNull*/
    @Override
    public Object setFirst(Object first) {
        throw new UnsupportedOperationException();
    }

    /*@NotNull*/
    @Override
    public Object setRest(Object rest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCommutative() {
        //Op op = op();
        return /*op == CONJ ? dtSpecial(dt()) : */op().commutative && subs() > 1;
    }

    @Override
    public abstract int dt();

    @Override
    public Term replace(Term from, Term to) {

        return MapSubst.replace(from, to, this);

//
//        Subterms oldSubs = subterms();
//        Subterms newSubs = oldSubs.replaceSub(from, to, op());
//
//        if (newSubs == oldSubs)
//            return this;
//
//        if (newSubs == null)
//            return Bool.Null;
//
//        int dt = dt();
//        Op o = op();
//        if (newSubs instanceof TermList) {
//            return o.the(dt, ((TermList) newSubs).arrayKeep());
//        } else {
//            return o.the(dt, newSubs);
//        }
//
    }

    @Override
    public boolean eventsOR(LongObjectPredicate<Term> each, long offset, boolean decomposeConjDTernal, boolean decomposeXternal) {
        return !eventsAND((when,what)-> !each.accept(when, what), offset, decomposeConjDTernal, decomposeXternal);
    }

    @Override
    public boolean eventsAND(LongObjectPredicate<Term> each, long offset, boolean decomposeConjDTernal, boolean decomposeXternal) {


        boolean decompose;
        int dt;

        if (opID() != CONJ.id) {
            decompose = false;
            dt = DTERNAL;
        } else {

            dt = dt();
            switch (dt) {

                case DTERNAL:
                    if (ConjSeq.isFactoredSeq(this)) {
                        Subterms ss = subterms();

                        //distribute the factored inner sequence
                        MetalBitSet eteComponents = ConjSeq.seqEternalComponents(ss);
                        Term seq = ConjSeq.seqTemporal(ss, eteComponents);

                        int sdt = seq.dt();
                        //non-special sequence
                        boolean unfactor = sdt != XTERNAL || decomposeXternal;
                        if (unfactor) {
                            Term factor = ConjSeq.seqEternal(ss, eteComponents);

                            return seq.eventsAND(
                                    (!decomposeConjDTernal) ?
                                            (when, what) -> {
                                                //combine the component with the eternal factor
                                                Term distributed = CONJ.the(what, factor);

                                                if (distributed.op() != CONJ)
                                                    throw new TermTransformException(SeparateSubtermsCompound.this, distributed, "invalid conjunction factorization"
                                                    );

                                                return each.accept(when, distributed);
                                            }
                                            :
                                            (when, what) ->
                                                    //provide the component and the eternal separately, at the appropriate time
                                                    each.accept(when, what) && each.accept(when, factor)

                                    , offset, decomposeConjDTernal, decomposeXternal);

                        }

                    }

                    if (decompose = decomposeConjDTernal)
                        dt = 0;

                    break;
//            case 0:
//                decompose = decomposeConjParallel;
//                break;
                case XTERNAL:
                    if (decompose = decomposeXternal)
                        dt = 0;
                    break;
                default:
                    decompose = true;
                    break;
            }
        }

        if (!decompose) {
            return each.accept(offset, this);
        } else {

            Subterms ee = subterms();

            long t = offset;

            boolean changeDT = dt != 0 && t != ETERNAL && t != TIMELESS /* motionless in time */;


            boolean fwd;
            if (changeDT) {
                fwd = dt >= 0;
                if (!fwd)
                    dt = -dt;
            } else {
                fwd = true;
            }

            int s = ee.subs() - 1;
            for (int i = 0; i <= s; i++) {
                Term ei = ee.sub(fwd ? i : s - i);
                if (!ei.eventsAND(each, t, decomposeConjDTernal, decomposeXternal))
                    return false;

                if (changeDT && i < s)
                    t += dt + ei.eventRange();
            }


            return true;
        }
    }

    @Override
    @Nullable
    public Term normalize(byte varOffset) {
        return varOffset == 0 && this.isNormalized() ? this : Op.terms.normalize(this, varOffset);

    }

    @Override
    public Term root() { return Op.terms.root(this);  }

    @Override
    public Term concept() { return TermBuilder.concept(this);  }

    @Override
    public int eventRange() {
        if (opID() == CONJ.id) {
            int dt = dt();
            if (dt == XTERNAL)
                return 0; //unknown actual range; logically must be considered as point-like event

            Subterms tt = subterms();
            int l = tt.subs();
            if (l == 2) {

                switch (dt) {
                    case DTERNAL:
                    case 0:
                        dt = 0;
                        break;
                    default:
                        dt = Math.abs(dt);
                        break;
                }

                return tt.subEventRange(0) + dt + tt.subEventRange(1);

            } else {
                int s = 0;
                for (int i = 0; i < l; i++)
                    s = Math.max(s, tt.subEventRange(i));
                return s;
            }
        }
        return 0;

    }

    @Override
    public Term dt(int dt) {
        return dt(dt, Op.terms);
    }

    @Override
    public Term dt(int nextDT, TermBuilder b) {
        return b.dt(this, nextDT);
    }

    @Override
    public boolean equalsRoot(Term x) {
        if (!(x instanceof Compound))
            return false;

        if (this.equals(x))
            return true;

        if (opID() != x.opID() || !hasAny(Op.Temporal)
                || structure()!=x.structure())
            return false;

        Term root = root(), xRoot;
        return (root != this && root.equals(x)) || (((xRoot = x.root())) != x && root.equals(xRoot));
    }

    @Override
    public /* final */ Term transform(TermTransform t) {
//        if (t instanceof RecursiveTermTransform) {
//            return transform((RecursiveTermTransform) t, null, XTERNAL);
//            //return transformBuffered(t, null, 256); //<- not ready yet
//        } else
            return t.applyCompound(this);
    }

    @Override
    public Term eventFirst() {
        if (Conj.isSeq(this)) {
            Term[] first = new Term[1];
            eventsAND((when, what) -> {
                first[0] = what;
                return false; //done got first
            }, 0, false, false);
            return first[0];
        }
        return this;
    }

    @Override
    public Term eventLast() {
        if (Conj.isSeq(this)) {
            Term[] last = new Term[1];
            eventsAND((when, what) -> {
                last[0] = what;
                return true; //HACK keep going to end
            }, 0, false, false);
            return last[0];
        }
        return this;
    }

    @Override
    public Predicate<Term> containing() {
        switch (subs()) {
            case 0:
                return (x) -> false;
            case 1:
                return sub(0)::equals;
            default:
                return this::contains;
        }
    }

    @Override
    public boolean containsInstance(Term t) {
        return ORwith((i, T) -> i == T, t);
    }

    @Override
    public boolean containsAll(Subterms ofThese) {
        return this.equals(ofThese) || ofThese.AND(this::contains);
    }

    @Override
    public boolean containsAny(Subterms ofThese) {
        return this.equals(ofThese) || ofThese.OR(this::contains);
    }

    @Override
    public <X> X[] array(Function<Term, X> map, IntFunction<X[]> arrayizer) {
        int s = subs();
        X[] xx = arrayizer.apply(s);
        for (int i = 0; i < s; i++)
            xx[i] = map.apply(sub(i));
        return xx;
    }

    @Override
    public int subEventRange(int i) {
        return sub(i).eventRange();
    }

    @Override
    @Nullable
    public Term subRoulette(FloatFunction<Term> subValue, Random rng) {
        int s = subs();
        switch (s) {
            case 0:
                return null;
            case 1:
                return sub(0);
            default:
                return sub(Roulette.selectRoulette(s, i -> subValue.floatValueOf(sub(i)), rng));
        }
    }

    @Override
    @Nullable
    public Term sub(Random rng) {
        int s = subs();
        switch (s) {
            case 0:
                return null;
            case 1:
                return sub(0);
            default:
                return sub(rng.nextInt(s));
        }
    }

    @Override
    public Subterms remove(Term event) {
        Term[] t = removing(event);
        return (t == null || t.length == subs()) ? this : Op.terms.subterms(t);
    }

    @Override
    public Subterms commuted() {
        if (subs() <= 1) return this;
        Term[] x = arrayShared();
        Term[] y = Terms.commute(x);
        return x == y ? this : new TermList(y);
    }

    @Override
    public boolean isSorted() {
        int s = subs();
        if (s < 2) return true;
        Term p = sub(0);
        for (int i = 1; i < s; i++) {
            Term n = sub(i);
            if (p.compareTo(n) > 0)
                return false;
            p = n;
        }
        return true;
    }

    @Override
    public boolean isCommuted() {
        int s = subs();
        if (s < 2) return true;
        Term p = sub(0);
        for (int i = 1; i < s; i++) {
            Term n = sub(i);
            if (p.compareTo(n) >= 0)
                return false;
            p = n;
        }
        return true;
    }

    @Override
    public boolean subEquals(int i, /*@NotNull*/ Term x) {
        return subs() > i && sub(i).equals(x);
    }

    @Override
    public /*@NotNull*/ SortedSet<Term> toSetSorted() {
        SortedSet<Term> u = new MetalTreeSet();
        addAllTo(u);
        return u;
    }

    @Override
    @SuppressWarnings("LambdaUnfriendlyMethodOverload")
    public /*@NotNull*/ SortedSet<Term> toSetSorted(UnaryOperator<Term> map) {
        MetalTreeSet<Term> u = new MetalTreeSet();
        addAllTo(u);
        return u;
    }

    @Override
    public /*@NotNull*/ SortedSet<Term> toSetSorted(Predicate<Term> t) {
        int s = subs();
        switch (s) {
            case 1:
                Term the = sub(0);
                return t.test(the) ? ArrayUnenforcedSortedSet.the(the) : ArrayUnenforcedSortedSet.empty;
            case 2:
                Term a = sub(0), b = sub(1);
                boolean aok = t.test(a), bok = t.test(b);
                if (aok && bok) {
                    return ArrayUnenforcedSortedSet.the(a, b);
                } else if (!aok && !bok) {
                    return ArrayUnenforcedSortedSet.empty;
                } else if (!bok) {
                    return ArrayUnenforcedSortedSet.the(a);
                } else
                    return ArrayUnenforcedSortedSet.the(b);
            default:
                List<Term> u = new FasterList<>(s);
                for (Term x : this) {
                    if (t.test(x)) u.add(x);
                }
                int us = u.size();
                switch (us) {
                    case 0:
                        return ArrayUnenforcedSortedSet.empty;
                    case 1:
                        return ArrayUnenforcedSortedSet.the(u.get(0));
                    case 2:
                        return ArrayUnenforcedSortedSet.the(u.get(0), u.get(1));
                    default:
                        return ArrayUnenforcedSortedSet.the(u.toArray(Op.EmptyTermArray));
                }
        }

    }

    @Override
    public Term[] arrayClone(Term[] target) {
        return arrayClone(target, 0, subs());
    }

    @Override
    public /*@NotNull*/ TermList toList() {
        return new TermList(this);
    }

    @Override
    public /*@NotNull*/ MutableSet<Term> toSet() {
        int s = subs();
        UnifiedSet<Term> u = new UnifiedSet(s, 0.99f);
        if (s > 0)
            addAllTo(u);
        return u;
    }

    @Override
    @Nullable
    public <C extends Collection<Term>> C collect(Predicate<Term> ifTrue, C c) {
        int s = subs();
        //UnifiedSet<Term> u = null;
        for (int i = 0; i < s; i++) {
            /*@NotNull*/
            Term x = sub(i);
            if (ifTrue.test(x)) {
//                    if (c == null)
//                        c = new UnifiedSet<>((s - i) * 2);
                c.add(x);
            }
        }

        return c;
    }

    @Override
    public void setNormalized() {

    }

    /*@NotNull*/
    @Override
    public Set<Term> recurseSubtermsToSet(Op _onlyType) {
        int onlyType = _onlyType.bit;
        if (!hasAny(onlyType))
            return Sets.mutable.empty();

        Set<Term> t = new HashSet(volume());


        recurseTerms(
            tt -> tt.hasAny(onlyType),
            tt -> {
                if (tt.opBit() == onlyType)
                    t.add(tt);
                return true;
            }, null);
        return t;
    }

    /*@NotNull*/
    @Override
    public boolean recurseSubtermsToSet(int inStructure, /*@NotNull*/ Collection<Term> t, boolean untilAddedORwhileNotRemoved) {
        boolean[] r = {false};
        Predicate<Term> selector = s -> {

            if (!untilAddedORwhileNotRemoved && r[0])
                return false;

            if (s.hasAny(inStructure)) {
                r[0] |= (untilAddedORwhileNotRemoved) ? t.add(s) : t.remove(s);
            }

            return true;
        };


        recurseTerms(
            (inStructure != -1) ?
                p -> p.hasAny(inStructure)
                :
                any -> true,
            selector, null);


        return r[0];
    }

    @Override
    public boolean equalTerms(/*@NotNull*/ Subterms c) {
        int s = subs();
        if (s != c.subs())
            return false;
for (int i = 0; i < s; i++) {
if (!sub(i).equals(c.sub(i)))
return false;
}
return true;
    }

    @Override
    public /* final */ boolean impossibleSubStructure(int structure) {
        //return !hasAll(structure);
        return !Op.has(subStructure(), structure, true);
    }

    @Override
    public Term[] terms(/*@NotNull*/ IntObjectPredicate<Term> filter) {
        TermList l = null;
        int s = subs();
        for (int i = 0; i < s; i++) {
            Term t = sub(i);
            if (filter.accept(i, t)) {
                if (l == null)
                    l = new TermList(subs() - i);
                l.add(t);
            }
        }
        return l == null ? Op.EmptyTermArray : l.arrayKeep();
    }

    @Override
    public boolean countEquals(Predicate<Term> match, int n) {
        int s = subs();
        if (n > s) return false; //impossible
        int c = 0;
        for (int i = 0; i < s; i++) {
            if (match.test(sub(i))) {
                c++;
                if (c > n)
                    return false;
            }
        }
        return c == n;
    }

    @Override
    public /* final */ int indexOf(/*@NotNull*/ Term t) {
        return indexOf(t, -1);
    }

    @Override
    @Nullable
    public Term subFirst(Predicate<Term> match) {
        int i = indexOf(match);
        return i != -1 ? sub(i) : null;
    }

    @Override
    public boolean impossibleSubTerm(Termlike target) {
        return impossibleSubVolume(target.volume()) || impossibleSubStructure(target.structure());
    }

    @Override
    public boolean impossibleSubTerm(int structure, int volume) {
        return impossibleSubStructure(structure) || impossibleSubVolume(volume);
    }

    @Override
    public MetalBitSet indicesOfBits(Predicate<Term> match) {
        int n = subs();
        MetalBitSet m = MetalBitSet.bits(n);
        for (int i = 0; i < n; i++) {
            if (match.test(sub(i)))
                m.set(i);
        }
        return m;
    }

    @Override
    public Term[] subsIncluding(Predicate<Term> toKeep) {
        return subsIncExc(indicesOfBits(toKeep), true);
    }

    @Override
    public Term[] subsIncluding(MetalBitSet toKeep) {
        return subsIncExc(toKeep, true);
    }

    @Override
    public Term[] removing(MetalBitSet toRemove) {
        return subsIncExc(toRemove, false);
    }

    @Override
    @Nullable
    public Term[] subsIncExc(MetalBitSet s, boolean includeOrExclude) {

        int c = s.cardinality();

        if (c == 0) {
//            if (!includeOrExclude)
//                throw new UnsupportedOperationException("should not reach here");
            return includeOrExclude ? Op.EmptyTermArray : arrayShared();
        }

        int size = subs();
        assert (c <= size) : "bitset has extra bits setAt beyond the range of subterms";

        if (includeOrExclude) {
            if (c == size) return arrayShared();
            if (c == 1) return new Term[]{sub(s.first(true))};
        } else {
            if (c == size) return EmptyTermArray;
            if (c == 1) return removing(s.first(true));
        }


        int newSize = includeOrExclude ? c : size - c;
        Term[] t = new Term[newSize];
        int j = 0;
        for (int i = 0; i < size; i++) {
            if (s.get(i) == includeOrExclude)
                t[j++] = sub(i);
        }
        return t;
    }

    /*@NotNull*/
    @Override
    public Term[] subRangeArray(int from, int to) {
        int n = subs();
        if (from == Integer.MIN_VALUE) from = 0;
        if (to == Integer.MAX_VALUE) to = n;

        if (from == 0 && to == n) {
            return arrayShared();

        } else {

            int s = to - from;
            if (s == 0)
                return EmptyTermArray;
            else {
                Term[] l = new Term[s];
                int y = from;
                for (int i = 0; i < s; i++)
                    l[i] = sub(y++);
                return l;
            }
        }
    }

    @Override
    public int indexOf(/*@NotNull*/ Predicate<Term> p) {
        return indexOf(p, -1);
    }

    @Override
    public int indexOf(/*@NotNull*/ Predicate<Term> p, int after) {
        int s = subs();
        Term prev = null;
        for (int i = after + 1; i < s; i++) {
            Term next = sub(i);
            if (different(prev, next)) {
                if (p.test(next))
                    return i;
                prev = next;
            }
        }
        return -1;
    }

    @Override
    public Subterms reversed() {
        return RemappedSubterms.reverse(this);
    }

    @Override
    public Term[] removing(int index) {
        int s = subs();
        Term[] x = new Term[s - 1];
        int k = 0;
        for (int j = 0; j < s; j++) {
            if (j != index)
                x[k++] = sub(j);
        }
        return x;

        //return ArrayUtils.remove(arrayShared(), Term[]::new, i);
    }

    @Override
    public int hashWith(Op op) {
        return hashWith(op.id);
    }

    @Override
    public int hashWith(/*byte*/int op) {
        return Util.hashCombine(this.hashCodeSubterms(), op);
    }

    @Override
    @Nullable
    public Term[] removing(Term x) {
        MetalBitSet toRemove = indicesOfBits(x::equals);
        return toRemove.cardinality() == 0 ? null : removing(toRemove);
    }

    @Override
    public Subterms transformSub(int which, UnaryOperator<Term> f) {
        Term x = sub(which);
        Term y = f.apply(x);
        //if (x == y)
        if (x.equals(y))
            return this;

        Term[] yy = arrayClone();
        yy[which] = y;
        return Op.terms.subterms(yy);
    }

    @Override
    public boolean containsPosOrNeg(Term x) {
        //TODO optimize
        return contains(x) || containsNeg(x);
    }
}
