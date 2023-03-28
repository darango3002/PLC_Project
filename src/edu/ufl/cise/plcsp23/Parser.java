package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.AST;
import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.IToken.Kind;
import edu.ufl.cise.plcsp23.ast.Dimension;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class Parser implements IParser{

    IToken t; // current token
    Scanner scanner;
    Parser(Scanner scanner) throws LexicalException {
        t = scanner.next();
        this.scanner = scanner;
    }


    @Override
    public AST parse() throws PLCException {
            return program();
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

    // Checks if the Kind of token is a Kind of Type
    protected boolean isTypeKind() {
        return isKind(Kind.RES_image, Kind.RES_pixel, Kind.RES_int, Kind.RES_string, Kind.RES_void);
    }
    void match(IToken c) throws SyntaxException, LexicalException {
        if (t == c) {
            consume();
        }
        else
            throw new SyntaxException("current token " + t + "does not match expected token: " + c);
    }

    void match(Kind k) throws SyntaxException, LexicalException {
        if (t.getKind() == k) {
            consume();
        }
        else
            throw new SyntaxException("current token " + t + "does not match expected token: " + k);
    }

    void consume() throws LexicalException {
        t = scanner.next();
    }

    // <program> ::= <Type> IDENT ( <param_list> ) <block>
    Program program() throws SyntaxException, LexicalException {
        IToken firstToken = t;
        Program p = null;

        Type type = null;
        Ident ident = null;
        List<NameDef> paramList = null;
        Block block = null;

        if (isTypeKind()) {
            type = Type.getType(t);
            consume();

            if (isKind(Kind.IDENT)) {
                ident = new Ident(t);
                consume();
            }
            else
                throw new SyntaxException("Expected Kind.IDENT! Received Kind: " + t.getKind());

            match(Kind.LPAREN);
            paramList = param_list();
            match(Kind.RPAREN);

            block = block();

            p = new Program(firstToken, type, ident, paramList, block);
        }
        else
            throw new SyntaxException("Expected Kind of Type! Received Kind: " + t.getKind());

        if (!isKind(Kind.EOF))
            throw new SyntaxException("Expected EOF token after end of program, but " + t.getKind() + " was received");

        return p;
    }

    // <block> ::= { <dec_list> <statement_list< }
    Block block() throws SyntaxException, LexicalException {
        IToken firstToken = t;
        Block b = null;
        List<Declaration> decList = null;
        List<Statement> statementList = null;

        match(Kind.LCURLY);
        decList = dec_list();
        statementList = statement_list();
        match(Kind.RCURLY);

        return new Block(firstToken, decList, statementList);
    }

    // <dec_list> ::= (<declaration>.)*
    List<Declaration> dec_list() throws SyntaxException, LexicalException {
        List<Declaration> decList = new ArrayList<Declaration>();
        Declaration d = null;

        while (isTypeKind()) {
            d = declaration();
            match(Kind.DOT);

            decList.add(d);
        }
        return decList;

    }

    // <statement_list> ::= (<statement>.)*
    List<Statement> statement_list() throws SyntaxException, LexicalException {
        List<Statement> statementList = new ArrayList<Statement>();
        Statement s = null;

        while (isKind(Kind.IDENT, Kind.RES_write, Kind.RES_while)) { // predict of <statement>
            s = statement();
            match(Kind.DOT);

            statementList.add(s);
        }
        return statementList;
    }

    // <param_list> ::= ε | <name_def> (, <name_def>)*
    List<NameDef> param_list() throws SyntaxException, LexicalException {
        IToken firstToken = t;
        List<NameDef> paramList = new ArrayList<NameDef>();

        if (isKind(Kind.RPAREN))
            return paramList;
        else if (isTypeKind()) {
            paramList.add(name_def());

            while (isKind(Kind.COMMA)) {
                consume();
                paramList.add(name_def());
            }
        }
        else
            throw new SyntaxException("Expected Kind of Type! Received Kind: " + t.getKind());

        return paramList;
    }

    Type type() throws SyntaxException, LexicalException {
        if (isKind(Kind.RES_image, Kind.RES_pixel, Kind.RES_int, Kind.RES_string, Kind.RES_void)) {
            consume();
            return Type.getType(t);
        }
        else {
            throw new SyntaxException("INVALID TYPE " + t.getKind());
        }
    }

    // <name_def> ::= <Type> (IDENT | <dimension> IDENT)
    NameDef name_def() throws SyntaxException, LexicalException {
        IToken firstToken = t;
        Type type = null;
        Dimension dimension = null;
        Ident ident = null;

        if (isTypeKind()) {
            type = Type.getType(t);
            consume();

            if (isKind(Kind.IDENT)) {
                ident = new Ident(t);
                consume();
            }
            else if (isKind(Kind.LSQUARE)) {
                dimension = dimension();

                if (isKind(Kind.IDENT)) {
                    ident = new Ident(t);
                    consume();
                }
                else
                    throw new SyntaxException("Expected Kind.IDENT! Received Kind: " + t.getKind());

            }
            else
                throw new SyntaxException("Expected Kind.IDENT or Kind.LSQUARE (first of <dimension>). Received Kind: " + t.getKind());

        }
        return new NameDef(firstToken, type, dimension, ident);

    }

    // <declaration> ::= NameDef (ε | = Expr)
    Declaration declaration() throws SyntaxException, LexicalException {
        IToken firstToken = t;
        NameDef nameDef = null;
        Expr initializer = null;

        if (isTypeKind()) {
            nameDef = name_def();

            if (isKind(Kind.ASSIGN)) {
                consume();
                initializer = expr();
            }
        }
        else
            throw new SyntaxException("Expected Kind of Type! Received Kind: " + t.getKind());

        return new Declaration(firstToken, nameDef, initializer);
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
    Expr conditional_expr() throws LexicalException, SyntaxException {
        IToken firstToken = t;
        Expr e = null;
        Expr guard = null;
        Expr trueCase = null;
        Expr falseCase = null;

//        consume(); // consumes 'if'
        match(Kind.RES_if);
        guard = expr();
        match(Kind.QUESTION);
        trueCase = expr();
        match(Kind.QUESTION);
        falseCase = expr();

        e = new ConditionalExpr(firstToken, guard, trueCase, falseCase);

        return e;
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
        IToken firstToken = t;
        Expr e = null;
        if (isKind(Kind.BANG, Kind.MINUS, Kind.RES_sin, Kind.RES_cos, Kind.RES_atan)) {
            IToken op = t;
            consume();
            e = new UnaryExpr(firstToken, op.getKind(), unary_expr());
        }
        // TODO: IMPLEMENT UnaryExprPostfix
        else
            e = unary_expr_postfix();

        return e;
    }

    Expr unary_expr_postfix() throws LexicalException, SyntaxException {
        IToken firstToken = t;
        Expr prim = primary_expr();
        PixelSelector pixel = null;
        ColorChannel channel = null;

        if (isKind(Kind.LSQUARE)) {
            pixel = pixel_selector();
        }
        if (isKind(Kind.COLON)) {
            channel = channel_selector();
        }
        if (pixel == null && channel == null) {
            return prim;
        }
        return new UnaryExprPostfix(t, prim, pixel, channel);
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
            match(Kind.RPAREN);
        }
        else if (isKind(Kind.RES_Z)) {
            e = new ZExpr(firstToken);
            consume();
        }
        else if (isKind(Kind.RES_rand)) {
            e = new RandomExpr(firstToken);
            consume();
        }
        else if (isKind(Kind.RES_x, Kind.RES_y, Kind.RES_a, Kind.RES_r)) {
            e = new PredeclaredVarExpr(firstToken);
            consume();
        }
        // TODO: IMPLEMENT ExpandedPixel and PixelFunctionExpr within PrimaryExpr
        else if (isKind(Kind.LSQUARE)) {
            e = expanded_pixel();
        }
        else if (isKind(Kind.RES_x_cart, Kind.RES_y_cart, Kind.RES_a_polar, Kind.RES_r_polar)) {
            e = pixel_function_expr();
        }
        else
            throw new SyntaxException("EMPTY EXPRESSION");

        return e;
    }

    ColorChannel channel_selector() throws SyntaxException, LexicalException {

        match(Kind.COLON);
        if (isKind(Kind.RES_red, Kind.RES_grn, Kind.RES_blu)) {
            ColorChannel color = ColorChannel.getColor(t);
            consume();
            return color;
        }
        else {
            throw new SyntaxException("INVALID CHANNEL SELECTOR COLOR");
        }
    }

    PixelSelector pixel_selector() throws SyntaxException, LexicalException {
        IToken firstToken = t;
        Expr x = null;
        Expr y = null;

        match(Kind.LSQUARE);
        x = expr();
        match(Kind.COMMA);
        y = expr();
        match(Kind.RSQUARE);

        return new PixelSelector(firstToken, x, y);
    }

    ExpandedPixelExpr expanded_pixel() throws SyntaxException, LexicalException {
        IToken firstToken = t;
        Expr expr1 = null;
        Expr expr2 = null;
        Expr expr3 = null;

        match(Kind.LSQUARE);
        expr1 = expr();
        match(Kind.COMMA);
        expr2 = expr();
        match(Kind.COMMA);
        expr3 = expr();
        match(Kind.RSQUARE);

        return new ExpandedPixelExpr(t, expr1, expr2, expr3);
    }

    PixelFuncExpr pixel_function_expr() throws SyntaxException, LexicalException {
        IToken firstToken = t;
        Kind function = null;
        PixelSelector selector = null;

        function = t.getKind();
        consume();
        selector = pixel_selector();

        return new PixelFuncExpr(firstToken, function, selector);
    }

    Dimension dimension() throws SyntaxException, LexicalException {
        IToken firstToken = t;
        Expr width = null;
        Expr height = null;

        match(Kind.LSQUARE);
        width = expr();
        match(Kind.COMMA);
        height = expr();
        match(Kind.RSQUARE);

        return new Dimension(firstToken, width, height);
    }

    LValue l_value() throws SyntaxException, LexicalException{
        IToken firstToken = t;
        Ident ident = null;
        PixelSelector ps = null;
        ColorChannel color = null;

        if (isKind(Kind.IDENT)) {
            ident = new Ident(t);
            consume();

            if (isKind(Kind.LSQUARE)) { // Predict of <pixel_selector>
                ps = pixel_selector();
            }
            if (isKind(Kind.COLON)) {
                color = channel_selector();
            }
        }
        return new LValue(firstToken, ident, ps, color);
    }

    Statement statement() throws SyntaxException, LexicalException {
        IToken firstToken = t;
        LValue lv = null;
        Expr e = null;
        Expr guard = null;
        Block block = null;

        if (isKind(Kind.IDENT)) {
            lv = l_value();
            match(Kind.ASSIGN);
            e = expr();
            return new AssignmentStatement(firstToken, lv, e);
        }
        else if (isKind(Kind.RES_write)) {
            consume();
            e = expr();
            return new WriteStatement(firstToken, e);
        }
        else if (isKind(Kind.RES_while)) {
            consume();
            guard = expr();
            block = block();
            return new WhileStatement(firstToken, guard, block);
        }
        else if (isKind(Kind.COLON)) {
            consume();
            e = expr();
            return new ReturnStatement(firstToken, e);
        }
        else
            throw new SyntaxException("Expected Kind.IDENT (LValue), Kind.RES_write, or Kind.RES_while. Received Kind :" + t.getKind());

    }


}
