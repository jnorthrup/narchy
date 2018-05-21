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
package jcog.grammar.evolve.generations;

import jcog.grammar.evolve.inputs.Context;
import jcog.grammar.evolve.tree.Node;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author MaleLabTs
 */
public class Ramped implements Generation {

    final Generation full;
    final Generation grow;
    final Context context;

    public Ramped(int maxDepth, Context context ) {
        this.full = new Full(maxDepth,context);
        this.grow = new Growth(maxDepth,context);
        this.context=context;
    }

     /**
     * This method returns a new population of the desired size. An half of the
     * population is generated through Growth algorithm, the other half by the
     * Full method
     * @param popSize the desired population size
     * @return a List of Node of size popSize
     */
    @Override
    public List<Node> generate(int popSize) {


        int popSizeGrow = popSize /2;
        int popSizeFull = popSize-popSizeGrow;

        List<Node> grow = this.full.generate(popSizeGrow);
        List<Node> full = this.grow.generate(popSizeFull);

        List<Node> population = new ArrayList<>(grow.size() + full.size());
        population.addAll(grow);
        population.addAll(full);

        return population;
    }
}
