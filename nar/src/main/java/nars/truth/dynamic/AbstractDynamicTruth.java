package nars.truth.dynamic;

import jcog.Util;
import jcog.math.LongInterval;
import jcog.util.ObjectLongLongPredicate;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.table.dynamic.DynamicTruthTable;
import nars.task.util.Answer;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.proj.TruthProjection;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

import static nars.NAL.STAMP_CAPACITY;
import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 12/4/16.
 */
public abstract class AbstractDynamicTruth {

	public abstract Truth truth(DynTaskify d /* eviMin, */);


	public abstract boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each);

	public boolean temporal() {
		return false;
	}

	/**
	 * used to reconstruct a dynamic target from some or all components
	 */
	public abstract Term reconstruct(Compound superterm, long start, long end, DynTaskify d);


	/**
	 * default subconcept Task resolver
	 */
	public Task subTask(TaskConcept subConcept, Term subTerm, long subStart, long subEnd, Predicate<Task> filter, DynTaskify d) {
		return subTask(
			(BeliefTable) subConcept.table(d.beliefOrGoal ? BELIEF : GOAL),
			subTerm, subStart, subEnd, filter, d);
	}

	public static Task subTask(BeliefTable table, Term subTerm, long subStart, long subEnd, Predicate<Task> filter, DynTaskify d) {
        NAR nar = d.nar;
        float dur = d.dur;
		Task x;
		switch (NAL.DYN_TASK_MATCH_MODE) {
			case 0:
				//may be too aggressive in evidence collection, preventing other components from succeeding
				x = table.matchExact(subStart, subEnd, subTerm, filter, dur, nar);
				break;
			case 1:
				//may be too aggressive in evidence collection, preventing other components from succeeding
				x = table.match(subStart, subEnd, subTerm, filter, dur, nar);
				break;
			case 2:
				@Nullable Answer a = table.sampleAnswer(subStart, subEnd, subTerm, filter, dur, nar);
				x = a!=null ? a.sample() : null;
				break;
			default:
				throw new UnsupportedOperationException();
		}

		return x;
	}

	public static ObjectBooleanToObjectFunction<Term, BeliefTable[]> table(AbstractDynamicTruth... models) {
		return new ObjectBooleanToObjectFunction<Term, BeliefTable[]>() {
            @Override
            public BeliefTable[] valueOf(Term t, boolean beliefOrGoal) {
                return Util.map(new Function<AbstractDynamicTruth, BeliefTable>() {
                    @Override
                    public BeliefTable apply(AbstractDynamicTruth m) {
                        return new DynamicTruthTable(t, m, beliefOrGoal);
                    }
                }, new BeliefTable[models.length], models);
            }
        };
	}


	/**
	 * estimates number of components, for allocation purposes
	 */
	public abstract int componentsEstimate();

	public @Nullable Task task(Compound template, long earliest, long s, long e, DynTaskify d) {
        Term y = reconstruct(template, s, e, d);
		if (y == null || !y.unneg().op().taskable /*|| y.hasXternal()*/) { //quick tests
//			if (NAL.DEBUG) {
//				//TEMPORARY for debug
////                  model.evalComponents(answer, (z,start,end)->{
////                      System.out.println(z);
////                      nar.conceptualizeDynamic(z).beliefs().match(answer);
////                      return true;
////                  });
////                  model.reconstruct(template, this, s, e);
////                throw new TermException("DynTaskify template not reconstructed: " + this, template);
//			}
			return null;
		}

        NAR nar = d.nar;


        boolean absolute = !temporal() || s == LongInterval.ETERNAL || earliest == LongInterval.ETERNAL;
		if (!absolute) {
			for (int i = 0, dSize = d.size(); i < dSize; i++) {
                Task x = d.get(i);
                long xStart = x.start();
				if (xStart != ETERNAL) {
                    long shift = xStart - earliest;
					long ss = s + shift, ee = e + shift;
					if (xStart != ss || x.end() != ee) {
                        Task tt = Task.project(x, ss, ee,
							NAL.truth.EVI_MIN, //minimal truth threshold for accumulating evidence
							false,
							1, //no need to dither truth or time here.  maybe in the final calculation though.
							d.dur,
							false,
							nar);
						if (tt == null)
							return null;
						d.setFast(i, tt);
					}
				}
			}
		}


        Truth t = this.truth(d);
		if (t == null)
			return null;

		if (d.ditherTruth) {
			//dither and limit truth
			t = t.dither(nar);
			if (t == null)
				return null;
		}

		if (d.model == DynamicConjTruth.ConjIntersection) {
			//adjust sequence length
            int r = y.eventRange();
			if (s!=ETERNAL && r > 0) {
				if (e - s > (long) r)
                    e = e - (long) r;
			}
		}

		return TruthProjection.merge(d::arrayCommit, y, t, d.stamp(STAMP_CAPACITY, nar.random()), d.beliefOrGoal, s, e, nar);
	}
}