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
  and or forall exists
  domain
  EnglishLanguage
  equal
  format
  instance
  inverse
  Predicate
  Relation
  SetOrClass
  subclass
  subrelation
  termFormat
  valence
  VariableArityRelation
*/

/*************************************************************************************************/
package nars.op.kif;

/*
Author: Adam Pease apease@articulatesoftware.com

some portions copyright Teknowledge, IPsoft

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA

Authors:
Adam Pease
Infosys LTD.
*/


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * ***************************************************************** Contains
 * methods for reading, writing knowledge bases and their configurations. Also
 * contains the inference engine process for the knowledge base.
 */
public class KB implements Serializable {

    private boolean isVisible = true;




    /** The name of the knowledge base. */
    public String name;

    /**
     * An ArrayList of Strings that are the full canonical pathnames of the
     * files that comprise the KB.
     */
    ArrayList<String> constituents = new ArrayList<>();

    /** The natural language in which axiom paraphrases should be presented. */
    public String language = "EnglishLanguage";

    /**
     * The location of preprocessed KIF files, suitable for loading into
     * EProver.
     */
    private String kbDir = null;




    /**
     * A synchronized SortedSet of Strings, which are all the terms in the KB.
     */
    public SortedSet<String> terms = Collections.synchronizedSortedSet(new TreeSet<>());

    /** The String constant that is the suffix for files of user assertions. */
    private static final String _userAssertionsString = "_UserAssertions.kif";

    /**
     * The String constant that is the suffix for files of cached assertions.
     */
    static final String _cacheFileSuffix = "_Cache.kif";

    /**
     * A Map of all the Formula objects in the KB. Each key is a String
     * representation of a Formula. Each value is the Formula object
     * corresponding to the key.
     */
    HashMap<String, Formula> formulaMap = new HashMap<>();

    /**
     * A HashMap of ArrayLists of String formulae, containing all the formulae
     * in the KB. Keys are the formula itself, a formula ID, and target indexes
     * created in KIF.createKey(). The actual formula can be retrieved by using
     * the returned String as the key for the variable formulaMap
     */
    public HashMap<String, ArrayList<String>> formulas = new HashMap<>();

    /**
     * The natural language formatting strings for relations in the KB. It is a
     * HashMap of language keys and HashMap values. The interior HashMap is target
     * name keys and String values.
     */
    private HashMap<String, HashMap<String, String>> formatMap = new HashMap<>();

    /** The natural language strings for terms in the KB. It is a HashMap of
     * language keys and HashMap values. The interior HashMap is target name keys
     * and String values.
     */
    private HashMap<String, HashMap<String, String>> termFormatMap = new HashMap<>();

    /** Errors found during loading of the KB constituents. */
    public TreeSet<String> errors = new TreeSet<>();

    /** Warnings found during loading of the KB constituents. */
    private final TreeSet<String> warnings = new TreeSet<>();

    /**
     * Future: If true, the contents of the KB have been modified without
     * updating the caches
     */
    private boolean modifiedContents = false;

    /**
     * If true, assertions of the form (predicate x x) will be included in the
     * relation cache tables.
     */
    private static final boolean cacheReflexiveAssertions = false;

    KBcache kbCache = null;

    private final Map<String, Integer> termFrequency = new HashMap<>();

    /*************************************************************** Constructor
     * which takes the name of the KB and the location where KBs preprocessed
     * for EProver should be placed.
     */
    public KB(final String n, final String dir) {

        name = n;
        kbDir = dir;
        kbCache  = new KBcache(this);












    }

    public KB(final String n, final String dir, final boolean visibility) {

        this(n, dir);
        isVisible = visibility;
    }

    /***************************************************************
     * Perform a deep copy of the kb input
     *
     * @param kbIn
     */
    public KB(final KB kbIn) {
        this.isVisible = kbIn.isVisible;





        this.name = kbIn.name;

        if (kbIn.constituents != null) {
            this.constituents = Lists.newArrayList(kbIn.constituents);
        }

        this.language = kbIn.language;

        this.kbDir = kbIn.kbDir;

        if (kbIn.terms != null) {
            this.terms = Collections.synchronizedSortedSet(new TreeSet<>(kbIn.terms));
        }

        if (kbIn.formulaMap != null) {
            for (final Map.Entry<String, Formula> pair : kbIn.formulaMap.entrySet()) {
                final String key = pair.getKey();
                final Formula newFormula = new Formula(pair.getValue());
                this.formulaMap.put(key, newFormula);
            }
        }

        if (kbIn.formulas != null) {
            for (final Map.Entry<String, ArrayList<String>> pair : kbIn.formulas.entrySet()) {
                final String key = pair.getKey();
                final ArrayList<String> newList = Lists.newArrayList(pair.getValue());
                this.formulas.put(key, newList);
            }
        }

        if (kbIn.formatMap != null) {
            this.formatMap = Maps.newHashMap(kbIn.formatMap);
        }

        if (kbIn.termFormatMap != null) {
            this.termFormatMap = Maps.newHashMap(kbIn.termFormatMap);
        }

        if (kbIn.errors != null) {
            this.errors = Sets.newTreeSet(kbIn.errors);
        }

        this.modifiedContents = kbIn.modifiedContents;

        this.kbCache = new KBcache(kbIn.kbCache, this);





    }

    public boolean isVisible() {
        return isVisible;
    }

    /***************************************************************
     * Constructor
     */
    public KB(final String n) {

        name = n;

        final KBmanager mgr = KBmanager.manager;
            kbDir = mgr.getPref("kbDir");









    }

    /**************************************************************
     * Returns a SortedSet of Strings, which are all the terms in the KB.
     */
    public SortedSet<String> getTerms() {

        return this.terms;
    }

    /****************************************************
     * REswitch determines if
     * a String is a RegEx or not based on its use of RE metacharacters.
     * "1"=nonRE, "2"=RE
     *
     * @param term
     *            A String
     * @return "1" or "2"
     */
    public static String REswitch(final String term) {

        for (final String s : Arrays.asList("(", "[", "{", "\\", "^", "$", "|", "}", "]", ")", "?", "*", "+")) {
            if (term.contains(s)) {
                return "2";
            }
        }
        return "1";
    }

    /***************************************************
     * Only called in
     * BrowseBody.jsp when a single match is found. Purpose is to simplify a
     * RegEx to its only matching target
     *
     * @param term
     *            a String
     * @return modified target a String
     */
    public String simplifyTerm(final String term) {

        if (getREMatch(term.intern()).size() == 1)
            return getREMatch(term.intern()).get(0);
        return term;
    }

    /****************************************************
     * Takes a target
     * (interpreted as a Regular Expression) and returns true if any target in the
     * KB has a match with the RE.
     *
     * @param term
     *            A String
     * @return true or false.
     */
    public boolean containsRE(final String term) {

        return (!getREMatch(term).isEmpty());
    }

    /****************************************************
     * Takes a target
     * (interpreted as a Regular Expression) and returns an ArrayList containing
     * every target in the KB that has a match with the RE.
     *
     * @param term
     *            A String
     * @return An ArrayList of terms that have a match to target
     */
    private ArrayList<String> getREMatch(final String term) {

        try {
            final Pattern p = Pattern.compile(term);
            final ArrayList<String> matchesList = new ArrayList<String>();
            for (final String t : getTerms()) {
                final Matcher m = p.matcher(t);
                if (m.matches())
                    matchesList.add(t);
            }
            return matchesList;
        }
        catch (final PatternSyntaxException ex) {
            final ArrayList<String> err = new ArrayList<String>();
            err.add("Invalid Input");
            return err;
        }
    }

    /**************************************************************
     * Sets the
     * synchronized SortedSet of all the terms in the KB to be kbTerms.
     */
    public void setTerms(final SortedSet<String> newTerms) {

        synchronized (getTerms()) {
            getTerms().clear();
            this.terms = Collections.synchronizedSortedSet(newTerms);
        }
    }

    /***************************************************************
     * Get an
     * ArrayList of Strings containing the language identifiers of available
     * natural language formatting templates.
     *
     * @return an ArrayList of Strings containing the language identifiers
     */
    ArrayList<String> availableLanguages() {

        final ArrayList<String> al = new ArrayList<String>();
        final ArrayList<Formula> col = ask("arg", 0, "format");
        final ArrayList<Formula> col2 = ask("arg", 0, "termFormat");
        if (col != null) {
            if (col2 != null)
                col.addAll(col2);
            for (final Formula f : col) {
                final String lang = f.getArgument(1);
                if (!al.contains(lang.intern()))
                    al.add(lang.intern());
            }
        }

        return al;
    }

    /***************************************************************
     * Remove from
     * the given set any item which is a superclass of another item in the setAt.
     *
     * @param set
     * @return
     */
    Set<String> removeSuperClasses(final Iterable<String> set) {

        final Set<String> returnSet = Sets.newHashSet(set);
        final Set<String> removeSet = Sets.newHashSet();

        
        for (final String first : returnSet) {
            for (final String second : returnSet) {
                if (isSubclass(first, second)) {
                    removeSet.add(second);
                }
            }
        }

        returnSet.removeAll(removeSet);
        return returnSet;
    }

    /***************************************************************
     * Arity
     * errors should already have been trapped in addConstituent() unless a
     * relation is used before it is defined. This routine is a comprehensive
     * re-check.
     */
    private void checkArity() {

        System.out.print("INFO in KB.checkArity(): Performing Arity Check");
        if (formulaMap != null && !formulaMap.isEmpty()) {
            final int counter = 0;
            final List<String> toRemove = new ArrayList<>();
            for (final String s : formulaMap.keySet()) {
                final Formula f = formulaMap.get(s);
                final String term = PredVarInst.hasCorrectArity(f, this);
                if (!StringUtil.emptyString(term)) {
                    errors.add("Formula in " + f.sourceFile + " rejected due to arity error of predicate " + term
                        + " in formula: \n" + f.theFormula);
                    toRemove.add(f.theFormula);
                }
            }
            System.out.println();
        }
        
        
    }

    /***************************************************************
     * Returns the
     * type (SUO-KIF SetOrClass name) for any argument in argPos position of an
     * assertion formed with the SUO-KIF Relation reln. If no argument type
     * value is directly stated for reln, this method tries to find a value
     * inherited from one of reln's super-relations.
     *
     * @param reln   A String denoting a SUO-KIF Relation
     * @param argPos An int denoting an argument position, where 0 is the position
     *               of reln itself
     * @return A String denoting a SUO-KIF SetOrClass, or null if no value can
     * be obtained
     */
    String getArgType(final String reln, final int argPos) {

        String className = null;
        String argType = FormulaPreprocessor.findType(argPos, reln, this);
        if (StringUtil.isNonEmptyString(argType)) {
            if (argType.endsWith("+"))
                argType = "SetOrClass";
            className = argType;
        }
        return className;
    }

    /***************************************************************
     * Returns the
     * type (SUO-KIF SetOrClass name) for any argument in argPos position of an
     * assertion formed with the SUO-KIF Relation reln. If no argument type
     * value is directly stated for reln, this method tries to find a value
     * inherited from one of reln's super-relations.
     *
     * @param reln   A String denoting a SUO-KIF Relation
     * @param argPos An int denoting an argument position, where 0 is the position
     *               of reln itself
     * @return A String denoting a SUO-KIF SetOrClass, or null if no value can
     * be obtained. A '+' is appended to the class name if the argument
     * is a subclass of the class, rather than an instance
     */
    public String getArgTypeClass(final String reln, final int argPos) {

        String className = null;
        final String argType = FormulaPreprocessor.findType(argPos, reln, this);
        if (StringUtil.isNonEmptyString(argType))
            className = argType;
        return className;
    }

    /***************************************************************
     * Determine whether a particular target is an immediate instance, which has a statement
     * of the form (instance target otherTerm). Note that this does not count for
     * terms such as Attribute(s) and Relation(s), which may be defined as
     * subAttribute(s) or subrelation(s) of another instance. If the target is not
     * an instance, return an empty ArrayList. Otherwise, return an ArrayList of
     * the Formula(s) in which the given target is defined as an instance.
     * Note! This does not return instances of the given target, but rather the
     * terms of which the given target is an instance.
     */
    ArrayList<Formula> instancesOf(final String term) {

        return askWithRestriction(1, term, 0, "instance");
    }

    /***************************************************************
     * Get all instances of a given target
     */
    public Set<String> instances(final String term) {

        final ArrayList<Formula> forms = askWithRestriction(2, term, 0, "instance");
        final Set<String> result = new HashSet<>();
        for (final Formula f : forms) {
            final String argument = f.getArgument(1);
            result.add(argument);
        }
        return result;
    }

    /***************************************************************
     * Returns
     * true if i is an instance of c, else returns false.
     *
     * @param i A String denoting an instance.
     * @param c A String denoting a Class.
     * @return true or false.
     */
    boolean isInstanceOf(final String i, final String c) {

        if (kbCache == null)
            return false;
        return kbCache.isInstanceOf(i, c);
    }

    /***************************************************************
     * Returns
     * true if i is an Attribute, else returns false.
     *
     * @param i A String denoting an possible instance of Attribute.
     * @return true or false.
     */
    public boolean isAttribute(final String i) {

        if (kbCache == null)
            return false;
        return kbCache.isInstanceOf(i, "Attribute");
    }

    /***************************************************************
     * Returns
     * true if i is c, is an instance of c, or is subclass of c, or is
     * subAttribute of c, else returns false.
     *
     * @param i A String denoting a class or instance.
     * @param c A String denoting the parent Class.
     * @return true or false.
     */
    public boolean isChildOf(final String i, final String c) {

        return i.equals(c) || isInstanceOf(i, c) || isSubclass(i, c) || isSubAttribute(i, c);
    }

    /***************************************************************
     * Returns
     * true if i is an instance of Function in any loaded KB, else returns
     * false.
     *
     * @param i A String denoting an instance.
     * @return true or false.
     */
    boolean isFunction(final String i) {

        if (kbCache != null && !StringUtil.emptyString(i)) {
            
                
                    
                    
                    








        }
        return false;
    }

    /***************************************************************
     * Returns
     * true if i is an instance of c in any loaded KB, else returns false.
     *
     * @param i A String denoting an instance.
     * @return true or false.
     */
    public static boolean isRelationInAnyKB(final String i) {

        final HashMap<String, KB> kbs = KBmanager.manager.kbs;
        if (!kbs.isEmpty()) {
            KB kb = null;
            for (final KB value : kbs.values()) {
                kb = value;
                if (kb.kbCache != null && kb.kbCache.relations != null && kb.kbCache.relations.contains(i))
                    return true;
            }
        }
        return false;
    }

    /**
     * *************************************************************
     */
    boolean isInstance(final String term) {

        final ArrayList<Formula> al = askWithRestriction(0, "instance", 1, term);
        return (al != null && !al.isEmpty());
    }

    /***************************************************************
     * Determine
     * whether a particular class or instance "child" is a child of the given
     * "parent".
     *
     * @param child  A String, the name of a target.
     * @param parent A String, the name of a target.
     * @return true if child and parent constitute an actual or implied relation
     * in the current KB, else false.
     */
    boolean childOf(final String child, final String parent) {

        if (child.equals(parent))
            return true;
        if (kbCache.transInstOf(child, parent))
            return true;
        for (final String s : Arrays.asList("instance", "subclass", "subrelation", "subAttribute")) {
            if (kbCache.childOfP(s, parent, child)) {
                return true;
            }
        }
        return false;
    }

    /***************************************************************
     * Returns
     * true if the subclass cache supports the conclusion that c1 is a subclass
     * of c2, else returns false.
     *
     * @param c1 A String, the name of a SetOrClass.
     * @param parent A String, the name of a SetOrClass.
     * @return boolean
     */
    boolean isSubclass(final String c1, final String parent) {

        if (StringUtil.emptyString(c1)) {
            System.out.println("Error in KB.isSubclass(): empty c1");
            Thread.dumpStack();
            return false;
        }
        if (StringUtil.emptyString(parent)) {
            System.out.println("Error in KB.isSubclass(): empty parent");
            Thread.dumpStack();
            return false;
        }
        if (c1.equals(parent))
            return true;
        if (StringUtil.isNonEmptyString(c1) && StringUtil.isNonEmptyString(parent))
            return kbCache.childOfP("subclass", parent, c1);
        return false;
    }

    /***************************************************************
     * Returns
     * true if the KB cache supports the conclusion that c1 is a subAttribute of
     * c2, else returns false.
     *
     * @param c1 A String, the name of a SetOrClass.
     * @param parent A String, the name of a SetOrClass.
     * @return boolean
     */
    private boolean isSubAttribute(final String c1, final String parent) {

        if (StringUtil.isNonEmptyString(c1) && StringUtil.isNonEmptyString(parent)) {
            return kbCache.childOfP("subAttribute", parent, c1);
        }
        return false;
    }

    /***************************************************************
     * Converts
     * all Formula objects in the input List to ArrayList tuples.
     *
     * @param formulaList A list of Formulas.
     * @return An ArrayList of formula tuples (ArrayLists), or an empty
     * ArrayList.
     */
    public static ArrayList<ArrayList<String>> formulasToArrayLists(final Collection<Formula> formulaList) {

        final ArrayList<ArrayList<String>> ans = new ArrayList<ArrayList<String>>();
        if (formulaList instanceof List) {
            final Iterator<Formula> it = formulaList.iterator();
            Formula f = null;
            while (it.hasNext()) {
                f = it.next();
                ans.add(f.literalToArrayList());
            }
        }
        return ans;
    }

    /* *************************************************************
     * Converts
     * all Strings in the input List to Formula objects.
     *
     * @param strings A list of Strings.
     * @return An ArrayList of Formulas, or an empty ArrayList.
     */
    public static ArrayList<Formula> stringsToFormulas(final Collection<String> strings) {

        final ArrayList<Formula> ans = new ArrayList<Formula>();
        if (strings instanceof List) {
            for (final String string : strings) {
                final Formula f = new Formula();
                f.read(string);
                ans.add(f);
            }
        }
        return ans;
    }

    /***************************************************************
     * Converts a
     * literal (List object) to a String.
     *
     * @param literal A List representing a SUO-KIF formula.
     * @return A String representing a SUO-KIF formula.
     */
    private static String literalListToString(final List<String> literal) {

        String b = "";
        if (literal instanceof List) {
            final StringJoiner joiner = new StringJoiner(" ", "(", ")");
            for (final String s : literal) {
                joiner.add(s);
            }
            b = joiner.toString();
        }
        return b;
    }

    /***************************************************************
     * Converts a
     * literal (List object) to a Formula.
     *
     * @param lit A List representing a SUO-KIF formula.
     * @return A SUO-KIF Formula object, or null if no Formula can be created.
     */
    public static Formula literalListToFormula(final List<String> lit) {

        Formula f = null;
        final String theFormula = literalListToString(lit);
        if (StringUtil.isNonEmptyString(theFormula)) {
            f = new Formula();
            f.read(theFormula);
        }
        return f;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing the terms (Strings) that correspond to targetArgnum
     * in the Formulas obtained from the method call askWithRestriction(argnum1,
     * term1, argnum2, term2).
     *
     * @param predicatesUsed A Set to which will be added the predicates of the ground
     *                       assertions actually used to gather the terms returned
     * @return An ArrayList of terms, or an empty ArrayList if no terms can be
     * retrieved.
     */
    private ArrayList<String> getTermsViaAskWithRestriction(final int argnum1, final String term1, final int argnum2, final String term2,
                                                            final int targetArgnum, final Collection<String> predicatesUsed) {

        final ArrayList<String> result = new ArrayList<String>();
        if (StringUtil.isNonEmptyString(term1) && !StringUtil.isQuotedString(term1)
                && StringUtil.isNonEmptyString(term2) && !StringUtil.isQuotedString(term2)) {
            final ArrayList<Formula> formulae = askWithRestriction(argnum1, term1, argnum2, term2);
            Formula f = null;
            for (final Formula value : formulae) {
                f = value;
                result.add(f.getArgument(targetArgnum));
            }
            if (predicatesUsed instanceof Set) {
                for (final Formula formula : formulae) {
                    f = formula;
                    predicatesUsed.add(f.car());
                }
            }
        }
        return result;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing the terms (Strings) that correspond to targetArgnum
     * in the Formulas obtained from the method call askWithRestriction(argnum1,
     * term1, argnum2, term2).
     *
     * @return An ArrayList of terms, or an empty ArrayList if no terms can be
     * retrieved.
     */
    private ArrayList<String> getTermsViaAskWithRestriction(final int argnum1, final String term1, final int argnum2, final String term2,
                                                            final int targetArgnum) {

        return getTermsViaAskWithRestriction(argnum1, term1, argnum2, term2, targetArgnum, null);
    }

    /***************************************************************
     * Returns the
     * first target found that corresponds to targetArgnum in the Formulas
     * obtained from the method call askWithRestriction(argnum1, term1, argnum2,
     * term2).
     *
     * @return A SUO-KIF target (String), or null is no answer can be retrieved.
     */
    public String getFirstTermViaAskWithRestriction(final int argnum1, final String term1, final int argnum2, final String term2,
                                                    final int targetArgnum) {

        String result = null;
        final ArrayList<String> terms = getTermsViaAskWithRestriction(argnum1, term1, argnum2, term2, targetArgnum);
        if (!terms.isEmpty())
            result = terms.get(0);
        return result;
    }

    /***************************************************************
     * @return an ArrayList of Formulas in which the two terms provided appear
     * in the indicated argument positions. If there are no Formula(s)
     * matching the given terms and respective argument positions,
     * return an empty ArrayList. Iterate through the smallest list of
     * results.
     */
    ArrayList<Formula> askWithRestriction(final int argnum1, final String term1, final int argnum2, final String term2) {

        final ArrayList<Formula> result = new ArrayList<Formula>();
        if (StringUtil.isNonEmptyString(term1) && StringUtil.isNonEmptyString(term2)) {
            final ArrayList<Formula> partial1 = ask("arg", argnum1, term1);
            final ArrayList<Formula> partial2 = ask("arg", argnum2, term2);


            ArrayList<Formula> partial = partial1;

            int arg = argnum2;
            String term = term2;
            if (partial1.size() > partial2.size()) {
                partial = partial2;
                arg = argnum1;
                term = term1;
            }
            Formula f = null;
            final int plen = partial.size();
            for (final Formula formula : partial) {
                f = formula;
                if (f == null)
                    System.out.println("Error in KB.askWithRestriction(): null formula searching on target: " + term);
                final String thisArg = f.getArgument(arg);
                if (thisArg == null) {
                    System.out.println("Error in KB.askWithRestriction(): null argument: " + f);
                } else if (f.getArgument(arg).equals(term))
                    result.add(f);
            }
        }
        return result;
    }

    /***************************************************************
     * Returns an
     * ArrayList of Formulas in which the two terms provided appear in the
     * indicated argument positions. If there are no Formula(s) matching the
     * given terms and respective argument positions, return an empty ArrayList.
     *
     * @return ArrayList
     */
    ArrayList<Formula> askWithTwoRestrictions(final int argnum1, final String term1, final int argnum2, final String term2, final int argnum3,
                                              final String term3) {

        final String[] args = new String[6];
        args[0] = "argnum1 = " + argnum1;
        args[1] = "term1 = " + term1;
        args[0] = "argnum2 = " + argnum2;
        args[1] = "term2 = " + term2;
        args[0] = "argnum3 = " + argnum3;
        args[1] = "term3 = " + term3;

        boolean b = true;
        for (final String s : Arrays.asList(term1, term2, term3)) {
            if (!StringUtil.isNonEmptyString(s)) {
                b = false;
                break;
            }
        }
        final ArrayList<Formula> result = new ArrayList<Formula>();
        if (b) {

            final ArrayList<Formula> partial1 = ask("arg", argnum1, term1);
            final ArrayList<Formula> partial2 = ask("arg", argnum2, term2);
            final ArrayList<Formula> partial3 = ask("arg", argnum3, term3);
            if (partial1 == null || partial2 == null || partial3 == null)
                return result;
            String termc = "";
            int argc = -1;
            String termb = "";
            int argb = -1;
            ArrayList<Formula> partiala = new ArrayList<Formula>();
            if (partial1.size() > partial2.size() && partial1.size() > partial3.size()) {
                argc = argnum1;
                termc = term1;
                if (partial2.size() > partial3.size()) {
                    argb = argnum2;
                    termb = term2;
                    partiala = partial3;
                }
                else {
                    argb = argnum3;
                    termb = term3;
                    partiala = partial2;
                }
            }
            if (partial2.size() > partial1.size() && partial2.size() > partial3.size()) {
                argc = argnum2;
                termc = term2;
                if (partial1.size() > partial3.size()) {
                    argb = argnum1;
                    termb = term1;
                    partiala = partial3;
                }
                else {
                    argb = argnum3;
                    termb = term3;
                    partiala = partial1;
                }
            }
            if (partial3.size() > partial1.size() && partial3.size() > partial2.size()) {
                argc = argnum3;
                termc = term3;
                if (partial1.size() > partial2.size()) {
                    argb = argnum1;
                    termb = term1;
                    partiala = partial2;
                }
                else {
                    argb = argnum2;
                    termb = term2;
                    partiala = partial1;
                }
            }
            for (final Formula f : partiala) {
                if (f.getArgument(argb).equals(termb)) {
                    if (f.getArgument(argc).equals(termc))
                        result.add(f);
                }
            }
        }
        return result;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing the SUO-KIF terms that match the request.
     *
     * @return An ArrayList of terms, or an empty ArrayList if no matches can be
     * found.
     */
    private ArrayList<String> getTermsViaAWTR(final int argnum1, final String term1, final int argnum2, final String term2, final int argnum3,
                                              final String term3, final int targetArgnum) {

        final ArrayList<String> ans = new ArrayList<String>();
        final List<Formula> formulae = askWithTwoRestrictions(argnum1, term1, argnum2, term2, argnum3, term3);
        Formula f = null;
        for (final Formula formula : formulae) {
            f = formula;
            ans.add(f.getArgument(targetArgnum));
        }
        return ans;
    }

    /***************************************************************
     * Returns the
     * first SUO-KIF terms that matches the request, or null.
     *
     * @return A target (String), or null.
     */
    public String getFirstTermViaAWTR(final int argnum1, final String term1, final int argnum2, final String term2, final int argnum3, final String term3,
                                      final int targetArgnum) {

        String ans = null;
        final List<String> terms = getTermsViaAWTR(argnum1, term1, argnum2, term2, argnum3, term3, targetArgnum);
        if (!terms.isEmpty())
            ans = terms.get(0);
        return ans;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing the terms (Strings) that correspond to targetArgnum
     * in the ground atomic Formulae in which knownArg is in the argument
     * position knownArgnum. The ArrayList returned will contain no duplicate
     * terms.
     *
     * @param knownArgnum  The argument position of knownArg
     * @param knownArg     The target that appears in the argument knownArgnum of the
     *                     ground atomic Formulae in the KB
     * @param targetArgnum The argument position of the terms being sought
     * @return An ArrayList of Strings, which will be empty if no match found.
     */
    public ArrayList<String> getTermsViaAsk(final int knownArgnum, final String knownArg, final int targetArgnum) {

        final ArrayList<String> result = new ArrayList<String>();
        final List<Formula> formulae = ask("arg", knownArgnum, knownArg);
        if (!formulae.isEmpty()) {
            final Set<String> ts = new TreeSet<>();
            Formula f = null;
            for (final Formula formula : formulae) {
                f = formula;
                ts.add(f.getArgument(targetArgnum));
            }
            result.addAll(ts);
        }
        return result;
    }

    /***************************************************************
     */
    private ArrayList<Formula> stringsToFormulas(final ArrayList<String> strings) {

        final ArrayList<Formula> result = new ArrayList<Formula>();
        if (strings == null)
            return result;
        for (final String s : strings) {
            final Formula f = formulaMap.get(s);
            if (f != null)
                result.add(f);
            else
                System.out.println("Error in KB.stringsToFormulas(): null formula for key: " + s);
        }
        return result;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing the Formulas that match the request.
     *
     * @param kind   May be one of "ant", "cons", "stmt", or "arg"
     * @param term   The target that appears in the statements being requested.
     * @param argnum The argument position of the target being asked for. The first
     *               argument after the predicate is "1". This parameter is ignored
     *               if the kind is "ant", "cons" or "stmt".
     * @return An ArrayList of Formula(s), which will be empty if no match
     * found.
     * see KIF.createKey()
     */
    public ArrayList<Formula> ask(final String kind, final int argnum, final String term) {

        String msg = null;
        if (StringUtil.emptyString(term)) {
            msg = ("Error in KB.ask(\"" + kind + "\", " + argnum + ", \"" + term + "\"), "
                    + "search target is null, or an empty string");
            errors.add(msg);
        }
        if (term.length() > 1 && (int) term.charAt(0) == (int) '"' && (int) term.charAt(term.length() - 1) == (int) '"') {
            msg = ("Error in KB.ask(), Strings are not indexed.  No results for " + term);
            errors.add(msg);
        }
        String key = null;
        if ("arg".equals(kind))
            key = kind + '-' + argnum + '-' + term;
        else
            key = kind + '-' + term;
        final ArrayList<String> alstr = formulas.get(key);

        final ArrayList<Formula> tmp = stringsToFormulas(alstr);
        final ArrayList<Formula> result = new ArrayList<Formula>();
        if (tmp != null)
            result.addAll(tmp);
        return result;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing the Formulae retrieved, possibly via multiple asks
     * that recursively use relation and all of its subrelations. Note that the
     * Formulas might be formed with different predicates, but all of the
     * predicates will be subrelations of relation and will be related to each
     * other in a subsumption hierarchy.
     * <p>
     * FIXME: this routine only gets subrelations one level down
     *
     * @param relation  The name of a predicate, which is assumed to be the 0th
     *                  argument of one or more atomic formulae
     * @param idxArgnum The argument position occupied by idxTerm in each ground
     *                  Formula to be retrieved
     * @param idxTerm   A constant that occupied idxArgnum position in each ground
     *                  Formula to be retrieved
     * @return an ArrayList of Formulas that satisfy the query, or an empty
     * ArrayList if no Formulae are retrieved.
     */
    public ArrayList<Formula> askWithPredicateSubsumption(final String relation, final int idxArgnum, final String idxTerm) {

        final ArrayList<Formula> ans = new ArrayList<Formula>();
        if (StringUtil.isNonEmptyString(relation) && StringUtil.isNonEmptyString(idxTerm) && (idxArgnum >= 0)) {
            
            
            

            final Set<String> relns = new HashSet<>();
            
            relns.add(relation);
            final ArrayList<Formula> subrelForms = askWithRestriction(0, "subrelation", 2, relation);
            for (final Formula f : subrelForms) {
                final String arg = f.getArgument(1);
                relns.add(arg);
            }
            final ArrayList<Formula> forms = ask("arg", idxArgnum, idxTerm);
            final Set<Formula> accumulator = new HashSet<>();
            for (final Formula f : forms) {
                if (!accumulator.contains(f)) {
                    final String arg = f.getArgument(0);
                    if (relns.contains(arg))
                        accumulator.add(f);
                }
            }
            ans.addAll(accumulator);
        }
        return ans;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing SUO-KIF constants, possibly retrieved via multiple
     * asks that recursively use relation and all of its subrelations.
     *
     * @param relation       The name of a predicate, which is assumed to be the 0th
     *                       argument of one or more atomic Formulae
     * @param idxArgnum      The argument position occupied by target in the ground atomic
     *                       Formulae that will be retrieved to gather the target (answer)
     *                       terms
     * @param idxTerm        A constant that occupies idxArgnum position in each of the
     *                       ground atomic Formulae that will be retrieved to gather the
     *                       target (answer) terms
     * @param targetArgnum   The argument position of the answer terms in the Formulae to
     *                       be retrieved
     * @param useInverses    If true, the inverses of relation and its subrelations will be
     *                       also be used to try to find answer terms
     * @param predicatesUsed A Set to which will be added the predicates of the ground
     *                       assertions actually used to gather the terms returned
     * @return an ArrayList of terms (SUO-KIF constants), or an empy ArrayList
     * if no terms can be retrieved
     */
    private ArrayList<String> getTermsViaPredicateSubsumption(final String relation, final int idxArgnum, final String idxTerm,
                                                              final int targetArgnum, final boolean useInverses, final Set predicatesUsed) {

        final ArrayList<String> ans = new ArrayList<String>();
        if (StringUtil.isNonEmptyString(relation) && StringUtil.isNonEmptyString(idxTerm) && (idxArgnum >= 0)
            
                ) {
            List<String> inverseSyns = null;
            List<String> inverses = null;
            if (useInverses) {
                inverseSyns = getTermsViaAskWithRestriction(0, "subrelation", 2, "inverse", 1);
                inverseSyns.addAll(getTermsViaAskWithRestriction(0, "equal", 2, "inverse", 1));
                inverseSyns.addAll(getTermsViaAskWithRestriction(0, "equal", 1, "inverse", 2));
                inverseSyns.add("inverse");
                SetUtil.removeDuplicates(inverseSyns);
                inverses = new ArrayList<>();
            }
            final Collection<String> predicates = new ArrayList<>();
            predicates.add(relation);
            final Collection<String> accumulator = new ArrayList<>();
            final Set<String> reduced = new TreeSet<>();
            while (!predicates.isEmpty()) {
                for (final String pred : predicates) {
                    reduced.addAll(
                            getTermsViaAskWithRestriction(0, pred, idxArgnum, idxTerm, targetArgnum, predicatesUsed));
                    accumulator.addAll(getTermsViaAskWithRestriction(0, "subrelation", 2, pred, 1));
                    accumulator.addAll(getTermsViaAskWithRestriction(0, "equal", 2, "subrelation", 1));
                    accumulator.addAll(getTermsViaAskWithRestriction(0, "equal", 1, "subrelation", 2));
                    accumulator.remove(pred);
                    if (useInverses) {
                        for (final String syn : inverseSyns) {
                            inverses.addAll(getTermsViaAskWithRestriction(0, syn, 1, pred, 2));
                            inverses.addAll(getTermsViaAskWithRestriction(0, syn, 2, pred, 1));
                        }
                    }
                }
                SetUtil.removeDuplicates(accumulator);
                predicates.clear();
                predicates.addAll(accumulator);
                accumulator.clear();
            }
            if (useInverses) {
                SetUtil.removeDuplicates(inverses);
                for (final String inv : inverses)
                    reduced.addAll(getTermsViaPredicateSubsumption(inv, targetArgnum, idxTerm, idxArgnum, false,
                            predicatesUsed));
            }
            ans.addAll(reduced);
        }
        return ans;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing SUO-KIF constants, possibly retrieved via multiple
     * asks that recursively use relation and all of its subrelations.
     *
     * @param relation     The name of a predicate, which is assumed to be the 0th
     *                     argument of one or more atomic Formulae
     * @param idxArgnum    The argument position occupied by target in the ground atomic
     *                     Formulae that will be retrieved to gather the target (answer)
     *                     terms
     * @param idxTerm      A constant that occupies idxArgnum position in each of the
     *                     ground atomic Formulae that will be retrieved to gather the
     *                     target (answer) terms
     * @param targetArgnum The argument position of the answer terms in the Formulae to
     *                     be retrieved
     * @param useInverses  If true, the inverses of relation and its subrelations will be
     *                     also be used to try to find answer terms
     * @return an ArrayList of terms (SUO-KIF constants), or an empy ArrayList
     * if no terms can be retrieved
     */
    private ArrayList<String> getTermsViaPredicateSubsumption(final String relation, final int idxArgnum, final String idxTerm,
                                                              final int targetArgnum, final boolean useInverses) {

        return getTermsViaPredicateSubsumption(relation, idxArgnum, idxTerm, targetArgnum, useInverses, null);
    }

    /***************************************************************
     * Returns the
     * first SUO-KIF constant found via asks using relation and its
     * subrelations.
     *
     * @param relation     The name of a predicate, which is assumed to be the 0th
     *                     argument of one or more atomic Formulae.
     * @param idxArgnum    The argument position occupied by target in the ground atomic
     *                     Formulae that will be retrieved to gather the target (answer)
     *                     terms.
     * @param idxTerm      A constant that occupies idxArgnum position in each of the
     *                     ground atomic Formulae that will be retrieved to gather the
     *                     target (answer) terms.
     * @param targetArgnum The argument position of the answer terms in the Formulae to
     *                     be retrieved.
     * @param useInverses  If true, the inverses of relation and its subrelations will be
     *                     also be used to try to find answer terms.
     * @return A SUO-KIF constants (String), or null if no target can be
     * retrieved.
     */
    public String getFirstTermViaPredicateSubsumption(final String relation, final int idxArgnum, final String idxTerm, final int targetArgnum,
                                                      final boolean useInverses) {

        String ans = null;
        if (StringUtil.isNonEmptyString(relation) && StringUtil.isNonEmptyString(idxTerm) && (idxArgnum >= 0)
            
                ) {
            final ArrayList<String> terms = getTermsViaPredicateSubsumption(relation, idxArgnum, idxTerm, targetArgnum,
                    useInverses);
            if (!terms.isEmpty())
                ans = terms.get(0);
        }
        return ans;
    }

    /***************************************************************
     * Returns an
     * ArrayList containing the transitive closure of relation starting from
     * idxTerm in position idxArgnum. The result does not contain idxTerm.
     *
     * @param relation     The name of a predicate, which is assumed to be the 0th
     *                     argument of one or more atomic Formulae
     * @param idxArgnum    The argument position occupied by target in the ground atomic
     *                     Formulae that will be retrieved to gather the target (answer)
     *                     terms
     * @param idxTerm      A constant that occupies idxArgnum position in the first
     *                     "level" of ground atomic Formulae that will be retrieved to
     *                     gather the target (answer) terms
     * @param targetArgnum The argument position of the answer terms in the Formulae to
     *                     be retrieved
     * @param useInverses  If true, the inverses of relation and its subrelations will be
     *                     also be used to try to find answer terms
     * @return an ArrayList of terms (SUO-KIF constants), or an empy ArrayList
     * if no terms can be retrieved
     */
    public ArrayList<String> getTransitiveClosureViaPredicateSubsumption(final String relation, final int idxArgnum, final String idxTerm,
                                                                         final int targetArgnum, final boolean useInverses) {

        final Collection<String> reduced = new TreeSet<>();
        final Collection<String> accumulator = new TreeSet<>(
            getTermsViaPredicateSubsumption(relation, idxArgnum, idxTerm, targetArgnum, useInverses));
        final List<String> working = new ArrayList<>();
        while (!accumulator.isEmpty()) {
            reduced.addAll(accumulator);
            working.clear();
            working.addAll(accumulator);
            accumulator.clear();
            for (final String term : working)
                accumulator
                        .addAll(getTermsViaPredicateSubsumption(relation, idxArgnum, term, targetArgnum, useInverses));
        }
        final ArrayList<String> ans = new ArrayList<String>(reduced);
        return ans;
    }

    /***************************************************************
     * Add all members of one collection to another.  If the argument
     * is null, do nothing.
     */
    private static void addAllSafe(final Collection c1, final Collection c2) {

        if (c1 != null && c2 != null)
            c1.addAll(c2);
    }

    /***************************************************************
     * Get all children of the given target following instance and
     * subclass relations as well as the indicated rel
     */
    private HashSet<String> getAllSub(final String term, final String rel) {


        ArrayList<String> temp = new ArrayList<String>();
        temp.add(term);
        HashSet<String> oldResult = new HashSet<String>();
        final HashSet<String> result = new HashSet<String>();
        while (!result.equals(oldResult)) {

            oldResult = new HashSet<>(temp);
            for (final String s : result) {
                addAllSafe(temp,kbCache.getChildTerms(s,"subclass"));
                addAllSafe(temp,kbCache.getChildTerms(s,"instance"));
                addAllSafe(temp,kbCache.getChildTerms(s,rel));
                addAllSafe(temp,kbCache.getInstancesForType(s));
            }
            result.addAll(temp);
            temp = new ArrayList<>(result);
        }
        return result;
    }

    /***************************************************************
     * Merges a
     * KIF object containing a single formula into the current KB.
     *
     * @param kif      A KIF object.
     * @param pathname The full, canonical pathname string of the constituent file in
     *                 which the formula will be saved, if known.
     * @return If any of the formulas are already present, returns an ArrayList
     * containing the old (existing) formulas, else returns an empty
     * ArrayList.
     */
    public ArrayList<Formula> merge(final KIFParser kif, final String pathname) {

        getTerms().addAll(kif.terms);
        final Set<String> keys = kif.formulas.keySet();
        final ArrayList<Formula> formulasPresent = new ArrayList<Formula>();
        for (final String key : keys) {
            final ArrayList<String> newFormulas = new ArrayList<String>(kif.formulas.get(key));
            if (formulas.containsKey(key)) {
                final ArrayList<String> oldFormulas = formulas.get(key);
                for (final String s : newFormulas) {
                    final Formula newFormula = kif.formulaMap.get(s);
                    if (pathname != null)
                        newFormula.sourceFile = pathname;
                    boolean found = false;
                    for (final String formula : oldFormulas) {
                        final Formula oldFormula = formulaMap.get(formula);
                        if (oldFormula != null && newFormula.theFormula.equals(oldFormula.theFormula)) {
                            found = true;


                            if (!formulasPresent.contains(oldFormula))
                                formulasPresent.add(oldFormula);
                        }
                    }
                    if (!found) {
                        oldFormulas.add(newFormula.theFormula);
                        formulaMap.put(newFormula.theFormula.intern(), newFormula);
                    }
                }
            } else {
                formulas.put(key, newFormulas);
                final Iterator<String> it2 = newFormulas.iterator();
                Formula f = null;
                while (it2.hasNext()) {
                    final String newformulaStr = it2.next();
                    final Formula newFormula = kif.formulaMap.get(newformulaStr);
                    f = formulaMap.get(newformulaStr);
                    if (f == null)

                        formulaMap.put(newFormula.theFormula.intern(), newFormula);
                    else if (StringUtil.isNonEmptyString(f.theFormula))
                        formulaMap.put(f.theFormula.intern(), f);
                }
            }
        }
        return formulasPresent;
    }

    /***************************************************************
     * Rename
     * term2 as term1 throughout the knowledge base. This is an operation with
     * side effects - the target names in the KB are changed.
     */
    public void rename(final String term2, final String term1) {

        final Set<Formula> formulas = new HashSet<>();
        for (int i = 0; i < 7; i++)
            formulas.addAll(ask("arg", i, term2));
        formulas.addAll(ask("ant", 0, term2));
        formulas.addAll(ask("cons", 0, term2));
        formulas.addAll(ask("stmt", 0, term2));
        for (final Formula f : formulas) {
            f.theFormula = f.rename(term2, term1).theFormula;
        }
    }

    /***************************************************************
     * Writes a
     * single user assertion (String) to the end of a file.
     *
     * @param formula A String representing a SUO-KIF Formula.
     * @param fname   A String denoting the pathname of the target file.
     * @return A long value indicating the number of bytes in the file after the
     * formula has been written. A value of 0L means that the file does
     * not exist, and so could not be written for some reason. A value
     * of -1 probably means that some error occurred.
     */
    private static long writeUserAssertion(final String formula, final String fname) throws IOException {

        long flen = -1L;
        FileWriter fr = null;
        try {
            final File file = new File(fname);
            fr = new FileWriter(file, true);
            fr.write(formula);
            fr.write("\n");
            flen = file.length();
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        finally {
            if (fr != null)
                fr.close();
        }
        return flen;
    }

    /* *************************************************************
    Writes all
     * the terms in the knowledge base to a file
     */
    public void writeTerms() throws IOException {

        final String fname = KBmanager.manager.getPref("kbDir") + File.separator + "terms.txt";
        FileWriter fr = null;
        try {
            final File file = new File(fname);
            fr = new FileWriter(file, true);
            for (final String term : terms) {
                fr.write(term);
                fr.write("\n");
            }
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        finally {
            if (fr != null)
                fr.close();
        }
    }

    /***************************************************************
     * Adds a
     * formula to the knowledge base. Returns an XML formatted String that
     * contains the response of the inference engine. It should be of the form
     * "<assertionResponse>...</assertionResponse>" where the body should be "
     * Formula has been added to the session database" if all went well.
     * <p>
     * TODO: If we find a way to directly add assertions into opened inference
     * engine, we can roll back to 1.111 version
     *
     * @param input The String representation of a SUO-KIF Formula.
     * @return A String indicating the status of the tell operation.
     */
    public String tell(final String input) {


        String result = "The formula could not be added";
        final KBmanager mgr = KBmanager.manager;
        final KIFParser kif = new KIFParser();
        final String msg = kif.parseStatement(input);
        if (msg != null) {
            result = "Error parsing \"" + input + "\" " + msg;
            return result;
        }
        if (kif.formulaMap.keySet().isEmpty()) {
            result = "The input could not be parsed";
            return result;
        }
        try {
            final String userAssertionKIF = this.name + _userAssertionsString;

            final File dir = new File(this.kbDir);
            final File kiffile = new File(dir, (userAssertionKIF));

            final String filename = kiffile.getCanonicalPath();
            final ArrayList<Formula> formulasAlreadyPresent = merge(kif, filename);
            
            
            
            
            
            
            
            




             {
                final List<Formula> parsedFormulas = new ArrayList<>();
                 for (final Formula parsedF : kif.formulaMap.values()) {


                     System.out.println("KB.tell: " + parsedF);
                     final String term = PredVarInst.hasCorrectArity(parsedF, this);
                     if (!StringUtil.emptyString(term)) {
                         result = result + "Formula in " + parsedF.sourceFile
                             + " rejected due to arity error of predicate " + term + " in formula: \n"
                             + parsedF.theFormula;
                     } else
                         parsedFormulas.add(parsedF);
                 }
                if (!parsedFormulas.isEmpty()) {
                    if (!constituents.contains(filename)) {
                        if (kiffile.exists()) 
                            
                            kiffile.delete();
                        final String userAssertionTPTP = userAssertionKIF.substring(0, userAssertionKIF.indexOf(".kif")) + ".tptp";
                        final File tptpfile = new File(dir, (userAssertionTPTP));
                        if (tptpfile.exists())
                            tptpfile.delete();
                        constituents.add(filename);
                        
                    }
                    for (final Formula parsedF : parsedFormulas) {
                        parsedF.endFilePosition = writeUserAssertion(parsedF.theFormula, filename);
                        parsedF.sourceFile = filename;
                    }
                    result = "The formula has been added for browsing";












                }
            }
        }
        catch (final IOException ioe) {
            ioe.printStackTrace();
            System.out.println(ioe.getMessage());
            result = ioe.getMessage();
        }
        return result;
    }








































































































































































































































































































































    /*****************************************************************
     */
    private int termDepth(final String term) {

        
        if (!terms.contains(term)) {
            System.out.println("KB.termDepth(): no such target " + term);
            return 0;
        }
        if ("Entity".equals(term) || StringUtil.isNumeric(term))
            return 0;
        if (!kbCache.subclassOf(term,"Entity") && !kbCache.transInstOf(term,"Entity"))
            return 0;
        final Set<String> rents = immediateParents(term);
        for (final String s : rents)
            return 1 + termDepth(s);
        return 0;
    }

    /*****************************************************************
     */
    private HashSet<String> immediateParents(final String term) {


        HashSet<String> result = new HashSet<String>();
        if (!terms.contains(term)) {
            System.out.println("KB.immediateParents(): no such target " + term);
            return result;
        }
        final ArrayList<Formula> forms = askWithRestriction(0,"subclass",1,term);
        forms.addAll(askWithRestriction(0,"instance",1,term));
        forms.addAll(askWithRestriction(0,"subrelation",1,term));
        forms.addAll(askWithRestriction(0,"subAttribute",1,term));
        final HashSet<String> strings = new HashSet<>();
        for (final Formula f : forms) {
            if (!f.isCached()) {
                final String argument = f.getArgument(2);
                strings.add(argument);
            }
        }
        result = strings;
        
        return result;
    }

    /*****************************************************************
     * Analogous to compareTo(), return -1,0 or 1 depending on whether
     * the first target is "smaller", equal to or "greater" than the
     * second, respectively.  A target that is the parent of another
     * is "smaller".  If not a parent of the other, the smaller target
     * is that which is fewer "levels" from their common parent.
     * Therefore, terms that are not the same can still be "equal"
     * if they're at the same level of the taxonomy.
     */
    public int compareTerms(final String t1, final String t2) {

        if (t1.equals(t2))
            return 0;
        if (kbCache.subAttributeOf(t1,t2) || kbCache.subclassOf(t1,t2))
            return -1;
        if (kbCache.subAttributeOf(t2,t1) || kbCache.subclassOf(t2,t1))
            return 1;
        final String p = kbCache.getCommonParent(t1,t2);
        final boolean found = false;
        final int depthT1 = termDepth(t1);
        final int depthT2 = termDepth(t2);
        return Integer.compare(depthT1, depthT2);
    }

    /*****************************************************************
     * Takes a target and returns true if the target occurs in the KB.
     *
     * @param term A String.
     * @return true or false.
     */
    private boolean containsTerm(final String term) {

        if (getTerms().contains(term.intern()))
            return true;
        else return getREMatch(term.intern()).size() == 1;
    }

    /*****************************************************************
     * Takes a formula string and returns true if the corresponding Formula occurs in
     * the KB.
     *
     * @param formula A String.
     * @return true or false.
     */
    public boolean containsFormula(final String formula) {

        return formulaMap.containsKey(formula.intern());
    }

    /***************************************************************** Count the
     * number of terms in the knowledge base in order to present statistics to
     * the user.
     *
     * @return The int(eger) number of terms in the knowledge base.
     */
    public int getCountTerms() {

        return getTerms().size();
    }

    /*****************************************************************
     * Count the number of relations in the knowledge base in order to present statistics
     * to the user.
     *
     * @return The int(eger) number of relations in the knowledge base.
     */
    public int getCountRelations() {

        return kbCache.relations.size();
    }

    /*****************************************************************
     * Count the number of formulas in the knowledge base in order to present statistics
     * to the user.
     *
     * @return The int(eger) number of formulas in the knowledge base.
     */
    public int getCountAxioms() {

        return formulaMap.size();
    }

    /*****************************************************************
     * An accessor providing a TreeSet of un-preProcessed String representations of
     * Formulae.
     *
     * @return A TreeSet of Strings.
     */
    public TreeSet<String> getFormulas() {

        return new TreeSet<>(formulaMap.keySet());
    }

    /*****************************************************************
     * An accessor providing a Formula
     */
    public Formula getFormulaByKey(final String key) {

        Formula f = null;
        final ArrayList<String> al = formulas.get(key);
        if ((al != null) && !al.isEmpty())
            f = formulaMap.get(al.get(0));
        return f;
    }

    /*****************************************************************
     * Count the number of rules in the knowledge base in order to present statistics to
     * the user. Note that the number of rules is a subset of the number of
     * formulas.
     *
     * @return The int(eger) number of rules in the knowledge base.
     */
    public int getCountRules() {

        long result = 0L;
        for (final Formula formula : formulaMap.values()) {
            if (formula.isRule()) {
                result++;
            }
        }
        final int count = (int) result;
        return count;
    }

    /*****************************************************************
     * Create an ArrayList of the specific size, filled with empty strings.
     */
    private static ArrayList<String> arrayListWithBlanks(final int size) {

        final ArrayList<String> al = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            final String s = "";
            al.add(s);
        }
        return al;
    }

    /** ***************************************************************
     * Get the alphabetically nearest terms to the given target, which is not in the KB.
     * Elements 0-(k-1) should be alphabetically lesser and k-(2*k-1)
     * alphabetically greater. If the target is at the beginning or end of the
     * alphabet, fill in blank items with the empty string: "".
     */
    private ArrayList<String> getNearestKTerms(final String term, final int k) {

        final ArrayList<String> al;
        if (k == 0)
            al = arrayListWithBlanks(1);
        else
            al = arrayListWithBlanks(2 * k);
        final Object[] t = getTerms().toArray();
        int i = 0;
        while (i < t.length - 1 && ((String) t[i]).compareTo(term) < 0)
            i++;
        if (k == 0) {
            al.set(0, (String) t[i]);
            return al;
        }
        int lower = i;
        while (i - lower < k && lower > 0) {
            lower--;
            al.set(k - (i - lower), (String) t[lower]);
        }
        int upper = i - 1;
        while (upper - i < (k - 1) && upper < t.length - 1) {
            upper++;
            al.set(k + (upper - i), (String) t[upper]);
        }
        return al;
    }

    /*****************************************************************
     * Get the alphabetically nearest terms to the given target, which is not in the KB.
     * Elements 0-14 should be alphabetically lesser and 15-29 alphabetically
     * greater. If the target is at the beginning or end of the alphabet, fill in
     * blank items with the empty string: "".
     */
    private ArrayList<String> getNearestTerms(final String term) {

        return getNearestKTerms(term, 15);
    }

    /*****************************************************************
     * Get the neighbors of this initial uppercase target (class or function).
     */
    public ArrayList<String> getNearestRelations(String term) {

        term = Character.toUpperCase(term.charAt(0)) + term.substring(1);
        return getNearestTerms(term);
    }

    /*****************************************************************
     * Get the neighbors of this initial lowercase target (relation).
     */
    public ArrayList<String> getNearestNonRelations(String term) {

        term = Character.toLowerCase(term.charAt(0)) + term.substring(1);
        return getNearestTerms(term);
    }

    /*****************************************************************
     * Get the alphabetically num lower neighbor of this initial target, which must exist
     * in the current KB otherwise an empty string is returned.
     */
    public String getAlphaBefore(String term, final int num) {

        if (!getTerms().contains(term)) {
            final ArrayList<String> al = getNearestKTerms(term, 0);
            term = al.get(0);
        }
        if (getTerms().size() < 1)
            return "";
        final ArrayList<String> tal = new ArrayList<String>(getTerms());
        int i = tal.indexOf(term.intern());
        if (i < 0)
            return "";
        i -= num;
        if (i < 0)
            i = 0;
        return tal.get(i);
    }

    /*****************************************************************
     * Get the alphabetically num higher neighbor of this initial target, which must exist
     * in the current KB otherwise an empty string is returned.
     */
    public String getAlphaAfter(String term, final int num) {

        if (!getTerms().contains(term)) {
            final ArrayList<String> al = getNearestKTerms(term, 0);
            term = al.get(0);
        }
        if (getTerms().size() < 1)
            return "";
        final ArrayList<String> tal = new ArrayList<String>(getTerms());
        int i = tal.indexOf(term.intern());
        if (i < 0)
            return "";
        i += num;
        if (i >= tal.size())
            i = tal.size() - 1;
        return tal.get(i);
    }

    /****************************************************************
     * This List is used to limit the number of warning messages logged by
     * loadFormatMaps(lang). If an attempt to load format or termFormat values
     * for lang is unsuccessful, the list is checked for the presence of lang.
     * If lang is not in the list, a warning message is logged and lang is added
     * to the list. The list is cleared whenever a constituent file is added or
     * removed for KB, since the latter might affect the availability of format
     * or termFormat values.
     */
    private final List<String> loadFormatMapsAttempted = new ArrayList<>();

    /****************************************************************
     * Populates the format maps for language lang.
     *
     * see termFormatMap is a HashMap of language keys and HashMap values. The
     *      interior HashMaps are target keys and format string values.
     *
     * see formatMap is the same but for relation format strings.
     */
    private void loadFormatMaps(final String lang) {

        if (formatMap == null)
            formatMap = new HashMap<>();
        if (termFormatMap == null)
            termFormatMap = new HashMap<>();
        formatMap.computeIfAbsent(lang, new Function<String, HashMap<String, String>>() {
            @Override
            public HashMap<String, String> apply(String k) {
                return new HashMap<>();
            }
        });
        termFormatMap.computeIfAbsent(lang, new Function<String, HashMap<String, String>>() {
            @Override
            public HashMap<String, String> apply(String k) {
                return new HashMap<>();
            }
        });

        if (!loadFormatMapsAttempted.contains(lang)) {
            ArrayList<Formula> col = askWithRestriction(0, "format", 1, lang);
            if ((col == null) || col.isEmpty())
                System.out.println("Error in KB.loadFormatMaps(): No relation format file loaded for language " + lang);
            else {
                final HashMap<String, String> langFormatMap = formatMap.get(lang);
                for (final Formula f : col) {
                    final String key = f.getArgument(2);
                    String format = f.getArgument(3);
                    format = StringUtil.removeEnclosingQuotes(format);
                    langFormatMap.put(key, format);
                }
            }
            col = askWithRestriction(0, "termFormat", 1, lang);
            if ((col == null) || col.isEmpty())
                System.out.println("Error in KB.loadFormatMaps(): No target format file loaded for language: " + lang);
            else {
                final HashMap<String, String> langTermFormatMap = termFormatMap.get(lang);
                for (final Formula f : col) {
                    final String key = f.getArgument(2);
                    String format = f.getArgument(3);
                    format = StringUtil.removeEnclosingQuotes(format);
                    langTermFormatMap.put(key, format);
                }
            }
            loadFormatMapsAttempted.add(lang);
        }
        language = lang;
    }

    /*****************************************************************
     * Clears all loaded format and termFormat maps, for all languages.
     */
    private void clearFormatMaps() {

        if (formatMap != null) {
            for (final HashMap<String, String> m : formatMap.values()) {
                if (m != null)
                    m.clear();
            }
            formatMap.clear();
        }
        if (termFormatMap != null) {
            for (final HashMap<String, String> m : termFormatMap.values()) {
                if (m != null)
                    m.clear();
            }
            termFormatMap.clear();
        }
        loadFormatMapsAttempted.clear();
    }

    /*****************************************************************
     * This method creates a dictionary (Map) of SUO-KIF target symbols -- the keys --
     * and a natural language string for each key that is the preferred name for
     * the target -- the values -- in the context denoted by lang. If the Map has
     * already been built and the language hasn't changed, just return the
     * existing map. This is a case of "lazy evaluation".
     *
     * @return An instance of Map where the keys are terms and the values are
     *         format strings.
     */
    public HashMap<String, String> getTermFormatMap(String lang) {

        if (!StringUtil.isNonEmptyString(lang))
            lang = "EnglishLanguage";
        if ((termFormatMap == null) || termFormatMap.isEmpty())
            loadFormatMaps(lang);
        final HashMap<String, String> langTermFormatMap = termFormatMap.get(lang);
        if ((langTermFormatMap == null) || langTermFormatMap.isEmpty())
            loadFormatMaps(lang);
        return termFormatMap.get(lang);
    }

    /*****************************************************************
     * This method creates an association list (Map) of the natural language format
     * string and the relation name for which that format string applies. If the
     * map has already been built and the language hasn't changed, just return
     * the existing map. This is a case of "lazy evaluation".
     *
     * @return An instance of Map where the keys are relation names and the
     *         values are format strings.
     */
    public HashMap<String, String> getFormatMap(String lang) {

        if (!StringUtil.isNonEmptyString(lang))
            lang = "EnglishLanguage";
        if ((formatMap == null) || formatMap.isEmpty())
            loadFormatMaps(lang);
        final HashMap<String, String> langFormatMap = formatMap.get(lang);
        if ((langFormatMap == null) || langFormatMap.isEmpty())
            loadFormatMaps(lang);
        return formatMap.get(lang);
    }

    /*****************************************************************
     * Deletes user assertions, both in the files and in the constituents list.
     */
    public void deleteUserAssertions() {

        String toRemove = null;
        for (final String constituent : constituents) {
            if (constituent.endsWith(_userAssertionsString)) {
                toRemove = constituent;
                break;
            }
        }

        if (toRemove != null) {
            constituents.remove(toRemove);
        }
    }
























    /***************************************************************
     * Add a new KB constituent by reading in the file, and then merging the formulas with
     * the existing set of formulas.
     *
     * @param filename
     *            - The full path of the file being added
     */
    void addConstituent(final String filename) {
        

        System.out.println("INFO in KB.addConstituent(): " + filename);
        String canonicalPath = null;
        final KIFParser file = new KIFParser();
        try {


            final File constituent = new File(filename);

            canonicalPath = constituent.getCanonicalPath();
            if (constituents.contains(canonicalPath))
                errors.add("Error. " + canonicalPath + " already loaded.");
            
            
        }
        catch (final Exception ex1) {
            final StringBuilder error = new StringBuilder();
            error.append(ex1.getMessage());
            error.append(" in file ").append(canonicalPath);
            errors.add(error.toString());
            System.out.println("Error in KB.addConstituent(): " + error);
            ex1.printStackTrace();
        }
        for (final Map.Entry<String, Integer> entry : file.termFrequency.entrySet()) {
            if (!termFrequency.containsKey(entry.getKey())) {
                termFrequency.put(entry.getKey(), entry.getValue());
            }
            else {
                termFrequency.put(entry.getKey(), termFrequency.get(entry.getKey()) + entry.getValue());
            }
        }

        final Iterator<String> it = file.formulas.keySet().iterator();
        int count = 0;
        while (it.hasNext()) {
            final String key = it.next();
            if ((count++ % 100) == 1)
                System.out.print(".");
            final ArrayList<String> newlist = file.formulas.get(key);


            for (final String form : newlist) {
                if (StringUtil.emptyString(form))
                    System.out.println("Error in KB.addConstituent() 1: formula is null ");
            }
            final ArrayList<String> list = formulas.get(key);

            if (list != null) {

                for (final String form : list) {
                    if (StringUtil.emptyString(form))
                        System.out.println("Error in KB.addConstituent() 2: formula is null ");
                }
                newlist.addAll(list);
            }
            formulas.put(key, newlist);
        }

        count = 0;
        for (final Formula f : file.formulaMap.values()) {
            final String internedFormula = f.theFormula.intern();
            if ((count++ % 100) == 1)
                System.out.print(".");
            if (!formulaMap.containsKey(internedFormula))
                formulaMap.put(internedFormula, f);
        }
        System.out.println("INFO in KB.addConstituent(): added " + file.formulaMap.values().size() + " formulas and "
                + file.terms.size() + " terms.");
        this.getTerms().addAll(file.terms);
        if (!constituents.contains(canonicalPath))
            constituents.add(canonicalPath);
        
        
        
        
        
        
        
    }

    /*****************************************************************
     * Reload all the KB constituents.
     */
    public String reload() {

        synchronized (this.getTerms()) {
            final List<String> list = new ArrayList<>();
            for (final String constituent : constituents) {
                if (!constituent.endsWith(_cacheFileSuffix)) {
                    list.add(constituent);
                }
            }
            final List<String> newConstituents = list;
            constituents.clear();
            formulas.clear();
            formulaMap.clear();
            terms.clear();
            clearFormatMaps();
            errors.clear();
            final Iterator<String> nci = newConstituents.iterator();
            if (nci.hasNext())
                System.out.println("INFO in KB.reload()");
            while (nci.hasNext()) {
                final String cName = nci.next();
                addConstituent(cName);
                
            }

            if ("yes".equalsIgnoreCase(KBmanager.manager.getPref("cache"))) {
                kbCache = new KBcache(this);
                kbCache.buildCaches();
                checkArity(); 
            }
            else {
                kbCache = new KBcache(this);
                
            }
            
            
            
            

        }
        return "";
    }

    /*****************************************************************
     * Write a KIF file consisting of all the formulas in the knowledge base.
     *
     * @param fname
     *            - the name of the file to write, including full path.
     */
    public void writeFile(final String fname) throws IOException {

        final Set<String> formulaSet = new HashSet<>();

        for (final String key : formulas.keySet()) {
            final ArrayList<String> list = formulas.get(key);
            formulaSet.addAll(list);
        }
        PrintWriter pr = null;
        FileWriter fr = null;
        try {
            fr = new FileWriter(fname);
            pr = new PrintWriter(fr);
            for (final String s : formulaMap.keySet()) {
                pr.println(s);
                pr.println();
            }
        }
        catch (final IOException e) {
            System.out.println("Error in KB.writeFile(): Error writing file " + fname);
            e.printStackTrace();
        }
        finally {
            if (pr != null) {
                pr.close();
            }
            if (fr != null) {
                fr.close();
            }
        }
    }


















    /***************************************************************
     * A HashMap for holding compiled regular expression patterns. The map is initialized
     * by calling compilePatterns().
     */
    private static HashMap<String, ArrayList> REGEX_PATTERNS = null;

    /*****************************************************************
     * This method returns a compiled regular expression Pattern object indexed by
     * key.
     *
     * @param key
     *            A String that is the retrieval key for a compiled regular
     *            expression Pattern.
     *
     * @return A compiled regular expression Pattern instance.
     */
    private static Pattern getCompiledPattern(final String key) {

        if (StringUtil.isNonEmptyString(key) && (REGEX_PATTERNS != null)) {
            final ArrayList al = REGEX_PATTERNS.get(key);
            if (al != null)
                return (Pattern) al.get(0);
        }
        return null;
    }

    /*****************************************************************
     * This method returns the int value that identifies the regular expression
     * binding group to be returned when there is a match.
     *
     * @param key
     *            A String that is the retrieval key for the binding group index
     *            associated with a compiled regular expression Pattern.
     *
     * @return An int that indexes a binding group.
     */
    private static int getPatternGroupIndex(final String key) {

        if (StringUtil.isNonEmptyString(key) && (REGEX_PATTERNS != null)) {
            final ArrayList al = REGEX_PATTERNS.get(key);
            if (al != null)
                return (Integer) al.get(1);
        }
        return -1;
    }

    /*****************************************************************
     * This method compiles and stores regular expression Pattern objects and binding
     * group indexes as two cell ArrayList objects. Each ArrayList is indexed by
     * a String retrieval key.
     *
     * @return void
     */
    private static void compilePatterns() {

        if (REGEX_PATTERNS == null) {
            REGEX_PATTERNS = new HashMap<>();
            final String[][] patternArray = { { "row_var", "@ROW\\d*", "0" },
                    
                    
                    { "open_lit", "\\(\\w+\\s+\\?\\w+[a-zA-Z_0-9-?\\s]+\\)", "0" },
                    { "pred_var_1", "\\(holds\\s+(\\?\\w+)\\W", "1" }, { "pred_var_2", "\\((\\?\\w+)\\W", "1" },
                    { "var_with_digit_suffix", "(\\D+)\\d*", "1" } };
            String pName = null;
            Pattern p = null;
            Integer groupN = null;
            ArrayList pVal = null;
            for (final String[] strings : patternArray) {
                pName = strings[0];
                p = Pattern.compile(strings[1]);
                groupN = Integer.valueOf(strings[2]);
                pVal = new ArrayList();
                pVal.add(p);
                pVal.add(groupN);
                REGEX_PATTERNS.put(pName, pVal);
            }
        }
    }

    /*****************************************************************
     * This method finds regular expression matches in an input string using a
     * compiled Pattern and binding group index retrieved with patternKey. If
     * the ArrayList accumulator is provided, match results are added to it and
     * it is returned. If accumulator is not provided (is null), then a new
     * ArrayList is created and returned if matches are found.
     *
     * @param input
     *            The input String in which matches are sought.
     *
     * @param patternKey
     *            A String used as the retrieval key for a regular expression
     *            Pattern object, and an int index identifying a binding group.
     *
     * @param accumulator
     *            An optional ArrayList to which matches are added. Note that if
     *            accumulator is provided, it will be the return value even if
     *            no new matches are found in the input String.
     *
     * @return An ArrayList, or null if no matches are found and an accumulator
     *         is not provided.
     */
    private static ArrayList<String> getMatches(final CharSequence input, final String patternKey, final ArrayList<String> accumulator) {

        ArrayList<String> ans = null;
        if (accumulator != null)
            ans = accumulator;
        if (REGEX_PATTERNS == null)
            KB.compilePatterns();
        if (StringUtil.isNonEmptyString(input) && StringUtil.isNonEmptyString(patternKey)) {
            final Pattern p = KB.getCompiledPattern(patternKey);
            if (p != null) {
                final Matcher m = p.matcher(input);
                final int gidx = KB.getPatternGroupIndex(patternKey);
                if (gidx >= 0) {
                    while (m.find()) {
                        final String rv = m.group(gidx);
                        if (StringUtil.isNonEmptyString(rv)) {
                            if (ans == null)
                                ans = new ArrayList<>();
                            if (!(ans.contains(rv)))
                                ans.add(rv);
                        }
                    }
                }
            }
        }
        return ans;
    }

    /*****************************************************************
     * This method finds regular expression matches in an input string using a
     * compiled Pattern and binding group index retrieved with patternKey, and
     * returns the results, if any, in an ArrayList.
     *
     * @param input
     *            The input String in which matches are sought.
     *
     * @param patternKey
     *            A String used as the retrieval key for a regular expression
     *            Pattern object, and an int index identifying a binding group.
     *
     * @return An ArrayList, or null if no matches are found.
     */
    static ArrayList<String> getMatches(final String input, final String patternKey) {
        return KB.getMatches(input, patternKey, null);
    }

    /*****************************************************************
     * This method retrieves Formulas by asking the query expression queryLit, and
     * returns the results, if any, in an ArrayList.
     *
     * @param queryLit
     *            The query, which is assumed to be a List (atomic literal)
     *            consisting of a single predicate and its arguments. The
     *            arguments could be variables, constants, or a mix of the two,
     *            but only the first constant encountered in a left to right
     *            sweep over the literal will be used in the actual query.
     *
     * @return An ArrayList of Formula objects, or an empty ArrayList if no
     *         answers are retrieved.
     */
    private ArrayList<Formula> askWithLiteral(final List<String> queryLit) {

        ArrayList<Formula> ans = new ArrayList<Formula>();
        if ((queryLit instanceof List) && !(queryLit.isEmpty())) {
            final String pred = queryLit.get(0);
            if ("instance".equals(pred) && isVariable(queryLit.get(1)) && !(isVariable(queryLit.get(2)))) {
                final String className = queryLit.get(2);
                String inst = null;
                String fStr = null;
                Formula f = null;
                final Set<String> ai = getAllInstances(className);
                for (final String s : ai) {
                    inst = s;
                    fStr = ("(instance " + inst + ' ' + className + ')');
                    f = new Formula();
                    f.read(fStr);
                    ans.add(f);
                }
            }
            else if ("valence".equals(pred) && isVariable(queryLit.get(1))
                    && isVariable(queryLit.get(2))) {
                final TreeSet<String> ai = getAllInstances("Relation");
                final Iterator<String> it = ai.iterator();
                int valence = 0;
                while (it.hasNext()) {
                    final String inst = it.next();
                    valence = kbCache.valences.get(inst);
                    if (valence > 0) {
                        final String fStr = ("(valence " + inst + ' ' + valence + ')');
                        final Formula f = new Formula();
                        f.read(fStr);
                        ans.add(f);
                    }
                }
            }
            else {
                String constant = null;
                int cidx = -1;
                final int qlLen = queryLit.size();
                String term = null;
                for (int i = 1; i < qlLen; i++) {
                    term = queryLit.get(i);
                    if (StringUtil.isNonEmptyString(term) && !isVariable(term)) {
                        constant = term;
                        cidx = i;
                        break;
                    }
                }
                if (constant != null)
                    ans = askWithRestriction(cidx, constant, 0, pred);
                else
                    ans = ask("arg", 0, pred);
            }
        }
        return ans;
    }

    /*****************************************************************
     * This method retrieves formulas by asking the query expression queryLit, and
     * returns the results, if any, in an ArrayList.
     *
     * @param queryLit
     *            The query, which is assumed to be an atomic literal consisting
     *            of a single predicate and its arguments. The arguments could
     *            be variables, constants, or a mix of the two, but only the
     *            first constant encountered in a left to right sweep over the
     *            literal will be used in the actual query.
     *
     * @return An ArrayList of Formula objects, or an empty ArrayList if no
     *         answers are retrieved.
     */
    public ArrayList<Formula> askWithLiteral(final Formula queryLit) {

        final List<String> input = queryLit.literalToArrayList();
        return askWithLiteral(input);
    }

    /*****************************************************************
     * This method retrieves the upward transitive closure of all Class names
     * contained in the input setAt. The members of the input set are not included
     * in the result setAt.
     *
     * @param classNames
     *            A Set object containing SUO-KIF class names (Strings).
     *
     * @return A Set of SUO-KIF class names, which could be empty.
     */
    public Set<String> getAllSuperClasses(final Collection<String> classNames) {

        final Set<String> ans = new HashSet<>();
        for (final String term : classNames) {
            ans.addAll(kbCache.getParentClasses(term));
        }
        return ans;
    }

    /*****************************************************************
     * This method retrieves all instances of the classes named in the input setAt.
     *
     * @param classNames
     *            A Set of String, containing SUO-KIF class names
     * @return A TreeSet, possibly empty, containing SUO-KIF constant names.
     */
    private TreeSet<String> getAllInstances(final Set<String> classNames) {

        final TreeSet<String> ans = new TreeSet<String>();
        if ((classNames instanceof TreeSet) && !classNames.isEmpty()) {
            String name = null;
            for (final String className : classNames) {
                name = className;
                ans.addAll(kbCache.getParentClassesOfInstance(name));
            }
        }
        return ans;
    }

    /*****************************************************************
     * This method retrieves all instances of the class named in the input String.
     *
     * @param className
     *            The name of a SUO-KIF Class.
     * @return A TreeSet, possibly empty, containing SUO-KIF constant names.
     */
    private TreeSet<String> getAllInstances(final String className) {

        if (StringUtil.isNonEmptyString(className)) {
            final TreeSet<String> input = new TreeSet<String>();
            input.add(className);
            return getAllInstances(input);
        }
        return new TreeSet<>();
    }

    /*****************************************************************
     * This method tries to find or compute a valence for the input relation.
     *
     * @param relnName
     *            A String, the name of a SUO-KIF Relation.
     * @return An int value. -1 means that no valence value could be found. 0
     *         means that the relation is a VariableArityRelation. 1-5 are the
     *         standard SUO-KIF valence values.
     */
    public int getValence(final String relnName) {

        if (kbCache.valences.get(relnName) == null) {
            if (Formula.isLogicalOperator(relnName)) 
                                                        
                                                        
                return -1;
            System.out.println("Error in KB.getValence(): No valence found for " + relnName);
            return -1;
        }
        else
            return kbCache.valences.get(relnName);
    }

    /*****************************************************************
     *
     * @return an ArrayList containing all predicates in this KB.
     */
    public ArrayList<String> collectPredicates() {

        return new ArrayList<>(kbCache.instances.get("Predicate"));
    }

    /*****************************************************************
     *
     * @param obj
     *            Any object
     *
     * @return true if obj is a String representation of a LISP empty list, else
     *         false.
     */
    public static boolean isEmptyList(final Object obj) {
        return (StringUtil.isNonEmptyString(obj) && Formula.empty((String) obj));
    }

    /*****************************************************************
     * A static utility method.
     *
     * @param obj
     *            Presumably, a String.
     * @return true if obj is a SUO-KIF variable, else false.
     */
    private static boolean isVariable(final String obj) {

        if (StringUtil.isNonEmptyString(obj)) {
            return (obj.startsWith("?") || obj.startsWith("@"));
        }
        return false;
    }

    /*****************************************************************
     * A static utility method.
     *
     * @param obj
     *            A String.
     * @return true if obj is a SUO-KIF logical quantifier, else false.
     */
    public static boolean isQuantifier(final String obj) {

        return (StringUtil.isNonEmptyString(obj) && ("forall".equals(obj) || "exists".equals(obj)));
    }

    /*****************************************************************
     * A static utility method.
     *
     * @param obj
     *            Presumably, a String.
     * @return true if obj is a SUO-KIF commutative logical operator, else
     *         false.
     */
    public static boolean isCommutative(final String obj) {

        return (StringUtil.isNonEmptyString(obj) && ("and".equals(obj) || "or".equals(obj)));
    }

    /***************************************************************
     * Hyperlink terms identified with '&%' to the URL that brings up that target in the
     * browser. Handle (and ignore) suffixes on the target. For example
     * "&%Processes" would get properly linked to the target "Process", if present
     * in the knowledge base.
     */
    public String formatDocumentation(String href, final String documentation, final String language) {

        String formatted = documentation;
        if (StringUtil.isNonEmptyString(formatted)) {
            boolean isStaticFile = false;
            final StringBuilder sb = new StringBuilder(formatted);
            String suffix = "";
            if (StringUtil.emptyString(href)) {
                href = "";
                suffix = ".html";
                isStaticFile = true;
            }
            else if (!href.endsWith("&target="))
                href += "&target=";
            int i = -1;
            int j = -1;
            int start = 0;
            String term = "";
            String formToPrint = "";
            while ((start < sb.length()) && ((i = sb.indexOf("&%", start)) != -1)) {
                sb.delete(i, (i + 2));
                j = i;
                while ((j < sb.length()) && !Character.isWhitespace(sb.charAt(j)) && (int) sb.charAt(j) != (int) '"')
                    j++;
                while (j > i) {
                    term = sb.substring(i, j);
                    if (containsTerm(term))
                        break;
                    j--;
                }
                if (j > i) {
                    
                    
                    formToPrint = term;
                    final StringBuilder hsb = new StringBuilder("<a href=\"");
                    hsb.append(href);
                    hsb.append(isStaticFile ? StringUtil.toSafeNamespaceDelimiter(term) : term);
                    hsb.append(suffix);
                    hsb.append("\">");
                    hsb.append(formToPrint);
                    hsb.append("</a>");
                    sb.replace(i, j, hsb.toString());
                    start = (i + hsb.length());
                }
            }
            formatted = sb.toString();
        }
        return formatted;
    }

    /***************************************************************
     * Save the contents of the current KB to a file.
     */
    public String writeInferenceEngineFormulas(final Set<String> forms) {

        FileWriter fw = null;
        PrintWriter pw = null;
        String filename = null;
        try {
            final String inferenceEngine = KBmanager.manager.getPref("inferenceEngine");
            if (StringUtil.isNonEmptyString(inferenceEngine)) {
                final File executable = new File(inferenceEngine);
                if (executable.exists()) {
                    final File dir = executable.getParentFile();
                    final File file = new File(dir, (this.name + "-v.kif"));
                    filename = file.getCanonicalPath();
                    fw = new FileWriter(filename);
                    pw = new PrintWriter(fw);
                    for (final String form : forms) {
                        pw.println(form);
                        pw.println();
                    }
                }
                else
                    System.out.println("Error in KB.writeInferenceEngineFormulas(): no executable " + inferenceEngine);
            }
        }
        catch (final IOException ioe) {
            System.out.println("Error in KB.writeInferenceEngineFormulas(): writing file: " + filename);
            System.out.println(ioe.getMessage());
            ioe.printStackTrace();
        }
        finally {
            try {
                if (pw != null) {
                    pw.close();
                }
                if (fw != null) {
                    fw.close();
                }
            }
            catch (final Exception ex) {
            }
        }
        return filename;
    }









































































































































    /*****************************************************************
     * @return a defensive copy of loadFormatMapsAttempted.
     */
    public ArrayList<String> getLoadFormatMapsAttempted() {

        return Lists.newArrayList(loadFormatMapsAttempted);
    }












    /***************************************************************
     */
    public static void main(final String[] args) {


        try {
            KBmanager.manager.initializeOnce();
            final KB kb = KBmanager.manager.getKB(KBmanager.manager.getPref("sumokbname"));
            System.out.println(kb.getAllSub("ColorAttribute","subAttribute"));
            
            
            
            
        }
        catch (final Exception ioe) {
            System.out.println(ioe.getMessage());
        }

        
        
        
        /*
        System.out.println("KB.main(): termDepth of Object: " + kb.termDepth("Object"));
        System.out.println("KB.main(): termDepth of Table: " + kb.termDepth("Table"));
        System.out.println("KB.main(): termDepth of immediateSubclass: " + kb.termDepth("immediateSubclass"));
        System.out.println("KB.main(): termDepth of Wagon: " + kb.termDepth("Wagon"));
        System.out.println("KB.main(): termDepth of Foo: " + kb.termDepth("Foo"));
*/
        /*
         * String foo = "(rel bar \"test\")"; Formula f = new Formula();
         * f.read(foo); System.out.println(f.getArgument(2).equals("\"test\""));
         */
    }
}
