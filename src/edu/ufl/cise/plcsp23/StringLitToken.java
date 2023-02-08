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
            char ch = source[pos + i];
            if (ch == '\\') {
                char nextChar = getEscapeSequence(source[pos + i + 1]);
                val += nextChar;
                i++;
            }
            else {
                val += ch;
            }
        }
        return val.substring(1, val.length() - 1);
    }

    private char getEscapeSequence(char ch) {
        if (ch == 'b') {
            return '\b';
        }
        else if (ch == 't') {
            return '\t';
        }
        else if (ch == 'n') {
            return '\n';
        }
        else if (ch == 'r') {
            return '\r';
        }
        else if (ch == '"') {
            return '\"';
        }
        else {
            return ch;
        }
    }
}
