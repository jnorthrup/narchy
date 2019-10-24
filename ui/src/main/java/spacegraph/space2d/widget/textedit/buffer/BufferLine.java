package spacegraph.space2d.widget.textedit.buffer;


import jcog.data.list.FasterList;

import java.util.Collections;
import java.util.List;

public class BufferLine implements Comparable<BufferLine> {

    private int rowNum;
    private final List<BufferChar> chars = new FasterList<>();
    private final BufferLineListener.BufferLineObserver observer = new BufferLineListener.BufferLineObserver();

    public void addListener(BufferLineListener listener) {
        observer.addListener(listener);
    }

    void updatePosition(int row) {
        this.rowNum = row;
//        int cols = chars.size();
//        for (int col = 0; col < cols; col++) {
//            chars.get(col).updatePosition(row, col);
//        }
        update();
    }

    public int length() {
        return chars.size();
    }

    public String toLineString() {
        if (length()==0)
            return "";

        StringBuilder buf = new StringBuilder(chars.size());
        for (BufferChar bc : chars) {
            buf.append(bc.getChar());
        }
        return buf.toString();
    }

    void insertChar(char c, int col) {

        BufferChar bc = new BufferChar(c);
        insertChar(col, bc);
    }

    public void insertChar(int col, BufferChar bc) {
        chars.add(col, bc);
//        updatePosition(rowNum); //, col, col+1);
        observer.addChar(bc, col);
    }

    /** returns the right=most substring intended to be moved to the new line */
    List<BufferChar> splitReturn(int col) {

        int cs = chars.size();
        if (col == cs)
            return Collections.EMPTY_LIST; //EOL, nothing

        FasterList<BufferChar> results = new FasterList<>(cs-col);
        while (cs-- > col) {
            BufferChar c = removeChar(col);
            results.addFast(c);
        }
        update();
        return results;
    }

    public void update() {
        observer.update(this);
    }

    BufferChar removeChar(int col) {
        BufferChar removedChar = chars.remove(col);
        observer.removeChar(removedChar);
        return removedChar;
    }

    public void join(BufferLine line) {
        chars.addAll(line.chars);
        updatePosition(rowNum);
    }

    public List<BufferChar> getChars() {
        return chars;
    }

    public int getRowNum() {
        return rowNum;
    }

    @Override
    public int compareTo(BufferLine o) {
        return Integer.compare(rowNum, o.rowNum);
    }
}
