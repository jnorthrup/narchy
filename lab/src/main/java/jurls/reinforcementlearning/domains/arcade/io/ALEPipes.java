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
package jurls.reinforcementlearning.domains.arcade.io;

import jurls.reinforcementlearning.domains.arcade.screen.ScreenMatrix;

import java.io.*;

/**
 * Class that communicates with ALE via pipes.
 *  The protocol used here is ALE 0.3.
 * 
 * @author Marc G. Bellemare
 */
public class ALEPipes {
    /** Data structure holding the screen image */
    protected ScreenMatrix screen;
    /** Data structure holding the RAM data */
    protected ConsoleRAM ram;
    /** Data structure holding RL data */
    protected RLData rlData;
    /** Whether termination was requested from the I/O channel */
    protected boolean terminateRequested;

    /** Input object */
    protected final BufferedReader in;
    /** Output object */
    protected final PrintStream out;

    /** Flags indicating the kind of data we want to receive from ALE */
    protected boolean updateScreen;
    protected boolean updateRam;
    protected boolean updateRLData;
    /** We will request that ALE sends data every 'frameskip' frames. */
    protected int frameskip;

    /** The action we send for player B (always noop in this case) */
    protected final int playerBAction = Actions.map("player_b_noop");

    /** A state variable used to track of whether we should receive or send data */
    protected boolean hasObserved;

    protected boolean useRLE = true;
    
    /** Uses stdin/stdout for communication */
    public ALEPipes() {
        this(new BufferedReader(new InputStreamReader(System.in)), System.out);
    }

    /** Uses named pipes */
    public ALEPipes(String pInfile, String pOutfile) throws IOException {
        this(new BufferedReader(new InputStreamReader(new FileInputStream(pInfile))),
                new PrintStream(new FileOutputStream(pOutfile)));
    }

    /** Initialize the default variables and set the I/O streams.
     * 
     * @param in
     * @param out
     */
    private ALEPipes(BufferedReader in, PrintStream out) {
        updateScreen = true;
        updateRam = false;
        updateRLData = true;
        frameskip = 0;

        this.in = in;
        this.out = out;
    }

    /** Closes the I/O channel.
     * 
     */
    public void close() {
        try {
            in.close();
            out.close();
        }
        catch (IOException e) {
            
        }
    }
    
    public void setUpdateScreen(boolean updateScreen) {
        this.updateScreen = updateScreen;
    }

    public void setUpdateRam(boolean updateRam) {
        this.updateRam = updateRam;
    }

    public void setUpdateRL(boolean updateRL) {
        this.updateRLData = updateRL;
    }
    
    /** A blocking method that sends initial information to ALE. See the
     *   documentation for protocol details.
     * 
     */
    public void initPipes() throws IOException {


        String line = in.readLine();
        String[] tokens = line.split("-");
        int width = Integer.parseInt(tokens[0]);
        int height = Integer.parseInt(tokens[1]);

        
        if (width <= 0 || height <= 0) {
            throw new RuntimeException("Invalid width/height: "+width+"x"+height);
        }

        
        screen = new ScreenMatrix(width, height);
        ram = new ConsoleRAM();
        rlData = new RLData();

        
        
        out.printf("%d,%d,%d,%d\n", updateScreen? 1:0, updateRam? 1:0, frameskip,
                updateRLData? 1:0);
        out.flush();
    }

    public int getFrameSkip() {
        return frameskip;
    }

    public void setFrameSkip(int frameskip) {
        this.frameskip = frameskip;
    }
    
    /** Returns the screen matrix from ALE.
     * 
     * @return
     */
    public ScreenMatrix getScreen() {
        return screen;
    }

    /** Returns the RAM from ALE.
     * 
     * @return
     */
    public ConsoleRAM getRAM() {
        return ram;
    }

    public RLData getRLData() {
        return rlData;
    }

    public boolean wantsTerminate() {
        return terminateRequested;
    }

    /** A blocking method which will get the next time step from ALE.
     *
     */
    public boolean observe() {
        
        
        if (hasObserved) {
            throw new RuntimeException("observe() called without subsequent act().");
        }
        else
            hasObserved = true;

        String line = null;

        
        try {
            line = in.readLine();
            if (line == null) return true;
        }
        catch (IOException e) {
            return true;
        }

        
        if ("DIE".equals(line)) {
            terminateRequested = true;
            return false;
        }

        
        if (line.length() > 0) {


            String[] tokens = line.split(":");

            int tokenIndex = 0;

            
            if (updateRam)
                readRam(tokens[tokenIndex++]);

            
            if (updateScreen) {
                String screenString = tokens[tokenIndex++];

                if (useRLE)
                    readScreenRLE(screenString);
                else
                    readScreenMatrix(screenString);
            }

            
            if (updateRLData) {
                readRLData(tokens[tokenIndex++]);
            }
        }

        return false;
    }

    /** After a call to observe(), send back the necessary action.
     * 
     * @param act
     * @return
     */
    public boolean act(int act) {
        
        if (!hasObserved) {
            throw new RuntimeException("act() called before observe().");
        }
        else
            hasObserved = false;

        sendAction(act);

        return false;
    }

    /** Helper function to send out an action to ALE */
    public void sendAction(int act) {
        
        
        out.printf("%d,%d\n", act, 18);
        out.flush();
    }

    /** Read in RL data from a given line.
     * 
     * @param line
     */
    public void readRLData(String line) {


        String[] tokens = line.split(",");

        
        rlData.isTerminal = (Integer.parseInt(tokens[0]) == 1);
        rlData.reward = Integer.parseInt(tokens[1]);
    }

    /** Reads the console RAM from a string 
      * @param line The RAM-part of the string sent by ALE.
      */
    public void readRam(String line) {
        int offset = 0;

        
        
        
        for (int ptr = 0; ptr < ConsoleRAM.RAM_SIZE; ptr++) {
            int v = Integer.parseInt(line.substring(offset, offset + 2), 16);
            ram.ram[ptr] = v;
            
            offset += 2;
        }
    }

    /** Reads the screen matrix update from a string. The string only contains the
     *   pixels that differ from the previous frame.
     *
     * @param line The screen part of the string sent by ALE.
     */
    public void readScreenMatrix(String line) {
        int ptr = 0;

        
        for (int y = 0; y < screen.height; y++)
            for (int x = 0; x < screen.width; x++) {
                int v = byteAt(line, ptr);
                screen.matrix[x][y] = v;
                ptr += 2;
            }
    }

    /** Parses a hex byte in the given String, at position 'ptr'. */
    private static int byteAt(String line, int ptr) {
        int ld = line.charAt(ptr+1);

        if (ld >= 'A') ld -= 'A' - 10;
        else ld -= '0';
        int hd = line.charAt(ptr);
        if (hd >= 'A') hd -= 'A' - 10;
        else hd -= '0';

        return (hd << 4) + ld;
    }

    /** Read in a run-length encoded screen. ALE 0.3-0.4 */
    public void readScreenRLE(String line) {
        int ptr = 0;


        int y = 0;
        int x = 0;

        while (ptr < line.length()) {

            int v = byteAt(line, ptr);
            int l = byteAt(line, ptr + 2);
            ptr += 4;

            for (int i = 0; i < l; i++) {
                screen.matrix[x][y] = v;
                if (++x >= screen.width) {
                    x = 0;
                    y++;

                    if (y >= screen.height && i < l - 1)
                        throw new RuntimeException ("Invalid run length data.");
                }
            }
        }
    }
}
