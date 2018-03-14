package nars.concept.dynamic;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.list.FasterList;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.table.TemporalBeliefTable;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import static nars.time.Tense.ETERNAL;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/** dynamically computes matching truths and tasks according to
 * a lossy 1-D wave updated directly by a signal input
 */
public class ScalarBeliefTable extends DynamicBeliefTable {

    /** prioritizes generated tasks */
    private FloatSupplier pri;

    /** shared stamp for the entire curve */
    final long[] stamp;

    static interface TimeSeries {

        void add(long time, Truth value);

        PreciseTruth truth(long start, long end, long dur);
    }

    /** naive implementation using a NavigableMap of indxed time points. not too smart since it cant represent mergeable flat ranges */
    static class DefaultTimeSeries implements TimeSeries {

        final NavigableMap<Long, Truth> at;
        private final int cap;

        DefaultTimeSeries(NavigableMap<Long, Truth> at, int cap) {
            this.at = at;
            this.cap = cap;
        }

        @Override
        public void add(long time, Truth value) {
            synchronized (at) {
                while (at.size()+1 > cap)
                    at.remove(at.firstKey());

                at.put(time, value);
            }
        }

        @Override
        public PreciseTruth truth(long start, long end, long dur) {
            //TruthPolation p = new TruthPolation(start, end, 1,

            if (at.isEmpty())
                return null;

            final float[] freqSum = {0};
            final float[] confSum = {0};
            final int[] ccount = {0};
            at.subMap(start, end).values().forEach(t -> {
                //TODO weight frequency by evi/conf
                freqSum[0] += t.freq();
                confSum[0] += t.conf();
                ccount[0]++;
            });

            int count = ccount[0];
            if (count > 0) {
                return new PreciseTruth(freqSum[0] / count, confSum[0] / count);
            } else {
                //TODO optimize for start==end

                Set<Long> s = Sets.mutable.of(
                        at.ceilingKey(start), at.floorKey(start),
                        at.ceilingKey(end), at.floorKey(end)
                );
                s.remove(null); //HACK

                if (s.isEmpty()) return null;

                LongObjectPair<Truth> b;
                if (s.size()==1) {
                    long bl = s.iterator().next();
                    Truth y = at.get(bl);
                    if (y!=null) // in case removed while calculating
                        b = pair(bl, y);
                    else
                        b = null;
                } else {
                    FasterList<LongObjectPair<Truth>> l = new FasterList<>(
                            Iterables.transform(s, x -> {
                                Truth y = at.get(x);
                                if (y!=null) // in case removed while calculating
                                    return pair(x, y);
                                else
                                    return null;
                            }
                    ));
                    b = l.max((c1, c2) -> {
                        long d1, d2;
                        if (c1 == null) d1 = Long.MAX_VALUE;
                        else {
                            long d1s = Math.abs(start - c1.getOne());
                            long d1e = Math.abs(end - c1.getOne());
                            d1 = Math.min(d1s, d1e);
                        }

                        if (c2 == null) d2 = Long.MAX_VALUE;
                        else {
                            long d2s = Math.abs(start - c2.getOne());
                            long d2e = Math.abs(end - c2.getOne());
                            d2 = Math.min(d2s, d2e);
                        }

                        return Long.compare(d2, d1); //reverse
                    });
                }
                if (b == null)
                    return null;

                long be = b.getOne();
                long dist = Math.min(Math.abs(be-start), Math.abs(be-end));
                Truth bt = b.getTwo();
                return bt.withEvi((float) Param.evi(bt.evi(), dist, dur ));

            }
        }
    }

    final TimeSeries series = new DefaultTimeSeries(new ConcurrentSkipListMap<>(), 512);

    public ScalarBeliefTable(Term c, boolean beliefOrGoal, TemporalBeliefTable t, long stamp) {
        super(c, beliefOrGoal, t);
        this.stamp = new long[] { stamp };
    }

    @Override
    public boolean add(Task input, TaskConcept concept, NAR nar) {

        long start = input.start();
        if (start!=ETERNAL) {
            //any truth stored in the history here is more or less considered the ultimate authority on this sensor
            Truth override = truthDynamic(start, input.end(), nar);
            if (override != null) {
                //TODO feedback absorb
                return false;
            }
        }

        return super.add(input, concept, nar);
    }

    @Override
    public Task matchDynamic(long start, long end, Term template, NAR nar) {
        Truth t = truthDynamic(start, end, nar);
        return t!=null ?
                new DynTruth.DynTruthTask(term, beliefOrGoal, t, nar, start, end, stamp(start, end, Param.STAMP_CAPACITY))
                    .pri(pri.asFloat())
                : null;
    }

    /** time units per stamp, must/should be absolute */
    static final int stampResolution = 8;

    private long[] stamp(long start, long end, int stampCapacity) {
        long sstart = stamp[0] + (start / stampResolution);
        long send = stamp[0] + (end / stampResolution);
        if (sstart == send)
            return new long[] { sstart };

        long x = Util.round(sstart, stampResolution);
        long range = end - start;
        int inc = Math.max(stampResolution, (int) Util.round(range / (stampCapacity * stampResolution), stampResolution));
        LongArrayList l = new LongArrayList((int)(1 + range/inc)); //TODO try to make exact capacity size
        do {
            l.add(x);
            x += inc;
        } while (x <= send);
        assert(l.size() <= stampCapacity);
        return l.toArray();
    }

    @Override
    protected @Nullable Truth truthDynamic(long start, long end, NAR nar) {
        return series.truth(start, end, nar.dur());
    }

    @Override
    protected @Nullable Term template(long start, long end, Term template, NAR nar) {
        return term;
    }

    public void pri(FloatSupplier pri) {
        this.pri = pri;
    }

    public void update(Truth value, long time, int dur) {
        series.add(time, value);
    }
}
