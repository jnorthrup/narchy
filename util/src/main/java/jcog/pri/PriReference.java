package jcog.pri;

import jcog.Util;
import jcog.math.FloatSupplier;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * prioritized reference
 */
public interface PriReference<X> extends Prioritizable, Supplier<X>, FloatSupplier {

    static float[] histogram(Iterable<? extends Prioritized> pp, float[] x) {
        var bins = x.length;
        double[] total = {0};

        for (Prioritized y : pp) {
            if (y == null)
                continue;
            var p = y.priElseZero();
            if (p > 1f) p = 1f;
            var b = Util.bin(p, bins);
            x[b]++;
            total[0]++;
        }

        var t = total[0];
        if (t > 0) {
            for (var i = 0; i < bins; i++)
                x[i] /= t;
        }
        return x;
    }

    /**
     * double[histogramID][bin]
     */
    static <X, Y> double[][] histogram( Iterable<PriReference<Y>> pp,  BiConsumer<PriReference<Y>, double[][]> each,  double[][] d) {

        for (var y : pp) {
            each.accept(y, d);
        }

        for (var e : d) {
            var total = Arrays.stream(e).sum();
            if (total > 0) {
                for (int i = 0, eLength = e.length; i < eLength; i++) {
                    var f = e[i];
                    e[i] /= total;
                }
            }
        }

        return d;
    }

    @Override
    default float asFloat() {
        return pri();
    }
}

