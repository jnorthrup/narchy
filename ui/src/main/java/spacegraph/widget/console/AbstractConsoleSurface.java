package spacegraph.widget.console;

import com.googlecode.lanterna.TextCharacter;
import spacegraph.widget.windo.Widget;

public abstract class AbstractConsoleSurface extends Widget implements Appendable {
    protected int rows, cols;

    public void resize(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
    }

    abstract public TextCharacter charAt(int col, int row);
}
