package nars.op;

import jcog.learn.Autoencoder;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.DurService;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jcog.Texts.n4;
import static nars.Op.BELIEF;
import static nars.Op.CONJ;
import static nars.Op.GOAL;

/** decompiles a continuously trained autoencoding of an input concept vector */
public class AutoConceptualizer {
    public final Autoencoder ae;
    public final List<Concept> in;
    private final DurService on;
    private final boolean beliefOrGoal;
    private float[] x;
    float learningRate = 0.1f;
    float noiseLevel = 0.001f;

    public AutoConceptualizer(List<Concept> in, boolean beliefOrGoal, int features, NAR n) {
        this.in = in;
        this.beliefOrGoal = beliefOrGoal;
        this.ae = new Autoencoder(in.size(), features, n.random());
        this.x = new float[in.size()];
        this.on = DurService.on(n, this::update);
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
                f = n.random().nextFloat();
            } else {
                f = t.freq();
            }
            x[i] = f;
        }
        //System.out.println(n4(x));
        float err = ae.put(x, learningRate, noiseLevel, 0, true);
        System.out.println("err=" + n4(err/inputs) + ": \t" + n4(ae.y));

        //decompile/unfactor the outputs
        //if err < thresh
        int outputs = ae.outputs();
        float[] b = new float[outputs];

        for (int i = 0; i < outputs; i++) {
            b[i] = 1; //basis vector for each output

            float[] a = ae.decode(b, true);
            System.out.println("\tfeature " + i + "=" + n4(a));
            Term feature = conj(a /* threshold, etc */, a.length/2, 0.01f);
            if (feature!=null)
                System.out.println("\t  " + feature);
            b[i] = 0; //clear
        }
    }

    private Term conj(float[] a, int maxArity, float threshold) {

        //sort by absolute polarity (divergence from 0.5), collecting the top N components
        int n = a.length;
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }

        jcog.data.array.Arrays.sort(order, (i)->Math.abs(0.5f - a[i]));

        Set<Term> x = new UnifiedSet(maxArity);
        for (int i = 0; i < maxArity; i++) {
            int oi = order[i];
            float aa = a[oi];
            if (Math.abs(aa - 0.5f) < threshold)
                break; //done

            x.add(in.get(oi).term().negIf(aa < 0.5f));
        }

        if (x.isEmpty())
            return null;
        return CONJ.the(0, x);
    }

}
