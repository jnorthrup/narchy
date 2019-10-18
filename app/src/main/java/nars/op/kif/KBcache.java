/** This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the termsof the GNU
license.  This software is released under the GNU Public License
<http:
by use of this code, to credit Articulate Software and Teknowledge in any
writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.  Please
cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. see also
http:

Note that this class, and therefore, Sigma, depends upon several terms
being present in the ontology in order to function as intended.  They are:
  domain
  domainSubclass
  Entity
  instance
  Relation
  subclass
  subrelation
  TransitiveRelation
*/

/*************************************************************************************************/

package nars.op.kif;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KBcache implements Serializable {

    public KB kb = null;

    public static boolean debug = false;

    
    public static final String _cacheFileSuffix = "_Cache.kif";

    
    public HashSet<String> relations = new HashSet<>();

    
    public HashSet<String> transRels = new HashSet<>();

    
    public HashSet<String> instTransRels = new HashSet<>();

    /** All the cached "parent" relations of all transitive relations
     * meaning the relations between all first arguments and the
     * transitive closure of second arguments.  The external HashMap
     * pairs relation name String keys to values that are the parent
     * relationships.  The interior HashMap is the set of terms and
     * their transitive closure of parents.
     */
    public HashMap<String, HashMap<String, HashSet<String>>> parents =
        new HashMap<>();

    /** Parent relations from instances, including those that are
     * transitive through (instance,instance) relations, such as
     * subAttribute and subrelation
     */
    public HashMap<String, HashSet<String>> instances =
        new HashMap<>();

    /** A temporary list of instances built during creation of the
     * children map, in order to efficiently create the instances map
     **/
    public HashSet<String> insts = new HashSet<>();

    /** All the cached "child" relations of all transitive relations
     * meaning the relations between all first arguments and the
     * transitive closure of second arguments.  The external HashMap
     * pairs relation name String keys to values that are the child
     * relationships.  The interior HashMap is the set of terms and
     * their transitive closure of children.
     */
    public HashMap<String, HashMap<String, HashSet<String>>> children =
        new HashMap<>();

    /** Relation name keys and argument types with 0th arg always ""
     * except in the case of Functions where the 0th arg will be the
     * function range.
     * Variable arity relations may have a type for the last argument,
     * which will be the type repeated for all extended arguments.
     * Note that types can be functions, rather than just terms. Note that
     * types (when there's a domainSubclass etc) are designated by a
     * '+' appended to the class name.
     **/
    public HashMap<String, ArrayList<String>> signatures =
        new HashMap<>();

    
    public HashMap<String, Integer> valences = new HashMap<>();

    /** Disjoint relations which were explicitly defined in "partition", "disjoint",
     * "disjointDecomposition" and "exhaustiveDecomposition" expressions
     **/
    public HashMap<String, HashSet<String>> explicitDisjointRelations = new HashMap<>();

    /****************************************************************
     * empty constructor for testing only
     */
    public KBcache() {

    }

    /****************************************************************
     */
    public KBcache(KB kb) {

        this.kb = kb;
    }

    /****************************************************************
     */
    public KBcache(KBcache kbCacheIn, KB kbIn) {

        this.kb = kbIn;
        if (kbCacheIn.relations != null) {
            this.relations = Sets.newHashSet(kbCacheIn.relations);
        }
        if (kbCacheIn.transRels != null) {
            this.transRels = Sets.newHashSet(kbCacheIn.transRels);
        }
        if (kbCacheIn.instTransRels != null) {
            this.instTransRels = Sets.newHashSet(kbCacheIn.instTransRels);
        }
        if (kbCacheIn.parents != null) {
            for (Map.Entry<String, HashMap<String, HashSet<String>>> outerEntry : kbCacheIn.parents.entrySet()) {
                String outerKey = outerEntry.getKey();

                HashMap<String, HashSet<String>> newInnerMap = Maps.newHashMap();
                HashMap<String, HashSet<String>> oldInnerMap = outerEntry.getValue();
                for (Map.Entry<String, HashSet<String>> innerEntry : oldInnerMap.entrySet()) {
                    String innerKey = innerEntry.getKey();

                    HashSet newInnerSet = Sets.newHashSet(innerEntry.getValue());
                    newInnerMap.put(innerKey, newInnerSet);
                }
                this.parents.put(outerKey, newInnerMap);
            }
        }
        if (kbCacheIn.instances != null) {
            for (Map.Entry<String, HashSet<String>> entry : kbCacheIn.instances.entrySet()) {
                String key = entry.getKey();
                HashSet<String> newSet = Sets.newHashSet(entry.getValue());
                this.instances.put(key, newSet);
            }
        }
        if (kbCacheIn.insts != null) {
            this.insts = Sets.newHashSet(kbCacheIn.insts);
        }
        if (kbCacheIn.children != null) {
            for (Map.Entry<String, HashMap<String, HashSet<String>>> outerEntry : kbCacheIn.children.entrySet()) {
                String outerKey = outerEntry.getKey();

                HashMap<String, HashSet<String>> newInnerMap = Maps.newHashMap();
                HashMap<String, HashSet<String>> oldInnerMap = outerEntry.getValue();
                for (Map.Entry<String, HashSet<String>> innerEntry : oldInnerMap.entrySet()) {
                    String innerKey = innerEntry.getKey();

                    HashSet newInnerSet = Sets.newHashSet(innerEntry.getValue());
                    newInnerMap.put(innerKey, newInnerSet);
                }
                this.children.put(outerKey, newInnerMap);
            }
        }

        if (kbCacheIn.signatures != null) {
            for (Map.Entry<String, ArrayList<String>> entry : kbCacheIn.signatures.entrySet()) {
                String key = entry.getKey();
                ArrayList<String> newSet = Lists.newArrayList(entry.getValue());
                this.signatures.put(key, newSet);
            }
        }
        if (kbCacheIn.valences != null) {
            this.valences = Maps.newHashMap(kbCacheIn.valences);
        }
        if (kbCacheIn.explicitDisjointRelations != null) {
            for (Map.Entry<String, HashSet<String>> entry : kbCacheIn.explicitDisjointRelations.entrySet()) {
                String key = entry.getKey();
                HashSet<String> newSet = Sets.newHashSet(entry.getValue());
                this.explicitDisjointRelations.put(key, newSet);
            }
        }
    }

    /**************************************************************
     * An ArrayList utility method
     */
    public int getArity(String rel) {

        return valences.get(rel);
    }

    /** ***************************************************************
     * An ArrayList utility method
     */
    private static void arrayListReplace(List<String> al, int index, String newEl) {
        
        if (index > al.size()) {
            System.out.println("Error in KBcache.arrayListReplace(): index " + index +
                    " out of bounds.");
            return;
        }
        al.remove(index);
        al.add(index,newEl);
    }
    
    /** ***************************************************************
     * Find whether the given child has the given parent for the given
     * transitive relation.  Return false if they are equal
     */
    public boolean childOfP(String rel, String parent, String child) {

        if (debug) System.out.println("INFO in KBcache.childOfP(): relation, parent, child: "
                + rel + ' ' + parent + ' ' + child);
        if (parent.equals(child)) {
            return false;
        }
        HashMap<String,HashSet<String>> childMap = children.get(rel);
        if (childMap == null)
            return false;
        HashSet<String> childSet = childMap.get(parent);
        if (debug) System.out.println("INFO in KBcache.childOfP(): children of " + parent + " : " + childSet);
        if (childSet == null) {
        	if (debug) System.out.println("INFO in KBcache.childOfP(): null childset for relation, parent, child: "
                + rel + ' ' + parent + ' ' + child);
        	return false;
        }
        if (debug) System.out.println("INFO in KBcache.childOfP(): child setAt contains " + child + " : " + childSet.contains(child));
        return childSet.contains(child);
    }

    /** *************************************************************
     * Returns true if i is an instance of c, else returns false.
     *
     * @param i A String denoting an instance.
     * @param c A String denoting a Class.
     * @return true or false.
     */
    public boolean isInstanceOf(String i, String c) {

        if (instances.containsKey(i)) {
            HashSet<String> hashSet = instances.get(i);
            return hashSet.contains(c);
        }
        else
            return false;
    }

    /** ***************************************************************
     * Find whether the given instance has the given parent class.  
     * Include paths the have transitive relations between instances such
     * as an Attribute that is a subAttribute of another instance, which
     * in turn then is an instance of the given class.
     * Return false if they are equal.
     */
    public boolean transInstOf(String child, String parent) {
    
        HashSet<String> prents = instances.get(child);
        if (prents != null)
            return prents.contains(parent);
        else
            return false;
    }
    
    /** ***************************************************************
     * Find whether the given class has the given parent class.  
     */
    public boolean subclassOf(String child, String parent) {
    
    	HashMap<String,HashSet<String>> prentsForRel = parents.get("subclass");
    	if (prentsForRel != null) {
	    	HashSet<String> prents = prentsForRel.get(child);
	        if (prents != null)
	            return prents.contains(parent);
	        else
	            return false;
	    	}
    	return false;
    }

    /** ***************************************************************
     * Find whether the given class is the subAttribute of the given parent class.
     */
    public boolean subAttributeOf(String child, String parent) {

        HashMap<String,HashSet<String>> prentsForRel = parents.get("subAttribute");
        if (prentsForRel != null) {
            HashSet<String> prents = prentsForRel.get(child);
            if (prents != null)
                return prents.contains(parent);
            else
                return false;
        }
        return false;
    }

    /** ***************************************************************
     * Record instances and their explicitly defined parent classes
     */
    public void buildDirectInstances() {
    	
        ArrayList<Formula> forms = kb.ask("arg", 0, "instance");
        for (Formula f : forms) {
            String child = f.getArgument(1);
            String parent = f.getArgument(2);
            HashMap<String, HashSet<String>> superclasses = parents.get("subclass");
            HashSet<String> iset = new HashSet<>();
            if (instances.get(child) != null)
                iset = instances.get(child);
            iset.add(parent);
            if (superclasses != null && superclasses.get(parent) != null)
                iset.addAll(superclasses.get(parent));
            instances.put(child, iset);
        }
    }

    /** ***************************************************************
     * build a disjoint-relations-map which were explicitly defined in
     * "partition", "exhaustiveDecomposition", "disjointDecomposition"
     * and "disjoint" expressions;
     */
    public void buildDisjointRelationsMap() {

        List<Formula> explicitDisjontFormulae = new ArrayList<>();
        explicitDisjontFormulae.addAll(kb.ask("arg", 0, "partition"));
        explicitDisjontFormulae.addAll(kb.ask("arg", 0, "disjoint"));
        explicitDisjontFormulae.addAll(kb.ask("arg", 0, "disjointDecomposition"));
        explicitDisjontFormulae.addAll(kb.ask("arg", 0, "exhaustiveDecomposition"));
        for (Formula f : explicitDisjontFormulae) {
            ArrayList<String> arguments = f.argumentsToArrayList(0);

            if (arguments != null && !arguments.isEmpty()) {
                int i = 2;
                if ("disjoint".equals(f.getArgument(0))) {
                    i = 1;
                }
                for ( ; i < arguments.size(); i++) {
                    String key = arguments.get(i);
                    int j = 2;
                    if ("disjoint".equals(f.getArgument(0)))
                        j = 1;
                    for ( ; j < arguments.size(); j++) {
                        if (j != i) {
                            String val = arguments.get(j);
                            if (!explicitDisjointRelations.containsKey(key)) {
                                HashSet<String> vals = new HashSet<>();
                                vals.add(val);
                                explicitDisjointRelations.put(key, vals);
                            }
                            else {
                                HashSet<String> vals = explicitDisjointRelations.get(key);
                                vals.add(val);
                                explicitDisjointRelations.put(key, vals);
                            }
                        }
                    }
                }
            }
        }
    }

    /** ***************************************************************
     * check if there are any two types in typeSet are disjoint or not;
     */
    public static boolean checkDisjoint(KB kb, HashSet<String> typeSet) {

        ArrayList<String> typeList = new ArrayList<>(typeSet);
        int size = typeList.size();
        for (int i = 0; i < size; i++) {
            String rel1 = typeList.get(i);
            for (int i1 = i + 1; i1 < size; i1++) {
                String rel2 = typeList.get(i1);
                if (checkDisjoint(kb, rel1, rel2) == true) {
                    return true;
                }
            }
        }
        return false;
    }

    /** ***************************************************************
     * check if rel1 and rel2 are disjoint
     * return true if rel1 and rel2 are disjoint; otherwise return false.
     */
    public static boolean checkDisjoint(KB kb, String rel1, String rel2) {

        HashSet<String> ancestors_rel1 = kb.kbCache.getParentClasses(rel1);
        HashSet<String> ancestors_rel2 = kb.kbCache.getParentClasses(rel2);
        if (ancestors_rel1 == null || ancestors_rel2 == null)
            return false;

        ancestors_rel1.add(rel1);
        ancestors_rel2.add(rel2);
        for (String s1 : ancestors_rel1) {
            for (String s2 : ancestors_rel2) {
                if (KBcache.isExplicitDisjoint(kb.kbCache.explicitDisjointRelations, s1, s2)) {
                    if (debug)
                        System.out.println(rel1 + " and " + rel2 +
                                " are disjoint relations, because of " + s1 + " and " + s2);
                    return true;
                }
            }
        }
        return false;
    }

    /** ***************************************************************
     * return true if rel1 and rel2 are explicitly defined as disjoint
     * relations; otherwise return false.
     */
    public static boolean isExplicitDisjoint(Map<String, HashSet<String>> explicitDisjointRelations,
                                             String rel1, String rel2) {

        if (explicitDisjointRelations.containsKey(rel1)) {
            return explicitDisjointRelations.get(rel1).contains(rel2);
        }
        else if (explicitDisjointRelations.containsKey(rel2)) {
            return explicitDisjointRelations.get(rel2).contains(rel1);
        }
        else
            return false;
    }

    /** ***************************************************************
     * Cache whether a given instance has a given parent class.  
     * Include paths the have transitive relations between instances such
     * as an Attribute that is a subAttribute of another instance, which
     * in turn then is an instance of the given class.
     * TODO: make sure that direct instances are recorded too
     */
    public void buildTransInstOf() {

        for (String child : insts) {
            ArrayList<Formula> forms = kb.ask("arg", 1, child);
            for (Formula f : forms) {
                String rel = f.getArgument(0);
                if (instTransRels.contains(rel) && !"subclass".equals(rel)) {
                    HashMap<String, HashSet<String>> prentList = parents.get(rel);
                    if (prentList != null) {
                        HashSet<String> prents = prentList.get(f.getArgument(1));
                        if (prents != null) {
                            for (String p : prents) {
                                ArrayList<Formula> forms2 = kb.askWithRestriction(0, "instance", 1, p);
                                for (Formula f2 : forms2) {
                                    String cl = f2.getArgument(2);
                                    HashMap<String, HashSet<String>> superclasses = parents.get("subclass");
                                    HashSet<String> pset = new HashSet<>();
                                    if (instances.get(child) != null)
                                        pset = instances.get(child);
                                    pset.add(cl);
                                    pset.addAll(superclasses.get(cl));
                                    instances.put(child, pset);
                                }
                            }
                        }
                    }
                } else if ("instance".equals(rel)) {
                    if ("exhaustiveAttribute".equals(child))
                        System.out.println("INFO in KBcache.buildTransInstOf(): f: " + f);
                    String cl = f.getArgument(2);
                    HashMap<String, HashSet<String>> superclasses = parents.get("subclass");
                    HashSet<String> iset = new HashSet<>();
                    if (instances.get(child) != null)
                        iset = instances.get(child);
                    iset.add(cl);
                    iset.addAll(superclasses.get(cl));
                    instances.put(child, iset);
                }
            }
        }
        buildDirectInstances();
    }

    /** ***************************************************************
     * @return the most specific parent of a set of classes
     */
    public String mostSpecificParent(Set<String> p1) {

        HashMap<String,HashSet<String>> subclasses = children.get("subclass");
        SortedSet<AVPair> countIndex = new TreeSet<>();
        for (String cl : p1) {
            HashSet<String> classes = subclasses.get(cl);
            int count = classes.size();
            String countString = Integer.toString(count);
            countString = StringUtil.fillString(countString, '0', 10, true);
            AVPair avp = new AVPair(countString, cl);
            countIndex.add(avp);
        }
        return countIndex.first().value;
    }

    /** ***************************************************************
     * @return the most specific parent of the two parameters or null if
     * there is no common parent.  TODO: Take into
     * account that there are instances, classes, relations, and attributes,
     */
    public String getCommonParent(String t1, String t2) {

        HashSet<String> p1 = new HashSet<>();
        Collection<String> p2 = new HashSet<>();
        if (kb.isInstance(t1)) {
            HashSet<String> temp = getParentClassesOfInstance(t1);
            if (temp != null)
                p1.addAll(temp);
        }
        else {
            HashSet<String> temp = getParentClasses(t1);
            if (temp != null)
                p1.addAll(temp);
        }
        if (kb.isInstance(t2)) {
            HashSet<String> temp = getParentClassesOfInstance(t2);
            if (temp != null)
                p2.addAll(temp);
        }
        else {
            HashSet<String> temp = getParentClasses(t2);
            if (temp != null)
                p2.addAll(temp);
        }
        p1.retainAll(p2);
        if (p1.isEmpty())
            return null;
        if (p1.size() == 1)
            return p1.iterator().next();

        return mostSpecificParent(p1);
    }

    /** ***************************************************************
     * return parent classes for the given cl from subclass expressions.
     */
    public HashSet<String> getParentClasses(String cl) {
        
        HashMap<String,HashSet<String>> ps = parents.get("subclass");
        if (ps != null)
            return ps.get(cl);
        else
            return null;
    }
    
    /** ***************************************************************
     * return child classes for the given cl from subclass expressions.
     */
    public HashSet<String> getChildClasses(String cl) {
        
        HashMap<String,HashSet<String>> ps = children.get("subclass");
        if (ps != null)
            return ps.get(cl);
        else
            return null;
    }

    /** ***************************************************************
     * return child target for the given cl from rel expressions.
     */
    public HashSet<String> getChildTerms(String cl, String rel) {

        HashMap<String,HashSet<String>> ps = children.get(rel);
        if (ps != null)
            return ps.get(cl);
        else
            return null;
    }

    /** ***************************************************************
     * return child classes for the given cl from subclass expressions.
     */
    public HashSet<String> getChildInstances(String cl) {

        HashSet<String> result = new HashSet<>();
        HashMap<String,HashSet<String>> ps = children.get("subclass");
        if (ps != null && ps.values() != null) {
            for (String cc : ps.get(cl)) {
                HashSet<String> insts = getInstancesForType(cc);
                if (insts != null)
                    result.addAll(insts);
            }
            return result;
        }
        else
            return null;
    }

    /** ***************************************************************
     * return classes for the given instance cl.
     *
     * For example, if we know (instance UnitedStates Nation), then
     * getParentClassesOfInstances(UnitedStates) returns Nation and its
     * super claasses from subclass expressions.
     */
    public HashSet<String> getParentClassesOfInstance(String cl) {
        
        HashSet<String> ps = instances.get(cl);
        if (ps != null)
            return ps;
        else
            return new HashSet<>();
    }

    /** ***************************************************************
     * Get all instances for the given input class
     *
     * For example, given the class "Nation", getInstancesForType(Nation)
     * returns all instances, like "America", "Austria", "Albania", etc.
     */
    public HashSet<String> getInstancesForType(String cl) {

        HashSet<String> instancesForType = new HashSet<>();
        for (Map.Entry<String, HashSet<String>> entry : instances.entrySet()) {
            HashSet<String> parents = entry.getValue();
            if (parents.contains(cl))
                instancesForType.add(entry.getKey());
        }
        return instancesForType;
    }

    /** ***************************************************************
     * Get the HashSet of the given arguments from an ArrayList of Formulas.
     */
    public static HashSet<String> collectArgFromFormulas(int arg, ArrayList<Formula> forms) {

        HashSet<String> subs = new HashSet<>();
        for (Formula f : forms) {
            String argument = f.getArgument(arg);
            subs.add(argument);
        }
        return subs;
    }
   
    /** ***************************************************************
     * Do a proper search for relations (including Functions), utilizing
     * the formal definitions, rather than the convention of initial
     * lower case letter.  This means getting any instance of Relation
     * tracing back through subclasses as well.
     */
    public void buildTransitiveRelationsSet() {
        
        HashSet<String> rels = new HashSet<>();
        rels.add("TransitiveRelation");
        while (!rels.isEmpty()) {
            HashSet<String> relSubs = new HashSet<>();
            for (String rel : rels) {
                relSubs = new HashSet<>();
                ArrayList<Formula> forms = kb.askWithRestriction(0, "subclass", 2, rel);

                if (forms != null) {
                    if (debug)
                        System.out.println("INFO in KBcache.buildTransitiveRelationsSet(): subclasses: " + forms);
                    relSubs.addAll(collectArgFromFormulas(1, forms));
                } else if (debug)
                    System.out.println("INFO in KBcache.buildTransitiveRelationsSet(): no subclasses for : " + rels);
                forms = kb.askWithRestriction(0, "instance", 2, rel);
                if (forms != null)
                    transRels.addAll(collectArgFromFormulas(1, forms));
                forms = kb.askWithRestriction(0, "subrelation", 2, rel);
                if (forms != null)
                    transRels.addAll(collectArgFromFormulas(1, forms));
            }
            rels = new HashSet<>(relSubs);
        }
    }
    
    /** ***************************************************************
     * Do a proper search for relations (including Functions), utilizing
     * the formal definitions, rather than the convention of initial
     * lower case letter.  This means getting any instance of Relation
     * tracing back through subclasses as well.
     */
    public void buildRelationsSet() {
        
        HashSet<String> rels = new HashSet<>();
        rels.add("Relation");
        while (!rels.isEmpty()) {
            Collection<String> relSubs = new HashSet<>();
            for (String rel : rels) {
                ArrayList<Formula> forms = kb.askWithRestriction(0, "subclass", 2, rel);
                if (forms != null)
                    relSubs.addAll(collectArgFromFormulas(1, forms));

                forms = kb.askWithRestriction(0, "instance", 2, rel);
                if (forms != null) {
                    relations.addAll(collectArgFromFormulas(1, forms));
                    relSubs.addAll(collectArgFromFormulas(1, forms));
                }
                forms = kb.askWithRestriction(0, "subrelation", 2, rel);
                if (forms != null) {
                    relations.addAll(collectArgFromFormulas(1, forms));
                    relSubs.addAll(collectArgFromFormulas(1, forms));
                }
            }
            rels = new HashSet<>(relSubs);
        }
    }

    /** ***************************************************************
     * Find the parent "roots" of any transitive relation - terms that
     * appear only as argument 2
     */
    private HashSet<String> findRoots(String rel) {

        ArrayList<Formula> forms = kb.ask("arg",0,rel);
        HashSet<String> arg1s = collectArgFromFormulas(1,forms);
        HashSet<String> arg2s = collectArgFromFormulas(2,forms);
        arg2s.removeAll(arg1s);
        HashSet<String> result = new HashSet<>(arg2s);
        return result;
    }
    
    /** ***************************************************************
     * Find the child "roots" of any transitive relation - terms that
     * appear only as argument 1
     */
    private HashSet<String> findLeaves(String rel) {

        ArrayList<Formula> forms = kb.ask("arg",0,rel);
        HashSet<String> arg1s = collectArgFromFormulas(1,forms);
        HashSet<String> arg2s = collectArgFromFormulas(2,forms);
        arg1s.removeAll(arg2s);
        HashSet<String> result = new HashSet<>(arg1s);
        return result;
    }
    
    /** ***************************************************************
     * Build "parent" relations based on breadth first search algorithm.
     */
    private void breadthFirstBuildParents(String root, String rel) {
        
        HashMap<String,HashSet<String>> relParents = parents.get(rel);
        if (relParents == null) {
            System.out.println("Error in KBcache.breadthFirstBuildParents(): no relation " + rel);
            return;
        }
        int threshold = 10;      
        HashMap<String, Integer> appearanceCount = new HashMap<>();  
        ArrayDeque<String> Q = new ArrayDeque<>();
        Q.add(root);
        while (!Q.isEmpty()) {
            String t = Q.remove();
            
            ArrayList<Formula> forms = kb.askWithRestriction(0,rel,2,t);
            if (forms != null) {
                HashSet<String> relSubs = collectArgFromFormulas(1,forms);

                for (String newTerm : relSubs) {
                    HashSet<String> oldParents = relParents.computeIfAbsent(t, k -> new HashSet<>());
                    HashSet<String> newParents = new HashSet<>(oldParents);
                    newParents.add(t);
                    HashSet<String> newTermParents = relParents.get(newTerm);
                    if (newTermParents != null)
                        newParents.addAll(newTermParents);
                    relParents.put(newTerm, newParents);

                    if (appearanceCount.get(newTerm) == null) {
                        appearanceCount.put(newTerm, 1);
                        Q.addFirst(newTerm);
                    } else if (appearanceCount.get(newTerm) <= threshold) {
                        appearanceCount.put(newTerm, appearanceCount.get(newTerm) + 1);
                        Q.addFirst(newTerm);
                    }
                }
            }
        }
    }
    
    /** ***************************************************************
     * Build "children" relations based on breadth first search algorithm.
     */
    private void breadthFirstBuildChildren(String root, String rel) {
        
        HashMap<String,HashSet<String>> relChildren = children.get(rel);
        if (relChildren == null) {
            System.out.println("Error in KBcache.breadthFirstBuildChildren(): no relation " + rel);
            return;
        }
        if (debug) System.out.println("INFO in KBcache.breadthFirst(): trying relation " + rel);
        ArrayDeque<String> Q = new ArrayDeque<>();
        Set<String> V = new HashSet<>();
        Q.add(root);
        V.add(root);
        while (!Q.isEmpty()) {
            String t = Q.remove();
            if (debug) System.out.println("visiting " + t);
            ArrayList<Formula> forms = kb.askWithRestriction(0,rel,1,t);
            if (forms != null) {
                HashSet<String> relSubs = collectArgFromFormulas(2,forms);
                if (debug) System.out.println("visiting subs of t: " + relSubs);
                for (String newTerm : relSubs) {
                    HashSet<String> oldChildren = relChildren.computeIfAbsent(t, k -> new HashSet<>());
                    HashSet<String> newChildren = new HashSet<>(oldChildren);
                    newChildren.add(t);
                    HashSet<String> newTermChildren = relChildren.get(newTerm);
                    if (newTermChildren != null)
                        newChildren.addAll(newTermChildren);
                    relChildren.put(newTerm, newChildren);
                    if (!V.contains(newTerm)) {
                        V.add(newTerm);
                        Q.addFirst(newTerm);
                    }
                }
            }
        }
        insts.addAll(relChildren.keySet());
    }
    
    /** ***************************************************************
     * For each transitive relation, find its transitive closure.  If
     * rel is transitive, and (rel A B) and (rel B C) then the entry for
     * rel is a HashMap where the key A has value ArrayList of {B,C}.
     */
    public void buildParents() {

        for (String rel : transRels) {
            HashMap<String, HashSet<String>> value = new HashMap<>();
            HashSet<String> roots = findRoots(rel);
            parents.put(rel, value);
            for (String root : roots) {
                breadthFirstBuildParents(root, rel);
            }
        }
    }

    /** ***************************************************************
     * For each transitive relation, find its transitive closure.  If
     * rel is transitive, and (rel A B) and (rel B C) then the entry for
     * rel is a HashMap where the key A has value ArrayList of {B,C}.
     */
    public void buildChildren() {

        for (String rel : transRels) {
            HashMap<String, HashSet<String>> value = new HashMap<>();
            HashSet<String> leaves = findLeaves(rel);
            children.put(rel, value);
            for (String root : leaves) {
                breadthFirstBuildChildren(root, rel);
            }
        }
    }
    
    /** ***************************************************************
     * Fill an array of String with the specified String up to but
     * not including the index, starting from the 1st argument and
     * ignoring the 0th argument.
     */
    private static void fillArray(String st, String[] ar, int start, int end) {
    
        for (int i = start; i < end; i++) 
            if (StringUtil.emptyString(ar[i]))
                ar[i] = st;        
    }
    
    /** ***************************************************************
     * Fill an array of String with the specified String up to but
     * not including the index, starting from the end of the array
     */
    private static void fillArrayList(String st, List<String> ar, int start, int end) {
    
        for (int i = start; i < end; i++) 
            if (i > ar.size()-1 || StringUtil.emptyString(ar.get(i)))
                ar.add(st);        
    }
    
    /** ***************************************************************
     * Build the argument type list for every relation. If the argument
     * is a domain subclass, append a "+" to the argument type.  If
     * no domain is defined for the given relation and argument position,
     * inherit it from the parent.  If there is no argument type, send
     * an error to the Sigma error list.
     * Relation name keys and argument types with 0th arg always "" except
     *   for functions which will have the range type as their 0th argument
     * public HashMap<String,ArrayList<String>> signatures =
     *      new HashMap<String,ArrayList<String>>();
     */
    public void collectDomains() {

        for (String rel : relations) {
            String[] domainArray = new String[Formula.MAX_PREDICATE_ARITY];
            int maxIndex = 0;
            domainArray[0] = "";
            ArrayList<Formula> forms = kb.askWithRestriction(0, "domain", 1, rel);
            if (forms != null) {
                for (Formula form : forms) {
                    int arg = Integer.parseInt(form.getArgument(2));
                    String type = form.getArgument(3);
                    domainArray[arg] = type;
                    if (arg > maxIndex)
                        maxIndex = arg;
                }
            }

            forms = kb.askWithRestriction(0, "domainSubclass", 1, rel);
            if (forms != null) {
                for (Formula form : forms) {
                    int arg = Integer.parseInt(form.getArgument(2));
                    String type = form.getArgument(3);
                    domainArray[arg] = type + '+';
                    if (arg > maxIndex)
                        maxIndex = arg;
                }
            }

            forms = kb.askWithRestriction(0, "range", 1, rel);
            if (forms != null) {
                if (forms.size() > 1)
                    System.out.println("Warning in KBcache.collectDomains(): more than one range statement" + forms);
                for (Formula form : forms) {
                    String type = form.getArgument(2);
                    domainArray[0] = type;
                }
            }

            forms = kb.askWithRestriction(0, "rangeSubclass", 1, rel);
            if (forms != null) {
                if (forms.size() > 1)
                    System.out.println("Warning in KBcache.collectDomains(): more than one rangeSubclass statement" + forms);
                for (Formula form : forms) {
                    String type = form.getArgument(2);
                    domainArray[0] = type + '+';
                }
            }

            fillArray("Entity", domainArray, 1, maxIndex);
            ArrayList<String> domains = new ArrayList<>(Arrays.asList(domainArray).subList(0, maxIndex + 1));
            signatures.put(rel, domains);
            valences.put(rel, maxIndex);
        }
        inheritDomains();
    }
    
    /** ***************************************************************
     * Note that this routine forces child relations to have arguments
     * that are the same or more specific than their parent relations.
     */
    private void breadthFirstInheritDomains(String root) {
        
        String rel = "subrelation";
        HashMap<String,HashSet<String>> relParents = parents.get("subrelation");
        if (relParents == null) {
            System.out.println("Error in KBcache.breadthFirstInheritDomains(): no relation subrelation");
            return;
        }
        ArrayDeque<String> Q = new ArrayDeque<>();
        Set<String> V = new HashSet<>();
        Q.add(root);
        V.add(root);
        while (!Q.isEmpty()) {
            String t = Q.remove();
            ArrayList<String> tdomains = signatures.get(t);
            ArrayList<Formula> forms = kb.askWithRestriction(0,rel,2,t);
            if (forms != null) {
                HashSet<String> relSubs = collectArgFromFormulas(1,forms);
                for (String newTerm : relSubs) {
                    ArrayList<String> newDomains = signatures.get(newTerm);
                    if (valences.get(t) == null) {
                        System.out.println("Error in KBcache.breadthFirstInheritDomains(): no valence for " + t);
                        continue;
                    } else if (valences.get(newTerm) == null || valences.get(newTerm) < valences.get(t)) {
                        fillArrayList("Entity", newDomains, valences.get(newTerm) + 1, valences.get(t) + 1);
                        valences.put(newTerm, valences.get(t));
                    }
                    for (int i = 1; i < valences.get(t); i++) {
                        String childArgType = newDomains.get(i);
                        String parentArgType = tdomains.get(i);


                        if (kb.askWithTwoRestrictions(0, "domain", 1, newTerm, 3, childArgType).isEmpty()) {
                            arrayListReplace(newDomains, i, parentArgType);
                        }
                    }
                    if (!V.contains(newTerm)) {
                        V.add(newTerm);
                        Q.addFirst(newTerm);
                    }
                }
            }
        }
    }
    
    /** *************************************************************
     * Delete and writes the cache .kif file then call addConstituent() so
     * that the file can be processed and loaded by the inference engine.
     */
    public void writeCacheFile() {
                           
        FileWriter fw = null;
        try {
            File dir = new File(KBmanager.manager.getPref("kbDir"));
            File f = new File(dir, (kb.name + _cacheFileSuffix));
            System.out.println("INFO in KBcache.writeCacheFile(): " + f.getName());
            if (f.exists()) 
                f.delete();                                           
            String filename = f.getCanonicalPath();
            fw = new FileWriter(f, true);
            Iterator<String> it = parents.keySet().iterator();
            while (it.hasNext()) {
                String rel = it.next();
                HashMap<String,HashSet<String>> valSet = parents.get(rel);
                for (String child : valSet.keySet()) {
                    HashSet<String> prents = valSet.get(child);
                    for (String parent : prents) {
                        String tuple = '(' + rel + ' ' + child + ' ' + parent + ')';
                        if (!kb.formulaMap.containsKey(tuple)) {
                            fw.write(tuple);
                            fw.write(System.getProperty("line.separator"));
                        }
                    }
                }                
            }
            it = instances.keySet().iterator();
            while (it.hasNext()) {
                String inst = it.next();
                HashSet<String> valSet = instances.get(inst);
                for (String parent : valSet) {
                    String tuple = "(instance " + inst + ' ' + parent + ')';
                    if (!kb.formulaMap.containsKey(tuple)) {
                        fw.write(tuple);
                        fw.write(System.getProperty("line.separator"));
                    }
                }
            }
            if (fw != null) {
                fw.close();
                fw = null;
            }
            kb.constituents.remove(filename);
            kb.addConstituent(filename);
            
            
        }                   
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (fw != null) 
                    fw.close();                
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /** ***************************************************************
     * Find domain and domainSubclass definitions that impact a child
     * relation.  If the type of an argument is less specific than
     * the same type of a parent's argument, use that of the parent.
     */
    public void inheritDomains() {
        
        HashSet<String> roots = findRoots("subrelation");
        for (String root : roots) {
            breadthFirstInheritDomains(root);
        }
    }

    /** ***************************************************************
     * Compile the set of transitive relations that are between instances
     */
    public void buildInstTransRels() {

        for (String rel : transRels) {
            boolean instrel;
            ArrayList<String> sig = signatures.get(rel);
            if (sig == null) {
                System.out.println("Error in KBcache.buildInstTransRels(): Error " + rel + " not found.");
            } else {
                boolean b = true;
                for (String s : sig) {
                    if (s.endsWith("+")) {
                        b = false;
                        break;
                    }
                }
                instrel = b;
                if (instrel)
                    instTransRels.add(rel);
            }
        }        
    }
    
    /** ***************************************************************
     * Main entry point for the class.  
     */
    public void buildCaches() {
        
        buildRelationsSet();
        buildTransitiveRelationsSet();
        buildParents();
        buildChildren(); 
        collectDomains();  
        buildInstTransRels();
        buildDirectInstances();
        buildDisjointRelationsMap(); 
        writeCacheFile();
        System.out.println("INFO in KBcache.buildCaches(): size: " + instances.keySet().size());
    }

    /** ***************************************************************
     * Copy all relevant information from a VariableArityRelation to a new
     * predicate that is a particular fixed arity.
     */
    public void copyNewPredFromVariableArity(String pred, String oldPred, int arity) {

        if (signatures.containsKey(oldPred))
            signatures.put(pred,signatures.get(oldPred));
        if (instances.containsKey(oldPred))
            instances.put(pred,instances.get(oldPred));
        valences.put(pred,arity);
    }

    /** *************************************************************
     */
    public void showState() {

        System.out.println("-------------- relations ----------------");
        Iterator<String> it = this.relations.iterator();
        while (it.hasNext())
            System.out.print(it.next() + ' ');
        System.out.println();
        
        System.out.println("-------------- transitives ----------------");
        it = this.transRels.iterator();
        while (it.hasNext())
            System.out.print(it.next() + ' ');
        System.out.println();
        System.out.println("-------------- parents ----------------");
        
        it = this.parents.keySet().iterator();
        while (it.hasNext()) {
            String rel = it.next();
            System.out.println("Relation: " + rel);
            HashMap<String,HashSet<String>> relmap = this.parents.get(rel);
            for (String term : relmap.keySet()) {
                System.out.println(term + ": " + relmap.get(term));
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("-------------- children ----------------");
        
        it = this.children.keySet().iterator();
        while (it.hasNext()) {
            String rel = it.next();
            System.out.println("Relation: " + rel);
            HashMap<String,HashSet<String>> relmap = this.children.get(rel);
            for (String term : relmap.keySet()) {
                System.out.println(term + ": " + relmap.get(term));
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("-------------- domains ----------------");

        for (String rel : this.relations) {
            ArrayList<String> domains = this.signatures.get(rel);
            System.out.println(rel + ": " + domains);
        }
        System.out.println();
        System.out.println("-------------- valences ----------------");
        for (String rel : this.valences.keySet()) {
            Integer arity = this.valences.get(rel);
            System.out.println(rel + ": " + arity);
        }
        System.out.println();
        System.out.println("-------------- insts ----------------");
        for (String inst : this.insts) {
            System.out.print(inst + ", ");
        }
        System.out.println();
        System.out.println();
        System.out.println("-------------- instances ----------------");
        for (String inst : this.instances.keySet()) {
            System.out.println(inst + ": " + this.instances.get(inst));
        }
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        KBmanager.manager.initializeOnce();
        KB kb = KBmanager.manager.getKB(KBmanager.manager.getPref("sumokbname"));
        System.out.println("**** Finished loading KB ***");
        KBcache nkbc = kb.kbCache;
        String term = "Object";
        HashSet<String> classes = nkbc.getChildClasses(term);
        HashSet<String> instances = nkbc.getChildInstances(term);
        System.out.println("number of child classes of " + term + ": " + classes.size());
        System.out.println("KBcache.main(): children of " + term + ": " +
                classes);
        System.out.println("number of instances of " + term + ": " + instances.size());
        System.out.println("KBcache.main(): instances of " + term + ": " +
               instances);
        term = "Process";
        classes = nkbc.getChildClasses(term);
        instances = nkbc.getChildInstances(term);
        System.out.println("number of classes of " + term + ": " + classes.size());
        System.out.println("KBcache.main(): children of " + term + ": " +
                classes);
        System.out.println("number of instances of " + term + ": " + instances.size());
        System.out.println("KBcache.main(): instances of " + term + ": " +
                instances);

        System.out.println("KBcache.main(): " + nkbc.getCommonParent("Kicking","Pushing"));
        /* List<Formula> forms = kb.ask("arg",0,"subrelation");
        for (Formula f : forms) {
            String rel = f.getArgument(1);
            System.out.println("is " + rel + " a relation: " + kb.isInstanceOf(rel,"Relation"));
        }
        */
    }
}
