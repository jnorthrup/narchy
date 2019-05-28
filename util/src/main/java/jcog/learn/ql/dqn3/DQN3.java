package jcog.learn.ql.dqn3;


import jcog.Util;
import jcog.data.list.FasterList;
import jcog.learn.Agent;
import jcog.random.XoRoShiRo128PlusRandom;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

/** untested */
public class DQN3 extends Agent {
    private final Random rand = new XoRoShiRo128PlusRandom();


    private final double gamma;
    private final int experienceAddEvery;
    private final int experienceSize;
    private final int learningStepsPerIteration;
    private final double tdErrorClamp;
    private final int numHiddenUnits;
//    private final int saveInterval;
    private final double alpha;
    private final double epsilon;
    private final Mat W1;
    private final Mat B1;
    private final Mat W2;
    private final Mat B2;
    private final FasterList<Experience> experience;
    private int experienceIndex;
    private int t;
    private double lastReward;
    private Mat lastState;
    private Mat currentState;
    private int lastAction;
    private int currentAction;
    private Graph lastG;

    public DQN3(final int inputs, final int numActions, final Map<Option, Double> config) {
        super(inputs, numActions);

        this.gamma = config.getOrDefault(Option.GAMMA, 0.75);
        this.epsilon = config.getOrDefault(Option.EPSILON, 0.1);
        this.alpha = config.getOrDefault(Option.ALPHA, 0.05);

        this.experienceAddEvery = DQN3.toInteger(config.getOrDefault(Option.EXPERIENCE_ADD_EVERY, 25.0));
        this.experienceSize = DQN3.toInteger(config.getOrDefault(Option.EXPERIENCE_SIZE, 5000.0));
        this.learningStepsPerIteration = DQN3.toInteger(config.getOrDefault(Option.LEARNING_STEPS_PER_ITERATION, 10.0));
        this.tdErrorClamp = config.getOrDefault(Option.TD_ERROR_CLAMP, 1.0);
        this.numHiddenUnits = DQN3.toInteger(config.getOrDefault(Option.NUM_HIDDEN_UNITS, 100.0));

//        this.saveInterval = DQN3.toInteger(config.getOrDefault(Option.SAVE_INTERVAL, 100.0));

        this.W1 = DQN3.createRandMat(rand, this.numHiddenUnits, this.inputs);
        this.B1 = new Mat(this.numHiddenUnits, 1);
        this.W2 = DQN3.createRandMat(rand, this.actions, this.numHiddenUnits);
        this.B2 = new Mat(this.actions, 1);

        this.experience = new FasterList();
        this.experienceIndex = 0;

        this.t = 0;

        this.lastReward = Double.NaN;
        this.lastState = null;
        this.currentState = null;
        this.lastAction = 0;
        this.currentAction = 0;
    }

    private static Mat createRandMat(Random rand, final int n, final int d) {
        final Mat mat = new Mat(n, d);
        Arrays.setAll(mat.w, i -> rand.nextGaussian() / 100);
        return mat;
    }

    private static int toInteger(final double val) {
        return (int) Math.round(val);
    }

    /** TODO fair random selection when exist equal values */
    private static int maxIndex(final double[] arr) {
        int maxIndex = 0;
        double maxVal = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > maxVal) {
                maxIndex = i;
                maxVal = arr[i];
            }
        }
        return maxIndex;
    }

    private Mat calcQ(final Mat state, final boolean needsBackprop) {
        this.lastG = new Graph(needsBackprop);
        return this.lastG.add(this.lastG.mul(this.W2, this.lastG.tanh(this.lastG.add(this.lastG.mul(this.W1, state), this.B1))), this.B2);
    }

    public int act(final double[] stateArr) {
        final Mat state = new Mat(this.inputs, 1, stateArr);

        final int action = rand.nextFloat() < this.epsilon ?
                rand.nextInt(this.actions) :
                DQN3.maxIndex(this.calcQ(state, false).w);
        this.lastState = this.currentState;
        this.lastAction = this.currentAction;
        this.currentState = state;
        this.currentAction = action;
        return action;
    }

    public void learn(final double reward) {
        if (isFirstRun() || lastState==null) {
            this.lastReward = reward;
            return;
        }

        Experience x = new Experience(this.lastState, this.lastAction, this.lastReward, this.currentState);
        this.learnFromTuple(x);
        if (this.t % this.experienceAddEvery == 0) {

            if (this.experience.size() > this.experienceIndex)
                this.experience.set(this.experienceIndex, x);
            else
                this.experience.add(this.experienceIndex, x);

            if (++this.experienceIndex > this.experienceSize)
                this.experienceIndex = 0;
        }
        this.t++;

//        if (this.t % this.saveInterval == 0)
//            this.saveModel();

        IntStream.range(0, Math.min(experience.size(), this.learningStepsPerIteration))
                .mapToObj(i -> this.experience.get(rand))
                .forEach(this::learnFromTuple);
        this.lastReward = reward;
    }

    private boolean isFirstRun() {
        return lastReward!=lastReward;
    }

    private void learnFromTuple(final Experience exp) {
        final Mat tMat = this.calcQ(exp.getCurrentState(), false);
        final double qMax = exp.getLastReward() + this.gamma * Arrays.stream(tMat.w).max().orElseThrow();

        final Mat pred = this.calcQ(exp.getLastState(), true);
        double tdError = pred.w[exp.getLastAction()] - qMax;
        if (Math.abs(tdError) > this.tdErrorClamp) {
            tdError = tdError > this.tdErrorClamp ?
                    this.tdErrorClamp :
                    -this.tdErrorClamp;
        }
        pred.dw[exp.getLastAction()] = tdError;
        this.lastG.backward();

        this.W1.update(this.alpha);
        this.W2.update(this.alpha);
        this.B1.update(this.alpha);
        this.B2.update(this.alpha);
    }

    @Override
    protected synchronized int decide(float[] actionFeedback /* TODO */, float reward, float[] input) {
        //if (currentState!=null)
            learn(reward);
        return act(Util.toDouble(input));
    }

//    private void saveModel() {
//        final File file = new File("dqnAgentW1.json");
//        final File file1 = new File("dqnAgentW2.json");
//        final File file2 = new File("dqnAgentB1.json");
//        final File file3 = new File("dqnAgentB2.json");
//        try {
//            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
//            writer.write(new Gson().toJson(this.W1));
//
//            writer = new BufferedWriter(new FileWriter(file1));
//            writer.write(new Gson().toJson(this.W2));
//
//            writer = new BufferedWriter(new FileWriter(file2));
//            writer.write(new Gson().toJson(this.B1));
//
//            writer = new BufferedWriter(new FileWriter(file3));
//            writer.write(new Gson().toJson(this.B2));
//        } catch (final IOException e) {
//            e.printStackTrace();
//        }
//    }

//    public Mat[] loadModel() {
//        final File file = new File("dqnAgentW1.json");
//        final File file1 = new File("dqnAgentW2.json");
//        final File file2 = new File("dqnAgentB1.json");
//        final File file3 = new File("dqnAgentB2.json");
//        if (!file.exists() || !file1.exists() || !file2.exists() || !file3.exists()) {
//            return null;
//        }
//        try {
//            final Gson gson = new Gson();
//            BufferedReader reader = new BufferedReader(new FileReader(file));
//            final Mat w1 = gson.fromJson(reader.lines().collect(Collectors.joining()), this.W1.getClass());
//
//            reader = new BufferedReader(new FileReader(file1));
//            final Mat w2 = gson.fromJson(reader.lines().collect(Collectors.joining()), this.W2.getClass());
//
//            reader = new BufferedReader(new FileReader(file2));
//            final Mat b1 = gson.fromJson(reader.lines().collect(Collectors.joining()), this.B1.getClass());
//
//            reader = new BufferedReader(new FileReader(file3));
//            final Mat b2 = gson.fromJson(reader.lines().collect(Collectors.joining()), this.B2.getClass());
//
//            return new Mat[]{w1, w2, b1, b2};
//        } catch (final FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        throw new RuntimeException();
//    }

    public enum Option {
        GAMMA, EPSILON, ALPHA, EXPERIENCE_ADD_EVERY, EXPERIENCE_SIZE, LEARNING_STEPS_PER_ITERATION, TD_ERROR_CLAMP,
        //SAVE_INTERVAL,
        NUM_HIDDEN_UNITS
    }

    static class Experience {
        private final Mat lastState;
        private final int lastAction;
        private final double lastReward;
        private final Mat currentState;

        Experience(final Mat lastState, final int lastAction, final double lastReward, final Mat currentState) {
            this.lastState = lastState;
            this.lastAction = lastAction;
            this.lastReward = lastReward;
            this.currentState = currentState;
        }

        Mat getLastState() {
            return this.lastState;
        }

        int getLastAction() {
            return this.lastAction;
        }

        double getLastReward() {
            return this.lastReward;
        }

        Mat getCurrentState() {
            return this.currentState;
        }
    }
}
