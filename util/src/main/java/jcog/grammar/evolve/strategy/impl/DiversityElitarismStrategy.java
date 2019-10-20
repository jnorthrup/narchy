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
import jcog.grammar.evolve.generations.Generation;
import jcog.grammar.evolve.generations.Ramped;
import jcog.grammar.evolve.objective.Ranking;
import jcog.grammar.evolve.selections.Selection;
import jcog.grammar.evolve.tree.Node;
import jcog.grammar.evolve.utils.UniqueList;
import jcog.grammar.evolve.utils.Utils;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.*;

/**
 * Optional accepted parameters:
 * "terminationCriteria", Boolean, then True the termination criteria is enabled when false is disabled, Default value: false
 * "terminationCriteriaGenerations", Integer, number of generations for the termination criteria.Default value: 200  
 * "deepDiversity", Boolean, when false the diversity is imposed only on new generated individuals (then those individuals are merged to the older ones)
 * when true, a new individual is accepted when it is unique thru the current populations and the new generated individuals (more strict condition) 
 * @author MaleLabTs
 */
public class DiversityElitarismStrategy extends DefaultStrategy{
    
    boolean deepDiversity = false;
    
    @Override
    protected void readParameters(Configuration configuration) {
        super.readParameters(configuration);
        var parameters = configuration.getStrategyParameters();
        if (parameters != null) {
            
            if (parameters.containsKey("deepDiversity")) {
                deepDiversity = Boolean.parseBoolean(parameters.get("deepDiversity"));
            }
        }
    }
    
    @Override
    protected void evolve() {
        var popSize = population.size();
        var oldPopSize = (int) (popSize * 0.9);

        List<Node> newPopulation = new UniqueList<>(popSize);
        
        if(deepDiversity){
            newPopulation.addAll(population);
        }


        var allPerfect = Arrays.stream(rankings.first().getFitness()).noneMatch(fitness -> Math.round(fitness * 10000) != 0);
        if (allPerfect) {
                return;
            }


        var stepPopSize = deepDiversity? popSize+oldPopSize : oldPopSize;

        var rng = context.getRandom();
        var crossoverProb = param.getCrossoverProbability();
        var sel = this.selection;
        var r = this.rankings;


        var rr = r.toArray(new Ranking[0]);


        while (newPopulation.size() < stepPopSize) {

            var random = rng.nextDouble();

            if (random <= crossoverProb && oldPopSize - newPopulation.size() >= 2) {
                var selectedA = sel.select(rr);
                var selectedB = sel.select(rr);

                var newIndividuals = variation.crossover(selectedA, selectedB, maxCrossoverTries);
                if (newIndividuals != null) {
                    newPopulation.add(newIndividuals.getOne());
                    newPopulation.add(newIndividuals.getTwo());
                }
            } else if (random <= crossoverProb + param.getMutationPobability()) {
                var mutant = sel.select(rr);
                mutant = variation.mutate(mutant);
                newPopulation.add(mutant);
            } else {
                var duplicated = sel.select(rr);
                newPopulation.add(duplicated);
            }
        }

        Generation ramped = new Ramped(maxDepth, context);
        var generated = ramped.generate(popSize - oldPopSize);
        newPopulation.addAll(generated);
        
        if(!deepDiversity){
            newPopulation.addAll(population);
        }

        
        MutableMap<Node,double[]> remaining = new UnifiedMap(newPopulation.size());
        eachRankings(newPopulation, objective, remaining);

        var maxPopulation = param.getPopulationSize();
        var nextPopSize =
                Math.min( remaining.size(),
                          maxPopulation ); 

        do {

            var nextRemaining = Utils.getFirstParetoFront(remaining, nextPopSize);

            if (nextRemaining == null) break;

            remaining = nextRemaining;

        }while (remaining.size() > nextPopSize);

        List<Node> pp = population; 
        pp.clear();

        Set<Ranking> rankings = this.rankings;
        rankings.clear();
        remaining.forEachKeyValue( (n,f) -> {
            rankings.add(new Ranking(n,f));
            pp.add(n);
        });

    }   

     
}
