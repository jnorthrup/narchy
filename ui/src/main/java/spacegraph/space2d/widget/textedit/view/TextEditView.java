package spacegraph.space2d.widget.textedit.view;

import com.jogamp.opengl.GL2;
import jcog.data.list.FasterList;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.widget.textedit.buffer.*;

import java.util.Collections;

public class TextEditView implements BufferListener {

    private final Buffer document;
    private final CursorView cursor;
    private final FasterList<LineView> lines = new FasterList<>();


    public TextEditView(Buffer buffer) {
        this.document = buffer;
        this.cursor = new CursorView(buffer.cursor());
        buffer.addListener(this);
        document.lines.forEach(this::addLine);
    }


    public void paint(boolean cursor, @Nullable RectFloat v, GL2 g) {
        //float charAspect = 1.4f;
        float charsWide = v.w;
        float charsHigh = v.h;
        float dx = 0, dy = 0;
        float vx = v.x, vy = v.y, vw = v.w, vh = v.h;

        g.glPushMatrix();
        g.glTranslatef(dx, 1f - (1f/charsHigh) + vy, 0);
        g.glScalef(1f/charsWide, 1f/charsHigh, 1f);



        int x1 = Math.max(0, (int) Math.floor(vx));
        int y1 = Math.max(0, (int) Math.round(vy));
        int x2 = x1 + (int) Math.ceil(vw);
        int y2 = y1 + (int) Math.round(vh);

        if (cursor)
            this.cursor.draw(g);

        for (int y = y1; y < y2; y++) {
            LineView line = lines.getSafe(y);
            if (line!=null)
                line.draw(g, x1, x2, y1 - y);
        }

        g.glPopMatrix();
    }

    @Override
    public void updateCursor(CursorPosition c) {
        LineView lv = lines.get(c.getRow());


        float x;
        if (document.isLineHead()) {
            x = (float) lv.getChars().stream().mapToDouble(cv -> cv.width() / 2).findFirst()
                            .orElse(cursor.getWidth() / 2);
        } else if (document.isLineLast()) {
            x = lv.getWidth() + (cursor.getWidth() / 2);
        } else {
            x = lv.getChars().get(c.getCol()).position.x;
        }
        cursor.position.set(x, lv.position.y, 0);
    }

    @Override
    public void update(Buffer buffer) {
        updatePositions();
        updateCursor(document.cursor());
    }

    @Override
    public void addLine(BufferLine bufferLine) {
        lines.add(new LineView(bufferLine));
        updatePositions();
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
            float fromY = from.position.y, toY = to.position.y;
            CharView leaveChar = from.leaveChar(c);
            leaveChar.position.y = -(toY - fromY);
            to.addChar(leaveChar);
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

//
//    private double documentHeight() {
//        double height = 0;
//        //for (LineView line : lines) {
//        height += lines.size() * LineView.getHeight();
//        //}
//        return height;
//    }
//
//    private double documentWidth() {
//        double maxWidth = 0;
//        for (LineView line : lines) {
//            double width = line.getWidth();
//            maxWidth = (maxWidth < width) ? width : maxWidth;
//        }
//        return maxWidth;
//    }