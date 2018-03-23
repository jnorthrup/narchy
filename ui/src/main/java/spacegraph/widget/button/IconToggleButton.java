package spacegraph.widget.button;

import spacegraph.render.ImageTexture;

public class IconToggleButton extends ColorToggle {

    public IconToggleButton(ImageTexture icon) {
        super(0.5f,0.5f,0.5f);
        content(icon.view(1f));
    }

    public static IconToggleButton awesome(String icon) {
        return new IconToggleButton(new ImageTexture("fontawesome://" + icon ));
    }


}
