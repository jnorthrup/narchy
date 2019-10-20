
/* This code is copyright Articulate Software (c) 2003.  
This software is released under the GNU Public License <http:
Users of this code are also requested, to credit Articulate Software in any
writings, briefings, publications, presentations, or 
other representations of any software which incorporates,
builds on, or uses this code. Please cite the following
article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http:
*/

package nars.op.kif;

import java.util.*;

/** **************************************************************
 *  The code in the section below implements an algorithm for
 *    translating SUO-KIF expressions to clausal form.  The
 *    public methods are:
 * 
 *    public Formula clausify()
 *    public ArrayList clausifyWithRenameInfo()
 *    public ArrayList toNegAndPosLitsWithRenameInfo()
 * 
 * The result is a single formula in conjunctive normal form
 * (CNF), which is actually a set of (possibly negated) clauses
 * surrounded by an "or".
 */
public class Clausifier  {

    private Formula thisFormula = null;

     
     
    private static int VAR_INDEX = 0;

     
     
    private static int SKOLEM_INDEX = 0;

    /** ***************************************************************
     */
    public Clausifier(String s) {

        thisFormula = new Formula();
        thisFormula.read(s);
    }

    /** ***************************************************************
     */
    public String toString() {

        return thisFormula.theFormula;
    }
    
    /** ***************************************************************
     *  Turn a conjunction into an ArrayList of separate statements
     */
    public ArrayList<Formula> separateConjunctions() {

        if (!"and".equals(thisFormula.car())) {
            System.out.println("Error Formula.separateConjunctions(): not a conjunction " + thisFormula);
            return null;
        }
        var temp = new Formula();
        temp.read(thisFormula.cdr());
        var result = new ArrayList<Formula>();
        while (!temp.empty()) {
            var clause = new Formula();
            clause.read(temp.car());
            result.add(clause);
            temp.read(temp.cdr());
        }
        return result;
    }

    /** ***************************************************************
     *  convenience method
     */
    public static ArrayList<Formula> separateConjunctions(Formula f) {
        var temp = new Clausifier(f.theFormula);
        return temp.separateConjunctions();
    }

    /** ***************************************************************
     *  convenience method
     */
    public static Formula clausify(Formula f) {
        var temp = new Clausifier(f.theFormula);
        return temp.clausify();
    }

    /** ***************************************************************
     * Note this returns a List of mixed types!  Fixme!
     * 
     * @return an ArrayList that contains three items: The new
     * clausal-form Formula, the original (input) SUO-KIF Formula, and
     * a Map containing a graph of all the variable substitions done
     * during the conversion to clausal form.  This Map makes it
     * possible to retrieve the correspondence between the variables
     * in the clausal form and the variables in the original
     * Formula. Some elements might be null if a clausal form
     * cannot be generated.
     *
     * @see clausify()
     * @see toNegAndPosLitsWithRenameInfo()
     * @see toCanonicalClausalForm();
     */
    public ArrayList clausifyWithRenameInfo() {

        var old = new Formula();
        old.theFormula = thisFormula.theFormula;
        Formula ans = null;
        thisFormula = equivalencesOut();
        thisFormula = implicationsOut();
        thisFormula = negationsIn();
        var allRenames = new HashMap();
        Map scopedRenames = new HashMap();
        Map topLevelVars = new HashMap();
        thisFormula = renameVariables(topLevelVars, scopedRenames, allRenames);
        thisFormula = existentialsOut();
        thisFormula = universalsOut();
        thisFormula = disjunctionsIn();
        Map standardizedRenames = new HashMap();
        thisFormula = standardizeApart(standardizedRenames);
        allRenames.putAll(standardizedRenames);
        var result = new ArrayList();
        result.add(thisFormula);
        result.add(old);
        result.add(allRenames);
        
        return result;
    }

    /** ***************************************************************
     * TODO: Note mixed types in return List!  Fixme!
     * 
     * This method converts the SUO-KIF Formula to an ArrayList of
     * clauses.  Each clause is an ArrayList containing an ArrayList
     * of negative literals, and an ArrayList of positive literals.
     * Either the neg lits list or the pos lits list could be empty.
     * Each literal is a Formula object.
     *
     * The first object in the returned ArrayList is an ArrayList of
     * clauses.
     *
     * The second object in the returned ArrayList is the original
     * (input) Formula object (this).
     *
     * The third object in the returned ArrayList is a Map that
     * contains a graph of all the variable substitions done during
     * the conversion of this Formula to clausal form.  This Map makes
     * it possible to retrieve the correspondences between the
     * variables in the clausal form and the variables in the original
     * Formula.
     *
     * @see clausify()
     * @see clausifyWithRenameInfo()
     * @see toCanonicalClausalForm();
     *
     * @return A three-element ArrayList, 
     *
     * [ 
     *   
     *   [ 
     *     
     *     [ 
     *       
     *       [ Formula1, Formula2, ..., FormulaN ],
     *       
     *       [ Formula1, Formula2, ..., FormulaN ] 
     *     ],
     *
     *     
     *     [ 
     *       
     *       [ Formula1, Formula2, ..., FormulaN ],
     *       
     *       [ Formula1, Formula2, ..., FormulaN ] 
     *     ],
     *
     *     ...,
     *   ],
     *
     *   
     *   <the-original-Formula>,
     *
     *   
     *   {a-Map-of-variable-renamings},
     *
     * ]
     */
    public ArrayList toNegAndPosLitsWithRenameInfo() {

        var ans = new ArrayList();
        List<Formula> clausesWithRenameInfo = this.clausifyWithRenameInfo();
        if (clausesWithRenameInfo.size() == 3) {
            var clausalForm = clausesWithRenameInfo.get(0);
            var cForm = new Clausifier(clausalForm.theFormula);
            ArrayList clauses = cForm.operatorsOut();
            if ((clauses != null) && !clauses.isEmpty()) {

                var newClauses = new ArrayList();
                Formula clause = null;
                for (var o : (Iterable<Formula>) clauses) {
                    var negLits = new ArrayList<Formula>();
                    var literals = new ArrayList();
                    literals.add(negLits);
                    var posLits = new ArrayList<Formula>();
                    literals.add(posLits);
                    clause = o;
                    if (clause.listP()) {
                        while (!clause.empty()) {
                            var lit = clause.car();
                            var litF = new Formula();
                            litF.read(lit);
                            var isNegLit = false;
                            if (litF.listP() && litF.car().equals(Formula.NOT)) {
                                litF.read(litF.cadr());
                                isNegLit = true;
                            }
                            if (litF.theFormula.equals(Formula.LOG_FALSE))
                                isNegLit = true;
                            if (isNegLit)
                                negLits.add(litF);
                            else
                                posLits.add(litF);
                            clause = clause.cdrAsFormula();
                        }
                    } else if (clause.theFormula.equals(Formula.LOG_FALSE))
                        negLits.add(clause);
                    else
                        posLits.add(clause);
                    newClauses.add(literals);
                }
                
                
                ans.add(newClauses);
            }
            if (ans.size() == 1) {
                var cwriLen = clausesWithRenameInfo.size();
                for (var j = 1; j < cwriLen; j++)
                    ans.add(clausesWithRenameInfo.get(j));                    
            }
        }
        return ans;
    }

    /** ***************************************************************
     *  convenience method
     */
    public static ArrayList toNegAndPosLitsWithRenameInfo(Formula f) {
        var temp = new Clausifier(f.theFormula);
        return temp.toNegAndPosLitsWithRenameInfo();
    }

    /** ***************************************************************
     * This method converts the SUO-KIF Formula to a canonical version
     * of clausal (resolution, conjunctive normal) form with Skolem
     * functions, following the procedure described in Logical
     * Foundations of Artificial Intelligence, by Michael Genesereth
     * and Nils Nilsson, 1987, pp. 63-66.  In canonical form, all
     * variables are renamed from index 0 and all literals are
     * alphabetically sorted.
     *
     * @see clausify()
     * @see clausifyWithRenameInfo()
     * @see toNegAndPosLitsWithRenameInfo()
     *
     * @return The Formula in a canonicalized version of clausal form
     */
    public Formula toCanonicalClausalForm() {

        var ans = new Formula();
        List clauseData = this.toNegAndPosLitsWithRenameInfo();
        if (clauseData.isEmpty()) 
            ans = thisFormula;
        else {
            Collection clauses = (List) clauseData.get(0);
            if (!clauses.isEmpty()) {
                List sortedClauses = new ArrayList();
                StringBuilder sb = null;
                Iterator itc = null;
                for (itc = clauses.iterator(); itc.hasNext();) {
                    var clause = (List) itc.next();
                    if (clause.size() == 2) {
                        sb = new StringBuilder();
                        var neglits = (List) clause.get(0);
                        if (neglits.size() > 1) Collections.sort(neglits);
                        var i = 0;
                        Iterator itl = null;
                        for (itl = neglits.iterator(); itl.hasNext(); i++) {
                            if (i > 0) sb.append(Formula.SPACE);
                            sb.append(Formula.LP);
                            sb.append(Formula.NOT);
                            sb.append(Formula.SPACE);
                            sb.append(itl.next());
                            sb.append(Formula.RP);
                        }
                        var poslits = (List) clause.get(1);
                        if (!poslits.isEmpty()) {
                            Collections.sort(poslits);
                            for (itl = poslits.iterator(); itl.hasNext(); i++) {
                                if (i > 0) sb.append(Formula.SPACE);
                                sb.append(itl.next());
                            }
                        }
                        if (i > 1) {
                            sb.insert(0, Formula.SPACE);
                            sb.insert(0, Formula.OR);
                            sb.insert(0, Formula.LP);
                            sb.append(Formula.RP);
                        }
                        sortedClauses.add(sb.toString());
                    }
                }
                Collections.sort(sortedClauses);
                sb = new StringBuilder();
                var j = 0;
                for (itc = sortedClauses.iterator(); itc.hasNext(); j++) {
                    if (j > 0) sb.append(Formula.SPACE);
                    sb.append(itc.next());
                }
                if (j > 1) {
                    sb.insert(0, Formula.SPACE);
                    sb.insert(0, Formula.AND);
                    sb.insert(0, Formula.LP);
                    sb.append(Formula.RP);
                }
                ans.read(normalizeVariables(sb.toString()));
            }
        }
        return ans;
    }

    /** ***************************************************************
     *  convenience method
     */
    public static Formula toCanonicalClausalForm(Formula f) {
        var temp = new Clausifier(f.theFormula);
        return temp.toCanonicalClausalForm();
    }

    /** ***************************************************************
     * <p>This method returns an open Formula that constitutes a KIF
     * query expression, which is generated from the canonicalized
     * negation of the original Formula.  The original Formula is
     * assumed to be a "rule", having both positive and negative
     * literals when converted to canonical clausal form.  This method
     * returns a query expression that can be used to test the
     * validity of the original Formula.  If the query succeeds
     * (evaluates to true, or retrieves variable bindings), then the
     * negation of the original Formula is valid, and the original
     * Formula must be invalid.</p>
     *
     * <p>Note that this method will work reliably only for standard
     * first order KIF expressions, and probably will not produce
     * expected (or sane) results if used with non-standard KIF.</p>
     *
     * @see Formula.toCanonicalClausalForm()
     *
     * @return A String representing an open KIF query expression.
     */
    protected Formula toOpenQueryForNegatedDualForm() {

        var qF = new Formula();
        var temp = new Formula();
        temp.theFormula = thisFormula.theFormula;
        qF.read(temp.makeQuantifiersExplicit(false));


        var negQF = new Formula();
        negQF.read("(not " + qF.theFormula + ')');


        var canonNegF = new Formula();
        canonNegF.read(Clausifier.toCanonicalClausalForm(negQF).theFormula);


        var queryF = new Formula();
        queryF.read(normalizeVariables(canonNegF.theFormula,
                                       true));
        var result = queryF;
        return result;
    }

    /** ***************************************************************
     * This method returns a canonical version of this Formula,
     * assumed to be a KIF "special" form, in which all internal
     * first-order KIF formulae are replaced by their canonical
     * versions, and all variables are renamed, in left to right
     * depth-first order of occurrence, starting from index 1.  This
     * method is intended to be applied to higher-order constructs
     * that expand the syntax of KIF.  To canonicalize ordinary
     * first-order KIF formulae, just use
     * <code>Formula.toCanonicalClausalForm()</code>.
     *
     * @see Formula.toCanonicalClausalForm()
     *
     * @param preserveSharedVariables If true, all variable sharing
     * between embedded expressions will be preserved, even though all
     * variables will still be renamed as part of the canonicalization
     * process
     *
     * @return A String representing the canonicalized version of this
     * special, non-standard KIF expression.
     */
    protected Formula toCanonicalKifSpecialForm(boolean preserveSharedVariables) {


        var flist = thisFormula.theFormula;
        var vpref = Formula.V_PREF;
        var rpref = Formula.R_PREF;
        var vrepl = "vvvv";
        var rrepl = "rrrr";
        if (preserveSharedVariables) {
            if (flist.contains(vpref)) flist = flist.replace(vpref, vrepl);
            if (flist.contains(rpref)) flist = flist.replace(rpref, rrepl);
        }
        var sb = new StringBuilder();
        if (Formula.listP(flist)) {
            if (Formula.empty(flist)) 
                sb.append(flist);                
            else {
                var f = new Formula();
                f.read(flist);
                var arg0 = f.car();
                if (Formula.isLogicalOperator(arg0)) 
                    sb.append(Clausifier.toCanonicalClausalForm(f).theFormula);                    
                else {
                    List tuple = f.literalToArrayList();
                    sb.append(Formula.LP);
                    var i = 0;
                    for (var it = tuple.iterator(); it.hasNext(); i++) {
                        var nextF = new Formula();
                        nextF.read((String) it.next());
                        if (i > 0) sb.append(Formula.SPACE);
                        sb.append(Clausifier.toCanonicalKifSpecialForm(nextF,false).theFormula);
                    }
                    sb.append(Formula.RP);
                }
            }
        }
        else 
            sb.append(flist);        

        flist = sb.toString();
        if (preserveSharedVariables) {
            flist = flist.replace(vrepl, vpref);
            flist = flist.replace(rrepl, rpref);
        }
        flist = normalizeVariables(flist);
        var canonSF = new Formula();
        canonSF.read(flist);
        var result = canonSF;
        return result;
    }

    /** ***************************************************************
     *  convenience method
     */
    private static Formula toCanonicalKifSpecialForm(Formula f, boolean preserveSharedVariables) {

        var temp = new Clausifier(f.theFormula);
        return temp.toCanonicalKifSpecialForm(preserveSharedVariables);
    }

    /** ***************************************************************
     * Returns a String in which all variables and row variables have
     * been normalized -- renamed, in depth-first order of occurrence,
     * starting from index 1 -- to support comparison of Formulae for
     * equality.
     *
     * @see normalizeVariables_1();
     *
     * @param input A String representing a SUO-KIF Formula, possibly
     * containing variables to be normalized
     * 
     * @return A String, typically representing a SUO-KIF Formula or
     * part of a Formula, in which the original variables have been
     * replaced by normalized forms
     */
    public static String normalizeVariables(String input) {
        return normalizeVariables(input, false);
    }

    /** ***************************************************************
     * Returns a String in which all variables and row variables have
     * been normalized -- renamed, in depth-first order of occurrence,
     * starting from index 1 -- to support comparison of Formulae for
     * equality.
     *
     * @see normalizeVariables_1();
     *
     * @param input A String representing a SUO-KIF Formula, possibly
     * containing variables to be normalized
     * 
     * @param replaceSkolemTerms If true, all Skolem terms in input
     * are treated as variables and are replaced with normalized
     * variable terms
     * 
     * @return A String, typically representing a SUO-KIF Formula or
     * part of a Formula, in which the original variables have been
     * replaced by normalized forms
     */
    protected static String normalizeVariables(String input, boolean replaceSkolemTerms) {

        int[] idxs = {1, 1};
        Map vmap = new HashMap();
        var result = normalizeVariables_1(input, idxs, vmap, replaceSkolemTerms);
        return result;
    }

    /** ***************************************************************
     * An internal helper method for normalizeVariables(String input).
     *
     * @see normalizeVariables();
     *
     * @param input A String possibly containing variables to be
     * normalized
     *
     * @param idxs A two-place int[] in which int[0] is the current
     * variable index, and int[1] is the current row variable index
     *
     * @param vmap A Map in which the keys are old variables and the
     * values are new variables
     * 
     * @return A String, typically a representing a SUO-KIF Formula or
     * part of a Formula.
     */
    protected static String normalizeVariables_1(String input,int[] idxs, 
                                                 Map vmap, boolean replaceSkolemTerms) {

        var sb = new StringBuilder();
        var flist = input.trim();
        var isSkolem = Formula.isSkolemTerm(flist);
        if ((replaceSkolemTerms && isSkolem) || Formula.isVariable(flist)) {
            var newvar = (String) vmap.get(flist);
            if (newvar == null) {
                var rvbase = (Formula.RVAR + "VAR");
                var vbase = Formula.VVAR;
                newvar = ((flist.startsWith(Formula.V_PREF) || isSkolem)
                          ? (vbase + idxs[0]++)
                          : (rvbase + idxs[1]++));
                vmap.put(flist, newvar);
            }
            sb.append(newvar);
        }
        else if (Formula.listP(flist)) {
            if (Formula.empty(flist)) 
                sb.append(flist);                
            else {
                var f = new Formula();
                f.read(flist);
                List tuple = f.literalToArrayList();
                sb.append(Formula.LP);
                var i = 0;
                for (var it = tuple.iterator(); it.hasNext(); i++) {
                    if (i > 0) sb.append(Formula.SPACE);
                    sb.append(normalizeVariables_1((String) it.next(),idxs,
                                                   vmap,replaceSkolemTerms));
                }
                sb.append(Formula.RP);
            }
        }
        else 
            sb.append(flist);
        var result = sb.toString();
        return result;
    }

    /** ***************************************************************
     * This method converts every occurrence of '<=>' in the Formula
     * to a conjunct with two occurrences of '=>'.
     * 
     * @return A Formula with no occurrences of '<=>'.
     *
     */
    private Formula equivalencesOut() {

        var ans = thisFormula;
        if (thisFormula.listP() && !(thisFormula.empty())) {
            var head = thisFormula.car();
            String theNewFormula = null;
            if (!StringUtil.emptyString(head) && Formula.listP(head)) {
                var headF = new Clausifier(head);
                var newHead = headF.equivalencesOut().theFormula;
                var theNewClausifier = new Clausifier(thisFormula.cdr());
                theNewFormula = theNewClausifier.equivalencesOut().cons(newHead).theFormula;
            }
            else if (head.equals(Formula.IFF)) {
                var second = thisFormula.cadr();
                var secondF = new Clausifier(second);
                var newSecond = secondF.equivalencesOut().theFormula;
                var third = thisFormula.caddr();
                var thirdF = new Clausifier(third);
                var newThird = thirdF.equivalencesOut().theFormula;
                theNewFormula = ("(and (=> " + newSecond + ' ' + newThird
                                 + ") (=> " + newThird + ' ' + newSecond + "))");
            }
            else {
                var fourth = new Clausifier(thisFormula.cdrAsFormula().theFormula);
                theNewFormula = fourth.equivalencesOut().cons(head).theFormula;                
            }
            if (theNewFormula != null) {
                ans = new Formula();
                ans.read(theNewFormula);
            }
        }
        return ans;
    }

    /** ***************************************************************
     * This method converts every occurrence of '(=> LHS RHS' in the
     * Formula to a disjunct of the form '(or (not LHS) RHS)'.
     * 
     * @return A Formula with no occurrences of '=>'.
     *
     */
    private Formula implicationsOut() {

        var ans = thisFormula;
        if (thisFormula.listP() && !thisFormula.empty()) {
            var head = thisFormula.car();
            String theNewFormula = null;
            if (!StringUtil.emptyString(head) && Formula.listP(head)) {
                var headF = new Clausifier(head);
                var newHead = headF.implicationsOut().theFormula;
                var theNewClausifier = new Clausifier(thisFormula.cdr());
                theNewFormula = theNewClausifier.implicationsOut().cons(newHead).theFormula;
            }
            else if (head.equals(Formula.IF)) {
                var second = thisFormula.cadr();
                var secondF = new Clausifier(second);
                var newSecond = secondF.implicationsOut().theFormula;
                var third = thisFormula.caddr();
                var thirdF = new Clausifier(third);
                var newThird = thirdF.implicationsOut().theFormula;
                theNewFormula = ("(or (not " + newSecond + ") " + newThird + ')');
            }
            else {
                var fourth = new Clausifier(thisFormula.cdrAsFormula().theFormula);
                theNewFormula = fourth.implicationsOut().cons(head).theFormula;                
            }
            if (theNewFormula != null) {
                ans = new Formula();
                ans.read(theNewFormula);
            }
        }
        return ans;
    }

    /** ***************************************************************
     * This method 'pushes in' all occurrences of 'not', so that each
     * occurrence has the narrowest possible scope, and also removes
     * from the Formula all occurrences of '(not (not ...))'.
     *
     * @see negationsIn_1().
     *
     * @return A Formula with all occurrences of 'not' accorded
     * narrowest scope, and no occurrences of '(not (not ...))'.
     */
    private Formula negationsIn() {

        var f = thisFormula;
        var ans = negationsIn_1();
        
        while (!f.theFormula.equals(ans.theFormula)) {     
            f = ans;
            ans = negationsIn_1(f);
        }
        return ans;
    }

    /** ***************************************************************
     * This method is used in negationsIn().  It recursively 'pushes
     * in' all occurrences of 'not', so that each occurrence has the
     * narrowest possible scope, and also removes from the Formula all
     * occurrences of '(not (not ...))'.
     *
     * @see negationsIn().
     *
     * @return A Formula with all occurrences of 'not' accorded
     * narrowest scope, and no occurrences of '(not (not ...))'.
     */
    private Formula negationsIn_1() {  

        if (thisFormula.listP()) {
            if (thisFormula.empty()) { return thisFormula; }
            var arg0 = thisFormula.car();
            var arg1 = thisFormula.cadr();
            if (arg0.equals(Formula.NOT) && Formula.listP(arg1)) {
                var arg1F = new Formula();
                arg1F.read(arg1);
                var arg0_of_arg1 = arg1F.car();
                if (arg0_of_arg1.equals(Formula.NOT)) {
                    var arg1_of_arg1 = arg1F.cadr();
                    var arg1_of_arg1F = new Formula();
                    arg1_of_arg1F.read(arg1_of_arg1);
                    return arg1_of_arg1F;
                }
                if (Formula.isCommutative(arg0_of_arg1)) {
                    String newOp = null;
                    if (arg0_of_arg1.equals(Formula.AND))
                    	newOp = Formula.OR; 
                    else
                    	newOp = Formula.AND;
                    return listAll(arg1F.cdrAsFormula(),"(not ", ")").cons(newOp);
                }
                if (Formula.isQuantifier(arg0_of_arg1)) {
                    var vars = arg1F.cadr();
                    var arg2_of_arg1 = arg1F.caddr();
                    String quant = null;
                    if (arg0_of_arg1.equals(Formula.UQUANT))
                    	quant = Formula.EQUANT;
                    else
                    	quant = Formula.UQUANT;
                    arg2_of_arg1 = ("(not " + arg2_of_arg1 + ')');
                    var arg2_of_arg1F = new Formula();
                    arg2_of_arg1F.read(arg2_of_arg1);
                    var theNewFormula = ('(' + quant + ' ' + vars + ' '
                                            + negationsIn_1(arg2_of_arg1F).theFormula + ')');
                    var newF = new Formula();
                    newF.read(theNewFormula);
                    return newF;
                }
                var theNewFormula = ("(not " + negationsIn_1(arg1F).theFormula + ')');
                var newF = new Formula();
                newF.read(theNewFormula);
                return newF;
            }
            if (Formula.isQuantifier(arg0)) {
                var arg2 = thisFormula.caddr();
                var arg2F = new Formula();
                arg2F.read(arg2);
                var newArg2 = negationsIn_1(arg2F).theFormula;
                var theNewFormula = ('(' + arg0 + ' ' + arg1 + ' ' + newArg2 + ')');
                var newF = new Formula();
                newF.read(theNewFormula);
                return newF;
            }
            if (Formula.listP(arg0)) {
                var arg0F = new Formula();
                arg0F.read(arg0);
                return negationsIn_1(thisFormula.cdrAsFormula()).cons(negationsIn_1(arg0F).theFormula);
            }
            return negationsIn_1(thisFormula.cdrAsFormula()).cons(arg0);
        }
        return thisFormula;
    }

    /** ***************************************************************
     *  convenience method
     */
    private static Formula negationsIn_1(Formula f) {
        var temp = new Clausifier(f.theFormula);
        return temp.negationsIn_1();
    }

    /** ***************************************************************
     * This method augments each element of the Formula by
     * concatenating optional Strings before and after the element.
     *
     * Note that in most cases the input Formula will be simply a
     * list, not a well-formed SUO-KIF Formula, and that the output
     * will therefore not necessarily be a well-formed Formula.
     *
     * @param before A String that, if present, is prepended to every
     * element of the Formula.
     *
     * @param after A String that, if present, is postpended to every
     * element of the Formula.
     * 
     * @return A Formula, or, more likely, simply a list, with the
     * String values corresponding to before and after added to each
     * element.     
     */
    private Formula listAll(String before, String after) {

        var ans = thisFormula;
        if (thisFormula.listP()) {
            var theNewFormula = "";
            var f = thisFormula;
            while (!(f.empty())) {
                var element = f.car();
                if (!StringUtil.emptyString(before)) 
                    element = (before + element);                
                if (!StringUtil.emptyString(after)) 
                    element += after;                
                theNewFormula += (Formula.SPACE + element);
                f = f.cdrAsFormula();
            }
            theNewFormula = (Formula.LP + theNewFormula.trim() + Formula.RP);
            if (!StringUtil.emptyString(theNewFormula)) {
                ans = new Formula();
                ans.read(theNewFormula);
            }
        }
        return ans;
    }

    /** ***************************************************************
     *  convenience method
     */
    private static Formula listAll(Formula f, String before, String after) {

        var temp = new Clausifier(f.theFormula);
        return temp.listAll(before,after);
    }

    /** ***************************************************************
     * This method increments VAR_INDEX and then returns the new int
     * value.  If VAR_INDEX is already at Integer.MAX_VALUE, then
     * VAR_INDEX is reset to 0.
     * 
     * @return An int value between 0 and Integer.MAX_VALUE inclusive.
     */
    private static int incVarIndex() {

        var oldVal = VAR_INDEX;
        if (oldVal == Integer.MAX_VALUE) 
            VAR_INDEX = 0;        
        else 
            ++VAR_INDEX;        
        return VAR_INDEX;
    }

    /** ***************************************************************
     * This method increments SKOLEM_INDEX and then returns the new int
     * value.  If SKOLEM_INDEX is already at Integer.MAX_VALUE, then
     * SKOLEM_INDEX is reset to 0.
     * 
     * @return An int value between 0 and Integer.MAX_VALUE inclusive.
     */
    private static int incSkolemIndex() {

        var oldVal = SKOLEM_INDEX;
        if (oldVal == Integer.MAX_VALUE)
            SKOLEM_INDEX = 0;        
        else
            ++SKOLEM_INDEX;        
        return SKOLEM_INDEX;
    }

    /** ***************************************************************
     * This method returns a new SUO-KIF variable String, modifying
     * any digit suffix to ensure that the variable will be unique.
     *
     * @param prefix An optional variable prefix string.
     * 
     * @return A new SUO-KIF variable.
     */
    private static String newVar(String prefix) {

        var base = Formula.VX;
        var varIdx = Integer.toString(incVarIndex());
        if (!StringUtil.emptyString(prefix)) {
            List woDigitSuffix = KB.getMatches(prefix, "var_with_digit_suffix");
            if (woDigitSuffix != null) 
                base = (String) woDigitSuffix.get(0);            
            else if (prefix.startsWith(Formula.RVAR)) 
                base = Formula.RVAR;            
            else if (prefix.startsWith(Formula.VX)) 
                base = Formula.VX;
            else 
                base = prefix;            
            if (!(base.startsWith(Formula.V_PREF) || base.startsWith(Formula.R_PREF))) 
                base = (Formula.V_PREF + base);            
        }
        return (base + varIdx);
    }

    /** ***************************************************************
     * This method returns a new SUO-KIF variable String, adding a
     * digit suffix to ensure that the variable will be unique.
     *
     * @return A new SUO-KIF variable
     */
    private static String newVar() {
        return newVar(null);
    }

    /** ***************************************************************
     * This method returns a new SUO-KIF row variable String,
     * modifying any digit suffix to ensure that the variable will be
     * unique.
     *
     * @return A new SUO-KIF row variable.
     */
    private static String newRowVar() {
        return newVar(Formula.RVAR);
    }

    /** ***************************************************************
     *  Replace term2 with term1
     */
    public Formula rename(String term2, String term1) {

        var newFormula = new Formula();
        newFormula.read("()");
        if (thisFormula.atom()) {
            if (thisFormula.theFormula.equals(term2)) 
                thisFormula.theFormula = term1;
            return thisFormula;
        }
        if (!thisFormula.empty()) {
            var f1 = new Formula();
            f1.read(thisFormula.car());
            if (f1.listP()) 
                newFormula = newFormula.cons(f1.rename(term2,term1));            
            else
                newFormula = newFormula.append(f1.rename(term2,term1));
            var f2 = new Formula();
            f2.read(thisFormula.cdr());
            newFormula = newFormula.append(f2.rename(term2,term1));
        }
        return newFormula;
    }

    /** ***************************************************************
     *  Replace variables with a value as given by the map argument
     */
    public Formula substituteVariables(Map<String,String> m) {

        var newFormula = new Formula();
        newFormula.read("()");
        if (thisFormula.atom()) {
            if (m.containsKey(thisFormula.theFormula)) {
                thisFormula.theFormula = m.get(thisFormula.theFormula);
                if (thisFormula.listP()) 
                    thisFormula.theFormula = '(' + thisFormula.theFormula + ')';
            }
            return thisFormula;
        }
        if (!thisFormula.empty()) {
            var f1 = new Formula();
            f1.read(thisFormula.car());
            if (f1.listP())
                newFormula = newFormula.cons(f1.substituteVariables(m));            
            else
                newFormula = newFormula.append(f1.substituteVariables(m));
            var f2 = new Formula();
            f2.read(thisFormula.cdr());
            newFormula = newFormula.append(f2.substituteVariables(m));
        }
        return newFormula;
    }

    /** ***************************************************************
     *  Extract all variables in a list
     */
    public static Set<String> extractVariables(Formula f) {

        Set<String> result = new TreeSet<>();
        extractVariablesRecursively(f, result);
        return result;
    }

    /** ***************************************************************
     *  Extract all variables in a list
     */
    private static void extractVariablesRecursively(Formula f, Set<String> vars) {

        if (f.atom()) {
            if (f.isVariable() || Formula.isSkolemTerm(f.theFormula)) {
                vars.add(f.theFormula);
            }
        } else if (!f.empty()) {
            var f1 = new Formula();
            f1.read(f.car());
            extractVariablesRecursively(f1, vars);
            var f2 = new Formula();
            f2.read(f.cdr());
            extractVariablesRecursively(f2, vars);
        }
    }

    /** **************************************************************
     *  Counter for instantiateVariables() to make sure generated
     *  symbols are unique.
     */
    private static int _GENSYM_COUNTER = 0;

    /** **************************************************************
     *  Create constants to fill variables.
     */
    public Formula instantiateVariables() {

        var f = renameVariables();
        var varList = f.collectVariables();
        var al = varList.get(0);
        al.addAll(varList.get(1));
        Map<String, String> vars = new TreeMap<>();
        for (var s : al) {
            _GENSYM_COUNTER++;
            var value = "GenSym" + _GENSYM_COUNTER;
            vars.put(s, value);
        }
        return f.substituteVariables(vars);
    }

    /** ***************************************************************
     * This method returns a new Formula in which all variables have
     * been renamed to ensure uniqueness.
     *
     * @see clausify()
     * @see renameVariables(Map topLevelVars, Map scopedRenames)
     *
     * @return A new SUO-KIF Formula with all variables renamed.
     */
    private Formula renameVariables() {

        Map topLevelVars = new HashMap();
        Map scopedRenames = new HashMap();
        Map allRenames = new HashMap();
        return renameVariables(topLevelVars, scopedRenames, allRenames);
    }

    /** ***************************************************************
     *  convenience method
     */
    public static Formula renameVariables(Formula f) {

        var temp = new Clausifier(f.theFormula);
        return temp.renameVariables();
    }

    /** ***************************************************************
     *  convenience method
     */
    public static Formula renameVariables(Formula f, Map topLevelVars, Map scopedRenames, Map allRenames) {

        var temp = new Clausifier(f.theFormula);
        return temp.renameVariables(topLevelVars,scopedRenames,allRenames);
    }

    /** ***************************************************************
     * This method returns a new Formula in which all variables have
     * been renamed to ensure uniqueness.
     *
     * @see renameVariables().
     *
     * @param topLevelVars A Map that is used to track renames of
     * implicitly universally quantified variables.
     *
     * @param scopedRenames A Map that is used to track renames of
     * explicitly quantified variables.
     *
     * @param allRenames A Map from all new vars in the Formula to
     * their old counterparts.
     *
     * @return A new SUO-KIF Formula with all variables renamed.
     */
    private Formula renameVariables(Map<String,String> topLevelVars, Map<String,String> scopedRenames, Map<String,String> allRenames) {

        if (thisFormula.listP()) {
            if (thisFormula.empty()) { return thisFormula; }
            var arg0 = thisFormula.car();
            if (Formula.isQuantifier(arg0)) {          
                
                Map<String,String> newScopedRenames = new HashMap<>(scopedRenames);
                var oldVars = thisFormula.cadr();
                var oldVarsF = new Formula();
                oldVarsF.read(oldVars);
                var newVars = "";
                while (oldVarsF != null && !oldVarsF.empty()) {
                    var oldVar = oldVarsF.car();
                    var newVar = newVar();
                    newScopedRenames.put(oldVar, newVar);
                    allRenames.put(newVar, oldVar);
                    newVars += (Formula.SPACE + newVar);
                    oldVarsF = oldVarsF.cdrAsFormula();
                }
                newVars = (Formula.LP + newVars.trim() + Formula.RP);
                var arg2 = thisFormula.caddr();
                var arg2F = new Formula();
                arg2F.read(arg2);
                var newArg2 = Clausifier.renameVariables(arg2F,topLevelVars,newScopedRenames,
                                                       allRenames).theFormula;
                var theNewFormula = (Formula.LP + arg0 + Formula.SPACE + newVars + Formula.SPACE + newArg2 + Formula.RP);
                var newF = new Formula();
                newF.read(theNewFormula);
                return newF;
            }
            var arg0F = new Formula();
            arg0F.read(arg0);
            var newArg0 = Clausifier.renameVariables(arg0F,topLevelVars,scopedRenames,
                                                   allRenames).theFormula;
            var newRest = Clausifier.renameVariables(thisFormula.cdrAsFormula(),topLevelVars,scopedRenames,
                                                                 allRenames).theFormula;
            var newRestF = new Formula();
            newRestF.read(newRest);
            var theNewFormula = newRestF.cons(newArg0).theFormula;
            var newF = new Formula();
            newF.read(theNewFormula);
            return newF;
        }
        if (Formula.isVariable(thisFormula.theFormula)) {
            var rnv = scopedRenames.get(thisFormula.theFormula);
            if (StringUtil.emptyString(rnv)) {
                rnv = topLevelVars.get(thisFormula.theFormula);
                if (StringUtil.emptyString(rnv)) {
                    rnv = newVar();
                    topLevelVars.put(thisFormula.theFormula, rnv);
                    allRenames.put(rnv, thisFormula.theFormula);
                }
            }
            var newF = new Formula();
            newF.read(rnv);
            return newF;
        }
        return thisFormula;
    }

    /** ***************************************************************
     * This method returns a new, unique skolem target with each
     * invocation.
     *
     * @param vars A sorted TreeSet of the universally quantified
     * variables that potentially define the skolem target.  The set may
     * be empty.
     *
     * @return A String.  The string will be a skolem functional target
     * (a list) if vars contains variables.  Otherwise, it will be an
     * atomic constant.
     */
    private static String newSkolemTerm(Set<String> vars) {

        var ans = Formula.SK_PREF;
        var idx = incSkolemIndex();
        if ((vars != null) && !vars.isEmpty()) {

            ans += (Formula.FN_SUFF + idx);
            for (var var : vars) {
                ans += (Formula.SPACE + var);
            }
            ans = (Formula.LP + ans + Formula.RP);
        }
        else 
            ans += idx;        
        return ans;
    }      

    /** ***************************************************************
     * This method returns a new Formula in which all existentially
     * quantified variables have been replaced by Skolem terms.
     *
     * @see existentialsOut(Map evSubs, TreeSet iUQVs, TreeSet scopedUQVs)
     * @see collectIUQVars(TreeSet iuqvs, TreeSet scopedVars)
     *
     * @return A new SUO-KIF Formula without existentially quantified
     * variables.
     */
    private Formula existentialsOut() {


        var iUQVs = new TreeSet<String>();


        var scopedVars = new TreeSet<String>();


        collectIUQVars(iUQVs, scopedVars);


        var scopedUQVs = new TreeSet<String>();
        Map<String, String> evSubs = new HashMap<>();
        return existentialsOut(evSubs, iUQVs, scopedUQVs);
    }

    /** ***************************************************************
     * This method returns a new Formula in which all existentially
     * quantified variables have been replaced by Skolem terms.
     *
     * @see existentialsOut()
     *
     * @param evSubs A Map of variable - skolem target substitution
     * pairs.
     *
     * @param iUQVs A TreeSet of implicitly universally quantified
     * variables.
     *
     * @param scopedUQVs A TreeSet of explicitly universally
     * quantified variables.
     *
     * @return A new SUO-KIF Formula without existentially quantified
     * variables.
     */
    private Formula existentialsOut(Map<String,String> evSubs, TreeSet<String> iUQVs, TreeSet<String> scopedUQVs) {
        
        if (thisFormula.listP()) {
            if (thisFormula.empty()) { return thisFormula; }
            var arg0 = thisFormula.car();
            if (arg0.equals(Formula.UQUANT)) {


                var newScopedUQVs = new TreeSet<String>(scopedUQVs);
                var varList = thisFormula.cadr();
                var varListF = new Formula();
                varListF.read(varList);
                while (!(varListF.empty())) {
                    var var = varListF.car();
                    newScopedUQVs.add(var);
                    varListF.read(varListF.cdr());
                }
                var arg2 = thisFormula.caddr();
                var arg2F = new Formula();
                arg2F.read(arg2);
                var theNewFormula = ("(forall " + varList + ' '
                                        + existentialsOut(arg2F, evSubs, iUQVs, 
                                                          newScopedUQVs).theFormula + ')');
                thisFormula.read(theNewFormula);
                return thisFormula;
            }
            if (arg0.equals(Formula.EQUANT)) {

                var uQVs = new TreeSet<String>(iUQVs);
                uQVs.addAll(scopedUQVs);

                var varList = thisFormula.cadr();
                var varListF = new Formula();
                varListF.read(varList);
                var eQVs = new ArrayList<String>();
                while (!(varListF.empty())) {
                    var var = varListF.car();
                    eQVs.add(var);
                    varListF.read(varListF.cdr());
                }


                for (var var : eQVs) {
                    var skTerm = newSkolemTerm(uQVs);
                    evSubs.put(var, skTerm);
                }
                var arg2 = thisFormula.caddr();
                var arg2F = new Formula();
                arg2F.read(arg2);
                return existentialsOut(arg2F,evSubs, iUQVs, scopedUQVs);
            }
            var arg0F = new Formula();
            arg0F.read(arg0);
            var newArg0 = existentialsOut(arg0F,evSubs,iUQVs,scopedUQVs).theFormula;
            return existentialsOut(thisFormula.cdrAsFormula(),evSubs, iUQVs, scopedUQVs).cons(newArg0);
        }
        if (Formula.isVariable(thisFormula.theFormula)) {
            var newTerm = evSubs.get(thisFormula.theFormula);
            if (!StringUtil.emptyString(newTerm)) 
                thisFormula.read(newTerm);                
            return thisFormula;
        }
        return thisFormula;
    }

    /** ***************************************************************
     *  convenience method
     */
    private static Formula existentialsOut(Formula f, Map evSubs, TreeSet iUQVs, TreeSet scopedUQVs) {

        var temp = new Clausifier(f.theFormula);
        return temp.existentialsOut(evSubs, iUQVs, scopedUQVs);
    }

    /** ***************************************************************
     * This method collects all variables in Formula that appear to be
     * only implicitly universally quantified and adds them to the
     * TreeSet iuqvs.  Note the iuqvs must be passed in.
     *
     * @param iuqvs A TreeSet for accumulating variables that appear
     * to be implicitly universally quantified.
     *
     * @param scopedVars A TreeSet containing explicitly quantified
     * variables.
     *
     * @return void
     */
    private void collectIUQVars(TreeSet iuqvs, TreeSet scopedVars) {

        if (thisFormula.listP() && !thisFormula.empty()) {
            var arg0 = thisFormula.car();
            if (Formula.isQuantifier(arg0)) {

                var newScopedVars = new TreeSet(scopedVars);

                var varList = thisFormula.cadr();
                var varListF = new Formula();
                varListF.read(varList);
                while (!(varListF.empty())) {
                    var var = varListF.car();
                    newScopedVars.add(var);
                    varListF = varListF.cdrAsFormula();
                }
                var arg2 = thisFormula.caddr();
                var arg2F = new Formula();
                arg2F.read(arg2);
                collectIUQVars(arg2F,iuqvs,newScopedVars);
            }
            else {
                var arg0F = new Formula();
                arg0F.read(arg0);
                collectIUQVars(arg0F,iuqvs,scopedVars);
                collectIUQVars(thisFormula.cdrAsFormula(),iuqvs,scopedVars);
            }
        }
        else if (Formula.isVariable(thisFormula.theFormula) 
                 && !(scopedVars.contains(thisFormula.theFormula))) {
            iuqvs.add(thisFormula.theFormula);
        }
    }

    /** ***************************************************************
     *  convenience method
     */
    private static void collectIUQVars(Formula f, TreeSet iuqvs, TreeSet scopedVars) {

        var temp = new Clausifier(f.theFormula);
        temp.collectIUQVars(iuqvs,scopedVars);
    }

    /** ***************************************************************
     * This method returns a new Formula in which explicit univeral
     * quantifiers have been removed.
     *
     * @see clausify()
     *
     * @return A new SUO-KIF Formula without explicit universal
     * quantifiers.
     */
    private Formula universalsOut() {

        if (thisFormula.listP()) {
            if (thisFormula.empty()) { return thisFormula; }
            var arg0 = thisFormula.car();
            if (arg0.equals(Formula.UQUANT)) {
                var arg2 = thisFormula.caddr();
                thisFormula.read(arg2);
                return universalsOut(thisFormula);
            }
            var arg0F = new Formula();
            arg0F.read(arg0);
            var newArg0 = universalsOut(arg0F).theFormula;
            return universalsOut(thisFormula.cdrAsFormula()).cons(newArg0);
        }
        return thisFormula;
    }

    /** ***************************************************************
     *  convenience method
     */
    public static Formula universalsOut(Formula f) {

        var temp = new Clausifier(f.theFormula);
        return temp.universalsOut();
    }

    /** ***************************************************************
     * This method returns a new Formula in which nested 'and', 'or',
     * and 'not' operators have been unnested:
     *
     * (not (not <literal> ...)) -> <literal>
     *
     * (and (and <literal-sequence> ...)) -> (and <literal-sequence> ...)
     *
     * (or (or <literal-sequence> ...)) -> (or <literal-sequence> ...)
     *
     * @see clausify()
     * @see nestedOperatorsOut_1()
     *
     * @return A new SUO-KIF Formula in which nested commutative
     * operators and 'not' have been unnested.
     */
    private Formula nestedOperatorsOut() {

        var f = thisFormula;
        var ans = nestedOperatorsOut_1();
        
        
        while (!f.theFormula.equals(ans.theFormula)) {     
            
            
            
            f = ans;
            ans = nestedOperatorsOut_1(f);
        }
        return ans;
    }

    /** ***************************************************************
     *  convenience method
     */
    private static Formula nestedOperatorsOut(Formula f) {

        var temp = new Clausifier(f.theFormula);
        return temp.nestedOperatorsOut();
    }

    /** ***************************************************************
     * @see clausify()
     * @see nestedOperatorsOut_1()
     *
     * @return A new SUO-KIF Formula in which nested commutative
     * operators and 'not' have been unnested.
     */
    private Formula nestedOperatorsOut_1() {

        if (thisFormula.listP()) {
            if (thisFormula.empty()) { return thisFormula; }
            var arg0 = thisFormula.car();
            if (Formula.isCommutative(arg0) || arg0.equals(Formula.NOT)) {
                var literals = new ArrayList();
                var restF = thisFormula.cdrAsFormula();
                while (!(restF.empty())) {
                    var lit = restF.car();
                    var litF = new Formula();
                    litF.read (lit);
                    if (litF.listP()) {
                        var litFarg0 = litF.car();
                        if (litFarg0.equals(arg0)) {
                            if (arg0.equals(Formula.NOT)) {
                                var theNewFormula = litF.cadr();
                                var newF = new Formula();
                                newF.read(theNewFormula);
                                return nestedOperatorsOut_1(newF);
                            }
                            var rest2F = litF.cdrAsFormula();
                            while (!(rest2F.empty())) {
                                var rest2arg0 = rest2F.car();
                                var rest2arg0F = new Formula();
                                rest2arg0F.read(rest2arg0);
                                literals.add(nestedOperatorsOut_1(rest2arg0F).theFormula);
                                rest2F = rest2F.cdrAsFormula();
                            }
                        }
                        else 
                            literals.add(nestedOperatorsOut_1(litF).theFormula);                            
                    }
                    else 
                        literals.add(lit);                        
                    restF = restF.cdrAsFormula();
                }
                var theNewFormula = (Formula.LP + arg0);
                for (var literal : literals) theNewFormula += (Formula.SPACE + literal);
                theNewFormula += Formula.RP;
                var newF = new Formula();
                newF.read(theNewFormula);
                return newF;
            }
            var arg0F = new Formula();
            arg0F.read(arg0);
            var newArg0 = nestedOperatorsOut_1(arg0F).theFormula;
            return nestedOperatorsOut_1(thisFormula.cdrAsFormula()).cons(newArg0);
        }
        return thisFormula;
    }

    /** ***************************************************************
     *  convenience method
     */
    private static Formula nestedOperatorsOut_1(Formula f) {

        var temp = new Clausifier(f.theFormula);
        return temp.nestedOperatorsOut_1();
    }

    /** ***************************************************************
     * This method returns a new Formula in which all occurrences of
     * 'or' have been accorded the least possible scope.
     *
     * (or P (and Q R)) -> (and (or P Q) (or P R))
     *
     * @see clausify()
     * @see disjunctionsIn_1()
     *
     * @return A new SUO-KIF Formula in which occurrences of 'or' have
     * been 'moved in' as far as possible.
     */
    private Formula disjunctionsIn() {

        var f = thisFormula;
        var ans = disjunctionsIn_1(nestedOperatorsOut());
        
        while (! f.theFormula.equals(ans.theFormula)) {     
            f = ans;
            ans = disjunctionsIn_1(nestedOperatorsOut(f));
        }
        return ans;
    }
 
    /** ***************************************************************
     * @see clausify()
     * @see disjunctionsIn()
     *
     * @return A new SUO-KIF Formula in which occurrences of 'or' have
     * been 'moved in' as far as possible.
     */
    private Formula disjunctionsIn_1() {

        if (thisFormula.listP()) {
            if (thisFormula.empty()) { return thisFormula; }
            var arg0 = thisFormula.car();
            if (arg0.equals(Formula.OR)) {
                List<String> disjuncts = new ArrayList<>();
                List<String> conjuncts = new ArrayList<>();
                var restF = thisFormula.cdrAsFormula();
                while (!(restF.empty())) {
                    var disjunct = restF.car();
                    var disjunctF = new Formula();
                    disjunctF.read(disjunct);
                    if (disjunctF.listP() 
                        && disjunctF.car().equals(Formula.AND) 
                        && conjuncts.isEmpty()) {
                        var rest2F = disjunctionsIn_1(disjunctF.cdrAsFormula());
                        while (!(rest2F.empty())) {
                            conjuncts.add(rest2F.car());
                            rest2F = rest2F.cdrAsFormula();
                        }
                    }
                    else 
                        disjuncts.add(disjunct);                        
                    restF = restF.cdrAsFormula();
                }

                if (conjuncts.isEmpty()) { return thisFormula; }

                var resultF = new Formula();
                resultF.read("()");
                var disjunctsString = "";
                for (var disjunct : disjuncts) disjunctsString += (Formula.SPACE + disjunct);
                disjunctsString = (Formula.LP + disjunctsString.trim() + Formula.RP);
                var disjunctsF = new Formula();
                disjunctsF.read(disjunctsString);
                for (var conjunct : conjuncts) {
                    var newDisjuncts =
                        disjunctionsIn_1(disjunctsF.cons(conjunct).cons(Formula.OR)).theFormula;
                    resultF = resultF.cons(newDisjuncts);
                }
                resultF = resultF.cons(Formula.AND);
                return resultF;
            }
            var arg0F = new Formula();
            arg0F.read(arg0);
            var newArg0 = disjunctionsIn_1(arg0F).theFormula;
            return disjunctionsIn_1(thisFormula.cdrAsFormula()).cons(newArg0);
        }
        return thisFormula;
    }

    /** ***************************************************************
     *  convenience method
     */
    private static Formula disjunctionsIn_1(Formula f) {

        var temp = new Clausifier(f.theFormula);
        return temp.disjunctionsIn_1();
    }

    /** ***************************************************************
     * This method returns an ArrayList of clauses.  Each clause is a
     * LISP list (really, a Formula) containing one or more Formulas.
     * The LISP list is assumed to be a disjunction, but there is no
     * 'or' at the head.
     *
     * @see clausify()
     *
     * @return An ArrayList of LISP lists, each of which contains one
     * or more Formulas.
     */
    private ArrayList<Formula> operatorsOut() {

        var result = new ArrayList<Formula>();
        if (!StringUtil.emptyString(thisFormula.theFormula)) {
            var clauses = new ArrayList<Formula>();
            if (thisFormula.listP()) {
                var arg0 = thisFormula.car();
                if (arg0.equals(Formula.AND)) {
                    var restF = thisFormula.cdrAsFormula();
                    while (!(restF.empty())) {
                        var fStr = restF.car();
                        var newF = new Formula();
                        newF.read(fStr);
                        clauses.add(newF);
                        restF = restF.cdrAsFormula();
                    }
                }
            }
            if (clauses.isEmpty()) 
                clauses.add(thisFormula);
            for (var clause : clauses) {
                var clauseF = new Formula();
                clauseF.read("()");
                var f = clause;
                if (f.listP()) {
                    if (f.car().equals(Formula.OR)) {
                        f = f.cdrAsFormula();
                        while (!(f.empty())) {
                            var lit = f.car();
                            clauseF = clauseF.cons(lit);
                            f = f.cdrAsFormula();
                        }
                    }
                }
                if (clauseF.empty())
                    clauseF = clauseF.cons(f.theFormula);
                result.add(clauseF);
            }
        }
        return result;
    }

    /** ***************************************************************
     * This method returns a Formula in which variables for separate
     * clauses have been 'standardized apart'.
     *
     * @see clausify()
     * @see standardizeApart(Map renameMap)
     * @see standardizeApart_1(Map renames, Map reverseRenames)
     *
     * @return A Formula.
     */
    private Formula standardizeApart() {
        Map<String, String> reverseRenames = new HashMap<>();
        return standardizeApart(thisFormula,reverseRenames);
    }

    /** ***************************************************************
     *  convenience method
     */
    private static Formula standardizeApart(Formula f, Map<String, String> renameMap) {

        var temp = new Clausifier(f.theFormula);
        return temp.standardizeApart(renameMap);
    }

    /** ***************************************************************
     * This method returns a Formula in which variables for separate
     * clauses have been 'standardized apart'.
     *
     * @see clausify()
     * @see standardizeApart()
     * @see standardizeApart_1(Map renames, Map reverseRenames)
     *
     * @param renameMap A Map for capturing one-to-one variable rename
     * correspondences.  Keys are new variables.  Values are old
     * variables.
     *
     * @return A Formula.
     */
    private Formula standardizeApart(Map<String,String> renameMap) {

        var result = thisFormula;
        Map<String,String> reverseRenames = null;
        if (renameMap instanceof Map) 
            reverseRenames = renameMap;            
        else 
            reverseRenames = new HashMap();

        if (!StringUtil.emptyString(thisFormula.theFormula)) {
            var clauses = new ArrayList<Formula>();
            if (thisFormula.listP()) {
                var arg0 = thisFormula.car();
                if (arg0.equals(Formula.AND)) {
                    var restF = thisFormula.cdrAsFormula();
                    while (!(restF.empty())) {
                        var fStr = restF.car();
                        var newF = new Formula();
                        newF.read(fStr);
                        clauses.add(newF);
                        restF = restF.cdrAsFormula();
                    }
                }
            }
            if (clauses.isEmpty()) 
                clauses.add(thisFormula);

            var n = clauses.size();
            for (var i = 0; i < n ; i++) {
                Map<String, String> renames = new HashMap<>();
                var oldClause = clauses.remove(0);
                clauses.add(standardizeApart_1(oldClause,renames,reverseRenames));
            }

            
            if (n > 1) {
                var theNewFormula = "(and";
                for (var f : clauses) {
                    theNewFormula += (Formula.SPACE + f.theFormula);
                }
                theNewFormula += Formula.RP;
                var newF = new Formula();
                newF.read(theNewFormula);
                result = newF;
            }
            else 
                result = clauses.get(0);                
        }
        return result;
    }

    /** ***************************************************************
     * This is a helper method for standardizeApart(renameMap).  It
     * assumes that the Formula will be a single clause.
     *
     * @see clausify()
     * @see standardizeApart()
     * @see standardizeApart(Map renameMap)
     *
     * @param renames A Map of correspondences between old variables
     * and new variables.
     *
     * @param reverseRenames A Map of correspondences between new
     * variables and old variables.
     *
     * @return A Formula
     */
    private Formula standardizeApart_1(Map<String, String> renames, Map<String, String> reverseRenames) {

        var ans = thisFormula;
        if (thisFormula.listP() && !(thisFormula.empty())) {
            var arg0 = thisFormula.car();
            var arg0F = new Formula();
            arg0F.read(arg0);
            arg0F = standardizeApart_1(arg0F,renames,reverseRenames);
            ans = standardizeApart_1(thisFormula.cdrAsFormula(),renames,reverseRenames).cons(arg0F.theFormula);
        }
        else if (Formula.isVariable(thisFormula.theFormula)) {
            var rnv = renames.get(thisFormula.theFormula);
            if (StringUtil.emptyString(rnv)) {
                rnv = newVar();
                renames.put(thisFormula.theFormula, rnv);
                reverseRenames.put(rnv, thisFormula.theFormula);
            }
            var rnvF = new Formula();
            rnvF.read(rnv);
            ans = rnvF;
        }
        return ans;
    }

    /** ***************************************************************
     *  convenience method
     */
    private static Formula standardizeApart_1(Formula f, Map<String, String> renames, Map<String, String> reverseRenames) {

        var temp = new Clausifier(f.theFormula);
        return temp.standardizeApart_1(renames, reverseRenames);
    }

    /** ***************************************************************
     * This method finds the original variable that corresponds to a new
     * variable.  Note that the clausification algorithm has two variable
     * renaming steps, and that after variables are standardized apart an
     * original variable might correspond to multiple clause variables.
     *
     * @param var A SUO-KIF variable (String)
     *
     * @param varMap A Map (graph) of successive new to old variable
     * correspondences.
     * 
     * @return The original SUO-KIF variable corresponding to the input.
     *
     **/
    public static String getOriginalVar(String var, Map<String, String> varMap) {

        String ans = null;
        if (!StringUtil.emptyString(var) && (varMap instanceof Map)) {
            ans = var;
            var next = varMap.get(ans);
            while (! ((next == null) || next.equals(ans))) {
                ans = next;
                next = varMap.get(ans);        
            }
            if (ans == null) 
                ans = var;                
        }
        return ans;
    }

    /** ***************************************************************
     * This method converts the SUO-KIF Formula to a version of
     * clausal (resolution, conjunctive normal) form with Skolem
     * functions, following the procedure described in Logical
     * Foundations of Artificial Intelligence, by Michael Genesereth
     * and Nils Nilsson, 1987, pp. 63-66.
     *
     * <p>A literal is an atomic formula.  (Note, however, that
     * because SUO-KIF allows higher-order formulae, not all SUO-KIF
     * literals are really atomic.)  A clause is a disjunction of
     * literals that share no variable names with literals in any
     * other clause in the KB.  Note that even a relatively simple
     * SUO-KIF formula might generate multiple clauses.  In such
     * cases, the Formula returned will be a conjunction of clauses.
     * (A KB is understood to be a conjunction of clauses.)  In all
     * cases, the Formula returned by this method should be a
     * well-formed SUO-KIF Formula if the input (original formula) is
     * a well-formed SUO-KIF Formula.  Rendering the output in true
     * (LISPy) clausal form would require an additional step, the
     * removal of all commutative logical operators, and the result
     * would not be well-formed SUO-KIF.</p>
     *
     * @see clausifyWithRenameInfo()
     * @see toNegAndPosLitsWithRenameInfo()
     * @see toCanonicalClausalForm();
     *
     * @return A SUO-KIF Formula in clausal form, or null if a clausal
     * form cannot be generated.
     */
    public Formula clausify() {

        thisFormula = equivalencesOut();
        thisFormula = implicationsOut();
        thisFormula = negationsIn();
        thisFormula = renameVariables();
        thisFormula = existentialsOut();
        thisFormula = universalsOut();
        thisFormula = disjunctionsIn();
        thisFormula = standardizeApart();
        return thisFormula;
    }
    
    /** ***************************************************************
     * A test method.
     */
    public static void test1() {

        var f = new Formula();
        f.read("(forall (?ROW1 ?ITEM) (equal (ListOrderFn (ListFn ?ROW1 ?ITEM) (ListLengthFn (ListFn ?ROW1 ?ITEM))) ?ITEM))");
        System.out.println("INFO in Clausifier.test1(): input formula: " + f);
        var clausalForm = Clausifier.clausify(f);
        System.out.println("INFO in Clausifier.test1(): output formula: " + clausalForm);
    }



































































    
    /** ***************************************************************
     */
    private static void testRemoveImpEq() {
    
        System.out.println();
        System.out.println("================== testRemoveImpEq ======================");
        var f = new Clausifier("(=> a b)");
        System.out.println("input: " + f);
        var form = f.equivalencesOut();
        System.out.println(form);
        f = new Clausifier(form.theFormula);
        form = f.implicationsOut();
        System.out.println(form);
        System.out.println();

        System.out.println("--------------------------------------------------");
        f = new Clausifier("(<=> a b)");
        System.out.println("input: " + f);
        form = f.equivalencesOut();
        System.out.println(form);
        f = new Clausifier(form.theFormula);
        form = f.implicationsOut();
        System.out.println(form);
        System.out.println();
        
        System.out.println("--------------------------------------------------");
        f = new Clausifier("(<=> (or (or (forall (?X) (a ?X)) (b ?X)) (exists (?X) (exists (?Y) (p ?X (f ?Y)))))  (q (g a) ?X))");
        System.out.println("input: " + f);
        form = f.equivalencesOut();
        System.out.println(form);
        f = new Clausifier(form.theFormula);
        form = f.implicationsOut();
        System.out.println(form);
        System.out.println();
    }

    /** ***************************************************************
     */
    private static void testMoveQuantifiersLeft() {
        
        System.out.println();
        System.out.println("================== testMoveQuantifiersLeft ======================");
        var f = new Clausifier("(or p (forall (?X) (q ?X)))");
        System.out.println("input: " + f);
        var form = f.existentialsOut();
        System.out.println("result: " + form);
        f = new Clausifier(form.theFormula);
        form = f.universalsOut();        
        System.out.println("result: " + form);
        System.out.println();
        
        /*
        System.out.println("input: " + form);
        form = moveQuantifiersLeft(form);
        System.out.println("result should be ![X]:p | q(X): " + form);
        System.out.println();
        
        form = BareFormula.string2form("~((![X]:a(X)) | b(X))");
        System.out.println("input: " + form);
        form = moveQuantifiersLeft(form);
        System.out.println("result: " + form);
        System.out.println(); 
        
        form = BareFormula.string2form("~(((![X]:a(X)) | b(X)) | (?[X]:(?[Y]:p(X, f(Y)))))");
        System.out.println("input: " + form);
        form = moveQuantifiersLeft(form);
        System.out.println("result: " + form);
        System.out.println();
        
        form = BareFormula.string2form("( (~(((![X]:a(X)) | b(X)) | (?[X]:(?[Y]:p(X, f(Y)))))) | q(g(a), X))");
        System.out.println("input: " + form);
        form = moveQuantifiersLeft(form);
        System.out.println("result: " + form);
        System.out.println();
*/
/*        System.out.println("--------------------------------------------------");
        f = new Clausifier("( ( (~(((![X]:a(X)) | b(X)) | (?[X]:(?[Y]:p(X, f(Y)))))) | q(g(a), X)) & " +
                                         "((~q(g(a), X)) | (((![X]:a(X)) | b(X)) | (?[X]:(?[Y]:p(X, f(Y)))))))");
        System.out.println("input: " + f);
        form = f.existentialsOut();        
        System.out.println("result: " + form);
        f = new Clausifier(form.theFormula);
        form = f.universalsOut();        
        System.out.println("result: " + form);
        System.out.println();
    */    
    } 

    /** ***************************************************************
     */
    private static void testMoveNegationIn() {
        
        System.out.println();
        System.out.println("================== testMoveNegationIn ======================");
        var f = new Clausifier("(not (or p q))");
        
        System.out.println("input: " + f);
        var form = f.negationsIn();
        System.out.println("result should be (and (not p) (not q)): " + form);
        System.out.println();
        
        f = new Clausifier("(not (and p q))");
        System.out.println("input: " + f);
        form = f.negationsIn();
        System.out.println("result should be (or (not p) (not q)): " + form);
        System.out.println();
        
        f = new Clausifier("(not (forall (?X) p))");
        System.out.println("input: " + f);
        form = f.negationsIn();
        System.out.println("result should be (exists (?X) (not p)): " + form);
        System.out.println();
        
        f = new Clausifier("(not (exists (?X) p))");
        System.out.println("input: " + f);
        form = f.negationsIn();
        System.out.println("result should be (forall (?X) (not p)): " + form);
        System.out.println();
        
        f = new Clausifier("(not (not p))");
        System.out.println("input: " + f);
        form = f.negationsIn();
        System.out.println("result should be (p): " + form);
        System.out.println(); 
       
        f = new Clausifier("(not (exists (?Y) (p ?X (f ?Y))))");
        System.out.println("input: " + f);
        form = f.negationsIn();
        System.out.println("result: " + form);
        System.out.println();
        
        f = new Clausifier("(not (exists (?X) (exists (?Y) (p ?X (f ?Y)))))");
        System.out.println("input: " + f);
        form = f.negationsIn();     
        System.out.println("expected result: (forall (?X) (forall (?Y) (not (p ?X (f ?Y)))))");
        System.out.println("result: " + form);
        System.out.println();
        
        f = new Clausifier("(not (or (or (forall (?X) (a ?X)) (b ?X)) (exists (?X) (exists (?Y) (p ?X (f ?Y))))))");
        System.out.println("input: " + f);
        form = f.negationsIn();
        System.out.println("result should be (and (and (exists (?X) (not (a ?X))) (not (b ?X))) (forall (?X) (forall (?Y) (not (p ?X (f ?Y))))))");
        System.out.println("actual: "+ form);
        System.out.println();
        
        f = new Clausifier("(and (or (not (or (or (forall (?X) (a ?X)) (b ?X)) (exists (?X) (exists (?Y) (p ?X (f ?Y)))))) (q (g a) ?X)) " +
        				         "(or (not (q (g a) ?X))  (or (or (forall (?X) (a ?X)) (b ?X)) (exists (?X) (exists (?Y) (p ?X (f ?Y))))))   )");
        System.out.println("input: " + f);
        form = f.negationsIn();
        System.out.println("expected: ( ( ((( (?[X]:~a(X)) & ~b(X)) & (![X]:(![Y]:~p(X, f(Y)))) )) | q(g(a), X)) & " +
                                       "((~q(g(a), X)) | (((![X]:a(X))|b(X)) | (?[X]:(?[Y]:p(X, f(Y)))) )))");
        System.out.println("result: " + form);
        System.out.println();   
          
    } 

    /** ***************************************************************
     */
    private static void testStandardizeVariables() {
        
        System.out.println();
        System.out.println("================== testStandardizeVariables ======================");
        var f = new Clausifier("(not (or (forall (?X) (a ?X)) (b ?X)))");
        System.out.println("input: " + f);
        var form = f.standardizeApart();
        System.out.println("result should be : (not (or (forall (?VAR2) (a ?VAR2)) (b ?VAR1)))");
        System.out.println("actual: "+ form);
        System.out.println();
        
        /*
        f = new Clausifier("(((~(((![X]:a(X))|b(X))|(?[X]:(?[Y]:p(X, f(Y))))))|q(g(a), X))&((~q(g(a), X))|(((![X]:a(X))|b(X))|(?[X]:(?[Y]:p(X, f(Y)))))))");
        System.out.println("input: " + form);
        form = f.standardizeApart();
        System.out.println("actual: "+ form);
        System.out.println();   
        */     
    }
    
    /** ***************************************************************
     */
    private static void testSkolemization() {
        
        System.out.println();
        System.out.println("================== testSkolemization ======================");
        var f = new Clausifier("(?[VAR0]:(![VAR3]:(![VAR2]:(?[VAR5]:(![VAR1]:(?[VAR4]:((((~a(VAR0)&~b(X))&~p(VAR2, f(VAR1)))|q(g(a), X))&(~q(g(a), X)|((a(VAR3)|b(X))|p(VAR5, f(VAR4)))))))))))");
        System.out.println("input: " + f);
        var form = f.existentialsOut();
        System.out.println("actual: "+ form);
        System.out.println();
    }
    
    /** ***************************************************************
     */
    private static void testDistribute() {
        
        System.out.println();
        System.out.println("================== testDistribute ======================");
        var f = new Clausifier("(or (and a b) c)");
        System.out.println("input: " + f);
        var form = f.disjunctionsIn();
        System.out.println("actual: " + form);
        /*
        System.out.println("input: " + form);
        form = distributeAndOverOr(form);
        System.out.println("result should be : (a | c) & (b | c)");
        System.out.println("actual: " + form);
        System.out.println();
        
        form = BareFormula.string2form("(a & b) | (c & d)");
        System.out.println("input: " + form);
        form = distributeAndOverOr(form);
        System.out.println("result should be : (a | c) & (a | d) & (b | c) & (d | b)");
        System.out.println("actual: " + form);
        System.out.println();
        */        
        
        f = new Clausifier("(((~holdsAt(VAR2, VAR1)|releasedAt(VAR2, plus(VAR1, n1)))|(happens(skf3, VAR1)&terminates(skf3, VAR2, VAR1)))|holdsAt(VAR2, plus(VAR1, n1)))");
        System.out.println("input: " + f);
        form = f.disjunctionsIn();
        System.out.println("actual: " + form);
        
        System.out.println();
    }
    
    /** ***************************************************************
     */
    private static void testClausificationSteps(String s) {

        System.out.println();
        System.out.println("================== testClausification ======================");
        var f = new Clausifier(s);
        System.out.println("input: " + f);
        System.out.println();
        var form = f.equivalencesOut();
        System.out.println("after Remove Equivalence: " + form);

        System.out.println();
        f = new Clausifier(form.theFormula);
        form = f.implicationsOut();
        System.out.println("after Implications out: " + form);
        
        System.out.println();
        f = new Clausifier(form.theFormula);
        form = f.negationsIn();
        System.out.println("after Move Negation In: " + form);

        System.out.println();
        f = new Clausifier(form.theFormula);
        form = f.renameVariables();
        System.out.println("after rename Variables: " + form);

        System.out.println();
        f = new Clausifier(form.theFormula);
        form = f.existentialsOut();
        System.out.println("after Move Quantifiers: " + form);

        System.out.println();
        f = new Clausifier(form.theFormula);
        form = f.universalsOut();
        System.out.println("after remove universal quantifiers: " + form);

        System.out.println();
        f = new Clausifier(form.theFormula);
        form = f.disjunctionsIn();
        System.out.println("after Distribution: " + form);

        System.out.println();
        f = new Clausifier(form.theFormula);
        form = f.standardizeApart();
        System.out.println("after Standardize apart: " + form);
        
        System.out.println();
        f = new Clausifier(form.theFormula);
        var forms = f.separateConjunctions();
        System.out.println("after separation: " + forms);
    }
    
    /** ***************************************************************
     */
    private static void testClausification() {
                
        
        testClausificationSteps("(![Fluent]:(![Time]:(((holdsAt(Fluent, Time)&(~releasedAt(Fluent, plus(Time, n1))))&(~(?[Event]:(happens(Event, Time)&terminates(Event, Fluent, Time)))))=>holdsAt(Fluent, plus(Time, n1)))))).");
    }
    
    /** ***************************************************************
     */
    private static void testClausificationSimple() {
        
        System.out.println();
        System.out.println("================== testClausificationSimple ======================");
        var form = new Formula();
        var c = new Clausifier("(<=> (or (or (forall (?X) (a ?X))  (b ?X)) (exists (?X) (exists (?Y) (p ?X (f ?Y))))) (q (g a) ?X))");

        System.out.println("input: " + c.thisFormula);
        System.out.println();
        var result = c.clausify();
        System.out.println(result);   
        
        c = new Clausifier("(<=> (and (equal (AbsoluteValueFn ?NUMBER1) ?NUMBER2) (instance ?NUMBER1 RealNumber) " +
                "(instance ?NUMBER2 RealNumber)) (or (and (instance ?NUMBER1 NonnegativeRealNumber) " +
                "(equal ?NUMBER1 ?NUMBER2)) (and (instance ?NUMBER1 NegativeRealNumber) (equal ?NUMBER2 " +
                "(SubtractionFn 0 ?NUMBER1)))))");
        System.out.println("input: " + c.thisFormula);
        System.out.println();
        result = c.clausify();        
        System.out.println(result);  
    }
    
    /** ***************************************************************
     */
    public static void main(String[] args) {
        
        
        
        
        
        
        
        
        testClausificationSimple();
    	
    }
}


