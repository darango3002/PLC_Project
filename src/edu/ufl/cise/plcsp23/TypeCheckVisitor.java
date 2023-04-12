package edu.ufl.cise.plcsp23;
import edu.ufl.cise.plcsp23.ast.*;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.ArrayList;

public class TypeCheckVisitor implements ASTVisitor {

    Type programType = null;
    public static class SymbolTable {

        public static class Pair<N extends AST, I extends Number> {
            NameDef nameDef;
            Integer scopeID;

            Pair(NameDef nameDef, Integer scopeID) {
                this.nameDef = nameDef;
                this.scopeID = scopeID;
            }

            public NameDef getNameDef() {
                return nameDef;
            }
            public Integer getScopeID() {
                return scopeID;
            }
        }
        int currentNum;
        Stack<Integer> scopeStack = new Stack<Integer>();

        void enterScope() {
            currentNum++;
            scopeStack.push(currentNum);
        }
        void closeScope() {
            currentNum = scopeStack.pop();
        }
        HashMap<String, ArrayList<Pair<NameDef, Integer>>> entries = new HashMap<>();
        //returns true if name successfully inserted in symbol table, false if already present
        public boolean insert(String name, NameDef nameDef) {
            if (entries.get(name) == null) {
                ArrayList<Pair<NameDef, Integer>> temp = new ArrayList<>();
                Pair<NameDef, Integer> pair = new Pair<>(nameDef, currentNum);
                temp.add(pair);
                entries.put(name, temp);
                return true;
            }
            else{
                ArrayList<Pair<NameDef, Integer>> temp = entries.get(name);
                for (Pair<NameDef, Integer> pair : temp) {
                    if (pair.getScopeID() == currentNum) {
                        return false;
                    }
                }
                Pair<NameDef, Integer> pair = new Pair<>(nameDef, currentNum);
                temp.add(pair);
                entries.put(name, temp);
                return true;
            }
        }
        //returns NameDef if present, or null if name not declared.
        //closest NameDef to scopeNum
        public NameDef lookup(String name) {
            if (entries.get(name) == null) return null;
            else {
                ArrayList<Pair<NameDef, Integer>> temp = entries.get(name);
                Pair<NameDef, Integer> current = null;
                int highScope = 0;
                for (Pair<NameDef, Integer> pair : temp) {
                    if (pair.getScopeID() > currentNum) {
                        continue;
                    }
                    else if (pair.scopeID > highScope && scopeStack.search(pair.scopeID) != -1) {
                        current = pair;
                        highScope = pair.getScopeID();
                    }
                }

                if (current != null)
                    return current.getNameDef();
                else
                    return null;
            }
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
        else if (targetType == Type.STRING){
            return (rhsType == Type.STRING || rhsType == Type.INT || rhsType == Type.PIXEL
                    || rhsType == Type.IMAGE);
        }
        else
            return false;
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
            initializer.setType(initializerType);
        }
        if (declaration.getNameDef().getType() == Type.IMAGE) {
            check(declaration.getInitializer() != null || declaration.getNameDef().getDimension() != null,
                    declaration, "Initializer and/or dimension cannot be null || Init: " + declaration.getInitializer()
                            + ", Dim: " + declaration.getNameDef().getDimension());
        }

        return declaration.getNameDef().visit(this, arg);
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException {
        String name = nameDef.getIdent().getName();
        boolean inserted = symbolTable.insert(name, nameDef);
        check(inserted, nameDef, "variable " + name + " already declared");

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
        primaryExpr.setType((Type)primaryExpr.visit(this, arg));
        Type resultType = null;
        if (unaryExprPostfix.getPixel() != null) {
            unaryExprPostfix.getPixel().visit(this, arg);
        }

        if (primaryExpr.getType() == Type.PIXEL){
            if (unaryExprPostfix.getPixel() == null && unaryExprPostfix.getColor() != null)
                resultType = Type.INT;
        }
        else if (primaryExpr.getType() == Type.IMAGE) {
            if (unaryExprPostfix.getPixel() == null && unaryExprPostfix.getColor() != null)
                resultType = Type.IMAGE;
            else if (unaryExprPostfix.getPixel() != null && unaryExprPostfix.getColor() == null)
                resultType = Type.PIXEL;
            else if (unaryExprPostfix.getPixel() != null && unaryExprPostfix.getColor() != null)
                resultType = Type.INT;
        }

        return resultType;
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        pixelFuncExpr.getSelector().visit(this, arg);
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
        Expr expr0 = conditionalExpr.getGuard();
        expr0.setType((Type)expr0.visit(this,arg));
        Expr expr1 = conditionalExpr.getTrueCase();
        expr1.setType((Type)expr1.visit(this,arg));
        Expr expr2 = conditionalExpr.getFalseCase();
        expr2.setType((Type)expr2.visit(this,arg));

        check(expr0.getType() == Type.INT, conditionalExpr, "Expr0 type != INT :: " + expr1.getFirstToken().getTokenString() +" ");
        check(expr1.getType() == expr2.getType(), conditionalExpr, "Types Expr1 != Expr2");

        conditionalExpr.setType(expr1.getType());
        return expr1.getType();
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        IToken.Kind op = binaryExpr.getOp();
        Type leftType = (Type) binaryExpr.getLeft().visit(this, arg);
        binaryExpr.getLeft().setType(leftType);
        Type rightType = (Type) binaryExpr.getRight().visit(this, arg);
        binaryExpr.getRight().setType(rightType);
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
        unaryExpr.getE().setType(exprType);
        Type resultType = null;

        switch(op) {
            case BANG -> {
                if (exprType == Type.INT) resultType = Type.INT;
                else if (exprType == Type.PIXEL) resultType = Type.PIXEL;
                else check(false, unaryExpr, "invalid Expr type");
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
        x.setType((Type)x.visit(this,arg));
        Expr y = pixelSelector.getY();
        y.setType((Type)y.visit(this,arg));

        check(x.getType() == Type.INT, pixelSelector, "Expr x != INT");
        check(y.getType() == Type.INT, pixelSelector, "Expr y != INT");

        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        Expr red = expandedPixelExpr.getRedExpr();
        red.setType((Type)red.visit(this,arg));
        Expr green = expandedPixelExpr.getGrnExpr();
        green.setType((Type)green.visit(this,arg));
        Expr blue = expandedPixelExpr.getBluExpr();
        blue.setType((Type)blue.visit(this,arg));

        check(red.getType() == Type.INT, expandedPixelExpr, "Expr red != INT");
        check(green.getType() == Type.INT, expandedPixelExpr, "Expr grn != INT");
        check(blue.getType() == Type.INT, expandedPixelExpr, "Expr blu != INT");

        expandedPixelExpr.setType(Type.PIXEL);
        return Type.PIXEL;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        Expr height = dimension.getHeight();
        height.setType((Type)height.visit(this,arg));
        Expr width = dimension.getWidth();
        width.setType((Type)width.visit(this,arg));

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

        if(lValue.getPixelSelector() != null)
            lValue.getPixelSelector().visit(this, arg);

        if (identType == Type.IMAGE) {
            if(lValue.getPixelSelector() == null && lValue.getChannelSelector() == null) returnType = Type.IMAGE;
            else if(lValue.getPixelSelector() != null && lValue.getChannelSelector() == null) returnType = Type.PIXEL;
            else if(lValue.getPixelSelector() == null && lValue.getChannelSelector() != null) returnType = Type.IMAGE;
            else if(lValue.getPixelSelector() != null && lValue.getChannelSelector() != null) returnType = Type.INT;
            else check(false, lValue, "invalid combination of pixel and channel selector");
        }
        else if (identType == Type.PIXEL) {
            if(lValue.getPixelSelector() == null && lValue.getChannelSelector() == null) returnType = Type.PIXEL;
            else if(lValue.getPixelSelector() == null && lValue.getChannelSelector() != null) returnType = Type.INT;
            else check(false, lValue, "invalid combination of pixel and channel selector");
        }
        else if (identType == Type.STRING) {
            if(lValue.getPixelSelector() == null && lValue.getChannelSelector() == null) returnType = Type.STRING;
            else check(false, lValue, "invalid combination of pixel and channel selector");
        }
        else if (identType == Type.INT) {
            if(lValue.getPixelSelector() == null && lValue.getChannelSelector() == null) returnType = Type.INT;
            else check(false, lValue, "invalid combination of pixel and channel selector");
        }

        return returnType;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        String name = statementAssign.getLv().getIdent().getName();
        NameDef nameDef = symbolTable.lookup(name);
        Type exprType = (Type) statementAssign.getE().visit(this, arg);
        statementAssign.getE().setType(exprType);
        Type lvalueType = (Type) statementAssign.getLv().visit(this, arg);
//        System.out.println(lvalueType);
        statementAssign.getLv().setlValueType(lvalueType);

        check(assignmentCompatible(lvalueType, statementAssign.getE().getType()), statementAssign, "left and right values do not match (L) " + nameDef.getType()+ " (R) " + statementAssign.getE().getType());
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException {
        Expr e = statementWrite.getE();
        e.setType((Type)e.visit(this,arg));
        return null;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException {
        Expr e = whileStatement.getGuard();
        Block block = whileStatement.getBlock();
        e.setType((Type)e.visit(this,arg));

        check(e.getType() == Type.INT, whileStatement, "Guard EXPR != INT");
        symbolTable.enterScope();
        block.visit(this, arg);
        symbolTable.closeScope();
        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        Expr returnExpr = returnStatement.getE();
        returnStatement.getE().setType((Type) returnExpr.visit(this, arg));
        boolean t = assignmentCompatible(programType,returnStatement.getE().getType());
        check(t, returnStatement, "program type (" + programType.name() + ") is not compatible w/ expr type (" + returnExpr.getType().name() + ")");
        return returnExpr.getType();
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLCException {
        return null;
    }

}
