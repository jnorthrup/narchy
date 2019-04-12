package nars.op;

import com.google.common.collect.Iterables;
import jcog.learn.Autoencoder;
import jcog.util.ArrayUtils;
import nars.$;
import nars.NAR;
import nars.agent.Game;
import nars.attention.What;
import nars.concept.Concept;
import nars.concept.sensor.AbstractSensor;
import nars.control.channel.CauseChannel;
import nars.table.BeliefTable;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.List;
import java.util.Set;

import static nars.Op.*;

/**
 * decompiles a continuously trained autoencoding of an input concept vector
 * TODO make DurService
 * TODO extend VectorSensor
 */
public class AutoConceptualizer extends AbstractSensor {

    public final Autoencoder ae;

    private final List<? extends Concept> concepts;

    private final boolean beliefOrGoal;
    private final float[] x;
    private final CauseChannel<ITask> in;
    float learningRate = 0.05f;
    float noiseLevel = 0.0002f;

    public AutoConceptualizer(List<? extends Concept> concepts, boolean beliefOrGoal, int features, NAR n) {
        super(n);
        this.concepts = concepts;
        this.beliefOrGoal = beliefOrGoal;
        this.ae = new Autoencoder(concepts.size(), features, n.random());
        this.x = new float[concepts.size()];
        this.in = n.newChannel(this);
    }


    @Override
    public Iterable<Termed> components() {
        return Iterables.transform(concepts, Termed::term);
    }

    @Override
    public void update(Game g) {

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

        int[] order = new int[inputs];
        for (int i = 0; i < outputs; i++) {
            b[i] = 1; 

            float[] a = ae.decode(b, true);
            
            Term feature = conj(order, a /* threshold, etc */, 3 /*a.length/2*/,
                    thresh);
            if (feature != null) {
                
                onFeature(feature, w);
            }
            b[i] = 0; 
        }
    }

    protected void onFeature(Term feature, What w) {
        SignalTask t = new SignalTask(feature, BELIEF, $.t(1f, 0.9f), nar.time(), nar.time() - nar.dur() / 2, nar.time() + nar.dur() / 2, nar.evidence());
        in.accept(t, w);
    }

    private Term conj(int[] order, float[] a, int maxArity, float threshold) {

        
        int n = a.length;

        for (int i = 0; i < n; i++) {
            order[i] = i;

        }


        float finalMean = 0.5f; 
        ArrayUtils.sort(order, (i) -> Math.abs(finalMean - a[i]));

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


}
