package spacegraph.space2d.widget.console;

import com.googlecode.lanterna.TextCharacter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class StringBitmapSurface extends BitmapConsoleSurface {

    volatile String text = "";


    public void text(String s) {
        if (!s.equals(text)) {
            this.text = s;
            int l = s.length();
            resize(l, 1);
            for (int i=0; i < l; i++)
                redraw(new TextCharacter(s.charAt(i)), i, 0);

            setUpdateNecessary();
        }
    }

    @Override
    protected boolean updateBackBuffer() {
        return false;
    }

    @Override
    public TextCharacter charAt(int col, int row) {
        return new TextCharacter(text.charAt(row));
    }

    @Override
    public Appendable append(CharSequence charSequence) throws IOException {
        throw new UnsupportedEncodingException();
    }

    @Override
    public Appendable append(CharSequence charSequence, int i, int i1) throws IOException {
        throw new UnsupportedEncodingException();
    }

    @Override
    public Appendable append(char c) throws IOException {
        throw new UnsupportedEncodingException();
    }
}
