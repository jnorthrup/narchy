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
import jcog.grammar.evolve.utils.BasicTokenizer;
import jcog.grammar.evolve.utils.Tokenizer;
import jcog.grammar.evolve.utils.Utils;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.*;

/**
 * Creates a initial population from the matches. Matches are modified in this way:
 * Significant tokens are left unchanged, other words are changed into the corresponding
 * character class (i.e. \w \d).
 * Sequences of identical classes are compacted using quantifiers.
 * @author MaleLabTs
 */
public class TokenizedPopulationBuilder implements InitialPopulationBuilder {

    private final List<Node> population = new FastList();
    private final Map<String,Double> winnerTokens = new UnifiedMap();
    
    private final Tokenizer tokenizer = new BasicTokenizer();
     
    /**
     * Initialises a population from examples by replacing charcters with "\\w"
     * and digits with "\\d"
     */
    public TokenizedPopulationBuilder() {
    }

    @Override
    public void init(List<Node> target) {
        target.addAll(population);
    }

 
    private static boolean matchW(String string){
        return (string.length()==1 && matchW(string.charAt(0)));
    }
    
    private static boolean matchW(char character){
        return Character.isAlphabetic(character) || Character.isDigit(character) || character == '_';
    }
    
    @Override
    public void setup(Configuration configuration) {
        var trainingDataset = configuration.getDatasetContainer().getTrainingDataset();
        this.population.addAll(this.setup(configuration, trainingDataset));
    }
        
    private List<Node> setup(Configuration configuration, DataSet usedTrainingDataset) {

        var dataSet = usedTrainingDataset;

        var TOKEN_THREASHOLD = 80.0;
        var DISCARD_W_TOKENS = true;
        var parameters = configuration.getPopulationBuilderParameters();
        if(parameters!=null){
            
            if(parameters.containsKey("tokenThreashold")){
                TOKEN_THREASHOLD = Double.parseDouble(parameters.get("tokenThreashold"));
            }
            if(parameters.containsKey("discardWtokens")){
                DISCARD_W_TOKENS = Boolean.parseBoolean(parameters.get("discardWtokens"));
            }
        }

        List<List<String>> matchTokens = new LinkedList<>();
        Map<String, Double> tokensCounter = new HashMap<>();
        for (var example : dataSet.getExamples()) {
            for (var match : example.getMatchedStrings()) {
                var tokens = tokenizer.tokenize(match);
                matchTokens.add(tokens);
                Set<String> tokensSet = new HashSet<>(tokens);
                for(var token : tokensSet){
                    if(matchW(token) && DISCARD_W_TOKENS){
                        continue;
                    }
                    if(tokensCounter.containsKey(token)){
                        var value = tokensCounter.get(token);
                        value++;
                        tokensCounter.put(token, value);
                    } else {
                        tokensCounter.put(token, 1.0);
                    }
                }
            }
        }

        var numberOfMatches = dataSet.getNumberMatches();
        for (var entry : tokensCounter.entrySet()) {
            var key = entry.getKey();
            var double1 = entry.getValue();
            Double doublePercentange = (double1 * 100.0) / numberOfMatches;
            entry.setValue(doublePercentange); 
             if(doublePercentange >= TOKEN_THREASHOLD){
                winnerTokens.put(key,doublePercentange);
            }
        }

        var popSize = configuration.getEvolutionParameters().getPopulationSize();

        var counter = 0;
        List<Node> newPopulation = new LinkedList<>();
        for (var tokenizedMatch : matchTokens){
            newPopulation.add(createIndividualFromString(tokenizedMatch, true, winnerTokens));
            newPopulation.add(createIndividualFromString(tokenizedMatch, false, winnerTokens));
            counter+=2;
            if (counter >= popSize) {
                break;
            }
        }
        return newPopulation;
    }

    static final RegexRange LETTERS_RANGE = new RegexRange("A-Za-z");

    private static Node createIndividualFromString(List<String> tokenizedString, boolean compact, Map<String, Double> winnerTokens) {
        Deque<Node> nodes = new LinkedList<>();

        var w = "\\w";
        var d = "\\d";

        
         
        for(var token : tokenizedString){
            if(winnerTokens.containsKey(token)){
                nodes.add(new Constant(Utils.escape(token)));
            } else {

                for (var c : token.toCharArray()) {
                    if (Character.isLetter(c)) {
                        nodes.add(new ListMatch(LETTERS_RANGE).cloneTree());
                    } else if (Character.isDigit(c)) {
                        nodes.add(new Constant(d));
                    } else {
                        nodes.add(new Constant(Utils.escape(c)));
                    }
                }
            }
        }
        
        
        
        if(compact){
            Deque<Node> newNodes = new LinkedList<>();
            
            
            while (!nodes.isEmpty()) {

                var node = nodes.pollFirst();
                var nodeValue = node.toString();
                var isRepeat = false;

                while (!nodes.isEmpty()){
                    var next = nodes.peek();
                    var nextValue = next.toString();

                    if(nodeValue.equals(nextValue)){
                        isRepeat = true;
                        
                        nodes.pollFirst();
                    } else {
                        
                        break;
                    } 
                }    
                if(isRepeat){
                    node = new MatchOneOrMore(node);
                }
                newNodes.add(node);                
            }
            nodes = newNodes;
        }


        Deque<Node> tmp = new LinkedList<>();
        while (nodes.size() > 1) {

            while (nodes.size() > 0) {
                var first = nodes.pollFirst();
                var second = nodes.pollFirst();

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
    public List<Node> init(Context context) {
        return setup(context.getConfiguration(), context.getCurrentDataSet());
    }
}
