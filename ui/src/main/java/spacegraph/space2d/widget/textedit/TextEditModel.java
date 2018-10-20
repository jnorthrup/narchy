package spacegraph.space2d.widget.textedit;

import jcog.data.list.FasterList;
import spacegraph.input.key.KeyPressed;
import spacegraph.space2d.widget.textedit.buffer.Buffer;
import spacegraph.space2d.widget.textedit.keybind.ActionRepository;
import spacegraph.space2d.widget.textedit.view.BufferView;
import spacegraph.space2d.widget.textedit.view.SmoothValue;
import spacegraph.space2d.widget.textedit.view.TextEditView;

import java.util.List;

public final class TextEditModel  {

//    int cursorX = 0, cursorY = 0;
//
//    public static class TextCell {
//        char c;
//        float r, g, b;
//        //TODO other properties
//        @Nullable Consumer<GL> render;
//    }

    private Buffer currentBuffer;
    private final List<TextEditView> drawn = new FasterList<>();

    public ActionRepository actions;
    public KeyPressed keys;
    private final SmoothValue scale = new SmoothValue(1);

    public TextEditModel() {
        this(new Buffer("", ""));
    }

    public TextEditModel(Buffer buf) {
        setBuffer(buf);
    }

    public synchronized void setBuffer(Buffer buf) {
        if (currentBuffer != buf) {
            drawn.clear();
            currentBuffer = buf;
            drawn.add(new BufferView(buf));
        }
    }

//    public boolean keyPressed(SupportKey supportKey, int keyCode, long when) {
//        return keys.keyPressed(supportKey, keyCode, when);
//    }
//
//    public boolean keyTyped(char typedString, long when) {
//        return keys.keyTyped(typedString, when);
//    }

    public void executeAction(String name, String... args) {
        actions.run(this, name, args);
    }

    public Buffer buffer() {
        return this.currentBuffer;
    }

    public List<TextEditView> drawables() {
        return drawn;
    }

    public SmoothValue scale() {
        return this.scale;
    }

    public void createNewBuffer() {
        setBuffer(new Buffer("scratch-" + System.currentTimeMillis(), ""));
    }

    public void reflesh() {
        drawn.clear();
        drawn.add(new BufferView(currentBuffer));
    }


}
