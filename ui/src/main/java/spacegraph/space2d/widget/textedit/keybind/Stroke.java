package spacegraph.space2d.widget.textedit.keybind;

import jcog.Util;

class Stroke {
    public final SupportKey supportKey;
    public final int key;
    private final int hash;

    public Stroke(SupportKey supportKey, int key) {
        this.supportKey = supportKey;
        this.key = key;
        this.hash = Util.hashCombine(supportKey, key);
    }

    @Override
    public String toString() {
        return String.format("Stroke[%s-%s]", supportKey, key);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Stroke) {
            Stroke stroke = (Stroke) obj;
            return (this.key == stroke.key) && (this.supportKey == stroke.supportKey);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hash;
    }
}

