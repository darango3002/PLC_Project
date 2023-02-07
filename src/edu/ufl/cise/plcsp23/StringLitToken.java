package edu.ufl.cise.plcsp23;

import java.util.Arrays;

public class StringLitToken extends Token implements IStringLitToken{

    public StringLitToken(Kind kind, int pos, int length, char[] source, int line, int column) {
        super(kind, pos, length, source, line, column);

    }
    @Override
    public Kind getKind() {
        return Kind.STRING_LIT;
    }

    public String getValue() {
        String val = "";
        for (int i = 0; i < length; i++) {
            val += source[pos + i];
        }
        return val.substring(1, val.length() - 1);
    }
}
