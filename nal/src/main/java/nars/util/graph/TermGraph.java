package nars.util.graph;

public enum TermGraph {

//    public static AdjGraph<Term, Float> termlink(NAR nar) {
//        AdjGraph<Term, Float> g = new AdjGraph<>(true);
//        return termlink(nar, g);
//    }
//
//    public static AdjGraph<Term, Float> termlink(NAR nar, AdjGraph<Term, Float> g) {
//        return termlink(nar, nar.conceptsActive(), g);
//    }
//
//    public static AdjGraph<Term, Float> termlink(NAR n, Stream<? extends Termed> it, AdjGraph<Term, Float> g) {
//        it.forEach(st -> {
//            Term s = st.target();
//            if (g.addIfNew(s)) {
//                Concept c = n.concept(s);
//                c.termlinks().forEach(tl -> {
//                    Term t = tl.get();
//                    if (t.equals(s))
//                        return;
//                    g.addNode(t);
//                    float p = tl.pri();
//                    if (p == p)
//                        g.setEdge(s, t, p);
//                });
//            }
//        });
//        return g;
//    }


//    public enum Statements {
//        ;
//
//        public static void update(AdjGraph<Term, Term> g, Iterable<Term> sources, NAR nar, Predicate<Term> acceptNode, Predicate<Term> acceptEdge) {
//
//            @Deprecated Set<Term> done =
//
//                    new HashSet();
//
//
//            Set<Termed> next =
//                    Sets.newConcurrentHashSet();
//
//
//
//            Iterables.addAll(next, sources);
//
//            int maxSize = 512;
//            do {
//                Iterator<Termed> ii = next.iterator();
//                while (ii.hasNext()) {
//                    Term t = ii.next().target();
//                    ii.remove();
//                    if (!done.addAt(t))
//                        continue;
//
//                    Concept tc = nar.concept(t);
//                    if (!(tc instanceof TaskConcept))
//                        return;
//
//                    recurseTerm(nar, g, (impl) -> {
//                        if (acceptEdge.test(impl) && done.addAt(impl)) {
//                            Term s = impl.sub(0);
//                            if (!acceptNode.test(s))
//                                return;
//
//                            Term p = impl.sub(1);
//                            if (!acceptNode.test(p))
//                                return;
//
//                            s = s.temporalize(Retemporalize.retemporalizeAllToZero);
//                            if (s == null || !s.op().conceptualizable)
//                                return;
//
//
//                            p = p.temporalize(Retemporalize.retemporalizeAllToZero);
//                            if (p == null || !p.op().conceptualizable)
//                                return;
//
//                            next.addAt(s);
//                            next.addAt(p);
//                            if (s.op().conceptualizable && p.op().conceptualizable) {
//                                g.addNode(s);
//                                g.addNode(p);
//                                g.setEdge(s, p, impl.concept());
//                            }
//                        }
//                    }, tc);
//                }
//            } while (!next.isEmpty() && g.nodeCount() < maxSize);
//
//        }
//
//    }

//    protected static void recurseTerm(NAR nar, AdjGraph<Term, Term> g, Consumer<Term> next, Concept tc) {
//
//
//        Consumer<TaskLink> each = ml -> {
//
//            Termed termed = ml.get(nar);
//            if (termed == null) return;
//            Term target = termed.target();
//            if (target == null) return;
//
//            if (target.op() == IMPL && !target.hasVarQuery() /*&& l.subterms().containsRecursively(t)*/ /* && m.vars()==0 */
//
//            ) {
//
//
//                next.accept(target.concept());
//
//
//            }
//        };
//
//        tc.tasklinks().forEach(each);
//    }


}






























































