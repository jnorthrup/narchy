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


        var operatingStack = new OperatingStack();


        while (!nodeStack.isEmpty()) {
            var node = nodeStack.removeLast();
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
        var shuntingStack = new ShuntingStack();
        Consumer<Node> c = n -> n.accept(shuntingStack);
        for (var node : nodeList) {
            c.accept(node);
        }
        shuntingStack.finish(nodeStack);
    }

    private void normalize() {
        var index = 0;
        var r = this.regex;
        var len = r.length();
        while (index < len) {
            var ch = r.charAt(index++);
            var nodeList = this.nodeList;
            switch (ch) {
                case '[':
                    tryConcat();
                    boolean isComplementarySet;
                    if (r.charAt(index) == '^') {
                        isComplementarySet = true;
                        index++;
                    } else isComplementarySet = false;
                    List<Character> all = new FasterList<>();
                    for (var next = r.charAt(index++); next != ']'; next = r.charAt(index++)) {
                        if (next == '\\' || next == '.') {
                            String token;
                            if (next == '\\') {
                                var nextNext = r.charAt(index++);
                                token = new String(new char[]{'\\', nextNext});
                            } else token = String.valueOf(next);
                            var tokenSet = CommonSets.interpretToken(token);
                            all.addAll(tokenSet);
                        } else all.add(next);
                    }
                    var chSet = CommonSets.minimum(CommonSets.listToArray(all));
                    if (isComplementarySet) {
                        chSet = CommonSets.complementarySet(chSet);
                    }
                    nodeList.add(LeftBracket.the);
                    for (var i = 0; i < chSet.length; i++) {
                        nodeList.add(new LChar(chSet[i]));
                        if (i == chSet.length - 1 || chSet[i + 1] == 0) break;
                        nodeList.add(new BOr());
                    }
                    nodeList.add(RightBracket.the);
                    itemTerminated = true;
                    break;
                case '{':
                    var deterministicLength = false;
                    var sb = new StringBuilder();
                    label:
                    for (var next = r.charAt(index++); ; ) {
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
                    var least = Texts.i(sb.toString());


                    var most = -1;
                    if (!deterministicLength) {
                        var next = r.charAt(index);
                        if (next != '}') {
                            sb = new StringBuilder();
                            for (var nextNext = r.charAt(index++); nextNext != '}'; nextNext = r.charAt(index++)) {
                                sb.append(nextNext);
                            }
                            if (sb.length() != 0) {
                                most = Texts.i(sb.toString());
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
                    if (ch == '\\' || ch == '.') {
                        String token;
                        if (ch == '\\') {
                            var next = r.charAt(index++);
                            token = new String(new char[]{'\\', next});
                        } else token = String.valueOf(ch);
                        var tokenSet = CommonSets.interpretToken(token);
                        nodeList.add(LeftBracket.the);
                        nodeList.add(new LChar(tokenSet.get(0)));
                        for (var i = 1; i < tokenSet.size(); i++) {
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
            var nodeList = this.nodeList;
            if (least == 0 && most == -1) {
                nodeList.add(new BMany());
                nodeList.add(new LNull());
            } else {
                List<Node> sample;
                if (last() instanceof RightBracket) {
                    sample = new LinkedList<>();
                    sample.add(nodeList.remove(nodeList.size() - 1));
                    var stack = 1;
                    for (var i = nodeList.size() - 1; i >= 0; i--) {
                        var node = nodeList.remove(i);
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
                    for (var i = 0; i < least; i++) {
                        nodeList.addAll(copyNodes(sample));
                        nodeList.add(new BConcat());
                    }
                    nodeList.addAll(copyNodes(sample));
                    nodeList.add(new BMany());
                    nodeList.add(new LNull());
                } else {
                    if (least != most) {
                        nodeList.add(LeftBracket.the);
                        for (var i = least; i <= most; i++) {
                            nodeList.add(LeftBracket.the);
                            if (i == 0) {
                                nodeList.add(LClosure.the);
                            } else {
                                for (var j = 0; j < i; j++) {
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
                        for (var i = 0; i < least; i++) {
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
        for (var node : sample) {
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
