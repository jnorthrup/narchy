/**
 * This code is copyright Articulate Software (c) 2003.  Some portions
 * copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
 * This software is released under the GNU Public License <http:
 * Users of this code also consent, by use of this code, to credit Articulate Software
 * and Teknowledge in any writings, briefings, publications, presentations, or
 * other representations of any software which incorporates, builds on, or uses this
 * code.  Please cite the following article in any publication with references:
 * <p>
 * Pease, A., (2003). The Sigma Ontology Development Environment,
 * in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 * August 9, Acapulco, Mexico.  See also http:
 *
 * Authors:
 * Adam Pease
 * Infosys LTD.
 */
package nars.op.kif;


import java.io.*;
import java.text.ParseException;
import java.util.*;

/******************************************************************
 * A class designed to read a file in SUO-KIF format into memory. See
 * <http:
 * language specification. readFile() and writeFile() are the primary entry
 * points and parse() does all the real work.
 *
 * @author Adam Pease
 */
public class KIFParser {

    /*****************************************************************
     * A numeric constant denoting normal parse mode, in which syntax constraints are
     * enforced.
     */
    public static final int NORMAL_PARSE_MODE = 1;
    public static int count = 0;

    /****************************************************************
     * A numeric constant denoting relaxed parse mode, in which fewer syntax constraints
     * are enforced than in NORMAL_PARSE_MODE.
     */
    public static final int RELAXED_PARSE_MODE = 2;


    private int parseMode = NORMAL_PARSE_MODE;

    /** The set of all terms in the knowledge base. This is a set of Strings. */
    public TreeSet<String> terms = new TreeSet<String>();

    /** A hashMap to store target frequencies for each target in knowledge base */
    public Map<String, Integer> termFrequency = new HashMap<String, Integer>();

    /**
     * A HashMap of ArrayLists of Formulas. Each String key points to a list of
     * String formulas that correspond to that key. For example, "arg-1-Foo"
     * would be one of several keys for "(instance Foo Bar)".
     *
     * @see #createKey(String, boolean, boolean, int, int) for key format.
     */
    public HashMap<String, ArrayList<String>> formulas = new HashMap<String, ArrayList<String>>();

    /**
     * A HashMap of String keys representing the formula, and Formula values.
     * For example, "(instance Foo Bar)" is a String key that might point to a
     * Formula that is that string, along with information about at what line
     * number and in what file it appears.
     */
    public HashMap<String, Formula> formulaMap = new HashMap<String, Formula>();

    private final String filename = "";

    private int totalLinesForComments = 0;

    /** warnings generated during parsing */
    public TreeSet<String> warningSet = new TreeSet<String>();
    /** errors generated during parsing */
    public Set<String> errorSet = new TreeSet<String>();

    /*****************************************************************
     * @return int Returns an integer value denoting the current parse mode.
     */
    public int getParseMode() {

        return this.parseMode;
    }

    /*****************************************************************
     * Sets the current parse mode to the input value mode.
     *
     * @param mode
     *            An integer value denoting a parsing mode.
     * @return void
     */
    public void setParseMode(int mode) {

        this.parseMode = mode;
    }

    /****************************************************************
     * This routine sets up the StreamTokenizer_s so that it parses SUO-KIF. = < >
     * are treated as word characters, as are normal alphanumerics. ; is the
     * line comment character and " is the quote character.
     */
    public static void setupStreamTokenizer(StreamTokenizer_s st) {

        st.whitespaceChars(0, 32);
        st.ordinaryChars(33, 44); 
        st.wordChars(45, 46); 
        st.ordinaryChar(47); 
        st.wordChars(48, 58); 
        st.ordinaryChar(59); 
        st.wordChars(60, 64); 
        st.wordChars(65, 90); 
        st.ordinaryChars(91, 94); 
        st.wordChars(95, 95); 
        st.ordinaryChar(96); 
        st.wordChars(97, 122); 
        st.ordinaryChars(123, 255); 
        
        st.quoteChar('"');
        st.commentChar(';');
        st.eolIsSignificant(true);
    }

    /*****************************************************************
     */
    private static void display(StreamTokenizer_s st, boolean inRule, boolean inAntecedent, boolean inConsequent,
                                int argumentNum, int parenLevel, String key) {

        StringBuilder result = new StringBuilder();
        result.append(inRule);
        result.append('\t');
        result.append(inAntecedent);
        result.append('\t');
        result.append(inConsequent);
        result.append('\t');
        result.append(st.ttype);
        result.append('\t');
        result.append(argumentNum);
        result.append('\t');
        result.append(parenLevel);
        result.append('\t');
        result.append(st.sval);
        result.append('\t');
        result.append(st.nval);
        result.append('\t');
        result.append(st);
        result.append('\t');
        result.append(key);
    }

    /*****************************************************************
     * This method has the side effect of setting the contents of formulaMap and
     * formulas as it parses the file. It throws a ParseException with file line
     * numbers if fatal errors are encountered during parsing. Keys in variable
     * "formulas" include the string representation of the formula.
     *
     * @return a Set of warnings that may indicate syntax errors, but not fatal
     *         parse errors.
     */
    protected TreeSet<String> parse(Reader r) {

        int mode = this.getParseMode();
        StringBuilder expression = new StringBuilder();
        int lastVal;
        Formula f = new Formula();
        String errStart = "Parsing error in " + filename;
        String errStr = null;
        int duplicateCount = 0;

        if (r == null) {
            errStr = "No Input Reader Specified";
            warningSet.add(errStr);
            System.err.println("Error in KIF.parse(): " + errStr);
            return warningSet;
        }
        try {
            count++;
            StreamTokenizer_s st = new StreamTokenizer_s(r);
            KIFParser.setupStreamTokenizer(st);
            int parenLevel = 0;
            boolean inRule = false;
            int argumentNum = -1;
            boolean inAntecedent = false;
            boolean inConsequent = false;
            HashSet<String> keySet = new HashSet<String>();
            
            boolean isEOL = false;
            do {
                lastVal = st.ttype;
                st.nextToken();
                
                
                if (st.ttype == StreamTokenizer.TT_EOL) {
                    if (isEOL) {
                        
                        
                        
                        if (f.startLine != 0 && (!keySet.isEmpty() || (expression.length() > 0))) {
                            errStr = (errStart + " possible missed closing parenthesis near start line " + f.startLine
                                    + " end line " + f.endLine + " for formula " + expression + "\n and key "
                                    + keySet + " keyset size " + keySet.size() + " exp length "
                                    + expression.length() + " comment lines " + totalLinesForComments);
                            errorSet.add(errStr);
                            throw new ParseException(errStr, f.startLine);
                        }
                        continue;
                    }
                    else { 
                        isEOL = true; 
                        continue;
                    }
                }
                else if (isEOL)
                    isEOL = false; 
                if (st.ttype == 40) { 
                    if (parenLevel == 0) {
                        
                        f = new Formula();
                        f.startLine = st.lineno() + totalLinesForComments;
                        f.sourceFile = filename;
                    }
                    parenLevel++;
                    if (inRule && !inAntecedent && !inConsequent)
                        inAntecedent = true;
                    else {
                        if (inRule && inAntecedent && (parenLevel == 2)) {
                            inAntecedent = false;
                            inConsequent = true;
                        }
                    }
                    if ((parenLevel != 0) && (lastVal != 40) && (expression.length() > 0))
                        expression.append(' ');
                    expression.append('(');
                }
                else if (st.ttype == 41) { 
                    parenLevel--;
                    expression.append(')');
                    if (parenLevel == 0) { 
                        String fstr = StringUtil.normalizeSpaceChars(expression.toString());
                        f.theFormula = fstr.intern();
                        if (formulaMap.keySet().contains(f.theFormula)) {
                            String warning = ("Duplicate axiom at line " + f.startLine + " of " + f.sourceFile + ": "
                                    + expression);
                            warningSet.add(warning);
                            System.out.println(warning);
                            duplicateCount++;
                        }
                        if (mode == NORMAL_PARSE_MODE) { 
                            String validArgs = "";


                            if (StringUtil.emptyString(validArgs))
                                validArgs = Formula.badQuantification();
                            if (StringUtil.isNonEmptyString(validArgs)) {
                                errStr = (errStart + ": Invalid number of arguments near line " + f.startLine + " : "
                                        + validArgs);
                                errorSet.add(errStr);
                                throw new ParseException(errStr, f.startLine);
                            }
                        }
                        keySet.add(f.theFormula); 
                        keySet.add(f.createID());
                        f.endLine = st.lineno() + totalLinesForComments;
                        Iterator<String> it = keySet.iterator();
                        while (it.hasNext()) { 
                            String fkey = it.next();
                            if (formulas.containsKey(fkey)) {
                                if (!formulaMap.keySet().contains(f.theFormula)) { 
                                    ArrayList<String> list = formulas.get(fkey);
                                    if (StringUtil.emptyString(f.theFormula)) {
                                        System.out.println("Error in KIF.parse(): Storing empty formula from line: "
                                                + f.startLine);
                                        errorSet.add(errStr);
                                    }
                                    else if (!list.contains(f.theFormula))
                                        list.add(f.theFormula);
                                }
                            }
                            else {
                                ArrayList<String> list = new ArrayList<String>();
                                if (StringUtil.emptyString(f.theFormula)) {
                                    System.out.println(
                                            "Error in KIF.parse(): Storing empty formula from line: " + f.startLine);
                                    errorSet.add(errStr);
                                }
                                else if (!list.contains(f.theFormula))
                                    list.add(f.theFormula);
                                formulas.put(fkey, list);
                            }
                        }
                        formulaMap.put(f.theFormula, f);
                        inConsequent = false;
                        inRule = false;
                        argumentNum = -1;
                        expression = new StringBuilder();
                        keySet.clear();
                    }
                    else if (parenLevel < 0) {
                        errStr = (errStart + ": Extra closing parenthesis found near line " + f.startLine);
                        errorSet.add(errStr);
                        throw new ParseException(errStr, f.startLine);
                    }
                }
                else if (st.ttype == 34) { 
                    st.sval = StringUtil.escapeQuoteChars(st.sval);
                    if (lastVal != 40) 
                        expression.append(' ');
                    expression.append('"');
                    String com = st.sval;
                    totalLinesForComments += countChar(com, (char) 0X0A);
                    expression.append(com);
                    expression.append('"');
                    if (parenLevel < 2) 
                        argumentNum = argumentNum + 1;
                }
                else if ((st.ttype == StreamTokenizer.TT_NUMBER) || 
                        (st.sval != null && (Character.isDigit(st.sval.charAt(0))))) {
                    if (lastVal != 40) 
                        expression.append(' ');
                    if (st.nval == 0)
                        expression.append(st.sval);
                    else
                        expression.append(st.nval);
                    if (parenLevel < 2) 
                        argumentNum = argumentNum + 1;
                }
                else if (st.ttype == StreamTokenizer.TT_WORD) { 
                    if ((st.sval.equals("=>") || st.sval.equals("<=>")) && parenLevel == 1)
                        inRule = true; 
                    if (parenLevel < 2) 
                        argumentNum = argumentNum + 1;
                    if (lastVal != 40) 
                        expression.append(' ');
                    expression.append(st.sval);
                    if (expression.length() > 64000) {
                        errStr = (errStart + ": Sentence over 64000 characters new line " + f.startLine);
                        errorSet.add(errStr);
                        throw new ParseException(errStr, f.startLine);
                    }
                    
                    if ((mode == NORMAL_PARSE_MODE) && (st.sval.charAt(0) != '?') && (st.sval.charAt(0) != '@')) { 
                        terms.add(st.sval); 

                        if (!termFrequency.containsKey(st.sval)) {
                            termFrequency.put(st.sval, 0);
                        }
                        termFrequency.put(st.sval, termFrequency.get(st.sval) + 1);

                        String key = createKey(st.sval, inAntecedent, inConsequent, argumentNum, parenLevel);
                        keySet.add(key); 
                    }
                }
                else if ((mode == RELAXED_PARSE_MODE) && (st.ttype == 96)) 
                    expression.append(" `");
                else if (st.ttype != StreamTokenizer.TT_EOF) {
                    errStr = (errStart + ": Illegal character near line " + f.startLine);
                    errorSet.add(errStr);
                    throw new ParseException(errStr, f.startLine);
                }
            } while (st.ttype != StreamTokenizer.TT_EOF);

            if (!keySet.isEmpty() || expression.length() > 0) {
                errStr = (errStart + ": Missed closing parenthesis near line " + f.startLine);
                errorSet.add(errStr);
                throw new ParseException(errStr, f.startLine);
            }
        }
        catch (Exception ex) {
            String message = ex.getMessage().replaceAll(":", "&58;"); 
            warningSet.add("Warning in KIF.parse() " + message);
            ex.printStackTrace();
        }
        if (duplicateCount > 0) {
            String warning = "WARNING in KIF.parse(Reader), " + duplicateCount + " duplicate statement"
                    + ((duplicateCount > 1) ? "s " : " ") + "detected in "
                    + (StringUtil.emptyString(filename) ? " the input file" : filename);
            warningSet.add(warning);
        }
        return warningSet;
    }

    /*****************************************************************
     * This routine creates a key that relates a token in a logical statement to the
     * entire statement. It prepends to the token a string indicating its
     * position in the statement. The key is of the form type-[num]-target, where
     * [num] is only present when the type is "arg", meaning a statement in
     * which the target is nested only within one pair of parentheses. The other
     * possible types are "ant" for rule antecedent, "cons" for rule consequent,
     * and "stmt" for cases where the target is nested inside multiple levels of
     * parentheses. An example key would be arg-0-instance for a appearance of
     * the target "instance" in a statement in the predicate position.
     *
     * @param sval            - the token such as "instance", "Human" etc.
     * @param inAntecedent    - whether the target appears in the antecedent of a rule.
     * @param inConsequent    - whether the target appears in the consequent of a rule.
     * @param argumentNum     - the argument position in which the target appears. The
     *            predicate position is argument 0. The first argument is 1 etc.
     * @param parenLevel      - if the paren level is > 1 then the target appears nested in a
     *            statement and the argument number is ignored.
     */
    private static String createKey(String sval, boolean inAntecedent, boolean inConsequent, int argumentNum, int parenLevel) {

        if (sval == null) {
            sval = "null";
        }
        String key = "";
        if (inAntecedent) {
            key = key.concat("ant-");
            key = key.concat(sval);
        }

        if (inConsequent) {
            key = key.concat("cons-");
            key = key.concat(sval);
        }

        if (!inAntecedent && !inConsequent && (parenLevel == 1)) {
            key = key.concat("arg-");
            key = key.concat(String.valueOf(argumentNum));
            key = key.concat("-");
            key = key.concat(sval);
        }
        if (!inAntecedent && !inConsequent && (parenLevel > 1)) {
            key = key.concat("stmt-");
            key = key.concat(sval);
        }
        return (key);
    }

    /*****************************************************************
     * Count the number of appearances of a certain character in a string.
     *
     * @param str - the string to be tested.
     * @param c   - the character to be counted.
     */
    private static int countChar(String str, char c) {

        int len = 0;
        char[] cArray = str.toCharArray();
        for (int i = 0; i < cArray.length; i++) {
            if (cArray[i] == c)
                len++;
        }
        return len;
    }

    /****************************************************************
     * Read a KIF file.
     *
     * @param fname - the full pathname of the file.
     */
    public void read(InputStream stream) throws IOException {

        //Exception exThr = null;
        try(Reader fr = new InputStreamReader(stream)) {
            parse(fr);
        }/* catch (Exception ex) {
            throw new RuntimeException(ex);



        }*/
//        if (exThr != null)
//            throw exThr;
    }

    /****************************************************************
     * Write a KIF file.
     *
     * @param fname - the name of the file to write, including full path.
     */
    public void writeFile(String fname) {

        FileWriter fr = null;
        PrintWriter pr = null;
        try {
            fr = new FileWriter(fname);
            pr = new PrintWriter(fr);
            Iterator<Formula> it = formulaMap.values().iterator();
            while (it.hasNext())
                pr.println(it.next().theFormula);
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                if (pr != null)
                    pr.close();
                if (fr != null)
                    fr.close();
            }
            catch (Exception ex2) {
            }
        }
    }

    /*****************************************************************
     * Parse a single formula.
     */
    public String parseStatement(String formula) {

        StringReader r = new StringReader(formula);
        boolean isError = false;
        try {
            isError = !parse(r).isEmpty();
            if (isError) {
                String msg = "Error parsing " + formula;
                return msg;
            }
        }
        catch (Exception e) {
            System.out.println("Error parsing " + formula);
            e.printStackTrace();
            return e.getMessage();
        }
        return null;
    }




















































    /*****************************************************************
     * Test method for this class.
     */
    public static void main(String[] args) {

        
        String exp = "(documentation foo \"(written by John Smith).\")";
        System.out.println(exp);
        KIFParser kif = new KIFParser();
        Reader r = new StringReader(exp);
        kif.parse(r);
        System.out.println(kif.formulaMap);
        ArrayList<String> al = kif.formulas.get("arg-0-documentation");
        String fstr = al.get(0);
        Formula f = kif.formulaMap.get(fstr);
        System.out.println(f);
        f.read(f.cdr());
        f.read(f.cdr());
        System.out.println(f);
        System.out.println(f.car());
    }


}
