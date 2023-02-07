package edu.ufl.cise.plcsp23;

import java.util.Arrays;

public class Token implements IToken{

    final Kind kind;
    final int pos;
    final int length;
    final char[] source;

    public Token(Kind kind, int pos, int length, char[] source) {
        super();
        this.kind = kind;
        this.pos = pos;
        this.length = length;
        this.source = source;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return null;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getTokenString() {
        String val = "";
        for (int i = 0; i < length; i++) {
            val += source[pos + i];
        }
        return val;
    }
}
