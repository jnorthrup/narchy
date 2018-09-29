package spacegraph.space2d.widget.chip;

import com.jogamp.newt.event.KeyEvent;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.text.VectorLabel;

public class KeyboardChip extends Bordering {

    private final VectorLabel txt;
    private final Port out;

    public KeyboardChip() {
        super();

        set(txt = new VectorLabel());
        set(S, new LabeledPane("out", out = new Port()), 0.1f);


    }

    @Override
    public boolean key(KeyEvent e, boolean pressed) {
        //FIFO, 0=unpressed
        if (pressed)
            out((e.isPrintableKey() ? e.getKeyChar() : e.getKeyCode()));
        else
            out(0);

        return true;

    }

    protected void out(int keyCode) {
        if (keyCode!=0)
            txt.text(String.valueOf(keyCode));
        else
            txt.text("");

        out.out(keyCode);
    }
}
