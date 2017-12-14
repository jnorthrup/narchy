package nars.util.graph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import jcog.data.graph.AdjGraph;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import nars.term.transform.Retemporalize;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.IMPL;
import static nars.time.Tense.XTERNAL;

public enum TermGraph {
    ;

    public static AdjGraph<Term, Float> termlink(NAR nar) {
        AdjGraph<Term, Float> g = new AdjGraph<>(true);
        return termlink(nar, g);
    }
    public static AdjGraph<Term, Float> termlink(NAR nar, AdjGraph<Term, Float> g) {
        return termlink(nar, nar.conceptsActive(), g);
    }

    public static AdjGraph<Term, Float> termlink(NAR n, Stream<? extends Termed> it, AdjGraph<Term, Float> g) {
        it.forEach(st -> {
            Term s = st.term();
            if (g.addIfNew(s)) {
                Concept c = n.concept(s);
                c.termlinks().forEach(tl -> {
                    Term t = tl.get();
                    if (t.equals(s))
                        return; //no self loop
                    g.addNode(t);
                    float p = tl.pri();
                    if (p == p)
                        g.setEdge(s, t, p);
                });
            }
        });
        return g;
    }


    public static class ImplGraph {

        //final static String VERTEX = "V";

        public ImplGraph() {
            super();
//            nar.onTask(t -> {
//                if (t.isBelief())
//                    task(nar, t);
//            });

        }

        protected boolean accept(Task t) {
            //example:
            return t.op() == IMPL;
        }

        public AdjGraph<Term, Term> snapshot(Iterable<Term> sources, NAR nar) {
            return snapshot(null, sources, nar);
        }

        public AdjGraph<Term, Term> snapshot(AdjGraph<Term, Term> g, Iterable<Term> sources, NAR nar) {

            if (g == null) {
                g = new AdjGraph<>(true);
            }

            @Deprecated Set<Term> done = Sets.newConcurrentHashSet();

            //TODO bag for pending concepts to visit?
            Set<Termed> next = Sets.newConcurrentHashSet();
            Iterables.addAll(next, sources);

            int maxSize = 512;
            do {
                Iterator<Termed> ii = next.iterator();
                while (ii.hasNext()) {
                    Term t = ii.next().term();
                    ii.remove();
                    if (!done.add(t))
                        continue;
                    AdjGraph<Term, Term> gg = g;
                    recurseTerm(nar, g, (impl) -> {
                        if (!done.add(impl)) {
                            Term s = impl.sub(0);
                            if (!acceptTerm(s)) {
                                return;
                            }

                            Term p = impl.sub(1);
                            if (!acceptTerm(p)) {
                                return;
                            }

                            s = s.temporalize(Retemporalize.retemporalizeAllToZero);
                            if (s == null)
                                return;
                            p = p.temporalize(Retemporalize.retemporalizeAllToZero);
                            if (p == null)
                                return;

                            next.add(s);
                            next.add(p);
                            impl(gg, nar, impl, s, p);
                        }
                    }, t);
                }
            } while (!next.isEmpty() && g.nodeCount() < maxSize);

            //System.out.println(g.nodeCount() + " " + g.edgeCount());
            return g;
        }

        protected void recurseTerm(NAR nar, AdjGraph<Term, Term> g, Consumer<Term> next, Term t) {


            Concept tc = nar.concept(t);
            if (tc == null)
                return; //ignore non-conceptualized

            Consumer<PriReference<? extends Termed>> each = ml -> {

                Termed termed = ml.get(); if (termed == null) return;
                Term term = termed.term();  if (term == null) return;
                Term l = term.conceptual();

                if (l.op() == IMPL && !l.hasVarQuery() /*&& l.subterms().containsRecursively(t)*/ /* && m.vars()==0 */
                    //&& ((Compound)m).containsTermRecursively(t)) {
                        ) {


                    //if (!g.nodes().contains(s) || !done.contains(p)) {
//                            if ((s.equals(t) || s.containsRecursively(t)) ||
//                                    (p.equals(t) || p.containsRecursively(t))) {
                    next.accept(l);
                    // }
                    //}
                }
            };
            tc.termlinks().forEach(each);
            tc.tasklinks().forEach(each);
        }

        protected boolean acceptTerm(Term p) {
            return true;
        }

        private void impl(AdjGraph<Term, Term> g, NAR nar, Term l, Term subj, Term pred) {

//            int dur = nar.dur();
//            Task t = nar.belief(l, when);
//            if (t == null)
//                return;

//            int dt = t.dt();
//            if (dt == DTERNAL)
//                dt = 0;
//
//            float evi =
//                    t.evi(when, dur);
//            //dt!=DTERNAL ? w2c(TruthPolation.evidenceDecay(t.evi(), dur, dt)) : t.conf();
//
//            float freq = t.freq();
//            boolean neg;
//            float val = (freq - 0.5f) * 2f * evi;
//            if (val < 0f) {
//                val = -val;
//                neg = true;
//            } else {
//                neg = false;
//            }
//
//            val *= TruthPolation.evidenceDecay(1f, dur, Math.abs(dt));
//
//            if (val!=val || val < Priority.EPSILON)
//                return;
//
//            boolean reverse = dt < 0;
//            Term S = reverse ? pred.negIf(neg) : subj;
//            Term P = reverse ? subj : pred.negIf(neg);
            if (subj.op().conceptualizable && pred.op().conceptualizable ) {
                g.addNode(subj);
                g.addNode(pred);
                g.setEdge(subj, pred, $.impl(subj, XTERNAL /* whatever best matches, regardless of time */, pred));
            }
        }

    }

}

//    public static final class ImplLink extends RawPLink<Term> {
//
//        final boolean subjNeg;
//        final boolean predNeg;
//
//        public ImplLink(Term o, float p, boolean subjNeg, boolean predNeg) {
//            super(o, p);
//            this.subjNeg = subjNeg;
//            this.predNeg = predNeg;
//        }
//
//        @Override
//        public boolean equals(@NotNull Object that) {
//            return super.equals(that) && ((ImplLink)that).subjNeg == subjNeg;
//        }
//
//        @Override
//        public int hashCode() {
//            return super.hashCode() * (subjNeg ? -1 : +1);
//        }
//
//    }
//
//    class ConceptVertex  {
//
//        //these are like more permanent set of termlinks for the given context they are stored by
//        final HijackBag<Term, ImplLink> in;
//        final HijackBag<Term, ImplLink> out;
//
//        public ConceptVertex(Random rng) {
//            in = new MyPLinkHijackBag(rng);
//            out = new MyPLinkHijackBag(rng);
//        }
//
//        private class MyPLinkHijackBag extends PLinkHijackBag {
//            public MyPLinkHijackBag(Random rng) {
//                super(32, 4, rng);
//            }
//
//            @Override
//            public float pri(@NotNull PLink key) {
//                float p = key.pri();
//                return Math.max(p - 0.5f, 0.5f - p); //most polarizing
//            }
//
//            @Override
//            protected float merge(@Nullable PLink existing, @NotNull PLink incoming, float scale) {
//
//                //average:
//                if (existing != null) {
//                    float pAdd = incoming.priElseZero();
//                    existing.priAvg(pAdd, scale);
//                    return 0;
//                } else {
//                    return 0;
//                }
//
//            }
//        }
//    }

