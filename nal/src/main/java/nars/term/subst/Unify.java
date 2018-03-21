package nars.term.subst;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import jcog.data.byt.AbstractBytes;
import jcog.data.byt.DynBytes;
import jcog.data.byt.RawBytes;
import jcog.list.FasterList;
import jcog.version.VersionMap;
import jcog.version.Versioned;
import jcog.version.Versioning;
import nars.Op;
import nars.Param;
import nars.derive.constraint.MatchConstraint;
import nars.derive.mutate.Termutator;
import nars.term.Term;
import nars.term.Termlike;
import nars.util.TermHashMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;


/* recurses a pair of compound term tree's subterms
across a hierarchy of sequential and permutative fanouts
where valid matches are discovered, backtracked,
and collected until power is depleted.



https://github.com/rupertlssmith/lojix/blob/master/lojix/prolog/src/main/com/thesett/aima/logic/fol/prolog/PrologUnifier.java
https://github.com/rupertlssmith/lojix/blob/master/lojix/wam_prolog/src/main/com/thesett/aima/logic/fol/wam/compiler/WAMInstruction.java
see this code for a clear explanation of what a prolog unifier does.
this code does some additional things but shares a general structure with the lojix code which i just found now
So it can be useful for a more easy to understand rewrite of this class TODO


*/
public abstract class Unify extends Versioning implements Subst {

    protected final static Logger logger = LoggerFactory.getLogger(Unify.class);

    public Random random;

    @Nullable
    public final Op type;


    public final Set<Termutator> termutes = new LinkedHashSet(8);

//    @NotNull
//    public final TermIndex terms;


    public final VersionMap<Term, Term> xy;

    /**
     * temporal tolerance; if -1, then it is not tested
     */
    public int dur = -1;

    /**
     * whether the variable unification allows to happen in reverse (a variable in Y can unify a constant in X)
     */
    public boolean varSymmetric = true;
    /**
     * whether common variables are allowed to be substituted on variable match
     */
    public boolean varCommonalize = true;

//    /**
//     * free variables remaining unassigned, for counting
//     */
//    protected final Versioned<Set<Term>> free;


    /**
     * @param type   if null, unifies any variable type.  if non-null, only unifies that type
     * @param random
     */
    protected Unify(@Nullable Op type, Random random, int stackMax, int initialTTL) {
        super(stackMax, initialTTL);

//        this.terms = terms;

        this.random = random;
        this.type = type;

        xy = new ConstrainedVersionMap(this);
        //this.free = new Versioned<>(this, 4); //task, belief, subIfUnifies + ?
        //this.freeCount = new Versioned<>(versioning, 8);

    }

    /**
     * spend an amount of TTL; returns whether it is still live
     */
    public final boolean use(int cost) {
        return ((ttl -= cost) > 0);
    }


    /**
     * called each time all variables are satisfied in a unique way
     *
     * @return whether to continue on any subsequent matches
     */
    public abstract void tryMatch();


//    void tryMatch(boolean termuting) {
//
//        if (termuting) {
//
//            if (Param.DEBUG_FILTER_DUPLICATE_MATCHES && matches == null)
//                matches = new UnifiedSet(1);
//
//            if (!Param.DEBUG_FILTER_DUPLICATE_MATCHES || matches.add(((ConstrainedVersionMap) xy).snapshot())) {
//
//                tryMatch(); //new unique match
//
//            } else {
//                //duplicate
//                throw new UnsupportedOperationException("duplicate match");
//            }
//        } else {
//
//            tryMatch();
//
//            if (Param.DEBUG_FILTER_DUPLICATE_MATCHES) {
//                assert (matches == null);
//                matches = Collections.emptySet();//indicates that there was a match, by being non-null
//            }
//        }
//
//    }

    public final boolean tryMutate(Termutator[] chain, int next) {
        if (!use(Param.TTL_MUTATE))
            return false;

        if (++next < chain.length) {
            chain[next].mutate(this, chain, next);
        } else {
            //tryMatch(true); //end of chain
            tryMatch(); //end of chain
        }
        return true;
    }

    @Nullable
    @Override
    public final Term xy(Term x0) {
        return xy.get(x0);

//        Term xy = x0, y = null;
//        while ((xy = this.xy.get(xy)) != null) { //completely dereference
//            y = xy;
//        }
//        return y;

//        //SAFE VERSION:
//        Term xy = x0, y0 = null, y = null;
//        while ((xy = this.xy.get(xy))!=null) { //completely dereference
//            if (y0!=null)
//                return y0;
//            y0 = y;
//            y = xy;
//        }
//        return y;

//        Term y0 = xy.get(x0);
//        if (y0 == null)
//            return null;
//        else {
//            Term y1 = xy.get(y0);
//            if (y1 == null)
//                return y0;
//            else
//                return y1;
//        }
    }

    private Set<AbstractBytes> matches = null;

    /**
     * unifies the next component, which can either be at the start (true, false), middle (false, false), or end (false, true)
     * of a matching context
     * <p>
     * setting finish=false allows matching in pieces before finishing
     * <p>
     * NOT thread safe, use from single thread only at a time
     */
    public boolean unify(Term x, Term y, boolean finish) {

        //assert(matches == null);
        matches = null;

        //accumulate any new free variables in this next matched term
//        Set<Term> freeX = freeVariables(x);
////        if (null == free.set(freeX)) //plus and not equals because this may continue from another unification!!!!!
////            return false;
//        if (freeX.isEmpty())
//            return x.equals(y);

        //assert (unassigned.isEmpty() ) : "non-purposeful unification";
        //this.freeCount.add( newFree.size() );

        if (x.unify(y, this)) {
            if (finish) {
                tryMatches();

                boolean matched = matches != null;
                matches = null;
                return matched;

            }
            return true;
        }

        assert (matches == null);
        return false;
    }

//    /**
//     * computes a lazy set with the new free variables added by the incoming term, to continue
//     * from a previous partial unification if necessary.
//     */
//    Set<Term> freeVariables(@NotNull Term x) {
//        Set<Term> prevFree = free.get();
//        Set<Term> nextFree = x.varsUnique(type, prevFree != null ?  prevFree : Collections.emptySet());
//        return concat(prevFree, nextFree);
//    }


    void tryMatches() {
        int ts = termutes.size();
        if (ts > 0) {

            //TODO use Termutator[] not List
            Termutator[] t = termutes.toArray(new Termutator[ts]);

            termutes.clear();

            //shuffle the ordering of the termutes themselves
            if (ts > 1)
                Util.shuffle(t, random);

            tryMutate(t, -1); //start combinatorial recurse

        } else {
            //tryMatch(false); //go directly to conclusion
            tryMatch();
        }

//        if (matched.size()>1)
//            System.out.println(matched);

//        matched.clear();

    }

//    private void tryMatch() {
//
//
////        if (freeCount.get() > 0) {
////            //quick test for no assignments
////            return;
////        }
//
//        //filter incomplete matches by detecting them here
//        //TODO use a counter to measure this instead of checking all the time
////        Iterator<Map.Entry<Term, Versioned<Term>>> ee = xy.map.entrySet().iterator();
////        while (ee.hasNext()) {
////            Map.Entry<Term, Versioned<Term>> e = ee.next();
////            Versioned<Term> v = e.getValue();
////            if ((v == null || v.get() == null) && matchType(e.getKey()))
////                return;
////        }
//
//
////        Set<Term> free = this.free.get();
////        Term[][] match = new Term[free.size()][];
////        int m = 0;
////        for (Term x : free) {
////            Term y = xy(x);
////            if (y == null)
////                return;
////            match[m++] = new Term[]{x, y};
////        }
////        Arrays.sort(match, matchElementComparator); //sort by key
//
////        if (!matched.add(((ConstrainedVersionMap)xy).snapshot()))
////            return; //already seen
//
//        onMatch( /*match*/);
//
//
//    }

    //final static Comparator<Term[]> matchElementComparator = Comparator.comparing(v -> v[0]);


    @Override
    public String toString() {
        return xy + "$" + ((Versioning<Term>) this).ttl;
    }


    /**
     * whether the op is assignable
     */
    public final boolean matchType(Op oy) {
        Op t = this.type;
        return t == null ?
                oy.var : //any variable
                oy == t; //the specified type
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("slow");
    }


    /**
     * returns true if the assignment was allowed, false otherwise
     * args should be non-null. the annotations are removed for perf reasons
     */
    public final boolean putXY(final Term x, final Term y) {

        if (y.containsRecursively(x)) {
            //TODO maybe create a common variable
            return false; //cyclic
        }

//        //TODO use a single Map.compute() function to avoid repeat hashmap gets
//        return xy.compute(x, (y0) -> {
//            if (y0 != null !replace(y0, y))
//                return null; //no change //<- should return y0 if replace failed
//            else
//                return y;   //try set
//        });

        Versioned<Term> y0Versioned = xy.getVersioned(x);
        if (y0Versioned != null) {
            Term y0 = y0Versioned.get();
            if (y0 != null) {
                if (y0.equals(y))
                    return true;

                if (y0.equalsRoot(y)) {
                    int ydt = y.dt();
                    if (ydt != XTERNAL) {
                        int y0dt = y0.dt();
                        if ((y0dt == XTERNAL) || (y0dt == DTERNAL && ydt != DTERNAL)) {
                            //replace because y is more temporally specific
                            replaceXY(x, y);
                        }
                    }
                    return true; //keep X, but continue
                } else {
                    return false; //mismatch
                }

            }

            return y0Versioned.set(y) != null;
        } else {
            return xy.tryPut(x, y);
        }
    }



    public final boolean replaceXY(final Term x, final Term y) {
        return xy.tryPut(x, y);
    }

    /**
     * stack counter, not time
     */
    public final int now() {
        return this.size;
    }
//
//    /** returns the updated value */
//    public int addTTL(int x) {
//        return this.ttl += x;
//    }

    public boolean constant(Termlike xsubs, Termlike ysubs) {
        //return relevantVariables(xsubs) || (varSymmetric && relevantVariables(ysubs));
        return constant(xsubs) && (!varSymmetric || constant(ysubs));
    }

    /**
     * whether is constant with respect to the current matched variable type
     */
    public boolean constant(Termlike x) {
        return !(type == null ?
                x.hasAny(Op.VAR_DEP.bit | Op.VAR_INDEP.bit | Op.VAR_QUERY.bit) || x.varPattern() > 0 :
                x.hasAny(type)
        );
    }

    private class ConstrainedVersionMap extends VersionMap<Term, Term> {
        public ConstrainedVersionMap(Versioning versioning) {
            super(versioning,
                    //4
                    new TermHashMap<>(),
                    1);
        }

//        @Nullable
//        @Override
//        public Term remove(Object key) {
//            Versioned<Term> x = map.remove(key);
//            if (x == null)
//                return null;
//            ConstrainedVersionedTerm cx = (ConstrainedVersionedTerm)x;
//            if (((ConstrainedVersionedTerm) x).forMatchedType)
//                assigned--;
//
//            return x.get();
//        }

//        @Override
//        public boolean tryPut(Term key, @NotNull Term value) {
//            int beforePut = matchType(key) ? now() : Integer.MAX_VALUE;
//            if (super.tryPut(key, value)) {
//                if (now() > beforePut) { //detects change and not just an equals() match
////                    int nextUnassigned = freeCount.get() - 1;
////
////                    if (nextUnassigned < 0)
////                        return false;
////                    //assert(nextUnassigned >= 0): "underflow";
////
////                    freeCount.add(nextUnassigned);
//                }
//                return true;
//            }
//            return false;
//        }

        @Override
        protected Versioned newEntry(Term x) {
            return new ConstrainedVersionedTerm();
        }

        public AbstractBytes snapshot() {
            List<RawBytes> pre = new FasterList<>(8);
            DynBytes b = new DynBytes(64);
            xy.forEach((x, y) -> {
                x.append((ByteArrayDataOutput) b);
                b.writeByte(0); //separator
                y.append((ByteArrayDataOutput) b);
                pre.add(b.rawCopy());
                b.clear();
            });

            int s = pre.size();
            switch (s) {
                case 0:
                    return AbstractBytes.EMPTY;
                case 1:
                    return pre.get(0);
                default:
                    Collections.sort(pre);
                    for (RawBytes r : pre) {
                        b.write(r.bytes);
                    }
                    //b.compact();
                    return b;
            }
        }
    }

    final class ConstrainedVersionedTerm extends Versioned<Term> {

        /**
         * lazily constructed
         */
        Versioned<MatchConstraint> constraints;

        ConstrainedVersionedTerm() {
            super(Unify.this, new Term[1]);
        }

        @Nullable
        @Override
        public Versioned<Term> set(Term next) {
            return valid(next) ? super.set(next) : null;
        }

        private boolean valid(Term x) {
            Versioned<MatchConstraint> c = this.constraints;
            if (c != null) {
                int s = c.size();
                for (int i = 0; i < s; i++)
                    if (c.get(i).invalid(x, Unify.this))
                        return false;
            }
            return true;
        }

        boolean constrain(MatchConstraint m) {

            Versioned<MatchConstraint> c = this.constraints;
            if (c == null)
                c = constraints = new Versioned(Unify.this, 4);

            return c.set(m) != null;
        }

    }


    public boolean constrain(MatchConstraint m) {
        return constrain(m.target, m);
    }

    public boolean constrain(Term target, MatchConstraint... mm) {
        Versioned<Term> v = xy.getOrCreateIfAbsent(target);
        for (MatchConstraint m : mm) {
            if (!((ConstrainedVersionedTerm) v).constrain(m)) {
                return false;
            }
        }
        return true;
    }

}


