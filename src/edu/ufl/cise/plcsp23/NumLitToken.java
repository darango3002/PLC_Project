package edu.ufl.cise.plcsp23;

import java.util.Arrays;

public class NumLitToken extends Token implements INumLitToken{

    public NumLitToken(Kind kind, int pos, int length, char[] source) {
        super(kind, pos, length, source);
    }

    @Override
    public Kind getKind() {
        return Kind.NUM_LIT;
    }
    @Override
    public int getValue() {
        String val = "";
        for (int i = 0; i < length; i++) {
            val += source[pos + i];
        }
        return Integer.parseInt(val);
    }
}
