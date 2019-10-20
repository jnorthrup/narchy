/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package spacegraph.space2d.phys.common;

import com.jogamp.opengl.GL2;
import jcog.math.v2;


/**
 * A transform contains translation and rotation. It is used to represent the position and
 * orientation of rigid frames.
 */
public class Transform extends Rot {

    /**
     * The translation caused by the transform
     */
    public final v2 pos;

    /**
     * The default constructor.
     */
    public Transform() {
        pos = new v2();
    }


    /**
     * Set this to equal another transform.
     */
    public final Transform set(Transform xf) {
        pos.set(xf.pos);
        this.set((Rot) xf);
        return this;
    }

    /**
     * Set this based on the position and angle.
     *
     * @param p
     * @param angle
     */
    public final void set(v2 p, float angle) {
        this.pos.set(p);
        this.set(angle);
    }

    /**
     * Set this to the identity transform.
     */
    public final void setIdentity() {
        pos.setZero();
        super.setIdentity();
    }

    public static v2 mul(Transform T, v2 v) {
        return new v2((T.c * v.x - T.s * v.y) + T.pos.x, (T.s * v.x + T.c * v.y) + T.pos.y);
    }

    public static void mulToOut(Transform T, v2 v, v2 out) {
        float ts = T.s;
        float vx = v.x;
        float vy = v.y;
        float tc = T.c;
        float tempy = (ts * vx + tc * vy) + T.pos.y;
        out.x = (tc * vx - ts * vy) + T.pos.x;
        out.y = tempy;
    }

    public static void mulToOutUnsafe(Transform T, v2 v, v2 out) {
        assert (v != out);
        Rot tq = T;
        out.x = (tq.c * v.x - tq.s * v.y) + T.pos.x;
        out.y = (tq.s * v.x + tq.c * v.y) + T.pos.y;
    }
    public static void mulToOutUnsafe(Transform T, v2 v, float scale, v2 out) {
        assert (v != out);
        float vy = v.y * scale;
        float vx = v.x * scale;
        Rot tq = T;
        v2 pos = T.pos;
        float tqs = tq.s;
        float tqc = tq.c;
        out.x = (tqc * vx - tqs * vy) + pos.x;
        out.y = (tqs * vx + tqc * vy) + pos.y;
    }

    public static void mulToOutUnsafe(Transform T, v2 v, float scale, GL2 gl) {
        float vy = v.y;
        float vx = v.x;
        Rot tq = T;
        v2 pos = T.pos;
        float tqs = tq.s;
        float tqc = tq.c;
        gl.glVertex2f(
                ((tqc * vx - tqs * vy) + pos.x)*scale,
                ((tqs * vx + tqc * vy) + pos.y)*scale
        );

    }

    public static v2 mulTrans(Transform T, v2 v) {
        float px = v.x - T.pos.x;
        float py = v.y - T.pos.y;
        float y = (-T.s * px + T.c * py);
        return new v2((T.c * px + T.s * py), y);
    }

    public static void mulTransToOut(Transform T, v2 v, v2 out) {
        float px = v.x - T.pos.x;
        float py = v.y - T.pos.y;
        float tempy = (-T.s * px + T.c * py);
        out.x = (T.c * px + T.s * py);
        out.y = tempy;
    }

    public static void mulTransToOutUnsafe(Transform T, v2 v, v2 out) {
        assert (v != out);
        float px = v.x - T.pos.x;
        float py = v.y - T.pos.y;
        out.x = (T.c * px + T.s * py);
        out.y = (-T.s * px + T.c * py);
    }

    public static Transform mul(Transform A, Transform B) {
        Transform C = new Transform();
        mulUnsafe(A, B, C);
        Rot.mulToOutUnsafe(A, B.pos, C.pos);
        C.pos.added(A.pos);
        return C;
    }

    public static void mulToOut(Transform A, Transform B, Transform out) {
        assert (out != A);
        mul(A, B, out);
        Rot.mulToOut(A, B.pos, out.pos);
        out.pos.added(A.pos);
    }

    public static void mulToOutUnsafe(Transform A, Transform B, Transform out) {
        assert (out != B);
        assert (out != A);
        mulUnsafe(A, B, out);
        Rot.mulToOutUnsafe(A, B.pos, out.pos);
        out.pos.added(A.pos);
    }


















    public static void mulTransToOutUnsafe(Transform A, Transform B,
                                           Transform out) {
        assert (out != A);
        assert (out != B);
        mulTransUnsafe(A, B, out);
        v2 pool = new v2();
        pool.set(B.pos).subbed(A.pos);
        mulTransUnsafe(A, pool, out.pos);
    }

    @Override
    public final String toString() {
        String s = "XForm:\n";
        s += "Position: " + pos + '\n';
        s += "R: \n" + super.toString() + '\n';
        return s;
    }
}
