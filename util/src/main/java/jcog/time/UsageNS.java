package jcog.time;

import jcog.Texts;
import jcog.data.list.FasterList;
import jcog.util.HashCachedPair;
import org.HdrHistogram.AtomicHistogram;
import org.eclipse.collections.api.block.procedure.Procedure;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class UsageNS<X> {

    public final Map<X, AtomicHistogram> usage = new ConcurrentHashMap<>();

    {
        Runtime.getRuntime().addShutdownHook(new Thread(this::print));
    }

    protected void print() {
        print(System.out);
    }

    protected void print(PrintStream out) {
        //FasterList<Pair<X, AtomicHistogram>> l = usage.entrySet().stream().map(e -> Tuples.pair(e.getKey(), e.getValue())).collect(toList());
        FasterList<HashCachedPair<X, AtomicHistogram>> fl = new FasterList();
        for (Map.Entry<X, AtomicHistogram> entry : usage.entrySet()) {
            X x = entry.getKey();
            AtomicHistogram h = entry.getValue();
            if (h.getTotalCount() > 0L) {
                fl.add(new HashCachedPair(x, h.copy()));
            }
        }
        fl.sortThis(new Comparator<HashCachedPair<X, AtomicHistogram>>() {
            @Override
            public int compare(HashCachedPair<X, AtomicHistogram> a, HashCachedPair<X, AtomicHistogram> b) {
                if (a == b) return 0;
                AtomicHistogram aa = a.getTwo();
                double am = (double) aa.getTotalCount() * aa.getMean();
                AtomicHistogram bb = b.getTwo();
                double bm = (double) bb.getTotalCount() * bb.getMean();
                int abm = Double.compare(bm, am); //descending
                if (abm != 0) {
                    return abm;
                } else {
                    return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
                }
            }
        });
        fl.forEach(new Procedure<HashCachedPair<X, AtomicHistogram>>() {
            @Override
            public void value(HashCachedPair<X, AtomicHistogram> xh) {

                //out.println(xh.getOne());
                out.println(xh.getTwo().getTotalCount() + "\t*" + Texts.INSTANCE.n4(xh.getTwo().getMean()) + "\t" + xh.getOne());
//            AtomicHistogram h = xh.getTwo();
//            if (h.getTotalCount() > 0) {
//                Texts.histogramPrint(h.copy(), out);
//            } else {
//                out.println("none");
//            }
//           out.println();

            }
        });
    }

    public AtomicHistogram the(X x) {
        return usage.computeIfAbsent(x, new Function<X, AtomicHistogram>() {
            @Override
            public AtomicHistogram apply(X a) {
                return new AtomicHistogram(1L, 1_000_000_000L, 2);
            }
        });
    }
}
