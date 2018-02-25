package spacegraph.widget.meta;

import jcog.list.FasterList;
import spacegraph.Surface;
import spacegraph.container.EmptySurface;
import spacegraph.container.Gridding;
import spacegraph.container.Splitting;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.windo.PhyWall;

public class WizardFrame extends Splitting {

    private final PushButton backButton;


    public WizardFrame(Surface next) {
        super();
        split(0.9f);

        set(new Gridding(
            //Undo?
            this.backButton = new PushButton("<-", this::pop),

            new EmptySurface(), new EmptySurface(),

            //Hide/Delete
            new PushButton("X", this::close)

        ), next);

        backButton.hide();
    }

    private final FasterList<Surface> stack = new FasterList();

    @Override
    public void replace(Surface existingChild, Surface nextChild) {

        assert(existingChild!=nextChild);

        synchronized (this) {
            if (get(1) == existingChild) {
                if (stack.isEmpty())
                    backButton.show();
                stack.add(existingChild);
                set(1, nextChild);
            } else {
                throw new UnsupportedOperationException();
            }

        }
    }

    public void pop() {
        synchronized (this) {
            Surface prev = stack.removeLast();
            if (stack.isEmpty())
                backButton.hide();
            assert(prev!=null);
            set(1, prev);
        }
    }
    public void close() {
        ((PhyWall.PhyWindow)((Surface)parent).parent).remove(); //HACK
//        if (!parent(PhyWall.class).remove()) { //HACK
//            throw new RuntimeException("not completely removed");
//        }
//            //((MutableContainer)(this.parent)).remove(this);
    }
}
