package nars.concept.dynamic;

import jcog.decide.Roulette;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.link.Tasklinks;
import nars.table.DefaultBeliefTable;
import nars.table.TemporalBeliefTable;
import nars.task.util.PredictionFeedback;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.IntFloatPair;
import org.eclipse.collections.impl.map.mutable.primitive.IntFloatHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Predicate;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;


public class DynamicBeliefTable extends DefaultBeliefTable {

    private final DynamicTruthModel model;
    private final boolean beliefOrGoal;
    private final Term term;


    public DynamicBeliefTable(Term c, TemporalBeliefTable t, DynamicTruthModel model, boolean beliefOrGoal) {
        super(t);
        this.term = c;
        this.model = model;
        this.beliefOrGoal = beliefOrGoal;
    }

    @Override
    public boolean add(Task input, TaskConcept concept, NAR nar) {

        if (Param.FILTER_DYNAMIC_MATCHES) {
            if (!input.isInput()) {

                long start, end;
                Task matched = match(start = input.start(), end = input.end(), input.term(), nar);

                //must be _during_ the same time and same term, same stamp, then compare Truth
                if (matched != null) {

                    if ((matched.start() <= input.start() && matched.end() >= input.end()) &&
                            matched.term().equals(input.term()) &&
                            Arrays.equals(matched.stamp(), input.stamp())) {

                        if (PredictionFeedback.absorb(matched, input, start, end, nar)) {
                            Tasklinks.linkTask(matched, matched.priElseZero(), concept, nar);
                            return false;
                        }

                    }
                }
            }
        }

        return super.add(input, concept, nar);
    }

    @Nullable
    protected Task generate(final Term template, long start, long end, NAR nar) {
        DynTruth yy = truth(start, end, template, nar);
        if (yy != null) {
            Task[] tt = new Task[1];
            yy.truth(term, model, (t) -> tt[0] = t, beliefOrGoal, nar);
            return tt[0];
        } else {
            return null;
        }
    }

    @Override
    public Truth truth(long start, long end, NAR nar) {
        //DynTruth d = truth(start, end, null, nar);
        DynTruth d = model.eval(term, beliefOrGoal, start, end, nar);
        if (d == null)
            return null;

        Truth tr = d.truth(term, model, null, beliefOrGoal, nar);

        Truth st = super.truth(start, end, nar);
        return tr!=null ? Truth.maxConf(tr, st) : st;
    }

//    /** prepare a term, if necessary, for use as template  */
//    private Term template(Term template, long start, long end, NAR nar) {
//        if (template.dt() == XTERNAL) {
//            int newDT = matchDT(template, start, end);
//            template = template.dt(newDT);
//        }
//
//        //still XTERNAL ? try using start..end as a dt
//        if (start!=DTERNAL && template.dt() == XTERNAL && template.subs()==2) {
//            template = template.dt((int) (end-start));
//        }
//
//        Retemporalize retemporalizeMode =
//                template.subterms().OR(Term::isTemporal) ?
//                    Retemporalize.retemporalizeXTERNALToZero  //dont unnecessarily attach DTERNALs to temporals
//                        :
//                    Retemporalize.retemporalizeXTERNALToDTERNAL //dont unnecessarily create temporals where DTERNAL could remain
//        ;
//        template = template.temporalize(retemporalizeMode);
//        if (template == null)
//            return null;
//
//
//        if (!template.conceptual().equals(term))
//            return null; //doesnt correspond to this concept anyway
//
//        return template;
//
////        if (t2 == null) {
////
////
////
////            //for some reason, retemporalizing to DTERNAL failed (ex: conj collision)
////            //so as a backup plan, use dt=+/-1
////            int dur = nar.dur();
////            Random rng = nar.random();
////            t2 = template.temporalize(new Retemporalize.RetemporalizeFromToFunc(XTERNAL,
////                    () -> rng.nextBoolean() ? +dur : -dur));
////        }
//////        if (t2!=null && t2.dt()==XTERNAL) {
//////            return template(t2, start, end ,nar);//temporary
//////            //throw new RuntimeException("wtf xternal");
//////        }
////
////        return t2;
//    }


    @Nullable
    protected DynTruth truth(long start, long end, @Nullable Term _template, NAR nar) {
        Term template = template(start, end, (_template != null ? _template : term), nar);
//        if (_template == null && !template.equals(term))
//            return null;
        return template != null ? model.eval(template, beliefOrGoal, start, end, nar) : null;

    }

    @Nullable
    private Term template(long start, long end, Term template, NAR nar) {
        if (this.term!=null && template.op()!=this.term.op())
            return null; //template doesnt match this (quick op test)

        int templateSubs = template.subs();
        assert (templateSubs > 1);
        boolean temporal = template.op().temporal;
        if (temporal) {
            int d = template.dt();
            if (d == XTERNAL) {
                int e = matchDT(start, end, templateSubs > 2, nar);
                assert (e != XTERNAL);
                Term next = template.dt(e);

                if ((next.subs() < templateSubs || next.dt() == XTERNAL)) {

                    /*if no dt can be calculated, return
                              0 or some non-zero value (ex: 1, end-start, etc) in case of repeating subterms. */
                    int artificialDT;


                    if (templateSubs == 2) {
                        if (start != end && end - start < Integer.MAX_VALUE) {
                            if (end != start) {
                                artificialDT = (int) (end - start);
                            } else {
                                artificialDT =
                                        (template.sub(0).unneg().equals(template.sub(1).unneg())) ?
                                                nar.dur() :
                                                0; //ok for simultaneous
                            }
                        } else {
                            artificialDT = nar.dur();
                        }


                    } else /*(if (templateSubs > 2)*/ {
                        assert (templateSubs > 2);
                        artificialDT = 0; //commutive conjunction
                    }

                    next = template.dt(artificialDT);

                    if (next.subs() < templateSubs || next.dt() == XTERNAL) {
                        if (templateSubs == 2) {

                            //possibly pulled an internal XTERNAL to the outside, so try artificializing this as well
                            int limit = 2;
                            int nextDT = XTERNAL;
                            do {
                                next = next.dt(artificialDT);
                                if (next instanceof Bool)
                                    return null;
                            } while (limit-- > 0 && (nextDT = next.dt()) == XTERNAL);

                            if (nextDT == XTERNAL)
                                return null; //give up

                        } else {
                            //create a random sequence of the terms, separated by artificial DT's
                            assert (template.op() == CONJ);
                            Term[] subs = template.subterms().arrayClone();
                            ArrayUtils.shuffle(subs, nar.random());
                            int dur = nar.dur();
                            next = subs[0];
                            for (int k = 1; k < subs.length; k++) {
                                next = Op.conjMerge(next, 0, subs[k], dur);
                                if (next instanceof Bool)
                                    return null; //it is probably possible to find another solution with a different shuffle
                            }
                        }
                    }
                }

                template = next;
            }
        }
        return template;
    }

    /**
     * returns an appropriate dt for the root term
     * of beliefs held in the table.  returns 0 if no other value can
     * be computed.
     */
    private int matchDT(long start, long end, boolean commutive, NAR nar) {

        int s = size();
        if (s == 0)
            return 0;

        int dur = nar.dur();

        IntFloatHashMap dtEvi = new IntFloatHashMap(s);
        forEachTask(t -> {
            int tdt = t.dt();
            if (tdt != DTERNAL) {
                if (tdt == XTERNAL)
                    throw new RuntimeException("XTERNAL should not be present in " + t);
                if ((t.term().subs() > 2) == commutive)
                    dtEvi.addToValue(tdt, t.evi(start, end, dur)); //maybe evi
            }
        });
        int n = dtEvi.size();
        if (n == 0) {
            return 0;
        } else {
            MutableList<IntFloatPair> ll = dtEvi.keyValuesView().toList();
            int selected = n != 1 ?
                    Roulette.decideRoulette(ll.size(), (i) -> ll.get(i).getTwo(), nar.random()) : 0;
            return ll.get(selected).getOne();
        }
    }

    @Override
    public Task match(long start, long end, Term template, NAR nar, Predicate<Task> filter) {
        Task x = super.match(start, end, template, nar, filter);


        Task y = generate(template, start, end, nar);
        if (y == null || y.equals(x)) return x;

        boolean dyn;
        if (x == null) {
            dyn = true;
        } else {
            //choose higher confidence
            int dur = nar.dur();
            float xc = x.evi(start, end, dur);
            float yc = y.evi(start, end, dur);

            //prefer the existing task within a small epsilon lower for efficiency
            dyn = yc >= xc + Param.TRUTH_EPSILON;
        }

        if (dyn) {
            //Activate.activate(y, y.priElseZero(), nar);
            return y;
        } else {
            return x;
        }

    }
}
