package spacegraph.widget.slider;

import spacegraph.container.Gridding;
import spacegraph.widget.button.PushButton;

public class AllOrNothingSlider {
    public static Gridding AllOrNothingSlider(FloatSlider f) {
        PushButton zeroButton = new PushButton("-").click((cb)->f.valueRelative(0f));
        PushButton oneButton = new PushButton("+").click((cb)->f.valueRelative(1f));
        return new Gridding(Gridding.HORIZONTAL, f, new Gridding(
                Gridding.VERTICAL, zeroButton, oneButton));
    }
}
