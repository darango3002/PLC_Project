package edu.ufl.cise.plcsp23.ast;
import edu.ufl.cise.plcsp23.TypeCheckException;
import edu.ufl.cise.plcsp23.PLCException;

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
        HashMap<String,Declaration> entries = new HashMap<>();
        //returns true if name successfully inserted in symbol table, false if already present
        public boolean insert(String name, Declaration declaration) {
            return (entries.putIfAbsent(name,declaration) == null);
        }
        //returns Declaration if present, or null if name not declared.
        public Declaration lookup(String name) {
            return entries.get(name);
        }
    }
    SymbolTable symbolTable = new SymbolTable();
    private void check(boolean condition, AST node, String message)
            throws TypeCheckException {
        if (! condition) { throw new TypeCheckException(message, node.getSourceLoc()); }
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {
        return null;
    }

    private boolean assignmentCompatible(Type LValueType, Type initializerType) {

        if (LValueType == Type.IMAGE) {
            return (initializerType == Type.IMAGE || initializerType == Type.PIXEL || initializerType == Type.STRING);
        }
        else if (LValueType == Type.PIXEL) {
            return (initializerType == Type.PIXEL || initializerType == Type.INT);
        }
        else if (LValueType == Type.INT) {
            return (initializerType == Type.INT || initializerType == Type.PIXEL);
        }
        else { // LValueType == Type.STRING
            return (initializerType == Type.STRING || initializerType == Type.INT || initializerType == Type.PIXEL
            || initializerType == Type.IMAGE);
        }
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        String name = declaration.getName();
        boolean inserted = symbolTable.insert(name,declaration);
        check(inserted, declaration, "variable " + name + "already declared");
        Expr initializer = declaration.getInitializer();
        if (initializer != null) {
            //infer type of initializer
            Type initializerType = (Type) initializer.visit(this,arg);
            check(assignmentCompatible(declaration.getType(), initializerType),declaration,
                    "type of expression and declared type do not match");
            declaration.setAssigned(true);
        }
        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitRandomExpr(RandomExpr randomExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException {
        return null;
    }


}
