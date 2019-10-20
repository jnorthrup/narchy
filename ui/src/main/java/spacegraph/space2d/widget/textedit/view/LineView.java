package spacegraph.space2d.widget.textedit.view;

import com.jogamp.opengl.GL2;
import jcog.TODO;
import jcog.data.list.FastCoWList;
import jcog.data.list.FasterList;
import spacegraph.space2d.widget.textedit.buffer.BufferChar;
import spacegraph.space2d.widget.textedit.buffer.BufferLine;
import spacegraph.space2d.widget.textedit.buffer.BufferLineListener;
import spacegraph.space2d.widget.textedit.hilite.TextStyle;

import java.util.List;
import java.util.function.Consumer;

public class LineView extends TextEditRenderable implements BufferLineListener, Comparable<LineView> {

    private final BufferLine bufferLine;
    private final FastCoWList<CharView> chars;
    private float width;

    LineView(BufferLine bufferLine) {
        this.bufferLine = bufferLine;
        bufferLine.addListener(this);
        var bufferChars = bufferLine.getChars();
        chars = new FastCoWList<>(bufferChars.size(), CharView[]::new);
        if (!bufferChars.isEmpty()) {
            update((c) -> {
                for (var bc : bufferChars)
                    c.add(new CharView(bc));
                //return true;
            });
        }
    }

    public static double getHeight() {
        return 1;
    }

    public float getWidth() {
        return width;
    }

    @Override
    public void innerDraw(GL2 gl) {
        for (var c : chars) {
            if (c != null)
                c.draw(gl);
        }
    }

    protected void update() {
        update((c) -> {  /* */ });
    }

    private void update(Consumer<FasterList<CharView>> with) {
        chars.synchDirect((cc) -> {

            with.accept(cc);

//            if (cc.size() > 1)
//                cc.sortThis();

            float width = 0;
            for (var c : cc) {
                var w = CharView.width() / 2;
                width += w;
                c.position.set(width, 0, 0);
                width += w;
            }
            this.width = width;

            return true; //TODO commit only if sort changed the order
        });
    }

    @Override
    public void update(BufferLine bl) {
        update();
    }

    @Override
    public void addChar(BufferChar bufferChar, int col) {
        addChar(new CharView(bufferChar), col);
    }

    @Override
    public void removeChar(BufferChar removed) {
        update((chars) -> chars.removeIf(x -> x.bufferChar() == removed));
    }

    BufferLine getBufferLine() {
        return bufferLine;
    }

    public FastCoWList<CharView> getChars() {
        return chars;
    }

    @Override
    public int compareTo(LineView o) {
        return bufferLine.compareTo(o.bufferLine);
    }

    CharView leaveChar(BufferChar bc) {

        var leaved = new CharView[1];
        update((chars) -> {
            var leave = chars.stream().filter(c -> c.bufferChar() == bc).findFirst().orElse(null);
            leaved[0] = leave;
            chars.remove(leave);
        });
        return leaved[0];
    }

    void addChar(CharView cv, int col) {
        chars.add(col, cv);
//        update((chars)->{
//            chars.add(col, cv);
//        });
    }

    public static LineView apply(int from, int to, TextStyle highlight) {
        throw new TODO();
    }

    public static String substring(int from, int to) {
        throw new TODO();
    }

    public int length() {
        return chars.size();
        //return width;
    }

    public void draw(GL2 gl, int x1, int x2, float dx, float dy) {

        gl.glPushMatrix();

        gl.glTranslatef(position.x - x1 + dx, dy, position.z);

        gl.glColor4f(color.x, color.y, color.z, color.w);

        //TODO line height, margin etc
        for (var x = x1; x < x2; x++) {
            var c = chars.get(x);
            if (c != null)
                c.draw(gl);
        }

        gl.glPopMatrix();
    }
}
