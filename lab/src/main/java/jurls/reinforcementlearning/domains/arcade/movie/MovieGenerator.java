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
package jurls.reinforcementlearning.domains.arcade.movie;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;

/** A class for exporting screen images to PNG files.
 *
 * @author Marc G. Bellemare <mgbellemare@ualberta.ca>
 */
public class MovieGenerator {
    /** How many times to show the same image sequence before moving on to the next */
    protected String baseFilename;

    /** The current frame index (used to obtain the PNG filename) */
    protected int pngIndex = 0;

    /** How many digits to use in generating the filename */
    protected static final int indexDigits = 6;

    /** Create a new MovieGenerator that saves images to /tmp/frames/atari_xxxxxx.png
     * 
     */
    public MovieGenerator() {
        this("/tmp/frames/atari_");
    }

    /** Create a new MovieGenerator with the specified base filename. To this
     *   base filename is appended a frame number and ".png" in order to obtain
     *   the full filename.
     *
     * @param baseFilename
     */
    public MovieGenerator(String baseFilename) {
        this.baseFilename = baseFilename;


        File fp = new File(baseFilename);
        File directory = fp.getParentFile();

        
        if (!directory.isDirectory()) {
            if (!directory.exists())
                directory.mkdir();
            else
                throw new IllegalArgumentException("File "+directory.getAbsolutePath()+" exists, "+
                        "is not a directory.");
        }
    }

    /** This method saves the given image to disk as the next frame. It then
     *   increments pngIndex.
     * 
     * @param image
     */
    public void record(BufferedImage image) {
        
        if (baseFilename == null)
            throw new IllegalArgumentException("Base filename is not defined.");


        NumberFormat formatter = NumberFormat.getInstance();
        formatter.setMinimumIntegerDigits(indexDigits);
        formatter.setGroupingUsed(false);


        String indexString = formatter.format((long) pngIndex);


        String filename = baseFilename + indexString + ".png";

        
        try {
            ImageIO.write(image, "png", new File(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        
        pngIndex++;
    }
}
