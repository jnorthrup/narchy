package spacegraph.widget.sketch;

import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.layout.VSplit;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.slider.FloatSlider;
import spacegraph.widget.tab.ButtonSet;
import spacegraph.widget.windo.Widget;

import static spacegraph.layout.Grid.grid;

/**
 * gesture-aware general-purpose 2d graphical input widgets
 */
abstract public class Sketch2D extends Widget {



    //final RectFloat2D view = new RectFloat2D(0,0,1,1);

    public static void main(String[] args) {

        Sketch2DBitmap canvas = new Sketch2DBitmap(256, 256);

        ButtonSet<CheckBox.ColorToggle> colorMenu = new ButtonSet<CheckBox.ColorToggle>(ButtonSet.Mode.One,
                new CheckBox.ColorToggle(0f, 0, 0), //black
                new CheckBox.ColorToggle(1f, 0, 0), //red
                new CheckBox.ColorToggle(1f, 0.5f, 0),//orange
                new CheckBox.ColorToggle(0.75f, 0.75f, 0),//yellow
                new CheckBox.ColorToggle(0f, 1, 0), //green
                new CheckBox.ColorToggle(0f, 0, 1), //blue
                new CheckBox.ColorToggle(1f, 0, 1), //purple
                new CheckBox.ColorToggle(0.5f, 0.5f, 0.5f), //gray
                new CheckBox.ColorToggle(1f, 1, 1) //white
        );
        colorMenu.on((cc,e)->{
            if (e) {
                canvas.color(cc.r, cc.g, cc.b);
            }
        });

        Surface toolMenu = grid(
            new FloatSlider("Width", 1, 0.1f, 3f),
            new FloatSlider("Alpha", 0.75f, 0f, 1f)
        );

        VSplit<ButtonSet, Sketch2DBitmap> sketch = new VSplit(
                grid(colorMenu, toolMenu), canvas,
                0.8f);

        SpaceGraph.window(sketch, 800, 800);
    }


}
