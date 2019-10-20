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
package jcog.grammar.evolve.objective;

import jcog.grammar.evolve.evaluators.TreeEvaluationException;
import jcog.grammar.evolve.evaluators.TreeEvaluator;
import jcog.grammar.evolve.inputs.Context;
import jcog.grammar.evolve.inputs.DataSet;
import jcog.grammar.evolve.inputs.DataSet.Bounds;
import jcog.grammar.evolve.inputs.DataSet.Example;
import jcog.grammar.evolve.tree.Node;
import jcog.grammar.evolve.utils.BasicStats;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * MultiObjective fitness, char fpr + fnr, ABS(Number_Extractions -
 * Number_Matches)), and regex length: three fitnesses, three objectives {FPR +
 * FNR, ABS(Number_Extractions - Number_Matches)), regexLength}
 *
 * @author MaleLabTs
 */
public class CharmaskMatchLengthObjective implements Objective {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger(CharmaskMatchLengthObjective.class);

    private Context context;
    
    @Override
    public void setup(Context context) {
        this.context = context;
        
    }

    @Override
    public double[] fitness(Node individual) {
        DataSet dataSetView = this.context.getCurrentDataSet();

        TreeEvaluator evaluator = context.getConfiguration().getEvaluator();
        double[] fitness = new double[3];

        double fitnessLenght;

        List<Bounds[]> evaluate;
        try {
            evaluate = evaluator.evaluate(individual, context);
            StringBuilder builder = new StringBuilder();
            individual.describe(builder);
            fitnessLenght = (double) builder.length();
        } catch (TreeEvaluationException ex) {
            logger.error("{}", ex);
            Arrays.fill(fitness, Double.POSITIVE_INFINITY);
            return fitness;
        }


        BasicStats statsOverall = new BasicStats();


        BasicStats statsCharsOverall = new BasicStats();

        int i = 0;
        for (Bounds[] result : evaluate) {
            BasicStats stats = new BasicStats();
            BasicStats statsChars = new BasicStats();

            Example example = dataSetView.getExample(i);
            if (example == null)
                continue;

            List<Bounds> expectedMatchMask = example.getMatch();
            List<Bounds> expectedUnmatchMask = example.getUnmatch();
            List<Bounds> annotatedMask = new FastList(expectedMatchMask);
            annotatedMask.addAll(expectedUnmatchMask);

            stats.tp = (long) countIdenticalRanges(result, expectedMatchMask);
            stats.fp = (long) Bounds.countRangesThatCollideZone(result, annotatedMask) - stats.tp;
            statsChars.tp = (long) intersection(result, expectedMatchMask);
            statsChars.fp = (long) intersection(result, expectedUnmatchMask);

            statsOverall.add(stats);
            statsCharsOverall.add(statsChars);
            i++;
        }

        statsCharsOverall.tn = (long) dataSetView.getNumberUnmatchedChars() - statsCharsOverall.fp;
        statsCharsOverall.fn = (long) dataSetView.getNumberMatchedChars() - statsCharsOverall.tp;

        fitness[0] = (statsCharsOverall.fpr() + statsCharsOverall.fnr()) * 100.0;
        fitness[1] = (double) Math.abs(statsOverall.fp + statsOverall.tp - (long) dataSetView.getNumberMatches());
        fitness[2] = fitnessLenght;

        return fitness;
    }

    
    private static int intersection(Bounds[] extractedRanges, List<Bounds> expectedRanges) {
        int overallNumChars = 0;
         
        for (Bounds extractedBounds : extractedRanges) {
            for (int i = 0, expectedRangesSize = expectedRanges.size(); i < expectedRangesSize; i++) {
                Bounds expectedBounds = expectedRanges.get(i);
                int numChars = Math.min(extractedBounds.end, expectedBounds.end) - Math.max(extractedBounds.start, expectedBounds.start);
                overallNumChars += Math.max(0, numChars);
            }
        }
        return overallNumChars;
    }

    
    private static int countIdenticalRanges(Bounds[] rangesA, List<Bounds> rangesB) {
        int identicalRanges = 0;
         
        for (Bounds boundsA : rangesA) {
            for (int i = 0, rangesBSize = rangesB.size(); i < rangesBSize; i++) {
                if (boundsA.equals(rangesB.get(i))) {
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
        return new CharmaskMatchLengthObjective();
    }
}
