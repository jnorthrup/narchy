package spacegraph.space2d;

import spacegraph.space2d.widget.meta.ErrorPanel;

import java.util.function.Supplier;

public enum SafeSurface {;

    //ImageTexture.awesome("exclamation-triangle").view(1)

    public static Surface safe(Supplier<Surface> s) {
        Surface r;
        try {
            r = s.get();
        } catch (Throwable e) {
            r = new ErrorPanel(e, s);
            e.printStackTrace();
        }

        if (r == null)
            r = new ErrorPanel(new NullPointerException(), s);

        return r;
    }
}
