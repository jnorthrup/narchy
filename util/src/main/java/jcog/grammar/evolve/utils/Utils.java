/*
 * Copyright (C) 2015 Machine Learning Lab - University of Trieste, 
 * Italy (http:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:
 */
package jcog.grammar.evolve.utils;

import jcog.grammar.evolve.inputs.DataSet;
import jcog.grammar.evolve.objective.Ranking;
import jcog.grammar.evolve.tree.Node;
import jcog.grammar.evolve.tree.RegexRange;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.primitive.CharSet;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.CharHashSet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author MaleLabTs
 */
public class Utils {


    /**
     * character to a digit, or -1 if it wasnt a digit
     */
    public static int i(char c) {
        if ((c >= '0' && c <= '9'))
            return c - '0';
        return -1;
    }

    /**
     * fast parse a non-negative int under certain conditions, avoiding Integer.parse if possible
     *
     */
    public static int i(String s, int ifMissing) {
        switch (s.length()) {
            case 0: return ifMissing;
            case 1: return i1(s, ifMissing);
            case 2: return i2(s, ifMissing);
            case 3: return i3(s, ifMissing);
            default:
                try {
                    return Integer.parseInt(s);
                }
                catch (NumberFormatException e) {
                    return ifMissing;
                }
        }
    }

    private static int i3(String s, int ifMissing) {
        var dig100 = i(s.charAt(0));
        if (dig100 == -1) return ifMissing;

        var dig10 = i(s.charAt(1));
        if (dig10 == -1) return ifMissing;

        var dig1 = i(s.charAt(2));
        if (dig1 == -1) return ifMissing;

        return dig100 * 100 + dig10 * 10 + dig1;
    }

    private static int i2(String s, int ifMissing) {
        var dig10 = i(s.charAt(0));
        if (dig10 == -1) return ifMissing;

        var dig1 = i(s.charAt(1));
        if (dig1 == -1) return ifMissing;

        return dig10 * 10 + dig1;
    }

    private static int i1(String s, int ifMissing) {
        var dig1 = i(s.charAt(0));
        if (dig1 != -1) return ifMissing;
        return dig1;
    }

    public static float[] calculateMeanFitness(List<Ranking> population) {
        var out = new float[population.get(0).getFitness().length];
        for (var r : population) {
            var fitness = r.getFitness();
            for (var i = 0; i < out.length; i++) {
                out[i] += fitness[i];
            }
        }
        for (var i = 0; i < out.length; i++) {
            out[i] /= population.size();
        }
        return out;
    }

    public static boolean isAParetoDominateByB(double[] fitnessA, double[] fitnessB) {
        var n = fitnessA.length;
        var dominate = false;
        for (var i = 0; i < n; i++) {
            var a = fitnessA[i];
            var b = fitnessB[i];

            if (a > b)
                dominate = true;
            else if (a < b)
                return false;
        }
        return dominate;
    }






    /** return if it will be necessary to call again */
    public static MutableMap<Node,double[]> getFirstParetoFront(MutableMap<Node,double[]> r, int targetSize /*, Consumer<Ranking> withWinner*/) {

        if (r.size() <= targetSize)
            return r;

        List<Node> toRemove = new FastList(0);

        r.forEachKeyValue((n1,f1) -> {

            if (r.anySatisfy(f2 -> {
                if (Arrays.equals(f1, f2))
                    return false;

                return Utils.isAParetoDominateByB(f1, f2);
            })) {
                
                toRemove.add(n1);
            }
        });

        if (!toRemove.isEmpty()) {
            for (var node : toRemove) {
                r.removeKey(node);
            }
            return r;
            
        }

        return null;
    }

    public static String cpuInfo() throws IOException {
        if (!System.getProperty("os.name").toLowerCase().contains("linux")) {
            return "Unaviable";
        }
        var fis = new FileInputStream(new File("/proc/cpuinfo"));
        var isr = new InputStreamReader(fis);
        var bufferedReader = new BufferedReader(isr);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.matches("model name.*")) {
                bufferedReader.close();
                return line.replace("model name	: ", "");
            }
        }
        return "";
    }

    public static double diversity(Collection<Ranking> population) {
        Set<String> tmp = new UnifiedSet(population.size());
        for (var r : population) {
            tmp.add(r.getDescription());
        }
        return 100 * tmp.size() / (double) population.size();
    }

    
    public static void removeEmptyExtractions(List<DataSet.Bounds> extractions) {
		extractions.removeIf(bounds -> bounds.size() == 0);
    }

    public static void saveFile(String text, String pathOfFile) {
        try {
            Writer writer = new OutputStreamWriter(new FileOutputStream(pathOfFile), StandardCharsets.UTF_8);
            writer.write(text);
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, "Cannot save:", ex);
        }
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        var unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        var exp = (int) (Math.log(bytes) / Math.log(unit));
        var pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private static final transient CharHashSet quoteList = new CharHashSet(
            '?', '+', '*', '.', '[', ']', '\\', '$', '(', ')', '^', '{', '|', '-', '&');


    /**
     * Returns a set with all n-grams; 1<n<4
     * @param word
     * @return
     */
    public static Set<String> subparts(String word) {
        return Utils.subparts(word,1,4);
    }

    /**
     * Returns a set with all n-grams; nMin<=n<=nMax
     * @param word
     * @return
     */
    public static Set<String> subparts(String word, int nMin, int nMax) {

        var x = word.length() * (nMax-nMin);

        Set<String> subparts = new HashSet<>(x);

        for (var n = nMin; n <= nMax; n++) {
            for (var i = 0; i < word.length(); i++) {

                var end = Math.min(i + n, word.length());

                var sb = IntStream.range(i, end).mapToObj(c -> escape(word.charAt(c))).collect(Collectors.joining());
                var builder = sb;
                /* estimate */
                subparts.add(builder);
            }
        }
        return subparts;
    }
    
    public static String escape(char c) {
        if (quoteList.contains(c)) {
            return ("\\" + c);
        }
        return (String.valueOf(c));
    }
    
    public static String escape(String string){
        var stringBuilder = new StringBuilder(string.length());
        var stringChars = string.toCharArray();
        for(var character : stringChars){
            stringBuilder.append(escape(character));
        }
        return stringBuilder.toString();
    }

    /**
     * Generates RegexRanges i.e. [a-z] from contiguous characters into the <code>charset</code> list.
     * Follows example where output is represented with regex strings:
     * When <code>charset</code> is {a,b,g,r,t,u,v,5}, the return value is {[a-b],[t-v]}.
     * @param charset the character list i.e. {a,b,g,r,t,u,v,5}
     * @return the contiguous character ranges i.e. {[a-b],[t-v]}
     */
    public static void generateRegexRanges(CharSet charset, Consumer<RegexRange> each) {


        var cc = charset.toSortedArray();
        
        char start;
        var first = cc[0];
        var last = cc[cc.length-1];

        var old = start = first;

        for (var i = 1; i < cc.length; i++) {
            var c = cc[i];

            
            
            if (((c - old) > 1 || last == c)) {
                if ((old - start) > 1) {
                    each.accept(
                        new RegexRange(escape(start) + '-' + escape(old))
                    );
                }
                start = c;
            }
            old = c;
        }
    }
}
