package nars.term.util.conj;

import jcog.Util;
import jcog.data.bit.MetalBitSet;
import nars.Task;
import nars.term.Term;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntFunction;

import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.dither;

public enum ConjSpans { ;

	//TODO: boolean inclStart, boolean inclEnd, int intermediateDivisions

	/** returns null on failure */
	public static @Nullable ConjBuilder add(List<Task> tt, int dither, MetalBitSet componentPolarity, ConjBuilder b) {
        int n = tt.size();
		if (n == 0)
			return b; //nothing

		if (n == 1) {
			//optimized case
            Task t = tt.get(0);
            long s = t.start();
			if (s == ETERNAL) {
				if (!b.add(ETERNAL, t.term().negIf(!componentPolarity.get(0))))
					return null;
			}
            long e = t.end();
			return b.add(s, t.term()) && ((e==s) || b.add(e, t.term())) ? b : null;
		}


		assert(n < 32); //for MetalBitSet

        LongObjectHashMap<MetalBitSet> inter = new LongObjectHashMap<MetalBitSet>(n*2);

		//collect extents of tasks
        long dur = Long.MAX_VALUE;
		for (int i = 0, ttSize = tt.size(); i < ttSize; i++) {
            Task t = tt.get(i);
            long s = t.start();
			if (s == ETERNAL) {
				if (!b.add(ETERNAL, t.term().negIf(!componentPolarity.get(i))))
					return null;
			}
			s = dither(s, dither, -1);
            long e = dither(t.end(), dither, +1);
			dur = Math.min(dur, e - s);
			inter.getIfAbsentPut(s, MetalBitSet::full).set(i);
		}

		//add adjusted endpoint
		for (int i = 0, ttSize = tt.size(); i < ttSize; i++) {
            Task t = tt.get(i);
            long s = t.start();
			if (s != ETERNAL) {
                long e = dither(t.end() - dur, dither, +1);
				if (e != s) {
					inter.getIfAbsentPut(e, MetalBitSet::full).set(i);
				}
			}
		}

        MutableList<LongObjectPair<MetalBitSet>> w = inter.keyValuesView().toSortedList();
        int wn = w.size();
		//add intermediate overlapping events
		for (int i = 0, ttSize = tt.size(); i < ttSize; i++) {
            Task t = tt.get(i);
            long s = t.start();
			if (s == ETERNAL) continue; //ignore, already added
			s = dither(s, dither, -1);
            long e = dither(t.end() - dur, dither, +1);
			for (int j = 0; j < wn; j++) {
                LongObjectPair<MetalBitSet> ww = w.get(j);
                long wj = ww.getOne();
				if (wj > s && wj < e)
					ww.getTwo().set(i);
			}
		}

        Term[] terms = Util.map(n, Term[]::new, new IntFunction<Term>() {
            @Override
            public Term apply(int I) {
                Task ttt = tt.get(I);
                return ttt.term().negIf(!componentPolarity.get(I));
            }
        });

		//add to builder
		for (int j = 0; j < wn; j++) {
            LongObjectPair<MetalBitSet> ww = w.get(j);
            long W = ww.getOne();
            int k = -1;
			while ((k = ww.getTwo().next(true, k+1, n))!=-1) {
				if (!b.add(W, terms[k]))
					return null;
			}
		}
		return b;
	}
}
