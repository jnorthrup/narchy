package nars.op;

import jcog.learn.Autoencoder;
import jcog.sort.QuickSort;
import nars.$;
import nars.NAR;
import nars.attention.What;
import nars.concept.Concept;
import nars.game.Game;
import nars.game.sensor.ComponentSignal;
import nars.game.sensor.VectorSensor;
import nars.table.BeliefTable;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.TemporalTask;
import nars.term.Neg;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static nars.Op.*;

/**
 * decompiles a continuously trained autoencoding of an input concept vector
 * TODO make DurService
 * TODO extend VectorSensor
 */
public class AutoConceptualizer extends VectorSensor {

    public final Autoencoder ae;

    private final List<ComponentSignal> concepts;

    private final boolean beliefOrGoal;
    private final float[] x;
    float learningRate = 0.05f;
    float noiseLevel = 0.0002f;

    public AutoConceptualizer(Term id, List<ComponentSignal> concepts, boolean beliefOrGoal, int features, NAR n) {
        super(id, n);
        this.concepts = concepts;
        this.beliefOrGoal = beliefOrGoal;
        this.ae = new Autoencoder(concepts.size(), features, n.random());
        this.x = new float[concepts.size()];
    }

    @Override
    public int size() {
        return concepts.size();
    }


    @Override
    public void accept(Game g) {

        var n = g.nar;
        var now = n.time();
        var x = this.x;
        var inputs = concepts.size();
        for (int i = 0, inSize = inputs; i < inSize; i++) {
            Concept xx = concepts.get(i);
            var t = ((BeliefTable) xx.table(beliefOrGoal ? BELIEF : GOAL)).truth(now, n);
            float f;
            if (t == null) {
                f = 0.5f;
            } else {
                f = t.freq();
            }
            x[i] = f;
        }

        var err = ae.put(x, learningRate, noiseLevel, 0, true);

        var outputs = ae.outputs();
        var b = new float[outputs];

        var thresh = n.freqResolution.floatValue();

        var w = g.what();

        var dur = w.dur();
        long start = Math.round(now - dur / 2), end = Math.round(now + dur / 2);
        var order = new int[inputs];
        Truth truth = $.t(1f, 0.9f);
        for (var i = 0; i < outputs; i++) {
            b[i] = 1;

            var a = ae.decode(b, true);

            var feature = conj(order, a /* threshold, etc */, 3 /*a.length/2*/,
                    thresh);
            if (feature != null)
                w.accept(onFeature(feature, truth, start, end, n.evidence()));

            b[i] = 0; 
        }
    }

    protected static TemporalTask onFeature(Term feature, Truth truth, long start, long end, long[] evi) {
        if (feature instanceof Neg) {
            feature = feature.unneg();
            truth = truth.neg();
        }
        return new SeriesBeliefTable.SeriesTask(feature, BELIEF, truth, start, end, evi);
    }

    private Term conj(int[] order, float[] a, int maxArity, float threshold) {


        var n = a.length;
        for (var i = 0; i < n; i++)
            order[i] = i;

        var finalMean = 0.5f;
        QuickSort.sort(order, (i) -> Math.abs(finalMean - a[i]));

        Set<Term> x = new UnifiedSet(maxArity);
        var j = 0;
        for (var i = 0; i < order.length && j < maxArity; i++) {
            var oi = order[i];
            var aa = a[oi];
            if (Math.abs(aa - 0.5f) < threshold)
                break; 

            x.add(concepts.get(oi).term().negIf(aa < finalMean));
            j++;
        }

        if (x.isEmpty())
            return null;
        return CONJ.the(0, x);
    }

    @Override
    public Iterator<ComponentSignal> iterator() {
        return concepts.iterator();
    }
}
