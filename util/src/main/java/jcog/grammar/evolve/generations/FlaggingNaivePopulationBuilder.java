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
package jcog.grammar.evolve.generations;

import jcog.data.list.FasterList;
import jcog.grammar.evolve.configuration.Configuration;
import jcog.grammar.evolve.inputs.Context;
import jcog.grammar.evolve.inputs.DataSet;
import jcog.grammar.evolve.inputs.DataSet.Example;
import jcog.grammar.evolve.tree.Constant;
import jcog.grammar.evolve.tree.Node;
import jcog.grammar.evolve.tree.RegexRange;
import jcog.grammar.evolve.tree.operator.Concatenator;
import jcog.grammar.evolve.tree.operator.ListMatch;
import jcog.grammar.evolve.tree.operator.MatchOneOrMore;
import jcog.grammar.evolve.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Create a population, 3 individuals for each positive example.
 * @author andrea + fabiano
 */
public class FlaggingNaivePopulationBuilder implements InitialPopulationBuilder {

    private final List<Node> population = new FasterList();
    private final boolean useDottification;
    private final boolean useWordClasses;

    /**
     * Initialises a population from examples by replacing charcters with "\\w"
     * and digits with "\\d"
     */
    public FlaggingNaivePopulationBuilder() {
        this.useDottification = true;
        this.useWordClasses = true;
    }

    /**
     * When dottification is true and useWordClasses is true, instances are
     * initialized using character classes "[A-Za-z]" "\\d" When dottification is
     * true and useWordClasses is false, instances are initialized by replacing
     * characters and digits with "."
     *
     * @param useDottification
     * @param useWordClasses
     */
    public FlaggingNaivePopulationBuilder(final boolean useDottification, final boolean useWordClasses) {
        this.useDottification = useDottification;
        this.useWordClasses = useWordClasses;
    }


    @Override
    public void init(final List<Node> target) {
        target.addAll(population);
    }

    @Override
    public void setup(final Configuration configuration) {
        final DataSet trainingDataset = configuration.getDatasetContainer().getTrainingDataset();
        this.population.addAll(this.setup(configuration, trainingDataset));
    }
    
    private List<Node> setup(final Configuration configuration, final DataSet usedTrainingDataset) {
        final DataSet dataSet = usedTrainingDataset;


        final Set<String> phrases = new HashSet<>();
        for (final Example example : dataSet.getExamples()) {
            if (!example.match.isEmpty()) {
                final String string = example.getString();
                phrases.add(string);
            }
        }

        final int examples = Math.min(configuration.getEvolutionParameters().getPopulationSize() / 3, phrases.size());

        final List<String> uniquePhrases = new ArrayList<>(phrases);

        int counter = 0;
        final List<Node> newPopulation = new LinkedList<>();
        for (final String node : uniquePhrases) {
            if (this.useDottification) {
                newPopulation.add(createByExample(node, true, false));
                newPopulation.add(createByExample(node, true, true));
            }
            newPopulation.add(createByExample(node, false, false));
            counter++;
            if (counter >= examples) {
                break;
            }
        }
        return newPopulation;
    }

    private Node createByExample(final String example, final boolean replace, final boolean compact) {


        final String d = this.useWordClasses ? "\\d" : ".";
        final Node letters;
        if(useWordClasses){
            final ListMatch l = new ListMatch();
            l.add(new RegexRange("A-Za-z"));
            letters = l;
        } else {
            letters = new Constant(".");
        }

        Deque<Node> nodes = new LinkedList<>();
        for (final char c : example.toCharArray()) {
            if (replace) {
                if (Character.isLetter(c)) {
                    nodes.add(letters.cloneTree());
                } else if (Character.isDigit(c)) {
                    nodes.add(new Constant(d));
                } else {
                    nodes.add(new Constant(Utils.escape(c)));
                }
            } else {
                nodes.add(new Constant(Utils.escape(c)));
            }
        }

        
        
        if(compact){
            final Deque<Node> newNodes = new LinkedList<>();
            
            
            while (nodes.size()>0) {
                Node node = nodes.pollFirst();
                final String nodeValue = node.toString();
                boolean isRepeat = false;
                while (nodes.size()>0){
                    final Node next = nodes.peek();
                    final String nextValue = next.toString();

                    if(nodeValue.equals(nextValue)){
                        isRepeat = true;
                        
                        nodes.pollFirst();
                    } else {
                        
                        break;
                    } 
                }    
                if(isRepeat){
                    final Node finalNode = new MatchOneOrMore(node);
                    node = finalNode;
                }
                newNodes.add(node);                
            }
            nodes = newNodes;
        }

        Deque<Node> tmp = new LinkedList<>();
        while (nodes.size() > 1) {

            while (nodes.size() > 0) {
                final Node first = nodes.pollFirst();
                final Node second = nodes.pollFirst();

                if (second != null) {
                    tmp.addLast(new Concatenator(first, second));
                } else {
                    tmp.addLast(first);
                }
            }

            nodes = tmp;
            tmp = new LinkedList<>();

        }

       
        return nodes.getFirst();
    }

    @Override
    public List<Node> init(final Context context) {
         return setup(context.getConfiguration(), context.getCurrentDataSet());
    }
}
