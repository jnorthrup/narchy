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
        double _uv = 0;
        double _normalizedU = 0, _normalizedV = 0;

        for (var i = 0; i < u.length; i++) {
            var uV = u[i].value;
            var vV = v[i].value;
            _uv += uV * vV;
            _normalizedU += Util.sqr(uV);
            _normalizedV += Util.sqr(vV);
        }
        _normalizedU = Math.sqrt(_normalizedU);
        _normalizedV = Math.sqrt(_normalizedV);

        var value = _uv / (_normalizedU * _normalizedV);
        if (!Double.isFinite(value))
            throw new NumberException("Cosine similarity is nan -> error", value);

        this.uNorm = _normalizedU;
        this.vNorm = _normalizedV;
        this.uv = _uv;

        var data = new Unit(value);
        return data;
    }

    @Override
    public void differentiate(Unit similarity, Unit[] uVector, Unit[] vVector) {
        var uvuu = uv / (uNorm * uNorm);
        var uvvv = uv / (vNorm * vNorm);
        var uvg = similarity.grad / (uNorm * vNorm);
        for (var i = 0; i < uVector.length; i++) {
            var u = uVector[i].value;
            var v = vVector[i].value;
            uVector[i].grad += (v - (u * uvuu)) * uvg;
            vVector[i].grad += (u - (v * uvvv)) * uvg;
        }
    }

}


