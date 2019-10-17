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
        ActionValuePair[] ret = Arrays.stream(actionValuePairs).map(actionValuePair -> new ActionValuePair(
                actionValuePair.getA(),
                actionValuePair.getV()
        )).toArray(ActionValuePair[]::new);

        double min = Arrays.stream(ret).mapToDouble(ActionValuePair::getV).min().orElse(Double.MAX_VALUE);

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
        sum += Arrays.stream(ret).mapToDouble(ActionValuePair::getV).sum();

        for (int i = 0; i < ret.length; ++i) {
            ret[i].setV(ret[i].getV() / sum);
        }
        return ret;
    }

}
