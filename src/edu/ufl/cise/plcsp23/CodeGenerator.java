package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.IToken.Kind;
import edu.ufl.cise.plcsp23.runtime.ImageOps;
import edu.ufl.cise.plcsp23.runtime.PLCRuntimeException;

import java.util.List;
import java.lang.Math.*;

public class CodeGenerator implements ASTVisitor {

    String imports;
    Type returnType;
    StringBuilder sb;
    String packageName = "";

    public CodeGenerator(String packageName) {
        imports = "";
        sb = new StringBuilder();
        this.packageName = packageName;

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
        else if (type == Type.IMAGE) {
            if (imports.indexOf("import java.awt.image.BufferedImage;") == -1) {
                imports += "import java.awt.image.BufferedImage;\n";
            }
            return "BufferedImage";
        }
        else if (type == Type.PIXEL) {
//            if (imports.indexOf("import java.awt.image.BufferedImage;") == -1) {
//                imports += "import java.awt.image.BufferedImage;\n";
//            }
            return "int";
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
        returnType = program.getType();
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

        // Add imports
        sb.insert(0, imports + "\n");
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCException {
        if (packageName != "" || packageName != null) {
            sb.append(packageName);
        }

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
        Dimension dim = nameDef.getDimension();

        nameDef.visit(this, arg);
        if (expr != null) {
            sb.append(" = ");
            if (nameDef.getType() == Type.IMAGE) {
                if (dim == null) {
                    if (expr.getType() == Type.STRING) {
                        sb.append("FileURLIO.readImage(");
                        expr.visit(this, arg);
                        sb.append(")");

                        if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.FileURLIO;") == -1) {
                            imports += "import edu.ufl.cise.plcsp23.runtime.FileURLIO;\n";
                        }
                    }
                    if (expr.getType() == Type.IMAGE) {
                        sb.append("ImageOps.cloneImage(");
                        expr.visit(this, arg);
                        sb.append(")");

                        if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps;") == -1) {
                            imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
                        }
                    }

                }
                else { // dimension != null
                    if (expr.getType() == Type.STRING) {
                        sb.append("FileURLIO.readImage(");
                        expr.visit(this, arg);
                        sb.append(", ");
                        dim.visit(this,arg);
                        sb.append(")");

                        if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.FileURLIO;") == -1) {
                            imports += "import edu.ufl.cise.plcsp23.runtime.FileURLIO;\n";
                        }
                    }
                    if (expr.getType() == Type.IMAGE) {
                        sb.append("ImageOps.copyAndResize(");
                        expr.visit(this, arg);
                        sb.append(", ");
                        dim.visit(this,arg);
                        sb.append(")");

                        if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps;") == -1) {
                            imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
                        }
                    }
                    if (expr.getType() == Type.PIXEL) {
                        sb.append("ImageOps.makeImage(");
                        dim.visit(this, arg);
                        sb.append(");\n");
                        sb.append("ImageOps.setAllPixels(");
                        sb.append(nameDef.getIdent().getName());
                        sb.append(", ");
                        expr.visit(this, arg);
                        sb.append(")");

                        if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps;") == -1) {
                            imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
                        }
                        if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.PixelOps;") == -1) {
                            imports += "import edu.ufl.cise.plcsp23.runtime.PixelOps;\n";
                        }
                    }

                }
            }
            else if (nameDef.getType() == Type.STRING && expr.getType() == Type.INT) {
                sb.append("Integer.toString(");
                expr.visit(this, arg);
                sb.append(")");
            }
            else {
                expr.visit(this, arg);
            }
        }
        else if (expr == null && nameDef.getType() == Type.IMAGE) { // no initializer expr but image type on LHS
            sb.append(" = ");
            sb.append("ImageOps.makeImage(");
            dim.visit(this,arg);
            sb.append(")");

            if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps;") == -1) {
                imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
            }
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
        Expr primaryExpr = unaryExprPostfix.getPrimary();
        PixelSelector pixl = unaryExprPostfix.getPixel();
        ColorChannel chnl = unaryExprPostfix.getColor();

        if (primaryExpr.getType() == Type.IMAGE) {
            if (pixl != null & chnl == null) {
                sb.append("ImageOps.getRGB(");
                primaryExpr.visit(this, arg);
                sb.append(", ");
                pixl.visit(this, arg);
                sb.append(")");

                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps;") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
                }
            }
            else if (pixl != null & chnl != null) {
                sb.append("PixelOps.");
                sb.append(chnl.name());
                sb.append("(ImageOps.getRGB(");
                primaryExpr.visit(this,arg);
                sb.append(", ");
                pixl.visit(this,arg);
                sb.append("))");

                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps;") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
                }
                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.PixelOps;") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.PixelOps;\n";
                }
            }
            else if (pixl == null & chnl != null) {
                sb.append("ImageOps.extract");
                sb.append(chnl.name().substring(0, 1).toUpperCase()); // capitalizes first char of color
                sb.append(chnl.name().substring(1)); // rest of color
                sb.append("(");
                primaryExpr.visit(this, arg);
                sb.append(")");

                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps;") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
                }
            }
            else {
                throw new PLCRuntimeException("unsupported combination of pixel and channel selector for IMAGE type");
            }
        }
        else if (primaryExpr.getType() == Type.PIXEL) {
            if (pixl == null & chnl != null) {
                sb.append("PixelOps.");
                sb.append(chnl.name());
                sb.append("(");
                primaryExpr.visit(this, arg);
                sb.append(")");

                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.PixelOps;") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.PixelOps;\n";
                }
            }
            else {
                throw new PLCRuntimeException("unsupported combination of pixel and channel selector for PIXEL type");
            }
        }

        return null;
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        throw new PLCException("NOT GOING TO IMPLEMENT");
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        Kind kind = predeclaredVarExpr.getKind();

        if (kind == Kind.RES_x) {
            sb.append("x");
        }
        else if (kind == Kind.RES_y) {
            sb.append("y");
        }

        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {
        Expr guard = conditionalExpr.getGuard();
        Expr trueCase = conditionalExpr.getTrueCase();
        Expr falseCase = conditionalExpr.getFalseCase();

        sb.append("((");
        guard.visit(this,arg);
        sb.append(") != 0 ? ");
        trueCase.visit(this, arg);
        sb.append(" : ");
        falseCase.visit(this, arg);
        sb.append(")");

        return null;
    }

//    private String getImageOp(Kind kind) {
//        String op = switch (kind) {
//            case PLUS -> "ImageOps.OP.PLUS";
//            default -> null;
//        };
//        return op;
//    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        Expr expr0 = binaryExpr.getLeft();
        Expr expr1 = binaryExpr.getRight();
        Kind opKind = binaryExpr.getOp();
        String javaOp = getJavaOp(binaryExpr.getOp());

//        System.out.println(expr0.getType() + " " + expr1.getType() + " " + binaryExpr.getOp());

        if (isKind(opKind, Kind.OR, Kind.AND)) { // is a boolean expr
            sb.append("(");
            expr0.visit(this, arg);
            sb.append(" != 0 ");
            sb.append(javaOp).append(" ");
            expr1.visit(this, arg);
            sb.append(" != 0) ? 1 : 0");
        }
        else if (isKind(opKind, Kind.LT, Kind.GT, Kind.LE, Kind.GE, Kind.EQ)) {
            if (expr0.getType() == Type.IMAGE && expr1.getType() == Type.IMAGE && isKind(opKind, Kind.EQ)) {
                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
                }
                sb.append("(");
                sb.append("(");
                sb.append("ImageOps.equals(");
                expr0.visit(this, arg);
                sb.append(", ");
                expr1.visit(this, arg);
                sb.append(")) ? 1 : 0)");
            }
            else {
                sb.append("(");
                sb.append("(");
                expr0.visit(this, arg);
                sb.append(javaOp);
                expr1.visit(this, arg);
                sb.append(") ? 1 : 0)");
            }

        }
        else if (isKind(opKind, Kind.EXP)) { // is an exponent
            sb.append("(");
            sb.append("(int)Math.pow(");
            expr0.visit(this, arg);
            sb.append(", ");
            expr1.visit(this, arg);
            sb.append("))");
        }
        else {
            if (expr0.getType() == Type.IMAGE && expr1.getType() == Type.IMAGE) {
                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
                }
                sb.append("ImageOps.binaryImageImageOp(");
                sb.append("ImageOps.OP.");
                sb.append(binaryExpr.getOp());
                sb.append(", ");
                expr0.visit(this, arg);
                sb.append(", ");
                expr1.visit(this, arg);
                sb.append(")");
            }
            else if (expr0.getType() == Type.IMAGE && expr1.getType() == Type.INT) {
                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
                }
                sb.append("ImageOps.binaryImageScalarOp(");
                sb.append("ImageOps.OP.");
                sb.append(binaryExpr.getOp());
                sb.append(", ");
                expr0.visit(this, arg);
                sb.append(", ");
                expr1.visit(this, arg);
                sb.append(")");
            }
            else if (expr0.getType() == Type.PIXEL && expr1.getType() == Type.PIXEL) {
                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
                }
                sb.append("ImageOps.binaryPackedPixelPixelOp(");
                sb.append("ImageOps.OP.");
                sb.append(binaryExpr.getOp());
                sb.append(", ");
                expr0.visit(this, arg);
                sb.append(", ");
                expr1.visit(this, arg);
                sb.append(")");
            }
            else if (expr0.getType() == Type.PIXEL && expr1.getType() == Type.INT) {
                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
                }
                sb.append("ImageOps.binaryPackedPixelIntOp(");
                sb.append("ImageOps.OP.");
                sb.append(binaryExpr.getOp());
                sb.append(", ");
                expr0.visit(this, arg);
                sb.append(", ");
                expr1.visit(this, arg);
                sb.append(")");
            }
            else if (expr0.getType() == Type.IMAGE && expr1.getType() == Type.PIXEL) {
                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
                }
                sb.append("ImageOps.binaryImagePixelOp(");
                sb.append("ImageOps.OP.");
                sb.append(binaryExpr.getOp());
                sb.append(", ");
                expr0.visit(this, arg);
                sb.append(", ");
                expr1.visit(this, arg);
                sb.append(")");
            }
            else {
                sb.append("(");
                expr0.visit(this, arg);
                sb.append(javaOp);
                expr1.visit(this, arg);
                sb.append(")");
            }
        }


        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        Expr expr = unaryExpr.getE();
        Kind op = unaryExpr.getOp();

        if (expr.getType() == Type.INT) {
            if (isKind(op, Kind.BANG)) {
                sb.append("(");
                expr.visit(this, arg);
                sb.append(" == 0 ? 1 : 0)");
            }
            else if (isKind(op, Kind.MINUS)) {
                sb.append("-");
                expr.visit(this, arg);
            }
        }
        else
            throw new PLCRuntimeException("Unary expr is not of type: INT");

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
        int randInt = (int)(Math.floor(Math.random() * 256));
        sb.append(randInt);

        if(imports.indexOf("import java.lang.Math.*;") == -1) {
            imports += "import java.lang.Math.*;\n";
        }
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        Expr expr0 = pixelSelector.getX();
        Expr expr1 = pixelSelector.getY();

        expr0.visit(this, arg);
        sb.append(", ");
        expr1.visit(this, arg);

        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        sb.append("PixelOps.pack(");
        expandedPixelExpr.getRedExpr().visit(this, arg);
        sb.append(", ");
        expandedPixelExpr.getGrnExpr().visit(this, arg);
        sb.append(", ");
        expandedPixelExpr.getBluExpr().visit(this, arg);
        sb.append(")");

        if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.PixelOps") == -1) {
            imports += "import edu.ufl.cise.plcsp23.runtime.PixelOps;\n";
        }
        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        dimension.getWidth().visit(this, arg);
        sb.append(", ");
        dimension.getHeight().visit(this, arg);
        return null;
    }

    // STATEMENTS //
    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        String name = lValue.getIdent().getName();

        sb.append(name);

        // ONLY HANDLES CASE WHERE PIXEL AND CHANNEL ARE NULL

        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        LValue lvalue = statementAssign.getLv();
        Expr expr = statementAssign.getE();
        PixelSelector pixel = lvalue.getPixelSelector();
        ColorChannel channel = lvalue.getChannelSelector();



        if (lvalue.getlValueType() == Type.STRING && expr.getType() == Type.INT) {
            lvalue.visit(this, arg);
            sb.append(" = ");
            sb.append("Integer.toString(");
            expr.visit(this, arg);
            sb.append(")");
        }
        else if (lvalue.getlValueType() == Type.PIXEL && pixel == null && channel == null) {
            lvalue.visit(this, arg);
            sb.append(" = ");
            //sb.append("PixelOps.pack(");
            expr.visit(this, arg);
           // sb.append(")");

            if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.PixelOps") == -1) {
                imports += "import edu.ufl.cise.plcsp23.runtime.PixelOps;\n";
            }
        }
        else if (lvalue.getlValueType() == Type.IMAGE && pixel == null && channel == null) {
            if (expr.getType() == Type.STRING) {
                sb.append("ImageOps.copyInto(FileURLIO.readImage(");
                expr.visit(this, arg);
                sb.append("), ");
                lvalue.visit(this, arg);
                sb.append(")");

                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.FileURLIO") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.FileURLIO;\n";
                }
            }
            else if (expr.getType() == Type.IMAGE) {
                sb.append("ImageOps.copyInto(");
                expr.visit(this, arg);
                sb.append(", ");
                lvalue.visit(this, arg);
                sb.append(")");

                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
                }
            }
            else if (expr.getType() == Type.PIXEL) {
                sb.append("ImageOps.setAllPixels(");
                lvalue.visit(this, arg);
                sb.append(", ");
                expr.visit(this, arg);
                sb.append(")");

                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ImageOps") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n";
                }
                if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.PixelOps") == -1) {
                    imports += "import edu.ufl.cise.plcsp23.runtime.PixelOps;\n";
                }
            }
        }
        else if (lvalue.getlValueType() == Type.PIXEL && pixel != null && channel == null) {
            sb.append("for (int y = 0; y != ");
            lvalue.visit(this, arg);
            sb.append(".getHeight(); y++){\n");
            sb.append("for (int x = 0; x != ");
            lvalue.visit(this, arg);
            sb.append(".getWidth(); x++){\n");
            sb.append("ImageOps.setRGB(");
            lvalue.visit(this, arg);
            sb.append(", x, y, ");
            expr.visit(this, arg);
//            sb.append(", ");
//            expr.visit(this,arg);
            sb.append("); }\n}\n");

        }
        else if (lvalue.getlValueType() == Type.INT && pixel != null && channel != null) {
            sb.append("for (int y = 0; y != ");
            lvalue.visit(this, arg);
            sb.append(".getHeight(); y++){\n");
            sb.append("for (int x = 0; x != ");
            lvalue.visit(this, arg);
            sb.append(".getWidth(); x++){\n");
            sb.append("ImageOps.setRGB(");
            lvalue.visit(this, arg);
            sb.append(", x, y, ");
            sb.append("PixelOps.set");
            sb.append(channel.name().substring(0, 1).toUpperCase()); // capitalizes first char of color
            sb.append(channel.name().substring(1)); // rest of color
            sb.append("(");
            sb.append("ImageOps.getRGB(");
            lvalue.visit(this, arg);
            sb.append(", ");
            pixel.visit(this, arg);
            sb.append(")");
            sb.append(", ");
            expr.visit(this, arg);
            sb.append("); }\n}\n");
        }
        else {
            lvalue.visit(this, arg);
            sb.append(" = ");
            expr.visit(this, arg);
        }
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException {
        Expr expr = statementWrite.getE();

        sb.append("ConsoleIO.write(");
        expr.visit(this, arg);
        sb.append(")");

        if (imports.indexOf("import edu.ufl.cise.plcsp23.runtime.ConsoleIO") == -1) {
            imports += "import edu.ufl.cise.plcsp23.runtime.ConsoleIO;\n";
        }
        return null;
    }

    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException {
        Expr guard = whileStatement.getGuard();
        Block block = whileStatement.getBlock();

        sb.append("while ((");
        guard.visit(this, arg);
        sb.append(") != 0) {\n");
        block.visit(this, arg);
        sb.append("}");

        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        Expr expr = returnStatement.getE();

        sb.append("return ");
        if (returnType == Type.STRING && expr.getType() == Type.INT) {
            sb.append("Integer.toString(");
            expr.visit(this, arg);
            sb.append(")");
        }
        else {
            expr.visit(this, arg);
        }
        return null;
    }


}
