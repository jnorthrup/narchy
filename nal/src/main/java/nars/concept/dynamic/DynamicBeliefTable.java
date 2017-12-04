package nars.concept.dynamic;

import jcog.decide.Roulette;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.BaseConcept;
import nars.concept.Tasklinks;
import nars.table.DefaultBeliefTable;
import nars.table.TemporalBeliefTable;
import nars.task.util.PredictionFeedback;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.truth.Truth;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.IntFloatPair;
import org.eclipse.collections.impl.map.mutable.primitive.IntFloatHashMap;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;


public class DynamicBeliefTable extends DefaultBeliefTable {

    final DynamicTruthModel model;
    private final boolean beliefOrGoal;
    private final Term term;


    public DynamicBeliefTable(Term c, TemporalBeliefTable t, DynamicTruthModel model, boolean beliefOrGoal) {
        super(t);
        this.term = c;
        this.model = model;
        this.beliefOrGoal = beliefOrGoal;
    }

    @Override
    public void add(Task input, BaseConcept concept, NAR nar) {

        if (!input.isInput()) {

            Task matched = match(input.start(), input.end(), input.term(), nar);

            if (matched != null && matched.term().equals(input.term())) {

                if (!(input.isEternal() ^ matched.isEternal())) { //must be of the same temporality

                    if (PredictionFeedback.absorb(matched, input, nar)) {
                        Tasklinks.linkTask(matched, matched.priElseZero(), concept, nar);
                        return;
                    }
                }

            }
        }

        super.add(input, concept, nar);
    }

    @Nullable
    protected Task generate(Term template, long start, long end, NAR nar) {
        DynTruth yy = truth(start, end, template, true, nar);
        return yy != null ? yy.task(beliefOrGoal, nar) : null;
    }

    @Override
    public Truth truth(long start, long end, NAR nar) {
        DynTruth d = truth(start, end, term, false, nar);
        return Truth.maxConf(d != null ? d.truth() : null,
                super.truth(start, end, nar) /* includes only non-dynamic beliefs */);
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
    protected DynTruth truth(long start, long end, Term template, boolean evidence, NAR nar) {
        template = template(start, end, template, nar);
        return template != null ? model.eval(template, beliefOrGoal, start, end, evidence, nar) : null;

    }

    @Nullable
    private Term template(long start, long end, Term template, NAR nar) {
        boolean temporal = template.op().temporal;
        if (temporal) {
            int d = template.dt();
            if (d == XTERNAL || d == DTERNAL) {
                int e = matchDT(start, end, nar);
                assert(e!=XTERNAL);
                Term next = template.dt(e);

                if (next.subs()==0 /* atomic */ || next.dt()==XTERNAL) {
                    /*if no dt can be calculated, return
                              0 or some non-zero value (ex: 1, end-start, etc) in case of repeating subterms. */
                    int artificialDT = (start!=end && end-start < Integer.MAX_VALUE) ?
                            ((int)(end-start)) : 1;
                    next = template.dt(artificialDT);
                }

                if (next instanceof Bool || next.dt()==XTERNAL) {
                    return null;
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
    private int matchDT(long start, long end, NAR nar) {

        int s = size();
        if (s == 0)
            return 0;

        int dur = nar.dur();

        IntFloatHashMap dtEvi = new IntFloatHashMap(s);
        forEachTask(t -> {
            int tdt = t.dt();
            if (tdt != DTERNAL)
                dtEvi.addToValue(tdt, t.evi(start, end, dur)); //maybe evi
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
    public Task match(long start, long end, Term template, NAR nar) {
        Task x = super.match(start, end, template, nar);


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
