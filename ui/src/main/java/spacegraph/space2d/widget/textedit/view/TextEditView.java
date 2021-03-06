package spacegraph.space2d.widget.textedit.view;

import com.jogamp.opengl.GL2;
import jcog.data.list.FastCoWList;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.widget.textedit.buffer.*;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class TextEditView implements BufferListener {

    private final Buffer document;
    private final CursorView cursor;
    private final FastCoWList<LineView> lines = new FastCoWList(LineView[]::new);


    protected TextEditView(Buffer buffer) {
        this.document = buffer;
        this.cursor = new CursorView(buffer.cursor());
        for (BufferLine line : document.lines) {
            _addLine(line);
        }
        buffer.addListener(this);
    }


    public void paint(boolean cursor, @Nullable RectFloat v, GL2 g) {
        //float charAspect = 1.4f;
        float charsWide = v.w;
        float charsHigh = v.h;
        float vx = v.x, vy = v.y, vw = v.w, vh = v.h;

        g.glPushMatrix();
        float dx = (float) 0;
        g.glTranslatef(dx, 1f - (0.5f / charsHigh) + vy, (float) 0);
        g.glScalef(1f / charsWide, 1f / charsHigh, 1f);

        int x1 = Math.max(0, (int) Math.floor((double) vx));
        int y1 = Math.max(0, (int) Math.floor((double) vy));
        int x2 = x1 + (int) Math.ceil((double) vw);
        int y2 = y1 + (int) Math.ceil((double) vh);

        if (cursor) {
            updateCursor(document.cursor());
            this.cursor.draw(g);
        }

        float ox = (float) x1 - vx;
        LineView[] ll = lines.array();
        for (int y = Math.max(0, y1); y < Math.min(ll.length, y2); y++) {
            LineView line = ll[y];
            if (line != null) {
                line.draw(g, x1, x2, ox, (float) (y1 - y));
            }
        }

        g.glPopMatrix();
    }


    private void updateCursor(CursorPosition c) {
        LineView lv = lines.get(c.getRow());
        if (lv == null)
            return; //HACK

        int lineChars = lv.length();

        float x;
        if (document.isLineStart()) {
//            x = (float) lv.getChars().stream().mapToDouble(cv -> cv.width() / 2).findFirst()
//                    .orElse(cursor.getWidth() / 2);
            x = CursorView.getWidth()/ 2.0F; //lv.getChars().get(0).width()/2;
        } else if (c.getCol() >= lineChars) {
            x = lv.getWidth() + (CursorView.getWidth() / 2.0F);
        } else {
            x = lv.getChars().get(c.getCol()).position.x;
        }

        cursor.position.set(x, lv.position.y, (float) 0);
    }

    @Override
    public void update(Buffer buffer) {
        updateY();
        updateCursor(document.cursor());
    }

    @Override
    public void addLine(BufferLine bufferLine) {
        _addLine(bufferLine);
        updateY();
    }

    private void _addLine(BufferLine bufferLine) {
        lines.add(new LineView(bufferLine));
    }

    @Override
    public void removeLine(BufferLine bufferLine) {
        if (lines.removeIf(new Predicate<LineView>() {
            @Override
            public boolean test(LineView lineView) {
                return lineView.getBufferLine() == bufferLine;
            }
        }))
            updateY();
    }

    @Override
    public void moveChar(BufferLine fromLine, BufferLine toLine, BufferChar c) {
        int[] k = {0};
        lines.stream().filter(new Predicate<LineView>() {
            @Override
            public boolean test(LineView l) {
                return l.getBufferLine() == fromLine;
            }
        }).findFirst().ifPresent(
                new Consumer<LineView>() {
                    @Override
                    public void accept(LineView from) {
                        lines.stream().filter(new Predicate<LineView>() {
                            @Override
                            public boolean test(LineView l) {
                                return l.getBufferLine() == toLine;
                            }
                        }).
                                findFirst().ifPresent(new Consumer<LineView>() {
                            @Override
                            public void accept(LineView to) {
                                float fromY = from.position.y, toY = to.position.y;
                                CharView leaveChar = from.leaveChar(c);
                                leaveChar.position.y = -(toY - fromY);
                                to.addChar(leaveChar, k[0]++);
                                to.update();
                            }
                        });
                    }
                });
    }

    /**
     * update y positions of each line
     */
    protected void updateY() {
        synchronized (lines) {
            lines.sort();

            float h = (float) 0;
            for (LineView lv : lines) {
                lv.position.y = h;
                h = (float) ((double) h - LineView.getHeight());
            }
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