package spacegraph.video;

import jcog.event.Off;
import spacegraph.space2d.container.unit.AspectAlign;

public class VideoSurface extends AspectAlign {


    private final Tex tex;
    protected final VideoSource in;
    private Off on;

    public VideoSurface(VideoSource in) {
        this(in, new Tex());
    }

    public VideoSurface(VideoSource in, Tex tex) {
        super(tex.view(), 1);
        this.tex = tex;
        this.in = in;
    }

    @Override
    protected void starting() {
        super.starting();
        on = in.tensor.on((x)-> {
            aspect(in.aspect());
            tex.set(in.image);
        });

//            on = in.eventChange.on(x -> {
//            WebcamEventType t = x.getType();
//            switch (t) {
//                case CLOSED:
//                case DISPOSED:
//                    //TODO display close status, close stream
//                    break;
//                case NEW_IMAGE:
//                    //((float) in.webcam.getViewSize().getHeight()) / ((float) in.webcam.getViewSize().getWidth())
//                    aspect(
//                            in.aspect()
//                    );
//                    ts.set(in.image);
//                    break;
//            }
//        });
    }

    @Override
    protected void stopping() {
        synchronized (in) {
            try {
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (on != null) {
                on.close();
                on = null;
            }
        }
        super.stopping();
    }


}
