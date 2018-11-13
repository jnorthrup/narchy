package mcaixictw.worldmodels;

import mcaixictw.BooleanArrayList;

import java.util.LinkedList;

/**
 * A bit context tree. Learns the next bit based on context which is of the same
 * length as the tree.
 */
public class ContextTree extends Worldmodel {


    protected ContextTree(String name, int depth) {
        super(name);
        history = new BooleanArrayList();
        root = new CTNode();
        this.depth = depth;
    }

    protected final BooleanArrayList history;
    protected CTNode root;
    protected int depth;

    /**
     * recursively traverses the tree and returns a string in human readable
     * format (for debug purposes)
     */
    public String toString() {
        String result = "";
        result += "ContextTree" + '\n';
        result += "name: " + name + '\n';
        result += "depth: " + depth + '\n';
        result += "history size: " + history.size() + '\n';
        return result;
    }

    public String prettyPrint() {
        String result = "";
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                boolean b1 = i != 0;
                boolean b2 = j != 0;

                BooleanArrayList symbols = new BooleanArrayList();
                symbols.add(b1);
                symbols.add(b2);

                result += "P(" + b2 + b1 + "|h) = " + predict(symbols) + '\n';
            }
        }
        if (root != null) {
            int nodesPerLevel = 1;
            int nodesPerNextLevel = 0;
            int nodesProcessed = 0;
            LinkedList<CTNode> bfsQueue = new LinkedList<>();
            bfsQueue.add(root);
            while (!bfsQueue.isEmpty()) {
                CTNode currNode = bfsQueue.removeFirst();
                result += currNode + " ";
                nodesProcessed++;
                if (currNode.child(false) != null) {
                    bfsQueue.addLast(currNode.child(false));
                    nodesPerNextLevel++;
                }
                if (currNode.child(true) != null) {
                    bfsQueue.addLast(currNode.child(true));
                    nodesPerNextLevel++;
                }
                if (nodesProcessed == nodesPerLevel) {
                    result += "\n\n";
                    nodesProcessed = 0;
                    nodesPerLevel = nodesPerNextLevel;
                    nodesPerNextLevel = 0;
                }
            }
        }
        return result;
    }

    protected void add(boolean sym, BooleanArrayList underlyingHistory) {
        CTNode currNode = root;


        double logKTMul = currNode.logKTMul(sym);
        currNode.logPest += logKTMul;
        assert (!Double.isNaN(currNode.logPest));

        currNode.incrCount(sym);


        int d = Math.max(underlyingHistory.size() - depth, 0);

        for (int i = underlyingHistory.size() - 1; i >= d; i--) {

            boolean currSymbol = underlyingHistory.get(i);

            CTNode prevNode = currNode;
            currNode = currNode.child(currSymbol);
            if (currNode == null)
                prevNode.setChild(currSymbol, currNode = new CTNode());


            currNode.logPest += currNode.logKTMul(sym);
            assert (!Double.isNaN(currNode.logPest));
            currNode.incrCount(sym);
        }
        updateProbabilities(currNode);
    }


    protected void remove(boolean sym, BooleanArrayList underlyingHistory) {

        CTNode currNode = root;

        assert (currNode.count(sym) > 0);
        currNode.decrCount(sym);
        // improve numerical stability
        currNode.logPest = currNode.count(false) + currNode.count(true) == 0 ? 0.0
                : currNode.logPest - currNode.logKTMul(sym);
        assert (!Double.isNaN(currNode.logPest));

        // Update all nodes with that context

        int d = Math.max(underlyingHistory.size() - depth, 0);

        for (int i = underlyingHistory.size() - 1; i >= d; i--) {

            boolean currSymbol = underlyingHistory.get(i);

            currNode = currNode.child(currSymbol);
            assert (currNode != null);

            // Update the node
            assert (currNode.count(sym) > 0);
            currNode.decrCount(sym);
            if ((currNode.count(false) + currNode.count(true)) == 0) {
                // Delete the current node, can't be the root node which was
                // processed above.
                currNode = currNode.parent;
                currNode.setChild(currSymbol, null);
                break;
            } else {
                currNode.logPest -= currNode.logKTMul(sym);
                assert (!Double.isNaN(currNode.logPest));
            }
        }
        updateProbabilities(currNode);
    }

    protected static void updateProbabilities(CTNode currNode) {


        if (currNode.child(false) == null && currNode.child(true) == null) {
            currNode.logPweight = currNode.logPest;
            currNode = currNode.parent;
        }

        double[] pChildW = new double[2];

        while (currNode != null) {

            CTNode f = currNode.child(false);
            CTNode t = currNode.child(true);
            pChildW[0] = f != null ? f.logPweight : 0.0;
            pChildW[1] = t != null ? t.logPweight : 0.0;


            double log_one_plus_exp = pChildW[0] + pChildW[1]
                    - currNode.logPest;

            if (log_one_plus_exp < 80.0) {
                log_one_plus_exp = Math.log(1.0 + Math.exp(log_one_plus_exp));
            }

            currNode.logPweight = Math.log(0.5) + currNode.logPest
                    + log_one_plus_exp;


            assert (!(Double.isNaN(currNode.logPweight) || Double
                    .isInfinite(currNode.logPweight)));


            currNode = currNode.parent;
        }
    }

//    /**
//     * either add or remove a symbol from the tree.
//     *
//     * @param sym
//     * @param addSymbol
//     */
//    @Deprecated
//    protected void change(boolean sym, boolean addSymbol,
//                          Stack<Boolean> underlyingHistory) {
//
//        CTNode currNode = root;
//
//
//        if (addSymbol) {
//            currNode.logPest += currNode.logKTMul(sym);
//            currNode.incrCount(sym);
//
//        } else {
//            assert (currNode.count(sym) > 0);
//            currNode.decrCount(sym);
//
//            if ((currNode.count(false) + currNode.count(true)) == 0) {
//                currNode.logPest = 0.0;
//            } else {
//                currNode.logPest -= currNode.logKTMul(sym);
//            }
//
//        }
//
//
//        int historySize = underlyingHistory.size();
//        int d = Math.max(historySize - depth, 0);
//
//        for (int i = historySize - 1; i >= d; i--) {
//
//            boolean currSymbol = underlyingHistory.get(i);
//
//            CTNode prevNode = currNode;
//            currNode = currNode.child(currSymbol);
//            if (currNode == null) {
//                assert (addSymbol == true);
//                prevNode.setChild(currSymbol, currNode = new CTNode());
//            }
//
//
//            if (addSymbol) {
//                currNode.logPest += currNode.logKTMul(sym);
//                currNode.incrCount(sym);
//            } else {
//                assert (currNode.count(sym) > 0);
//                currNode.decrCount(sym);
//
//
//                if ((currNode.count(false) + currNode.count(true)) == 0) {
//                    (currNode = currNode.parent).setChild(currSymbol, null);
//                    break;
//                } else {
//                    currNode.logPest -= currNode.logKTMul(sym);
//                }
//            }
//
//        }
//
//
//        if (currNode.child(false) == null && currNode.child(true) == null) {
//            currNode.logPweight = currNode.logPest;
//            currNode = currNode.parent;
//        }
//
//        double[] pChild_w = new double[2];
//
//        while (currNode != null) {
//            for (int c = 0; c < 2; c++) {
//                boolean cBool = c == 1;
//                CTNode cc = currNode.child(cBool);
//                pChild_w[c] = (cc != null) ? cc.logPweight : 0;
//            }
//
//
//            double p_x = pChild_w[0] + pChild_w[1] - currNode.logPweight;
//
//
//            double p_w = p_x + Math.log(0.5) + currNode.logPweight;
//
////			if (Util.DebugOutput) {
////				System.out
////						.println("currNode.log_prob_weighted: "
////								+ currNode.logPweight + " p_x: " + p_x
////								+ " p_w: " + p_w);
////			}
//
//            assert (!addSymbol || currNode.logPweight >= p_w);
//            assert (addSymbol || currNode.logPweight <= p_w);
//            currNode.logPweight = p_w;
//
//
//            currNode = currNode.parent;
//        }
//    }

    /**
     * update the model with a symbol.
     *
     * @param sym
     */
    protected void update(boolean sym) {
        add(sym, history);
        history.add(sym);
    }

    /**
     * most recent symbol has the highest index.
     *
     * @param symlist
     */
    @Override
    public void update(BooleanArrayList symlist) {


        symlist.forEach(this::update);
    }

    /**
     * updates the history statistics, without touching the context tree
     * <p>
     * most recent symbol has the highest index.
     *
     * @param symlist
     */
    @Override
    public void updateHistory(BooleanArrayList symlist) {

        history.addAll(symlist);
    }

    /**
     * removes the most recently observed symbol from the context tree
     */
    public void revert() {


        boolean sym = history.pop();
        remove(sym, history);
    }

    @Override
    public void revert(int numSymbols) {


        for (int i = 0; i < numSymbols; i++) {
            revert();
        }


    }

    /**
     * shrinks the history down to a former size
     *
     * @param newsize
     */
    @Override
    public void revertHistory(int newsize) {

        assert (newsize <= history.size());


        int toRemove = history.size() - newsize;
        history.popFast(toRemove);


    }

    /**
     * generate a specified number of random symbols distributed according to
     * the context tree statistics
     *
     * @param bits
     * @return
     */
    @Override
    public BooleanArrayList genRandomSymbols(int bits) {
        BooleanArrayList result = genRandomSymbolsAndUpdate(bits);

        revert(bits);
        return result;
    }

    double eps = 1E-8;

    /**
     * generate a specified number of random symbols distributed according to
     * the context tree statistics and update the context tree with the newly
     * generated bits. last predicted symbol has the highest index.
     */
    @Override
    public BooleanArrayList genRandomSymbolsAndUpdate(int bits) {
        BooleanArrayList result = new BooleanArrayList(bits);
        for (int i = 0; i < bits; i++) {
            boolean sampledSymbol = false;
            if (Math.random() <= predict(new BooleanArrayList(true))) {
                sampledSymbol = true;
            }
            result.add(sampledSymbol);
            update(sampledSymbol);
        }
        return result;
    }

    /**
     * the estimated probability of observing a particular sequence
     *
     * @param symbols
     * @return
     */
    @Override
    public double predict(BooleanArrayList symbols) {

        double logProbBefore = logBlockProbability();


        symbols.forEach(this::update);

        double logProbAfter = logBlockProbability();
        assert (logProbAfter <= logProbBefore);


        revert(symbols.size());


        double logProbSym = logProbAfter - logProbBefore;

        double result = Math.exp(logProbSym);
        assert (result >= 0.0 && result <= 1.0);

        return result;
    }

    /**
     * the logarithm of the block probability of the whole sequence
     *
     * @return
     */
    public double logBlockProbability() {
        double logProb = root.logPweight;
        assert (!Double.isNaN(logProb) && !Double.isInfinite(logProb));
        assert (Math.exp(logProb) >= 0.0 && Math.exp(logProb) <= 1.0);
        return logProb;
    }

    /**
     * get the n'th most recent history symbol, n == 0 returns the most recent
     * symbol.
     *
     * @param n
     * @return
     */
    @Override
    public boolean nthHistorySymbol(int n) {


        return history.get(history.size() - (n + 1));
    }

    @Override
    public int historySize() {
        return history.size();
    }

    public int size() {
        return root.size();
    }

    public int depth() {
        return this.depth;
    }


    public CTNode getRoot() {
        return root;
    }

    public void setRoot(CTNode root) {
        this.root = root;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * returns the node corresponding to the given context. returns null if the
     * node does not exist.
     *
     * @param symbols
     * @return
     */
    public CTNode getNode(BooleanArrayList symbols) {
        CTNode currNode = root;
        for (int i = 0; i < symbols.size(); i++) {
            boolean currSym = symbols.get(i);
            currNode = currSym ? currNode.getChild1() : currNode.getChild0();
            if (currNode == null) {
                return null;
            }
        }
        return currNode;
    }
}
