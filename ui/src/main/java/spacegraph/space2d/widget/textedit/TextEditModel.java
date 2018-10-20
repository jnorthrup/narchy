package spacegraph.space2d.widget.textedit;

import spacegraph.input.key.KeyPressed;
import spacegraph.space2d.widget.textedit.buffer.Buffer;
import spacegraph.space2d.widget.textedit.view.BufferView;

public final class TextEditModel  {

//    int cursorX = 0, cursorY = 0;
//
//    public static class TextCell {
//        char c;
//        float r, g, b;
//        //TODO other properties
//        @Nullable Consumer<GL> render;
//    }

    /** current buffer */
    private Buffer buffer;

    public BufferView view = null;

    public TextEditActions actions;
    public KeyPressed keys;

    public TextEditModel() {
        this(new Buffer("", ""));
    }

    public TextEditModel(Buffer buf) {
        setBuffer(buf);
    }

    public synchronized void setBuffer(Buffer buf) {
        if (buffer != buf) {
            buffer = buf;
            view = new BufferView(buf);
        }
    }

//    public boolean keyPressed(SupportKey supportKey, int keyCode, long when) {
//        return keys.keyPressed(supportKey, keyCode, when);
//    }
//
//    public boolean keyTyped(char typedString, long when) {
//        return keys.keyTyped(typedString, when);
//    }

    public void execute(String name, String... args) {
        actions.run(this, name, args);
    }

    public final Buffer buffer() {
        return this.buffer;
    }


    public void createNewBuffer() {
        setBuffer(new Buffer("scratch-" + System.currentTimeMillis(), ""));
    }


}
