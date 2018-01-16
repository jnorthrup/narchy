package spacegraph.widget.console;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.dialogs.FileDialogBuilder;
import spacegraph.SpaceGraph;

import java.util.function.Consumer;

public class FileBrowser extends ConsoleGUI {

    public FileBrowser(int cols, int rows) {
        super(cols, rows);
    }

    @Override
    protected void init(BasicWindow window) {
        //window.getTextGUI().getGUIThread().invokeLater(()->{
                window.setComponent(
                        new FileDialogBuilder()

                                .build().getComponent() );
                        //.showDialog(window.getTextGUI());
        //});

    }

    public static void main(String[] args) {
        SpaceGraph.window(new FileBrowser(50, 25), 500, 500);
    }
}
