package spacegraph.space2d.widget.textedit.buffer;


import jcog.data.list.FasterList;

import java.util.Collection;

public interface BufferLineListener {
  void update(BufferLine bl);

  void addChar(BufferChar bufferChar);

  void removeChar(BufferChar bufferChar);

  class BufferLineObserver implements BufferLineListener {

    private final Collection<BufferLineListener> listeners = new FasterList<>();

    public void addListener(BufferLineListener listener) {
      listeners.add(listener);
    }

    @Override
    public void update(BufferLine bl) {
      listeners.forEach(l -> l.update(bl));
    }

    @Override
    public void addChar(BufferChar bufferChar) {
      listeners.forEach(l -> l.addChar(bufferChar));
    }

    @Override
    public void removeChar(BufferChar bufferChar) {
      listeners.forEach(l -> l.removeChar(bufferChar));
    }
  }
}
