//package spacegraph.obj;
//
//import com.googlecode.lanterna.TerminalTextUtils;
//import com.googlecode.lanterna.input.KeyStroke;
//import com.googlecode.lanterna.input.KeyType;
//import com.jogamp.newt.event.KeyEvent;
//import com.jogamp.newt.event.KeyListener;
//import com.jogamp.opengl.GL2;
//import nars.$;
//import nars.gui.ConceptWidget;
//import nars.nar.Default;
//import spacegraph.*;
//import spacegraph.phys.collision.shapes.BoxShape;
//import spacegraph.phys.collision.shapes.CollisionShape;
//import spacegraph.phys.collision.shapes.ConvexInternalShape;
//import spacegraph.phys.dynamics.RigidBody;
//
//import javax.vecmath.Vector3f;
//import java.awt.event.InputEvent;
//import java.util.ArrayDeque;
//import java.util.List;
//import java.util.function.Function;
//
//import static javax.vecmath.Vector3f.v;
//
///**
// * Created by me on 7/9/16.
// */
//public class Physiconsole extends ListInput<Object, Spatial<Object>> implements Function<Object, Spatial<Object>> {
//
//
//    private final int capacity;
//    ArrayDeque<Object> buffer;
//
//    public boolean needsLayout;
//
//    public Physiconsole(int capacity) {
//        buffer = new ArrayDeque(capacity);
//        this.capacity = capacity;
//    }
//
////    public static class LineBlock extends Spatial {
////        public LineBlock() {
////
////        }
////    }
//
//    public void println(String line) {
//        append(line);
//    }
//
//    public void append(Object s) {
//        if (buffer.contains(s)) {
//            buffer.remove(s);
//        } else if (capacity == buffer.size()) {
//            Object popped = buffer.removeFirst();
//            space.getOrAdd(popped);
//        }
//        buffer.add(s);
//        Object[] bb = buffer.toArray(new Object[buffer.size()]);
//        commit(bb); //HACK todo optimize
//    }
//
//    @Override
//    public void commit(Object[] xx) {
//        super.commit(xx);
//        needsLayout = true;
//    }
//
//    @Override
//    public void start(SpaceGraph space) {
//        super.start(space);
//        space.with(new SpaceTransform() {
//
//            @Override
//            public void update(SpaceGraph g, List verts, float dt) {
//                if (needsLayout) {
//                    layout();
//                    needsLayout = false;
//                }
//            }
//        });
//    }
//
//    protected void layout() {
//        float x = 0, y = 0, z = 0;
//
//
//        float marginY = 0.5f;
//
//        for (Spatial v : active) {
//            RigidBody body = v.body;
//            if (body == null)
//                continue;
//
//            Vector3f vs = ((ConvexInternalShape) body.shape()).implicitShapeDimensions;
//            float r = 2f;
//            float width = vs.x * r;
//            float height = vs.y * r;
//            v.move(width / 2f, y, z);
//            y += height + marginY;
//        }
//    }
//
//
//    public static void main(String[] args) {
//
//        ConsoleSurface.EditTerminal edit = new ConsoleSurface.EditTerminal(40, 16);
//
//        Physiconsole p = new Physiconsole(9);
//        SpaceGraph<?> s = new SpaceGraph(p, p) {
//            @Override
//            public void init(GL2 gl) {
//                super.init(gl);
//
//                addKeyListener(new KeyListener() {
//
//                    @Override
//                    public void keyReleased(KeyEvent e) {
//                        if (!e.isPrintableKey() || e.isActionKey() || e.isModifierKey())
//                            return;
//
//                        char cc = e.getKeyChar();
//                        if (!TerminalTextUtils.isControlCharacter(cc)) {
//                            boolean altDown = (e.getModifiers() & KeyEvent.ALT_MASK) != 0;
//                            boolean ctrlDown = (e.getModifiers() & InputEvent.CTRL_MASK) != 0;
//
//                            KeyStroke next = new KeyStroke(cc,ctrlDown, altDown);
//                            if (next != null) {
//                                edit.gui.handleInput(next);
//                                edit.refresh();
//                            }
//                        }
//
//                    }
//
//                    @Override
//                    public void keyPressed(KeyEvent e) {
//
//
//                        //see: GraphicalTerminalImplementation.java
//
//                        KeyStroke next = null;
//                        boolean altDown = (e.getModifiers() & KeyEvent.ALT_MASK) != 0;
//                        boolean ctrlDown = (e.getModifiers() & InputEvent.CTRL_MASK) != 0;
//                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
//                            next = (new KeyStroke(KeyType.Enter, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
//                            next = (new KeyStroke(KeyType.Escape, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
//                            next = (new KeyStroke(KeyType.Backspace, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
//                            next = (new KeyStroke(KeyType.ArrowLeft, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
//                            next = (new KeyStroke(KeyType.ArrowRight, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
//                            next = (new KeyStroke(KeyType.ArrowUp, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
//                            next = (new KeyStroke(KeyType.ArrowDown, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_INSERT) {
//                            next = (new KeyStroke(KeyType.Insert, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
//                            next = (new KeyStroke(KeyType.Delete, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_HOME) {
//                            next = (new KeyStroke(KeyType.Home, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_END) {
//                            next = (new KeyStroke(KeyType.End, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
//                            next = (new KeyStroke(KeyType.PageUp, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
//                            next = (new KeyStroke(KeyType.PageDown, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_F1) {
//                            next = (new KeyStroke(KeyType.F1, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_F2) {
//                            next = (new KeyStroke(KeyType.F2, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_F3) {
//                            next = (new KeyStroke(KeyType.F3, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_F4) {
//                            next = (new KeyStroke(KeyType.F4, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_F5) {
//                            next = (new KeyStroke(KeyType.F5, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_F6) {
//                            next = (new KeyStroke(KeyType.F6, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_F7) {
//                            next = (new KeyStroke(KeyType.F7, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_F8) {
//                            next = (new KeyStroke(KeyType.F8, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_F9) {
//                            next = (new KeyStroke(KeyType.F9, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_F10) {
//                            next = (new KeyStroke(KeyType.F10, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_F11) {
//                            next = (new KeyStroke(KeyType.F11, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_F12) {
//                            next = (new KeyStroke(KeyType.F12, ctrlDown, altDown));
//                        } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
//                            if (e.isShiftDown()) {
//                                next = (new KeyStroke(KeyType.ReverseTab, ctrlDown, altDown));
//                            } else {
//                                next = (new KeyStroke(KeyType.Tab, ctrlDown, altDown));
//                            }
//                        } else {
//                            //keyTyped doesn't catch this scenario (for whatever reason...) so we have to do it here
//                            if (altDown && ctrlDown && e.getKeyCode() >= 'A' && e.getKeyCode() <= 'Z') {
//                                char asLowerCase = Character.toLowerCase((char) e.getKeyCode());
//                                next = (new KeyStroke(asLowerCase, true, true));
//                            }
//                        }
//
//                        if (next!=null) {
//                            //edit.input(next);
//                            edit.gui.handleInput(next);
//                            edit.refresh();
//                        }
//                    }
//                });
//            }
//        };
//
//
//        Default n = new Default();
//        n.input("<a --> b>.");
//        n.input("<b --> c>. :|:");
//        n.input("<c --> d>. :|:");
//        n.input("<c --> b>. :|:");
//        n.input("<?x <=> (b|d)>?");
//        n.onTask(t -> {
//            p.append(t.toString());
//        });
//
//
//        s.add(new Facial(new ConsoleSurface(edit)).scale(500f, 400f));
//        s.add(new Facial(new CrosshairSurface(s)));
//
//
//        n.loop(5f);
//        s.show(1200, 800);
//
//
//    }
//
//
//    @Override
//    protected void updateImpl() {
//
//    }
//
//    @Override
//    public long now() {
//        return 0;
//    }
//
//    @Override
//    public Spatial apply(Object x) {
//        String s = x.toString();
//
//        float cAspect = 2f;
//        float sx = s.length() / cAspect;
//        float sy = 1f;
//
//        ConceptWidget w = new ConceptWidget($.quote(s), 0, nar) {
//            protected CollisionShape newShape() {
//
//                return new BoxShape(v(sx, sy, 0.1f));
//            }
//
//            @Override
//            protected void colorshape(GL2 gl) {
//                gl.glColor3f(0.1f, 0.1f, 0.1f);
//            }
//        };
//
//
//        return w;
//    }
//}
//
