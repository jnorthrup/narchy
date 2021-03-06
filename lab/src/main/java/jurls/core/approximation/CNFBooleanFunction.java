/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.core.approximation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 *
 * @author thorsten
 * @see http:
 */
public class CNFBooleanFunction implements ParameterizedFunction {

    private final int numOutputBits;
    private final int[] numBitsPerVariable;
    private final int[][] cnf;
    private final long[] parameters;
    private final long[] variables;
    private final long[] intermediates;
    private final Random random = new Random();

    public CNFBooleanFunction(int numInputBits, int numOutputBits, int numInputs) {
        this.numOutputBits = numOutputBits;
        variables = new long[numInputBits];
        numBitsPerVariable = new int[numInputs];

        int error = 0;
        int sum = 0;
        for (int i = 0; i < numBitsPerVariable.length; ++i) {
            numBitsPerVariable[i] = 0;
            while (error < numInputBits && sum < numInputBits) {
                numBitsPerVariable[i]++;
                sum++;
                error += numInputs;
            }
            error -= numBitsPerVariable[i] * numInputs;
        }

        ArrayList<List<Integer>> cnf2 = new ArrayList<List<Integer>>();

        int[] indices = {1, 2, 3};
        do {
            List<List<Integer>> clauses = new ArrayList<>();
            clauses.add(new ArrayList<>());
            for (int k : indices) {
                clauses = extend(clauses, k);
            }
            cnf2.addAll(clauses);
        } while (increment(indices, indices.length - 1, numInputBits));

        cnf = new int[cnf2.size()][];
        for (int i = 0; i < cnf.length; ++i) {
            List<Integer> maxTerm = cnf2.get(i);
            cnf[i] = new int[maxTerm.size()];
            for (int j = 0; j < maxTerm.size(); ++j) {
                cnf[i][j] = maxTerm.get(j);
            }
        }

        parameters = new long[cnf.length];
        Arrays.fill(parameters, ~0l);
        intermediates = new long[cnf.length];
    }

    private static boolean increment(int[] indices, int k, int n) {
        indices[k]++;
        if (indices[k] > n) {
            if (k == 0) {
                return false;
            }
            if (increment(indices, k - 1, n - 1)) {
                indices[k] = indices[k - 1];
            } else {
                return false;
            }
        }
        return true;
    }

    private static List<List<Integer>> extend(List<List<Integer>> xs, int k) {
        List<List<Integer>> ys = new ArrayList<>();

        for (int i = 0; i < xs.size(); ++i) {
            List<Integer> is = new ArrayList<>(xs.get(i));
            is.add(-k);
            ys.add(is);
            is = new ArrayList<>(xs.get(i));
            is.add(k);
            ys.add(is);
        }

        return ys;
    }

    private long compute(int clauseIndex) {
        long b = 0L;
        int[] maxTerm = cnf[clauseIndex];

        for (int i = 0; i < maxTerm.length; ++i) {
            int literal = maxTerm[i];
            if (literal > 0) {
                b |= variables[literal - 1];
            } else {
                b |= ~variables[-literal - 1];
            }
        }
        b |= parameters[clauseIndex];

        return b;
    }

    private long compute() {
        long a = ~0l;

        for (int j = 0; j < cnf.length; ++j) {
            long b = compute(j);
            intermediates[j] = b;
            a &= b;
        }

        a &= (1l << numOutputBits) - 1l;
        return a;
    }

    @Override
    public double value(double[] xs) {
        int j = 0;

        for (int i = 0; i < xs.length; ++i) {
            long v = Math.round((double) ((1l << numBitsPerVariable[i]) - 1L) * xs[i]);

            for (int k = 0; k < numBitsPerVariable[i]; ++k, ++j) {
                if (((v >> k) & 1L) == 1L) {
                    variables[j] = ~0l;
                } else {
                    variables[j] = 0l;
                }
            }
        }

        return (double) compute() / (double) ((1l << numOutputBits) - 1L);
    }

    @Override
    public void learn(double[] xs, double y) {
        long currents = Math.round(value(xs) * (double) ((1l << numOutputBits) - 1L));
        long targets = Math.round(y * (double) ((1l << numOutputBits) - 1L));

        ArrayList<Integer> ps = new ArrayList<Integer>(numOutputBits);
        for (int i = 0; i < numOutputBits; ++i) {
            boolean target = ((targets >> i) & 1L) == 1L;
            boolean current = ((currents >> i) & 1L) == 1L;
            ps.clear();
            if (target && !current) {
                for (int j = 0; j < intermediates.length; ++j) {
                    if (((intermediates[j] >> i) & 1L) == 0L) {
                        ps.add(j);
                    }
                }
            } else if (!target && current) {
                for (int j = 0; j < parameters.length; ++j) {
                    parameters[j] ^= 1l << i;
                    if (((compute(j) >> i) & 1L) == 0L) {
                        ps.add(j);
                    }
                    parameters[j] ^= 1l << i;
                }
            }

            
            if (!ps.isEmpty()) {
                int p = ps.get(random.nextInt(ps.size()));
                parameters[p] ^= 1l << i;
            }
        }
    }

    @Override
    public int numberOfParameters() {
        return parameters.length;
    }

    @Override
    public int numberOfInputs() {
        return numBitsPerVariable.length;
    }

    @Override
    public void parameterGradient(double[] output, double... xs) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void addToParameters(double[] deltas) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public double minOutputDebug() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public double maxOutputDebug() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

}
