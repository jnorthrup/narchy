package jcog.learn.ntm.memory.address.content;

import jcog.learn.ntm.control.Unit;


public class SimilarityMeasure   
{
    private final ISimilarityFunction simliarityFunc;
    private final Unit[] _u;
    private final Unit[] _v;
    public final Unit similarity;

    public SimilarityMeasure(ISimilarityFunction similarityFunction, Unit[] u, Unit[] v) {
        simliarityFunc = similarityFunction;
        _u = u;
        _v = v;
        similarity = similarityFunction.calculate(u, v);
    }

    public void backwardErrorPropagation() {
        simliarityFunc.differentiate(similarity,_u,_v);
    }

}


