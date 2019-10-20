package org.zhz.dfargx.automata;

import org.eclipse.collections.impl.map.mutable.primitive.CharObjectHashMap;

import java.util.HashSet;
import java.util.Set;

/**
 * Created on 2015/5/10.
 */
public class NFAState {

    private static int nextID;
    public final Set<NFAState> directTable;
    public final CharObjectHashMap<Set<NFAState>> transitions;
    private final int id;

    public NFAState(int id) {
        directTable = new HashSet<>();
        transitions = new CharObjectHashMap<>();
        this.id = id;
    }

    public static NFAState create() {
        return new NFAState(nextID++);
    }


    @Override
    public String toString() {
        return id + "{" +
                "direct*" + directTable.size() +
                (transitions.isEmpty() ? "" : ", transi*" + transitions.keySet() )+
                '}';
    }

    public void transitionRule(char ch, NFAState state) {
        transitions.getIfAbsentPut(ch, HashSet::new).add(state);
    }

    public void directRule(NFAState state) {
        directTable.add(state);
    }





    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        NFAState state = (NFAState) o;
        return id == state.id; 
    }

    @Override
    public int hashCode() {
        return id; 
    }

}
