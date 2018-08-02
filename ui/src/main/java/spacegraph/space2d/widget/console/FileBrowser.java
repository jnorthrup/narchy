package spacegraph.space2d.widget.console;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.dialogs.FileDialogBuilder;
import spacegraph.SpaceGraph;

public class FileBrowser extends ConsoleGUI {

    private FileBrowser(int cols, int rows) {
        super(cols, rows);
    }

    @Override
    protected void init(BasicWindow window) {
        
                window.setComponent(
                        new FileDialogBuilder()

                                .build().getComponent() );
                        
        

    }

    public static void main(String[] args) {
        SpaceGraph.window(new FileBrowser(50, 15), 500, 500);
    }
}
