package nars.truth.util;

import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

/** thread-safe truth accumulator/integrator
 *  TODO implement Truth interface, rename to ConcurrentTruth, extend AtomicDoubleArray
 * */
public class TruthAccumulator extends AtomicReference<double[]> {

    public TruthAccumulator() {
        commit();
    }

    public @Nullable Truth commitAverage() {
        return truth(commit(), false);
    }
    public @Nullable Truth commitSum() {
        return truth(commit(), true);
    }

    public double[] commit() {
        return getAndSet(new double[3]);
    }

    public PreciseTruth peekSum() {
        return truth(get(), true);
    }
    public @Nullable Truth peekAverage() {
        return truth(get(), false);
    }

    private static @Nullable PreciseTruth truth(double[] fc, boolean sumOrAverage) {

        double e = fc[1];
        if (e <= (double) 0)
            return null;

        int n = (int)fc[2];
        float ee = ((sumOrAverage) ? ((float)e) : ((float)e)/ (float) n);
        return PreciseTruth.byEvi((float)(fc[0]/e), (double) ee);
    }


    public void add(@Nullable Truth t) {

        if (t == null)
            return;


        double f = (double) t.freq();
        double e = t.evi();
        add(f, e);
    }

    private void add(double f, double e) {
        double fe = f * e;

        getAndUpdate(new UnaryOperator<double[]>() {
            @Override
            public double[] apply(double[] fc) {
                fc[0] += fe;
                fc[1] += e;
                fc[2] += 1.0;
                return fc;
            }
        });
    }


    @Override
    public String toString() {
        Truth t = peekSum();
        return t!=null ? t.toString() : "null";
    }

}
