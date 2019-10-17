/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.op.data;

import nars.$;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Produces canonical "Reflective-Narsese" representation of a parameter target
 *
 * @author me
 */
public class reflect {

    static final Atomic REFLECT_OP = Atomic.the("reflect");

    /**
     * <(*,subject,object) --> predicate>
     */
    public static @Nullable Term sop(Term subject, Term object, Term predicate) {
        return $.inh($.p(reflect(subject), reflect(object)), predicate);
    }

    public static @Nullable Term sopNamed(String operatorName, @NotNull Compound s) {
        

        















        
        return $.inh($.p(reflect(s.sub(0)), reflect(s.sub(1))), $.quote(operatorName));
    }

    public static @Nullable Term sop(@NotNull Subterms s, Term predicate) {
        return $.inh($.p(reflect(s.sub(0)), reflect(s.sub(1))), predicate);
    }

    public static @Nullable Term sop(String operatorName, @NotNull Subterms c) {
        Term[] m = new Term[c.subs()];
        for (int i = 0; i < c.subs(); i++) {
            if ((m[i] = reflect(c.sub(i))) == null)
                return null;
        }

        

        















        
        return $.inh($.p(m), $.quote(operatorName));
    }

    public static @Nullable Term reflect(Term t) {
        if (t.subs() == 0) {
            return t;
        }
        switch (t.op()) {
            
            case PROD:
                return t;
            
            
            default:
                return sop(t.op().toString(), t.subterms());
        }
    }

//    public static class ReflectClonedTask extends LeakBack {
//        final static Logger logger = LoggerFactory.getLogger(ReflectClonedTask.class);
//
//        private final NAR n;
//        final static float VOL_RATIO_MAX = 2f;
//        private final StableBloomFilter<Task> filter;
//
//        public ReflectClonedTask(int cap, NAR n) {
//            super(cap, n);
//            this.n = n;
//            this.filter = Task.newBloomFilter(1024, n.random());
//        }
//
//        @Override
//        public boolean preFilter(Task next) {
//            if (super.preFilter(next)) {
//                Term tt = next.target();
//                if (tt.subs() > 1 && !tt.hasAny(VAR_QUERY)) {
//                    if (tt.volume() <= n.termVolumeMax.intValue() * 0.75f)
//                        return filter.addIfMissing(next);
//                }
//            }
//            return false;
//        }
//
//        @Override
//        protected float leak(Task next) {
//            Term x = next.target().concept();
//            Term r = $.func(REFLECT_OP, x).eval(n, true).normalize();
//            if (x.equals(r))
//                return 0f;
//            if ((r != null && r.subs() > 0)) {
//                int yvol = r.volume();
//                if (yvol <= n.termVolumeMax.intValue()) {
//                    Task y = Task.clone(next, r);
//                    if (y != null) {
//                        y.pri(next.priElseZero() * Util.unitize(x.target().volume() / ((float)yvol)));
//                        logger.info("+ {}", y);
//                        input(y);
//                        return 1;
//                    }
//                }
//            }
//            return 0;
//        }
//    }
//
//    public static class ReflectSimilarToTaskTerm extends LeakBack {
//
//        final static Logger logger = LoggerFactory.getLogger(ReflectSimilarToTaskTerm.class);
//
//        final static float VOL_RATIO_MAX = 0.5f;
//        private final NAR n;
//        private final StableBloomFilter<Term> filter;
//
//
//        public ReflectSimilarToTaskTerm(int cap, NAR n) {
//            super(cap, n);
//            this.filter = Terms.newTermBloomFilter(n.random(), 1024);
//            this.n = n;
//        }
//
//
//        @Override
//        public boolean preFilter(Task next) {
//            if (super.preFilter(next)) {
//                Term tt = next.target();
//                if (tt.subs() > 1 && !tt.hasAny(VAR_QUERY))
//                    if (tt.volume() <= n.termVolumeMax.intValue() * VOL_RATIO_MAX)
//                        return filter.addIfMissing(tt.target().concept());
//
//            }
//
//            return false;
//        }
//
//
//        @Override
//        protected float leak(Task next) {
//
//
//
//            Term x = next.target().concept();
//            Term reflectionSim = $.sim($.func(REFLECT_OP, x), x).eval(n, true).normalize();
//            if ((reflectionSim != null && reflectionSim.subs() > 0)) {
//                int rvol = reflectionSim.volume();
//                if (rvol <= n.termVolumeMax.intValue()) {
//
//                    float c = !x.hasVars() ?
//                            n.confDefault(BELIEF) :
//                            n.confMin.floatValue();
//
//                    Task t = new NALTask(reflectionSim, BELIEF, $.t(1f, c), n.time(), ETERNAL, ETERNAL, n.evidence());
//                    t.pri(next.priElseZero() * Util.unitize(x.target().volume() / ((float)rvol)));
//                    input(t);
//                    logger.info("+ {}", reflectionSim);
//                    return 1;
//                }
//            }
//            return 0;
//        }
//    }
}

