package spacegraph.space2d.widget.button;

import spacegraph.space2d.Surface;

import java.util.Map;

public enum MapSwitch { ;
    /** TODO decide initialization semantics */
    public static <X> Surface the(Map<X,Runnable> x) {

        final int[] initialButton = {-1};

        ToggleButton[] b = new ToggleButton[x.size()];

        final int[] i = {0};
        x.forEach((xx,r)->{
            CheckBox tb = new CheckBox(xx.toString());
            tb.on((c, enabled) -> {
                if (enabled)
                    r.run();
            });
//                if (xx == initialValue)
//                    initialButton = i;
            b[i[0]++] = tb;
        });


        return EnumSwitch.newSwitch(b, initialButton[0]);

    }

}
