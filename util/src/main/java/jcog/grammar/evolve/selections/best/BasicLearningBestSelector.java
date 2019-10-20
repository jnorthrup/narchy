/*
 * Copyright (C) 2015 Machine Learning Lab - University of Trieste, 
 * Italy (http:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:
 */
package jcog.grammar.evolve.selections.best;

import jcog.grammar.evolve.outputs.FinalSolution;
import jcog.grammar.evolve.outputs.JobEvolutionTrace;
import jcog.grammar.evolve.outputs.Results;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Picks one individual per job (the best one using the evolution fitness); then it evaluates their performance
 * on the learning setAt. The individual with the best performance is promoted as the best one..
 * 
 * @author MaleLabTs
 */
public class BasicLearningBestSelector implements BestSelector {
    
    private static final Logger LOG = Logger.getLogger(BasicLearningBestSelector.class.getName());
   

    @Override
    public void setup(Map<String, String> parameters) {
        
    }

    @Override
    public void elaborate(Results results) {
        BasicLearningBestSelector.selectAndPopulateBest(results);
    }
    
    private static void selectAndPopulateBest(Results results) {
        var bestIndividualIndex = Double.NEGATIVE_INFINITY;
        var bestLength = Integer.MAX_VALUE;
        FinalSolution best = null;
        for (var jobEvolutionTrace : results.getJobEvolutionTraces()) {
            var bestOfJob = jobEvolutionTrace.getFinalGeneration().get(0);
            double precision = bestOfJob.getLearningPerformances().get("match precision");
            double recall = bestOfJob.getLearningPerformances().get("match recall");
            var bestJobLength = bestOfJob.getSolution().length();
            precision = (Double.isNaN(precision))?0:precision;
            recall = (Double.isNaN(recall))?0:recall;
            var individualIndex = (precision+recall)/2;
            if ((individualIndex > bestIndividualIndex) || ((individualIndex == bestIndividualIndex) && (bestLength > bestJobLength))) {
                    bestLength = bestJobLength;
                    best = bestOfJob;
                    bestIndividualIndex = individualIndex;
            }
            
        }
        results.setBestSolution(best);
     
    }
}
