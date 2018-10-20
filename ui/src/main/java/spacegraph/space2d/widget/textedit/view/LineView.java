package spacegraph.space2d.widget.textedit.view;

import com.jogamp.opengl.GL2;
import jcog.TODO;
import jcog.data.list.FasterList;
import spacegraph.space2d.widget.textedit.buffer.BufferChar;
import spacegraph.space2d.widget.textedit.buffer.BufferLine;
import spacegraph.space2d.widget.textedit.buffer.BufferLineListener;
import spacegraph.space2d.widget.textedit.hilite.TextStyle;

import java.util.Collections;
import java.util.List;

public class LineView extends TextEditRenderable implements BufferLineListener, Comparable<LineView> {

    private final BufferLine bufferLine;
    private final List<CharView> chars = new FasterList<>();

    public LineView(BufferLine bufferLine) {
        this.bufferLine = bufferLine;
        bufferLine.addListener(this);
        List<BufferChar> bufferChars = bufferLine.getChars();
        for (BufferChar bc : bufferChars) {
            chars.add(new CharView(bc));
        }
        updatePositions();
    }

    public static double getHeight() {
        return 1;
    }

    public float getWidth() {
        float width = 0;
        for (CharView c : chars) {
            width += c.width();
        }
        return width;
    }

    @Override
    public void innerDraw(GL2 gl) {
        for (CharView c : chars)
            c.draw(gl);
    }

    private void updatePositions() {
        Collections.sort(chars);
        float width = 0;
        for (CharView c : chars) {
            float w = c.width() / 2;
            width += w;
            c.position.set(width, 0, 0);
            width += w;
        }
    }

    @Override
    public void update(BufferLine bl) {
        updatePositions();
    }

    @Override
    public void addChar(BufferChar bufferChar) {
        CharView cv = new CharView(bufferChar);
        chars.add(cv);
        updatePositions();
    }

    @Override
    public void removeChar(BufferChar removed) {
        if (chars.removeIf(x -> x.bufferChar() == removed)) {
            updatePositions();
        }
    }

    public BufferLine getBufferLine() {
        return bufferLine;
    }

    public List<CharView> getChars() {
        return chars;
    }

    @Override
    public int compareTo(LineView o) {
        return bufferLine.compareTo(o.bufferLine);
    }

    public CharView leaveChar(BufferChar bc) {
        CharView leave = chars.stream().filter(c -> c.bufferChar() == bc).findFirst().orElse(null);
        chars.remove(leave);
        updatePositions();
        return leave;
    }

    public void visitChar(CharView cv) {
        chars.add(cv);
        updatePositions();
    }

    public LineView apply(int from, int to, TextStyle highlight) {
        throw new TODO();
    }

    public String substring(int from, int to) {
        throw new TODO();
    }

    public int length() {
        return chars.size();
    }
}
