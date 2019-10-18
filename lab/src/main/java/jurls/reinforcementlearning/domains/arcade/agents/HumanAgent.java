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

import jurls.reinforcementlearning.domains.arcade.io.ConsoleRAM;
import jurls.reinforcementlearning.domains.arcade.io.RLData;
import jurls.reinforcementlearning.domains.arcade.movie.MovieGenerator;
import jurls.reinforcementlearning.domains.arcade.screen.ScreenMatrix;

import java.awt.image.BufferedImage;

/** An 'agent' meant to be controlled by a human. Used to play the game and
 *   demonstrate the GUI.
 *
 * @author Marc G. Bellemare
 */
public class HumanAgent extends AbstractAgent {
    /** Whether we want to export screen data to disk */
    protected boolean exportFrames;
    /** The base filename used for exporting screen data. The files will be name
     *    sequentially, e.g. frame_000000.png, frame_000001.png, etc.
     *  @see MovieGenerator
     */
    protected String exportFrameBasename = "frames/frame_";

    /** The object used to save frames to the disk */
    protected MovieGenerator movieGenerator;

    /** Variables to enforce 60 frames per second */
    protected long lastFrameTime;
    protected long lastWaitTime;
    protected final int framesPerSecond = 60;
    protected long millisFraction = 0;
    protected long timeError = 0;

    /** Variables to display relevant RL information */
    protected int rewardDisplayCounter = 0;
    protected int lastReward = 0;

    /** Keep track of whether we told the user that the game is over */
    protected boolean displayedGameOver = false;
    
    protected int numFramesToDisplayRewardFor = framesPerSecond * 1;

    public HumanAgent() {
        super();
    }

    public HumanAgent(boolean useGUI, String namedPipesName, boolean exportFrames) {
        super(useGUI, namedPipesName);

        this.exportFrames = exportFrames;

        
        if (this.exportFrames) {
            movieGenerator = new MovieGenerator(exportFrameBasename);
        }
    }

    public boolean wantsScreenData() {
        return true;
    }

    public boolean wantsRamData() {
        return false;
    }

    public boolean wantsRLData() {
        return true;
    }

    public boolean shouldTerminate() {
        
        return ui.quitRequested();
    }

    @Override
    public long getPauseLength() {
        
        
        long targetDelta = 1000 / framesPerSecond;
        long deltaRemainder = 1000 % framesPerSecond;
        millisFraction += deltaRemainder;

        
        while (millisFraction > framesPerSecond) {
            targetDelta += 1;
            millisFraction -= framesPerSecond;
        }
        
        long time = System.currentTimeMillis();
        if (lastFrameTime == 0) {
            timeError += targetDelta;
        }
        else {
            long deltaTime = time - lastFrameTime;
            
            timeError += targetDelta - (deltaTime - lastWaitTime);
        }

        lastFrameTime = time;

        if (timeError > 0) {
            lastWaitTime = timeError;
            timeError = 0;
            return lastWaitTime;
        }
        else { 
            lastWaitTime = 0;
            return 0;
        }
    }

    @Override
    public int selectAction() {
        
        int action = ui.getKeyboardAction();

        return action;
    }
    
    @Override
    public void observe(ScreenMatrix screen, ConsoleRAM ram, RLData rlData) {
        
        if (exportFrames) {
            BufferedImage image = converter.convert(screen);
            movieGenerator.record(image);
        }

        
        if (rlData.reward != 0)
            ui.addMessage("Reward: "+rlData.reward);
        
        if (rlData.isTerminal) {
            if (!displayedGameOver) {
                ui.addMessage("GAME OVER");
                displayedGameOver = true;
            }
        }
        else
            displayedGameOver = false;
    }

    /** A simple main class for running the Human agent.
     *
     * @param args
     */
    public static void main(String[] args) {
        
        boolean useGUI = true;
        String namedPipesName = null;
        boolean exportFrames = false;
        
        
        int argIndex = 0;

        boolean doneParsing = (args.length == 0);

        
        while (!doneParsing) {
            
            if ("-nogui".equals(args[argIndex])) {
                useGUI = false;
                argIndex++;
            }
            
            
            else if ("-named_pipes".equals(args[argIndex]) && (argIndex + 1) < args.length) {
                namedPipesName = args[argIndex+1];

                argIndex += 2;
            }
            
            else if ("-export_frames".equals(args[argIndex])) {
                exportFrames = true;
                argIndex++;
            }
            
            else {
                printUsage();
                System.exit(-1);
            }

            
            if (argIndex >= args.length)
                doneParsing = true;
        }

        HumanAgent agent = new HumanAgent(useGUI, namedPipesName, exportFrames);

        agent.run();
    }

    /** Prints out command-line usage text.
     *
     */
    public static void printUsage() {
        System.err.println ("Invalid argument.");
        System.err.println ("Usage: java HumanAgent [-nogui] [-named_pipes filename] [-export_frames]\n");
        System.err.println ("Example: java HumanAgent -named_pipes /tmp/ale_fifo_");
        System.err.println ("  Will start an agent that communicates with ALE via named pipes \n"+
                "  /tmp/ale_fifo_in and /tmp/ale_fifo_out");
    }
}
