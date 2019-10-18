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
        text(value);
    }

    public boolean isEmpty() {
        return lines.isEmpty() || lines.size()==1 && lines.get(0).length()==0;
    }

    public void clear() {

        synchronized (this) {

            int l = lines.size();
            if (l > 0) {
                for (BufferLine line : lines) {
                    observer.removeLine(line);
                }
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

    public void addListener(BufferListener listener) {
        observer.addListener(listener);
    }

    @Deprecated private void update() {
        int bound = lines.size();
        for (int i = 0; i < bound; i++) {
            lines.get(i).updatePosition(i);
        }
        observer.update(this);
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
                    line.insertChar(string.charAt(i), colStart + i);
                }
                currentCursor.incCol(n);
                if (update)
                    update();
            }
        }
    }

    public void insertEnter(boolean update) {
        synchronized (this) {
            BufferLine currentRow = currentLine();

            List<BufferChar> nextLineChars = currentRow.splitReturn(currentCursor.getCol());

            BufferLine nextLine = new BufferLine();
            lines.add(currentCursor.getRow() + 1, nextLine);
            observer.addLine(nextLine);
            currentCursor.setCol(0);
            currentCursor.incRow(1);

            if (update) {
                update();
                //observer.update(this); //light
            }


            //List<BufferChar> nlc = nextLine.getChars();
            int[] k = {0};
            //nlc.add(c);
            //observer.moveChar(currentLine, nextLine, c);
            for (BufferChar c : nextLineChars) {
                nextLine.insertChar(k[0]++, c);
            }

            nextLine.update();

            if (update) {
                update();
            }
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



    public boolean textEquals(String s) {
        if (s.isEmpty()) {
            return isEmpty();
        } else {
            if (lines.isEmpty())
                return false;
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
        String buf = text() +
                String.format("Caret:[%d,%d]", currentCursor.getCol(), currentCursor.getRow());
        return buf;
    }

    public CursorPosition cursor() {
        return currentCursor;
    }

    public void backspace() {
        synchronized(this) {
            if (!isBufferHead() || !isLineStart()) {
                back();
                delete();
            }
        }
    }

    public void delete() {
        synchronized(this) {
            if (!isEmpty() && (!isBufferLast() || !isLineEnd())) {
                if (!isLineEnd()) {
                    currentLine().removeChar(currentCursor.getCol());
                } else {
                    isLineEnd();
                    if (!isBufferLast()) {
                        BufferLine currentLine = currentLine();
                        BufferLine removedLine = lines.remove(currentCursor.getRow() + 1);
                        currentLine.join(removedLine);
                        //TODO fix where characters are appended here. to not use moveChar() then remove that method
                        //see splitReturn() for similarity
                        for (BufferChar c : removedLine.getChars()) {
                            observer.moveChar(removedLine, currentLine, c);
                        }
                        observer.removeLine(removedLine);
                    }
                }
                update();
            }
        }
    }

    public void head() {
        currentCursor.setCol(0);
    }

    public void last() {
        last(true);
    }
    public void last(boolean update) {
        currentCursor.setCol(currentLine().length());
    }

    public void back() {
        if (isLineStart()) {
            boolean isDocHead = isBufferHead();
            previous();
            if (!isDocHead) {
                last();
            }
        } else {
            currentCursor.decCol(1);
        }
    }

    public void forward() {
        if (isLineEnd()) {
            boolean isDocLast = isBufferLast();
            next();
            if (!isDocLast) {
                head();
            }
        } else {
            currentCursor.incCol(1);
        }
    }

    public void previous() {
        if (!isBufferHead()) {
            if (currentCursor.decRow(1)) {
                if (currentCursor.getCol() > currentLine().length()) {
                    last(false);
                }
            }
        }
    }

    public void next() {
        if (!isBufferLast()) {
            if (currentCursor.incRow(1)) {
                if (currentCursor.getCol() > currentLine().length()) {
                    last(false);
                }
            }

        }
    }

    public void bufferHead() {
        currentCursor.setRow(0);
        currentCursor.setCol(0);
    }

    public void bufferLast() {
        currentCursor.setRow(lines.size() - 1);
        currentCursor.setCol(currentLine().length());
    }

    private boolean isBufferHead() {
        return currentCursor.getRow() == 0;
    }

    public boolean isLineStart() {
        return currentCursor.getCol() == 0;
    }

    private boolean isBufferLast() {
        return currentCursor.getRow() == lines.size() - 1;
    }

    public boolean isLineEnd() {

        int ll = currentLine().length();
        int cc = currentCursor.getCol();
        if (cc > ll) {
            currentCursor.setCol(ll);
            cc = ll;
        }
        return cc == ll;
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


    public void text(String text) {
        synchronized (this) {
            clear();
            insert(text);
        }
    }
}
