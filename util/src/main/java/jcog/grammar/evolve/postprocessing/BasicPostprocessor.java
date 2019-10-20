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
package jcog.grammar.evolve.postprocessing;

import jcog.grammar.evolve.configuration.Configuration;
import jcog.grammar.evolve.evaluators.TreeEvaluationException;
import jcog.grammar.evolve.evaluators.TreeEvaluator;
import jcog.grammar.evolve.inputs.Context;
import jcog.grammar.evolve.inputs.DataSet;
import jcog.grammar.evolve.inputs.DataSet.Bounds;
import jcog.grammar.evolve.inputs.DataSet.Example;
import jcog.grammar.evolve.outputs.Results;
import jcog.grammar.evolve.tree.Constant;
import jcog.grammar.evolve.tree.Node;
import jcog.grammar.evolve.utils.BasicStats;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This is a simplified postprocessor which implements only the basic workflow, i.e. solution selection
 * This class is not intended to provide any output (BasicPostprocessor is not going to write to console 
 * or saving JSON files)
 * @author MaleLabTs
 */
public class BasicPostprocessor implements Postprocessor {

    private static final Logger LOG = Logger.getLogger(BasicPostprocessor.class.getName());
    private boolean populateOptionalFields = true;
    public static final String PARAMETER_NAME_POPULATE_OPTIONAL_FIELDS = "populateOptionalFields";
   
    @Override
    public void setup(Map<String, String> parameters) {
    }

    @Override
    public void elaborate(Configuration config, Results results, long timeTaken) {

        var parameters = config.getPostprocessorParameters();
        if(parameters!=null){
            if(parameters.containsKey(PARAMETER_NAME_POPULATE_OPTIONAL_FIELDS)){
                this.populateOptionalFields = Boolean.parseBoolean(parameters.get(PARAMETER_NAME_POPULATE_OPTIONAL_FIELDS));
            }
        }
          
        
        
        
        config.getBestSelector().elaborate(results);
        results.setOverallExecutionTimeMillis(timeTaken);
        
        if(populateOptionalFields){
            results.setExamples(config.getDatasetContainer().getDataset().getExamples());
        }
        try {
            
            results.setBestExtractions(BasicPostprocessor.getEvaluations(results.getBestSolution().getSolution(), config, Context.EvaluationPhases.LEARNING));
            results.setBestExtractionsStrings(BasicPostprocessor.getEvaluationsStrings(results.getBestExtractions(),config.getDatasetContainer().getLearningDataset()));
            results.setBestExtractionsStats(BasicPostprocessor.getEvaluationStats(results.getBestExtractions(), config));
        } catch (TreeEvaluationException ex) {
            Logger.getLogger(BasicPostprocessor.class.getName()).log(Level.SEVERE, null, ex);
        }


        var training = config.getDatasetContainer().getTrainingDataset();
        var learning = config.getDatasetContainer().getLearningDataset();

        var numberTrainingMatches = training.getNumberMatches();
        var numberTrainingUnmatches = training.getNumberUnmatches();
        results.setNumberTrainingMatches(numberTrainingMatches);
        results.setNumberTrainingUnmatches(numberTrainingUnmatches);

        var numberMatches = learning.getNumberMatches();
        var numberUnmatches = learning.getNumberUnmatches();
        var numberMatchedChars = learning.getNumberMatchedChars();
        var numberUnmatchedChars = learning.getNumberUnmatchedChars();
        var numberAllChars = config.getDatasetContainer().getDataset().getNumberOfChars();
        results.setNumberMatches(numberMatches);
        results.setNumberUnmatches(numberUnmatches);
        results.setNumberMatchedChars(numberMatchedChars);
        results.setNumberUnmatchedChars(numberUnmatchedChars);
        results.setNumberAllChars(numberAllChars);
        
    }
    
    private static List<Bounds[]> getEvaluations(String solution, Configuration configuration, Context.EvaluationPhases phase) throws TreeEvaluationException{
        var treeEvaluator = configuration.getEvaluator();
        Node bestIndividualReplica = new Constant(solution);
        return treeEvaluator.evaluate(bestIndividualReplica, new Context(phase, configuration));
    }
    
    private static List<List<String>> getEvaluationsStrings(List<Bounds[]> extractions, DataSet dataset){
        List<List<String>> evaluationsStrings = new ArrayList<>();
        var it = dataset.getExamples().iterator();
        for (var extractionsOfExample : extractions) {
            var example = it.next();
            var extractionsOfExampleStrings = Arrays.stream(extractionsOfExample).map(bounds -> example.getString().substring(bounds.start, bounds.end)).collect(Collectors.toList());
            evaluationsStrings.add(extractionsOfExampleStrings);
        }
        return evaluationsStrings;
    }

    
    
    private static List<BasicStats> getEvaluationStats(List<Bounds[]> evaluation, Configuration config) {
        var dataset = config.getDatasetContainer().getLearningDataset();
        List<BasicStats> statsPerExample = new LinkedList<>();
        for (var index = 0; index < dataset.getExamples().size(); index++) {
            var extractionsList = evaluation.get(index);
            Set<Bounds> extractionsSet = UnifiedSet.newSetWith(extractionsList);
            var example = dataset.getExample(index);
            extractionsSet.removeAll(example.getMatch());
            var exampleStats = new BasicStats();
            exampleStats.fn = -1; 
            exampleStats.fp = extractionsSet.size();
            exampleStats.tp = extractionsList.length - exampleStats.fp;
            exampleStats.tn = -1; 
            statsPerExample.add(exampleStats);
        }
        return statsPerExample;
    }
}
