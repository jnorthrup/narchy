package spacegraph.space2d.widget.port;

import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.CheckBox;

import static javax.swing.UIManager.put;

/** TODO finish */
public class EnabledPort extends Gridding {

    final CheckBox enable = new CheckBox("");

    public EnabledPort(Port p) {
        enable.on(true);
        put(0, enable.on(this::enable));
    }

    protected void enable(boolean b) {

    }
}
