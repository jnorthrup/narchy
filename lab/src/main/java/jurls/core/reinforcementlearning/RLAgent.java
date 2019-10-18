/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.core.reinforcementlearning;

import jurls.core.LearnerAndActor;
import jurls.core.approximation.ApproxParameters;
import jurls.core.approximation.ParameterizedFunctionGenerator;
import jurls.core.utils.ActionValuePair;
import jurls.core.utils.Utils;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 *
 * @author thorsten
 */
public class RLAgent extends LearnerAndActor {

    private final UpdateProcedure updateProcedure;
    private final double[][] s = new double[2][];
    private final int[] a = new int[2];
    private final int numActions;
    
    private final RLParameters rLParameters;
    private final ApproxParameters approxParameters;
    private final UpdateProcedure.Context context = new UpdateProcedure.Context();
    private final ActionSelector actionSelector;
    private final double[] stateAction;
    private final double[][] memory;
    private int memoryIndex = 0;
    private final double[] stateMax;
    private final double[] stateMin;
    private final double[] normalizedState;
    private double rewardMin = Double.POSITIVE_INFINITY;
    private double rewardMax = Double.NEGATIVE_INFINITY;
    private double factor1 = 0;
    private double factor2 = 0;
    private double rSum = 0;
    private double epsilon = 0;
    private static final double factor1ComponentDivisor = 1000;

    public RLAgent(
            ParameterizedFunctionGenerator parameterizedFunctionGenerator,
            UpdateProcedure updateProcedure,
            ActionSelector actionSelector,
            int numActions,
            double[] s0,
            ApproxParameters approxParameters,
            RLParameters rLParameters,
            int memorySize
    ) {
        this.parameterizedFunction = parameterizedFunctionGenerator.generate(s0.length + 1);
        this.updateProcedure = updateProcedure;
        this.actionSelector = actionSelector;
        this.numActions = numActions;
        context.previousDeltas = new double[parameterizedFunction.numberOfParameters()];
        context.e = new double[parameterizedFunction.numberOfParameters()];
        this.approxParameters = approxParameters;
        this.rLParameters = rLParameters;
        stateAction = new double[s0.length + 1];
        memory = new double[memorySize][];

        for (int i = 0; i < memory.length; ++i) {
            memory[i] = new double[s0.length];
        }
        stateMin = new double[s0.length];
        Arrays.fill(stateMin, Double.POSITIVE_INFINITY);
        stateMax = new double[s0.length];
        Arrays.fill(stateMax, Double.NEGATIVE_INFINITY);
        normalizedState = new double[s0.length];
    }

    public double[] getStateNormalized() { return normalizedState; }

    @Override
    public int learnAndAction(double[] state, double reward, double[] previousState, int previousAction) {
        final double U = 0.01; 

        for (int i = 0; i < state.length; ++i) {
            if (state[i] > stateMax[i]) {
                stateMax[i] = state[i];
            }
            if (state[i] < stateMin[i]) {
                stateMin[i] = state[i];
            }
            if (stateMin[i] == stateMax[i]) {
                stateMax[i] = stateMin[i] + U;
            }
            normalizedState[i] = (state[i] - stateMin[i]) / (stateMax[i] - stateMin[i]);
            memory[memoryIndex][i] = normalizedState[i];
        }

        memoryIndex++;
        if (memoryIndex >= memory.length) {
            memoryIndex = 0;
        }

        double nextFactor1 = Arrays.stream(memory).mapToDouble(m -> {
            double sum = IntStream.range(0, m.length).mapToDouble(j -> normalizedState[j] - m[j]).map(d -> d * d).sum();
            return sum;
        }).map(sum2 -> 1 / (1 + sum2 * factor1ComponentDivisor)).sum();
        nextFactor1 /= memory.length;

        if (reward > rewardMax) {
            rewardMax = reward;
        }
        if (reward < rewardMin) {
            rewardMin = reward;
        }
        if (rewardMin == rewardMax) {
            rewardMax = rewardMin + U;
        }
        double r = (reward - rewardMin) / (rewardMax - rewardMin);
        rSum = r + rSum * rLParameters.getGamma();
        double referenceQ = 1 / (1 - rLParameters.getGamma());

        factor1 = nextFactor1;
        factor2 = 1 - rSum / referenceQ;

        epsilon = rLParameters.getEpsilon() * factor1 * factor2;

        s[0] = previousState;
        s[1] = state;
        a[0] = previousAction;
        a[1] = chooseAction(state);

        updateProcedure.update(
                approxParameters,
                rLParameters, context,
                r, s, a, parameterizedFunction,
                numActions
        );

        updateCounters();

        return a[1];
    }

    public ActionValuePair[] getActionProbabilities(double[] state) {
        int bound = numActions;
        ActionValuePair[] actionValuePairs = IntStream.range(0, bound).mapToObj(i -> new ActionValuePair(
                i,
                Utils.q(parameterizedFunction, stateAction, state, i)
        )).toArray(ActionValuePair[]::new);

        return actionSelector.fromQValuesToProbabilities(epsilon, actionValuePairs);
    }

    public int chooseAction(double[] state) {
        ActionValuePair[] actionProbabilityPairs = getActionProbabilities(state);
        Arrays.sort(actionProbabilityPairs, (o1, o2) -> (int) Math.signum(o1.getV() - o2.getV()));

        
        int i = actionProbabilityPairs.length-1; 


        






        return actionProbabilityPairs[i].getA();
    }

    @Override
    public String getDebugString(int indent) {
        String ind = Utils.makeIndent(indent);
        return ind + "Q/SARSA(lambda)\n"
                + ind + "factor1 = " + factor1 + "\n"
                + ind + "factor2 = " + factor2 + "\n"
                + ind + "epsilon = " + epsilon + "\n"
                + super.getDebugString(indent);
    }

    @Override
    public void stop() {
    }
}
