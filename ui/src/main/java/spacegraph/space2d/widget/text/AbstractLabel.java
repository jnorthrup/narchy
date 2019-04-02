package spacegraph.space2d.widget.text;

import spacegraph.space2d.container.EmptyContainer;
import spacegraph.util.math.Color4f;

public abstract class AbstractLabel extends EmptyContainer {

    protected volatile String text;

    protected final Color4f fgColor = new Color4f(1f, 1f, 1f, 1f);
    protected final Color4f bgColor = fgColor;

    protected static final float charAspect = 1.8f;

    public AbstractLabel() {

    }


    public AbstractLabel text(String newValue) {
        if (!newValue.equals(this.text)) {
            this.text = newValue;
            layout();
        }
        return this;
    }

    public final String text() { return text; }

    @Override
    public String toString() {
        return "Label[" + text + ']';
    }
}
