/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.core.reinforcementlearning;

import jurls.core.utils.ActionValuePair;

import java.util.Arrays;

/**
 *
 * @author thorsten
 */
public class ByQActionSelector implements ActionSelector {

    @Override
    public ActionValuePair[] fromQValuesToProbabilities(double epsilon, ActionValuePair[] actionValuePairs) {
        var ret = Arrays.stream(actionValuePairs).map(actionValuePair1 -> new ActionValuePair(
                actionValuePair1.getA(),
                actionValuePair1.getV()
        )).toArray(ActionValuePair[]::new);

        var seen = false;
        double best = 0;
        for (var pair : ret) {
            var valuePair1V = pair.getV();
            if (!seen || Double.compare(valuePair1V, best) < 0) {
                seen = true;
                best = valuePair1V;
            }
        }
        var min = seen ? best : Double.MAX_VALUE;

        for (var i = 0; i < ret.length; ++i) {
            ret[i].setV(ret[i].getV() + min);
        }

        var sum = Arrays.stream(ret).mapToDouble(ActionValuePair::getV).sum();

        for (var i = 0; i < ret.length; ++i) {
            ret[i].setV(ret[i].getV() / sum);
        }

        for (var i = 0; i < ret.length; ++i) {
            var v = ret[i].getV();
            ret[i].setV(
                    
                    v * v
            );
        }

        sum = 0;
        var result = Arrays.stream(ret).mapToDouble(ActionValuePair::getV).sum();
        sum += result;

        for (var i = 0; i < ret.length; ++i) {
            ret[i].setV(ret[i].getV() / sum);
        }
        return ret;
    }

}
