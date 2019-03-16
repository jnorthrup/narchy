package spacegraph.space2d.widget.textedit.buffer;


import jcog.data.list.FastCoWList;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Buffer {

    final String bufferName;

    private CursorPosition currentCursor = new CursorPosition(0, 0), mark = new CursorPosition(0, 0);

    public final FastCoWList<BufferLine> lines = new FastCoWList<>(BufferLine[]::new);

    private final BufferListener.BufferObserver observer = new BufferListener.BufferObserver();

    public Buffer(String bufferName, String value) {
        this.bufferName = bufferName;
        clear();
        insert(value);
    }

    public boolean isEmpty() {
        return lines.isEmpty() || lines.size()==1 && lines.get(0).length()==0;
    }

    public void clear() {

        synchronized (this) {

            int l = lines.size();
            if (l != 1 || lines.get(0).length() != 0) {
                if (l > 0) {
                    lines.forEach(observer::removeLine);
                    lines.clear();
                }

                BufferLine bl = new BufferLine();
                lines.add(bl);
                observer.addLine(bl);

                currentCursor.setCol(0);
                currentCursor.setRow(0);
                mark.setCol(0);
                mark.setRow(0);

                update();
            }
        }
    }

    public void addListener(BufferListener listener) {
        observer.addListener(listener);
    }

    private void update() {
        IntStream.range(0, lines.size()).forEach(i -> lines.get(i).updatePosition(i));
        observer.update(this);
        observer.updateCursor(currentCursor);
    }

    public void insert(String string) {
        switch (string) {
            case "":
                break;
            case "\r":
            case "\n":
            case "\r\n":
                insertEnter(true);
                break;
            default:
                if (string.contains("\n")) {
                    String[] values = string.split("\n");
                    synchronized(this) {
                        for (String x : values) {
                            insertChars(x, false);
                            insertEnter(false);
                        }
                        update();
                    }
                } else {
                    insertChars(string, true);
                }
//                String[] values = string.split("(\r\n|\n|\r)");
//                if (values.length == 1) {
//                    values[0].codePoints().forEach(
//                            codePoint -> insertChar(new String(Character.toChars(codePoint))));
//                } else {
//                    Arrays.stream(values).forEach(v -> {
//                        v.codePoints().forEach(codePoint -> insertChar(new String(Character.toChars(codePoint))));
//                        insertEnter();
//                    });
//                }
                break;
        }

    }

    public void insertChars(CharSequence string, boolean update) {
        int n = string.length();
        if (n > 0) {
            synchronized (this) {
                BufferLine line = currentLine();
                int colStart = currentCursor.getCol();
                for (int i = 0; i < n; i++) {
                    line.insertChar(colStart + i, string.charAt(i));
                }
                currentCursor.incCol(n);
                if (update)
                    update();
            }
        }
    }

    public void insertEnter(boolean update) {
        synchronized (this) {
            BufferLine currentLine = currentLine();

            List<BufferChar> leaveChars = currentLine.insertEnter(currentCursor.getCol());

            BufferLine nextLine = new BufferLine();
            lines.add(currentCursor.getRow() + 1, nextLine);
            if (update) {
                update();
            }
            observer.addLine(nextLine);
            leaveChars.forEach(c -> {
                nextLine.getChars().add(c);
                observer.moveChar(currentLine, nextLine, c);
            });
            currentCursor.setCol(0);
            currentCursor.incRow(1);
            observer.updateCursor(currentCursor);
        }
    }

    /** width in columns */
    public int width() {
        return lines.stream().mapToInt(BufferLine::length).max().orElse(0);
    }

    /** height in lines aka rows */
    public int height() { return lines.size(); }

    private BufferLine currentLine() {
        //synchronized (this) {
            return lines.get(currentCursor.getRow());
        //}
    }

//    public BufferLine preLine() {
//        if (currentCursor.getRow() == 0) {
//            return null;
//        }
//        return lines.get(currentCursor.getRow() - 1);
//    }
//
//    public BufferLine postLine() {
//        if (currentCursor.getRow() == lines.size() - 1) {
//            return null;
//        }
//        return lines.get(currentCursor.getRow() - 1);
//    }

    public void set(String text) {
        synchronized (this) {
            clear();
            insert(text);
        }
    }



    public boolean textEquals(String s) {
        if (s.isEmpty()) {
            return isEmpty();
        } else {
            if (lines.isEmpty())
                return s.isEmpty();
            else {
                //TODO optimize without necessarily constructing String
                return text().equals(s);
            }
        }
    }

    public String text() {
        switch (lines.size()) {
            case 0: return "";
            case 1: return lines.get(0).toLineString();
            default:
                return lines.stream().map(BufferLine::toLineString).collect(Collectors.joining("\n"));
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(text());
        buf.append(String.format("Caret:[%d,%d]", currentCursor.getCol(), currentCursor.getRow()));
        return buf.toString();
    }

    public CursorPosition cursor() {
        return currentCursor;
    }

    public void backspace() {
        synchronized(this) {
            if (!isBufferHead() || !isLineHead()) {
                back();
                delete();
            }
        }
    }

    public void delete() {
        synchronized(this) {
            if (!isEmpty() && (!isBufferLast() || !isLineLast())) {
                if (!isLineLast()) {
                    currentLine().removeChar(currentCursor.getCol());
                } else {
                    isLineLast();
                    if (!isBufferLast()) {
                        BufferLine currentLine = currentLine();
                        BufferLine removedLine = lines.remove(currentCursor.getRow() + 1);
                        currentLine.join(removedLine);
                        removedLine.getChars().forEach(c -> observer.moveChar(removedLine, currentLine, c));
                        observer.removeLine(removedLine);
                    }
                }
                update();
            }
        }
    }

    public void head() {
        if (currentCursor.setCol(0))
            observer.updateCursor(currentCursor);
    }

    public void last() {
        last(true);
    }
    public void last(boolean update) {
        if (currentCursor.setCol(currentLine().length()) && update)
            observer.updateCursor(currentCursor);
    }

    public void back() {
        if (isLineHead()) {
            boolean isDocHead = isBufferHead();
            previous();
            if (!isDocHead) {
                last();
            }
        } else {
            if (currentCursor.decCol(1))
                observer.updateCursor(currentCursor);
        }
    }

    public void forward() {
        if (isLineLast()) {
            boolean isDocLast = isBufferLast();
            next();
            if (!isDocLast) {
                head();
            }
        } else {
            if (currentCursor.incCol(1))
                observer.updateCursor(currentCursor);
        }
    }

    public void previous() {
        if (!isBufferHead()) {
            if (currentCursor.decRow(1)) {
                if (currentCursor.getCol() > currentLine().length()) {
                    last(false);
                }
                observer.updateCursor(currentCursor);
            }
        }
    }

    public void next() {
        if (!isBufferLast()) {
            if (currentCursor.incRow(1)) {
                if (currentCursor.getCol() > currentLine().length()) {
                    last(false);
                }
                observer.updateCursor(currentCursor);
            }

        }
    }

    public void bufferHead() {
        if (currentCursor.setRow(0) || currentCursor.setCol(0))
            observer.updateCursor(currentCursor);
    }

    public void bufferLast() {
        if (currentCursor.setRow(lines.size() - 1) || currentCursor.setCol(currentLine().length()))
            observer.updateCursor(currentCursor);
    }

    private boolean isBufferHead() {
        return currentCursor.getRow() == 0;
    }

    public boolean isLineHead() {
        return currentCursor.getCol() == 0;
    }

    private boolean isBufferLast() {
        return currentCursor.getRow() == lines.size() - 1;
    }

    public boolean isLineLast() {
        return currentCursor.getCol() == currentLine().length();
    }


    public void mark() {
        mark.setCol(currentCursor.getCol());
        mark.setRow(currentCursor.getRow());
    }

    public String copy() {
        StringBuilder buf = new StringBuilder();
        if (mark.compareTo(currentCursor) == 0) {
            return buf.toString();
        }

        CursorPosition head = mark;
        CursorPosition tail = currentCursor;
        if (mark.compareTo(currentCursor) > 0) {
            head = currentCursor;
            tail = mark;
        }

        if (head.getRow() == tail.getRow()) {
            buf.append(lines.get(head.getRow()).toLineString(), head.getCol(), tail.getCol());
        } else {
            buf.append(lines.get(head.getRow()).toLineString().substring(head.getCol()));
            if (tail.getRow() - head.getRow() > 1) {
                for (int i = head.getRow() + 1; i < tail.getRow(); i++) {
                    buf.append('\n');
                    buf.append(lines.get(i).toLineString());
                }
            }
            buf.append('\n');
            buf.append(lines.get(tail.getRow()).toLineString(), 0, tail.getCol());
        }
        return buf.toString();
    }

    public void cut() {
        if (mark.compareTo(currentCursor) == 0) {
            return;
        }
        if (mark.compareTo(currentCursor) > 0) {
            CursorPosition tmp = mark;
            mark = currentCursor;
            currentCursor = tmp;
        }
        while (mark.compareTo(currentCursor) != 0) {
            backspace();
        }
    }


}
