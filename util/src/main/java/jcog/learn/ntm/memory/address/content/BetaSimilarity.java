package jcog.learn.ntm.memory.address.content;

import jcog.learn.ntm.control.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;


public class BetaSimilarity extends Unit {
    public final Unit _beta;

    public final SimilarityMeasure measure;

    

    /** Key strength beta */
    private final double B;

    public BetaSimilarity(Unit beta, SimilarityMeasure m) {
        super(0);
        _beta = beta;
        measure = m;
        
        B = Math.exp(_beta.value);





        this.value  = (m != null) ? (B * m.similarity.value) : 0.0;
    }

    public BetaSimilarity() {
        this(new Unit(0.0), null);
    }

    public void backwardErrorPropagation() {
        Unit similarity = measure.similarity;
        double betaGradient = grad;
        _beta.grad += similarity.value * B * betaGradient;
        similarity.grad += B * betaGradient;
    }

    public static BetaSimilarity[][] getTensor2(int x, int y) {
        List<BetaSimilarity[]> list = new ArrayList<>();
        for (int i = 0; i < x; i++) {
            BetaSimilarity[] vector = getVector(y);
            list.add(vector);
        }
        BetaSimilarity[][] tensor = list.toArray(new BetaSimilarity[0][]);

        return tensor;
    }

    public static BetaSimilarity[] getVector(int x) {
        List<BetaSimilarity> list = new ArrayList<>();
        for (int i = 0; i < x; i++) {
            BetaSimilarity betaSimilarity = new BetaSimilarity();
            list.add(betaSimilarity);
        }
        BetaSimilarity[] vector = list.toArray(new BetaSimilarity[0]);
        return vector;
    }

}


