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
import nars.task.util.signal.SignalTask;
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

        NAR n = g.nar;
        long now = n.time();
        float[] x = this.x;
        int inputs = concepts.size();
        for (int i = 0, inSize = inputs; i < inSize; i++) {
            Concept xx = concepts.get(i);
            Truth t = ((BeliefTable) xx.table(beliefOrGoal ? BELIEF : GOAL)).truth(now, n);
            float f;
            if (t == null) {
                f = 0.5f;
            } else {
                f = t.freq();
            }
            x[i] = f;
        }
        
        float err = ae.put(x, learningRate, noiseLevel, 0, true);
        
        int outputs = ae.outputs();
        float[] b = new float[outputs];

        float thresh = n.freqResolution.floatValue();

        What w = g.what();

        float dur = w.dur();
        long start = Math.round(now - dur / 2), end = Math.round(now + dur / 2);
        int[] order = new int[inputs];
        Truth truth = $.t(1f, 0.9f);
        for (int i = 0; i < outputs; i++) {
            b[i] = 1; 

            float[] a = ae.decode(b, true);
            
            Term feature = conj(order, a /* threshold, etc */, 3 /*a.length/2*/,
                    thresh);
            if (feature != null)
                w.accept(onFeature(feature, truth, now, start, end, n.evidence()));

            b[i] = 0; 
        }
    }

    protected SignalTask onFeature(Term feature, Truth truth, long now, long start, long end, long[] evi) {
        if (feature instanceof Neg) {
            feature = feature.unneg();
            truth = truth.neg();
        }
        return new SignalTask(feature, BELIEF, truth,
                now,
                start,end, evi);
    }

    private Term conj(int[] order, float[] a, int maxArity, float threshold) {

        
        int n = a.length;

        for (int i = 0; i < n; i++) {
            order[i] = i;

        }


        float finalMean = 0.5f; 
        QuickSort.sort(order, (i) -> Math.abs(finalMean - a[i]));

        Set<Term> x = new UnifiedSet(maxArity);
        int j = 0;
        for (int i = 0; i < order.length && j < maxArity; i++) {
            int oi = order[i];
            float aa = a[oi];
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
