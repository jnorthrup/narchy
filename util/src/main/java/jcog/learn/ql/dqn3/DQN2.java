package jcog.learn.ql.dqn3;

import jcog.Util;
import jcog.decide.DecideEpsilonGreedy;
import jcog.decide.Deciding;
import jcog.learn.Agent;
import jcog.learn.LivePredictor;
import jcog.learn.Predictor;
import jcog.random.XoRoShiRo128PlusRandom;

import java.util.Arrays;
import java.util.Random;

/** adapted from: https://github.com/nunomachado/nando-dql
 * TODO add replay buffer in subclass
 * untested
 * */
public class DQN2 extends Agent {

    private final Random random;
    private final int actions;

    private int lastAction = -1;
    private final float[] lastState;

    /* list of memories with the last game moves */
//    private List<Memory> mem;

//    /* neural network (NN) */
//    private BasicNetwork network;
//
//    /* backpropagation for trainign the NN*/
//    private ResilientPropagation train;
    //public final MLPMap valuePredict;
    public final Predictor valuePredict;


    final Deciding actionDecider;

    /* Deep Q-learning configuration */
    final QLearningConfig learnConfig;
    private final double[] targetFuture;

    public DQN2(int inputs, int actions) {
        this(inputs, actions, new XoRoShiRo128PlusRandom(1));
    }

    public DQN2( int inputs, int actions, Random rng ) {
        super(inputs, actions);

        this.random = rng;

        actionDecider = new DecideEpsilonGreedy(0.05f, rng);
        learnConfig = new QLearningConfig();
        //mem = new FasterList<Memory>( experienceCapacity  );

        this.lastState = new float[inputs];
        this.targetFuture = new double[actions];
        this.actions = actions;

        //LSTM
        valuePredict = new LivePredictor.LSTMPredictor(0.04f, 4);

        //MLP
        //valuePredict = new LivePredictor.MLPPredictor(0.05f, new XoRoShiRo128PlusRandom(1));

        valuePredict.learn(new double[inputs], new double[actions]); /** HACK initialize */

//            network = new BasicNetwork();
//            network.addLayer( new BasicLayer( null, true, 9 ) );
//            network.addLayer( new BasicLayer( new ActivationTANH(), true, 27 ) );
//            network.addLayer( new BasicLayer( new ActivationTANH(), true, 27 ) );
//            network.addLayer( new BasicLayer( new ActivationTANH(), true, 27 ) );
//            network.addLayer( new BasicLayer( new ActivationSigmoid(), false, 9 ) );
//            network.getStructure().finalizeStructure();
//            network.reset();

    }


//    /**
//     * Store a move for posterior replay
//     *
//     * @param m
//     */
//    public void remember( Memory m ) {
//            mem.addAt( m );
//    }



    /**
     * Play the next move by:
     * 1) using the NN to compute the per-action reward based on the current state
     * 2) picking the available action with the highest reward
     *
     * @param actionFeedback
     * @param input
     * @return
     */
    @Override public int decide(float[] actionFeedback, float reward, float[] input) {

        double[] rewardPrediction = valuePredict.predict(input);

        //train the network with a memory
        if (lastAction!=-1) {
/*
            from: rl.js
            // want: Q(s,a) = r + gamma * max_a' Q(s',a')

            // compute the target Q value
            var tmat = this.forwardQ(this.net, s1, false);
            var qmax = r0 + this.gamma * tmat.w[R.maxi(tmat.w)];

            // now predict
            var pred = this.forwardQ(this.net, s0, true);

            var tderror = pred.w[a0] - qmax;
            var clamp = this.tderror_clamp;
            //if (Math.abs(tderror) > clamp) {  // huber loss to robustify
                if (tderror > clamp) tderror = clamp;
                if (tderror < -clamp) tderror = -clamp;
            //}
            pred.dw[a0] = tderror;
            this.lastG.backward(); // compute gradients on net params

            // update net
            R.updateNet(this.net, this.alpha);
             */

            //predict the future discounted reward
            float target =
                    //reward;
                    (float) ((reward) + learnConfig.getGamma() *
                            //Util.max(rewardPrediction));
                            Util.max(rewardPrediction)-Util.min(rewardPrediction));


            //make the agent to approximately map
            //the current state to future discounted reward

            Arrays.fill(targetFuture, 0);
            //System.arraycopy(rewardPrediction, 0, targetFuture, 0, actions);

            targetFuture[lastAction] =
                    //Util.clamp((float) (rewardPrediction[lastAction] - target), -1, +1);
                    //(float) (rewardPrediction[lastAction] - target);
                    target;

            valuePredict.learn(Util.toDouble(lastState), targetFuture);

        }

        ((DecideEpsilonGreedy)actionDecider).epsilonRandom = (float) learnConfig.getEpsilon();
        int nextAction = actionDecider.applyAsInt(rewardPrediction);

        System.arraycopy(input, 0, lastState, 0, input.length);

        return this.lastAction = nextAction;
    }




//    public void replay( int batchSize ) {
//
//        Collections.shuffle(mem); //HACK expensive
//
//        int size = Math.min( mem.size(), batchSize );
//        double target = 0;
//
//        for ( int i = 0; i < size; i++ )
//        {
//            Memory m = mem.get( i );
//
//            //if done, make our target reward
//            target = m.reward;
//
//            if ( !m.isDone() )
//            {
//                //predict the future discounted reward
//                MLData prediction = network.compute( m.nextState.toMLData() );
//                Pair<Integer, Double> maxReward = getMaxReward( prediction );
//                target = m.reward + learnConfig.getGamma() * maxReward.getValue();
//            }
//
//            //make the agent to approximately map
//            //the current state to future discounted reward
//            MLData targetFuture = network.compute( m.state.toMLData() );
//            targetFuture.setData( Actions.getActionIndex( m.action ), target );
//            double[][] targetOutput =
//                    { targetFuture.getData() }; //transform targetFuture into double[][] as required by the BasicMLDataSet
//
//            //Train the Neural Net with the state and targetFuture (with the updated reward value)
//            MLDataSet trainingSet = new BasicMLDataSet( m.state.toTrainingData(), targetOutput );
//            train = new ResilientPropagation( network, trainingSet );
//            train.iteration();
//
//        }
//
//        //update epsilon (exploration rate)
//        double epsilon = learnConfig.getEpsilon();
//        if ( epsilon > learnConfig.getEpsilon_min() )
//        {
//            epsilon *= learnConfig.getEpsilon_decay();
//            learnConfig.setEpsilon( epsilon );
//        }
//    }

//    /**
//     * Play an 'epoch' number of games to train the agent
//     */
//    public void trainAgent( Agent adversary )
//    {
//        //to measure improvement via training
//        int totalReward = 0;
//        int epochs = learnConfig.getEpochs();
//        ArrayList<Integer> rewardLog = new ArrayList<Integer>( epochs );
//
//        for ( int i = 0; i < epochs; i++ )
//        {
//            System.out.println( "\n ====== EPOCH " + i + " ======" );
//            //initialize game-board and current status
//            GameMain game = new GameMain();
//            Random r = new Random();
//            game.currentPlayer = r.nextBoolean() ? Seed.CROSS : Seed.NOUGHT; //randomly pick first player
//            int moves = 1;
//
//            do
//            {
//                boolean validAction = false;
//                double reward = 0;
//                boolean isDone = false;
//                Board oldBoard = game.board.clone();
//                String a;
//
//                //this agent's turn to play
//                if ( game.currentPlayer == Seed.CROSS )
//                {
//                    //////// don't penalizing invalid actions
//                    do
//                    {
//                        a = this.act( game.board );
//                        String[] actions = a.split( " " );
//                        System.out.println( ">> X in " + a );
//                        validAction = game.playerMove( game.currentPlayer, Integer.valueOf( actions[0] ),
//                                Integer.valueOf( actions[1] ) );
//                    }
//                    while ( !validAction );
//
//                    //update board and currentState
//                    reward = game.updateGame( game.currentPlayer );
//                    isDone = ( game.currentState != GameState.PLAYING );
//                    //*//////////////////////////////////////
//
//                    ////////// trying to learn invalid actions by penalizing them
//                   /* a = this.act(game.board);
//                    String[] actions = a.split(" ");
//                    System.out.println(">> X in " + a);
//                    validAction = game.playerMove(game.currentPlayer, Integer.valueOf(actions[0]), Integer.valueOf(actions[1]));
//                    if(!validAction){
//                        //if the action isn't valid, the reward is 0
//                        reward = 0;
//                        isDone = false;
//                    }
//                    else{
//                        //update board and currentState
//                        reward = game.updateGame(game.currentPlayer);
//                        isDone = (game.currentState != GameState.PLAYING);
//                    }
//                    //*//////////////////////////////////////
//                }
//                else
//                { //the other agent's turn
//                    do
//                    {
//                        a = adversary.act( game.board );
//                        String[] actions = a.split( " " );
//                        System.out.println( ">> O in " + a );
//                        validAction = game.playerMove( game.currentPlayer, Integer.valueOf( actions[0] ),
//                                Integer.valueOf( actions[1] ) );
//                    }
//                    while ( !validAction );
//
//                    //update board and currentState
//                    reward = game.updateGame( game.currentPlayer );
//                    isDone = ( game.currentState != GameState.PLAYING );
//                }
//
//                //Remember the previous state, action, reward, and done (after both agents have played)
//                if ( game.currentPlayer == Seed.CROSS || isDone )
//                {
//                    if ( game.currentPlayer == Seed.CROSS )
//                    {
//                        Memory m = new Memory( oldBoard, a, reward, game.board.clone(), isDone );
//                        this.remember( m );
//                    }
//                    else
//                    {//means that we just lost/tied the game due to a play by the other player
//                        //change the reward of our last move to -1 or 0.5
//                        this.memoryList.get( memoryList.size() - 1 ).reward = reward;
//                        this.memoryList.get( memoryList.size() - 1 ).setNextState( game.board );
//                    }
//                }
//                else if ( moves > 1 )
//                {
//                    //update the board of previously stored memory with the adversary's play
//                    this.memoryList.get( memoryList.size() - 1 ).setNextState( game.board );
//                }
//
//                game.board.paint();
//
//                // Print message and reward if game ended
//                if ( isDone )
//                {
//                    if ( game.currentState == GameState.CROSS_WON )
//                    {
//                        System.out.print( "'X' WON! =)" );
//                    }
//                    else if ( game.currentState == GameState.NOUGHT_WON )
//                    {
//                        System.out.print( "'O' won! =(" );
//                    }
//                    else if ( game.currentState == GameState.DRAW )
//                    {
//                        System.out.print( "It's Draw! =/" );
//                    }
//                    totalReward += reward;
//                    rewardLog.addAt( totalReward );
//                    System.out.println( " REWARD: " + reward + " (" + moves + " moves)" );
//                }
//
//                // Switch player if previous action was valid
//                if ( validAction )
//                {
//                    moves++;
//                    game.currentPlayer = ( game.currentPlayer == Seed.CROSS ) ? Seed.NOUGHT : Seed.CROSS;
//                }
//
//            }
//            while ( game.currentState == GameState.PLAYING );  // repeat until game-over
//
//            this.replay( learnConfig.getBatchsize() );
//        }
//        System.out.println( "TOTAL REWARD " + totalReward );
//        DrawPlot plot = new DrawPlot( this.toString() + " vs " + adversary.toString() + " for " + epochs + " rounds" );
//        plot.drawRewardPlot( rewardLog, "Reward" );
//    }


    /**
     * A action
     * S state
     */
    public static class Memory<A,S> {
        private int hash; //used to speed up equals
        public S state;  //origin state
        public A action; //action consisting of a string of format "r c" meaning put an X in row r and column c
        public double reward;    //reward obtained by applying action to state
        public S nextState; //destination state after applying action to origin state
        boolean done; //indicates if state is the final state

        public Memory(S s, A a, double r, S ns, boolean isFinalState) {
            state = s;
            action = a;
            reward = r;
            nextState = ns;
            done = isFinalState;

            //compute hash
            computeHash();

        }

        public boolean isDone() {
            return done;
        }

        public void setState(S newState) {
            this.state = newState;
        }

        public void setNextState(S newState) {
            this.nextState = newState;

            //update the hash after setting the new state
            computeHash();
        }

        /**
         * Two memories are the same if their action is identical and yields the same next state
         */
        private void computeHash() {
            hash = Util.hashCombine(action.hashCode(), nextState.hashCode());
        }

        @Override
        public boolean equals(Object o) {
            return this == o || action.equals(((Memory)o).action) && nextState.equals(((Memory)o).nextState);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static class QLearningConfig {
        /* a number of games we want the agent to play */
        private int epochs;

        /* decay or discount rate, to calculate the future discounted reward */
        private double gamma;

        /* exploration rate -> the rate in which an agent randomly decides its action rather than prediction */
        private double epsilon;

        /* we want to decrease the number of explorations as it gets good at playing games */
        private double epsilon_decay;

        /* we want the agent to explore at least this amount */
        private double epsilon_min;

        /* determines how much the neural net learns in each iteration */
        private double learning_rate;

        /* the number of memories used during replay */
        private int batchsize;

        public QLearningConfig()
        {
            //default configuration
            epochs = 2500;
            gamma = 0.5;
            epsilon = 0.1;
            epsilon_decay = 0.995;
            epsilon_min = 0.01;
            learning_rate = 0.1;
            batchsize = 128;
        }

        public QLearningConfig(int epochs, double gamma, double epsilon, double epsilon_decay, double epsilon_min, double learning_rate, int batchsize) {
            this.epochs = epochs;
            this.gamma = gamma;
            this.epsilon = epsilon;
            this.epsilon_decay = epsilon_decay;
            this.epsilon_min = epsilon_min;
            this.learning_rate = learning_rate;
            this.batchsize = batchsize;
        }

        public int getEpochs() {
            return epochs;
        }

        public void setEpochs(int epochs) {
            this.epochs = epochs;
        }

        public double getGamma() {
            return gamma;
        }

        public void setGamma(double gamma) {
            this.gamma = gamma;
        }

        public double getEpsilon() {
            return epsilon;
        }

        public void setEpsilon(double epsilon) {
            this.epsilon = epsilon;
        }

        public double getEpsilon_decay() {
            return epsilon_decay;
        }

        public void setEpsilon_decay(double epsilon_decay) {
            this.epsilon_decay = epsilon_decay;
        }

        public double getEpsilon_min() {
            return epsilon_min;
        }

        public void setEpsilon_min(double epsilon_min) {
            this.epsilon_min = epsilon_min;
        }

        public double getLearning_rate() {
            return learning_rate;
        }

        public void setLearning_rate(double learning_rate) {
            this.learning_rate = learning_rate;
        }

        public int getBatchsize() {
            return batchsize;
        }

        public void setBatchsize(int batchsize) {
            this.batchsize = batchsize;
        }
    }
}
