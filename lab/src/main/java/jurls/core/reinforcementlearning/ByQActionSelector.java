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
        ActionValuePair[] ret = Arrays.stream(actionValuePairs).map(actionValuePair1 -> new ActionValuePair(
                actionValuePair1.getA(),
                actionValuePair1.getV()
        )).toArray(ActionValuePair[]::new);

        boolean seen = false;
        double best = 0;
        for (ActionValuePair pair : ret) {
            double valuePair1V = pair.getV();
            if (!seen || Double.compare(valuePair1V, best) < 0) {
                seen = true;
                best = valuePair1V;
            }
        }
        double min = seen ? best : Double.MAX_VALUE;

        for (int i = 0; i < ret.length; ++i) {
            ret[i].setV(ret[i].getV() + min);
        }

        double sum = Arrays.stream(ret).mapToDouble(ActionValuePair::getV).sum();

        for (int i = 0; i < ret.length; ++i) {
            ret[i].setV(ret[i].getV() / sum);
        }

        for (int i = 0; i < ret.length; ++i) {
            double v = ret[i].getV();
            ret[i].setV(
                    
                    v * v
            );
        }

        sum = 0;
        double result = Arrays.stream(ret).mapToDouble(ActionValuePair::getV).sum();
        sum += result;

        for (int i = 0; i < ret.length; ++i) {
            ret[i].setV(ret[i].getV() / sum);
        }
        return ret;
    }

}
