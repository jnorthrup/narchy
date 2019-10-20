package com.jujutsu.tsne.matrix;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MatrixUtils {
    private static final Pattern LINE = Pattern.compile("\\s*");

    public static double[][] simpleRead2DMatrix(File file) {
        return simpleRead2DMatrix(file, " ");
    }

    public static double[][] simpleRead2DMatrix(File file, String columnDelimiter) {
        try (var fr = new FileReader(file)) {
            var m = simpleRead2DMatrix(fr, columnDelimiter);
            fr.close();
            return m;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static double[][] simpleRead2DMatrix(Reader r, String columnDelimiter) throws IOException {


        Collection<double[]> rows = new ArrayList<>();

        {
            var b = new BufferedReader(r);
            String line;
            while ((line = b.readLine()) != null && !LINE.matcher(line).matches()) {
                var cols = line.trim().split(columnDelimiter);
                var row = new double[cols.length];
                for (var j = 0; j < cols.length; j++) {
                    if (!(cols[j].isEmpty())) {
                        row[j] = Double.parseDouble(cols[j].trim());
                    }
                }
                rows.add(row);
            }

        }

        var array = new double[rows.size()][];
        var currentRow = 0;
        for (var ds : rows) {
            array[currentRow++] = ds;
        }

        return array;
    }

    public static String[] simpleReadLines(File file) {
        Collection<String> rows;

        try (var fr = new FileReader(file)) {
            try (var b = new BufferedReader(fr)) {
                rows = b.lines().map(String::trim).collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        var lines = new String[rows.size()];
        var currentRow = 0;
        for (var line : rows) {
            lines[currentRow++] = line;
        }

        return lines;
    }
}
