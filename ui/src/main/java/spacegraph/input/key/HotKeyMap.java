package spacegraph.input.key;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import org.eclipse.collections.impl.map.mutable.primitive.ShortObjectHashMap;

import java.io.PrintStream;

class HotKeyMap extends KeyAdapter {

    static final class Reaction {
        final String name;
        final Runnable run;
        private final short code;

        Reaction(short code, String name, Runnable run) {
            this.code = code;
            this.name = name;
            this.run = run;
        }

        @Override
        public String toString() {
            int awtCode = Keyboard.newtKeyCode2AWTKeyCode(code);
            return java.awt.event.KeyEvent.getKeyText(awtCode) + "=" + name;
        }
    }

    private final ShortObjectHashMap<Reaction> onPressed = new ShortObjectHashMap<>();

    void add(short code, String s, Runnable r) {
        onPressed.put(code, new Reaction(code, s, r));
        onPressed.compact();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        Reaction r = onPressed.get(e.getKeyCode());
        if (r!=null) {
            r.run.run();
            e.setConsumed(true);
        }
    }

    void print(PrintStream out) {
        onPressed.forEach(out::println);
    }

}
