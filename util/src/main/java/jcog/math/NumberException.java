package jcog.math;


public final class NumberException extends RuntimeException {


    private final Number value;

    public NumberException(String message, Number value) {
        super(message);
        this.value = value;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + value;
    }
}
