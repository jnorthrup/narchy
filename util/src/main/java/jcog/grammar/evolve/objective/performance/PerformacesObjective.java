/*
 * Copyright (C) 2015 Machine Learning Lab - University of Trieste, 
 * Italy (http://machinelearning.inginf.units.it/)  
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
    //private DataSet dataSetView;

    //private int numberCharsInMatches = 0;
    @Override
    public void setup(Context context) {
        this.context = context;
        //this.dataSetView = this.context.getCurrentDataSet();
        //this.dataSetView.populateMatchesStrings();
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

        //match stats makes sense only for tp e fp values... we cannot use instance statistic formulas other than precision
        BasicStats statsOverall = new BasicStats();

        //char stats can be managed as ususal
        BasicStats statsCharsOverall = new BasicStats();

        //Flagging
        BasicStats statsOverallFlagging = new BasicStats();

        int i = 0;
        for (Bounds[] result : evaluate) {
            BasicStats stats = new BasicStats();
            BasicStats statsChars = new BasicStats();
            BasicStats statsFlagging = new BasicStats();

            //Characted extracted in the right place (match)
            Example example = dataSetView.getExample(i);
            List<Bounds> expectedMatchMask = example.getMatch();
            List<Bounds> expectedUnmatchMask = example.getUnmatch();
            List<Bounds> annotatedMask = new ArrayList<>(expectedMatchMask);
            annotatedMask.addAll(expectedUnmatchMask);

            stats.tp = countIdenticalRanges(result, expectedMatchMask);
            stats.fp = Bounds.countRangesThatCollideZone(result, annotatedMask) - stats.tp;
            statsChars.tp = intersection(result, expectedMatchMask);
            statsChars.fp = intersection(result, expectedUnmatchMask);

            //Flagging
            
            if (!isUnannotated(example)){
                statsFlagging.tp = isTruePositive(result, example.match) ? 1 : 0;
                statsFlagging.fp = isFalsePositive(result, example.unmatch) ? 1 : 0;
                statsFlagging.fn = isFalseNegative(result, example.match) ? 1 : 0;
                statsFlagging.tn = isTrueNegative(result, example.unmatch) ? 1 : 0;
                statsOverallFlagging.add(statsFlagging);
            }
            
            statsOverall.add(stats);
            statsCharsOverall.add(statsChars);
            i++;
        }

        statsCharsOverall.tn = dataSetView.getNumberUnmatchedChars() - statsCharsOverall.fp;
        statsCharsOverall.fn = dataSetView.getNumberMatchedChars() - statsCharsOverall.tp;

        double charAccuracy = statsCharsOverall.accuracy(); //Chars extraction accuracy
        double charPrecision = statsCharsOverall.precision(); //Chars precision (how many extracted chars are in positive matches?)
        double charRecall = statsCharsOverall.recall(); //Chars recall (how many extracted chars vs overall chars in positive matches)
        double precision = statsOverall.precision(); //How many extractions are correct?
        double recall = statsOverall.recall(dataSetView.getNumberMatches()); //Right extractions vs overall matches number
        double fmeasure = 2 * (precision * recall) / (precision + recall);

        fitness[0] = precision;
        fitness[1] = recall;
        fitness[2] = charPrecision;
        fitness[3] = charRecall;
        fitness[4] = charAccuracy;
        fitness[5] = fmeasure;
        //Flagging
        fitness[6] = statsOverallFlagging.accuracy();
        fitness[7] = statsOverallFlagging.fpr();
        fitness[8] = statsOverallFlagging.fnr();
        fitness[9] = statsOverallFlagging.precision();
        fitness[10] = statsOverallFlagging.recall();
        fitness[11] = statsOverallFlagging.fMeasure();

        return fitness;
    }

    //Returns number of chars of this extracted ranges which falls into expected ranges
    private static int intersection(Bounds[] extractedRanges, List<Bounds> expectedRanges) {
        int overallNumChars = 0;

        for (Bounds extractedBounds : extractedRanges) {

            int extractedEnd = extractedBounds.end;
            int extractedStart = extractedBounds.start;

            for (Bounds expectedBounds : expectedRanges) {

                int numChars = Math.min(extractedEnd, expectedBounds.end) -
                        Math.max(extractedStart, expectedBounds.start);

                overallNumChars += Math.max(0, numChars);

            }
        }
        return overallNumChars;
    }

    //Rerurns the number of idential intervals in two list of ranges
    private static int countIdenticalRanges(Bounds[] rangesA, List<Bounds> rangesB) {
        int identicalRanges = 0;

        for (Bounds boundsA : rangesA) {
            for (Bounds boundsB : rangesB) {
                if (boundsA.equals(boundsB)) {
                    identicalRanges++;
                    break;
                }
            }
        }
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
            //flagging performances
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
        Map<String, Double> phasePerformances = null; //let it explode in case of errors
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
