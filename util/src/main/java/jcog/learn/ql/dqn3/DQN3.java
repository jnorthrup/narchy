package jcog.learn.ql.dqn3;


import jcog.Util;
import jcog.data.list.FasterList;
import jcog.decide.DecideEpsilonGreedy;
import jcog.decide.Deciding;
import jcog.learn.Agent;
import jcog.random.XoRoShiRo128PlusRandom;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

/** untested */
public class DQN3 extends Agent {



    public double gamma;
    private final float experienceAddProb;
    private final int experienceSize;
    private final int experienceLearnedPerIteration;
    private final double tdErrorClamp;

    private final double alpha;
    public final Mat W1;
    public final Mat B1;
    public final Mat W2;
    public final Mat B2;
    private final FasterList<Experience> experience;
    private int experienceIndex;

    private double lastReward;
    private Mat lastState;
    public Mat currentState;
//    private int lastAction;
    private int currentAction;
    public double[] input;

    public double lastErr;

    final Random rng = new XoRoShiRo128PlusRandom();
    final Deciding actionDecider = new DecideEpsilonGreedy(0.1f, rng);

    public DQN3(int inputs, int numActions) {
        this(inputs, numActions, Map.of( /* empty */));
    }

    public DQN3(int inputs, int numActions, Map<Option, Double> config) {
        super(inputs, numActions);

        this.gamma = config.getOrDefault(Option.GAMMA, 0.9);
        this.alpha = config.getOrDefault(Option.ALPHA, 0.01);

        /* estimate */
        int numHiddenUnits = (int) Math.round(config.getOrDefault(Option.NUM_HIDDEN_UNITS, (double) inputs * numActions /* estimate */));

        this.experienceAddProb = 1f/(int) Math.round(config.getOrDefault(Option.EXPERIENCE_ADD_EVERY, 25.0));
        this.experienceSize = (int) Math.round(config.getOrDefault(Option.EXPERIENCE_SIZE, 64.0 /* 1024 */));
        this.experienceLearnedPerIteration = (int) Math.round(config.getOrDefault(Option.LEARNING_STEPS_PER_ITERATION, 4.0));
        this.tdErrorClamp = config.getOrDefault(Option.TD_ERROR_CLAMP, 1.0);

        float rngRange =
                (float) (this.alpha / numHiddenUnits);
                //0.5f;
                //0.01f;

        this.W1 = DQN3.matRandom(rng, numHiddenUnits, this.inputs, rngRange);
        this.B1 = DQN3.matRandom(rng, numHiddenUnits, 1, rngRange);
        this.W2 = DQN3.matRandom(rng, this.actions, numHiddenUnits, rngRange);
        this.B2 = DQN3.matRandom(rng, this.actions, 1, rngRange);

        this.experience = new FasterList(experienceSize);
        this.experienceIndex = 0;



        this.lastReward = Double.NaN;
        this.lastState = null;
        this.currentState = null;
//        this.lastAction = 0;
        this.currentAction = -1;
    }


    @Override
    protected synchronized int decide(float[] actionFeedback /* TODO */, float reward, float[] input) {

        //bipolarize
//        for (int i = 0, actionFeedbackLength = actionFeedback.length; i < actionFeedbackLength; i++)
//            actionFeedback[i] = (actionFeedback[i]-0.5f)*2;

        double err = learn(actionFeedback, reward);


        lastErr = err;
        //System.out.println(this + " err=" + err);
        return act(Util.toDouble(input));
    }

    private static Mat matRandom(Random rand, int n, int d, float range) {
        Mat mat = new Mat(n, d);
        Arrays.setAll(mat.w, i -> rand.nextGaussian() * range);
        return mat;
    }

    private Mat calcQ(Mat s) {
        return calcQ(s, null);
    }

    private Mat calcQ(Mat s, @Nullable MatrixTransform g) {

        if(g==null) g = new MatrixTransform(false);

        Mat m = g.add(g.mul(W2, g.tanh(g.add(g.mul(W1, s), B1))), B2);
        //Mat m = g.add(g.tanh(g.mul(W2, g.tanh(g.add(g.mul(W1, s), B1)))), B2);
        //Mat m = g.add(g.tanh(g.mul(W2, g.add(g.mul(W1, s), B1))), B2);


        return m;
    }

    public int act(double[] stateArr) {
        this.input = stateArr;
        Mat state = new Mat(this.inputs, 1, stateArr);

        int action = decide(state);

        this.lastState = this.currentState;
//        this.lastAction = this.currentAction;
        this.currentState = state;
        this.currentAction = action;
        return action;
    }

    protected int decide(Mat state) {
        double[] qq = this.calcQ(state).w;
        return actionDecider.applyAsInt(qq);
    }

    public double learn(float[] actionFeedback, double reward) {
        if (isFirstRun() || lastState==null) {
            this.lastReward = reward;
            return reward;
        }

        Experience x = new Experience(this.lastState, actionFeedback.clone(), this.lastReward, this.currentState);
        double err = this.learn(x);
        this.lastReward = reward;

        rememberPlayBack(x);

        learnPlayBack(x);

        return err;
    }

    void learnPlayBack(Experience x) {
        int e = Math.min(experience.size(), this.experienceLearnedPerIteration);
        for (int i = 0; i < e; i++)
            learn(this.experience.get(rng));
    }

    void rememberPlayBack(Experience x) {
        if (rng.nextFloat() < this.experienceAddProb) {

            if (this.experience.size() > this.experienceIndex)
                this.experience.set(this.experienceIndex, x);
            else
                this.experience.add(this.experienceIndex, x);

            if (++this.experienceIndex > this.experienceSize)
                this.experienceIndex = 0;
        }
    }

    private boolean isFirstRun() {
        return lastReward!=lastReward;
    }

    /** returns total error */
    private double learn(Experience exp) {

        Mat next = this.calcQ(exp.currentState);


        //final double qMax = exp.lastReward + this.gamma * next.w[Util.argmax(next.w)];

        MatrixTransform g = new MatrixTransform(true);
        Mat pred = this.calcQ(exp.lastState, g);

        float actionNorm = 1; //assume already normalized
//        float actionNorm = Util.sum(exp.lastAction);
//        if (actionNorm < Float.MIN_NORMAL)
//            actionNorm = 1;

        double errTotal = 0;
        for (int i = 0; i < exp.lastAction.length; i++) {
            //var qmax = r0 + this.gamma * tmat.w[R.maxi(tmat.w)];
            double qMax = exp.lastReward + this.gamma * next.w[i];

            double err = (pred.w[i] - qMax) * exp.lastAction[i]/actionNorm;
            double tdError = Util.clamp(err, -tdErrorClamp, tdErrorClamp);
            pred.dw[i] = tdError;
            errTotal += Math.abs(err);
            //errTotal += Math.abs(tdError);
        }

        g.backward();

        this.W1.update(this.alpha);
        this.W2.update(this.alpha);
        this.B1.update(this.alpha);
        this.B2.update(this.alpha);

        return errTotal;
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

        Experience(Mat lastState, float[] lastAction, double lastReward, Mat currentState) {
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

