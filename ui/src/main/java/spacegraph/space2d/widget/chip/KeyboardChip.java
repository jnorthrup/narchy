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

        int kc = map(keyCode);
        if (kc!=0)
            txt.text(label(kc));
        else
            txt.text("");


        out.out(kc /*new ArrayTensor(new float[] { kc })*/);
    }

    /** return 0 to filter */
    protected int map(int keyCode) {
        return keyCode;
    }

    protected String label(int keyCode) {
        return String.valueOf(keyCode);
    }

    public static class ArrowKeysChip extends KeyboardChip {
        @Override
        protected String label(int keyCode) {
            switch (keyCode) {
                case 1:
                    return "left";
                case 2:
                    return "right";
            }
            return "";
        }

        @Override
        protected int map(int keyCode) {
            switch(keyCode) {
                case KeyEvent.VK_LEFT:
                    return 1;
                case KeyEvent.VK_RIGHT:
                    return 2;
            }
            return 0;
        }
    }
}
