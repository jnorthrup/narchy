/*
 * Java Arcade Learning Environment (A.L.E) Agent
 *  Copyright (C) 2011-2012 Marc G. Bellemare <mgbellemare@ualberta.ca>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http:
 */
package jurls.reinforcementlearning.domains.arcade.agents;

import jurls.reinforcementlearning.domains.arcade.io.Actions;
import jurls.reinforcementlearning.domains.arcade.io.ConsoleRAM;
import jurls.reinforcementlearning.domains.arcade.io.RLData;
import jurls.reinforcementlearning.domains.arcade.rl.FeatureMap;
import jurls.reinforcementlearning.domains.arcade.rl.FrameHistory;
import jurls.reinforcementlearning.domains.arcade.rl.SarsaLearner;
import jurls.reinforcementlearning.domains.arcade.screen.ScreenMatrix;

/** A RL agent which learns using the SARSA(lambda) algorithm and acts according
 *   to an epsilon-greedy policy.
 *  At the time of writing this comment, the agent is in charge of resetting the
 *   system at the end of an episode.
 *
 * @author Marc G. Bellemare <mgbellemare@ualberta.ca>
 */
public class RLAgent extends AbstractAgent {
    protected final int numActions = Actions.numPlayerActions;

    /** The map from raw screen data to a feature vector */
    protected FeatureMap featureMap;
    /** The agent core (SARSA and epsilon-greedy) */
    protected SarsaLearner learner;

    /** The action selected by the learner */
    protected int learnerAction;
    /** A history of recent screen data */
    protected FrameHistory history;

    /** Keeping track of how many episodes we've played */
    protected int episodeNumber;
    protected int maxNumEpisodes = 10;
    
    /** Whether this is the first step of a given episode */
    protected boolean firstStep;
    /** Whether we should send a reset action */
    protected boolean requestReset;

    /** Creates a new RL agent.
     * 
     * @param useGUI
     * @param pipesBasename
     */
    public RLAgent(boolean useGUI, String pipesBasename) {
        super(useGUI, pipesBasename);
        featureMap = new FeatureMap();
        
        learner = new SarsaLearner(featureMap.numFeatures(), numActions);

        
        learner.setAlpha(0.01 / featureMap.numFeatures());

        var requiredHistoryLength = FeatureMap.historyLength();
        
        history = new FrameHistory(requiredHistoryLength);

        requestReset = true;
        episodeNumber = 1;
    }

    public boolean shouldTerminate() {
        
        return (io.wantsTerminate() || episodeNumber > maxNumEpisodes);
    }

    @Override
    public long getPauseLength() {
        return 0;
    }

    @Override
    public int selectAction() {
        
        if (requestReset) {
            firstStep = true;
            requestReset = false;
            return Actions.map("system_reset");
        }

        
        else
            return learnerAction;
    }

    @Override
    public void observe(ScreenMatrix image, ConsoleRAM ram, RLData rlData) {
        
        history.addFrame(image);
        
        rlStep(image, ram, rlData);
    }

    /** Take one RL step by observing an image and selecting the next action.
     *   This is done by invoking the SarsaLearner's agent_ methods.
     * 
     * @param image
     * @param ram
     * @param features
     */
    public void rlStep(ScreenMatrix image, ConsoleRAM ram, RLData rlData) {

        var features = featureMap.getFeatures(history);

        if (firstStep) {
            
            learnerAction = learner.agent_start(features);

            firstStep = false;
        }
        else {
            var terminal = rlData.isTerminal;
            double reward = rlData.reward;

            
            if (!terminal)
                learnerAction = learner.agent_step(reward, features);
            
            
            else
                episodeEnd(reward);
        }
    }

    /** Perform an end-of-episode learning step */
    protected void episodeEnd(double reward) {
        learner.agent_end(reward);
        
        learnerAction = Actions.map("player_a_noop");

        
        requestReset = true;

        
        System.err.println ("Episode "+episodeNumber);
        episodeNumber++;

        if (episodeNumber > maxNumEpisodes)
            System.err.println (maxNumEpisodes+" episodes, terminating...");
    }

    public boolean wantsRamData() {
        return false;
    }
    
    public boolean wantsRLData() {
        return true;
    }

    public boolean wantsScreenData() {
        return true;
    }

    /** Main class for running the RL agent.
     * 
     * @param args
     */
    public static void main(String[] args) {

        var useGUI = true;
        String namedPipesName = null;


        var argIndex = 0;

        var doneParsing = (args.length == 0);

        
        while (!doneParsing) {
            
            if ("-nogui".equals(args[argIndex])) {
                useGUI = false;
                argIndex++;
            }
            
            
            else if ("-named_pipes".equals(args[argIndex]) && (argIndex + 1) < args.length) {
                namedPipesName = args[argIndex+1];

                argIndex += 2;
            }
            
            else {
                printUsage();
                System.exit(-1);
            }

            
            if (argIndex >= args.length)
                doneParsing = true;
        }

        var agent = new RLAgent(useGUI, namedPipesName);

        agent.run();
    }

    /** Prints out command-line usage text.
     *
     */
    public static void printUsage() {
        System.err.println ("Invalid argument.");
        System.err.println ("Usage: java RLAgent [-nogui] [-named_pipes filename]\n");
        System.err.println ("Example: java RLAgent -named_pipes /tmp/ale_fifo_");
        System.err.println ("  Will start an agent that communicates with ALE via named pipes \n"+
                "  /tmp/ale_fifo_in and /tmp/ale_fifo_out");
    }
}
