package nars.agent.util;

import nars.concept.action.ActionConcept;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.jetbrains.annotations.Nullable;

public class UnipolarMotor implements ActionConcept.MotorFunction {

    private final boolean freqOrExp;
    private final FloatToFloatFunction ifGoalMissing;
    private final FloatToFloatFunction update;
    private final FloatFloatToObjectFunction<Truth> truther;
    float lastF;

    public UnipolarMotor(boolean freqOrExp, FloatToFloatFunction ifGoalMissing, FloatToFloatFunction update, FloatFloatToObjectFunction<Truth> truther) {
        this.freqOrExp = freqOrExp;
        this.ifGoalMissing = ifGoalMissing;
        this.update = update;
        this.truther = truther;
        this.lastF = 0.5f;
    }

    @Override
    public @Nullable Truth apply(@Nullable Truth b, @Nullable Truth g) {
        float goal = (g != null) ?
                (freqOrExp ? g.freq() : g.expectation()) : ifGoalMissing.valueOf(lastF);

        lastF = goal;

        float feedbackFreq = (goal == goal) ? update.valueOf(goal) : Float.NaN;

        return feedbackFreq == feedbackFreq ? truther.value(feedbackFreq, g!=null ? g.conf() : 0) : null;
    }
}
