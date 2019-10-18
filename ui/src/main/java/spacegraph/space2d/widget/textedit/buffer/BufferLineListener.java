package spacegraph.space2d.widget.textedit.buffer;


import jcog.data.list.FasterList;

import java.util.Collection;

public interface BufferLineListener {
  void update(BufferLine bl);

  void addChar(BufferChar bufferChar, int col);

  void removeChar(BufferChar bufferChar);

  class BufferLineObserver implements BufferLineListener {

    private final Collection<BufferLineListener> listeners = new FasterList<>();

    public void addListener(BufferLineListener listener) {
      listeners.add(listener);
    }

    @Override
    public void update(BufferLine bl) {
        for (BufferLineListener l : listeners) {
            l.update(bl);
        }
    }

    @Override
    public void addChar(BufferChar bufferChar, int col) {
        for (BufferLineListener l : listeners) {
            l.addChar(bufferChar, col);
        }
    }

    @Override
    public void removeChar(BufferChar bufferChar) {
        for (BufferLineListener l : listeners) {
            l.removeChar(bufferChar);
        }
    }
  }
}
