package spacegraph.widget.console;

import com.googlecode.lanterna.TextCharacter;
import spacegraph.widget.windo.Widget;

import java.awt.*;

public abstract class AbstractConsoleSurface extends Widget implements Appendable {
    protected int rows, cols;

    public void resize(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        //align(Align.Center, cols/1.5f, rows);
    }

    abstract public TextCharacter charAt(int col, int row);
}
