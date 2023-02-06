package edu.ufl.cise.plcsp23;

import java.util.Arrays;

public class NumLitToken extends Token implements INumLitToken{

    public NumLitToken(Kind kind, int pos, int length, char[] source) {
        super(kind, pos, length, source);
    }

    @Override
    public int getValue() {
        return Integer.parseInt(Arrays.toString(source));
    }
}
