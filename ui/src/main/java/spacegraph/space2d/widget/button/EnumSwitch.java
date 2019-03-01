package spacegraph.space2d.widget.button;

import jcog.math.MutableEnum;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.text.LabeledPane;

import java.util.EnumSet;

public class EnumSwitch {

    public static <C extends Enum<C>> Surface newSwitch(MutableEnum x, String label) {


        EnumSet<C> s = EnumSet.allOf(x.klass);

        int initialButton = -1;

        ToggleButton[] b = new ToggleButton[s.size()];

        Enum initialValue = x.get();

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


        ButtonSet editPane = new ButtonSet(ButtonSet.Mode.One, b);

        if (initialButton != -1) {
            b[initialButton].on(true);
        }

        return LabeledPane.the(label, editPane);

//        return new LabeledPane(label, new ToggleButton(states).on((nextState)->{
//            states.set(1);
//        }));

    }
}
