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
package jcog.grammar.evolve.objective.performance;

import jcog.grammar.evolve.configuration.Configuration;
import jcog.grammar.evolve.evaluators.TreeEvaluationException;
import jcog.grammar.evolve.evaluators.TreeEvaluator;
import jcog.grammar.evolve.inputs.Context;
import jcog.grammar.evolve.inputs.DataSet;
import jcog.grammar.evolve.inputs.DataSet.Bounds;
import jcog.grammar.evolve.inputs.DataSet.Example;
import jcog.grammar.evolve.objective.Objective;
import jcog.grammar.evolve.outputs.FinalSolution;
import jcog.grammar.evolve.tree.Constant;
import jcog.grammar.evolve.tree.Node;
import jcog.grammar.evolve.utils.BasicStats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is not a fitness, but returns individual performances like in a
 * mutli-objective fitness style. fitness[0] = precision fitness[1] = recall
 * fitness[2] = charPrecision fitness[3] = charRecall fitness[4] = charAccuracy
 * fitness[5] = match fmeasure
 *
 * @author MaleLabTs
 */
public class PerformacesObjective implements Objective {

    private Context context;
    

    
    @Override
    public void setup(Context context) {
        this.context = context;
        
        
    }

    @Override
    public double[] fitness(Node individual) {
        DataSet dataSetView = this.context.getCurrentDataSet();
        TreeEvaluator evaluator = context.getConfiguration().getEvaluator();
        double[] fitness = new double[12];
        List<Bounds[]> evaluate;
        try {
            evaluate = evaluator.evaluate(individual, context);
        } catch (TreeEvaluationException ex) {
            Logger.getLogger(PerformacesObjective.class.getName()).log(Level.SEVERE, null, ex);
            Arrays.fill(fitness, Double.POSITIVE_INFINITY);
            return fitness;
        }


        BasicStats statsOverall = new BasicStats();


        BasicStats statsCharsOverall = new BasicStats();


        BasicStats statsOverallFlagging = new BasicStats();

        int i = 0;
        for (Bounds[] result : evaluate) {
            BasicStats stats = new BasicStats();
            BasicStats statsChars = new BasicStats();
            BasicStats statsFlagging = new BasicStats();


            Example example = dataSetView.getExample(i);
            List<Bounds> expectedMatchMask = example.getMatch();
            List<Bounds> expectedUnmatchMask = example.getUnmatch();
            List<Bounds> annotatedMask = new ArrayList<>(expectedMatchMask);
            annotatedMask.addAll(expectedUnmatchMask);

            stats.tp = (long) countIdenticalRanges(result, expectedMatchMask);
            stats.fp = (long) Bounds.countRangesThatCollideZone(result, annotatedMask) - stats.tp;
            statsChars.tp = (long) intersection(result, expectedMatchMask);
            statsChars.fp = (long) intersection(result, expectedUnmatchMask);

            
            
            if (!isUnannotated(example)){
                statsFlagging.tp = (long) (isTruePositive(result, example.match) ? 1 : 0);
                statsFlagging.fp = (long) (isFalsePositive(result, example.unmatch) ? 1 : 0);
                statsFlagging.fn = (long) (isFalseNegative(result, example.match) ? 1 : 0);
                statsFlagging.tn = (long) (isTrueNegative(result, example.unmatch) ? 1 : 0);
                statsOverallFlagging.add(statsFlagging);
            }
            
            statsOverall.add(stats);
            statsCharsOverall.add(statsChars);
            i++;
        }

        statsCharsOverall.tn = (long) dataSetView.getNumberUnmatchedChars() - statsCharsOverall.fp;
        statsCharsOverall.fn = (long) dataSetView.getNumberMatchedChars() - statsCharsOverall.tp;

        double charAccuracy = statsCharsOverall.accuracy();
        double charPrecision = statsCharsOverall.precision();
        double charRecall = statsCharsOverall.recall();
        double precision = statsOverall.precision();
        double recall = statsOverall.recall(dataSetView.getNumberMatches());

        fitness[0] = precision;
        fitness[1] = recall;
        fitness[2] = charPrecision;
        fitness[3] = charRecall;
        fitness[4] = charAccuracy;
        double fmeasure = 2.0 * (precision * recall) / (precision + recall);
        fitness[5] = fmeasure;
        
        fitness[6] = statsOverallFlagging.accuracy();
        fitness[7] = statsOverallFlagging.fpr();
        fitness[8] = statsOverallFlagging.fnr();
        fitness[9] = statsOverallFlagging.precision();
        fitness[10] = statsOverallFlagging.recall();
        fitness[11] = statsOverallFlagging.fMeasure();

        return fitness;
    }

    
    private static int intersection(Bounds[] extractedRanges, List<Bounds> expectedRanges) {
        int overallNumChars = 0;

        for (Bounds extractedBounds : extractedRanges) {

            int extractedEnd = extractedBounds.end;
            int extractedStart = extractedBounds.start;

            int sum = 0;
            for (Bounds expectedBounds : expectedRanges) {
                int numChars = Math.min(extractedEnd, expectedBounds.end) -
                        Math.max(extractedStart, expectedBounds.start);
                int max = Math.max(0, numChars);
                sum += max;
            }
            overallNumChars += sum;
        }
        return overallNumChars;
    }

    
    private static int countIdenticalRanges(Bounds[] rangesA, List<Bounds> rangesB) {
        int identicalRanges = (int) Arrays.stream(rangesA).filter(new Predicate<Bounds>() {
            @Override
            public boolean test(Bounds boundsA) {
                for (Bounds bounds : rangesB) {
                    if (boundsA.equals(bounds)) {
                        return true;
                    }
                }
                return false;
            }
        }).count();

        return identicalRanges;
    }

    @Override
    public TreeEvaluator getTreeEvaluator() {
        return context.getConfiguration().getEvaluator();
    }

    @Override
    public Objective cloneObjective() {
        return new PerformacesObjective();
    }

    public static void populatePerformancesMap(double[] performances, Map<String, Double> performancesMap, boolean isFlagging) {
        if(!isFlagging){
            performancesMap.put("match precision", performances[0]);
            performancesMap.put("match recall", performances[1]);
            performancesMap.put("character precision", performances[2]);
            performancesMap.put("character recall", performances[3]);
            performancesMap.put("character accuracy", performances[4]);
            performancesMap.put("match f-measure", performances[5]);
        } else {
            
            performancesMap.put("flag accuracy", performances[6]);
            performancesMap.put("flag fpr", performances[7]);
            performancesMap.put("flag fnr", performances[8]);
            performancesMap.put("flag precision", performances[9]);
            performancesMap.put("flag recall", performances[10]);
            performancesMap.put("flag f-measure", performances[11]);
        } 
    }

    /**
     * Populate the the performances of finalSolution for the requested phase
     * (training, validation, training)
     *
     * @param phase training, validation or learning phase
     * @param configuration the configuration instance
     * @param finalSolution
     */
    public static void populateFinalSolutionPerformances(Context.EvaluationPhases phase, Configuration configuration, FinalSolution finalSolution, boolean isFlagging) {
        Objective phaseObjective = PerformancesFactory.buildObjective(phase, configuration);
        Node finalTree = new Constant(finalSolution.getSolution());
        double[] phasePerformaceRoughtValues = phaseObjective.fitness(finalTree);
        Map<String, Double> phasePerformances = null; 
        switch (phase) {
            case TRAINING:
                phasePerformances = finalSolution.getTrainingPerformances();
                break;
            case VALIDATION:
                phasePerformances = finalSolution.getValidationPerformances();
                break;
            case LEARNING:
                phasePerformances = finalSolution.getLearningPerformances();
                break;
        }

        PerformacesObjective.populatePerformancesMap(phasePerformaceRoughtValues, phasePerformances, isFlagging);
    }

    public static boolean isUnannotated(Example ex) {
        return ex.match.isEmpty() && ex.unmatch.isEmpty();
    }

    public static boolean isTruePositive(Bounds[] individualMatches, List<Bounds> expectedMatches) {
        return individualMatches.length > 0 && !expectedMatches.isEmpty();
    }

    public static boolean isFalsePositive(Bounds[] individualMatches, List<Bounds> expectedUnmatches) {
        return individualMatches.length > 0 && !expectedUnmatches.isEmpty();
    }

    public static boolean isFalseNegative(Bounds[] individualMatches, List<Bounds> expectedMatches) {
        return individualMatches.length == 0 && !expectedMatches.isEmpty();
    }

    public static boolean isTrueNegative(Bounds[] individualMatches, List<Bounds> expectedUnmatches) {
        return individualMatches.length == 0  && !expectedUnmatches.isEmpty();
    }
}
