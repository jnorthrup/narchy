package spacegraph.space2d.widget.slider;

import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;

class AllOrNothingSlider {
    public static Gridding AllOrNothingSlider(FloatSlider f) {
        PushButton zeroButton = new PushButton("-").click((cb)->f.valueRelative(0f));
        PushButton oneButton = new PushButton("+").click((cb)->f.valueRelative(1f));
        return new Gridding(Gridding.HORIZONTAL, f, new Gridding(
                Gridding.VERTICAL, zeroButton, oneButton));
    }
}
