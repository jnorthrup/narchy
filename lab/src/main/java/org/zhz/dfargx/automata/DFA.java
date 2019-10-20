package org.zhz.dfargx.automata;

import org.eclipse.collections.impl.map.mutable.primitive.CharObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.zhz.dfargx.util.CommonSets;

import java.util.*;

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
        var initState = nfaStateList.get(0);
        var finalState = nfaStateList.get(1);

        var closureMap = calculateClosure(nfaStateList);

        
        Map<NFAState, CharObjectHashMap<Set<NFAState>>> nfaTransitionMap = new HashMap<>();
        for (var state : nfaStateList) {
            CharObjectHashMap<Set<NFAState>> subMap = new CharObjectHashMap();
            for (char ch = 0; ch < CommonSets.ENCODING_LENGTH; ch++) {
                var closure = closureMap.get(state);
                var reachable = traceReachable(closure, ch, closureMap);
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
        var subMap = originalDFATransitionMap.get(stateSet);
        if (subMap == null) {
            subMap = new CharObjectHashMap<>();
            originalDFATransitionMap.put(stateSet, subMap);
        }
        for (char ch = 0; ch < CommonSets.ENCODING_LENGTH; ch++) {
            Set<NFAState> union = new HashSet<>();
            for (var state : stateSet) {
                var nfaSet = nfaTransitionMap.get(state).get(ch);
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
        for (var state : nfaStateList) {
            Set<NFAState> closure = new HashSet<>();
            dfsClosure(state, closure);
            map.put(state, closure);
        }
        return map;
    }

    private static void dfsClosure(NFAState state, Set<NFAState> closure) {
        closure.add(state);
        for (var next : state.directTable) {
            dfsClosure(next, closure);
        }
    }

    private static Set<NFAState> traceReachable(Set<NFAState> closure, char ch, Map<NFAState, Set<NFAState>> closureMap) {
        Set<NFAState> result = new HashSet<>();
        for (var closureState : closure) {
            var transitionMap = closureState.transitions;
            var stateSet = transitionMap.get(ch);
            if (stateSet != null) {
                for (var state : stateSet) {
                    result.addAll(closureMap.get(state)); 
                }
            }
        }
        return result;
    }

    private int[][] minimize(Map<Set<NFAState>, CharObjectHashMap<Set<NFAState>>> oriDFATransitionMap, Set<NFAState> initClosure, NFAState finalNFAState) {
        Map<Set<NFAState>, Integer> stateRenamingMap = new HashMap<>();
        var initStateAfterRenaming = -1;
        var renamingStateID = 1;

        
        for (var nfaState : oriDFATransitionMap.keySet()) {
            if (initStateAfterRenaming == -1 && nfaState.equals(initClosure)) {
                initStateAfterRenaming = renamingStateID; 
            }
            stateRenamingMap.put(nfaState, renamingStateID++);
        }

        Map<Integer, int[]> renamedDFATransitionTable = new HashMap<>();
        renamedDFATransitionTable.put(0, newRejectState());
        Map<Integer, Boolean> finalFlags = new HashMap<>();
        finalFlags.put(0, false);

        
        for (var entry : oriDFATransitionMap.entrySet()) {
            var ek = entry.getKey();
            renamingStateID = stateRenamingMap.get(ek);
            var state = newRejectState();
            var ev = entry.getValue();

            ev.forEachKeyValue( (k, v) -> state[k] = stateRenamingMap.get(v));

            renamedDFATransitionTable.put(renamingStateID, state);

            finalFlags.put(renamingStateID, ek.contains(finalNFAState));
        }


        var groupFlags = new IntIntHashMap();
        for (var i = 0; i < finalFlags.size(); i++) {
            boolean b = finalFlags.get(i);
            groupFlags.put(i, b ? 0 : 1);
        }

        var groupTotal = 2;
        int preGroupTotal;
        do { 
            preGroupTotal = groupTotal;
            for (var sensitiveGroup = 0; sensitiveGroup < preGroupTotal; sensitiveGroup++) {
                
                Map<Map<Integer, Integer>, Set<Integer>> invertMap = new HashMap<>();
                for (var sid = 0; sid < groupFlags.size(); sid++) {
                    var group = groupFlags.get(sid);
                    if (sensitiveGroup == group) {
                        Map<Integer, Integer> targetGroupTable = new HashMap<>(CommonSets.ENCODING_LENGTH);
                        for (char ch = 0; ch < CommonSets.ENCODING_LENGTH; ch++) {
                            var targetState = renamedDFATransitionTable.get(sid)[ch];
                            var targetGroup = groupFlags.get(targetState);
                            targetGroupTable.put((int) ch, targetGroup);
                        }
                        var stateIDSet = invertMap.computeIfAbsent(targetGroupTable, k -> new HashSet<>());
                        stateIDSet.add(sid);
                    }
                }

                var first = true;
                for (var stateIDSet : invertMap.values()) {
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

        var fs = this.fs = new boolean[groupTotal];
        for (var i = 0; i < groupTotal; i++) {
            fs[i] = finalGroupFlags.contains(i);
        }


        var tt = new int[groupTotal][];

        for (var groupID = 0; groupID < groupTotal; groupID++) {
            for (var sid = 0; sid < groupFlags.size(); sid++) {
                if (groupID == groupFlags.get(sid)) {
                    var oriState = renamedDFATransitionTable.get(sid);
                    var state = new int[CommonSets.ENCODING_LENGTH];
                    for (char ch = 0; ch < CommonSets.ENCODING_LENGTH; ch++) {
                        var next = oriState[ch];
                        state[ch] = groupFlags.get(next);
                    }
                    tt[groupID] = state;
                    break;
                }
            }
        }
        return tt;
    }


    private static int[] newRejectState() {
        var state = new int[CommonSets.ENCODING_LENGTH];
        
        return state;
    }




}
