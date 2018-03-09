package spacegraph.widget.button;

import spacegraph.render.ImageTexture;

public class IconToggle extends ColorToggle {

    public IconToggle(ImageTexture icon) {
        super(0.5f,0.5f,0.5f);
        content(icon.view(1f));
    }


}
