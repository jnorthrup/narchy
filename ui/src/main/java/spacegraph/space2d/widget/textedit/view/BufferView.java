package spacegraph.space2d.widget.textedit.view;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import jcog.data.list.FasterList;
import spacegraph.space2d.widget.textedit.buffer.*;

import java.util.Collections;
import java.util.List;

public class BufferView extends TextEditView implements BufferListener {

    private final Buffer document;
    private final CaretView caret;
    private final List<LineView> lines = new FasterList<>();
    private final SmoothValue width = new SmoothValue();
    private final SmoothValue height = new SmoothValue();

    public BufferView(Buffer buffer) {
        this.document = buffer;
        this.caret = new CaretView(buffer.getCaret());
        color.update(0.5, 0.5, 0.5, 0.5);
        buffer.addListener(this);
        buildLines();
    }

    public void preDraw(GL2 gl) {
        gl.glPushMatrix();
        angle.updateRotate(gl);
        scale.updateScale(gl);
        position.updateTranslate(gl);
        color.updateColor(gl);
    }

    private void buildLines() {
        document.getLines().forEach(this::addLine);
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

    @Override
    public void innerDraw(GL2 gl) {
        double w = documentWidth() + 2;
        double h = documentHeight();

        Position position = this.position;
        Position cPos = caret.position;
        position.update(-cPos.getX().value(false), -cPos.getY().value(false), 0);

        width.set(w);
        height.set(-h);

        gl.glDisable(GL.GL_TEXTURE_2D);

        gl.glRectd(-1, 1, width.value(), height.value());
        for (LineView line : lines) {
            line.draw(gl);
        }

        updateCaret(document.getCaret());
        caret.draw(gl);
    }

    @Override
    public void updateCaret(CursorPosition c) {
        LineView lv = lines.get(c.getRow());
        List<CharView> cvl = lv.getChars();

        double x;
        if (document.isLineHead()) {
            x =
                    lv.getChars().stream().mapToDouble(cv -> cv.width() / 2).findFirst()
                            .orElse(caret.getWidth() / 2);
        } else if (document.isLineLast()) {
            x = lv.getWidth() + (caret.getWidth() / 2);
        } else {
            x = cvl.get(c.getCol()).position.getX().getLastValue();
        }
        double y = -lv.position.getY().getLastValue();

        caret.updatePosition(x, y);
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
        double h = 0;
        for (LineView lv2 : lines) {
            if (bufferLine == lv2.getBufferLine()) {
                lv2.position.getY().setWithoutSmooth(h);
            } else {
                lv2.position.getY().set(h);
            }
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
            double fromY = from.position.getY().value(false);
            double toY = to.position.getY().getLastValue();
            CharView leaveChar = from.leaveChar(c);
            leaveChar.position.getY().setWithoutSmooth(-(toY - fromY));
            to.visitChar(leaveChar);
        }));
    }

    private void updatePositions() {
        Collections.sort(lines);
        double h = 0;
        for (LineView lv : lines) {
            lv.position.getY().set(h);
            h -= LineView.getHeight();
        }
    }

}
