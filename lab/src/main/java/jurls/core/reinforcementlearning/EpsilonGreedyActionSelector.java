/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.core.reinforcementlearning;

import jurls.core.utils.ActionValuePair;

/**
 *
 * @author thorsten
 */
public class EpsilonGreedyActionSelector implements ActionSelector {

    @Override
    public ActionValuePair[] fromQValuesToProbabilities(double epsilon, ActionValuePair[] actionValuePairs) {
        var bestPair = 0;

        for (var i = 0; i < actionValuePairs.length; ++i) {
            if (actionValuePairs[i].getV() > actionValuePairs[bestPair].getV()) {
                bestPair = i;
            }
        }


        var ret = new ActionValuePair[actionValuePairs.length];

        for (var i = 0; i < actionValuePairs.length; ++i) {
            ret[i] = new ActionValuePair(
                    actionValuePairs[i].getA(),
                    i == bestPair ? 1 - epsilon : epsilon / (ret.length - 1)
            );
        }

        return ret;
    }

}
