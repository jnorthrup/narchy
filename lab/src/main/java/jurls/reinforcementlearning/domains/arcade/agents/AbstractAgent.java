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

import jurls.reinforcementlearning.domains.arcade.gui.AbstractUI;
import jurls.reinforcementlearning.domains.arcade.gui.AgentGUI;
import jurls.reinforcementlearning.domains.arcade.gui.NullUI;
import jurls.reinforcementlearning.domains.arcade.io.ALEPipes;
import jurls.reinforcementlearning.domains.arcade.io.ConsoleRAM;
import jurls.reinforcementlearning.domains.arcade.io.RLData;
import jurls.reinforcementlearning.domains.arcade.screen.*;

import java.awt.image.BufferedImage;
import java.io.IOException;

/** An abstract agent class. New agents can be created by extending this class
 *   and implementing its abstract methods.
 * 
 * @author Marc G. Bellemare
 */
public abstract class AbstractAgent {
    /** Used to convert ALE screen data to GUI images */
    protected ScreenConverter converter;

    /** The UI used for displaying images and receiving actions */
    protected AbstractUI ui;
    /** The I/O object used to communicate with ALE */
    protected ALEPipes io;

    /** Parameters */
    /** Whether to use a GUI */
    protected boolean useGUI;
    /** If non-null, we communicate via named pipes rather than stdin/stdout */
    protected String namedPipesBasename;

    /** Create a new agent that communicates with ALE via stdin/out and
     *    uses the graphical user interface.
     */

    /** Create a new agent with the specified parameters. The user can specify
     *   the base name for two FIFO pipes used to communicate with ALE. If
     *   namedPipesBasename is not null, then the files namedPipesBasename+"_in"
     *   and namedPipesBasename+"_out" are read and written to by the agent.
     *   See ALE documentation for more details on running with named pipes.
     *
     * @param useGUI If true, a GUI is used to display received screen data.
     * @param namedPipesBasename If non-null, the base filename for the two FIFO
     *   files used to communicate with ALE.
     */
    public AbstractAgent(boolean useGUI, String namedPipesBasename) {
        init(useGUI, namedPipesBasename);
    }
    public AbstractAgent() {
        super();
    }

    protected void init(boolean useGUI, String namedPipesBasename) {
        this.useGUI = useGUI;
        this.namedPipesBasename = namedPipesBasename;

        
        ColorPalette palette = makePalette("NTSC");

        
        converter = new ScreenConverter(palette);

        init();

    }

    /** Create a color palette used to display the screen. The currently available
     *   choices are NTSC (128 colors) and SECAM (8 colors).
     * 
     * @param paletteName The name of the palette (NTSC or SECAM).
     * @return
     */
    protected static ColorPalette makePalette(String paletteName) {
        switch (paletteName) {
            case "NTSC":
                return new NTSCPalette();
            case "SECAM":
                return new SECAMPalette();
            default:
                throw new IllegalArgumentException("Invalid palette: " + paletteName);
        }
    }

    /** Initialize relevant bits of the agent
     * 
     */
    public final void init() {
        if (useGUI) {
            
            ui = new AgentGUI();
        }
        else {
            ui = new NullUI();
        }

        
        initIO();
    }

    /** Initialize the I/O object for this agent.
     * 
     */
    protected void initIO() {
        io = null;

        try {
            
            if (namedPipesBasename != null)
                io = new ALEPipes(namedPipesBasename + "out", namedPipesBasename + "in");
            else
                io = new ALEPipes();

            
            io.setUpdateScreen(useGUI || wantsScreenData());
            io.setUpdateRam(wantsRamData());
            io.setUpdateRL(wantsRLData());
            io.initPipes();
        }
        catch (IOException e) {
            System.err.println ("Could not initialize pipes: "+e.getMessage());
            System.exit(-1);
        }
    }

    /** The main program loop. In turn, we will obtain a new screen from ALE,
     *    pass it on to the agent and send back an action (which may be a reset
     *    request).
     */
    public void run() {
        boolean done = false;

        
        while (!done) {
            
            done = io.observe();
            
            if (done) break;
            
            
            ScreenMatrix screen = io.getScreen();
            
            updateImage(screen);
            
            observe(screen, io.getRAM(), io.getRLData());

            
            int action = selectAction();
            
            done = io.act(action);

            
            long pauseLength = getPauseLength();
            
            if (pauseLength > 0) {
                pause(pauseLength);
            }

            
            done |= shouldTerminate();
        }

        
        ui.die();
    }

    /** Internal method to update the image displayed in the GUI.
     * 
     * @param currentScreen
     */
    protected void updateImage(ScreenMatrix currentScreen) {
        
        
        if (ui instanceof NullUI) {
            ui.updateFrameCount();
            return;
        }
        
        
        BufferedImage img = converter.convert(currentScreen);

        
        ui.updateFrameCount();
        ui.setImage(img);
        ui.refresh();
    }

    protected static void pause(long waitTime) {
        try {
            Thread.sleep(waitTime);
        }
        catch (Exception e) {
        }
    }

    /** Returns how long to pause for, in milliseconds, before the next time step.
     * 
     * @return
     */
    public abstract long getPauseLength();
    /** Returns the agent's next action.
     *
     * @return
     */
    public abstract int selectAction();
    /** Provides the agent with the latest screen, RAM and RL data.
     * 
     * @param screen
     * @param ram
     * @param rlData
     */
    public abstract void observe(ScreenMatrix screen, ConsoleRAM ram, RLData rlData);
    /** Returns true to indicate that we should exit the program.
     * 
     * @return
     */
    public abstract boolean shouldTerminate();
    /** Returns true if we want to receive the screen matrix from ALE.
     *
     * @return
     */
    public abstract boolean wantsScreenData();
    /** Returns true if we want to receive the RAM from ALE.
     *
     * @return
     */
    public abstract boolean wantsRamData();
    /** Returns true if we want to receive RL data from ALE.
     * 
     * @return
     */
    public abstract boolean wantsRLData();
}
