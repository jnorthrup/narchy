package spacegraph.space2d.widget.textedit.buffer;


import jcog.data.list.FasterList;

import java.util.Collection;

public interface BufferListener {

    void update(Buffer buffer);

    void addLine(BufferLine bufferLine);

    void removeLine(BufferLine bufferLine);

    void moveChar(BufferLine fromLine, BufferLine toLine, BufferChar c);

    class BufferObserver implements BufferListener {

        private final Collection<BufferListener> listeners = new FasterList<>();

        public void addListener(BufferListener listener) {
            listeners.add(listener);
        }

        @Override
        public void update(Buffer buffer) {
            for (var l : listeners) {
                l.update(buffer);
            }
        }

        @Override
        public void addLine(BufferLine bufferLine) {
            for (var l : listeners) {
                l.addLine(bufferLine);
            }
        }

        @Override
        public void removeLine(BufferLine bufferLine) {
            for (var l : listeners) {
                l.removeLine(bufferLine);
            }
        }

        public void moveChar(BufferLine fromLine, BufferLine toLine, BufferChar c) {
            for (var l : listeners) {
                l.moveChar(fromLine, toLine, c);
            }
        }

    }
}
