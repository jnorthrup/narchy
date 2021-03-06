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
package jcog.grammar.evolve.evaluators;

import jcog.grammar.evolve.inputs.Context;
import jcog.grammar.evolve.inputs.Context.EvaluationPhases;
import jcog.grammar.evolve.inputs.DataSet.Bounds;
import jcog.grammar.evolve.tree.Node;
import jcog.grammar.evolve.utils.Triplet;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.ToLongFunction;

/**
 *
 * @author MaleLabTs
 */
public class CachedTreeEvaluator extends DefaultTreeEvaluator implements CachedEvaluator{

    private final Map<Triplet<EvaluationPhases, Boolean, String>, List<Bounds[]>> cache =
            
            new ConcurrentHashMap<>();

    private long hit = 0L;
    private long miss = 0L;

    @Override
    public List<Bounds[]> evaluate(Node root, Context context) throws TreeEvaluationException {

        StringBuilder sb = new StringBuilder();
        root.describe(sb);

        Triplet<EvaluationPhases, Boolean, String> key = new Triplet<EvaluationPhases, Boolean, String>(context.getPhase(), context.isStripedPhase(), sb.toString());
        /*synchronized (cache)*/ {
            TreeEvaluationException[] error = new TreeEvaluationException[1];
            List<Bounds[]> result = cache.compute(key, new BiFunction<Triplet<EvaluationPhases, Boolean, String>, List<Bounds[]>, List<Bounds[]>>() {
                @Override
                public List<Bounds[]> apply(Triplet<EvaluationPhases, Boolean, String> k, List<Bounds[]> res) {
                    if (res != null) {
                        hit++;
                        return res;
                    } else {
                        miss++;

                        try {
                            return CachedTreeEvaluator.super.evaluate(root, context);
                        } catch (TreeEvaluationException e) {
                            error[0] = e;
                        }

                    }
                    return null;
                }
            });
            if (result == null && error[0]!=null)
                throw error[0];
            return result;
        }

        
    }

    @Override
    public double getRatio(){
        return (double)this.hit/ (double) (this.hit + this.miss);
    }
    
    @Override
    public long getCacheSizeBytes(){
        synchronized (cache) {
            long cacheSize = cache.values().stream().mapToLong(new ToLongFunction<List<Bounds[]>>() {
                @Override
                public long applyAsLong(List<Bounds[]> list) {
                    long sum = 0L;
                    for (Bounds[] exampleResult : list) {
                        long length = (long) exampleResult.length;
                        sum += length;
                    }
                    return sum;
                }
            }).sum();
            cacheSize = cacheSize * (long) (Integer.SIZE / 4);
            return cacheSize;
        }
    }
}
