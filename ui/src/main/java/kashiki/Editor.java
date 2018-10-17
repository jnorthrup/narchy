package kashiki;

import kashiki.buffer.Buffer;
import kashiki.keybind.ActionRepository;
import kashiki.keybind.KashikiKeyListener;
import kashiki.keybind.SupportKey;
import kashiki.view.Base;
import kashiki.view.BufferView;
import kashiki.view.SmoothValue;

import java.util.ArrayList;
import java.util.List;

public final class Editor implements KashikiKeyListener {

    private Buffer currentBuffer;
    private final List<Base> drawn = new ArrayList<>();

    public ActionRepository actions;
    public KashikiKeyListener keys;
    private final SmoothValue scale = new SmoothValue(1);

    public Editor() {
        this(new Buffer("", ""));
    }

    public Editor(Buffer buf) {
        setBuffer(buf);
    }

    public synchronized void setBuffer(Buffer buf) {
        if (currentBuffer != buf) {
            drawn.clear();
            currentBuffer = buf;
            drawn.add(new BufferView(buf));
        }
    }

    public boolean keyPressed(SupportKey supportKey, int keyCode, long when) {
        return keys.keyPressed(supportKey, keyCode, when);
    }

    public boolean keyTyped(char typedString, long when) {
        return keys.keyTyped(typedString, when);
    }

    public void executeAction(String name, String... args) {
        actions.run(this, name, args);
    }

    public Buffer buffer() {
        return this.currentBuffer;
    }

    public List<Base> drawables() {
        return drawn;
    }

    public SmoothValue scale() {
        return this.scale;
    }

    public void createNewBuffer() {
        Buffer buf = new Buffer("scratch-" + System.currentTimeMillis(), "");

        setBuffer(buf);
        drawn.clear();
        BufferView bufView = new BufferView(buf);
        drawn.add(bufView);
    }

    public void reflesh() {
        drawn.clear();
        drawn.add(new BufferView(currentBuffer));
    }


}
