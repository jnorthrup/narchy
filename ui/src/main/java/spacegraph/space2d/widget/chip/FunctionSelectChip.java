package spacegraph.space2d.widget.chip;

import jcog.data.list.FasterList;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.ToggleButton;
import spacegraph.space2d.widget.tab.ButtonSet;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class FunctionSelectChip<X,Y> extends AbstractFunctionChip<X,Y> {

    final Map<String, Function<X, Y>> ff = new TreeMap();

    private volatile Function<X, Y> f = (x) -> null;

    public FunctionSelectChip(Map<String,Function<X,Y>> m) {
        super();

        ff.putAll(m);


        List<ToggleButton> fb = new FasterList(m.size());
        ff.forEach((n, f)->{
            fb.add(new CheckBox(n).on(()->{
                FunctionSelectChip.this.f = f;
            }));
        });

        ButtonSet selector = new ButtonSet(ButtonSet.Mode.One, fb);

        set(in, selector, out);
    }
    @Override
    protected Function<X, Y> f() {
        return f;
    }
}
