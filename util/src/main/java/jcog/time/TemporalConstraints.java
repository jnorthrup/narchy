/**
 * This class implements allen's path consistency algorithm for reasoning on temporal interval relationships. Find more information in his paper:
 * Allen, J. F. Maintaining Knowledge about Temporal Intervals Communications of the ACM, 1983, 26, 832-843
 * <p>
 * As Allen points out: the path consistency algorithm is not complete. However, this is not so important in practice.
 * A paper analyzing the completeness of the path consistency algorithm can be found here:
 * Nebel, B. & Bürckert, H.-J. Reasoning about Temporal Relations: A Maximal Tractable Subclass of Allen's Interval Algebra Journal of the ACM, 1995, 42, 43-66
 * A complete version for all temporal relationships (which is significantly slower than the path consistency algorithm) will be part of a future version.
 * <p>
 * Please note that the paper contains some minor mistakes which have been corrected in this source code (see also comments).
 * <p>
 * Copyright 2010 Jörn Franke Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http:
 */
package jcog.time;

import jcog.data.set.ArrayHashSet;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;


/**
 * An Allen Temporal Constraints Network
 *
 * @author Jörn Franke <jornfranke@gmail.com>
 *
 * see also: https://github.com/anskarl/LoMRF/blob/develop/docs/2_2_temporal_inference_examples.md#predicate-schemas
 */
public class TemporalConstraints<E> {


    
    static final short bin_before = (short) 1;
    private static final short bin_after = (short) 2;
    private static final short bin_during = (short) 4;
    static final short bin_contains = (short) 8;
    static final short bin_overlaps = (short) 16;
    private static final short bin_overlappedby = (short) 32;
    static final short bin_meets = (short) 64;
    private static final short bin_metby = (short) 128;
    static final short bin_starts = (short) 256;
    private static final short bin_startedby = (short) 512;
    static final short bin_finishes = (short) 1024;
    private static final short bin_finishedby = (short) 2048;
    static final short bin_equals = (short) 4096;
    private static final short bin_all = (short) ((int) bin_before | (int) bin_after | (int) bin_during | (int) bin_contains | (int) bin_overlaps | (int) bin_overlappedby | (int) bin_meets | (int) bin_metby | (int) bin_starts | (int) bin_startedby | (int) bin_finishes | (int) bin_finishedby | (int) bin_equals);
    
    private static final String str_before = "before";
    private static final String str_after = "after";
    private static final String str_during = "during";
    private static final String str_contains = "contains";

    
    private static final String str_overlaps = "overlaps";
    private static final String str_overlappedby = "overlapped by";
    private static final String str_meets = "meets";
    private static final String str_metby = "met by";
    private static final String str_starts = "starts";
    private static final String str_startedby = "started by";
    private static final String str_finishes = "finishes";
    private static final String str_finishedby = "finished by";
    private static final String str_equals = "equals";
    private static final short[][] transitivematrixshort = {
            
            {bin_before, bin_all, (short) (bin_before | (int) bin_overlaps | (int) bin_meets | (int) bin_during | (int) bin_starts), bin_before, bin_before, (short) (bin_before | (int) bin_overlaps | (int) bin_meets | (int) bin_during | (int) bin_starts), bin_before, (short) (bin_before | (int) bin_overlaps | (int) bin_meets | (int) bin_during | (int) bin_starts), bin_before, bin_before, (short) (bin_before | (int) bin_overlaps | (int) bin_meets | (int) bin_during | (int) bin_starts), bin_before, bin_before},
            

            
            {bin_all, bin_after, (short) (bin_after | (int) bin_overlappedby | (int) bin_metby | (int) bin_during | (int) bin_finishes), bin_after, (short) (bin_after | (int) bin_overlappedby | (int) bin_metby | (int) bin_during | (int) bin_finishes), bin_after, (short) (bin_after | (int) bin_overlappedby | (int) bin_metby | (int) bin_during | (int) bin_finishes), bin_after, (short) (bin_after | (int) bin_overlappedby | (int) bin_metby | (int) bin_during | (int) bin_finishes), bin_after, bin_after, bin_after, bin_after},
            
            
            {bin_before, bin_after, bin_during, bin_all, (short) (bin_before | (int) bin_overlaps | (int) bin_meets | (int) bin_during | (int) bin_starts), (short) (bin_after | (int) bin_overlappedby | (int) bin_metby | (int) bin_during | (int) bin_finishes), bin_before, bin_after, bin_during, (short) (bin_after | (int) bin_overlappedby | (int) bin_metby | (int) bin_during | (int) bin_finishes), bin_during, (short) (bin_before | (int) bin_overlaps | (int) bin_meets | (int) bin_during | (int) bin_starts), bin_during},

            
            {(short) (bin_before | (int) bin_overlaps | (int) bin_meets | (int) bin_contains | (int) bin_finishedby), (short) (bin_after | (int) bin_overlappedby | (int) bin_contains | (int) bin_metby | (int) bin_startedby), (short) (bin_overlaps | (int) bin_overlappedby | (int) bin_during | (int) bin_contains | (int) bin_equals | (int) bin_starts | (int) bin_startedby | (int) bin_finishes | (int) bin_finishedby), bin_contains, (short) (bin_overlaps | (int) bin_contains | (int) bin_finishedby), (short) (bin_overlappedby | (int) bin_contains | (int) bin_startedby), (short) (bin_overlaps | (int) bin_contains | (int) bin_finishedby), (short) (bin_overlappedby | (int) bin_contains | (int) bin_startedby), (short) (bin_contains | (int) bin_finishedby | (int) bin_overlaps), bin_contains, (short) (bin_contains | (int) bin_startedby | (int) bin_overlappedby), bin_contains, bin_contains},

            
            {bin_before, (short) (bin_after | (int) bin_overlappedby | (int) bin_contains | (int) bin_metby | (int) bin_startedby), (short) (bin_overlaps | (int) bin_during | (int) bin_starts), (short) (bin_before | (int) bin_overlaps | (int) bin_meets | (int) bin_contains | (int) bin_finishedby), (short) (bin_before | (int) bin_overlaps | (int) bin_meets), (short) (bin_overlaps | (int) bin_overlappedby | (int) bin_during | (int) bin_contains | (int) bin_equals | (int) bin_starts | (int) bin_startedby | (int) bin_finishes | (int) bin_finishedby), bin_before, (short) (bin_overlappedby | (int) bin_contains | (int) bin_startedby), bin_overlaps, (short) (bin_contains | (int) bin_finishedby | (int) bin_overlaps), (short) (bin_during | (int) bin_starts | (int) bin_overlaps), (short) (bin_before | (int) bin_overlaps | (int) bin_meets), bin_overlaps},

            
            {(short) (bin_before | (int) bin_overlaps | (int) bin_meets | (int) bin_contains | (int) bin_finishedby), bin_after, (short) (bin_overlappedby | (int) bin_during | (int) bin_finishes), (short) (bin_after | (int) bin_overlappedby | (int) bin_metby | (int) bin_contains | (int) bin_startedby), (short) (bin_overlaps | (int) bin_overlappedby | (int) bin_during | (int) bin_contains | (int) bin_equals | (int) bin_starts | (int) bin_startedby | (int) bin_finishes | (int) bin_finishedby), (short) (bin_after | (int) bin_overlappedby | (int) bin_metby), (short) (bin_overlaps | (int) bin_contains | (int) bin_finishedby), bin_after, (short) (bin_overlappedby | (int) bin_during | (int) bin_finishes), (short) (bin_overlappedby | (int) bin_after | (int) bin_metby), bin_overlappedby, (short) (bin_overlappedby | (int) bin_contains | (int) bin_startedby), bin_overlappedby},

            
            {bin_before, (short) (bin_after | (int) bin_overlappedby | (int) bin_metby | (int) bin_contains | (int) bin_startedby), (short) (bin_overlaps | (int) bin_during | (int) bin_starts), bin_before, bin_before, (short) (bin_overlaps | (int) bin_during | (int) bin_starts), bin_before, (short) (bin_finishes | (int) bin_finishedby | (int) bin_equals), bin_meets, bin_meets, (short) (bin_during | (int) bin_starts | (int) bin_overlaps), bin_before, bin_meets},

            
            {(short) (bin_before | (int) bin_overlaps | (int) bin_meets | (int) bin_contains | (int) bin_finishedby), bin_after, (short) (bin_overlappedby | (int) bin_during | (int) bin_finishes), bin_after, (short) (bin_overlappedby | (int) bin_during | (int) bin_finishes), bin_after, (short) (bin_starts | (int) bin_startedby | (int) bin_equals), bin_after, (short) (bin_during | (int) bin_finishes | (int) bin_overlappedby), bin_after, bin_metby, bin_metby, bin_metby},

            
            {bin_before, bin_after, bin_during, (short) (bin_before | (int) bin_overlaps | (int) bin_meets | (int) bin_contains | (int) bin_finishedby), (short) (bin_before | (int) bin_overlaps | (int) bin_meets), (short) (bin_overlappedby | (int) bin_during | (int) bin_finishes), bin_before, bin_metby, bin_starts, (short) (bin_starts | (int) bin_startedby | (int) bin_equals), bin_during, (short) (bin_before | (int) bin_meets | (int) bin_overlaps), bin_starts},

            
            {(short) (bin_before | (int) bin_overlaps | (int) bin_meets | (int) bin_contains | (int) bin_finishedby), bin_after, (short) (bin_overlappedby | (int) bin_during | (int) bin_finishes), bin_contains, (short) (bin_overlaps | (int) bin_contains | (int) bin_finishedby), bin_overlappedby, (short) (bin_overlaps | (int) bin_contains | (int) bin_finishedby), bin_metby, (short) (bin_starts | (int) bin_startedby | (int) bin_equals), bin_startedby, bin_overlappedby, bin_contains, bin_startedby},

            
            {bin_before, bin_after, bin_during, (short) (bin_after | (int) bin_overlappedby | (int) bin_metby | (int) bin_contains | (int) bin_startedby), (short) (bin_overlaps | (int) bin_during | (int) bin_starts), (short) (bin_after | (int) bin_overlappedby | (int) bin_metby), bin_meets, bin_after, bin_during, (short) (bin_after | (int) bin_overlappedby | (int) bin_metby), bin_finishes, (short) (bin_finishes | (int) bin_finishedby | (int) bin_equals), bin_finishes},

            
            {bin_before, (short) (bin_after | (int) bin_overlappedby | (int) bin_metby | (int) bin_contains | (int) bin_startedby), (short) (bin_overlaps | (int) bin_during | (int) bin_starts), bin_contains, bin_overlaps, (short) (bin_overlappedby | (int) bin_contains | (int) bin_startedby), bin_meets, (short) (bin_startedby | (int) bin_overlappedby | (int) bin_contains), bin_overlaps, bin_contains, (short) (bin_finishes | (int) bin_finishedby | (int) bin_equals), bin_finishedby, bin_finishedby},

            
            {bin_before, bin_after, bin_during, bin_contains, bin_overlaps, bin_overlappedby, bin_meets, bin_metby, bin_starts, bin_startedby, bin_finishes, bin_finishedby, bin_equals},


    };
    
    private final ArrayList<Constraint<E>> modeledConstraints;
    
    private final ArrayHashSet<AllenNode<E>> modeledNodes;
    
    private final ArrayList<ShortArrayList> constraintnetwork;
    
    private boolean previouslyInconsistent = false;

    /*
     * Constructor creates an empty constraint network
     *
     *
     */

    TemporalConstraints() {
        this.modeledNodes = new ArrayHashSet<>();
        this.modeledConstraints = new ArrayList<>();
        this.constraintnetwork = new ArrayList<>();
    }

    /*
     * Adds a node to the list of modeled nodes constraint network
     *
     * @param nodeAdd Node to be added to the list of modeled nodes and the constraint network
     *
     * @return true if node has been successfully added, false, if not
     *
     */

    public boolean add(AllenNode<E> nodeAdd) {
        
        if (!this.modeledNodes.add(nodeAdd)) {
            return false;
        } else {
            this.addConstraint(nodeAdd);
            return true;
        }
    }


    /*
     * Adds a node to the constraint network. By default all interval relationships are possible from this node to the others and the other
     * way around. This has to be verified/corrected by the path consistency algorithm (@see #pathConsistency)
     *
     * @param nodeAdd Node to be added to the constraint network
     *
     */

    private void addConstraint(AllenNode<E> nodeAdd) {


        ArrayList<ShortArrayList> constraintnetwork1 = this.constraintnetwork;
        for (int i = 0, constraintnetwork1Size = constraintnetwork1.size(); i < constraintnetwork1Size; i++) {
            constraintnetwork1.get(i).add(bin_all); 
        }
        
        this.constraintnetwork.add(new ShortArrayList());
        nodeAdd.allen = this.constraintnetwork.size() - 1;
        
        if (this.constraintnetwork.size() > 1) {
            ShortArrayList previousALShort = this.constraintnetwork.get(0);
            for (int i = 0; i < previousALShort.size(); i++)
                
                this.constraintnetwork.get(this.constraintnetwork.size() - 1).add(bin_all); 

        } else
            this.constraintnetwork.get(this.constraintnetwork.size() - 1).add(bin_equals); 
        
        this.constraintnetwork.get(this.constraintnetwork.size() - 1).set(this.constraintnetwork.size() - 1, bin_equals); 

    }



    /*
     * This method adds a constraint to the list of modeled constraints and the constraint network
     *
     *  @param constraintAdd Constraint to be added to the list of modeled constraints and the constraint network
     *
     *  @return true if constraint has been added, false, if not
     *
     */

    boolean add(Constraint<E> c) {
        
        if (!this.modeledConstraints.contains(c)) {

            ArrayList<Constraint<E>> modeledConstraints1 = this.modeledConstraints;
            for (int i = 0, modeledConstraints1Size = modeledConstraints1.size(); i < modeledConstraints1Size; i++) {
                Constraint<E> currentConstraint = modeledConstraints1.get(i);
                AllenNode<E> src = currentConstraint.src;
                if (src.equals(c.src) && (currentConstraint.tgt.equals(c.tgt))) {
                    return false;
                }
                if (src.equals(c.tgt) && (currentConstraint.tgt.equals(c.src))) {
                    return false;
                }
            }
            
            if (!this.modeledNodes.contains(c.src) || (!this.modeledNodes.contains(c.tgt))) {
                return false;
            }
            this.modeledConstraints.add(c);
        } else {
            return false;
        }
        this.addConstraintToConstraintNetwork(c);
        
        if (!this.pathConsistency()) this.previouslyInconsistent = true;
        return true;
    }

    /*
     * Adds a constraint to the constraint network
     *
     * @param constraintAdd constraint to be added to the constraint network
     *
     */

    private void addConstraintToConstraintNetwork(Constraint<E> constraintAdd) {


        int i = constraintAdd.src.allen;
        int j = constraintAdd.tgt.allen;

        
        this.constraintnetwork.get(i).set(j, constraintAdd.constraints);
        
        this.constraintnetwork.get(j).set(i, TemporalConstraints.inverseConstraintsShort(constraintAdd.constraints));

    }


    /*
     * Implements allen's path consistency algorithm. Please note that the algorithm may not be able to detect all inconsistent networks
     * (see references above). Please note that another output of this algorithm is an updated constraint network (@see #getConstraintNetwork), with
     * all possible constraints between nodes given the defined constraints
     *
     * @return true, network is consistent and false, network is inconsistent
     *
     */

    boolean pathConsistency() {
        if (previouslyInconsistent) return false; 
        if (this.modeledConstraints.isEmpty()) return true;

        int S = this.constraintnetwork.size();
        ArrayList<BooleanArrayList> stack = new ArrayList<BooleanArrayList>(S);
        for (int i = 0; i < S; i++) {
            BooleanArrayList currentStackEntryAL = new BooleanArrayList(S);
            for (int j = 0; j < S; j++) {
                currentStackEntryAL.add(false); 
            }
            stack.add(currentStackEntryAL);
        }


        Constraint<E> start = this.modeledConstraints.get(modeledConstraints.size() - 1);

        List<IntIntPair> batchStack = new ArrayList<>();
        batchStack.add(pair(start.src.allen, start.tgt.allen));
        
        stack.get(start.src.allen).set(start.tgt.allen, true);
        
        stack.get(start.tgt.allen).set(start.src.allen, true);
        

        while (!batchStack.isEmpty()) {

            IntIntPair currentEdge = batchStack.get(0);
            batchStack.remove(0);
            
            stack.get(currentEdge.getOne()).set(currentEdge.getTwo(), false);


            int i = currentEdge.getOne();
            ShortArrayList ci = this.constraintnetwork.get(i);

            int j = currentEdge.getTwo();
            
            for (int k = 0; k < S; k++) {

                ShortArrayList cnk = this.constraintnetwork.get(k);
                short ckj = cnk.get(j);
                short cki = cnk.get(i);

                short cik = ci.get(k);
                short cjk = this.constraintnetwork.get(j).get(k);
                short cij = ci.get(j);
                
                
                
                short ckiij = collectConstraintsShort(cki, cij);

                ckj = (short) ((int) ckj & (int) ckiij);
                
                if ((int) ckj == 0) {
                    return false;
                }


                short ckjtemp = cnk.get(j);
                if (((int) ckj != (int) ckjtemp && ((int) ckjtemp & (int) ckj) == (int) ckj)) {

                    
                    if ((!stack.get(k).get(j))) {  
                        
                        stack.get(k).set(j, true);
                        IntIntPair updatePair = pair(k, j);
                        batchStack.add(updatePair);
                    }
                    if ((!stack.get(j).get(k))) {  
                        
                        stack.get(j).set(k, true);
                        IntIntPair updatePairInverse = pair(j, k);
                        batchStack.add(updatePairInverse);
                    }
                    
                    cnk.set(j, ckj);

                    short iCon = TemporalConstraints.inverseConstraintsShort(ckj);
                    this.constraintnetwork.get(j).set(k, iCon);
                }
                
                
                short cijjk = collectConstraintsShort(cij, cjk);

                cik = (short) ((int) cik & (int) cijjk);
                
                if ((int) cik == 0) {

                    return false;
                }


                short ciktemp = ci.get(k);
                if (((int) cik != (int) ciktemp && ((int) ciktemp & (int) cik) == (int) cik)) {

                    if ((!stack.get(i).get(k))) {  
                        
                        stack.get(i).set(k, true);
                        IntIntPair updatePair = pair(i, k);
                        batchStack.add(updatePair);
                    }
                    if ((!stack.get(k).get(i))) {  
                        
                        stack.get(k).set(i, true);
                        IntIntPair updatePairInverse = pair(k, i);
                        batchStack.add(updatePairInverse);
                    }
                    
                    ci.set(k, cik);

                    short iCon = TemporalConstraints.inverseConstraintsShort(cik);
                    cnk.set(i, iCon);


                }

            }
        }
        return true;

    }

    /*
     * This is one of the most costly functions in Allen's path consistency algorithm. For each constraint (between A and B) given the first parameter it looks
     * up for each constraint (between B and C) given in the second parameter in the transivity table the relationship between A and C.
     *
     * @param c1 constraints between A and B
     * @param c2 constraints between B and C
     *
     * @return constraints between A and C
     *
     *
     */

    private static Short collectConstraintsShort(Short c1, Short c2) {
        short result = (short) 0;
        
        for (int i = 0; i < 14; i++) {

            short c1select = (short) (1 << i);
            
            if ((int) (short) (c1 & (int) c1select) == (int) c1select) {
                for (int j = 0; j < 14; j++) {

                    short c2select = (short) (1 << j);
                    
                    if ((int) (short) (c2 & (int) c2select) == (int) c2select) {

                        short constraints = transitivematrixshort[i][j];

                        result = (short) ((int) result | (int) constraints);
                        if (((int) result & (int) bin_all) == (int) bin_all) {
                            return result; 
                        }
                    }
                }
            }
        }
        return result;
    }

    /*
     * This method inverts the constraints given in the parameter (e.g. the constraint "overlaps" becomes the constraint "overlapped by" and vice versa)
     *
     *  @param c constraints (represented as  a short) to invert
     *
     * @return inverted constraints
     *
     */

    private static short inverseConstraintsShort(Short c) {
        
        short result = (short) 0;
        
        if ((int) (short) (c & (int) bin_before) == (int) bin_before) result = (short) ((int) result | (int) bin_after);
        
        if ((int) (short) (c & (int) bin_after) == (int) bin_after) result = (short) ((int) result | (int) bin_before);
        
        if ((int) (short) (c & (int) bin_during) == (int) bin_during) result = (short) ((int) result | (int) bin_contains);
        
        if ((int) (short) (c & (int) bin_contains) == (int) bin_contains) result = (short) ((int) result | (int) bin_during);
        
        if ((int) (short) (c & (int) bin_overlaps) == (int) bin_overlaps) result = (short) ((int) result | (int) bin_overlappedby);
        
        if ((int) (short) (c & (int) bin_overlappedby) == (int) bin_overlappedby) result = (short) ((int) result | (int) bin_overlaps);
        
        if ((int) (short) (c & (int) bin_meets) == (int) bin_meets) result = (short) ((int) result | (int) bin_metby);
        
        if ((int) (short) (c & (int) bin_metby) == (int) bin_metby) result = (short) ((int) result | (int) bin_meets);
        
        if ((int) (short) (c & (int) bin_starts) == (int) bin_starts) result = (short) ((int) result | (int) bin_startedby);
        
        if ((int) (short) (c & (int) bin_startedby) == (int) bin_startedby) result = (short) ((int) result | (int) bin_starts);
        
        if ((int) (short) (c & (int) bin_finishes) == (int) bin_finishes) result = (short) ((int) result | (int) bin_finishedby);
        
        if ((int) (short) (c & (int) bin_finishedby) == (int) bin_finishedby) result = (short) ((int) result | (int) bin_finishes);
        
        if ((int) (short) (c & (int) bin_equals) == (int) bin_equals) result = (short) ((int) result | (int) bin_equals);

        return result;
    }












    /*
     * This method returns the list of modeled constraints
     *
     * @return list of modeled constraints
     *
     */
    ArrayList<Constraint<E>> getModeledConstraints() {
        return this.modeledConstraints;
    }

    /*
     * This method returns the lsit of modeled nodes
     *
     *  @return list of modeled nodes
     *
     */

    ArrayHashSet<AllenNode<E>> getModeledNodes() {
        return this.modeledNodes;
    }

    /*
     * Returns a list of names of the constraints given in the set of constraints
     *
     * @param set of constraints c
     *
     * @return list of names of the constraints given in c
     *
     *
     */

    public static ArrayList<String> getConstraintStringFromConstraintShort(short c) {
        ArrayList<String> result = new ArrayList<String>();
        
        if ((int) (short) ((int) c & (int) bin_before) == (int) bin_before) result.add(str_before);
        
        if ((int) (short) ((int) c & (int) bin_after) == (int) bin_after) result.add(str_after);
        
        if ((int) (short) ((int) c & (int) bin_during) == (int) bin_during) result.add(str_during);
        
        if ((int) (short) ((int) c & (int) bin_contains) == (int) bin_contains) result.add(str_contains);
        
        if ((int) (short) ((int) c & (int) bin_overlaps) == (int) bin_overlaps) result.add(str_overlaps);
        
        if ((int) (short) ((int) c & (int) bin_overlappedby) == (int) bin_overlappedby) result.add(str_overlappedby);
        
        if ((int) (short) ((int) c & (int) bin_meets) == (int) bin_meets) result.add(str_meets);
        
        if ((int) (short) ((int) c & (int) bin_metby) == (int) bin_metby) result.add(str_metby);
        
        if ((int) (short) ((int) c & (int) bin_starts) == (int) bin_starts) result.add(str_starts);
        
        if ((int) (short) ((int) c & (int) bin_startedby) == (int) bin_startedby) result.add(str_startedby);
        
        if ((int) (short) ((int) c & (int) bin_finishes) == (int) bin_finishes) result.add(str_finishes);
        
        if ((int) (short) ((int) c & (int) bin_finishedby) == (int) bin_finishedby) result.add(str_finishedby);
        
        if ((int) (short) ((int) c & (int) bin_equals) == (int) bin_equals) result.add(str_equals);
        return result;
    }

    /**
     * @author Jörn Franke <jornfranke@gmail.com>
     *
     */
    static class Constraint<E> {
        final AllenNode<E> src;
        final AllenNode<E> tgt;
        final short constraints;

        Constraint(AllenNode<E> src, AllenNode<E> tgt, short constraints) {
            this.src = src;
            this.tgt = tgt;
            this.constraints=constraints;
        }

    }

    /**
     * @author Jörn Franke <jornfranke@gmail.com>
     *
     */
    static class AllenNode<E> {

        final E id;

        int allen;

        AllenNode(E id) {
            this.id = id;
        }

    }
}
