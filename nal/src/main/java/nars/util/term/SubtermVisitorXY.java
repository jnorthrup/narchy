//package nars.term.visit;
//
//import jcog.list.FasterList;
//import nars.term.Compound;
//import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import static nars.term.Terms.compoundOrNull;
//import static nars.term.visit.SubtermVisitorXY.Next.End;
//
///**
// * detailed visitor pattern
// */
//abstract public class SubtermVisitorXY {
//
//    @NotNull final FasterList<Compound> pathy = new FasterList(8);
//    @NotNull final ByteArrayList pathx = new ByteArrayList(8);
//
//    @NotNull public final Compound root;
//
//    @Nullable Compound cursorY;
//    byte cursorX;
//
//    protected SubtermVisitorXY(@NotNull Compound root) {
//        this.cursorY = this.root = root;
//        this.cursorX = 0;
//
//        Next next;
//        while ((next = accept(cursorX, cursorY, pathy.size() ))!=End) {
//            switch (next) {
//                case Left:
//                    cursorX--;
//                    if (cursorX < 0) throw new UnsupportedOperationException();
//                    break;
//                case Right:
//                    cursorX++;
//                    if (cursorX >= cursorY.size()) throw new UnsupportedOperationException();
//                    break;
//                case Up:
//                    up();
//                    break;
//                case Down:
//                    Compound ny = compoundOrNull(cursorY.sub(cursorX));
//                    if (ny ==null)
//                        throw new UnsupportedOperationException();
//                    pathy.add(cursorY);
//                    cursorY = ny;
//                    pathx.add(cursorX);
//                    cursorX = 0;
//                    break;
//                case Next:
//                    if (cursorX + 1 >= cursorY.size()) {
//                        if (pathy.isEmpty())
//                            return; //finished
//
//                        up();
//                    }
//                    cursorX++;
//
//                    break;
//            }
//        }
//
//
//    }
//
//    public void up() {
//        if ((cursorY = pathy.removeLastElseNull()) == null)
//            throw new UnsupportedOperationException();
//
//        cursorX = pathx.removeAtIndex(pathx.size()-1);
//    }
//
//    public enum Next {
//        Left,
//        Right,
//        Up,
//        Down,
//        Next,
//        End
//    }
//
//
//    @NotNull
//    public abstract Next accept(int subterm, @NotNull Compound superterm, int depth);
//
//}
