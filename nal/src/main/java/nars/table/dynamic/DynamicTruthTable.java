package nars.table.dynamic;

import jcog.WTF;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.task.util.Answer;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.dynamic.DynStampTruth;
import nars.truth.dynamic.DynamicTruthModel;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;
import static nars.truth.dynamic.DynamicTruthModel.DynamicConjTruth.ConjIntersection;


/**
 * computes dynamic truth according to implicit truth functions
 * determined by recursive evaluation of the compound's sub-component's truths
 */
public final class DynamicTruthTable extends DynamicTaskTable {

    private final DynamicTruthModel model;

    public DynamicTruthTable(Term c, DynamicTruthModel model, boolean beliefOrGoal) {
        super(c, beliefOrGoal);
        this.model = model;
    }


    @Override
    public final void match(Answer t) {

        if (t.template == null)
            t.template(term);

        Task tt = taskDynamic(t);
        if (tt!=null)
            t.tryAccept(tt);
    }


    /**
     * generates a dynamic matching task
     */
    @Nullable
    public Task taskDynamic(Answer a) {
        Term template = a.template;

        NAR nar = a.nar;

        //TODO allow use of time's specified intersect/contain mode
        DynStampTruth yy = model.eval(template, beliefOrGoal, a.time.start, a.time.end, a.filter, false, nar);
        if (yy == null)
            return null;

        long s, e;



            int eternals = yy.count(x -> x.isEternal());
            if (eternals == 0) {
                s = yy.minValue(Stamp::start);
                e = yy.maxValue(Stamp::end);
            } else if (eternals == yy.size()) {
                s = ETERNAL;
                e = ETERNAL;
            } else {
                s = yy.minValue(t -> t.start() != ETERNAL ? t.start() : TIMELESS);
                e = yy.maxValue(Stamp::end);
            }

        float eviFactor;
        if (model == ConjIntersection) {
            if (s!=ETERNAL)
                e = s + (yy.minValue(t -> t.isEternal() ? 1 : t.range())-1);
            eviFactor = 1;
        } else {

//        //HACK discount by estimated evidence loss due to time gaps
//        float maxEvi = yy.maxValue((Task t) -> t.evi());
//        TruthPolation p = Param.truth(s, e, nar.dur());
//        yy.forEach(p::add);
//        float eviMax = p.truth().evi();
//        eviFactor = Math.min(1, eviMax / maxEvi);

            //HACK estimate by time range only
            if (s != ETERNAL) {
                long range = (e - s) + 1;
                eviFactor = (float) (yy.sumOfLong((Task x) -> x.isEternal() ? range : x.range()) / (((double) range * yy.size())));
                assert (eviFactor <= 1f);
            } else {
                eviFactor = 1;
            }
        }

        Truth t = model.apply(yy, nar);
        t = (t!=null && eviFactor!=1) ? PreciseTruth.byEvi(t.freq(), t.evi() * eviFactor) : t;
        if (t == null)
            return null;


        Term reconstruct = model.reconstruct(template, yy, nar);
        if (reconstruct == null) {
            if (Param.DEBUG)
                throw new WTF("could not reconstruct: " + template + ' ' + yy);
            return null;
        }

        return yy.task(reconstruct, t, yy::stamp, beliefOrGoal, s, e, nar);
    }


//    @Override
//    protected Truth truthDynamic(long start, long end, Term template, Predicate<Task> filter, NAR nar) {
//
//        DynStampTruth d = model.eval(template, beliefOrGoal, start, end, filter, true, nar);
//
//        return d != null ? d.truth(template, model, beliefOrGoal, nar) : null;
//
//    }


}














































