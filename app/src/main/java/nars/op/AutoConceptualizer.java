package nars.op;

import jcog.learn.Autoencoder;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.sensor.AbstractSensor;
import nars.control.DurService;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.List;
import java.util.Set;

import static nars.Op.*;

/**
 * decompiles a continuously trained autoencoding of an input concept vector
 * TODO make DurService
 */
public class AutoConceptualizer extends AbstractSensor {

    public final Autoencoder ae;

    private final List<? extends Concept> in;

    private final DurService on;
    private final boolean beliefOrGoal;
    private final float[] x;
    float learningRate = 0.05f;
    float noiseLevel = 0.0002f;

    public AutoConceptualizer(List<? extends Concept> in, boolean beliefOrGoal, int features, NAR n) {
        super(n);
        this.in = in;
        this.beliefOrGoal = beliefOrGoal;
        this.ae = new Autoencoder(in.size(), features, n.random());
        this.x = new float[in.size()];
        this.on = DurService.on(n, this::update);
    }

    @Override
    public void update(long last, long now, NAR nar) {
        update(nar);
    }

    protected void update(NAR n) {
        long now = n.time();
        float[] x = this.x;
        int inputs = in.size();
        for (int i = 0, inSize = inputs; i < inSize; i++) {
            Concept xx = in.get(i);
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

        int[] order = new int[inputs];
        for (int i = 0; i < outputs; i++) {
            b[i] = 1; 

            float[] a = ae.decode(b, true);
            
            Term feature = conj(order, a /* threshold, etc */, 3 /*a.length/2*/,
                    thresh);
            if (feature != null) {
                
                onFeature(feature);
            }
            b[i] = 0; 
        }
    }

    protected void onFeature(Term feature) {

    }

    private Term conj(int[] order, float[] a, int maxArity, float threshold) {

        
        int n = a.length;

        for (int i = 0; i < n; i++) {
            order[i] = i;

        }


        float finalMean = 0.5f; 
        ArrayUtils.sort(order, (i) -> Math.abs(finalMean - a[i]));

        Set<Term> x = new UnifiedSet(maxArity);
        for (int i = 0; i < maxArity; i++) {
            int oi = order[i];
            float aa = a[oi];
            if (Math.abs(aa - 0.5f) < threshold)
                break; 

            x.add(in.get(oi).term().negIf(aa < finalMean));
        }

        if (x.isEmpty())
            return null;
        return CONJ.the(0, x);
    }


}
