package edu.ufl.cise.plcsp23;

public class NumLitToken extends Token implements INumLitToken{

    public NumLitToken(Kind kind, int pos, int length, char[] source, int line, int column) {
        super(kind, pos, length, source, line, column);
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
