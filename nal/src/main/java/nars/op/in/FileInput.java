package nars.op.in;

import com.google.common.io.Files;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
    Given a filepath, loads the file as task input
 */
public class FileInput {

    public static @NotNull Collection<Task> load(@NotNull NAR p, @NotNull File input) throws IOException, Narsese.NarseseException {
        List<Task> t = tasks(p, input);
        p.input(t);
        return t;
    }
    public static List<Task> tasks(@NotNull final NAR nar, @NotNull File input) throws IOException, Narsese.NarseseException {
        return Narsese.the().tasks(load(input), nar);
    }

    public static String load(@NotNull String path) throws IOException {
        return load(new File(path));
    }

    public static String load(@NotNull File file) throws IOException {
        return Files.toString(file, Charset.defaultCharset());
    }

    public static String load(@NotNull File file, Function<? super String, CharSequence> lineTransform) throws IOException {
        List<String> lines = Files.readLines(file, Charset.defaultCharset());
        return lines.stream().map(lineTransform).collect(Collectors.joining("\n"));
    }


}
