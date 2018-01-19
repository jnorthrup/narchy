package spacegraph.widget.tab;

import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.layout.Grid;
import spacegraph.layout.MutableContainer;
import spacegraph.layout.VSplit;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.button.ToggleButton;
import spacegraph.widget.sketch.Sketch2DBitmap;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Created by me on 12/2/16.
 */
public class TabPane extends VSplit {


    private final ButtonSet tabs;
    private final MutableContainer content;

    private static final float CONTENT_VISIBLE_SPLIT = 0.9f;


    public TabPane(Map<String, Supplier<Surface>> builder) {
        super();

        content = new Grid();

        tabs = new ButtonSet<>(ButtonSet.Mode.Multi, builder.entrySet().stream().map(x -> {
            final Surface[] created = {null};
            Supplier<Surface> creator = x.getValue();
            String label = x.getKey();
            return new CheckBox(label).on((cb, a) -> {
                synchronized (content) {
                    if (a) {
                        content.add(created[0] =  creator.get() );
                        set(CONTENT_VISIBLE_SPLIT); //hide empty content area
                    } else {
                         if (created[0] !=null) {
                             content.remove(created[0]);
                             created[0] = null;
                         }
                         if (content.isEmpty()) {
                            set(0f); //hide empty content area
                         }
                    }
                }
            });
        }).toArray(ToggleButton[]::new));

        set(tabs, content);
    }


    public static void main(String[] args) {
        SpaceGraph.window(new TabPane(Map.of(
                "a", ()->new Sketch2DBitmap(40,40),
                "b", ()->new PushButton("x"))), 800, 800);
    }

}
