package spacegraph.space2d.widget.textedit;

import com.jogamp.opengl.GL2;
import jcog.math.v2;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.ScrollXY;
import spacegraph.space2d.container.unit.UnitContainer;

import java.util.function.BiConsumer;


public class TextEdit extends ScrollXY<TextEditModel>  {

    public final MyTextEditModel model;





    @Deprecated public TextEdit() {
        this(16, 3);
    }

    public TextEdit(int cols, int rows /*boolean editable*/) {
        super(new MyTextEditModel());

        v2 initialSize = new v2(cols, rows);
        viewMin(new v2(1,1));
        viewMax(initialSize);
        view(initialSize);

        this.model = (MyTextEditModel) content;
        updateScroll();

        model.painter = (gl,r)->{
            model.paint(bounds, view(), model.focused, gl);
        };

        model.actions = TextEditActions.DEFAULT_ACTIONS;
        model.keys = TextEditActions.DEFAULT_KEYS;

    }

    public TextEdit(String initialText) {
        this();

        text(initialText);
    }

    public static Appendable out() {
        TextEdit te = new TextEdit();
        return new AppendableUnitContainer(te
                .viewMin(new v2(8,8))
                .viewMax(new v2(32,32))
                .view(8, 8)) {

            @Override
            public AppendableUnitContainer append(CharSequence charSequence) {
                te.model.buffer().insert(charSequence.toString());
                return this;
            }

            @Override
            public AppendableUnitContainer append(CharSequence charSequence, int i, int i1) {
                te.model.buffer().insert(charSequence.subSequence(i, i1).toString());
                return this;
            }

            @Override
            public AppendableUnitContainer append(char c) {
                te.model.buffer().insert(String.valueOf(c));
                return this;
            }
        };
    }






    public void append(String text) {
        model.buffer().insert(text);
    }

    public void text(String text) {
        model.buffer().set(text);
    }

    public String text() {
        return model.buffer().text();
    }

    private void updateScroll() {
        viewMax(new v2(model.buffer.width(), model.buffer.height()));
    }

    public TextEdit focus() {
        model.focus();
        return this;
    }

    public abstract static class AppendableUnitContainer<S extends Surface> extends UnitContainer<S> implements Appendable {

        public AppendableUnitContainer(S x) {
            super(x);
        }

    }

    private static class MyTextEditModel extends TextEditModel {
        public BiConsumer<GL2,SurfaceRender> painter = null;
        @Override
        protected void paintIt(GL2 gl, SurfaceRender rr) {
            super.paintIt(gl, rr);
            painter.accept(gl, rr);
        }
    }
}
