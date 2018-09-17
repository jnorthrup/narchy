package spacegraph.space2d.widget.meta;

import jcog.data.list.FasterList;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.windo.Windo;

public class WizardFrame extends Splitting {

    private final PushButton backButton;

    private final FasterList<Surface> stack = new FasterList();

    public WizardFrame(Surface next) {
        super();
        split(0.9f);

        T(new Gridding(
            
            this.backButton = new PushButton("<-", this::pop),

            new EmptySurface(), new EmptySurface(),

            
            new PushButton("X", this::close)

        ));
        B(next);

        backButton.hide();
    }

    @Override
    public Surface put(int index, Surface x) {
        Surface y = super.put(index, x);
        if (y!=x && y!=null && x!=null)
            replaced(x, y);
        return y;
    }

    protected void replaced(Surface existingChild, Surface nextChild) {

        synchronized (this) {
            if (get(1) == existingChild) {
                if (stack.isEmpty())
                    backButton.show();
                stack.add(existingChild);
                become(nextChild);
            } else {
                throw new UnsupportedOperationException();
            }

        }
    }

    protected void become(Surface next) {
        B(next);
    }

    private void pop() {
        synchronized (this) {
            Surface prev = stack.removeLast();
            if (stack.isEmpty())
                backButton.hide();
            assert(prev!=null);
            become(prev);
        }
    }

    private void close() {
        synchronized (this) {
            parent(Windo.class).detach();
        }
    }
}
