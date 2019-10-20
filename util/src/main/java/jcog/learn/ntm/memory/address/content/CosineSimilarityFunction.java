package jcog.learn.ntm.memory.address.content;

import jcog.Util;
import jcog.learn.ntm.control.Unit;
import jcog.math.NumberException;

public class CosineSimilarityFunction implements ISimilarityFunction
{

    double uv;
    double uNorm;
    double vNorm;

    @Override
    public Unit calculate(Unit[] u, Unit[] v) {
        double _uv = (double) 0;
        double _normalizedU = (double) 0, _normalizedV = (double) 0;

        for (int i = 0; i < u.length; i++) {
            double uV = u[i].value;
            double vV = v[i].value;
            _uv += uV * vV;
            _normalizedU += Util.sqr(uV);
            _normalizedV += Util.sqr(vV);
        }
        _normalizedU = Math.sqrt(_normalizedU);
        _normalizedV = Math.sqrt(_normalizedV);

        double value = _uv / (_normalizedU * _normalizedV);
        if (!Double.isFinite(value))
            throw new NumberException("Cosine similarity is nan -> error", value);

        this.uNorm = _normalizedU;
        this.vNorm = _normalizedV;
        this.uv = _uv;

        Unit data = new Unit(value);
        return data;
    }

    @Override
    public void differentiate(Unit similarity, Unit[] uVector, Unit[] vVector) {
        double uvuu = uv / (uNorm * uNorm);
        double uvvv = uv / (vNorm * vNorm);
        double uvg = similarity.grad / (uNorm * vNorm);
        for (int i = 0; i < uVector.length; i++) {
            double u = uVector[i].value;
            double v = vVector[i].value;
            uVector[i].grad += (v - (u * uvuu)) * uvg;
            vVector[i].grad += (u - (v * uvvv)) * uvg;
        }
    }

}


