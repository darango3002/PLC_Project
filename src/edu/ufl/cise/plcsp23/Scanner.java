package edu.ufl.cise.plcsp23;

import java.util.Arrays;
import java.util.HashMap;

import edu.ufl.cise.plcsp23.IToken.Kind;

public class Scanner implements IScanner {
    final String input;
    //array containing input chars, terminated with extra char 0
    final char[] inputChars;

    //invariant ch == inputChars[pos]
    int pos; //position of ch
    char ch; //next char
    int currentLine;
    int currentColumn;

    private enum State {
        START,
        IN_IDENT,
        IN_NUM_LIT,
        IN_STRING_LIT,
        IN_STRING_ESCAPE,
        IN_ESCAPE_SEQUENCE,
        HAVE_EQ,
        HAVE_LT,
        HAVE_GT,
        HAVE_BITAND,
        HAVE_BITOR,
        HAVE_TIMES,
        IN_EXCHANGE,
    }

    private static HashMap<String, Kind> reservedWords;
    static {
        reservedWords = new HashMap<String, Kind>();

        reservedWords.put("image", Kind.RES_image);
        reservedWords.put("pixel", Kind.RES_pixel);
        reservedWords.put("int", Kind.RES_int);
        reservedWords.put("string", Kind.RES_string);
        reservedWords.put("void", Kind.RES_void);
        reservedWords.put("nil", Kind.RES_nil);
        reservedWords.put("load", Kind.RES_load);
        reservedWords.put("display", Kind.RES_display);
        reservedWords.put("write", Kind.RES_write);
        reservedWords.put("x", Kind.RES_x);
        reservedWords.put("y", Kind.RES_y);
        reservedWords.put("a", Kind.RES_a);
        reservedWords.put("r", Kind.RES_r);
        reservedWords.put("X", Kind.RES_X);
        reservedWords.put("Y", Kind.RES_Y);
        reservedWords.put("Z", Kind.RES_Z);
        reservedWords.put("x_cart", Kind.RES_x_cart);
        reservedWords.put("y_cart", Kind.RES_y_cart);
        reservedWords.put("a_polar", Kind.RES_a_polar);
        reservedWords.put("r_polar", Kind.RES_r_polar);
        reservedWords.put("rand", Kind.RES_rand);
        reservedWords.put("sin", Kind.RES_sin);
        reservedWords.put("cos", Kind.RES_cos);
        reservedWords.put("atan", Kind.RES_atan);
        reservedWords.put("if", Kind.RES_if);
        reservedWords.put("while", Kind.RES_while);
    }

    //constructor
    public Scanner(String input){
        this.input = input;
        inputChars = Arrays.copyOf(input.toCharArray(), input.length() + 1);
        pos = 0;
        ch = inputChars[pos];
        currentLine = 1;
        currentColumn = 1;
    }

    @Override public IToken next() throws LexicalException {
        Token token = scanToken();
        return token;
    }

    private void nextChar() {
        pos++;
        ch = inputChars[pos];
        nextColumn();
    }

    private void nextLine() {
        currentLine++;
        currentColumn = 1;
    }

    private void nextColumn() {
        currentColumn++;
    }

    private Token scanToken() throws LexicalException {
        State state = State.START;
        int tokenStart = -1;
        int startLine = -1;
        int startColumn = -1;

        while(true) {
            System.out.println(ch);
            switch (state) {
                case START -> {
                    tokenStart = pos;
                    startLine = currentLine;
                    startColumn = currentColumn;

                    switch (ch) {
                        // EOL
                        case 0 -> { //end of input
                            return new Token(Kind.EOF, tokenStart, 0, inputChars, startLine, startColumn);
                        }

                        // WHITESPACE
                        case 32, 13, 10, 9, 12 -> { //whitespace chars ( SP | CR | LF | TAB | FF )
                            if (ch == 10) {
                                nextChar();
                                nextLine();
                            }
                            else {
                                nextChar();
                            }
                        }

                        // NUM_LIT START and ZERO
                        case '1','2','3','4','5','6','7','8','9' -> {
                            state = State.IN_NUM_LIT;
                            nextChar();
                        }
                        case '0' -> {
                            nextChar();
                            return new NumLitToken(Kind.NUM_LIT, tokenStart, 1, inputChars, startLine, startColumn);
                        }

                        // STRING LIT START
                        case '"' -> {
                            state = State.IN_STRING_LIT;
                            nextChar();
                        }

                        // SINGLE CHAR OPERATORS OR SEPARATORS
                        case '.' -> {
                            nextChar();
                            return new Token(Kind.DOT, tokenStart, 1, inputChars, startLine, startColumn);
                        }
                        case ',' -> {
                            nextChar();
                            return new Token(Kind.COMMA, tokenStart, 1, inputChars, startLine, startColumn);
                        }
                        case '?' -> {
                            nextChar();
                            return new Token(Kind.QUESTION, tokenStart, 1, inputChars, startLine, startColumn);
                        }
                        case ':' -> {
                            nextChar();
                            return new Token(Kind.COLON, tokenStart, 1, inputChars, startLine, startColumn);
                        }
                        case '(' -> {
                            nextChar();
                            return new Token(Kind.LPAREN, tokenStart, 1, inputChars, startLine, startColumn);
                        }
                        case ')' -> {
                            nextChar();
                            return new Token(Kind.RPAREN, tokenStart, 1, inputChars, startLine, startColumn);
                        }
                        case '[' -> {
                            nextChar();
                            return new Token(Kind.LSQUARE, tokenStart, 1, inputChars, startLine, startColumn);
                        }
                        case ']' -> {
                            nextChar();
                            return new Token(Kind.RSQUARE, tokenStart, 1, inputChars, startLine, startColumn);
                        }
                        case '{' -> {
                            nextChar();
                            return new Token(Kind.LCURLY, tokenStart, 1, inputChars, startLine, startColumn);
                        }
                        case '}' -> {
                            nextChar();
                            return new Token(Kind.RCURLY, tokenStart, 1, inputChars, startLine, startColumn);
                        }
                        case '!' -> {
                            nextChar();
                            return new Token(Kind.BANG, tokenStart, 1, inputChars, startLine, startColumn);
                        }
                        case '+' -> {
                            nextChar();
                            return new Token(Kind.PLUS, tokenStart, 1, inputChars, startLine, startColumn);
                        }
                        case '-' -> {
                            nextChar();
                            return new Token(Kind.MINUS, tokenStart, 1, inputChars, startLine, startColumn);
                        }
                        case '/' -> {
                            nextChar();
                            return new Token(Kind.DIV, tokenStart, 1, inputChars, startLine, startColumn);
                        }
                        case '%' -> {
                            nextChar();
                            return new Token(Kind.MOD, tokenStart, 1, inputChars, startLine, startColumn);
                        }

                        // MULTI CHAR OPERATORS OR SEPARATORS
                        case '=' -> {
                            state = State.HAVE_EQ;
                            nextChar();
                        }
                        case '<' -> {
                            state = State.HAVE_LT;
                            nextChar();
                        }
                        case '>' -> {
                            state = State.HAVE_GT;
                            nextChar();
                        }
                        case '&' -> {
                            state = State.HAVE_BITAND;
                            nextChar();
                        }
                        case '|' -> {
                            state = State.HAVE_BITOR;
                            nextChar();
                        }
                        case '*' -> {
                            state = State.HAVE_TIMES;
                            nextChar();
                        }

                        // COMMENT
                        case '~' -> {
//                            System.out.println(ch);
                            nextChar();
                            while (true) {
//                                System.out.println(ch);
                                if (ch == '\n') {
//                                    System.out.println("ESCAPE");
                                    nextChar();
                                    nextLine();
                                    break;
                                }
                                nextChar();
                            }
                        }

                        /*
                        TODO:
                         CHECK FOR STRING_LIT START
                         ADD ESCAPE SEQUENCES,
                         MORE?

                         Add BITAND and BITOR
                         */


                        default -> {
                            System.out.println(ch);
                            if (isLetter(ch) || ch == '_') { // IDENTIFIER
                                state = State.IN_IDENT;
                                nextChar();
                            }
                            else {
                                throw new LexicalException("not implemented yet: " + ch);
                            }
                        }
                    }
                }
                case HAVE_TIMES -> {
                    if (ch == '*') {
                        nextChar();
                        return new Token(Kind.EXP, tokenStart, 2, inputChars, startLine, startColumn);
                    }
                    else {
                        return new Token(Kind.TIMES, tokenStart, 1, inputChars, startLine, startColumn);
                    }
                }
                case HAVE_EQ -> {
                    if (ch == '=') {
                        state = State.START;
                        nextChar();
                        return new Token(Kind.EQ, tokenStart,2, inputChars, startLine, startColumn);
                    }
                    else {
//                        nextChar();
                        return new Token(Kind.ASSIGN, tokenStart,1, inputChars, startLine, startColumn);
                    }
                }
                case HAVE_LT -> {
                    if (ch == '=') {
                        nextChar();
                        return new Token(Kind.LE, tokenStart, 2, inputChars, startLine, startColumn);
                    }
                    else if (ch == '-') {
                        state = State.IN_EXCHANGE;
                        nextChar();
                    }
                    else {
//                        nextChar();
                        return new Token(Kind.LT, tokenStart, 1, inputChars, startLine, startColumn);
                    }
                }
                case IN_EXCHANGE -> {
                    if (ch == '>') {
                        nextChar();
                        return new Token(Kind.EXCHANGE, tokenStart, 3, inputChars, startLine, startColumn);
                    }
                    else {
                        throw new LexicalException("exchange exception");
                    }
                }
                case HAVE_GT -> {
                    if (ch == '=') {
                        nextChar();
                        return new Token(Kind.GE, tokenStart, 2, inputChars, startLine, startColumn);
                    }
                    else {
//                        nextChar();
                        return new Token(Kind.GT, tokenStart, 1, inputChars, startLine, startColumn);
                    }
                }
                case HAVE_BITAND -> {
                    if (ch == '&') {
                        nextChar();
                        return new Token(Kind.AND, tokenStart, 2, inputChars, startLine, startColumn);
                    }
                    else {
                        return new Token(Kind.BITAND, tokenStart, 1, inputChars, startLine, startColumn);
                    }
                }
                case HAVE_BITOR -> {
                    if (ch == '|') {
                        nextChar();
                        return new Token(Kind.OR, tokenStart, 2, inputChars, startLine, startColumn);
                    }
                    else {
                        return new Token(Kind.BITOR, tokenStart, 2, inputChars, startLine, startColumn);
                    }
                }
                case IN_NUM_LIT -> {
                    if (isDigit(ch)) {
                        nextChar();
                    }
                    else {
                        int length = pos-tokenStart;

                        try { //checking for large inputs of integers
                            Integer.parseInt(input.substring(tokenStart, length + tokenStart));
                        }
                        catch (NumberFormatException ignored) {
                            error("Number is too large!");
                        }

                        return new NumLitToken(Kind.NUM_LIT, tokenStart, length, inputChars, startLine, startColumn);
                    }
                }
                case IN_STRING_LIT -> {

                    if (ch == 10 || ch == 13) { // LF OR CR (newline and carriage return)
                        error("NEWLINE OR CARRIAGE RETURN: " + (int)ch);
                    }
                    else if (ch == '\\') {
                        state = State.IN_STRING_ESCAPE;
                        nextChar();
                    }
                    else if (ch == '"') {
                        int length = pos-tokenStart + 1;
                        nextChar();
                        return new StringLitToken(Kind.STRING_LIT, tokenStart, length, inputChars, startLine, startColumn);
                    }
                    else if (ch == 0) {
                        error("No ending quotes");
                    }
                    else { // ANY ASCII VALUE EXCEPT CR AND LF
                        nextChar();
                    }

                }
                case IN_STRING_ESCAPE -> {
                    if (isEscapeChar(ch)) {
                        state = State.IN_STRING_LIT;
                        nextChar();
                    }
                    else {
                        error("unidentified escape sequence using char: " + (int)ch);
                    }
                }
                case IN_IDENT -> {
                    System.out.println(ch);
                    if(isLetter(ch) || isDigit(ch) || ch == '_') {
                        nextChar();
                    }
                    else {
                        int length = pos-tokenStart;
//                        System.out.println(length);
//                        System.out.println("token end " + ch);
                        int temp = tokenStart;
                        String tempToken = "";
                        while(temp < pos) {
//                            System.out.print(inputChars[temp]);
                            tempToken += inputChars[temp];
                            temp++;
                        }
                        System.out.println("NEW TOKEN: " + tempToken);
                        if (reservedWords.containsKey(tempToken)) {
                            System.out.println("KEYWORD: " + tempToken);
                            return new Token(reservedWords.get(tempToken), tokenStart, length, inputChars, startLine, startColumn);
                        }
                        else {
                            return new Token(Kind.IDENT, tokenStart, length, inputChars, startLine, startColumn);
                        }
                    }
                }
                default -> {
                    throw new LexicalException("Bug in Scanner: " + ch);
                }
            }
        }
    }

    //UTILITY FUNCTIONS
    private boolean isDigit(int ch) {
        return '0' <= ch && ch <= '9';
    }

    private boolean isLetter(int ch) {
        return ('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z');
    }

    private boolean isEscapeChar(int ch) {
        return (ch == 'b' || ch == 't' || ch == 'n' || ch == 'r' || ch == '"' || ch == '\\');
    }

    private void error (String message) throws LexicalException {
        throw new LexicalException("Error at pos " + pos + ": "  + message);
    }


}
