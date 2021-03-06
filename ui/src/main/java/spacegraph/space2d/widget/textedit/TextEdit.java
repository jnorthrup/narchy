package spacegraph.space2d.widget.textedit;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.event.ListTopic;
import jcog.event.Topic;
import jcog.math.v2;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.ScrollXY;
import spacegraph.space2d.container.unit.Clipped;
import spacegraph.space2d.container.unit.UnitContainer;
import spacegraph.space2d.widget.textedit.buffer.Buffer;

import java.util.function.Consumer;


public class TextEdit extends ScrollXY<TextEditModel>  {

    public final Topic<TextEdit> onChange = new ListTopic<>();
    private final MyTextEditView model;


    public TextEdit(int cols) {
        this(cols, 1, -1, -1);
    }

    public TextEdit(int cols, int rows /*boolean editable*/) {
        this(cols, rows, -1, -1);
    }

    public TextEdit(int cols, int rows, int colsMax, int rowsMax /*boolean editable*/) {
        this();
        viewMax(new v2((float) Math.max(cols, colsMax), (float) Math.max(rows, rowsMax)));
        view((float) cols, (float) rows);
    }

    public TextEdit() {
        super();

        MyTextEditView model;
        set(this.model = model = new MyTextEditView());
        model.actions = TextEditActions.DEFAULT_ACTIONS;
        model.keys = TextEditActions.DEFAULT_KEYS;


        v2 initialSize = new v2(1.0F, 1.0F);
        viewMinMax(new v2(1.0F, 1.0F), initialSize);
        view(initialSize);



        updateScroll();
    }

    public TextEdit(String initialText) {
        this();

        text(initialText);
        viewAll();
    }

    public static Appendable out() {
        TextEdit e = new TextEdit();
        return new AppendableUnitContainer<>(e) {

            @Override
            public AppendableUnitContainer append(CharSequence charSequence) {
                e.insert(charSequence.toString());
                return this;
            }

            @Override
            public AppendableUnitContainer append(CharSequence charSequence, int i, int i1) {
                e.insert(charSequence.subSequence(i, i1).toString());
                return this;
            }

            @Override
            public AppendableUnitContainer append(char c) {
                e.insert(String.valueOf(c));
                return this;
            }
        };
    }

    public TextEdit insert(String text) {
        buffer().insert(text);
        //viewAll();
        return this;
    }

    protected Buffer buffer() {
        return surface().buffer();
    }

    protected MyTextEditView surface() {
        return ((MyTextEditView) ((Clipped)get(C)).the());
    }

    public TextEdit clear() {
        buffer().clear();
        return this;
    }

    public TextEdit text(String text) {
        buffer().text(text);
        return this;
    }

    /** TODO add reasonable limits (too many lines to display etc) */
    private void viewAll() {
        Buffer b = buffer();
        int w = b.width(), h = b.height();
        viewMax(new v2(Math.max(viewMax.x, (float) w), Math.max(viewMax.y, (float) h)));
        view((float) w, (float) h);
    }

    public String text() {
        return buffer().text();
    }

    private void updateScroll() {
        Buffer buffer = buffer();
        viewMax(new v2(Math.max(viewMax.x, (float) buffer.width()), Math.max(viewMax.y, (float) buffer.height())));
        onChange.emit(TextEdit.this);
    }

    public TextEdit focus() {
        surface().focus();
        return this;
    }

    public TextEdit onChange(Consumer<TextEdit> e) {
        onChange.on(e);
        return this;
    }

    public TextEdit onKeyPress(Consumer<KeyEvent> e) {
        model.keyPress.on(e);
        return this;
    }

    private abstract static class AppendableUnitContainer<S extends Surface> extends UnitContainer<S> implements Appendable {

        public AppendableUnitContainer(S x) {
            super(x);
        }

    }

    private class MyTextEditView extends TextEditModel {

        @Override
        protected void paintIt(GL2 gl, ReSurface r) {
            //super.paintIt(gl, rr);
            paint(content.bounds, view(), focused, gl);
        }

        @Override
        protected void updated() {
            super.updated();
            updateScroll();
        }
    }
}
