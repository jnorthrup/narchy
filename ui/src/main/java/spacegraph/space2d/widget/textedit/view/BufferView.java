package spacegraph.space2d.widget.textedit.view;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import jcog.data.list.FasterList;
import spacegraph.space2d.widget.textedit.buffer.*;

import java.util.Collections;
import java.util.List;

public class BufferView implements BufferListener {

    private final Buffer document;
    private final CursorView caret;
    private final List<LineView> lines = new FasterList<>();


    public BufferView(Buffer buffer) {
        this.document = buffer;
        this.caret = new CursorView(buffer.getCaret());
        buffer.addListener(this);
        buildLines();
    }


    private void buildLines() {
        document.lines.forEach(this::addLine);
    }

    private double documentHeight() {
        double height = 0;
        //for (LineView line : lines) {
        height += lines.size() * LineView.getHeight();
        //}
        return height;
    }

    private double documentWidth() {
        double maxWidth = 0;
        for (LineView line : lines) {
            double width = line.getWidth();
            maxWidth = (maxWidth < width) ? width : maxWidth;
        }
        return maxWidth;
    }

    public void paint(GL2 gl) {
//        double w = documentWidth() + 2;
//        double h = documentHeight();


        gl.glDisable(GL.GL_TEXTURE_2D);

        updateCaret(document.getCaret());
        caret.draw(gl);

        for (LineView line : lines)
            line.draw(gl);


    }

    @Override
    public void updateCaret(CursorPosition c) {
        LineView lv = lines.get(c.getRow());
        List<CharView> cvl = lv.getChars();

        float x;
        if (document.isLineHead()) {
            x =
                    (float) lv.getChars().stream().mapToDouble(cv -> cv.width() / 2).findFirst()
                            .orElse(caret.getWidth() / 2);
        } else if (document.isLineLast()) {
            x = lv.getWidth() + (caret.getWidth() / 2);
        } else {
            x = cvl.get(c.getCol()).position.x;
        }
        float y = +lv.position.y;

        caret.position.set(x, y, 0);
    }

    @Override
    public void update(Buffer buffer) {
        updatePositions();
    }

    @Override
    public void addLine(BufferLine bufferLine) {
        LineView lv1 = new LineView(bufferLine);
        lines.add(lv1);
        Collections.sort(lines);
        float h = 0;
        for (LineView lv2 : lines) {
            lv2.position.y = h;
            h -= LineView.getHeight();
        }
    }

    @Override
    public void removeLine(BufferLine bufferLine) {
        lines.removeIf(lineView -> lineView.getBufferLine() == bufferLine);
        for (LineView lv : lines) {
            if (lv.getBufferLine() == bufferLine) {
                lines.remove(lv);
            }
        }
        updatePositions();
    }

    @Override
    public void moveChar(BufferLine fromLine, BufferLine toLine, BufferChar c) {
        lines.stream().filter(l -> l.getBufferLine() == fromLine).findFirst().ifPresent((from) -> lines.stream().filter(l -> l.getBufferLine() == toLine).findFirst().ifPresent((to) -> {
            float fromY = from.position.y;
            float toY = to.position.y;
            CharView leaveChar = from.leaveChar(c);
            leaveChar.position.y = (-(toY - fromY));
            to.visitChar(leaveChar);
        }));
    }

    private void updatePositions() {
        Collections.sort(lines);
        float h = 0f;
        for (LineView lv : lines) {
            lv.position.y = h;
            h -= LineView.getHeight();
        }
    }

}
