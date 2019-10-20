package spacegraph.space2d.widget.button;

import jcog.math.MutableEnum;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.text.LabeledPane;

import java.util.EnumSet;

public enum EnumSwitch { ;

    protected static ButtonSet newSwitch(ToggleButton[] b, int i2) {
        var editPane = new ButtonSet(ButtonSet.Mode.One, b);

        if (i2 != -1) {
            b[i2].on(true);
        }

        return editPane;
    }

    public static <C extends Enum<C>> Surface the(MutableEnum x, String label) {


        EnumSet<C> s = EnumSet.allOf(x.klass);

        var initialButton = -1;

        var b = new ToggleButton[s.size()];

        var initialValue = x.get();

        var i = 0;
        for (var xx : s) {
            var tb = new CheckBox(xx.name());
            tb.on((c, enabled) -> {
                if (enabled)
                    x.set(xx);
            });
            if (xx == initialValue)
                initialButton = i;
            b[i] = tb;
            i++;
        }


        return LabeledPane.the(label, newSwitch(b, initialButton));


    }
}
