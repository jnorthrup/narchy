/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package jcog.io;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.compare;
import static java.lang.System.in;
import static java.lang.System.out;
import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static java.util.regex.Pattern.compile;

/**
 * CUSTOMIZED FROM:
 * https:
 * <p/>
 * Twokenize -- a tokenizer designed for Twitter text in English and some other
 * European languages. This is the Java version. If you want the old Python
 * version, see: http:
 * <p/>
 * This tokenizer code has gone through a long history:
 * <p/>
 * (1) Brendan O'Connor wrote original version in Python,
 * http:
 * Topic Summarization for Twitter. Brendan O'Connor, Michel Krieger, and David
 * Ahn. ICWSM-2010 (demo track),
 * http:
 * Gimpel and Daniel Mills modified it for POS tagging for the CMU ARK Twitter
 * POS Tagger (2b) Jason Baldridge and David Snyder ported it to Scala (3)
 * Brendan bugfixed the Scala port and merged with POS-specific changes for the
 * CMU ARK Twitter POS Tagger (4) Tobi Owoputi ported it back to Java and added
 * many improvements (2012-06)
 * <p/>
 * Current home is http:
 * http:
 * <p/>
 * There have been at least 2 other Java ports, but they are not in the lineage
 * for the code here.
 */
public enum Twokenize {
    ;

    static Pattern Contractions = compile("(?i)(\\w+)(n['’′]t|['’′]ve|['’′]ll|['’′]d|['’′]re|['’′]s|['’′]m)$");
    static Pattern Whitespace = compile("[\\s\\p{Zs}]+");

    static final String punctChars = "['\"“”‘’.?!…,:;]";
    
    static String punctSeq = "['\"“”‘’]+|[.?!,…]+|[:;]+";    
    static final String entity = "&(?:amp|lt|gt|quot);";
    

    
    
    // BTO 2012-06: everyone thinks the daringfireball regex should be better, but they're wrong.
    // If you actually empirically test it the results are bad.
    // Please see https://github.com/brendano/ark-tweet-nlp/pull/9
    static final String urlStart1 = "(?:https?://|\\bwww\\.)";
    static final String commonTLDs = "(?:com|org|edu|gov|net|mil|aero|asia|biz|cat|coop|info|int|jobs|mobi|museum|name|pro|tel|travel|xxx)";
    static final String ccTLDs = "(?:ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|ax|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|"
            + "bv|bw|by|bz|ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cs|cu|cv|cx|cy|cz|dd|de|dj|dk|dm|do|dz|ec|ee|eg|eh|"
            + "er|es|et|eu|fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|"
            + "hu|id|ie|il|im|in|io|iq|ir|is|it|je|jm|jo|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|"
            + "lu|lv|ly|ma|mc|md|me|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|na|nc|ne|nf|ng|ni|nl|no|np|"
            + "nr|nu|nz|om|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|ps|pt|pw|py|qa|re|ro|rs|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|sk|"
            + "sl|sm|sn|so|sr|ss|st|su|sv|sy|sz|tc|td|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|us|uy|uz|"
            + "va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|za|zm|zw)";    //TODO: remove obscure country domains?
    static final String urlStart2 = "\\b(?:[A-Za-z\\d-])+(?:\\.[A-Za-z0-9]+){0,3}\\." + "(?:" + commonTLDs + '|' + ccTLDs + ')' + "(?:\\." + ccTLDs + ")?(?=\\W|$)";
    static final String urlBody = "(?:[^.\\s<>][^\\s<>]*?)?";
    static final String urlExtraCrapBeforeEnd = "(?:" + punctChars + '|' + entity + ")+?";
    static final String urlEnd = "(?:\\.\\.+|[<>]|\\s|$)";
    public static String url = "(?:" + urlStart1 + '|' + urlStart2 + ')' + urlBody + "(?=(?:" + urlExtraCrapBeforeEnd + ")?" + urlEnd + ')';

    
    static String timeLike = "\\d+(?::\\d+){1,2}";
    static String numNum = "\\d+[.\\d+]";
    static String numberWithCommas = "(?:(?<!\\d)\\d{1,3},)+?\\d{3}" + "(?=(?:[^,\\d]|$))";
    static String numComb = "\\p{Sc}?\\d+(?:\\.\\d+)+%?";

    
    static final String boundaryNotDot = "(?:$|\\s|[“\\u0022?!,:;]|" + entity + ')';
    static final String aa1 = "(?:[A-Za-z]\\.){2,}(?=" + boundaryNotDot + ')';
    static final String aa2 = "[^A-Za-z](?:[A-Za-z]\\.){1,}[A-Za-z](?=" + boundaryNotDot + ')';
    static final String standardAbbreviations = "\\b(?:[Mm]r|[Mm]rs|[Mm]s|[Dd]r|[Ss]r|[Jj]r|[Rr]ep|[Ss]en|[Ss]t)\\.";
    static String arbitraryAbbrev = "(?:" + aa1 + '|' + aa2 + '|' + standardAbbreviations + ')';
    static String separators = "(?:--+|―|—|~|–|=)";
    static String decorations = "(?:[♫♪]+|[★☆]+|[♥❤♡]+|[\\u2639-\\u263b]+|[\\ue001-\\uebbb]+)";
    static String thingsThatSplitWords = "[^\\s\\.,?\"]";
    

    public static String OR(String... parts) {
        var prefix = "(?:";
        var sb = new StringBuilder();
        for (var s : parts) {
            sb.append(prefix);
            prefix = "|";
            sb.append(s);
        }
        sb.append(')');
        return sb.toString();
    }

    
    static final String normalEyes = "(?iu)[:=]"; 
    static final String wink = "[;]";
    static final String noseArea = "(?:|-|[^a-zA-Z0-9 ])"; 
    static final String happyMouths = "[D\\)\\]\\}]+";
    static final String sadMouths = "[\\(\\[\\{]+";
    static final String tongue = "[pPd3]+";
    static final String otherMouths = "(?:[oO]+|[/\\\\]+|[vV]+|[Ss]+|[|]+)"; 

    
    
    
    static final String bfLeft = "(♥|0|o|°|v|\\$|t|x|;|\\u0CA0|@|ʘ|•|・|◕|\\^|¬|\\*)";
    static final String bfCenter = "(?:[\\.]|[_-]+)";
    static final String bfRight = "\\2";
    static final String s3 = "(?:--['\"])";
    static final String s4 = "(?:<|&lt;|>|&gt;)[\\._-]+(?:<|&lt;|>|&gt;)";
    static final String s5 = "(?:[.][_]+[.])";
    static final String basicface = "(?:(?i)" + bfLeft + bfCenter + bfRight + ")|" + s3 + '|' + s4 + '|' + s5;

    static final String eeLeft = "[＼\\\\ƪԄ\\(（<>;ヽ\\-=~\\*]+";
    static final String eeRight = "[\\-=\\);'\\u0022<>ʃ）/／ノﾉ丿╯σっµ~\\*]+";
    static final String eeSymbol = "[^A-Za-z0-9\\s\\(\\)\\*:=-]";
    static final String eastEmote = eeLeft + "(?:" + basicface + '|' + eeSymbol + ")+" + eeRight;

    public static String emoticon = OR(
            
            "(?:>|&gt;)?" + OR(normalEyes, wink) + OR(noseArea, "[Oo]")
                    + OR(tongue + "(?=\\W|$|RT|rt|Rt)", otherMouths + "(?=\\W|$|RT|rt|Rt)", sadMouths, happyMouths),
            
            
            "(?<=(?: |^))" + OR(sadMouths, happyMouths, otherMouths) + noseArea + OR(normalEyes, wink) + "(?:<|&lt;)?",
            
            eastEmote.replaceFirst("2", "1"), basicface
            
            
    );

    static String Hearts = "(?:<+/?3+)+"; 

    static String Arrows = "(?:<*[-―—=]*>+|<+[-―—=]*>*)|\\p{InArrows}+";

    
    
    
    
    
    
    
    
    
    static String Hashtag = "#[a-zA-Z0-9_]+";  
    
    static String AtMention = "[@＠][a-zA-Z0-9_]+";

    
    
    
    static final String Bound = "(?:\\W|^|$)";
    public static String Email = "(?<=" + Bound + ")[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}(?=" + Bound + ')';

    public static final String word = "[\\p{Alpha}]+";

    static String embeddedApostrophe = word + "[''']" + word;

    
    
    static Map<String, Pattern> patterns = new HashMap() {
        {
            /*  Pattern.compile(
             OR(*/

            

            put("word", compile(word));
            put("hearticon", compile(Hearts));
            put("url", compile(url));
            put("email", compile(Email));
            put("temporal", compile(timeLike));
            put("num", compile(numNum));
            
            put("num", compile(numComb));
            put("emoticon", compile(emoticon));
            put("arrows", compile(Arrows));
            put("entity", compile(entity));
            put("punct", compile(punctSeq));
            put("abbrev", compile(arbitraryAbbrev));
            put("separator", compile(separators));
            put("decoration", compile(decorations));
            put("apostrophe", compile(embeddedApostrophe));
            put("hashtag", compile(Hashtag));
            put("mention", compile(AtMention));
        }
    };
    /*));
                            
     */

    
    
    
    
    
    
    
    
    static final String edgePunctChars = "'\"“”‘’«»{}\\(\\)\\[]\\*&";
    static final String edgePunct = '[' + edgePunctChars + ']';
    static final String notEdgePunct = "[a-zA-Z0-9]"; 
    static final String offEdge = "(^|$|:|;|\\s|\\.|,)";  
    static Pattern EdgePunctLeft = compile(offEdge + '(' + edgePunct + "+)(" + notEdgePunct + ')');
    static Pattern EdgePunctRight = compile('(' + notEdgePunct + ")(" + edgePunct + "+)" + offEdge);

    public static String splitEdgePunct(String input) {
        var m1 = EdgePunctLeft.matcher(input);
        input = m1.replaceAll("$1$2 $3");
        m1 = EdgePunctRight.matcher(input);
        input = m1.replaceAll("$1 $2$3");
        return input;
    }

    private static class Pair<T1, T2> {

        public T1 first;
        public T2 second;

        public Pair(T1 x, T2 y) {
            first = x;
            second = y;
        }

        @Override
        public String toString() {
            return "(" + first + ',' + second + ')';
        }

    }

    public static class Span implements Comparable<Span> {
        public final String content;
        public final String pattern;
        public final int start;
        public final int stop;
        public final int length;

        public Span(String content, String pattern, int start, int stop) {
            this.content = content;
            this.pattern = pattern;
            this.start = start;
            this.stop = stop;
            length = stop - start;
        }

        @Override
        public boolean equals(Object obj) {
            var t = (Span)obj;
            return start!=t.start && stop!=t.stop;
        }

        @Override
        public int compareTo(Span t) {
            return compare(start, t.start);
        }

        @Override
        public String toString() {
            return '(' + content + ',' + pattern + ')';
        }

        private boolean contains(Span b) {
            return (b.length < length) && (b.start >= start) && (b.stop <= stop);
        }
    }

    
    private static List<Span> simpleTokenize(String text) {


        var splitPunctText = splitEdgePunct(text);

        

        
        
        
        
        
        
        List<Span> spans = new ArrayList<>();    

        for (var p : patterns.entrySet()) {
            var matches = p.getValue().matcher(splitPunctText);
            while (matches.find()) {

                
                if (matches.start() != matches.end()) { 

                    
                    spans.add(
                            new Span(splitPunctText.substring(matches.start(), matches.end()), p.getKey(), matches.start(), matches.end()));
                    
                    
                }
            }
        }

        Collections.sort(spans);
        return spans;














































    }

    private static List<Pair<String, Object>> addAllnonempty(List<Pair<String, Object>> master, List<Pair<String, Object>> smaller) {
        for (var s : smaller) {
            var strim = s.first.trim();
            if (!strim.isEmpty()) {
                s.first = strim;
                master.add(s);
            }
        }
        return master;
    }

    /**
     * "foo bar " => "foo bar"
     */
    public static String squeezeWhitespace(String input) {
        return Whitespace.matcher(input).replaceAll(" ").trim();
    }

    
    private static List<String> splitToken(String token) {

        var m = Contractions.matcher(token);
        if (m.find()) {
            String[] contract = {m.group(1), m.group(2)};
            return asList(contract);
        }
        String[] contract = {token};
        return asList(contract);
    }

    /**
     * Assume 'text' has no HTML escaping. *
     */
    public static List<Span> tokenize(String text) {
        var l = simpleTokenize(squeezeWhitespace(text));

        Set<Span> hidden = new HashSet(l.size());

        for (var a : l) {
            if (hidden.contains(a)) continue;
            for (var b : l) {
                if (hidden.contains(b)) continue;
                if (a.contains(b))
                    hidden.add(b);
                else if (b.contains(a)) {
                    hidden.add(a);
                    break;
                }

            }
        }

        l.removeAll(hidden);
        return l;
    }

    /**
     * Twitter text comes HTML-escaped, so unescape it. We also first unescape
     * &amp;'s, in case the text has been buggily double-escaped.
     */
    public static String normalizeTextForTagger(String text) {
        text = text.replaceAll("&amp;", "&");
        
        return text;
    }

    /**
     * This is intended for raw tweet text -- we do some HTML entity unescaping
     * before running the tagger.
     * <p/>
     * This function normalizes the input text BEFORE calling the tokenizer. So
     * the tokens you get back may not exactly correspond to substrings of the
     * original text.
     */
    public static List<Span> twokenize(String text) {
        var sp = tokenize(normalizeTextForTagger(text));
        sort(sp);
        return sp;
    }

    /**
     * Tokenizes tweet texts on standard input, tokenizations on standard
     * output. Input and output UTF-8.
     */
    public static void main(String[] args) throws IOException {
        var input = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        var output = new PrintStream(out, true, StandardCharsets.UTF_8);
        String line;
        while ((line = input.readLine()) != null) {
            var toks = twokenize(line);
            for (var i = 0; i < toks.size(); i++) {
                output.print(toks.get(i));
                if (i < toks.size() - 1) {
                    output.print(" ");
                }
            }
            output.print("\n");
        }
    }

}
