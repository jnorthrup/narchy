/*
 * Copyright (C) 2015 Machine Learning Lab - University of Trieste, 
 * Italy (http:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:
 */
package jcog.grammar.evolve.variations;

import jcog.grammar.evolve.configuration.EvolutionParameters;
import jcog.grammar.evolve.generations.Generation;
import jcog.grammar.evolve.generations.Growth;
import jcog.grammar.evolve.inputs.Context;
import jcog.grammar.evolve.tree.Leaf;
import jcog.grammar.evolve.tree.Node;
import jcog.grammar.evolve.tree.ParentNode;
import jcog.grammar.evolve.tree.operator.Group;
import jcog.grammar.evolve.tree.operator.NonCapturingGroup;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author MaleLabTs
 */
public class Variation {

    private final Context context;
    private final Generation growth;

    public Variation(Context context) {
        this.context = context;
        this.growth = new Growth(5, context);
    }

    /**
     * This method performs the crossover operation on two individuals
     *
     * @param individualA the first individual to apply the crossover
     * @param individualB the second individual to apply the crossover
     * @return two new individuals
     */
    public Twin<Node> crossover(Node individualA, Node individualB, int maxTries) {
        var isGood = false;
        Node newIndividualA = null;
        Node newIndividualB = null;

        for (var tries = 0; tries < maxTries; tries++) {

            newIndividualA = individualA.cloneTree();
            newIndividualB = individualB.cloneTree();

            var randomNodeA = pickRandomNode(newIndividualA);
            var randomNodeB = pickRandomNode(newIndividualB);

            if (randomNodeA != null && randomNodeB != null) {

                var aParent = randomNodeA.getParent();
                var aChilds = aParent.children();
                var aIndex = aChilds.indexOf(randomNodeA);
                var bParent = randomNodeB.getParent();
                var bChilds = bParent.children();
                var bIndex = bChilds.indexOf(randomNodeB);
                aParent.set(aIndex, randomNodeB);
                bParent.set(bIndex, randomNodeA);
                randomNodeA.setParent(bParent);
                randomNodeB.setParent(aParent);

                 
                if (checkMaxDepth(newIndividualA, 1)
                        && checkMaxDepth(newIndividualB, 1)
                        && newIndividualA.isValid()
                        && newIndividualB.isValid()) {
                    /*newIndividualA=normalizeGroup(newIndividualA);
                    newIndividualB=normalizeGroup(newIndividualB);*/
                    isGood = true;
                    break;
                }

            }

        }


        if (isGood) {
            return Tuples.twin(newIndividualA, newIndividualB);
        } else {
            return null;

        }
    }

    /**
     * This method apply a mutation on an individual
     *
     * @param individual the indidual on which aplly the mutation
     * @return a new mutated individual
     */
    public Node mutate(Node individual) {

        var newNodes = this.growth.generate(20);
        var mutant = individual.cloneTree();


        for (var newNode : newNodes) {

            var randomNode = pickRandomNode(mutant);
            if (randomNode != null) {
                replaceNode(randomNode, newNode);
                if (checkMaxDepth(mutant, 1) && mutant.isValid()) {
                    break;
                }
            }
            mutant = individual.cloneTree();

        }

        return mutant;
    }

    private Node pickRandomNode(Node individual) {
        var param = context.getConfiguration().getEvolutionParameters();
        List<Node> nodeList = new ArrayList<>();

        var random = this.context.getRandom().nextFloat();

        if (random <= param.getNodeCrossoverSelectionProbability()) {
            enlistNode(individual, nodeList, false);
        } else if (random <= param.getNodeCrossoverSelectionProbability() + param.getLeafCrossoverSelectionProbability()) {
            enlistNode(individual, nodeList, true);
        } else {
            nodeList.add(individual);
        }

        
        if (nodeList.isEmpty()) {
            enlistNode(individual, nodeList, true);
        }

        if (nodeList.isEmpty()) {
            return null;
        }
        var randomIndex = this.context.getRandom().nextInt(nodeList.size());
        return nodeList.get(randomIndex);
    }

    private static void enlistNode(Node root, List<Node> nodes, boolean isLeaf) {

        if (isNodePickable(root, isLeaf)) {
            nodes.add(root);

        }
        for (var child : root.children()) {
            enlistNode(child, nodes, isLeaf);
        }


    }

    private static boolean isNodePickable(Node root, boolean isLeaf) {
        return root instanceof Leaf == isLeaf
                && root.getParent() != null;
    }

    private static void replaceNode(Node oldChild, Node newChild) {

        var parent = oldChild.getParent();
        var childs = parent.children();
        var index = childs.indexOf(oldChild);
        newChild.setParent(parent);
        oldChild.setParent(null);
        parent.set(index, newChild);
    }

    private static void swapNodes(Node a, Node b) {

        var aParent = a.getParent();
        var aChilds = aParent.children();
        var aIndex = aChilds.indexOf(a);
        var bParent = b.getParent();
        var bChilds = bParent.children();
        var bIndex = bChilds.indexOf(b);
        aParent.set(aIndex, b);
        bParent.set(bIndex, a);
        a.setParent(bParent);
        b.setParent(aParent);
    }

    private boolean checkMaxDepth(Node root, int depth) {
        if (depth > context.getConfiguration().getEvolutionParameters().getMaxDepthAfterCrossover()) {
            return false;
        }
        if (root instanceof Leaf) {
            return true;
        }

        var acc = true;
        for (var child : root.children()) {
            var checkMaxDepth = checkMaxDepth(child, depth + 1);
            acc = acc && checkMaxDepth;
        }
        var ret = acc;

        return ret;
    }

    private static void checkSingleGroup(Node root, List<Group> groups) {

        if (root instanceof Group) {
            groups.add((Group) root);
        }
        for (var child : root.children()) {
            checkSingleGroup(child, groups);
        }

    }

    private Node normalizeGroup(Node root) {
        List<Group> groups = new LinkedList<>();
        checkSingleGroup(root, groups);

        if (groups.size() < 2) {
            return root;
        }
        var nextInt = this.context.getRandom().nextInt(groups.size());

        groups.remove(nextInt);

        for (var group : groups) {
            var ncg = new NonCapturingGroup();
            if (group != root) {
                ncg.setParent(group.getParent());
                var indexOf = ncg.getParent().children().indexOf(group);
                ncg.getParent().children().set(indexOf, ncg);

            } else {
                root = ncg;
            }
            ncg.addAll(group.children());
            ncg.children().get(0).setParent(ncg);
        }

        return root;
    }
}
