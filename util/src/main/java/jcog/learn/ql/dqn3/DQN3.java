package jcog.learn.ql.dqn3;


import jcog.Util;
import jcog.data.list.FasterList;
import jcog.learn.Agent;
import jcog.random.XoRoShiRo128PlusRandom;

import java.util.Arrays;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Random;

/** untested */
public class DQN3 extends Agent {
    private final Random rand = new XoRoShiRo128PlusRandom();


    private final double gamma;
    private final int experienceAddPeriod;
    private final int experienceSize;
    private final int experienceLearnedPerIteration;
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
//    private int lastAction;
    private int currentAction;
    private Graph lastG;

    public DQN3(final int inputs, final int numActions, final Map<Option, Double> config) {
        super(inputs, numActions);

        this.gamma = config.getOrDefault(Option.GAMMA, 0.75);
        this.epsilon = config.getOrDefault(Option.EPSILON, 0.1);
        this.alpha = config.getOrDefault(Option.ALPHA, 0.05);

        this.numHiddenUnits = (int) Math.round(config.getOrDefault(Option.NUM_HIDDEN_UNITS, 100.0));

        this.experienceAddPeriod = (int) Math.round(config.getOrDefault(Option.EXPERIENCE_ADD_EVERY, 25.0));
        this.experienceSize = (int) Math.round(config.getOrDefault(Option.EXPERIENCE_SIZE, 5000.0));
        this.experienceLearnedPerIteration = (int) Math.round(config.getOrDefault(Option.LEARNING_STEPS_PER_ITERATION, 10.0));
        this.tdErrorClamp = config.getOrDefault(Option.TD_ERROR_CLAMP, 1.0);

        this.W1 = DQN3.matRandom(rand, this.numHiddenUnits, this.inputs, 1f/numHiddenUnits);
        this.B1 = new Mat(this.numHiddenUnits, 1);
        this.W2 = DQN3.matRandom(rand, this.actions, this.numHiddenUnits, 1/numHiddenUnits);
        this.B2 = new Mat(this.actions, 1);

        this.experience = new FasterList(experienceSize);
        this.experienceIndex = 0;

        this.t = 0;

        this.lastReward = Double.NaN;
        this.lastState = null;
        this.currentState = null;
//        this.lastAction = 0;
        this.currentAction = 0;
    }

    @Override
    protected synchronized int decide(float[] actionFeedback /* TODO */, float reward, float[] input) {
        //if (currentState!=null)
        learn(actionFeedback, reward);
        return act(Util.toDouble(input));
    }

    private static Mat matRandom(Random rand, final int n, final int d, float range) {
        final Mat mat = new Mat(n, d);
        Arrays.setAll(mat.w, i -> rand.nextGaussian() * range);
        return mat;
    }

    private Mat calcQ(final Mat state, final boolean needsBackprop) {
        Graph g = this.lastG = new Graph(needsBackprop);
        return g.add(g.mul(W2, g.tanh(g.add(g.mul(W1, state), B1))), B2);
    }

    public int act(final double[] stateArr) {

        final Mat state = new Mat(this.inputs, 1, stateArr);

        final int action = decide(state);

        this.lastState = this.currentState;
//        this.lastAction = this.currentAction;
        this.currentState = state;
        this.currentAction = action;
        return action;
    }

    protected int decide(Mat state) {
        //this is greedy epsilon action selector TODO abstract
        return rand.nextFloat() < this.epsilon ?
                rand.nextInt(this.actions) :
                Util.argmax(this.calcQ(state, false).w);
    }

    public void learn(float[] actionFeedback, final double reward) {
        if (isFirstRun() || lastState==null) {
            this.lastReward = reward;
            return;
        }

        Experience x = new Experience(this.lastState, actionFeedback.clone(), this.lastReward, this.currentState);
        this.learn(x);
        if (this.t++ % this.experienceAddPeriod == 0) {

            if (this.experience.size() > this.experienceIndex)
                this.experience.set(this.experienceIndex, x);
            else
                this.experience.add(this.experienceIndex, x);

            if (++this.experienceIndex > this.experienceSize)
                this.experienceIndex = 0;
        }


//        if (this.t % this.saveInterval == 0)
//            this.saveModel();

        int bound = Math.min(experience.size(), this.experienceLearnedPerIteration);
        for (int i = 0; i < bound; i++) {
            learn(this.experience.get(rand));
        }
        this.lastReward = reward;
    }

    private boolean isFirstRun() {
        return lastReward!=lastReward;
    }

    private void learn(final Experience exp) {
        final Mat tMat = this.calcQ(exp.currentState, false);
        boolean seen = false;
        double best = 0;
        for (double v : tMat.w) {
            if (!seen || Double.compare(v, best) > 0) {
                seen = true;
                best = v;
            }
        }
        final double qMax = exp.lastReward + this.gamma * (seen ? OptionalDouble.of(best) : OptionalDouble.empty()).orElseThrow();

        final Mat pred = this.calcQ(exp.lastState, true);

        for (int i = 0; i < exp.lastAction.length; i++) {
            double tdError = Util.clamp((pred.w[i] * exp.lastAction[i]) - qMax, -tdErrorClamp, tdErrorClamp);
            pred.dw[i] = tdError;
        }

        this.lastG.backward();

        this.W1.update(this.alpha);
        this.W2.update(this.alpha);
        this.B1.update(this.alpha);
        this.B2.update(this.alpha);
    }


    public enum Option {
        GAMMA, EPSILON, ALPHA, EXPERIENCE_ADD_EVERY, EXPERIENCE_SIZE, LEARNING_STEPS_PER_ITERATION, TD_ERROR_CLAMP,
        //SAVE_INTERVAL,
        NUM_HIDDEN_UNITS
    }

    static class Experience {
        public final Mat lastState;
        public final float[] lastAction;
        public final double lastReward;
        public final Mat currentState;

        Experience(final Mat lastState, float[] lastAction, final double lastReward, final Mat currentState) {
            this.lastState = lastState;
            this.lastAction = lastAction;
            this.lastReward = lastReward;
            this.currentState = currentState;
        }

    }
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

