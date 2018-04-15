/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.op.data;

import jcog.Util;
import jcog.bloom.StableBloomFilter;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.bag.leak.LeakBack;
import nars.subterm.Subterms;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.BELIEF;
import static nars.Op.VAR_QUERY;
import static nars.time.Tense.ETERNAL;

/**
 * Produces canonical "Reflective-Narsese" representation of a parameter term
 *
 * @author me
 */
public class reflect {

    final static Atomic REFLECT_OP = Atomic.the("reflect");

    /**
     * <(*,subject,object) --> predicate>
     */
    @Nullable
    public static Term sop(Term subject, Term object, Term predicate) {
        return $.inh($.p(reflect(subject), reflect(object)), predicate);
    }

    @Nullable
    public static Term sopNamed(String operatorName, @NotNull Compound s) {
        //return Atom.the(Utf8.toUtf8(name));

        //return $.the('"' + t + '"');

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
        return $.inh($.p(reflect(s.sub(0)), reflect(s.sub(1))), $.quote(operatorName));
    }

    @Nullable
    public static Term sop(@NotNull Subterms s, Term predicate) {
        return $.inh($.p(reflect(s.sub(0)), reflect(s.sub(1))), predicate);
    }

    @Nullable
    public static Term sop(String operatorName, @NotNull Subterms c) {
        Term[] m = new Term[c.subs()];
        for (int i = 0; i < c.subs(); i++) {
            if ((m[i] = reflect(c.sub(i))) == null)
                return null;
        }

        //return Atom.the(Utf8.toUtf8(name));

        //return $.the('"' + t + '"');

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
        return $.inh($.p(m), $.quote(operatorName));
    }

    @Nullable
    public static Term reflect(Term t) {
        if (t.subs() == 0) {
            return t;
        }
        switch (t.op()) {
            //case NEG: return t; //wont work
            case PROD:
                return t;
            //case INH: return sop(t, "inheritance");
            //case SIM:  return sop(t, "similarity");
            default:
                return sop(t.op().toString(), t.subterms());
        }
    }

    public static class ReflectClonedTask extends LeakBack {
        final static Logger logger = LoggerFactory.getLogger(ReflectClonedTask.class);

        private final NAR n;
        final static float VOL_RATIO_MAX = 2f;
        private final StableBloomFilter<Task> filter;

        public ReflectClonedTask(int cap, NAR n) {
            super(cap, n);
            this.n = n;
            this.filter = Task.newBloomFilter(1024, n.random());
        }

        @Override
        public boolean preFilter(Task next) {
            if (super.preFilter(next)) {
                Term tt = next.term();
                if (tt.subs() > 1 && !tt.hasAny(VAR_QUERY)) {
                    if (tt.volume() <= n.termVolumeMax.intValue() * 0.75f)
                        return filter.addIfMissing(next);
                }
            }
            return false;
        }

        @Override
        protected float leak(Task next) {
            Term x = next.term().concept();
            Term r = $.func(REFLECT_OP, x).eval(n.concepts.functors).normalize();
            if (x.equals(r)) //can happen
                return 0f;
            if ((r != null && r.subs() > 0)) {
                int yvol = r.volume();
                if (yvol <= n.termVolumeMax.intValue()) {
                    Task y = Task.clone(next, r);
                    if (y != null) {
                        y.pri(next.priElseZero() * Util.unitize(x.term().volume() / ((float)yvol)));
                        logger.info("+ {}", y);
                        input(y);
                        return 1;
                    }
                }
            }
            return 0;
        }
    }

    public static class ReflectSimilarToTaskTerm extends LeakBack {

        final static Logger logger = LoggerFactory.getLogger(ReflectSimilarToTaskTerm.class);

        final static float VOL_RATIO_MAX = 0.5f; //estimate
        private final NAR n;
        private final StableBloomFilter<Term> filter;


        public ReflectSimilarToTaskTerm(int cap, NAR n) {
            super(cap, n);
            this.filter = Terms.newTermBloomFilter(n.random(), 1024);
            this.n = n;
        }


        @Override
        public boolean preFilter(Task next) {
            if (super.preFilter(next)) {
                Term tt = next.term();
                if (tt.subs() > 1 && !tt.hasAny(VAR_QUERY))
                    if (tt.volume() <= n.termVolumeMax.intValue() * VOL_RATIO_MAX)
                        return filter.addIfMissing(tt.term().concept());

            }

            return false;
        }


        @Override
        protected float leak(Task next) {



            Term x = next.term().concept();
            Term reflectionSim = $.sim($.func(REFLECT_OP, x), x).eval(n.concepts.functors).normalize();
            if ((reflectionSim != null && reflectionSim.subs() > 0)) {
                int rvol = reflectionSim.volume();
                if (rvol <= n.termVolumeMax.intValue()) {

                    float c = x.vars() == 0 ?
                            n.confDefault(BELIEF) :
                            n.confMin.floatValue(); //if there is a variable, avoid becoming overconfident about linking across it. maybe this is too extreme of a conf discount

                    Task t = new NALTask(reflectionSim, BELIEF, $.t(1f, c), n.time(), ETERNAL, ETERNAL, n.time.nextStampArray());
                    t.pri(next.priElseZero() * Util.unitize(x.term().volume() / ((float)rvol)));
                    input(t);
                    logger.info("+ {}", reflectionSim);
                    return 1;
                }
            }
            return 0;
        }
    }
}

