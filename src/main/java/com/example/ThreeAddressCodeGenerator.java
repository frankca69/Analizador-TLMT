package com.example;

import com.example.ast.*;
import java.util.ArrayList;
import java.util.List;

public class ThreeAddressCodeGenerator {

    private int labelCount = 1;
    private int tempCount = 1;
    private final List<String> code = new ArrayList<>();

    public List<String> generate(ProgramaNode programNode) {
        if (programNode == null) {
            code.add("Error: No se proporcionó un árbol de sintaxis abstracta (AST).");
            return code;
        }
        try {
            visit(programNode);
        } catch (Exception e) {
            code.add("Error durante la generación de código: " + e.getMessage());
            // Opcional: imprimir el stack trace para depuración
            // e.printStackTrace();
        }
        return code;
    }

    private String newLabel() {
        return "L" + labelCount++;
    }

    private String newTemp() {
        return "t" + tempCount++;
    }

    private void visit(NodoAST node) {
        if (node instanceof ProgramaNode) {
            visitProgramaNode((ProgramaNode) node);
        } else if (node instanceof AsignacionNode) {
            visitAsignacionNode((AsignacionNode) node);
        } else if (node instanceof SiNode) {
            visitSiNode((SiNode) node);
        } else if (node instanceof EscribirNode) {
            visitEscribirNode((EscribirNode) node);
        } else if (node instanceof LeerNode) {
            visitLeerNode((LeerNode) node);
        } else if (node instanceof DefinirNode) {
            visitDefinirNode((DefinirNode) node);
        }
        // Agrega más casos para otros tipos de nodos si es necesario
    }

    private void visitDefinirNode(DefinirNode node) {
        String tipoDato = node.getTipoDatoNombre().toUpperCase();
        if (tipoDato.equals("LOGICO")) {
            tipoDato = "BOOLEAN";
        }
        for (IdentificadorNode variable : node.getVariables()) {
            code.add("DECLARE " + variable.getNombre() + " " + tipoDato);
        }
    }

    private void visitProgramaNode(ProgramaNode node) {
        // code.add("start_program: " + node.getNombreProceso().getNombre());
        for (NodoAST statement : node.getSentencias()) {
            visit(statement);
        }
        code.add("END");
    }

    private void visitAsignacionNode(AsignacionNode node) {
        String exprResult = visitExpression(node.getExpresion());
        if (node.getExpresion() instanceof LiteralNode) {
            String temp = newTemp();
            code.add(temp + " = " + exprResult);
            code.add(node.getIdentificador().getNombre() + " = " + temp);
        } else {
            code.add(node.getIdentificador().getNombre() + " = " + exprResult);
        }
    }

    private void visitSiNode(SiNode node) {
        String conditionResult = visitExpression(node.getCondicion());

        String labelTrue = newLabel();
        String labelFalse = newLabel();
        String labelEnd = newLabel();

        if (node.tieneSiNo()) {
            code.add("IF " + conditionResult + " GOTO " + labelTrue);
            code.add("GOTO " + labelFalse);

            // Bloque "entonces"
            code.add(labelTrue + ":");
            for (NodoAST statement : node.getBloqueEntonces()) {
                visit(statement);
            }
            code.add("GOTO " + labelEnd);

            // Bloque "sino"
            code.add(labelFalse + ":");
            for (NodoAST statement : node.getBloqueSiNo()) {
                visit(statement);
            }
            code.add("GOTO " + labelEnd);

        } else {
            code.add("IF " + conditionResult + " GOTO " + labelTrue);
            code.add("GOTO " + labelEnd);

            // Bloque "entonces"
            code.add(labelTrue + ":");
            for (NodoAST statement : node.getBloqueEntonces()) {
                visit(statement);
            }
            code.add("GOTO " + labelEnd);
        }

        code.add(labelEnd + ":");
    }

    private void visitEscribirNode(EscribirNode node) {
        List<String> expressions = new ArrayList<>();
        for (NodoAST expr : node.getExpresiones()) {
            if (expr instanceof LiteralNode) {
                Object value = ((LiteralNode) expr).getValor();
                if (value instanceof String) {
                    expressions.add("\"" + value + "\"");
                } else {
                    expressions.add(String.valueOf(value));
                }
            } else {
                expressions.add(visitExpression(expr));
            }
        }
        code.add("PRINT " + String.join(", ", expressions));
    }

    private void visitLeerNode(LeerNode node) {
        for (IdentificadorNode var : node.getVariables()) {
            code.add("read " + var.getNombre());
        }
    }

    private String visitExpression(NodoAST node) {
        if (node instanceof LiteralNode) {
            return String.valueOf(((LiteralNode) node).getValor());
        } else if (node instanceof IdentificadorNode) {
            return ((IdentificadorNode) node).getNombre();
        } else if (node instanceof ExpresionBinariaNode) {
            return visitExpresionBinariaNode((ExpresionBinariaNode) node);
        } else if (node instanceof ExpresionParentizadaNode) {
            return visitExpression(((ExpresionParentizadaNode) node).getExpresionInterna());
        }
        return "unknown_expr";
    }

    private String visitExpresionBinariaNode(ExpresionBinariaNode node) {
        String left = visitExpression(node.getIzquierda());
        String right = visitExpression(node.getDerecha());
        String temp = newTemp();
        code.add(temp + " = " + left + " " + node.getOperador().getLexeme() + " " + right);
        return temp;
    }
}
