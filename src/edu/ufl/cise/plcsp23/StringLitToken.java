package edu.ufl.cise.plcsp23;

import java.util.Arrays;

public class StringLitToken extends Token implements IStringLitToken{

    public StringLitToken(Kind kind, int pos, int length, char[] source) {
        super(kind, pos, length, source);

    }
    @Override
    public Kind getKind() {
        return Kind.STRING_LIT;
    }

    @Override
    public String getTokenString() {
        return Arrays.toString(source);
    }

    public String getValue() {
        return null;
    }
}
