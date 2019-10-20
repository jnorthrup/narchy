package org.zhz.dfargx;

import org.zhz.dfargx.node.LNull;
import org.zhz.dfargx.node.Node;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Supplier;

import static java.lang.System.out;

/**
 * Created on 2015/5/9.
 */
public class TreePrinter {

    private static final TreePrinter instance = new TreePrinter();

    public static TreePrinter getInstance() {
        return instance;
    }

    public static void printTreeCopied(Node root) {
        printTree(copyTree(root));
    }

    public static void printTree(Node root) {
        var h = calculateDepth(root, 0);
        complete(root, 0, h, LNull::new);
        Queue<Node> q = new ArrayDeque<>();
        q.offer(root);
        var t = 1;
        var j = 0;
        while (!q.isEmpty()) {
            var node = q.poll();
            var width = 4 * (int) Math.pow(2, h - t);
            if (node instanceof LNull) {
                for (var i = 0; i < width * 2; i++) {
                    out.print(' ');
                }
            } else {
                var s = node.toString();
                for (var i = 0; i < width; i++) {
                    if (!(node.left() instanceof LNull)) {
                        if (i < width / 2 || t == h) {
                            out.print(' ');
                        } else if (i == width / 2) {
                            out.print('|');
                        } else out.print('-');
                    } else out.print(' ');
                }
                out.print(s);
                for (var i = s.length(); i < width; i++) {
                    if (!(node.right() instanceof LNull)) {
                        if (i < width / 2 && t != h) {
                            out.print('-');
                        } else if (i == width / 2 && t != h) {
                            out.print('|');
                        } else out.print(' ');
                    } else out.print(' ');
                }
            }
            if (++j == Math.pow(2, t) - 1) {
                out.println();
                t += 1;
            }
            node.ifHasLeft(q::offer);
            node.ifHasRight(q::offer);
        }
    }

    private static void complete(Node node, int k, int h) {
        complete(node, k, h, LNull::new);
    }

    private static void complete(Node node, int k, int h, Supplier<Node> fillEmptyWith) {
        if (k + 1 < h) {
            node.ifEmptyLeft(fillEmptyWith);
            complete(node.left(), k + 1, h, fillEmptyWith);
            node.ifEmptyRight(fillEmptyWith);
            complete(node.right(), k + 1, h, fillEmptyWith);
        }
    }

    private static int calculateDepth(Node node, int depth) {
        depth += 1;
        int l = depth, r = depth;
        if (node.hasLeft()) {
            l = calculateDepth(node.left(), depth);
        }
        if (node.hasRight()) {
            r = calculateDepth(node.right(), depth);
        }
        return l > r ? l : r > depth ? r : depth;
    }

    private static Node copyTree(Node root) {
        var newRoot = root.copy();
        if (root.hasLeft()) {
            newRoot.setLeft(copyTree(root.left()));
        }
        if (root.hasRight()) {
            newRoot.setRight(copyTree(root.right()));
        }
        return newRoot;
    }
}
