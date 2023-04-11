package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.IToken.Kind;

import java.util.List;
import java.lang.Math.*;
import java.util.Objects;

public class CodeGenerator implements ASTVisitor {

    String imports;
    StringBuilder sb;

    public CodeGenerator() {
        imports = "";
        sb = new StringBuilder();
    }

    protected String getJavaType(Type type) throws PLCException{
        if (type == Type.INT) {
            return "int";
        }
        else if (type == Type.STRING) {
            return "String";
        }
        else if (type == Type.VOID) {
            return "void";
        }
        else
            throw new PLCException("unimplemented type to java type");
    }

    protected String getJavaOp(Kind kind) throws PLCException{
        String op = switch (kind) {
            case PLUS -> "+";
            case MINUS -> "-";
            case TIMES -> "*";
            case DIV -> "/";
            case MOD -> "%";
            case LT -> "<";
            case GT -> ">";
            case LE -> "<=";
            case GE -> ">=";
            case EQ -> "==";
            case OR -> "||";
            case BITOR -> "|";
            case AND -> "&&";
            case BITAND -> "&";
            case EXP -> "**";
            //TODO: implement how each operator functions according to assignment PDF
            default -> throw new PLCException("unimplemented java op");
        };

        return op;
    }

    protected boolean isKind(Kind opKind, Kind... kinds) {
        for (Kind k : kinds) {
            if (k == opKind)
                return true;
        }
        return false;
    }

    public void generateApplyMethod(Program program, Object arg) throws PLCException {
        String javaProgramType = getJavaType(program.getType());
        List<NameDef> paramList = program.getParamList();
        Block block = program.getBlock();

        sb.append("public static ");
        sb.append(javaProgramType).append(" apply(");
        for (NameDef param : paramList) {
            param.visit(this, arg);
            sb.append(", ");
        }

        if (paramList.size() > 0)
            sb.setLength(sb.length() - 2); // removes unwanted ', ' from parameter list

        sb.append(") {\n");
        block.visit(this, arg);
        sb.append("}\n");
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCException {
        String name = program.getIdent().getName();

        sb.append("public class ");
        sb.append(name).append("{\n");
        generateApplyMethod(program, arg);
        sb.append("}");

        return sb.toString();
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCException {
        List<Declaration> decList = block.getDecList();
        List<Statement> stateList = block.getStatementList();

        for (Declaration dec: decList) {
            dec.visit(this, arg);
            sb.append(";\n");
        }
        for (Statement statement: stateList) {
            statement.visit(this, arg);
            sb.append(";\n");
        }

        return null;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        NameDef nameDef = declaration.getNameDef();
        Expr expr = declaration.getInitializer();

        nameDef.visit(this, arg);
        if (expr != null) {
            sb.append(" = ");
            expr.visit(this, arg);
        }

        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException {
        String javaType = getJavaType(nameDef.getType());
        String name = nameDef.getIdent().getName();

        sb.append(javaType).append(" ").append(name);
        return null;
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLCException {
        // NOT SURE IF WE NEED TO IMPLEMENT THIS ?
        return null;
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {
        Expr guard = conditionalExpr.getGuard();
        Expr trueCase = conditionalExpr.getTrueCase();
        Expr falseCase = conditionalExpr.getFalseCase();

        sb.append("(");
        guard.visit(this,arg);
        sb.append(" != 0 ? ");
        trueCase.visit(this, arg);
        sb.append(" : ");
        falseCase.visit(this, arg);
        sb.append(")");

        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        Expr expr0 = binaryExpr.getLeft();
        Expr expr1 = binaryExpr.getRight();
        Kind opKind = binaryExpr.getOp();
        String javaOp = getJavaOp(binaryExpr.getOp());

        sb.append("(");

        if (isKind(opKind, Kind.OR, Kind.AND)) { // is a boolean expr
            expr0.visit(this, arg);
            sb.append(" != 0 ");
            sb.append(javaOp).append(" ");
            expr1.visit(this, arg);
            sb.append(" != 0) ? 1 : 0");
        }
        else if (isKind(opKind, Kind.LT, Kind.GT, Kind.LE, Kind.GE, Kind.EQ)) {
            expr0.visit(this, arg);
            sb.append(javaOp);
            expr1.visit(this, arg);
            sb.append(") ? 1 : 0");
        }
        else if (isKind(opKind, Kind.EXP)) { // is an exponent
            // maybe needs import?
            sb.append("(int)Math.pow(");
            expr0.visit(this, arg);
            sb.append(", ");
            expr1.visit(this, arg);
            sb.append("))");
        }
        else {
            expr0.visit(this, arg);
            sb.append(javaOp);
            expr1.visit(this, arg);
            sb.append(")");
        }


        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        String name = stringLitExpr.getValue();

        sb.append("\"");
        sb.append(name);
        sb.append("\"");
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        String name = identExpr.getName();
        sb.append(name);
        return null;
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException {
        sb.append(255);
        return null;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException {
        int numLit = numLitExpr.getValue();
        sb.append(numLit);
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

    // STATEMENTS //
    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        PixelSelector pixel = lValue.getPixelSelector();
        ColorChannel channel = lValue.getChannelSelector();
        String name = lValue.getIdent().getName();

        sb.append(name);
        // ONLY HANDLES CASE WHERE PIXEL AND CHANNEL ARE NULL
        if (pixel != null)
            throw new PLCException("Not implemented yet");
        if (channel != null)
            throw new PLCException("Not implemented yet");

        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        LValue lvalue = statementAssign.getLv();
        Expr expr = statementAssign.getE();

        lvalue.visit(this, arg);
        sb.append(" = ");
        expr.visit(this, arg);
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
        Expr expr = returnStatement.getE();

        sb.append("return ");
        expr.visit(this, arg);
        return null;
    }


}
