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


import jcog.data.list.FasterList;
import jcog.grammar.evolve.configuration.Configuration;
import jcog.grammar.evolve.configuration.EvolutionParameters;
import jcog.grammar.evolve.evaluators.TreeEvaluationException;
import jcog.grammar.evolve.generations.Generation;
import jcog.grammar.evolve.generations.InitialPopulationBuilder;
import jcog.grammar.evolve.generations.Ramped;
import jcog.grammar.evolve.inputs.Context;
import jcog.grammar.evolve.objective.Objective;
import jcog.grammar.evolve.objective.Ranking;
import jcog.grammar.evolve.selections.Selection;
import jcog.grammar.evolve.selections.Tournament;
import jcog.grammar.evolve.strategy.ExecutionListener;
import jcog.grammar.evolve.strategy.RunStrategy;
import jcog.grammar.evolve.tree.Node;
import jcog.grammar.evolve.variations.Variation;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Implements the default evolution strategy, termination criteria can be enabled thru parameters.
 * Optional accepted parameters:
 * "terminationCriteria", Boolean, then True the termination criteria is enabled when false is disabled, Default value: false
 * "terminationCriteriaGenerations", Integer, number of generations for the termination criteria.Default value: 200
 *
 * @author MaleLabTs
 */
public class DefaultStrategy implements RunStrategy {

    protected Context context;
    protected int maxDepth;
    protected final FasterList<Node> population = new FasterList();
    protected final TreeSet<Ranking> rankings = new TreeSet(RankingComparator);
    protected Selection selection;
    protected Objective objective;
    protected Variation variation;
    protected EvolutionParameters param;
    protected ExecutionListener listener;
    protected boolean terminationCriteria = false; 
    

    @Deprecated
    protected int terminationCriteriaGenerations = 200;
    @Deprecated
    protected int maxCrossoverTries = 20;


    @Override
    public void setup(Configuration configuration, ExecutionListener listener) {
        this.param = configuration.getEvolutionParameters();

        this.readParameters(configuration);

        this.context = new Context(Context.EvaluationPhases.TRAINING, configuration);
        this.maxDepth = param.getCreationMaxDepth();
        
        this.objective = configuration.getObjective();
        this.selection = new Tournament(this.context);
        this.variation = new Variation(this.context);
        this.listener = listener;

        this.objective.setup(context);
    }

    protected void readParameters(Configuration configuration) {
        var parameters = configuration.getStrategyParameters();
        if (parameters != null) {
            
            if (parameters.containsKey("terminationCriteriaGenerations")) {
                terminationCriteriaGenerations = Integer.parseInt(parameters.get("terminationCriteriaGenerations"));
            }
            if (parameters.containsKey("terminationCriteria")) {
                terminationCriteria = Boolean.parseBoolean(parameters.get("terminationCriteria"));
            }
        }
    }

    @Override
    public Void call() throws TreeEvaluationException {
        try {
            listener.evolutionStarted(this);

            var ctx = this.context;

            var populationBuilder = ctx.getConfiguration().getPopulationBuilder();

            populationBuilder.init(population);
            Generation ramped = new Ramped(this.maxDepth, this.context);
            var popSize = param.getPopulationSize();
            population.addAll(ramped.generate(popSize - this.population.size()));

            
            
            
            eachRankings(population, objective, (n, f) -> rankings.add(new Ranking(n, f)));















            
            String oldGenerationBestValue = null;
            var terminationCriteriaGenerationsCounter = 0;
            var doneGenerations = 0;

            for (var generation = 0; generation < param.getGenerations(); generation++) {
                ctx.setStripedPhase(ctx.getDataSetContainer().isDataSetStriped() && ((generation % ctx.getDataSetContainer().getProposedNormalDatasetInterval()) != 0));

                evolve();

                var best = rankings.first();
                doneGenerations = generation + 1;
                if (listener != null) {
                    listener.logGeneration(this, doneGenerations, best.getNode(), best.getFitness(), rankings);
                }
                var allPerfect = Arrays.stream(best.getFitness()).noneMatch(fitness -> Math.round(fitness * 10000) != 0);
                if (allPerfect) {
                    break;
                }

                if (terminationCriteria) {
                    var newBestValue = best.getDescription();
                    if (newBestValue.equals(oldGenerationBestValue)) {
                        terminationCriteriaGenerationsCounter++;
                    } else {
                        terminationCriteriaGenerationsCounter = 0;
                    }
                    if (terminationCriteriaGenerationsCounter >= this.terminationCriteriaGenerations) {
                        break;
                    }
                    oldGenerationBestValue = newBestValue;
                }

                if (Thread.interrupted()) {
                    break;
                }

            }

            
            TreeSet<Ranking> tmp = new TreeSet(RankingComparator);
            sortRankings(population, objective, tmp);


            listener.evolutionComplete(this, doneGenerations - 1, tmp);






            return null;
        } catch (Throwable x) {
            throw new TreeEvaluationException("Error during evaluation of a tree", x, this);
        }
    }

    protected void evolve() {
        throw new RuntimeException("share the impl from DiversityElitismStrategy");


























































    }

    protected static MutableMap<Node, double[]> buildRankings(List<Node> population, Objective objective) {
        MutableMap<Node, double[]> each = new UnifiedMap();
        eachRankings(population, objective, each);
        return each;
    }

    protected static void eachRankings(List<Node> population, Objective objective, BiConsumer<Node, double[]> each) {
        for (var tree : population) {
            each.accept(tree, objective.fitness(tree));
        }
    }

    protected static void eachRankings(List<Node> population, Objective objective, Map<Node, double[]> each) {
        eachRankings(population, objective, each::put);
    }
    protected static void sortRankings(List<Node> population, Objective objective, TreeSet<Ranking> each) {
        eachRankings(population, objective, (n, f) -> each.add(new Ranking(n, f)));
    }

    @Override
    public Configuration getConfiguration() {
        return context.getConfiguration();
    }

    @Override
    public ExecutionListener getExecutionListener() {
        return listener;
    }

    protected void sortByFirst(List<Ranking> front) {
        front.sort(RankingComparator);
    }

    @Override
    public Context getContext() {
        return this.context;
    }

    static final Comparator<Ranking> RankingComparator = (o1, o2) -> {
        if (o1 == o2) return 0;

        var f1 = o1.getFitness();
        var f2 = o2.getFitness();
        var n = f1.length;

        double balance = 0;

        for (var i = 0; i < n; i++) {
            var v1 = f1[i];
            var v2 = f2[i];
            if (v1==v2) continue;


            balance += (v1/(v1+v2) - 0.5);
        }



        if (balance > 0) return 1;
        if (balance < 0) return -1;







        return Integer.compare(o1.hashCode(), o2.hashCode());
    };
}
