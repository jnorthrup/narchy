package spacegraph.space2d.widget.textedit.buffer;


import jcog.data.list.FasterList;

import java.util.Collection;

public interface BufferListener {

    void update(Buffer buffer);

    void updateCaret(CursorPosition cursor);

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
            listeners.forEach((l) -> l.update(buffer));
        }

        @Override
        public void updateCaret(CursorPosition cursor) {
            listeners.forEach((l) -> l.updateCaret(cursor));
        }

        @Override
        public void addLine(BufferLine bufferLine) {
            listeners.forEach((l) -> l.addLine(bufferLine));
        }

        @Override
        public void removeLine(BufferLine bufferLine) {
            listeners.forEach((l) -> l.removeLine(bufferLine));
        }

        public void moveChar(BufferLine fromLine, BufferLine toLine, BufferChar c) {
            listeners.forEach((l) -> l.moveChar(fromLine, toLine, c));
        }

    }
}
