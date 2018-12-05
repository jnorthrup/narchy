package spacegraph.space2d.widget.button;

import jcog.math.MutableEnum;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.text.LabeledPane;

import java.util.EnumSet;

public class EnumSwitch {

    public static <C extends Enum<C>> Surface newSwitch(MutableEnum x, String label) {
        EnumSet<C> s = EnumSet.allOf(x.klass);

        Enum initialValue = x.get();
        int initialButton = -1;

        ToggleButton[] b = new ToggleButton[s.size()];
        int i = 0;
        for (C xx : s) {
            CheckBox tb = new CheckBox(xx.name());
            tb.on((c, enabled) -> {
                if (enabled)
                    x.set(xx);
            });
            if (xx == initialValue)
                initialButton = i;
            b[i] = tb;
            i++;
        }


//JDK12 compiler error has trouble with this:
//        ToggleButton[] b = ((EnumSet) EnumSet.allOf(x.klass)).stream().map(e -> {
//            CheckBox tb = new CheckBox(e.name());
//            tb.on((c, enabled) -> {
//                if (enabled)
//                    x.set(e);
//            });
//            return tb;
//        }).toArray(ToggleButton[]::new);
//
        ButtonSet bs = new ButtonSet(ButtonSet.Mode.One, b);

        if (initialButton != -1) {
            b[initialButton].on(true);
        }

        return new LabeledPane(label, bs);
    }
}
