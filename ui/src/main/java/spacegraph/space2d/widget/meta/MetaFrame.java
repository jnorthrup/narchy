package spacegraph.space2d.widget.meta;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Scale;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;

/**
 * a dynamic frame for attaching to widgets providing access to context menus, controls, and display
 */
public class MetaFrame extends Bordering {




    public MetaFrame(Surface surface) {
        super(surface);



//        Runnable zoomer = () -> surface.root().zoom(surface);


        Surface n =
                
                new VectorLabel(name(surface));
                


        borderWest = borderEast = 0;
        set(N, n);

//        Surface m = grid(
//                PushButton.awesome("tag"),
//                PushButton.awesome("sitemap")
//        );
//        set(E, m);

        PushButton hideButton = PushButton.awesome("times");
        set(NE, new Scale(hideButton, 0.8f));

        
        


        Surface wm = (surface instanceof Menu) ? ((Menu) surface).menu() : null;
        if (wm != null)
            set(S, wm);
        else
            borderSouth = 0;

    }

    protected String name(Surface widget) {
        return widget.toString();
    }

























    public void close() {
        
    }


    public interface Menu {
        Surface menu();
    }
}
