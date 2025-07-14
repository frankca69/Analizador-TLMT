package com.example;

import com.example.ast.*;
import java.util.ArrayList;
import java.util.List;

public class ThreeAddressCodeGenerator {

    private int labelCount = 0;
    private int tempCount = 0;
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
            // No se genera código para las definiciones, ya que es una declaración.
        }
        // Agrega más casos para otros tipos de nodos si es necesario
    }

    private void visitProgramaNode(ProgramaNode node) {
        code.add("start_program: " + node.getNombreProceso().getNombre());
        for (NodoAST statement : node.getSentencias()) {
            visit(statement);
        }
        code.add("end_program");
    }

    private void visitAsignacionNode(AsignacionNode node) {
        String exprResult = visitExpression(node.getExpresion());
        code.add(node.getIdentificador().getNombre() + " = " + exprResult);
    }

    private void visitSiNode(SiNode node) {
        String labelElse = newLabel();
        String labelEnd = newLabel();

        String conditionResult = visitExpression(node.getCondicion());
        code.add("if_false " + conditionResult + " goto " + labelElse);

        // Bloque "entonces"
        for (NodoAST statement : node.getBloqueEntonces()) {
            visit(statement);
        }
        code.add("goto " + labelEnd);

        // Bloque "sino"
        code.add(labelElse + ":");
        if (node.tieneSiNo()) {
            for (NodoAST statement : node.getBloqueSiNo()) {
                visit(statement);
            }
        }

        code.add(labelEnd + ":");
    }

    private void visitEscribirNode(EscribirNode node) {
        for (NodoAST expr : node.getExpresiones()) {
            String result = visitExpression(expr);
            code.add("write " + result);
        }
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
