package org.zhz.dfargx;

import jcog.Texts;
import jcog.data.list.FasterList;
import org.zhz.dfargx.node.*;
import org.zhz.dfargx.node.bracket.LeftBracket;
import org.zhz.dfargx.node.bracket.RightBracket;
import org.zhz.dfargx.stack.OperatingStack;
import org.zhz.dfargx.stack.ShuntingStack;
import org.zhz.dfargx.util.CommonSets;
import org.zhz.dfargx.util.InvalidSyntaxException;

import java.util.Collections;
import java.util.EmptyStackException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created on 2015/5/8.
 */
public class SyntaxTree {
    private final String regex;
    private final FasterList<Node> nodeList = new FasterList();
    private final FasterList<Node> nodeStack = new FasterList();

    private final Node root;

    private boolean itemTerminated;


    public SyntaxTree(String regex) {
        this.regex = regex;
        itemTerminated = false;
        normalize();

        shunt();


        OperatingStack operatingStack = new OperatingStack();


        while (!nodeStack.isEmpty()) {
            Node node = nodeStack.removeLast();
            node.accept(operatingStack);
        }
        try {
            root = operatingStack.pop();
        } catch (EmptyStackException e) {
            throw new InvalidSyntaxException(e);
        }
        if (!operatingStack.isEmpty()) {
            throw new InvalidSyntaxException();
        }
    }

    private void shunt() {
        ShuntingStack shuntingStack = new ShuntingStack();
        Consumer<Node> c = new Consumer<Node>() {
            @Override
            public void accept(Node n) {
                n.accept(shuntingStack);
            }
        };
        for (Node node : nodeList) {
            c.accept(node);
        }
        shuntingStack.finish(nodeStack);
    }

    private void normalize() {
        int index = 0;
        String r = this.regex;
        int len = r.length();
        while (index < len) {
            char ch = r.charAt(index++);
            FasterList<Node> nodeList = this.nodeList;
            switch (ch) {
                case '[':
                    tryConcat();
                    boolean isComplementarySet;
                    if ((int) r.charAt(index) == (int) '^') {
                        isComplementarySet = true;
                        index++;
                    } else isComplementarySet = false;
                    List<Character> all = new FasterList<>();
                    for (char next = r.charAt(index++); (int) next != (int) ']'; next = r.charAt(index++)) {
                        if ((int) next == (int) '\\' || (int) next == (int) '.') {
                            String token;
                            if ((int) next == (int) '\\') {
                                char nextNext = r.charAt(index++);
                                token = new String(new char[]{'\\', nextNext});
                            } else token = String.valueOf(next);
                            List<Character> tokenSet = CommonSets.interpretToken(token);
                            all.addAll(tokenSet);
                        } else all.add(next);
                    }
                    char[] chSet = CommonSets.minimum(CommonSets.listToArray(all));
                    if (isComplementarySet) {
                        chSet = CommonSets.complementarySet(chSet);
                    }
                    nodeList.add(LeftBracket.the);
                    for (int i = 0; i < chSet.length; i++) {
                        nodeList.add(new LChar(chSet[i]));
                        if (i == chSet.length - 1 || (int) chSet[i + 1] == 0) break;
                        nodeList.add(new BOr());
                    }
                    nodeList.add(RightBracket.the);
                    itemTerminated = true;
                    break;
                case '{':
                    boolean deterministicLength = false;
                    StringBuilder sb = new StringBuilder();
                    label:
                    for (char next = r.charAt(index++); ; ) {
                        sb.append(next);
                        next = r.charAt(index++);
                        switch (next) {
                            case '}':
                                deterministicLength = true;
                                break label;
                            case ',':
                                break label;
                        }
                    }
                    int least = Texts.INSTANCE.i(sb.toString());


                    int most = -1;
                    if (!deterministicLength) {
                        char next = r.charAt(index);
                        if ((int) next != (int) '}') {
                            sb = new StringBuilder();
                            for (char nextNext = r.charAt(index++); (int) nextNext != (int) '}'; nextNext = r.charAt(index++)) {
                                sb.append(nextNext);
                            }
                            if (sb.length() != 0) {
                                most = Texts.INSTANCE.i(sb.toString());
                            }
                        }
                    } else most = least;

                    performMany(least, most);
                    itemTerminated = true;
                    break;
                case '(':
                    tryConcat();
                    nodeList.add(LeftBracket.the);
                    itemTerminated = false;
                    break;
                case ')':
                    nodeList.add(RightBracket.the);
                    itemTerminated = true;
                    break;
                case '*':
                    performMany(0, -1);
                    itemTerminated = true;
                    break;
                case '?':
                    performMany(0, 1);
                    itemTerminated = true;
                    break;
                case '+':
                    performMany(1, -1);
                    itemTerminated = true;
                    break;
                case '|':
                    nodeList.add(new BOr());
                    itemTerminated = false;
                    break;
                default:
                    tryConcat();
                    if ((int) ch == (int) '\\' || (int) ch == (int) '.') {
                        String token;
                        if ((int) ch == (int) '\\') {
                            char next = r.charAt(index++);
                            token = new String(new char[]{'\\', next});
                        } else token = String.valueOf(ch);
                        List<Character> tokenSet = CommonSets.interpretToken(token);
                        nodeList.add(LeftBracket.the);
                        nodeList.add(new LChar(tokenSet.get(0)));
                        for (int i = 1; i < tokenSet.size(); i++) {
                            nodeList.add(new BOr());
                            nodeList.add(new LChar(tokenSet.get(i)));
                        }
                        nodeList.add(RightBracket.the);
                    } else nodeList.add(new LChar(ch));

                    itemTerminated = true;
                    break;
            }
        }
    }

    
    private void performMany(int least, int most) {
        if (!(least == 1 && most == 1)) {
            FasterList<Node> nodeList = this.nodeList;
            if (least == 0 && most == -1) {
                nodeList.add(new BMany());
                nodeList.add(new LNull());
            } else {
                List<Node> sample;
                if (last() instanceof RightBracket) {
                    sample = new LinkedList<>();
                    sample.add(nodeList.remove(nodeList.size() - 1));
                    int stack = 1;
                    for (int i = nodeList.size() - 1; i >= 0; i--) {
                        Node node = nodeList.remove(i);
                        if (node instanceof RightBracket) {
                            stack++;
                        } else if (node instanceof LeftBracket) {
                            stack--;
                        }
                        sample.add(0, node);
                        if (stack == 0) {
                            break;
                        }
                    }
                } else sample = Collections.singletonList(nodeList.remove(nodeList.size() - 1));

                if (most == -1) {
                    for (int i = 0; i < least; i++) {
                        nodeList.addAll(copyNodes(sample));
                        nodeList.add(new BConcat());
                    }
                    nodeList.addAll(copyNodes(sample));
                    nodeList.add(new BMany());
                    nodeList.add(new LNull());
                } else {
                    if (least != most) {
                        nodeList.add(LeftBracket.the);
                        for (int i = least; i <= most; i++) {
                            nodeList.add(LeftBracket.the);
                            if (i == 0) {
                                nodeList.add(LClosure.the);
                            } else {
                                for (int j = 0; j < i; j++) {
                                    nodeList.addAll(copyNodes(sample));
                                    if (j != i - 1) {
                                        nodeList.add(new BConcat());
                                    }
                                }
                            }
                            nodeList.add(RightBracket.the);
                            if (i != most) {
                                nodeList.add(new BOr());
                            }
                        }
                        nodeList.add(RightBracket.the);
                    } else {
                        nodeList.add(LeftBracket.the);
                        for (int i = 0; i < least; i++) {
                            nodeList.addAll(copyNodes(sample));
                            if (i != least - 1) {
                                nodeList.add(new BConcat());
                            }
                        }
                        nodeList.add(RightBracket.the);
                    }
                }
            }
        }
    }

    public static List<Node> copyNodes(List<Node> sample) {
        List<Node> result = new FasterList(sample.size());
        for (Node node : sample) {
            result.add(node.copy());
        }
        return result;
    }

    private Node last() {
        return nodeList.getLast();
    }


    private void tryConcat() {
        if (itemTerminated) {
            nodeList.add(new BConcat());
            itemTerminated = false;
        }
    }

    public Node getRoot() {
        return root;
    }

}
