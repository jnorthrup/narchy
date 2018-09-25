package spacegraph.space2d.widget.port;

import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.CheckBox;

/** TODO finish */
public class EnabledPort extends Splitting {

    final CheckBox enable = new CheckBox("");

    public EnabledPort(Port p) {
        enable.set(true);
        put(0, enable.on((x)->{
            enable(x);
        }));
    }

    protected void enable(boolean b) {

    }
}
