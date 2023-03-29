package edu.ufl.cise.plcsp23.ast;
import edu.ufl.cise.plcsp23.TypeCheckException;
import edu.ufl.cise.plcsp23.PLCException;
import edu.ufl.cise.plcsp23.IToken;

import java.util.HashMap;
import java.util.Stack;
import java.util.ArrayList;

public class TypeCheckVisitor implements ASTVisitor {
    public static class SymbolTable {
        int currentNum;
        int nextNum;
        Stack<Integer> scopeStack;

        void enterScope() {
            currentNum = nextNum++;
            scopeStack.push(currentNum);
        }
        void closeScope() {
            currentNum = scopeStack.pop();
        }
        // TODO: Eventually need to update HashMap to handle while loops (nested scopes)
        // use ArrayList<Pair<Declaration, int (scopeID)>> to implement the values
        HashMap<String,NameDef> entries = new HashMap<>();
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
        return null;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        Expr initializer = declaration.getInitializer();
        if (initializer != null) {
            //infer type of initializer
            Type initializerType = (Type) initializer.visit(this,arg);
            check(assignmentCompatible(declaration.getType(), initializerType) ,declaration,
                    "type of expression and declared type do not match");
        }
        if (declaration.getType() == Type.IMAGE) {
            check(declaration.getInitializer() != null || declaration.getNameDef().getDimension() != null,
                    declaration, "Initializer and/or dimension cannot be null || Init: " + declaration.getInitializer()
                            + ", Dim: " + declaration.getNameDef().getDimension());
        }
        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException {
        String name = nameDef.getIdent().getName();
        boolean inserted = symbolTable.insert(name,nameDef);
        check(inserted, nameDef, "variable " + name + "already declared");

        if (nameDef.getDimension() != null) {
            check(nameDef.getType() == Type.IMAGE, nameDef, "type is not IMAGE");
        }
        check(nameDef.getType() != null, nameDef, "type of nameDef is null");
        check(symbolTable.lookup(name) == null, nameDef, "name was previously defined in scope");

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
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        String name = identExpr.getName();
        NameDef nameDef = symbolTable.lookup(name);
        check(nameDef != null, identExpr, "undefined identifier " + name);
        check(nameDef.isAssigned(), identExpr, "using uninitialized variable");
        identExpr.setNameDef(nameDef);
        Type type = nameDef.getType();
        identExpr.setType(type);
        return type;
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitRandomExpr(RandomExpr randomExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        return null;
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
        return null;
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLCException {
        return null;
    }

}
