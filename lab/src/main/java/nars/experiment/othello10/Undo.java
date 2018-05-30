package nars.experiment.othello10;/*
 * Created on 2004/12/22
 *
 */

import java.awt.*;


public class Undo {

    public int x;
    public int y;

    public int count;

    public Point[] pos;
    
    public Undo(int x, int y) {
        this.x = x;
        this.y = y;
        count = 0;
        pos = new Point[64];
    }
}
