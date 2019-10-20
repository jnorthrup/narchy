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
package jcog.grammar.evolve.strategy.impl;

import jcog.grammar.evolve.configuration.Configuration;
import jcog.grammar.evolve.evaluators.TreeEvaluationException;
import jcog.grammar.evolve.evaluators.TreeEvaluator;
import jcog.grammar.evolve.inputs.Context;
import jcog.grammar.evolve.inputs.DataSet;
import jcog.grammar.evolve.objective.Objective;
import jcog.grammar.evolve.objective.Ranking;
import jcog.grammar.evolve.objective.performance.PerformacesObjective;
import jcog.grammar.evolve.objective.performance.PerformancesFactory;
import jcog.grammar.evolve.outputs.FinalSolution;
import jcog.grammar.evolve.outputs.JobEvolutionTrace;
import jcog.grammar.evolve.outputs.Results;
import jcog.grammar.evolve.strategy.ExecutionListener;
import jcog.grammar.evolve.strategy.ExecutionListenerFactory;
import jcog.grammar.evolve.strategy.ExecutionStrategy;
import jcog.grammar.evolve.strategy.RunStrategy;
import jcog.grammar.evolve.tree.Constant;
import jcog.grammar.evolve.tree.Node;
import jcog.grammar.evolve.utils.BasicStats;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Execution listener with the same workflow as CoolExecutionListener but there is no console output.
 * The status of evolution is accessible thru the getStatus() method.
 * This listener can automatically call the PostPorcessor in order to complete the elaboration.
 * It provides public methods in order to get more detailed informations about best solution, extractions,
 * and a list of statistical indexes.
 * @author MaleLabTs
 */
public class BasicExecutionListener implements ExecutionListener, ExecutionListenerFactory {

    private static final Logger LOG = Logger.getLogger(DefaultExecutionListener.class.getName());
    private final Map<Integer, Long> jobStartTimes = new ConcurrentHashMap<>();
    private final NavigableSet<Integer> remove = new TreeSet<>();
    private long startTime = System.currentTimeMillis();
    private boolean callPostProcessorAutomatically = false;
    
    private final Results results;
    private final Configuration configuration;
    private final BasicExecutionStatus status = new BasicExecutionStatus();
    private final boolean isFlagging;
    
    
    public BasicExecutionListener(Configuration configuration, Results results, boolean callPostProcessorAutomatically) {
        this(configuration, results);
        this.callPostProcessorAutomatically = callPostProcessorAutomatically;
    }
    
    public BasicExecutionListener(Configuration configuration, Results results) {
        this.configuration = configuration;
        this.status.jobTotal = configuration.getJobs();
        this.status.overallGenerations = configuration.getEvolutionParameters().getGenerations() * status.jobTotal;
        this.status.isSearchRunning = true;
        this.status.hasFinalResult = false;
        this.results = results;
        this.isFlagging = configuration.isIsFlagging();
    }

    private boolean firstEvolution = true;
    @Override
    public void evolutionStarted(RunStrategy strategy) {
        var jobId = strategy.getConfiguration().getJobId();
        this.jobStartTimes.put(jobId, System.currentTimeMillis());
        if(firstEvolution){
            this.startTime = System.currentTimeMillis();
            firstEvolution = false;
        }    
    }

    
    @Override
    public void logGeneration(RunStrategy strategy, int generation, Node best, double[] fitness, Collection<Ranking> population) {
        var jobId = strategy.getConfiguration().getJobId();
      
        this.status.overallGenerationsDone++;

        var timeTakenPerGen = (double)(System.currentTimeMillis() - startTime) / this.status.overallGenerationsDone;
        var elapsedMillis = (long)((this.status.overallGenerations - this.status.overallGenerationsDone) * timeTakenPerGen);

        this.status.evolutionEta = String.format("%d h, %d m, %d s",
                TimeUnit.MILLISECONDS.toHours(elapsedMillis),
                TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(elapsedMillis)),
                TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedMillis)));

        var bestRanking = new Ranking(best, fitness);
        var generationBestSolution = new FinalSolution(bestRanking);


        var trainingObjective = PerformancesFactory.buildObjective(Context.EvaluationPhases.TRAINING, strategy.getConfiguration());
        var trainingPerformace = trainingObjective.fitness(best);
        PerformacesObjective.populatePerformancesMap(trainingPerformace, generationBestSolution.getTrainingPerformances(), isFlagging);
                       
        status.updateBest(generationBestSolution);
        
        
        

        results.addCharachterEvaluated(strategy.getContext().getCurrentDataSet().getNumberOfChars() * population.size());
    }

    @Override
    public void evolutionComplete(RunStrategy strategy, int generation, Collection<Ranking> population) {
        var jobId = strategy.getConfiguration().getJobId();
        var executionTime = System.currentTimeMillis() - this.jobStartTimes.remove(jobId);


        var jumpedGenerations = strategy.getConfiguration().getEvolutionParameters().getGenerations() - generation;
        this.status.overallGenerationsDone+=jumpedGenerations;
             
        synchronized (remove) {
            remove.add(jobId);
        }

        this.status.jobDone++;

        var jobTrace = this.results.getJobTrace(jobId);
        jobTrace.setExecutionTime(executionTime);
        
        /*
         Populate Job final population with FinalSolution(s). The final population has the same order as fitness ranking but contains fitness and performance info
         The performance are propulated here:
         */
        var trainingObjective = PerformancesFactory.buildObjective(Context.EvaluationPhases.TRAINING, strategy.getConfiguration());
        var validationObjective = PerformancesFactory.buildObjective(Context.EvaluationPhases.VALIDATION, strategy.getConfiguration());
        var learningObjective = PerformancesFactory.buildObjective(Context.EvaluationPhases.LEARNING, strategy.getConfiguration());

        var i = 0;
        for (var individual : population) {
            var finalSolution = new FinalSolution(individual);
            
            if(i++==0){
                var trainingPerformace = trainingObjective.fitness(individual.getNode());
                var validationPerformance = validationObjective.fitness(individual.getNode());
                var learningPerformance = learningObjective.fitness(individual.getNode());
                PerformacesObjective.populatePerformancesMap(trainingPerformace, finalSolution.getTrainingPerformances(),isFlagging);
                PerformacesObjective.populatePerformancesMap(validationPerformance, finalSolution.getValidationPerformances(),isFlagging);
                PerformacesObjective.populatePerformancesMap(learningPerformance, finalSolution.getLearningPerformances(),isFlagging);
            }
            jobTrace.getFinalGeneration().add(finalSolution);
        }
        
        
        if (this.status.jobDone >= strategy.getConfiguration().getJobs()) {
            
        
            
            
            if(callPostProcessorAutomatically) {
                callPostProcessor();
            }
        }
    }

    @Override
    public void evolutionFailed(RunStrategy strategy, TreeEvaluationException cause) {
        var jobId = strategy.getConfiguration().getJobId();
        synchronized (remove) {
            remove.add(jobId);
        }
        this.status.jobDone++;
        this.status.jobFailed++;
        if (this.status.jobDone >= strategy.getConfiguration().getJobs()) {
            
            if(callPostProcessorAutomatically) {
                callPostProcessor();
            }
        }
            
        LOG.log(Level.SEVERE, "Job "+jobId+" failed", cause);
        
    }

    private void callPostProcessor(){
        if (configuration.getPostProcessor() != null) {
            var elaborationTime = System.currentTimeMillis() - startTime;
            configuration.getPostProcessor().elaborate(configuration, results, elaborationTime);
        }
        this.status.isSearchRunning = false;
        this.status.best = this.results.getBestSolution();
        this.status.hasFinalResult = true;
    }

    @Override
    public void register(ExecutionStrategy strategy) {
        
    }

    @Override
    public ExecutionListener getNewListener() {
        return this;
    }

    public BasicExecutionStatus getStatus() {
        return status;
    }

    public Results getResults() {
        return results;
    }

    public List<DataSet.Bounds[]> getBestEvaluations() throws TreeEvaluationException{
        var treeEvaluator = this.configuration.getEvaluator();
        Node bestIndividualReplica = new Constant(this.status.best.getSolution());
        return treeEvaluator.evaluate(bestIndividualReplica, new Context(Context.EvaluationPhases.LEARNING, this.configuration));
    }
 
    
    public List<BasicStats> getBestEvaluationStats(int startIndex, int endIndex) throws TreeEvaluationException{
        var bestevaluations = getBestEvaluations();
        var dataset = this.configuration.getDatasetContainer().getLearningDataset();
        List<BasicStats> statsPerExample = new LinkedList<>();
        for (var index = startIndex; index <= endIndex; index++) {
            var extractionsList = bestevaluations.get(index);
            Set<DataSet.Bounds> extractionsSet = UnifiedSet.newSetWith(extractionsList);
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
    
    @Override
    public void evolutionStopped() {
        this.status.isSearchRunning = false;
    }
    
}
