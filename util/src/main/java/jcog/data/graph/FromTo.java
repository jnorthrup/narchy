package jcog.data.graph;

import org.jetbrains.annotations.Nullable;

public interface FromTo<F, X> /* extends Triple<F,X,F> */ {
    F from();
    X what();
    F to();

    default F to(boolean outOrIn) {
        return outOrIn ? to() : from();
    }

    default F from(boolean outOrIn) {
        return outOrIn ? from() : to();
    }

    @Nullable
    default F other(F x) {
        F f = from();
        F t = to();
        if (f == x) return t;
        else if (t == x) return f;
        else return null;
    }
//    default F other(F at) {
//        if (at.equals(from()))
//            return to();
//        else {
//            //assert(to().equals(at));
//            return from();
//        }
//    }
}
