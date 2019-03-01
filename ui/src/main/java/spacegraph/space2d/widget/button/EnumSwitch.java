package spacegraph.space2d.widget.button;

import jcog.math.MutableEnum;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Switching;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.EnumSet;
import java.util.function.Supplier;

public class EnumSwitch {

    public static <C extends Enum<C>> Surface newSwitch(MutableEnum x, String label) {


        EnumSet<C> s = EnumSet.allOf(x.klass);

        int initialButton = -1;

        ToggleButton[] b = new ToggleButton[s.size()];

        Enum initialValue = x.get();

        int i = 0;
        for (C xx : s) {
            CheckBox tb = new CheckBox(xx.name());

            if (xx == initialValue)
                initialButton = i;
            b[i] = tb;
            i++;
        }
        if (initialButton != -1) {
            b[initialButton].on(true);
        }

        ButtonSet editPane = new ButtonSet(ButtonSet.Mode.One, b);

        Supplier<Surface> mainLabel = () -> {
            Enum v = x.get();
            return new VectorLabel(v != null ? v.name() : "");
        };

        Switching states = new Switching(
            ()->{
                return new Gridding(mainLabel.get(), editPane);
            },
            () -> {
                return editPane;
            }
        );



        int ii = 0;
        for (C xx : s) {
            b[ii++].on((c, enabled) -> {
                if (enabled) {
                    x.set(xx);
                    states.set(0);
                }
            });
        }

        return new LabeledPane(label, new ToggleButton(states).on((nextState)->{
            states.set(1);
        }));

    }
}
