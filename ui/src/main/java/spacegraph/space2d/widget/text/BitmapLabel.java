package spacegraph.space2d.widget.text;


import jcog.Texts;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.widget.console.BitmapTextGrid;
import spacegraph.util.math.Color4f;

public class BitmapLabel extends AbstractLabel {

    private BitmapTextGrid view;
//    static final float minPixelsToBeVisible = 7;

    private volatile RectFloat textBounds;

    public BitmapLabel() {
        this("");
    }

    public BitmapLabel(String text) {
        super();

        textBounds = bounds;



        view = new MyBitmapTextGrid();
        text(text);
    }

    @Override
    protected void starting() {
        view.start(this);
    }

    @Override
    protected void stopping() {
        view.delete();
        super.stopping();
    }

    @Override
    public AbstractLabel text(String next) {
        String prev = text;
        if (prev==null || !prev.equals(next)) {

            int rows = 1 + Texts.countRows(next, '\n');
            boolean resized;
            if (rows == 1) {
                resized = view.resize(next.length(), 1);
            } else {
                //HACK do better
                //int cols = Arrays.stream(next.split("\n")).mapToInt(String::length).max().getAsInt();
                resized = view.resize(Texts.countCols(next), rows);
            }

            super.text(next);

            view.invalidate();
        }
        return this;
    }


    @Override
    protected void doLayout(float dtS) {
        RectFloat b = bounds;
        int r = view.rows; if (r > 0) {
            int c = view.cols; if (c > 0) {
                b = AspectAlign.innerBounds(b, (r * characterAspectRatio) / c);
            }
        }
        textBounds = b;
        //view.invalidate();
    }

    @Override
    protected final void renderContent(ReSurface r) {
        view.pos(bounds);
        view.renderIfVisible(r);
    }


    public BitmapLabel alpha(float a) {
        this.view.alpha(a);
        return this;
    }


    public AbstractLabel textColor(float rr, float gg, float bb, float aa) {
        fgColor.set((rr), (gg), (bb), 1f);
        return this;
    }

    public final AbstractLabel textColor(float rr, float gg, float bb) {
        return textColor(rr, gg, bb, 1f);
    }

    public AbstractLabel backgroundColor(float rr, float gg, float bb, float aa) {
        bgColor.set((rr), (gg), (bb), aa);
        return this;
    }

    public final AbstractLabel backgroundColor(float rr, float gg, float bb) {
        return backgroundColor(rr, gg, bb, 1f);
    }


    private class MyBitmapTextGrid extends BitmapTextGrid {

        MyBitmapTextGrid() {
            cursorCol = cursorRow = -1; //hidden
        }

        @Override
        protected RectFloat textBounds() {
            return textBounds;
        }

        @Override
        protected boolean renderText() {

            clearBackground(); //may not be necessary if only one line and all characters are used but in multiline the matrix currently isnt regular so some chars will not be redrawn

            String s = BitmapLabel.this.text;
            int n = s.length();
            int row = 0, col =0;
            final Color4f fg = BitmapLabel.this.fgColor, bg = BitmapLabel.this.bgColor;
            for (int i = 0; i < n; i++) {
                char c = s.charAt(i);
                if (c == '\n') {
                    row++;
                    col = 0;
                } else {
                    redraw(c, col++, row, fg, bg);
                }
            }
            return true;
        }


    }
}
