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
package jurls.reinforcementlearning.domains.arcade.screen;

import java.io.*;

/** Encapsulates screen matrix data. Also provides basic save/load operations on
 *   screen data.
 *
 * @author Marc G. Bellemare <mgbellemare@ualberta.ca>
 */
public class ScreenMatrix implements Cloneable {
    public int[][] matrix;
    public int width;
    public int height;

    /** Create a new, blank screen matrix with the given dimensions.
     * 
     * @param w width
     * @param h height
     */
    public ScreenMatrix(int w, int h) {
        matrix = new int[w][h];
        width = w;
        height = h;
    }

    /** Load a screen from a text file, in ALE format. The first line contains
     *   <width>,<height> .
     *  Each subsequent line (210 of them) contains a screen row with comma-separated
     *   values.
     * 
     * @param filename
     */
    public ScreenMatrix(String filename) throws IOException {

        var in = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));


        var line = in.readLine();
        var tokens = line.split(",");

        width = Integer.parseInt(tokens[0]);
        height = Integer.parseInt(tokens[1]);

        this.matrix = new int[width][height];

        var rowIndex = 0;

        
        while ((line = in.readLine()) != null) {
            
            tokens = line.split(",");
            assert (tokens.length == width);

            for (var x = 0; x < tokens.length; x++) {
                this.matrix[x][rowIndex] = Integer.parseInt(tokens[x]);
            }

            rowIndex++;
        }
    }

    /** Saves this screen matrix as a text file. Can then be loaded using the
     *   relevant constructor.
     * 
     * @param filename
     * @throws IOException
     */
    public void saveData(String filename) throws IOException {
        var out = new PrintStream(new FileOutputStream(filename));

        
        out.println(width+","+height);

        
        for (var y = 0; y < height; y++) {
            
            for (var x = 0; x < width; x++) {
                out.print(matrix[x][y]);
                if (x < width - 1) out.print(",");
            }

            out.println();
        }
    }

    /** Clones this screen matrix. Data is copied.
     * 
     * @return
     */
    @Override
    public Object clone() {
        try {
            var img = (ScreenMatrix)super.clone();

            
            img.matrix = new int[this.width][this.height];
        
            for (var x = 0; x < this.width; x++) {
                System.arraycopy(this.matrix[x], 0, img.matrix[x], 0, this.height);
            }
            return img;
        }
        catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
