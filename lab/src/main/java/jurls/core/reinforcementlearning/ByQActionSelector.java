/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.core.reinforcementlearning;

import jurls.core.utils.ActionValuePair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author thorsten
 */
public class ByQActionSelector implements ActionSelector {

    @Override
    public ActionValuePair[] fromQValuesToProbabilities(double epsilon, ActionValuePair[] actionValuePairs) {
        List<ActionValuePair> list = new ArrayList<>();
        for (ActionValuePair actionValuePair1 : actionValuePairs) {
            ActionValuePair valuePair1 = new ActionValuePair(
                    actionValuePair1.getA(),
                    actionValuePair1.getV()
            );
            list.add(valuePair1);
        }
        ActionValuePair[] ret = list.toArray(new ActionValuePair[0]);

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

        double sum = 0.0;
        for (ActionValuePair valuePair : ret) {
            double actionValuePair1V = valuePair.getV();
            sum += actionValuePair1V;
        }

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
        double result = 0.0;
        for (ActionValuePair actionValuePair : ret) {
            double v = actionValuePair.getV();
            result += v;
        }
        sum += result;

        for (int i = 0; i < ret.length; ++i) {
            ret[i].setV(ret[i].getV() / sum);
        }
        return ret;
    }

}
