package spacegraph.space2d.widget.meta;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.PushButton;

/** quick "OK button" dialog wrapper */
public class OKSurface extends Splitting {

    public OKSurface(Object content) {
        this(new ObjectSurface<>(content));
    }

    public OKSurface(Surface content) {
        super();
        vertical().set(
            content,
                0.1f, Splitting.row(new EmptySurface(), 0.8f,
                new PushButton("OK").clicked((Runnable)(OKSurface.this::remove))
            )
        );
    }
}
