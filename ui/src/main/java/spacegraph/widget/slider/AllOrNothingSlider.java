package spacegraph.widget.slider;

import spacegraph.Surface;
import spacegraph.layout.Grid;
import spacegraph.widget.button.PushButton;

public class AllOrNothingSlider {
    public static Grid AllOrNothingSlider(FloatSlider f) {
        PushButton zeroButton = new PushButton("-").click((cb)->f.valueRelative(0f));
        PushButton oneButton = new PushButton("+").click((cb)->f.valueRelative(1f));
        return new Grid(Grid.HORIZONTAL, new Surface[] { zeroButton, f, oneButton });
    }
}
