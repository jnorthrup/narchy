/**
 * This code is copyright Articulate Software (c) 2003-2007. Some portions
 * copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
 * This software is released under the GNU Public License
 * <http:
 * use of this code, to credit Articulate Software and Teknowledge in any
 * writings, briefings, publications, presentations, or other representations of
 * any software which incorporates, builds on, or uses this code. Please cite
 * the following article in any publication with references:
 *
 * Pease, A., (2003). The Sigma Ontology Development Environment, in Working
 * Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems, August
 * 9, Acapulco, Mexico. See also http:
 */
package nars.op.kif;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ***************************************************************
 * This program finds and displays SUMO terms that are related in meaning to the
 * English expressions that are entered as input. Note that this program uses
 * four WordNet data files, "NOUN.EXC", "VERB.EXC" etc, as well as four WordNet
 * to SUMO mappings files called "WordNetMappings-nouns.txt",
 * "WordNetMappings-verbs.txt" etc The main part of the program prompts the user
 * for an English target and then returns associated SUMO concepts. The two
 * primary public methods are initOnce() and page().
 *
 * @author Ian Niles
 * @author Adam Pease
 */
public class WordNet {

    public static WordNet wn;
    private static String baseDir = "";
    private static File baseDirFile;
    public static boolean initNeeded = true;

    private static final String[][] wnFilenamePatterns
            = {{"noun_mappings", "WordNetMappings.*noun.*txt"},
            {"verb_mappings", "WordNetMappings.*verb.*txt"},
            {"adj_mappings", "WordNetMappings.*adj.*txt"},
            {"adv_mappings", "WordNetMappings.*adv.*txt"},
            {"noun_exceptions", "noun.exc"},
            {"verb_exceptions", "verb.exc"},
            {"adj_exceptions", "adj.exc"},
            {"adv_exceptions", "adv.exc"},
            {"sense_indexes", "index.sense"},
            {"word_frequencies", "wordFrequencies.txt"},
            {"stopwords", "stopwords.txt"},
            {"messages", "messages.txt"}
            };

    /**
     * Returns the WordNet File object corresponding to key. The purpose of this
     * accessor is to make it easier to deal with possible changes to these file
     * names, since the descriptive key, ideally, need not change. Each key maps
     * to a regular expression that is used to match against filenames found in
     * the directory denoted by WordNet.baseDir. If multiple filenames match the
     * pattern for one key, then the file that was most recently changed
     * (presumably, saved) is chosen.
     *
     * @param key A descriptive literal String that maps to a regular expression
     * pattern used to obtain a WordNet file.
     *
     * @return A File object
     */
    public static File getWnFile(String key) {
        File theFile = null;
        try {
            String pattern = null;
            int i;
            for (i = 0; i < wnFilenamePatterns.length; i++) {
                if ((wnFilenamePatterns[i][0]).equalsIgnoreCase(key)) {
                    pattern = wnFilenamePatterns[i][1];
                    break;
                }
            }
            if ((pattern != null) && (baseDirFile != null)) {
                File[] wnFiles = baseDirFile.listFiles();
                if (wnFiles != null) {
                    for (i = 0; i < wnFiles.length; i++) {
                        if (wnFiles[i].getName().matches(pattern) && wnFiles[i].exists()) {
                            if (theFile != null) {
                                if (wnFiles[i].lastModified() > theFile.lastModified()) {
                                    theFile = wnFiles[i];
                                }
                            } else {
                                theFile = wnFiles[i];
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return theFile;
    }

    /**
     * This array contains all of the regular expression strings that will be
     * compiled to Pattern objects for use in the methods in this file.
     */
    @SuppressWarnings("HardcodedFileSeparator")
    private static final String[] regexPatternStrings
            = {
                
                "^\\s*\\d\\d\\s\\S\\s\\d\\S\\s",
                
                "^([a-zA-Z0-9'._\\-]\\S*)\\s([0-9a-f])\\s",
                
                "^...\\s",
                
                "^(\\S\\S?)\\s([0-9]{8})\\s(.)\\s([0-9a-f]{4})\\s?",
                
                "^..\\s",
                
                "^\\+\\s(\\d\\d)\\s(\\d\\d)\\s?",
                
                "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?&%\\S+[\\S\\s]+)$",
                
                "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$",
                
                "(\\S+)\\s+(\\S+)",
                
                "(\\S+)\\s+(\\S+)\\s+(\\S+)",
                
                "^([0-9]{8})([^|]+)\\|\\s([\\S\\s]+?)\\s(\\(?&%\\S+[\\S\\s]+)$",
                
                "^([0-9]{8})([^|]+)\\|\\s([\\S\\s]+)$",
                
                "(\\S+)\\s+(\\S+)",
                
                "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?&%\\S+[\\S\\s]+)$",
                
                "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$",
                
                "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)\\s(\\(?&%\\S+[\\S\\s]+)$",
                
                "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$",
                
                "^Word: ([^ ]+) Values: (.*)",
                
                "([^%]+)%([^:]*):[^:]*:[^:]*:[^:]*:[^ ]* ([^ ]+) ([^ ]+) .*",
                
                "(\\w)'re",
                
                "(\\w)'m",
                
                "(\\w)n't",
                
                "(\\w)'ll",
                
                "(\\w)'s",
                
                "(\\w)'d",
                
                "(\\w)'ve"
            };

    /**
     * This array contains all of the compiled Pattern objects that will be used
     * by methods in this file.
     */
    private static Pattern[] regexPatterns;

    /**
     * This method compiles all of the regular expression pattern strings in
     * regexPatternStrings and puts the resulting compiled Pattern objects in
     * the Pattern[] regexPatterns.
     */
    private static void compileRegexPatterns() {
        System.out.println("INFO in WordNet.compileRegexPatterns(): compiling patterns");
        regexPatterns = new Pattern[regexPatternStrings.length];
        for (int i = 0; i < regexPatternStrings.length; i++) {
            regexPatterns[i] = Pattern.compile(regexPatternStrings[i]);
            if (!(regexPatterns[i] instanceof Pattern)) {
                System.out.println("ERROR in WordNet.compileRegexPatterns(): could not compile \""
                        + regexPatternStrings[i]
                        + '"');
            }
        }
    }

    private final Hashtable nounSynsetHash = new Hashtable();   
    private final Hashtable verbSynsetHash = new Hashtable();   
    private final Hashtable adjectiveSynsetHash = new Hashtable();
    private final Hashtable adverbSynsetHash = new Hashtable();

    private final Hashtable verbDocumentationHash = new Hashtable();       
    private final Hashtable adjectiveDocumentationHash = new Hashtable();  
    private final Hashtable adverbDocumentationHash = new Hashtable();
    private final Hashtable nounDocumentationHash = new Hashtable();

    public Hashtable nounSUMOHash = new Hashtable();   
    public Hashtable verbSUMOHash = new Hashtable();   
    public Hashtable adjectiveSUMOHash = new Hashtable();
    public Hashtable adverbSUMOHash = new Hashtable();

    /**
     * Keys are SUMO terms, values are ArrayLists(s) of POS-prefixed synset
     * String(s) with part of speech prepended to the synset number.
     */
    public Hashtable SUMOHash = new Hashtable();

    /**
     * Keys are String POS-prefixed synsets. Values are ArrayList(s) of
     * String(s) which are words. Note that the order of words in the file is
     * preserved.
     */
    public Hashtable synsetsToWords = new Hashtable();

    private final Hashtable exceptionNounHash = new Hashtable();  
    private final Hashtable exceptionVerbHash = new Hashtable();  

    private final Map exceptionNounPluralHash = new Hashtable();
    private final Map exceptionVerbPastHash = new Hashtable();

    /**
     * Keys are POS-prefixed synsets, values are ArrayList(s) of AVPair(s) in
     * which the attribute is a pointer type according to
     * http:
     * a POS-prefixed synset
     */
    public Hashtable relations = new Hashtable();

    /**
     * a HashMap of HashMaps where the key is a word sense of the form
     * word_POS_num signifying the word, part of speech and number of the sense
     * in WordNet. The value is a HashMap of words and the number of times that
     * word cooccurs in sentences with the word sense given in the key.
     */
    protected HashMap wordFrequencies = new HashMap();

    /**
     * English "stop words" such as "a", "at", "them", which have no or little
     * inherent meaning when taken alone.
     */
    private final List stopwords = new ArrayList();

    /**
     * A HashMap where the keys are of the form word_POS_num, and values are 8
     * digit WordNet synset byte offsets.
     */
    private final HashMap senseIndex = new HashMap();

    /**
     * A HashMap where keys are 8 digit WordNet synset byte offsets or synsets
     * appended with a dash and a specific word such as "12345678-foo". Values
     * are ArrayList(s) of String verb frame numbers.
     */
    private final HashMap verbFrames = new HashMap();

    /**
     * A HashMap with words as keys and ArrayList as values. The ArrayList
     * contains word senses which are Strings of the form word_POS_num
     * signifying the word, part of speech and number of the sense in WordNet.
     */
    private final HashMap wordsToSenses = new HashMap();

    private Pattern p;
    private Matcher m;

    public static final int NOUN = 1;
    public static final int VERB = 2;
    public static final int ADJECTIVE = 3;
    public static final int ADVERB = 4;
    public static final int ADJECTIVE_SATELLITE = 5;

    /**
     * ***************************************************************
     * Add a synset (with part of speech number prefix) and the SUMO target that
     * maps to it.
     */
    private void addSUMOHash(String term, String synset) {

        
        
        term = term.substring(2, term.length() - 1);
        ArrayList synsets = (ArrayList) SUMOHash.get(term);
        if (synsets == null) {
            synsets = new ArrayList();
            SUMOHash.put(term, synsets);
        }
        synsets.add(synset);
    }

    /**
     * ***************************************************************
     * Return an ArrayList of the string split by spaces.
     */
    private static ArrayList splitToArrayList(String st) {

        String[] sentar = st.split(" ");
        ArrayList words = new ArrayList(Arrays.asList(sentar));
        return words;
    }

    /**
     * ***************************************************************
     * Add a synset and its corresponding word to the synsetsToWords variable.
     * Prefix the synset with its part of speech before adding.
     */
    private void addToSynsetsToWords(String word, String synsetStr, String POS) {

        ArrayList al = (ArrayList) synsetsToWords.get(POS + synsetStr);
        if (al == null) {
            al = new ArrayList();
            synsetsToWords.put(POS + synsetStr, al);
        }
        al.add(word);

        switch (POS.charAt(0)) {
            case '1':
                @SuppressWarnings("LocalVariableUsedAndDeclaredInDifferentSwitchBranches") String synsets = (String) nounSynsetHash.get(word);
                if (synsets == null) {
                    synsets = "";
                }
                if (!synsets.contains(synsetStr)) {
                    if (!synsets.isEmpty()) {
                        synsets += " ";
                    }
                    synsets += synsetStr;
                    nounSynsetHash.put(word, synsets);
                }
                break;
            case '2':
                synsets = (String) verbSynsetHash.get(word);
                if (synsets == null) {
                    synsets = "";
                }
                if (!synsets.contains(synsetStr)) {
                    if (!synsets.isEmpty()) {
                        synsets += " ";
                    }
                    synsets += synsetStr;
                    verbSynsetHash.put(word, synsets);
                }
                break;
            case '3':
                synsets = (String) adjectiveSynsetHash.get(word);
                if (synsets == null) {
                    synsets = "";
                }
                if (!synsets.contains(synsetStr)) {
                    if (!synsets.isEmpty()) {
                        synsets += " ";
                    }
                    synsets += synsetStr;
                    adjectiveSynsetHash.put(word, synsets);
                }
                break;
            case '4':
                synsets = (String) adverbSynsetHash.get(word);
                if (synsets == null) {
                    synsets = "";
                }
                if (!synsets.contains(synsetStr)) {
                    if (!synsets.isEmpty()) {
                        synsets += " ";
                    }
                    synsets += synsetStr;
                    adverbSynsetHash.put(word, synsets);
                }
                break;
        }
    }

    /**
     * ***************************************************************
     * Process some of the fields in a WordNet .DAT file as described at
     * http:
     * POS-prefix. Input should be of the form lex_filenum ss_type w_cnt word
     * lex_id [word lex_id...] p_cnt [ptr...] [frames...]
     */
    private void processPointers(String synset, String pointers) {

        
        
        m = regexPatterns[0].matcher(pointers);
        pointers = m.replaceFirst("");
        

        
        
        
        m = regexPatterns[1].matcher(pointers);
        while (m.lookingAt()) {
            String word = m.group(1);
            if (word.length() > 3 && ("(a)".equals(word.substring(word.length() - 3))
                    || "(p)".equals(word.substring(word.length() - 3)))) {
                word = word.substring(0, word.length() - 3);
            }
            if (word.length() > 4 && "(ip)".equals(word.substring(word.length() - 4))) {
                word = word.substring(0, word.length() - 4);
            }
            String count = m.group(2);
            addToSynsetsToWords(word, synset.substring(1), synset.substring(0, 1));
            pointers = m.replaceFirst("");
            m = regexPatterns[1].matcher(pointers);
        }
        

        
        
        
        m = regexPatterns[2].matcher(pointers);
        pointers = m.replaceFirst("");

        
        
        
        
        
        m = regexPatterns[3].matcher(pointers);
        while (m.lookingAt()) {
            String ptr = m.group(1);
            String targetSynset = m.group(2);
            String targetPOS = m.group(3);
            String sourceTarget = m.group(4);
            targetPOS = (new Character(WordNetUtilities.posLetterToNumber(targetPOS.charAt(0)))).toString();
            pointers = m.replaceFirst("");
            m = regexPatterns[3].matcher(pointers);
            ptr = WordNetUtilities.convertWordNetPointer(ptr);
            AVPair avp = new AVPair();
            avp.attribute = ptr;
            avp.value = targetPOS + targetSynset;
            ArrayList al = new ArrayList();
            if (relations.containsKey(synset)) {
                al = (ArrayList) relations.get(synset);
            } else {
                relations.put(synset, al);
            }
            
            
            al.add(avp);
        }
        if (!pointers.isEmpty() && !" ".equals(pointers)) {
            
            
            if (synset.charAt(0) == '2') {
                
                m = regexPatterns[4].matcher(pointers);
                pointers = m.replaceFirst("");
                
                m = regexPatterns[5].matcher(pointers);
                while (m.lookingAt()) {
                    String frameNum = m.group(1);
                    String wordNum = m.group(2);
                    String key;
                    if ("00".equals(wordNum)) {
                        key = synset.substring(1);
                    } else {
                        int num = Integer.parseInt(wordNum);
                        ArrayList al = (ArrayList) synsetsToWords.get(synset);
                        if (al == null) {
                            System.out.println("Error in WordNet.processPointers(): "
                                    + synset
                                    + " has no words for pointers: \""
                                    + pointers
                                    + '"');
                        }
                        String word = (String) al.get(num - 1);
                        key = synset.substring(1) + '-' + word;
                    }
                    ArrayList frames = new ArrayList();
                    if (!verbFrames.containsKey(key)) {
                        verbFrames.put(key, frames);
                    } else {
                        frames = (ArrayList) verbFrames.get(key);
                    }
                    frames.add(frameNum);
                    pointers = m.replaceFirst("");
                    m = regexPatterns[5].matcher(pointers);
                }
            } else {
                System.out.println("Error in WordNet.processPointers(): " + synset.charAt(0) + " leftover pointers: \"" + pointers + '"');
            }
        }
    }

    /**
     * ***************************************************************
     */
    private void addSUMOMapping(String SUMO, String synset) {

        SUMO = SUMO.trim();
        switch (synset.charAt(0)) {
            case '1':
                nounSUMOHash.put(synset.substring(1), SUMO);
                break;
            case '2':
                verbSUMOHash.put(synset.substring(1), SUMO);
                break;
            case '3':
                adjectiveSUMOHash.put(synset.substring(1), SUMO);
                break;
            case '4':
                adverbSUMOHash.put(synset.substring(1), SUMO);
                break;
        }
        addSUMOHash(SUMO, synset);
    }

    /**
     * ***************************************************************
     * Get the SUMO mapping for a POS-prefixed synset
     */
    public String getSUMOMapping(String synset) {

        if (synset == null) {
            System.out.println("Error in WordNet.getSUMOMapping: null synset ");
            return null;
        }
        switch (synset.charAt(0)) {
            case '1':
                return (String) nounSUMOHash.get(synset.substring(1));
            case '2':
                return (String) verbSUMOHash.get(synset.substring(1));
            case '3':
                return (String) adjectiveSUMOHash.get(synset.substring(1));
            case '4':
                return (String) adverbSUMOHash.get(synset.substring(1));
        }
        System.out.println("Error in WordNet.getSUMOMapping: improper first character for synset: " + synset);
        return null;
    }

    /**
     * ***************************************************************
     * Create the hashtables nounSynsetHash, nounDocumentationHash, nounSUMOhash
     * and exceptionNounHash that contain the WordNet noun synsets, word
     * definitions, mappings to SUMO, and plural exception forms, respectively.
     * Throws an IOException if the files are not found.
     */
    private void readNouns() {

        System.out.println("INFO in WordNet.readNouns(): Reading WordNet noun files");

        try {

            
            String line;
            File nounFile = getWnFile("noun_mappings");
            if (nounFile == null) {
                System.out.println("INFO in WordNet.readNouns(): "
                        + "The noun mappings file does not exist");
                return;
            }
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(nounFile);
            
            LineNumberReader lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0) {
                    System.out.print('.');
                }
                line = line.trim();
                
                m = regexPatterns[6].matcher(line);
                boolean anyAreNull = false;
                if (m.matches()) {
                    for (int i = 1; i < 5; i++) {
                        anyAreNull = (m.group(i) == null);
                        if (anyAreNull) {
                            break;
                        }
                    }
                    if (!anyAreNull) {
                        addSUMOMapping(m.group(4), '1' + m.group(1));
                        nounDocumentationHash.put(m.group(1), m.group(3)); 
                        processPointers('1' + m.group(1), m.group(2));
                    }
                } else {
                    
                    m = regexPatterns[7].matcher(line);
                    if (m.matches()) {
                        nounDocumentationHash.put(m.group(1), m.group(3));
                        processPointers('1' + m.group(1), m.group(2));
                    } else {
                        
                        if (!line.isEmpty() && line.charAt(0) != ';') {
                            System.out.println();
                            System.out.println("Error in WordNet.readNouns(): No match in "
                                    + nounFile.getCanonicalPath()
                                    + " for line "
                                    + line);
                        }
                    }
                }
            }
            System.out.println("x");
            System.out.println("  "
                    + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process "
                    + nounFile.getCanonicalPath());

            
            nounFile = getWnFile("noun_exceptions");
            if (nounFile == null) {
                System.out.println("INFO in WordNet.readNouns(): "
                        + "The noun mapping exceptions file does not exist");
                return;
            }
            t1 = System.currentTimeMillis();
            r = new FileReader(nounFile);
            lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                
                m = regexPatterns[8].matcher(line);
                if (m.matches()) {
                    exceptionNounHash.put(m.group(1), m.group(2));      
                    exceptionNounPluralHash.put(m.group(2), m.group(1));
                } else {
                    
                    m = regexPatterns[9].matcher(line);
                    if (m.matches()) {
                        exceptionNounHash.put(m.group(1), m.group(2));      
                        exceptionNounPluralHash.put(m.group(2), m.group(1));
                        exceptionNounPluralHash.put(m.group(3), m.group(1));
                    } else if (!line.isEmpty() && line.charAt(0) != ';') {
                        System.out.println("Error in WordNet.readNouns(): No match in "
                                + nounFile.getCanonicalPath()
                                + " for line "
                                + line);
                    }
                }
            }
            System.out.println("  "
                    + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process "
                    + nounFile.getCanonicalPath());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     * Create the hashtables verbSynsetHash, verbDocumentationHash, verbSUMOhash
     * and exceptionVerbHash that contain the WordNet verb synsets, word
     * definitions, mappings to SUMO, and plural exception forms, respectively.
     * Throws an IOException if the files are not found.
     */
    private void readVerbs() {

        System.out.println("INFO in WordNet.readVerbs(): Reading WordNet verb files");

        try {
            String line;
            File verbFile = getWnFile("verb_mappings");
            if (verbFile == null) {
                System.out.println("INFO in WordNet.readVerbs(): "
                        + "The verb mappings file does not exist");
                return;
            }
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(verbFile);
            LineNumberReader lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0) {
                    System.out.print('.');
                }
                line = line.trim();
                
                m = regexPatterns[10].matcher(line);
                if (m.matches()) {
                    verbDocumentationHash.put(m.group(1), m.group(3));
                    addSUMOMapping(m.group(4), '2' + m.group(1));
                    processPointers('2' + m.group(1), m.group(2));
                } else {
                    
                    m = regexPatterns[11].matcher(line);
                    if (m.matches()) {
                        verbDocumentationHash.put(m.group(1), m.group(3));
                        processPointers('2' + m.group(1), m.group(2));
                    } else {
                        
                        if (!line.isEmpty() && line.charAt(0) != ';') {
                            System.out.println();
                            System.out.println("Error in WordNet.readVerbs(): No match in "
                                    + verbFile.getCanonicalPath()
                                    + " for line "
                                    + line);
                        }
                    }
                }
            }
            System.out.println("x");
            System.out.println("  "
                    + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process "
                    + verbFile.getCanonicalPath());

            
            verbFile = getWnFile("verb_exceptions");
            if (verbFile == null) {
                System.out.println("INFO in WordNet.readVerbs(): "
                        + "The verb mapping exceptions file does not exist");
                return;
            }
            t1 = System.currentTimeMillis();
            r = new FileReader(verbFile);
            lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                
                m = regexPatterns[12].matcher(line);
                if (m.matches()) {
                    exceptionVerbHash.put(m.group(1), m.group(2));          
                    exceptionVerbPastHash.put(m.group(2), m.group(1));
                } else if (!line.isEmpty() && line.charAt(0) != ';') {
                    System.out.println("Error in WordNet.readVerbs(): No match in "
                            + verbFile.getCanonicalPath()
                            + " for line "
                            + line);
                }
            }
            System.out.println("  "
                    + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process "
                    + verbFile.getCanonicalPath());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     * Create the hashtables adjectiveSynsetHash, adjectiveDocumentationHash,
     * and adjectiveSUMOhash that contain the WordNet adjective synsets, word
     * definitions, and mappings to SUMO, respectively. Throws an IOException if
     * the files are not found.
     */
    private void readAdjectives() {

        System.out.println("INFO in WordNet.readAdjectives(): Reading WordNet adjective files");

        try {
            String line;
            File adjFile = getWnFile("adj_mappings");
            if (adjFile == null) {
                System.out.println("INFO in WordNet.readAdjectives(): "
                        + "The adjective mappings file does not exist");
                return;
            }
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(adjFile);
            LineNumberReader lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0) {
                    System.out.print('.');
                }
                line = line.trim();
                
                m = regexPatterns[13].matcher(line);
                if (m.matches()) {
                    adjectiveDocumentationHash.put(m.group(1), m.group(3));
                    addSUMOMapping(m.group(4), '3' + m.group(1));
                    processPointers('3' + m.group(1), m.group(2));
                } else {
                    
                    m = regexPatterns[14].matcher(line);
                    if (m.matches()) {
                        adjectiveDocumentationHash.put(m.group(1), m.group(3));
                        processPointers('3' + m.group(1), m.group(2));
                    } else {
                        
                        if (!line.isEmpty() && line.charAt(0) != ';') {
                            System.out.println();
                            System.out.println("Error in WordNet.readAdjectives(): No match in "
                                    + adjFile.getCanonicalPath()
                                    + " for line "
                                    + line);
                        }
                    }
                }
            }
            System.out.println("x");
            System.out.println("  "
                    + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process "
                    + adjFile.getCanonicalPath());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     * Create the hashtables adverbSynsetHash, adverbDocumentationHash, and
     * adverbSUMOhash that contain the WordNet adverb synsets, word definitions,
     * and mappings to SUMO, respectively. Throws an IOException if the files
     * are not found.
     */
    private void readAdverbs() {

        System.out.println("INFO in WordNet.readAdverbs(): Reading WordNet adverb files");

        try {
            String line;
            File advFile = getWnFile("adv_mappings");
            if (advFile == null) {
                System.out.println("INFO in WordNet.readAdverbs(): "
                        + "The adverb mappings file does not exist");
                return;
            }
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(advFile);
            LineNumberReader lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0) {
                    System.out.print('.');
                }
                line = line.trim();
                
                m = regexPatterns[15].matcher(line);
                if (m.matches()) {
                    adverbDocumentationHash.put(m.group(1), m.group(3));
                    addSUMOMapping(m.group(4), '4' + m.group(1));
                    processPointers('4' + m.group(1), m.group(2));
                } else {
                    
                    m = regexPatterns[16].matcher(line);
                    if (m.matches()) {
                        adverbDocumentationHash.put(m.group(1), m.group(3));
                        processPointers('4' + m.group(1), m.group(2));
                    } else {
                        
                        if (!line.isEmpty() && line.charAt(0) != ';') {
                            System.out.println();
                            System.out.println("Error in WordNet.readAdverbs(): No match in "
                                    + advFile.getCanonicalPath()
                                    + " for line "
                                    + line);
                        }
                    }
                }
            }
            System.out.println("x");
            System.out.println("  "
                    + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process "
                    + advFile.getCanonicalPath());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     * Return a HashMap of HashMaps where the key is a word sense of the form
     * word_POS_num signifying the word, part of speech and number of the sense
     * in WordNet. The value is a HashMap of words and the number of times that
     * word cooccurs in sentences with the word sense given in the key.
     */
    public void readWordFrequencies() {

        System.out.println("INFO in WordNet.readWordFrequencies(): Reading WordNet word frequencies");

        int counter = 0;
        File wfFile;
        String canonicalPath = "";
        try {
            wfFile = getWnFile("word_frequencies");
            if (wfFile == null) {
                System.out.println("INFO in WordNet.readWordFrequencies(): "
                        + "The word frequencies file does not exist");
                return;
            }
            canonicalPath = wfFile.getCanonicalPath();
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(wfFile);
            LineNumberReader lr = new LineNumberReader(r);
            Matcher m;
            String key;
            String values;
            String[] words;
            HashMap frequencies;
            String word;
            String freq;
            String line;
            while ((line = lr.readLine()) != null) {
                line = line.trim();
                
                m = regexPatterns[17].matcher(line);
                if (m.matches()) {
                    key = m.group(1);
                    values = m.group(2);
                    words = values.split(" ");
                    frequencies = new HashMap();
                    for (int i = 0; i < words.length - 3; i++) {
                        if ("SUMOterm:".equals(words[i])) {
                            i = words.length;
                        } else {
                            if (words[i].indexOf('_') == -1) {
                                
                                
                            } else {
                                word = words[i].substring(0, words[i].indexOf('_'));
                                freq = words[i].substring(words[i].lastIndexOf('_') + 1);
                                frequencies.put(word.intern(), Integer.decode(freq));
                            }
                        }
                    }
                    wordFrequencies.put(key.intern(), frequencies);
                    counter++;
                    if (counter == 1000) {
                        System.out.print(".");
                        counter = 0;
                    }
                }
            }
            System.out.println("x");
            System.out.println("  "
                    + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process "
                    + canonicalPath);
        } catch (Exception i) {
            System.out.println();
            System.out.println("Error in WordNet.readWordFrequencies() reading file "
                    + canonicalPath
                    + ": "
                    + i.getMessage());
            i.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     */
    public void readStopWords() {

        System.out.println("INFO in WordNet.readStopWords(): Reading stop words");
        File swFile;
        String canonicalPath = "";
        try {
            swFile = getWnFile("stopwords");
            if (swFile == null) {
                System.out.println("INFO in WordNet.readStopWords(): "
                        + "The stopwords file does not exist");
                return;
            }
            canonicalPath = swFile.getCanonicalPath();
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(swFile);
            LineNumberReader lr = new LineNumberReader(r);
            String line;
            while ((line = lr.readLine()) != null) {
                stopwords.add(line.intern());
            }
            System.out.println("  "
                    + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process "
                    + canonicalPath);
        } catch (Exception i) {
            System.out.println("Error in WordNet.readStopWords() reading file "
                    + canonicalPath
                    + ": "
                    + i.getMessage());
            i.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     */
    public void readSenseIndex() {

        System.out.println("INFO in WordNet.readSenseIndex(): Reading WordNet sense index");

        int counter = 0;
        File siFile;
        String canonicalPath = "";
        try {
            siFile = getWnFile("sense_indexes");
            if (siFile == null) {
                System.out.println("INFO in WordNet.readSenseIndex(): "
                        + "The sense indexes file does not exist");
                return;
            }
            canonicalPath = siFile.getCanonicalPath();
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(siFile);
            LineNumberReader lr = new LineNumberReader(r);
            
            Matcher m;
            String word;
            String pos;
            String synset;
            String sensenum;
            String posString;
            String key;
            ArrayList al;
            String line;
            while ((line = lr.readLine()) != null) {
                
                m = regexPatterns[18].matcher(line);
                if (m.matches()) {
                    word = m.group(1);
                    pos = m.group(2);
                    synset = m.group(3);
                    sensenum = m.group(4);
                    posString = WordNetUtilities.posNumberToLetters(pos);
                    key = word + '_' + posString + '_' + sensenum;
                    word = word.intern();
                    al = (ArrayList) wordsToSenses.get(word);
                    if (al == null) {
                        al = new ArrayList();
                        wordsToSenses.put(word, al);
                    }
                    al.add(key);
                    senseIndex.put(key, synset);
                    counter++;
                    if (counter == 1000) {
                        
                        
                        System.out.print('.');
                        counter = 0;
                    }
                }
            }
            System.out.println("x");
            System.out.println("  "
                    + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process "
                    + canonicalPath);
        } catch (Exception i) {
            System.out.println();
            System.out.println("Error in WordNet.readSenseIndex() reading file "
                    + canonicalPath
                    + ": "
                    + i.getMessage());
            i.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     * Return the best guess at the synset for the given word in the context of
     * the sentence. Returns an ArrayList consisting of a 9-digit WordNet
     * synset, the corresponding SUMO target, and the score reflecting the quality
     * of the guess the given synset is the right one.
     */
    private ArrayList findSUMOWordSenseArray(String word, Iterable words, int POS) {

        String SUMOterm = null;

        
        ArrayList senses = (ArrayList) wordsToSenses.get(word.intern());
        if (senses == null) {
            
            return new ArrayList();
        }
        int firstSense = -1;
        int bestSense = -1;
        int bestTotal = -1;
        for (int i = 0; i < senses.size(); i++) {
            String sense = (String) senses.get(i);
            if (WordNetUtilities.sensePOS(sense) == POS) {
                
                if (firstSense == -1) {
                    firstSense = i;
                }
                HashMap senseAssoc = (HashMap) wordFrequencies.get(sense.intern());
                if (senseAssoc != null) {
                    int total = 0;
                    for (Object word1 : words) {
                        String lowercase = ((String) word1).toLowerCase().intern();
                        if (senseAssoc.containsKey(lowercase)) {
                            total += ((Number) senseAssoc.get(lowercase)).intValue();
                        }
                    }
                    if (total > bestTotal) {
                        bestTotal = total;
                        bestSense = i;
                    }
                    
                    
                }
            }
        }
        if (bestSense == -1) {             
            if (firstSense == -1) {        
                
                
                return new ArrayList();
            }
            bestSense = firstSense;
        }
        String senseValue = (String) senses.get(bestSense);
        
        String synset = (String) senseIndex.get(senseValue.intern());
        
        switch (POS) {
            case NOUN:
                SUMOterm = (String) nounSUMOHash.get(synset.intern());
                break;
            case VERB:
                SUMOterm = (String) verbSUMOHash.get(synset.intern());
                break;
            case ADJECTIVE:
                SUMOterm = (String) adjectiveSUMOHash.get(synset.intern());
                break;
            case ADVERB:
                SUMOterm = (String) adverbSUMOHash.get(synset.intern());
                break;
        }
        
        if (SUMOterm != null) {                                                
            SUMOterm = SUMOterm.replaceAll("&%", "");
            SUMOterm = SUMOterm.replaceAll("[+=@]", "");
        }
        ArrayList result = new ArrayList();
        result.add((POS) + synset);
        result.add(SUMOterm);
        result.add((Integer.valueOf(bestTotal)).toString());
        return result;
    }

    /**
     * ***************************************************************
     * Return the best guess at the synset for the given word in the context of
     * the sentence. Returns a SUMO target.
     */
    private String findSUMOWordSense(String word, ArrayList words, int POS) {

        ArrayList result = findSUMOWordSenseArray(word, words, POS);
        return (String) result.get(1);
    }

    /**
     * ***************************************************************
     * Return the best guess at the synset for the given word in the context of
     * the sentence. Returns a SUMO target.
     */
    public String findSUMOWordSense(String word, ArrayList words) {

        int bestScore = 0;
        int POS = 0;
        String bestTerm = "";
        for (int i = 1; i < 4; i++) {
            String newWord = "";
            if (i == 1) {
                newWord = nounRootForm(word, word.toLowerCase());
            }
            if (i == 2) {
                newWord = verbRootForm(word, word.toLowerCase());
            }
            if (newWord != null && !newWord.isEmpty()) {
                word = newWord;
            }
            ArrayList al = findSUMOWordSenseArray(word, words, i);
            if (al != null && !al.isEmpty()) {
                String synset = (String) al.get(0); 
                String SUMOterm = (String) al.get(1);
                String bestTotal = (String) al.get(2);
                int total = Integer.parseInt(bestTotal);
                if (total > bestScore) {
                    bestScore = total;
                    POS = i;
                    bestTerm = SUMOterm;
                }
            }
        }
        return bestTerm;
    }

    /**
     * ***************************************************************
     * Remove punctuation and contractions from a sentence.
     */
    private static String removePunctuation(String sentence) {

        Matcher m;

        
        m = regexPatterns[19].matcher(sentence);
        while (m.find()) {
            
            String group = m.group(1);
            sentence = m.replaceFirst(group);
            m.reset(sentence);
        }
        
        m = regexPatterns[20].matcher(sentence);
        while (m.find()) {
            
            String group = m.group(1);
            sentence = m.replaceFirst(group);
            m.reset(sentence);
        }
        
        m = regexPatterns[21].matcher(sentence);
        while (m.find()) {
            
            String group = m.group(1);
            sentence = m.replaceFirst(group);
            m.reset(sentence);
        }
        
        m = regexPatterns[22].matcher(sentence);
        while (m.find()) {
            
            String group = m.group(1);
            sentence = m.replaceFirst(group);
            m.reset(sentence);
        }
        
        m = regexPatterns[23].matcher(sentence);
        while (m.find()) {
            
            String group = m.group(1);
            sentence = m.replaceFirst(group);
            m.reset(sentence);
        }
        
        m = regexPatterns[24].matcher(sentence);
        while (m.find()) {
            
            String group = m.group(1);
            sentence = m.replaceFirst(group);
            m.reset(sentence);
        }
        
        m = regexPatterns[25].matcher(sentence);
        while (m.find()) {
            
            String group = m.group(1);
            sentence = m.replaceFirst(group);
            m.reset(sentence);
        }
        sentence = sentence.replaceAll("'", "");
        sentence = sentence.replaceAll("\"", "");
        sentence = sentence.replaceAll("\\.", "");
        sentence = sentence.replaceAll(";", "");
        sentence = sentence.replaceAll(":", "");
        sentence = sentence.replaceAll("\\?", "");
        sentence = sentence.replaceAll("!", "");
        sentence = sentence.replaceAll(", ", " ");
        sentence = sentence.replaceAll(",[^ ]", ", ");
        sentence = sentence.replaceAll(" {2}", " ");
        return sentence;
    }

    /**
     * ***************************************************************
     * Remove stop words from a sentence.
     */
    private String removeStopWords(String sentence) {

        String result = "";
        ArrayList al = splitToArrayList(sentence);
        for (Object anAl : al) {
            String word = (String) anAl;
            if (!stopwords.contains(word.toLowerCase())) {
                result = result != null && result.isEmpty() ? word : result + ' ' + word;
            }
        }
        return result;
    }

    /**
     * ***************************************************************
     * Collect all the SUMO terms that represent the best guess at meanings for
     * all the words in a sentence.
     */
    public String getBestDefaultSense(String word) {

        String SUMO = "";
        String newWord = "";
        int i = 0;
        while (SUMO != null && SUMO.isEmpty() && i < 4) {
            i++;
            if (i == 1) {
                newWord = nounRootForm(word, word.toLowerCase());
            }
            if (i == 2) {
                newWord = verbRootForm(word, word.toLowerCase());
            }
            if (newWord != null && !newWord.isEmpty()) {
                word = newWord;
            }
            SUMO = getSUMOterm(word, i);
        }
        return SUMO;
    }

    /**
     * ***************************************************************
     * Collect all the SUMO terms that represent the best guess at meanings for
     * all the words in a sentence.
     */
    public String collectSUMOWordSenses(String sentence) {

        String result = "";
        
        String newSentence = removePunctuation(sentence);
        newSentence = removeStopWords(newSentence);
        
        ArrayList al = splitToArrayList(newSentence);
        for (int i = 0; i < al.size(); i++) {
            String word = (String) al.get(i);
            String SUMO = findSUMOWordSense(word, al);
            if (SUMO != null && !SUMO.isEmpty()) {
                result = result != null && result.isEmpty() ? SUMO : result + ' ' + SUMO;
            } else {                                    
                SUMO = getBestDefaultSense(word);
                if (SUMO != null && !SUMO.isEmpty()) {
                    result = result != null && result.isEmpty() ? SUMO : result + ' ' + SUMO;
                }
            }
            /**
             * if (SUMO == null || SUMO == "") System.out.println("INFO in
             * findSUMOWordSense(): word not found: " + word); else
             * System.out.println("INFO in findSUMOWordSense(): word, target: " +
             * word + ", " + SUMO);
             */
        }
        return result;
    }

    /**
     * ***************************************************************
     * Read the WordNet files only on initialization of the class.
     */
    public static void initOnce() {

        try {
            if (initNeeded) {
                if (WordNet.baseDir == null || WordNet.baseDir.isEmpty()) {
                    WordNet.baseDir = KBmanager.manager.getPref("kbDir");
                }
                baseDirFile = new File(WordNet.baseDir);
                wn = new WordNet();
                WordNet.compileRegexPatterns();
                wn.readNouns();
                wn.readVerbs();
                wn.readAdjectives();
                wn.readAdverbs();
                
                wn.readStopWords();
                wn.readSenseIndex();
                initNeeded = false;
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     * Split apart the block of synsets, and return the separated values as an
     * array.
     */
    private static String[] splitSynsets(String synsetBlock) {

        String[] synsetList = null;
        if (synsetBlock != null) {
            synsetList = synsetBlock.split("\\s+");
        }
        return synsetList;
    }

    /**
     * ***************************************************************
     * The main routine which looks up the search word in the hashtables to find
     * the relevant word definitions and SUMO mappings.
     *
     * @param word is the word the user is asking to search for.
     * @param type is whether the word is a noun or verb (we need to addAt
     * capability for adjectives and adverbs.
     * @param
     */
    @SuppressWarnings("HardcodedFileSeparator")
    private String sumoDisplay(String synsetBlock, String word, String type, String sumokbname, String synsetNum) {

        StringBuilder result = new StringBuilder();
        String synset;
        String documentation = "";
        String sumoEquivalent = "";
        int listLength;
        String[] synsetList = splitSynsets(synsetBlock);

        listLength = synsetList != null ? synsetList.length : 0;
        result.append("<i>According to WordNet, the ").append(type).append('"').append(word).append("\" has ");
        result.append(listLength).append(" sense(s).</i><P>\n\n");

        for (int i = 0; i < listLength; i++) {         
            synset = synsetList[i];
            synset = synset.trim();
            if (synset.equals(synsetNum)) {
                result.append("<b>");
            }
            if (type.compareTo("noun") == 0) {
                documentation = (String) nounDocumentationHash.get(synset);
                result.append("<a href=\"WordNet.jsp?synset=1").append(synset).append("\">1").append(synset).append("</a> ");
                result.append(' ').append(documentation).append(".\n");
                sumoEquivalent = (String) nounSUMOHash.get(synset);
            } else {
                if (type.compareTo("verb") == 0) {
                    documentation = (String) verbDocumentationHash.get(synset);
                    result.append("<a href=\"WordNet.jsp?synset=2").append(synset).append("\">2").append(synset).append("</a> ");
                    result.append(' ').append(documentation).append(".\n");
                    sumoEquivalent = (String) verbSUMOHash.get(synset);
                } else {
                    if (type.compareTo("adjective") == 0) {
                        documentation = (String) adjectiveDocumentationHash.get(synset);
                        result.append("<a href=\"WordNet.jsp?synset=3").append(synset).append("\">3").append(synset).append("</a> ");
                        result.append(' ').append(documentation).append(".\n");
                        sumoEquivalent = (String) adjectiveSUMOHash.get(synset);
                    } else {
                        if (type.compareTo("adverb") == 0) {
                            documentation = (String) adverbDocumentationHash.get(synset);
                            result.append("<a href=\"WordNet.jsp?synset=4").append(synset).append("\">4").append(synset).append("</a> ");
                            result.append(' ').append(documentation).append(".\n");
                            sumoEquivalent = (String) adverbSUMOHash.get(synset);
                        }
                    }
                }
            }
            if (synset.equals(synsetNum)) {
                result.append("</b>");
            }
            if (sumoEquivalent == null) {
                result.append("<P><ul><li>").append(word).append(" not yet mapped to SUMO</ul><P>");
            } else {
                
                result.append(sumoEquivalent).append(' ').append(sumokbname);
            }
        }
        String searchTerm = word.replaceAll("_+", "+");
        searchTerm = searchTerm.replaceAll("\\s+", "+");
        result.append("<hr>Explore the word <a href=\"http://wordnet.princeton.edu/perl/webwn/webwn?s=");
        result.append(searchTerm).append("\">").append(word).append("</a> on the WordNet web site.\n");
        return result.toString();
    }

    /**
     * ***************************************************************
     * Return the root form of the noun, or null if it's not in the lexicon.
     */
    public String nounRootForm(String mixedCase, String input) {

        String result = null;

        
        if ((exceptionNounHash.containsKey(mixedCase))
                || (exceptionNounHash.containsKey(input))) {
            result = (String) exceptionNounHash.get(exceptionNounHash.containsKey(mixedCase) ? mixedCase : input);
        } else {
            
            if (WordNetUtilities.substTest(input, "s$", "", nounSynsetHash)) {
                result = WordNetUtilities.subst(input, "s$", "");
            } else {
                if (WordNetUtilities.substTest(input, "ses$", "s", nounSynsetHash)) {
                    result = WordNetUtilities.subst(input, "ses$", "s");
                } else {
                    if (WordNetUtilities.substTest(input, "xes$", "x", nounSynsetHash)) {
                        result = WordNetUtilities.subst(input, "xes$", "x");
                    } else {
                        if (WordNetUtilities.substTest(input, "zes$", "z", nounSynsetHash)) {
                            result = WordNetUtilities.subst(input, "zes$", "z");
                        } else {
                            if (WordNetUtilities.substTest(input, "ches$", "ch", nounSynsetHash)) {
                                result = WordNetUtilities.subst(input, "ches$", "ch");
                            } else {
                                if (WordNetUtilities.substTest(input, "shes$", "sh", nounSynsetHash)) {
                                    result = WordNetUtilities.subst(input, "shes$", "sh");
                                } else {
                                    if (nounSynsetHash.containsKey(mixedCase)) {
                                        result = mixedCase;
                                    } else {
                                        if (nounSynsetHash.containsKey(input)) {
                                            result = input;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * ***************************************************************
     * This routine converts a noun to its singular form and gets the synsets
     * for it, then passes those synsets to sumoDisplay() for processing. First
     * check to see if the input value or its lower-case version are entered in
     * the WordNet exception list (NOUN.EXC). If so, then use the regular form
     * in the exception list to find the synsets in the NOUN.DAT file. If the
     * word is not in the exception list, check to see if the lower case version
     * of the input value is a plural and search over NOUN.DAT in the singular
     * form if it is.
     */
    private String processNoun(String sumokbname, String mixedCase, String input, String synset) {

        String regular;
        String synsetBlock;

        regular = nounRootForm(mixedCase, input);
        if (regular != null) {
            synsetBlock = (String) nounSynsetHash.get(regular);
            return sumoDisplay(synsetBlock, mixedCase, "noun", sumokbname, synset);
        } else {
            return "<P>There are no associated SUMO terms for the noun \"" + mixedCase + "\".<P>\n";
        }
    }

    /**
     * ***************************************************************
     * Return the present tense singular form of the verb, or null if it's not
     * in the lexicon.
     */
    public String verbRootForm(String mixedCase, String input) {

        String result = null;

        if ((exceptionVerbHash.containsKey(mixedCase))
                || (exceptionVerbHash.containsKey(input))) {
            result = (String) exceptionVerbHash.get(exceptionVerbHash.containsKey(mixedCase) ? mixedCase : input);
        } else {
            
            if (WordNetUtilities.substTest(input, "s$", "", verbSynsetHash)) {
                result = WordNetUtilities.subst(input, "s$", "");
            } else {
                if (WordNetUtilities.substTest(input, "es$", "", verbSynsetHash)) {
                    result = WordNetUtilities.subst(input, "es$", "");
                } else {
                    if (WordNetUtilities.substTest(input, "ies$", "y", verbSynsetHash)) {
                        result = WordNetUtilities.subst(input, "ies$", "y");
                    } else {
                        if (WordNetUtilities.substTest(input, "ed$", "", verbSynsetHash)) {
                            result = WordNetUtilities.subst(input, "ed$", "");
                        } else {
                            if (WordNetUtilities.substTest(input, "ed$", "e", verbSynsetHash)) {
                                result = WordNetUtilities.subst(input, "ed$", "e");
                            } else {
                                if (WordNetUtilities.substTest(input, "ing$", "e", verbSynsetHash)) {
                                    result = WordNetUtilities.subst(input, "ing$", "e");
                                } else {
                                    if (WordNetUtilities.substTest(input, "ing$", "", verbSynsetHash)) {
                                        result = WordNetUtilities.subst(input, "ing$", "");
                                    } else {
                                        if (verbSynsetHash.containsKey(mixedCase)) {
                                            result = mixedCase;
                                        } else {
                                            if (verbSynsetHash.containsKey(input)) {
                                                result = input;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * ***************************************************************
     * This routine converts a verb to its present tense singular form and gets
     * the synsets for it, then passes those synsets to sumoDisplay() for
     * processing. First check to see if the input value or its lower-case
     * version are entered in the WordNet exception list (VERB.EXC). If so, then
     * use the regular form in the exception list to find the synsets in the
     * VERB.DAT file. If the word is not in the exception list, check to see if
     * the lower case version of the input value is a singular form and search
     * over VERB.DAT with the infinitive form if it is.
     */
    private String processVerb(String sumokbname, String mixedCase, String input, String synset) {

        String regular;
        String synsetBlock;

        regular = verbRootForm(mixedCase, input);
        if (regular != null) {
            synsetBlock = (String) verbSynsetHash.get(regular);
            return sumoDisplay(synsetBlock, mixedCase, "verb", sumokbname, synset);
        } else {
            return "<P>There are no associated SUMO terms for the verb \"" + mixedCase + "\".<P>\n";
        }
    }

    /**
     * ***************************************************************
     * This routine gets the synsets for an adverb, then passes those synsets to
     * sumoDisplay() for processing.
     */
    private String processAdverb(String sumokbname, String mixedCase, String input, String synset) {

        StringBuilder result = new StringBuilder();
        String synsetBlock;

        synsetBlock = (String) adverbSynsetHash.get(input);
        result.append(sumoDisplay(synsetBlock, mixedCase, "adverb", sumokbname, synset));

        return (result.toString());
    }

    /**
     * ***************************************************************
     * This routine gets the synsets for an adjective, then passes those synsets
     * to sumoDisplay() for processing.
     */
    private String processAdjective(String sumokbname, String mixedCase, String input, String synset) {

        StringBuilder result = new StringBuilder();
        String synsetBlock;

        synsetBlock = (String) adjectiveSynsetHash.get(input);
        result.append(sumoDisplay(synsetBlock, mixedCase, "adjective", sumokbname, synset));

        return (result.toString());
    }

    /**
     * ***************************************************************
     * Get all the synsets for a given word.
     *
     * @return a TreeMap of word keys and values that are ArrayLists of synset
     * Strings
     */
    public TreeMap getSensesFromWord(String word) {

        TreeMap result = new TreeMap();
        String verbRoot = verbRootForm(word, word.toLowerCase());
        String nounRoot = nounRootForm(word, word.toLowerCase());
        ArrayList senses = (ArrayList) wordsToSenses.get(verbRoot);
        if (senses != null) {
            for (Object sense1 : senses) {
                String sense = (String) sense1;                
                String POS = WordNetUtilities.getPOSfromKey(sense);
                String synset = WordNetUtilities.posLettersToNumber(POS) + senseIndex.get(sense);
                Iterable words = (ArrayList) synsetsToWords.get(synset);
                for (Object word1 : words) {
                    String newword = (String) word1;
                    ArrayList al = (ArrayList) result.get(newword);
                    if (al == null) {
                        al = new ArrayList();
                        result.put(newword, al);
                    }
                    al.add(synset);
                }
            }
        }
        senses = (ArrayList) wordsToSenses.get(nounRoot);
        if (senses != null) {
            for (Object sense1 : senses) {
                String sense = (String) sense1;                
                String POS = WordNetUtilities.getPOSfromKey(sense);
                String synset = WordNetUtilities.posLettersToNumber(POS) + senseIndex.get(sense);
                Iterable words = (ArrayList) synsetsToWords.get(synset);
                for (Object word1 : words) {
                    String newword = (String) word1;
                    ArrayList al = (ArrayList) result.get(newword);
                    if (al == null) {
                        al = new ArrayList();
                        result.put(newword, al);
                    }
                    al.add(synset);
                }
            }
        }

        return result;
    }

    /**
     * ***************************************************************
     * Get the words and synsets corresponding to a SUMO target. The return is a
     * Map of words with their corresponding synset number.
     */
    public TreeMap getWordsFromTerm(String SUMOterm) {

        TreeMap result = new TreeMap();
        Iterable synsets = (ArrayList) SUMOHash.get(SUMOterm);
        if (synsets == null) {
            System.out.println("INFO in WordNet.getWordsFromTerm(): No synsets for target : " + SUMOterm);
            return null;
        }
        for (Object synset1 : synsets) {
            String synset = (String) synset1;
            Iterable words = (ArrayList) synsetsToWords.get(synset);
            if (words == null) {
                System.out.println("INFO in WordNet.getWordsFromTerm(): No words for synset: " + synset);
                return null;
            }
            for (Object word1 : words) {
                String word = (String) word1;
                result.put(word, synset);
            }
        }
        return result;
    }

    /**
     * ***************************************************************
     * Get the SUMO target for the given root form word and part of speech.
     */
    public String getSUMOterm(String word, int pos) {

        if (word == null || word.isEmpty()) {
            return null;
        }
        String synsetBlock = null;  

        
        if (pos == NOUN) {
            synsetBlock = (String) nounSynsetHash.get(word);
        }
        if (pos == VERB) {
            synsetBlock = (String) verbSynsetHash.get(word);
        }
        if (pos == ADJECTIVE) {
            synsetBlock = (String) adjectiveSynsetHash.get(word);
        }
        if (pos == ADVERB) {
            synsetBlock = (String) adverbSynsetHash.get(word);
        }

        int listLength;
        String synset;
        String[] synsetList = null;
        if (synsetBlock != null) {
            synsetList = synsetBlock.split("\\s+");
        }
        String term = null;

        if (synsetList != null) {
            synset = synsetList[0];   
            synset = synset.trim();
            if (pos == NOUN) {
                term = (String) nounSUMOHash.get(synset);
            }
            if (pos == VERB) {
                term = (String) verbSUMOHash.get(synset);
            }
            if (pos == ADJECTIVE) {
                term = (String) adjectiveSUMOHash.get(synset);
            }
            if (pos == ADVERB) {
                term = (String) adverbSUMOHash.get(synset);
            }
        }
        return term != null ? term.trim().substring(2, term.trim().length() - 1) : null;
    }

    /**
     * ***************************************************************
     * Does WordNet contain the given word.
     */
    public boolean containsWord(String word, int pos) {

        System.out.println("INFO in WordNet.containsWord: Checking word : " + word);
        if (pos == NOUN && nounSynsetHash.containsKey(word)) {
            return true;
        }
        if (pos == VERB && verbSynsetHash.containsKey(word)) {
            return true;
        }
        if (pos == ADJECTIVE && adjectiveSynsetHash.containsKey(word)) {
            return true;
        }
        return pos == ADVERB && adverbSynsetHash.containsKey(word);
    }

    /**
     * ***************************************************************
     * This is the regular point of entry for this class. It takes the word the
     * user is searching for, and the part of speech index, does the search, and
     * returns the string with HTML formatting codes to present to the user. The
     * part of speech codes must be the same as in the menu options in
     * WordNet.jsp and Browse.jsp
     *
     * @param inp The string the user is searching for.
     * @param pos The part of speech of the word 1=noun, 2=verb, 3=adjective,
     * 4=adverb
     * @return A string contained the HTML formatted search result.
     */
    public String page(String inp, int pos, String sumokbname, String synset) {

        String input = inp;
        StringBuilder buf = new StringBuilder();

        String mixedCase = input;
        String[] s = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length; i++) {
            sb.append(s[i]);
            if ((i + 1) < s.length) {
                sb.append('_');
            }
        }

        input = sb.toString().toLowerCase();
        if (pos == NOUN) {
            buf.append(processNoun(sumokbname, mixedCase, input, synset));
        }
        if (pos == VERB) {
            buf.append(processVerb(sumokbname, mixedCase, input, synset));
        }
        if (pos == ADJECTIVE) {
            buf.append(processAdjective(sumokbname, mixedCase, input, synset));
        }
        if (pos == ADVERB) {
            buf.append(processAdverb(sumokbname, mixedCase, input, synset));
        }
        buf.append('\n');

        return buf.toString();
    }

    /**
     * ***************************************************************
     * @param synset is a synset with POS-prefix
     */
    public String displaySynset(String sumokbname, String synset) {

        StringBuilder buf = new StringBuilder();
        char POS = synset.charAt(0);
        String gloss = "";
        String SUMOterm = "";
        String POSstring = "";
        String bareSynset = synset.substring(1);
        switch (POS) {
            case '1':
                gloss = (String) nounDocumentationHash.get(bareSynset);
                SUMOterm = (String) nounSUMOHash.get(bareSynset);
                POSstring = "Noun";
                break;
            case '2':
                gloss = (String) verbDocumentationHash.get(bareSynset);
                SUMOterm = (String) verbSUMOHash.get(bareSynset);
                POSstring = "Verb";
                break;
            case '3':
                gloss = (String) adjectiveDocumentationHash.get(bareSynset);
                SUMOterm = (String) adjectiveSUMOHash.get(bareSynset);
                POSstring = "Adjective";
                break;
            case '4':
                gloss = (String) adverbDocumentationHash.get(bareSynset);
                SUMOterm = (String) adverbSUMOHash.get(bareSynset);
                POSstring = "Adverb";
                break;
        }
        if (gloss == null) {
            return (synset + " is not a valid synset number.<P>\n");
        }
        buf.append("<b>").append(POSstring).append(" Synset:</b> ").append(synset);

        if (SUMOterm != null && !SUMOterm.isEmpty()) {
            
            buf.append(SUMOterm).append("  ").append(sumokbname);
        }

        TreeSet words = new TreeSet();
        ArrayList al = (ArrayList) synsetsToWords.get(synset);
        if (al != null) {
            words.addAll(al);
        }
        buf.append(" <b>Words:</b> ");
        Iterator it = words.iterator();
        while (it.hasNext()) {
            String word = (String) it.next();
            buf.append(word);
            if (it.hasNext()) {
                buf.append(", ");
            }
        }
        buf.append("<P>\n <b>Gloss:</b> ").append(gloss);
        buf.append("<P>\n");
        al = (ArrayList) relations.get(synset);
        if (al != null) {
            it = al.iterator();
            while (it.hasNext()) {
                AVPair avp = (AVPair) it.next();
                buf.append(avp.attribute).append(' ');
                buf.append("<a href=\"WordNet.jsp?synset=").append(avp.value).append("\">").append(avp.value).append("</a> - ");
                words = new TreeSet();
                Collection al2 = (ArrayList) synsetsToWords.get(avp.value);
                if (al2 != null) {
                    words.addAll(al2);
                }
                Iterator it2 = words.iterator();
                while (it2.hasNext()) {
                    String word = (String) it2.next();
                    buf.append(word);
                    if (it2.hasNext()) {
                        buf.append(", ");
                    }
                }
                buf.append("<br>\n");
            }
            buf.append("<P>\n");
        }
        return buf.toString();
    }

    /**
     * *************************************************************
     */
    private static boolean arrayContains(int[] ar, int value) {

        
        for (int anAr : ar) {
            if (anAr == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * *************************************************************
     * Frame transitivity intransitive - 1,2,3,4,7,23,35 transitive - everything
     * else ditransitive - 15,16,17,18,19
     */
    private String getTransitivity(String synset, String word) {

        
        int[] intrans = {1, 2, 3, 4, 7, 23, 35};
        int[] ditrans = {15, 16, 17, 18, 19};
        String intransitive = "no";
        String transitive = "no";
        String ditransitive = "no";
        List frames = new ArrayList();
        ArrayList res = (ArrayList) verbFrames.get(synset);
        if (res != null) {
            frames.addAll(res);
        }
        res = (ArrayList) verbFrames.get(synset + '-' + word);
        if (res != null) {
            frames.addAll(res);
        }
        for (Object frame : frames) {
            int value = Integer.parseInt((String) frame);
            if (arrayContains(intrans, value)) {
                intransitive = "intransitive";
            } else if (arrayContains(ditrans, value)) {
                ditransitive = "ditransitive";
            } else {
                transitive = "transitive";
            }
        }

        return '[' + intransitive + ',' + transitive + ',' + ditransitive + ']';
    }

    /**
     * *************************************************************
     * Replace underscores with commas, wrap hyphenatid and apostrophed words in
     * single quotes, and wrap the whole phrase in brackets.
     */
    @SuppressWarnings("HardcodedFileSeparator")
    private static String processMultiWord(String word) {

        word = word.replace('_', ',');
        word = word.replace("'", "\\'");
        String[] words = word.split(",");
        word = "";
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()
                    && (words[i].indexOf('-') > -1 || (words[i].indexOf('.') > -1)
                    || (words[i].contains("\\'")) || Character.isUpperCase(words[i].charAt(0)) || Character.isDigit(words[i].charAt(0)))) {
                words[i] = '\'' + words[i] + '\'';
            }
            word += words[i];
            if (i < words.length - 1) {
                word += ",";
            }
        }
        return '[' + word + ']';
    }

    /**
     * *************************************************************
     * verb_in_lexicon(Verb for singular mode, Verb for plural mode,
     * {transitive, intransitive, [intransitive, transitive, ditransitive], [no,
     * no, ditransitive], [no, transitive, no], [intransitive, no, no], [no,
     * transitive, ditransitive], [intransitive, transitive, no], [no, no, no],
     * [intransitive, no, ditransitive]}, singular, {simple, prepositional,
     * compound, phrasal}, {event, state}, SUMOMapping., Synset_ID).
     */
    @SuppressWarnings("HardcodedFileSeparator")
    private void writeVerbsProlog(PrintWriter pw, KB kb) {

        for (Object o : verbSynsetHash.keySet()) {
            String word = (String) o;
            String compound = "simple";
            if (word.indexOf('_') > -1) {
                compound = "compound";
            }

            String stringSynsets = (String) verbSynsetHash.get(word);
            String plural = WordNetUtilities.verbPlural(word);
            if (word.indexOf('_') > -1) {
                word = processMultiWord(word);
                plural = processMultiWord(plural);

            } else {
                word = word.replace("'", "\\'");
                if (word.indexOf('-') > -1 || (word.indexOf('.') > -1)
                        || (word.contains("\\'")) || Character.isUpperCase(word.charAt(0)) || Character.isDigit(word.charAt(0))) {
                    word = '\'' + word + '\'';
                    plural = '\'' + plural + '\'';
                }
            }
            String[] synsetList = splitSynsets(stringSynsets);
            Iterator it2 = verbSUMOHash.keySet().iterator();
            for (String synset : synsetList) {
                String sumoTerm = (String) verbSUMOHash.get(synset);
                if (sumoTerm != null && !sumoTerm.isEmpty()) {
                    String bareSumoTerm = WordNetUtilities.getBareSUMOTerm(sumoTerm);
                    String transitivity = getTransitivity(synset, word);
                    String eventstate = "state";
                    if (kb.childOf(bareSumoTerm, "Process")) {
                        eventstate = "event";
                    }
                    pw.println("verb_in_lexicon(" + plural + ',' + word + ',' + transitivity
                            + ", singular, " + compound + ", " + eventstate + ", '" + bareSumoTerm + "',2"
                            + synset + ").");
                }
            }
        }
    }

    /**
     * *************************************************************
     * adjective_in_lexicon(Adj, CELT_form, {normal, two_place}, {positive,
     * ungraded, comparative, superlative}, SUMOMapping).
     */
    @SuppressWarnings("HardcodedFileSeparator")
    private void writeAdjectivesProlog(PrintWriter pw, KB kb) {

        for (Object o : adjectiveSynsetHash.keySet()) {
            String word = (String) o;
            String compound = "simple";
            if (word.indexOf('_') > -1) {
                compound = "compound";
            }

            String stringSynsets = (String) adjectiveSynsetHash.get(word);
            if (word.indexOf('_') > -1) {
                word = processMultiWord(word);

            } else {
                word = word.replace("'", "\\'");
                if (word.indexOf('-') > -1 || (word.indexOf('.') > -1)
                        || (word.contains("\\'")) || Character.isUpperCase(word.charAt(0)) || Character.isDigit(word.charAt(0))) {
                    word = '\'' + word + '\'';
                }
            }
            String[] synsetList = splitSynsets(stringSynsets);
            Iterator it2 = adjectiveSUMOHash.keySet().iterator();
            for (String synset : synsetList) {
                String sumoTerm = (String) adjectiveSUMOHash.get(synset);
                if (sumoTerm != null && !sumoTerm.isEmpty()) {
                    String bareSumoTerm = WordNetUtilities.getBareSUMOTerm(sumoTerm);
                    pw.println("adjective_in_lexicon(" + word + ',' + word + ",normal,positive,"
                            + bareSumoTerm + ").");
                }
            }
        }
    }

    /**
     * *************************************************************
     * adverb_in_lexicon(Adv, {location, direction, time, duration, frequency,
     * manner}, SUMOMapping).
     */
    @SuppressWarnings("HardcodedFileSeparator")
    private void writeAdverbsProlog(PrintWriter pw, KB kb) {

        for (Object o : verbSynsetHash.keySet()) {
            String word = (String) o;
            String compound = "simple";
            if (word.indexOf('_') > -1) {
                compound = "compound";
            }

            String stringSynsets = (String) verbSynsetHash.get(word);
            if (word.indexOf('_') > -1) {
                word = processMultiWord(word);

            } else {
                word = word.replace("'", "\\'");
                if (word.indexOf('-') > -1 || (word.indexOf('.') > -1)
                        || (word.contains("\\'")) || Character.isUpperCase(word.charAt(0)) || Character.isDigit(word.charAt(0))) {
                    word = '\'' + word + '\'';
                }
            }
            String[] synsetList = splitSynsets(stringSynsets);
            Iterator it2 = verbSUMOHash.keySet().iterator();
            for (String synset : synsetList) {
                String sumoTerm = (String) verbSUMOHash.get(synset);
                if (sumoTerm != null && !sumoTerm.isEmpty()) {
                    String bareSumoTerm = WordNetUtilities.getBareSUMOTerm(sumoTerm);
                    pw.println("adverb_in_lexicon(" + word + ",null," + bareSumoTerm + ").");
                }
            }
        }
    }

    /**
     * *************************************************************
     * noun_in_lexicon(Noun,{object, person, time}, neuter, {count, mass},
     * singular, SUMOMapping, Synset_ID).
     */
    @SuppressWarnings("HardcodedFileSeparator")
    private void writeNounsProlog(PrintWriter pw, KB kb) {

        for (Object o : nounSynsetHash.keySet()) {
            String word = (String) o;
            String stringSynsets = (String) nounSynsetHash.get(word);
            boolean uppercase = false;
            if (Character.isUpperCase(word.charAt(0))) {
                uppercase = true;
            }
            if (word.indexOf('_') > -1) {
                word = processMultiWord(word);
            } else {
                word = word.replace("'", "\\'");
                if (word.indexOf('-') > -1 || (word.indexOf('.') > -1)
                        || (word.contains("\\'")) || Character.isUpperCase(word.charAt(0)) || Character.isDigit(word.charAt(0))) {
                    word = '\'' + word + '\'';
                }
            }
            String[] synsetList = splitSynsets(stringSynsets);
            for (String synset : synsetList) {
                String sumoTerm = (String) nounSUMOHash.get(synset);
                if (sumoTerm != null && !sumoTerm.isEmpty()) {
                    String bareSumoTerm = WordNetUtilities.getBareSUMOTerm(sumoTerm);
                    char mapping = WordNetUtilities.getSUMOMappingSuffix(sumoTerm);
                    String type = "object";
                    if (kb.childOf(bareSumoTerm, "Human") || kb.childOf(bareSumoTerm, "SocialRole")) {
                        type = "person";
                    }
                    if (kb.childOf(bareSumoTerm, "TimePosition") || kb.childOf(bareSumoTerm, "Process")) {
                        type = "time";
                    }
                    String countOrMass = "count";
                    if (kb.childOf(bareSumoTerm, "Substance")) {
                        countOrMass = "mass";
                    }
                    boolean instance = false;
                    if (uppercase && mapping == '@') {
                        instance = true;
                    }
                    if (mapping == '=') {
                        ArrayList al;
                        al = kb.instancesOf(bareSumoTerm);
                        if (!al.isEmpty()) {
                            instance = true;
                        }
                    }
                    if (instance && uppercase) {
                        ArrayList al = kb.askWithRestriction(1, bareSumoTerm, 0, "instance");
                        String parentTerm;
                        parentTerm = al != null && !al.isEmpty() ? ((Formula) al.get(0)).getArgument(2) : bareSumoTerm;
                        pw.println("proper_noun_in_lexicon(" + word + ',' + type + ", neuter, singular, '"
                                + parentTerm + "','" + bareSumoTerm + "',1" + synset + ").");
                    } else {
                        pw.println("noun_in_lexicon(" + word + ',' + type + ", neuter, "
                                + countOrMass + ", singular, '" + bareSumoTerm + "',1"
                                + synset + ").");
                    }
                }
            }
        }
    }

    /**
     * ***************************************************************
     */
    public void writeProlog(KB kb) {

        String dir = WordNet.baseDir;
        String fname = "WordNet.pl";

        try (FileWriter fw = new FileWriter(dir + File.separator + fname); PrintWriter pw = new PrintWriter(fw)) {
            writeNounsProlog(pw, kb);
            writeVerbsProlog(pw, kb);
            writeAdjectivesProlog(pw, kb);
            writeAdverbsProlog(pw, kb);
        } catch (Exception e) {
            System.out.println("Error writing file " + dir + File.separator + fname + '\n' + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     */
    private static String senseKeyPOS(String senseKey) {

        if (senseKey == null || senseKey.isEmpty()) {
            return "";
        }
        int underscore2 = senseKey.lastIndexOf('_');
        if (underscore2 < 0) {
            return "";
        }
        int underscore1 = senseKey.lastIndexOf('_', underscore2 - 1);
        if (underscore1 < 0) {
            return "";
        }
        return senseKey.substring(underscore1 + 1, underscore2);
    }

    /**
     * ***************************************************************
     */
    private static String senseKeySenseNum(String senseKey) {

        if (senseKey == null) {
            return "";
        }

        int underscore2 = senseKey.lastIndexOf('_');
        if (underscore2 < 0) {
            return "";
        }
        int underscore1 = senseKey.lastIndexOf('_', underscore2 - 1);
        if (underscore1 < 0) {
            return "";
        }
        return senseKey.substring(underscore2 + 1);
    }

    /**
     * ***************************************************************
     * Find the "word number" of a word and synset, which is its place in the
     * list of words belonging to a given synset. Return -1 if not found.
     */
    private int findWordNum(String POS, String synset, String word) {

        ArrayList al = (ArrayList) synsetsToWords.get(POS + synset);
        if (al == null || al.size() < 1) {
            System.out.println("Error in WordNet.findWordNum(): No words found for synset: " + POS + synset + " and word " + word);
            return -1;
        }
        for (int i = 0; i < al.size(); i++) {
            String storedWord = (String) al.get(i);
            if (word.equalsIgnoreCase(storedWord)) {
                return i + 1;
            }
        }
        System.out.println("Error in WordNet.findWordNum(): No match found for synset: " + POS + synset + " and word " + word);
        System.out.println(al);
        return -1;
    }

    /**
     * ***************************************************************
     */
    @SuppressWarnings("HardcodedFileSeparator")
    private static String processWordForProlog(String word) {

        String result = word;
        int start = 0;
        while (result.indexOf('\'', start) > -1) {
            int i;
            i = result.indexOf('\'', start);
            
            result = i == 0 ? "''" + result.substring(i + 1) : result.substring(0, i) + "\\'" + result.substring(i + 1);
            start = i + 2;
        }
        return result;
    }

    /**
     * ***************************************************************
     * Write WordNet data to a prolog file with a single kind of clause in the
     * following format: s(Synset_ID, Word_No_in_the_Synset, Word, SS_Type,
     * Synset_Rank_By_the_Word,Tag_Count)
     */
    public void writeWordNetS() {

        String dir = WordNet.baseDir;
        String fname = "Wn_s.pl";

        try (FileWriter fw = new FileWriter(dir + File.separator + fname); PrintWriter pw = new PrintWriter(fw)) {
            if (wordsToSenses.keySet().size() < 1) {
                System.out.println("Error in WordNet.writeWordNetS(): No contents in sense index");
            }
            for (Object o : wordsToSenses.keySet()) {
                String word = (String) o;
                String processedWord = processWordForProlog(word);
                List keys = (ArrayList) wordsToSenses.get(word);
                Iterator it2 = keys.iterator();
                if (keys.size() < 1) {
                    System.out.println("Error in WordNet.writeWordNetS(): No synsets for word: " + word);
                }
                while (it2.hasNext()) {
                    String senseKey = (String) it2.next();

                    String POS = senseKeyPOS(senseKey);
                    String senseNum = senseKeySenseNum(senseKey);
                    if (POS != null && POS.isEmpty() || senseNum != null && senseNum.isEmpty()) {
                        System.out.println("Error in WordNet.writeWordNetS(): Bad sense key: " + senseKey);
                    }
                    POS = WordNetUtilities.posLettersToNumber(POS);
                    String POSchar = Character.toString(WordNetUtilities.posNumberToLetter(POS.charAt(0)));
                    String synset = (String) senseIndex.get(senseKey);
                    int wordNum = findWordNum(POS, synset, word);
                    pw.println("s(" + POS + synset + ',' + wordNum + ",'" + processedWord + "'," + POSchar + ',' + senseNum + ",1).");
                }
            }
        } catch (Exception e) {
            System.out.println("Error writing file " + dir + File.separator + fname + '\n' + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     */
    public void writeWordNetHyp() {

        String dir = WordNet.baseDir;
        String fname = "Wn_hyp.pl";

        try (FileWriter fw = new FileWriter(dir + File.separator + fname); PrintWriter pw = new PrintWriter(fw)) {

            if (relations.keySet().size() < 1) {
                System.out.println("Error in WordNet.writeWordNetHyp(): No contents in relations");
            }
            for (Object o : relations.keySet()) {
                String synset = (String) o;


                List rels = (ArrayList) relations.get(synset);
                if (rels == null || rels.size() < 1) {
                    System.out.println("Error in WordNet.writeWordNetHyp(): No contents in rels for synset: " + synset);
                }

                if (rels != null) {
                    for (Object rel1 : rels) {
                        AVPair rel = (AVPair) rel1;
                        if ("hypernym".equals(rel.attribute)) {
                            pw.println("hyp(" + synset + ',' + rel.value + ").");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error writing file " + dir + File.separator + fname + '\n' + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     * Double any single quotes that appear.
     */
    public static String processPrologString(String doc) {

        int start = 0;
        while (doc.indexOf('\'', start) > -1) {
            int i;
            i = doc.indexOf('\'', start);
            
            doc = i == 0 ? "''" + doc.substring(i + 1) : doc.substring(0, i) + "''" + doc.substring(i + 1);
            start = i + 2;
        }
        return doc;
    }

    /**
     * ***************************************************************
     */
    public void writeWordNetG() {

        String dir = WordNet.baseDir;
        String fname = "Wn_g.pl";

        try (FileWriter fw = new FileWriter(dir + File.separator + fname); PrintWriter pw = new PrintWriter(fw)) {
            Iterator it = nounDocumentationHash.keySet().iterator();
            while (it.hasNext()) {
                String synset = (String) it.next();
                String doc = (String) nounDocumentationHash.get(synset);
                doc = processPrologString(doc);
                pw.println("g(" + '1' + synset + ",'(" + doc + ")').");
            }
            it = verbDocumentationHash.keySet().iterator();
            while (it.hasNext()) {
                String synset = (String) it.next();
                String doc = (String) verbDocumentationHash.get(synset);
                doc = processPrologString(doc);
                pw.println("g(" + '2' + synset + ",'(" + doc + ")').");
            }
            it = adjectiveDocumentationHash.keySet().iterator();
            while (it.hasNext()) {
                String synset = (String) it.next();
                String doc = (String) adjectiveDocumentationHash.get(synset);
                doc = processPrologString(doc);
                pw.println("g(" + '3' + synset + ",'(" + doc + ")').");
            }
            it = adverbDocumentationHash.keySet().iterator();
            while (it.hasNext()) {
                String synset = (String) it.next();
                String doc = (String) adverbDocumentationHash.get(synset);
                doc = processPrologString(doc);
                pw.println("g(" + '4' + synset + ",'(" + doc + ")').");
            }
        } catch (Exception e) {
            System.out.println("Error writing file " + dir + File.separator + fname + '\n' + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     */
    public void writeWordNetProlog() {

        writeWordNetS();
        writeWordNetHyp();
        writeWordNetG();
    }

    /**
     * ***************************************************************
     */
    public static void computeSentenceTerms() {

        System.out.println("INFO in WordNet.computeSentenceTerms(): computing terms");

        File msgFile;
        String canonicalPath = "";
        try {
            msgFile = getWnFile("messages");
            if (msgFile == null) {
                System.out.println("INFO in WordNet.computeSentenceTerms(): "
                        + "The messages file does not exist");
                return;
            }
            canonicalPath = msgFile.getCanonicalPath();
            FileReader r = new FileReader(msgFile);
            LineNumberReader lr = new LineNumberReader(r);
            String line;
            while ((line = lr.readLine()) != null) {
                String result = WordNet.wn.collectSUMOWordSenses(line);
                System.out.println(line);
                System.out.println(result);
                System.out.println();
            }
        } catch (Exception ioe) {
            System.out.println("Error in WordNet.computeSentenceTerms() reading "
                    + canonicalPath
                    + ": "
                    + ioe.getMessage());
            ioe.printStackTrace();
        }
    }

    /**
     * ***************************************************************
     * A main method, used only for testing. It should not be called during
     * normal operation.
     */
    public static void main(String[] args) {

        try {
            
            WordNet.initOnce();
            

            
            
            
            
            computeSentenceTerms();
        } catch (Exception e) {
            System.out.println("Error in WordNet.main():" + e.getMessage());
        }

        
        
        
    }

}
