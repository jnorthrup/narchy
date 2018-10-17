package kashiki.buffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Buffer {
  final String bufferName;
  private CursorPosition currentCursor = new CursorPosition(0, 0);
  private CursorPosition mark = new CursorPosition(0, 0);
  final List<BufferLine> lines = new ArrayList<>();
  private final BufferListener.BufferObserver observer = new BufferListener.BufferObserver();

  public Buffer(String bufferName, String value) {
    this.bufferName = bufferName;
    BufferLine bl = new BufferLine();
    lines.add(bl);
    observer.addLine(bl);
    update();
    insertString(value);
  }

  public void addListener(BufferListener listener) {
    observer.addListener(listener);
  }

  private void update() {
    for (int i = 0; i < lines.size(); i++) {
      lines.get(i).updatePosition(i);
    }
    observer.update(this);
    observer.updateCaret(currentCursor);
  }

  public void insertString(String string) {
    String[] values = string.split("(\r\n|\n)");
    if (values.length == 0) {
      insertEnter();
    } else if (values.length == 1) {
      values[0].codePoints().forEach(
          codePoint -> insertChar(new String(Character.toChars(codePoint))));
    } else {
      Arrays.stream(values).forEach(v -> {
        v.codePoints().forEach(codePoint -> insertChar(new String(Character.toChars(codePoint))));
        insertEnter();
      });
    }
  }

  private void insertChar(String c) {
    currentLine().insertChar(currentCursor.getCol(), c);
    currentCursor.incCol(1);
    observer.updateCaret(currentCursor);
  }

  public void insertEnter() {
    BufferLine currentLine = currentLine();
    BufferLine nextLine = new BufferLine();
    List<BufferChar> leaveChars = currentLine.insertEnter(currentCursor.getCol());
    lines.add(currentCursor.getRow() + 1, nextLine);
    update();
    observer.addLine(nextLine);
    leaveChars.forEach(c -> {
      nextLine.getChars().add(c);
      observer.moveChar(currentLine, nextLine, c);
    });
    currentCursor.setCol(0);
    currentCursor.incRow(1);
    observer.updateCaret(currentCursor);
  }

  public int getMaxColNum() {
    return lines.stream().max(Comparator.comparingInt(BufferLine::getLength))
        .orElse(new BufferLine()).getLength();
  }

  public List<BufferLine> getLines() {
    return lines;
  }

  private BufferLine currentLine() {
    return lines.get(currentCursor.getRow());
  }

  public BufferLine preLine() {
    if (currentCursor.getRow() == 0) {
      return null;
    }
    return lines.get(currentCursor.getRow() - 1);
  }

  public BufferLine postLine() {
    if (currentCursor.getRow() == lines.size() - 1) {
      return null;
    }
    return lines.get(currentCursor.getRow() - 1);
  }

  public String toBufferString() {
    return lines.stream().map(BufferLine::toLineString).collect(Collectors.joining("\n"));
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(toBufferString());
    buf.append(String.format("Caret:[%d,%d]", currentCursor.getCol(), currentCursor.getRow()));
    return buf.toString();
  }

  public CursorPosition getCaret() {
    return currentCursor;
  }

  public void backspace() {
    if (isBufferHead() && isLineHead()) {
      return;
    }
    back();
    delete();
  }

  public void delete() {
    if (!isBufferLast() || !isLineLast()) {
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
    }
  }

  public void head() {
    currentCursor.setCol(0);
    observer.updateCaret(currentCursor);
  }

  public void last() {
    currentCursor.setCol(currentLine().getLength());
    observer.updateCaret(currentCursor);
  }

  public void back() {
    if (isLineHead()) {
      boolean isDocHead = isBufferHead();
      previous();
      if (!isDocHead) {
        last();
      }
      return;
    }
    currentCursor.decCol(1);
    observer.updateCaret(currentCursor);
  }

  public void forward() {
    if (isLineLast()) {
      boolean isDocLast = isBufferLast();
      next();
      if (!isDocLast) {
        head();
      }
      return;
    }
    currentCursor.incCol(1);
    observer.updateCaret(currentCursor);
  }

  public void previous() {
    if (isBufferHead()) {
      return;
    }
    currentCursor.decRow(1);
    if (currentCursor.getCol() > currentLine().getLength()) {
      last();
    }
    observer.updateCaret(currentCursor);
  }

  public void next() {
    if (isBufferLast()) {
      return;
    }
    currentCursor.incRow(1);
    if (currentCursor.getCol() > currentLine().getLength()) {
      last();
    }
    observer.updateCaret(currentCursor);
  }

  public void bufferHead() {
    currentCursor.setRow(0);
    currentCursor.setCol(0);
    observer.updateCaret(currentCursor);
  }

  public void bufferLast() {
    currentCursor.setRow(lines.size() - 1);
    currentCursor.setCol(currentLine().getLength());
    observer.updateCaret(currentCursor);
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
    return currentCursor.getCol() == currentLine().getLength();
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
