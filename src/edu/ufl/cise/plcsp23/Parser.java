package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.AST;
import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.IToken.Kind;

public class Parser implements IParser{

    IToken t; // current token
    Scanner scanner;
    Parser(Scanner scanner) throws LexicalException {
        t = scanner.next();
        this.scanner = scanner;
    }


    @Override
    public AST parse() throws PLCException {
            return expr();
    }

    protected boolean isKind(Kind kind) {
        return t.getKind() == kind;
    }

    protected boolean isKind(Kind... kinds) {
        for (Kind k : kinds) {
            if (k == t.getKind())
                return true;
        }
        return false;
    }
    void match(IToken c) throws SyntaxException, LexicalException {
        if (t == c) {
            consume();
        }
        else
            throw new SyntaxException("current token " + t + "does not match expected token: " + c);
    }

    void consume() throws LexicalException {
        t = scanner.next();
    }

    // <expr> ::=   <conditional_expr> | <or_expr>
    Expr expr() throws SyntaxException, LexicalException {
        IToken firstToken = t;
        Expr e = null;
        if (isKind(Kind.RES_if)) {
            e = conditional_expr();
        }
        else {
            e = or_expr();
        }
        return e;
    }

    // <conditional_expr>  ::= if <expr> ? <expr> ? <expr>
    Expr conditional_expr() {
        return null;
        //TODO: Implement
    }

    // <or_expr> ::=  <and_expr> (  ( | | || ) <and_expr>)*
    Expr or_expr() throws LexicalException, SyntaxException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;
        left = and_expr();
        while(isKind(Kind.OR, Kind.BITOR)) {
            IToken op = t;
            consume();
            right = and_expr();
            left = new BinaryExpr(firstToken, left, op.getKind(), right);
        }
        return left;
    }

    // <and_expr> ::=  <comparison_expr> ( ( & | && )  <comparison_expr>)*
    Expr and_expr() throws LexicalException, SyntaxException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;
        left = comparison_expr();
        while(isKind(Kind.AND, Kind.BITAND)) {
            IToken op = t;
            consume();
            right = comparison_expr();
            left = new BinaryExpr(firstToken, left, op.getKind(), right);
        }
        return left;
    }

    // <comparison_expr> ::=   <power_expr> ( (< | > | == | <= | >=) <power_expr>)*
    Expr comparison_expr() throws LexicalException, SyntaxException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;
        left = power_expr();
        while(isKind(Kind.LT, Kind.GT, Kind.EQ, Kind.LE, Kind.GE)) {
            IToken op = t;
            consume();
            right = power_expr();
            left = new BinaryExpr(firstToken, left, op.getKind(), right);
        }
        return left;
    }

    // <power_expr> ::=    <additive_expr> (** <power_expr>)*
    Expr power_expr() throws LexicalException, SyntaxException { //
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;
        left = additive_expr();
        while(isKind(Kind.EXP)) {
            IToken op = t;
            consume();
            right = power_expr();
            left = new BinaryExpr(firstToken, left, op.getKind(), right);
        }
        return left;
    }

    // <additive_expr> ::=  <multiplicative_expr> ( ( + | - ) <multiplicative_expr> )*
    Expr additive_expr() throws LexicalException, SyntaxException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;
        left = multiplicative_expr();
        while(isKind(Kind.PLUS, Kind.MINUS)) {
            IToken op = t;
            consume();
            right = multiplicative_expr();
            left = new BinaryExpr(firstToken, left, op.getKind(), right);
        }
        return left;
    }

    // <multiplicative_expr> ::= <unary_expr> (( * | / | % ) <unary_expr>)*
    Expr multiplicative_expr() throws LexicalException, SyntaxException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;
        left = unary_expr();
        while(isKind(Kind.TIMES, Kind.DIV, Kind.MOD)) {
            IToken op = t;
            consume();
            right = unary_expr();
            left = new BinaryExpr(firstToken, left, op.getKind(), right);
        }
        return left;
    }

    // <unary_expr> ::= ( ! | - | sin | cos | atan) <unary_expr> |   <primary_expr>
    Expr unary_expr() throws LexicalException, SyntaxException {
        // TODO: FIX (returns null for some reason)
        IToken firstToken = t;
        Expr e = null;
        if (isKind(Kind.BANG, Kind.MINUS, Kind.RES_sin, Kind.RES_cos, Kind.RES_atan)) {
            IToken op = t;
            consume();
            e = new UnaryExpr(firstToken, op.getKind(), e);
        }
        else
            e = primary_expr();

        return e;
    }

    // <primary_expr> ::=
    // STRING_LIT |
    // NUM_LIT |
    // IDENT |
    // ( <expr> ) |
    // Z |
    // rand
    Expr primary_expr() throws LexicalException, SyntaxException {
        IToken firstToken = t;
        Expr e = null;
        if (isKind(Kind.STRING_LIT)) {
            e = new StringLitExpr(firstToken);
            consume();
        }
        else if (isKind(Kind.NUM_LIT)) {
            e = new NumLitExpr(firstToken);
            consume();
        }
        else if (isKind(Kind.IDENT)) {
            e = new IdentExpr(firstToken);
            consume();
        }
        else if (isKind(Kind.LPAREN)) {
            consume();
            e = expr();
            match(t);
        }
        else if (isKind(Kind.RES_Z)) {
            e = new ZExpr(firstToken);
            consume();
        }
        else if (isKind(Kind.RES_rand)) {
            e = new RandomExpr(firstToken);
            consume();
        }
        else
            throw new SyntaxException("EMPTY EXPRESSION");

        return e;
    }


}
