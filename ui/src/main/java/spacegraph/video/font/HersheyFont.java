package spacegraph.video.font;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtil;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import spacegraph.video.Draw;

import java.io.IOException;
import java.util.List;

public final class HersheyFont {

    private static final HersheyFont[] fontMono;

    static final int offsetR = ('R');
    /*int idx, verts, */
    final int leftPos;
    final int rightPos;

    final byte[][] segments;
    private int id;

    static {

        String[] lines = null;


        for (var tries = 0; tries < 2; tries++) {
            try {
                var font =
                        "futural";

                lines = new String(Draw.class.getClassLoader().getResourceAsStream("spacegraph/font/hershey/" + font + ".jhf").readAllBytes()).split("\n");
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            Util.sleepMS(1);
        }
        if (lines == null) {
            lines = ArrayUtil.EMPTY_STRING_ARRAY;
        }

        var scratch = "";
        List<HersheyFont> glyphs = new FasterList(256);
        for (var line : lines) {
            var c = line;
            if (c.endsWith("\n"))
                c = c.substring(0, c.length() - 1);


            if (Character.isDigit(c.charAt(4))) {
                var nextGlyph = new HersheyFont(c + scratch);

                glyphs.add(nextGlyph);
                scratch = "";
            } else {
                scratch += c;
            }
        }


        fontMono = glyphs.toArray(new HersheyFont[0]);
    }

    /**
     * call glPush before this, and after all textNext's. returns the character width to translate by to display the next character (left to right direction)
     */
    public static void textStart(GL2 gl, float scaleX, float scaleY, float x, float y, float z) {

        gl.glTranslatef(x, y, z);
        gl.glScalef(scaleX / 20, scaleY / 20, 1);
    }

    public static void textNext(GL2 gl, char c, float x) {

        var ci = c - 32;
        if (ci >= 0 && ci < fontMono.length) {
            fontMono[ci].draw(gl, x * 20);
        }

    }

    /** load glyphs into GL context */
    public static void load(GL2 gl) {
        for (var x : fontMono)
            x.init(gl);
    }

    HersheyFont(String hspec) {


        leftPos = (hspec.charAt(8)) - offsetR;
        rightPos = (hspec.charAt(9)) - offsetR;

        //        boolean penUp = true;
        var currentSeg = new ByteArrayList(8);

        var spec = (hspec.substring(10));
        var ss = spec.length() - 1;
        FasterList<byte[]> segments = new FasterList();
        for (var i = 0; i < ss; i += 2) {

            char ci = spec.charAt(i), cii = spec.charAt(i + 1);

            if (cii == 'R' && ci == ' ') {
//                penUp = true;
                segments.add(currentSeg.toArray());
                currentSeg = new ByteArrayList(8);
                continue;
            }

            var curX = ci - offsetR;
            currentSeg.add((byte) curX);
            var curY = cii - offsetR;
            currentSeg.add((byte) (10 - curY));
        }
        if (!currentSeg.isEmpty())
            segments.add(currentSeg.toArray());

        this.segments = segments.toArray(new byte[segments.size()][]);


    }

    public static void hersheyText(GL2 gl, CharSequence s, float scale, float x, float y, float z) {
        hersheyText(gl, s, scale, scale, x, y, z, Draw.TextAlignment.Center);
    }

    public static void hersheyText(GL2 gl, CharSequence s, float scale, float x, float y, float z, Draw.TextAlignment a) {
        hersheyText(gl, s, scale, scale, x, y, z, a);
    }

    public static void hersheyText(GL2 gl, CharSequence s, float scaleX, float scaleY, float x, float y, float z, Draw.TextAlignment a) {


        var sl = s.length();
        if (sl == 0)
            return;

        var totalWidth = sl * scaleX;
        switch (a) {
            case Left:
                x += scaleX / 2f;
                break;
            case Right:
                x -= totalWidth;
                break;
            case Center:
                x -= totalWidth / 2f;
                break;
        }

        Draw.push(gl);

        textStart(gl, scaleX, scaleY, x, y, z);

        for (var i = 0; i < sl; i++) {
            textNext(gl, s.charAt(i), i);
        }

        Draw.pop(gl);
    }

    public static void hersheyText(GL2 gl, char c, float scale, float x, float y, float z) {
        hersheyText(gl, c, scale, scale, x, y, z);
    }

    private static void hersheyText(GL2 gl, char c, float scaleX, float scaleY, float x, float y, float z) {

        var ci = c - 32;
        if (ci >= 0 && (ci < fontMono.length)) {

            Draw.push(gl);

            var sx = scaleX / 20f;
            var sy = scaleY / 20f;
            gl.glScalef(sx, sy, 1f);

            gl.glTranslatef(x / sx, y / sy, z);

            fontMono[ci].draw(gl);
            Draw.pop(gl);
        }
    }

    void draw(GL2 gl, float x) {


        var xShifted = Math.abs(x) > Float.MIN_NORMAL;

        if (xShifted)
            gl.glTranslatef(x, 0, 0);

        draw(gl);

        if (xShifted)
            gl.glTranslatef(-x, 0, 0);
    }

    final void draw(GL2 gl) {
        gl.glCallList(id);
    }

    private void init(GL2 gl) {
        id = gl.glGenLists(1);
        gl.glNewList(id, GL2.GL_COMPILE);

        for (var seg : segments) {

            var ss = seg.length;

            gl.glBegin(GL.GL_LINE_STRIP);
            for (var j = 0; j < ss; )
                gl.glVertex2i(seg[j++], seg[j++]);
            gl.glEnd();
        }

        gl.glEndList();
    }
}
