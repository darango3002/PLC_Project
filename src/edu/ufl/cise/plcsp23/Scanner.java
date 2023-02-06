package edu.ufl.cise.plcsp23;

import java.util.Arrays;
import java.util.HashMap;

import edu.ufl.cise.plcsp23.IToken.Kind;
import org.junit.jupiter.api.TestClassOrder;

public class Scanner implements IScanner {
    final String input;
    //array containing input chars, terminated with extra char 0
    final char[] inputChars;

    //invariant ch == inputChars[pos]
    int pos; //position of ch
    char ch; //next char

    private enum State {
        START,
        IN_IDENT,
        IN_NUM_LIT,
        HAVE_EQ,
        HAVE_LT,
        HAVE_GT,
        HAVE_BITAND,
        HAVE_BITOR,
        HAVE_TIMES,

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
        reservedWords.put("Z", Kind.RES_X);
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

    }

    @Override public IToken next() throws LexicalException {
        Token token = scanToken();
        return token;
    }

    public void nextChar() {
        pos++;
        ch = inputChars[pos];
    }

    private Token scanToken() throws LexicalException {
        State state = State.START;
        int tokenStart = -1;

        while(true) {
            switch (state) {
                case START -> {
                    tokenStart = pos;
                    switch (ch) {
                        // EOL
                        case 0 -> { //end of input
                            return new Token(Kind.EOF, tokenStart, 0, inputChars);
                        }

                        // WHITESPACE
                        case 32, 13, 10, 9, 12 -> nextChar(); //whitespace chars ( SP | CR | LF | TAB | FF )

                        // NUM_LIT START and ZERO
                        case '1','2','3','4','5','6','7','8','9' -> {
                            state = State.IN_NUM_LIT;
                            nextChar();
                        }
                        case '0' -> {
                            nextChar();
                            return new NumLitToken(Kind.NUM_LIT, tokenStart, 1, inputChars);

                        }

                        // SINGLE CHAR OPERATORS OR SEPARATORS
                        case '.' -> {
                            nextChar();
                            return new Token(Kind.DOT, tokenStart, 1, inputChars);
                        }
                        case ',' -> {
                            nextChar();
                            return new Token(Kind.COMMA, tokenStart, 1, inputChars);
                        }
                        case '?' -> {
                            nextChar();
                            return new Token(Kind.QUESTION, tokenStart, 1, inputChars);
                        }
                        case ':' -> {
                            nextChar();
                            return new Token(Kind.COLON, tokenStart, 1, inputChars);
                        }
                        case '(' -> {
                            nextChar();
                            return new Token(Kind.LPAREN, tokenStart, 1, inputChars);
                        }
                        case ')' -> {
                            nextChar();
                            return new Token(Kind.RPAREN, tokenStart, 1, inputChars);
                        }
                        case '[' -> {
                            nextChar();
                            return new Token(Kind.LSQUARE, tokenStart, 1, inputChars);
                        }
                        case ']' -> {
                            nextChar();
                            return new Token(Kind.RSQUARE, tokenStart, 1, inputChars);
                        }
                        case '{' -> {
                            nextChar();
                            return new Token(Kind.LCURLY, tokenStart, 1, inputChars);
                        }
                        case '}' -> {
                            nextChar();
                            return new Token(Kind.RCURLY, tokenStart, 1, inputChars);
                        }
                        case '!' -> {
                            nextChar();
                            return new Token(Kind.BANG, tokenStart, 1, inputChars);
                        }
                        case '+' -> {
                            nextChar();
                            return new Token(Kind.PLUS, tokenStart, 1, inputChars);
                        }
                        case '-' -> {
                            nextChar();
                            return new Token(Kind.MINUS, tokenStart, 1, inputChars);
                        }
                        case '/' -> {
                            nextChar();
                            return new Token(Kind.DIV, tokenStart, 1, inputChars);
                        }
                        case '%' -> {
                            nextChar();
                            return new Token(Kind.MOD, tokenStart, 1, inputChars);
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

                        /*
                        TODO:
                         ADD SYSTEM FOR DETERMINING COLUMN AND ROW
                         CHECK FOR IDENT START
                         CHECK FOR NUM_LIT START
                         CHECK FOR STRING_LIT START
                         ADD ESCAPE SEQUENCES,
                         CHECK RESERVED WORDS,
                         CHECK FOR COMMENTS,
                         MORE?
                         */


                        default -> {
                            throw new UnsupportedOperationException(
                                    "not implemented yet");
                        }
                    }
                }
                case HAVE_EQ -> {
                    if (ch == '=') {
                        state = State.START;
                        nextChar();
                        return new Token(Kind.EQ, tokenStart,2, inputChars);
                    }
                    else {
                        error("expected =");
                    }
                }
                case IN_NUM_LIT -> {
                    if (isDigit(ch)) {
                        nextChar();
                    }
                    else {
                        int length = pos-tokenStart;
                        return new NumLitToken(Kind.NUM_LIT, tokenStart, length, inputChars);
                    }
                }
                default -> {
                    throw new UnsupportedOperationException("Bug in Scanner");
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

    private boolean isEscapeSequence(int ch) {
        return (ch == '\b'); // TODO: add rest of escape sequences
    }



    private void error (String message) throws LexicalException {
        throw new LexicalException("Error at pos " + pos + ": "  + message);
    }


}