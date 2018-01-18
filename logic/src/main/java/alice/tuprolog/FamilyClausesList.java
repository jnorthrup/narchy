package alice.tuprolog;


import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;

/**
 * <code>FamilyClausesList</code> is a common <code>LinkedList</code>
 * which stores {@link ClauseInfo} objects. Internally it indexes stored data
 * in such a way that, knowing what type of clauses are required, only
 * goal compatible clauses are returned
 *
 * @author Paolo Contessi
 * @see LinkedList
 * @since 2.2
 */
public class FamilyClausesList extends
        //ConcurrentLinkedDeque<ClauseInfo> {
        //FasterList<ClauseInfo> {
        ArrayDeque<ClauseInfo> {

    private final FamilyClausesIndex<Number> numCompClausesIndex;
    private final FamilyClausesIndex<String> constantCompClausesIndex;
    private final FamilyClausesIndex<String> structCompClausesIndex;
    private final Deque<ClauseInfo> listCompClausesList;

    //private LinkedList<ClauseInfo> clausesList;

//    public FamilyClausesList(Collection<ClauseInfo> copy) {
//        this();
//        copy.forEach(this::addLast);
//    }

    public FamilyClausesList() {
        super();

        numCompClausesIndex = new FamilyClausesIndex<>();
        constantCompClausesIndex = new FamilyClausesIndex<>();
        structCompClausesIndex = new FamilyClausesIndex<>();

        listCompClausesList = new ArrayDeque<>();
    }

    /**
     * Adds the given clause as first of the family
     *
     * @param ci The clause to be added (with related informations)
     */
    //@Override
    public void addFirst(ClauseInfo ci) {
        super.addFirst( ci);

        // Add first in type related storage
        register(ci, true);
    }

    /**
     * Adds the given clause as last of the family
     *
     * @param ci The clause to be added (with related informations)
     */
    //@Override
    public void addLast(ClauseInfo ci) {
        super.addLast(ci);

        // Add last in type related storage
        register(ci, false);
    }

    @Override
    public boolean add(ClauseInfo o) {
        return add(o, false);
    }

    public final boolean add(ClauseInfo o, boolean first) {
        if (first)
            addFirst(o);
        else
            addLast(o);
        return true;
    }



    @Override
    public ClauseInfo removeLast() {
        int s = size();
        return s == 0 ? null : super.removeLast();

//		ClauseInfo ci = getLast();
//		if (remove(ci)){
//			return ci;
//		}
//
//		return null;
    }


    //@Override
    public ClauseInfo remove() {
        return removeFirst();
    }


    @Override
    public boolean remove(Object ci) {
        if (super.remove(ci)) {
            unregister((ClauseInfo) ci);

            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        while (removeLast() != null) {
        }
    }

    /**
     * Retrieves a sublist of all the clauses of the same family as the goal
     * and which, in all probability, could match with the given goal
     *
     * @param goal The goal to be resolved
     * @return The list of goal-compatible predicates
     */
    public Deque<ClauseInfo> get(Term goal) {
        // Gets the correct list and encapsulates it in ReadOnlyLinkedList
        if (goal instanceof Struct) {
            Struct g = (Struct) goal.term();

            /*
             * If no arguments no optimization can be applied
             * (and probably no optimization is needed)
             */
            if (g.subs() == 0) {
                //return new ReadOnlyLinkedList<>(this);
                return /*Collections.unmodifiableList*/(this);
            }

            /* Retrieves first argument and checks type */
            Term t = g.sub(0).term();
            if (t instanceof Var) {
                /*
                 * if first argument is an unbounded variable,
                 * no reasoning is possible, all family must be returned
                 */
                //return new ReadOnlyLinkedList<>(this);
                return /*Collections.unmodifiableList*/(this);
            } else if (t.isAtom()) {
                if (t instanceof Number) {
                    /* retrieves clauses whose first argument is numeric (or Var)
                     * and same as goal's first argument, if no clauses
                     * are retrieved, all clauses with a variable
                     * as first argument
                     */
                    //return new ReadOnlyLinkedList<>(
                    return /*Collections.unmodifiableList*/(
                            numCompClausesIndex.get((Number) t));
                } else if (t instanceof Struct) {
                    /* retrieves clauses whose first argument is a constant (or Var)
                     * and same as goal's first argument, if no clauses
                     * are retrieved, all clauses with a variable
                     * as first argument
                     */
                    //return new ReadOnlyLinkedList<>(
                    return /*Collections.unmodifiableList*/(
                            constantCompClausesIndex.get(((Struct) t).name()));
                }
            } else if (t instanceof Struct) {
                return /*Collections.unmodifiableList*/(
                        isAList((Struct) t) ?
                                listCompClausesList :
                                structCompClausesIndex.get(((Struct) t).key())
                );
            }
        }

        /* Default behaviour: no optimization done */
        return /*Collections.unmodifiableList*/(this);
        //return new ReadOnlyLinkedList<>(this);
    }





//    private Iterator<ClauseInfo> superListIterator(int index) {
//        return super.Iterator(index);
//    }

//    @Override
//    public Iterator<ClauseInfo> listIterator(int index) {
//        return new ListItr(this, index);
//    }

    private static boolean isAList(Struct t) {
        /*
         * Checks if a Struct is also a list.
         * A list can be an empty list, or a Struct with name equals to "."
         * and arity equals to 2.
         */
        return t.isEmptyList() || (t.subs() == 2 && t.name().equals("."));

    }

    // Updates indexes, storing informations about the last added clause
    void register(ClauseInfo ci, boolean first) {
        // See FamilyClausesList.get(Term): same concept
        Struct g = ci.head;

        if (g.subs() == 0) {
            return;
        }

        Term t = g.sub(0).term();
        if (t instanceof Var) {
            numCompClausesIndex.insertAsShared(ci, first);
            constantCompClausesIndex.insertAsShared(ci, first);
            structCompClausesIndex.insertAsShared(ci, first);

            if (first) {
                listCompClausesList.addFirst(ci);
            } else {
                listCompClausesList.addLast(ci);
            }
        } else if (t.isAtom()) {
            if (t instanceof Number) {
                numCompClausesIndex.insert((Number) t, ci, first);
            } else if (t instanceof Struct) {
                constantCompClausesIndex.insert(((Struct) t).name(), ci, first);
            }
        } else if (t instanceof Struct) {
            if (isAList((Struct) t)) {
                if (first) {
                    listCompClausesList.addFirst(ci);
                } else {
                    listCompClausesList.addLast(ci);
                }
            } else {
                structCompClausesIndex.insert(((Struct) t).key(), ci, first);
            }
        }

    }

    // Updates indexes, deleting informations about the last removed clause
    void unregister(ClauseInfo ci) {
        Term clause = ci.head;
        if (clause != null) {
            Struct g = (Struct) clause.term();

            if (g.subs() == 0) {
                return;
            }

            Term t = g.sub(0).term();
            if (t instanceof Var) {
                numCompClausesIndex.removeShared(ci);
                constantCompClausesIndex.removeShared(ci);
                structCompClausesIndex.removeShared(ci);

                listCompClausesList.remove(ci);
            } else if (t.isAtom()) {
                if (t instanceof Number) {
                    numCompClausesIndex.remove((Number) t, ci);
                } else if (t instanceof Struct) {
                    constantCompClausesIndex.remove(((Struct) t).name(), ci);
                }
            } else if (t instanceof Struct) {
                if (t.isList()) {
                    listCompClausesList.remove(ci);
                } else {
                    structCompClausesIndex.remove(((Struct) t).key(), ci);
                }
            }
        }
    }

//    private static class ListItr implements ListIterator<ClauseInfo> {
//
//        private final ListIterator<ClauseInfo> it;
//        private final Deque<ClauseInfo> l;
//        private int currentIndex;
//
//        public ListItr(FamilyClausesList list, int index) {
//            l = list;
//            it = list.superListIterator(index);
//        }
//
//        @Override
//        public boolean hasNext() {
//            return it.hasNext();
//        }
//
//        @Override
//        public ClauseInfo next() {
//            // Alessandro Montanari - alessandro.montanar5@studio.unibo.it
//            currentIndex = it.nextIndex();
//
//            return it.next();
//        }
//
//        @Override
//        public boolean hasPrevious() {
//            return it.hasPrevious();
//        }
//
//        @Override
//        public ClauseInfo previous() {
//            // Alessandro Montanari - alessandro.montanar5@studio.unibo.it
//            currentIndex = it.previousIndex();
//
//            return it.previous();
//        }
//
//        @Override
//        public int nextIndex() {
//            return it.nextIndex();
//        }
//
//        @Override
//        public int previousIndex() {
//            return it.previousIndex();
//        }
//
//        @Override
//        public void remove() {
//            throw new UnsupportedOperationException();
//        }
//
//        ////        @Override
//////        public void remove() {
//////            // Alessandro Montanari - alessandro.montanar5@studio.unibo.it
//////            ClauseInfo ci = l.get(currentIndex--);
//////
//////            it.remove();
//////
//////            unregister(ci);
//////        }
//
//        @Override
//        public void set(ClauseInfo o) {
//            it.set(o);
//            //throw new UnsupportedOperationException("Not supported.");
//        }
//
//        @Override
//        public void add(ClauseInfo o) {
//            l.add(o);
//        }
//
////		public ListIterator<ClauseInfo> getIt(){
////			return this;
////		}
//
//
//    }


}


