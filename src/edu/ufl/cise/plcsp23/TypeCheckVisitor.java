package edu.ufl.cise.plcsp23;
import edu.ufl.cise.plcsp23.TypeCheckException;
import edu.ufl.cise.plcsp23.PLCException;
import edu.ufl.cise.plcsp23.IToken;
import edu.ufl.cise.plcsp23.ast.*;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.ArrayList;

public class TypeCheckVisitor implements ASTVisitor {

    Type programType = null;
    public static class SymbolTable {
        int currentNum;
        int nextNum;
        Stack<Integer> scopeStack = new Stack<Integer>();

        void enterScope() {
            currentNum = nextNum++;
            scopeStack.push(currentNum);
        }
        void closeScope() {
            currentNum = scopeStack.pop();
        }
        // TODO: Eventually need to update HashMap to handle while loops (nested scopes)
        // use ArrayList<Pair<Declaration, int (scopeID)>> to implement the values
        HashMap<String, NameDef> entries = new HashMap<>();
        //returns true if name successfully inserted in symbol table, false if already present
        public boolean insert(String name, NameDef nameDef) {
            return (entries.putIfAbsent(name,nameDef) == null);
        }
        //returns NameDef if present, or null if name not declared.
        public NameDef lookup(String name) {
            return entries.get(name);
        }
    }
    SymbolTable symbolTable = new SymbolTable();
    private void check(boolean condition, AST node, String message)
            throws TypeCheckException {
        if (! condition) { throw new TypeCheckException(message, node.getSourceLoc()); }
    }

    private boolean assignmentCompatible(Type targetType, Type rhsType) {

        if (targetType == Type.IMAGE) {
            return (rhsType == Type.IMAGE || rhsType == Type.PIXEL || rhsType == Type.STRING);
        }
        else if (targetType == Type.PIXEL) {
            return (rhsType == Type.PIXEL || rhsType == Type.INT);
        }
        else if (targetType == Type.INT) {
            return (rhsType == Type.INT || rhsType == Type.PIXEL);
        }
        else { // LValueType == Type.STRING
            return (rhsType == Type.STRING || rhsType == Type.INT || rhsType == Type.PIXEL
                    || rhsType == Type.IMAGE);
        }
    }

    // VISITOR METHODS

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCException {
        symbolTable.enterScope();
        programType = program.getType();
        List<NameDef> paramList = program.getParamList();
        Block block = program.getBlock();
        for (NameDef parameter : paramList) {
            parameter.visit(this, arg);
        }
        block.visit(this, arg);
        symbolTable.closeScope();
        return program;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCException {
        List<Declaration> declarations = block.getDecList();
        List<Statement> statements = block.getStatementList();
        for (Declaration declaration : declarations) {
            declaration.visit(this, arg);
        }
        for (Statement statement : statements) {
            statement.visit(this, arg);
        }
        return block;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        Expr initializer = declaration.getInitializer();
        if (initializer != null) {
            //infer type of initializer
            Type initializerType = (Type) initializer.visit(this,arg);
            check(assignmentCompatible(declaration.getNameDef().getType(), initializerType) ,declaration,
                    "type of expression and declared type do not match");
        }
        if (declaration.getNameDef().getType() == Type.IMAGE) {
            check(declaration.getInitializer() != null || declaration.getNameDef().getDimension() != null,
                    declaration, "Initializer and/or dimension cannot be null || Init: " + declaration.getInitializer()
                            + ", Dim: " + declaration.getNameDef().getDimension());
        }

        declaration.getNameDef().visit(this, arg);

        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException {
        String name = nameDef.getIdent().getName();
        boolean inserted = symbolTable.insert(name, nameDef);
        check(inserted, nameDef, "variable " + name + "already declared");

        if (nameDef.getDimension() != null) {
            check(nameDef.getType() == Type.IMAGE, nameDef, "type is not IMAGE");
            visitDimension(nameDef.getDimension(), arg);
        }
        check(nameDef.getType() != Type.VOID, nameDef, "type of nameDef cannot be void");
        check(symbolTable.lookup(name) != null, nameDef, "name was previously defined in scope");

        return nameDef.getType();
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException {
        Expr primaryExpr = unaryExprPostfix.getPrimary();
        Type resultType = null;
        if (primaryExpr.getType() == Type.PIXEL){
            if (unaryExprPostfix.getPixel() == null && unaryExprPostfix.getColor() != null)
                resultType = Type.INT;
        }
        else if (primaryExpr.getType() == Type.IMAGE) {
            if (unaryExprPostfix.getPixel() == null && unaryExprPostfix.getColor() != null)
                resultType = Type.IMAGE;
            if (unaryExprPostfix.getPixel() != null && unaryExprPostfix.getColor() == null)
                resultType = Type.PIXEL;
            if (unaryExprPostfix.getPixel() != null && unaryExprPostfix.getColor() != null)
                resultType = Type.INT;
        }

        return resultType;
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        pixelFuncExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        predeclaredVarExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {
        Expr expr1 = conditionalExpr.getGuard();
        Expr expr2 = conditionalExpr.getTrueCase();
        Expr expr3 = conditionalExpr.getFalseCase();

        System.out.println(expr1.getType());

        check(expr1.getType() == Type.INT, conditionalExpr, "Expr1 type != INT");
        check(expr1.getType() == expr2.getType(), conditionalExpr, "Expr1 type != INT");

        conditionalExpr.setType(expr1.getType());
        return expr1.getType();
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        IToken.Kind op = binaryExpr.getOp();
        Type leftType = (Type) binaryExpr.getLeft().visit(this, arg);
        Type rightType = (Type) binaryExpr.getRight().visit(this, arg);
        Type resultType = null;
        switch(op) {
            case BITOR,BITAND -> {
                check(leftType == Type.PIXEL && rightType == Type.PIXEL, binaryExpr, "incompatible types for comparison");
                resultType = Type.PIXEL;
            }
            case OR,AND,LT,GT,LE,GE -> {
                check(leftType == Type.INT && rightType == Type.INT, binaryExpr, "incompatible types for comparison");
                resultType = Type.INT;
            }
            case EQ -> {
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else if (leftType == Type.PIXEL && rightType == Type.PIXEL) resultType = Type.INT;
                else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.INT;
                else if (leftType == Type.STRING && rightType == Type.STRING) resultType = Type.INT;
                else check(false, binaryExpr, "incompatible types for operator");
            }
            case EXP     -> {
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else if (leftType == Type.PIXEL && rightType == Type.INT) resultType = Type.PIXEL;
                else check(false, binaryExpr, "incompatible types for operator");
            }
            case PLUS -> {
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else if (leftType == Type.PIXEL && rightType == Type.PIXEL) resultType = Type.PIXEL;
                else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.IMAGE;
                else if (leftType == Type.STRING && rightType == Type.STRING) resultType = Type.STRING;
                else check(false, binaryExpr, "incompatible types for operator");
            }
            case MINUS -> {
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else if (leftType == Type.PIXEL && rightType == Type.PIXEL) resultType = Type.PIXEL;
                else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.IMAGE;
                else check(false, binaryExpr, "incompatible types for operator");
            }
            case TIMES,DIV,MOD -> {
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else if (leftType == Type.PIXEL && rightType == Type.PIXEL) resultType = Type.PIXEL;
                else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.IMAGE;
                else if (leftType == Type.PIXEL && rightType == Type.INT) resultType = Type.PIXEL;
                else if (leftType == Type.IMAGE && rightType == Type.INT) resultType = Type.IMAGE;
                else check(false, binaryExpr, "incompatible types for operator");
            }
            default -> {
                throw new PLCException("compiler error");
            }
        }
        binaryExpr.setType(resultType);
        return resultType;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        IToken.Kind op = unaryExpr.getOp();
        Type exprType = (Type) unaryExpr.getE().visit(this, arg);
        Type resultType = null;

        switch(op) {
            case BANG -> {
                if (exprType == Type.INT) resultType = Type.INT;
                else if (exprType == Type.PIXEL) resultType = Type.PIXEL;
                check(false, unaryExpr, "invalid Expr type");
            }
            case MINUS, RES_cos, RES_sin, RES_atan -> {
                check(exprType == Type.INT, unaryExpr, "invalid Expr type");
                resultType = Type.INT;
            }
        }
        return resultType;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        stringLitExpr.setType(Type.STRING);
        return Type.STRING;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException {
        numLitExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        String name = identExpr.getName();
        NameDef nameDef = symbolTable.lookup(name);
        check(nameDef != null, identExpr, "undefined identifier " + name);
        identExpr.setNameDef(nameDef);
        Type type = nameDef.getType();
        identExpr.setType(type);
        return type;
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException {
        zExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitRandomExpr(RandomExpr randomExpr, Object arg) throws PLCException {
        randomExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        Expr x = pixelSelector.getX();
        Expr y = pixelSelector.getY();

        check(x.getType() == Type.INT, pixelSelector, "Expr x != INT");
        check(y.getType() == Type.INT, pixelSelector, "Expr y != INT");

        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        Expr red = expandedPixelExpr.getRedExpr();
        Expr green = expandedPixelExpr.getGrnExpr();
        Expr blue = expandedPixelExpr.getBluExpr();

        check(red.getType() == Type.INT, expandedPixelExpr, "Expr red != INT");
        check(green.getType() == Type.INT, expandedPixelExpr, "Expr grn != INT");
        check(blue.getType() == Type.INT, expandedPixelExpr, "Expr blu != INT");

        expandedPixelExpr.setType(Type.PIXEL);
        return Type.PIXEL;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        Expr height = dimension.getHeight();
        Expr width = dimension.getWidth();

        check(height.getType() == Type.INT, dimension, "Expr height != INT");
        check(width.getType() == Type.INT, dimension, "Expr width != INT");
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        String name = lValue.getIdent().getName();
        NameDef nameDef = symbolTable.lookup(name);
        Type returnType = null;
        check(nameDef != null, lValue, "ident not declared or not visible in scope");
        Type identType = nameDef.getType();

        if (identType == Type.IMAGE) {
            if(lValue.getPixelSelector() == null && lValue.getColor() == null) returnType = Type.IMAGE;
            else if(lValue.getPixelSelector() != null && lValue.getColor() == null) returnType = Type.PIXEL;
            else if(lValue.getPixelSelector() == null && lValue.getColor() != null) returnType = Type.IMAGE;
            else if(lValue.getPixelSelector() != null && lValue.getColor() != null) returnType = Type.INT;
            else check(false, lValue, "invalid combination of pixel and channel selector");
        }
        else if (identType == Type.PIXEL) {
            if(lValue.getPixelSelector() == null && lValue.getColor() == null) returnType = Type.PIXEL;
            else if(lValue.getPixelSelector() == null && lValue.getColor() != null) returnType = Type.INT;
            else check(false, lValue, "invalid combination of pixel and channel selector");
        }
        else if (identType == Type.STRING) {
            if(lValue.getPixelSelector() == null && lValue.getColor() == null) returnType = Type.STRING;
            else check(false, lValue, "invalid combination of pixel and channel selector");
        }
        else if (identType == Type.INT) {
            if(lValue.getPixelSelector() == null && lValue.getColor() == null) returnType = Type.INT;
            else check(false, lValue, "invalid combination of pixel and channel selector");
        }

        return returnType;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        Expr returnExpr = returnStatement.getE();
        check(returnExpr.getType() == programType, returnStatement, "program type does not match return expr type");
        return returnStatement.getE().getType();
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLCException {
        return null;
    }

}
