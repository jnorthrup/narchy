package org.zhz.dfargx.automata;

import org.eclipse.collections.api.block.procedure.primitive.CharObjectProcedure;
import org.eclipse.collections.impl.map.mutable.primitive.CharObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.zhz.dfargx.util.CommonSets;

import java.util.*;
import java.util.function.Function;

/**
 * Created on 2015/5/10.
 */
public class DFA {

    public final int[][] transitions;
    private int is; 
    private int rs; 
    private boolean[] fs; 

    public DFA(List<NFAState> nfaStateList) {
        is = rs = -1;
        fs = null;
        this.transitions = convert(nfaStateList);
    }

    public int getRejectedState() {
        return rs;
    }

    public int getInitState() {
        return is;
    }

    public boolean[] getFinalStates() {
        return fs;
    }

    private int[][] convert(List<NFAState> nfaStateList) {
        NFAState initState = nfaStateList.get(0);
        NFAState finalState = nfaStateList.get(1);

        Map<NFAState, Set<NFAState>> closureMap = calculateClosure(nfaStateList);

        
        Map<NFAState, CharObjectHashMap<Set<NFAState>>> nfaTransitionMap = new HashMap<>();
        for (NFAState state : nfaStateList) {
            CharObjectHashMap<Set<NFAState>> subMap = new CharObjectHashMap();
            for (char ch = (char) 0; (int) ch < CommonSets.ENCODING_LENGTH; ch++) {
                Set<NFAState> closure = closureMap.get(state);
                Set<NFAState> reachable = traceReachable(closure, ch, closureMap);
                if (!reachable.isEmpty()) {
                    subMap.put(ch, reachable);
                }
            }
            nfaTransitionMap.put(state, subMap);
        }

        
        Map<Set<NFAState>, CharObjectHashMap<Set<NFAState>>> originalDFATransitionMap = new HashMap<>();
        constructOriginalDFA(closureMap.get(initState), nfaTransitionMap, originalDFATransitionMap);

        
        return minimize(originalDFATransitionMap, closureMap.get(initState), finalState);
    }

    private static void constructOriginalDFA(Set<NFAState> stateSet, Map<NFAState, CharObjectHashMap<Set<NFAState>>> nfaTransitionMap, Map<Set<NFAState>, CharObjectHashMap<Set<NFAState>>> originalDFATransitionMap) {
        CharObjectHashMap<Set<NFAState>> subMap = originalDFATransitionMap.get(stateSet);
        if (subMap == null) {
            subMap = new CharObjectHashMap<>();
            originalDFATransitionMap.put(stateSet, subMap);
        }
        for (char ch = (char) 0; (int) ch < CommonSets.ENCODING_LENGTH; ch++) {
            Set<NFAState> union = new HashSet<>();
            for (NFAState state : stateSet) {
                Set<NFAState> nfaSet = nfaTransitionMap.get(state).get(ch);
                if (nfaSet != null) {
                    union.addAll(nfaSet);
                }
            }
            if (!union.isEmpty()) {
                subMap.put(ch, union);
                if (!originalDFATransitionMap.containsKey(union)) {
                    constructOriginalDFA(union, nfaTransitionMap, originalDFATransitionMap);
                }
            }
        }
    }

    private static Map<NFAState, Set<NFAState>> calculateClosure(List<NFAState> nfaStateList) {
        Map<NFAState, Set<NFAState>> map = new HashMap<>();
        for (NFAState state : nfaStateList) {
            Set<NFAState> closure = new HashSet<>();
            dfsClosure(state, closure);
            map.put(state, closure);
        }
        return map;
    }

    private static void dfsClosure(NFAState state, Set<NFAState> closure) {
        closure.add(state);
        for (NFAState next : state.directTable) {
            dfsClosure(next, closure);
        }
    }

    private static Set<NFAState> traceReachable(Set<NFAState> closure, char ch, Map<NFAState, Set<NFAState>> closureMap) {
        Set<NFAState> result = new HashSet<>();
        for (NFAState closureState : closure) {
            CharObjectHashMap<Set<NFAState>> transitionMap = closureState.transitions;
            Set<NFAState> stateSet = transitionMap.get(ch);
            if (stateSet != null) {
                for (NFAState state : stateSet) {
                    result.addAll(closureMap.get(state)); 
                }
            }
        }
        return result;
    }

    private int[][] minimize(Map<Set<NFAState>, CharObjectHashMap<Set<NFAState>>> oriDFATransitionMap, Set<NFAState> initClosure, NFAState finalNFAState) {
        Map<Set<NFAState>, Integer> stateRenamingMap = new HashMap<>();
        int initStateAfterRenaming = -1;
        int renamingStateID = 1;

        
        for (Set<NFAState> nfaState : oriDFATransitionMap.keySet()) {
            if (initStateAfterRenaming == -1 && nfaState.equals(initClosure)) {
                initStateAfterRenaming = renamingStateID; 
            }
            stateRenamingMap.put(nfaState, renamingStateID++);
        }

        Map<Integer, int[]> renamedDFATransitionTable = new HashMap<>();
        renamedDFATransitionTable.put(0, newRejectState());
        Map<Integer, Boolean> finalFlags = new HashMap<>();
        finalFlags.put(0, false);

        
        for (Map.Entry<Set<NFAState>, CharObjectHashMap<Set<NFAState>>> entry : oriDFATransitionMap.entrySet()) {
            Set<NFAState> ek = entry.getKey();
            renamingStateID = stateRenamingMap.get(ek);
            int[] state = newRejectState();
            CharObjectHashMap<Set<NFAState>> ev = entry.getValue();

            ev.forEachKeyValue(new CharObjectProcedure<Set<NFAState>>() {
                @Override
                public void value(char k, Set<NFAState> v) {
                    state[(int) k] = stateRenamingMap.get(v);
                }
            });

            renamedDFATransitionTable.put(renamingStateID, state);

            finalFlags.put(renamingStateID, ek.contains(finalNFAState));
        }


        IntIntHashMap groupFlags = new IntIntHashMap();
        for (int i = 0; i < finalFlags.size(); i++) {
            boolean b = finalFlags.get(i);
            groupFlags.put(i, b ? 0 : 1);
        }

        int groupTotal = 2;
        int preGroupTotal;
        do { 
            preGroupTotal = groupTotal;
            for (int sensitiveGroup = 0; sensitiveGroup < preGroupTotal; sensitiveGroup++) {
                
                Map<Map<Integer, Integer>, Set<Integer>> invertMap = new HashMap<>();
                for (int sid = 0; sid < groupFlags.size(); sid++) {
                    int group = groupFlags.get(sid);
                    if (sensitiveGroup == group) {
                        Map<Integer, Integer> targetGroupTable = new HashMap<>(CommonSets.ENCODING_LENGTH);
                        for (char ch = (char) 0; (int) ch < CommonSets.ENCODING_LENGTH; ch++) {
                            int targetState = renamedDFATransitionTable.get(sid)[(int) ch];
                            int targetGroup = groupFlags.get(targetState);
                            targetGroupTable.put((int) ch, targetGroup);
                        }
                        Set<Integer> stateIDSet = invertMap.computeIfAbsent(targetGroupTable, new Function<Map<Integer, Integer>, Set<Integer>>() {
                            @Override
                            public Set<Integer> apply(Map<Integer, Integer> k) {
                                return new HashSet<>();
                            }
                        });
                        stateIDSet.add(sid);
                    }
                }

                boolean first = true;
                for (Set<Integer> stateIDSet : invertMap.values()) {
                    if (first) {
                        first = false;
                    } else {
                        for (int sid : stateIDSet) {
                            groupFlags.put(sid, groupTotal);
                        }
                        groupTotal++;
                    }
                }
            }
        } while (preGroupTotal != groupTotal);

        
        is = groupFlags.get(initStateAfterRenaming);

        
        rs = groupFlags.get(0);

        
        Set<Integer> finalGroupFlags = new HashSet<>();
        for (int i = 0, groupFlagsSize = groupFlags.size(); i < groupFlagsSize; i++) {
            Integer groupFlag = groupFlags.get(i);
            if (finalFlags.get(i)) {
                finalGroupFlags.add(groupFlag);
            }
        }

        boolean[] fs = this.fs = new boolean[groupTotal];
        for (int i = 0; i < groupTotal; i++) {
            fs[i] = finalGroupFlags.contains(i);
        }


        int[][] tt = new int[groupTotal][];

        for (int groupID = 0; groupID < groupTotal; groupID++) {
            for (int sid = 0; sid < groupFlags.size(); sid++) {
                if (groupID == groupFlags.get(sid)) {
                    int[] oriState = renamedDFATransitionTable.get(sid);
                    int[] state = new int[CommonSets.ENCODING_LENGTH];
                    for (char ch = (char) 0; (int) ch < CommonSets.ENCODING_LENGTH; ch++) {
                        int next = oriState[(int) ch];
                        state[(int) ch] = groupFlags.get(next);
                    }
                    tt[groupID] = state;
                    break;
                }
            }
        }
        return tt;
    }


    private static int[] newRejectState() {
        int[] state = new int[CommonSets.ENCODING_LENGTH];
        
        return state;
    }




}
