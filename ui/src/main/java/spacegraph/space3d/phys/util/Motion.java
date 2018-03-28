//package spacegraph.space3d.phys.util;
//
//import spacegraph.space3d.phys.math.MotionState;
//import spacegraph.space3d.phys.math.Transform;
//import spacegraph.util.math.v3;
//
//
//public class Motion extends MotionState {
//
//    public final Transform t = new Transform();
//
//    public final Transform centerOfMassOffset;
//
//    public Motion() {
//        this.t.setIdentity();
//        this.centerOfMassOffset = null;
//    }
//
//    public Motion(Transform startTrans) {
//        this.t.set(startTrans);
//        this.centerOfMassOffset = null;
//    }
//
//    public Motion(Transform startTrans, Transform centerOfMassOffset) {
//        this.t.set(startTrans);
//        this.centerOfMassOffset = new Transform();
//        this.centerOfMassOffset.set(centerOfMassOffset);
//    }
//
//    @Override
//    public Transform getWorldTransform(Transform out) {
//        Transform w = this.t;
//        Transform c = this.centerOfMassOffset;
//        if (c !=null) {
//            out.inverse(c);
//            out.mul(w);
//        } else {
//            out.set(w);
//        }
//        return out;
//    }
//
//    @Override
//    public void setWorldTransform(Transform centerOfMassWorldTrans) {
//        Transform w = this.t;
//        w.set(centerOfMassWorldTrans);
//        Transform c = this.centerOfMassOffset;
//        if (c != null) {
//            w.mul(c);
//        }
//    }
//
//    public void center(v3 v) {
//        t.set(v);
//    }
//}
