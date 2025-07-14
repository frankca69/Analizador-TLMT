package com.example.semantico;

import com.example.ast.NodoAST;

public class ArbolSemantico {

    private NodoAST nodo;
    private String tipo;

    public ArbolSemantico(NodoAST nodo, String tipo) {
        this.nodo = nodo;
        this.tipo = tipo;
    }

    public String aRepresentacionTextual(String prefijo, boolean esUltimo) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefijo);
        sb.append(esUltimo ? "└── " : "├── ");
        sb.append(nodo.getClass().getSimpleName());
        sb.append(" [tipo: ").append(tipo).append("]");
        sb.append("\n");

        if (nodo instanceof com.example.ast.ProgramaNode) {
            com.example.ast.ProgramaNode programa = (com.example.ast.ProgramaNode) nodo;
            for (int i = 0; i < programa.getSentencias().size(); i++) {
                NodoAST sentencia = programa.getSentencias().get(i);
                ArbolSemantico subArbol = new ArbolSemantico(sentencia, "sentencia");
                sb.append(subArbol.aRepresentacionTextual(prefijo + (esUltimo ? "    " : "│   "), i == programa.getSentencias().size() - 1));
            }
        } else if (nodo instanceof com.example.ast.DefinirNode) {
            com.example.ast.DefinirNode definir = (com.example.ast.DefinirNode) nodo;
            for (int i = 0; i < definir.getVariables().size(); i++) {
                ArbolSemantico subArbol = new ArbolSemantico(definir.getVariables().get(i), "identificador");
                sb.append(subArbol.aRepresentacionTextual(prefijo + (esUltimo ? "    " : "│   "), i == definir.getVariables().size() - 1));
            }
        } else if (nodo instanceof com.example.ast.AsignacionNode) {
            com.example.ast.AsignacionNode asignacion = (com.example.ast.AsignacionNode) nodo;
            ArbolSemantico subArbolIdentificador = new ArbolSemantico(asignacion.getIdentificador(), "identificador");
            sb.append(subArbolIdentificador.aRepresentacionTextual(prefijo + (esUltimo ? "    " : "│   "), false));
            ArbolSemantico subArbolExpresion = new ArbolSemantico(asignacion.getExpresion(), "expresion");
            sb.append(subArbolExpresion.aRepresentacionTextual(prefijo + (esUltimo ? "    " : "│   "), true));
        } else if (nodo instanceof com.example.ast.EscribirNode) {
            com.example.ast.EscribirNode escribir = (com.example.ast.EscribirNode) nodo;
            for (int i = 0; i < escribir.getExpresiones().size(); i++) {
                ArbolSemantico subArbol = new ArbolSemantico(escribir.getExpresiones().get(i), "expresion");
                sb.append(subArbol.aRepresentacionTextual(prefijo + (esUltimo ? "    " : "│   "), i == escribir.getExpresiones().size() - 1));
            }
        } else if (nodo instanceof com.example.ast.LeerNode) {
            com.example.ast.LeerNode leer = (com.example.ast.LeerNode) nodo;
            for (int i = 0; i < leer.getVariables().size(); i++) {
                ArbolSemantico subArbol = new ArbolSemantico(leer.getVariables().get(i), "identificador");
                sb.append(subArbol.aRepresentacionTextual(prefijo + (esUltimo ? "    " : "│   "), i == leer.getVariables().size() - 1));
            }
        } else if (nodo instanceof com.example.ast.SiNode) {
            com.example.ast.SiNode si = (com.example.ast.SiNode) nodo;
            ArbolSemantico subArbolCondicion = new ArbolSemantico(si.getCondicion(), "condicion");
            sb.append(subArbolCondicion.aRepresentacionTextual(prefijo + (esUltimo ? "    " : "│   "), false));

            for (int i = 0; i < si.getBloqueEntonces().size(); i++) {
                ArbolSemantico subArbol = new ArbolSemantico(si.getBloqueEntonces().get(i), "sentencia");
                sb.append(subArbol.aRepresentacionTextual(prefijo + (esUltimo ? "    " : "│   "), i == si.getBloqueEntonces().size() - 1 && si.getBloqueSiNo() == null));
            }

            if (si.getBloqueSiNo() != null) {
                for (int i = 0; i < si.getBloqueSiNo().size(); i++) {
                    ArbolSemantico subArbol = new ArbolSemantico(si.getBloqueSiNo().get(i), "sentencia");
                    sb.append(subArbol.aRepresentacionTextual(prefijo + (esUltimo ? "    " : "│   "), i == si.getBloqueSiNo().size() - 1));
                }
            }
        } else if (nodo instanceof com.example.ast.ExpresionBinariaNode) {
            com.example.ast.ExpresionBinariaNode expresion = (com.example.ast.ExpresionBinariaNode) nodo;
            ArbolSemantico subArbolIzquierda = new ArbolSemantico(expresion.getIzquierda(), "izquierda");
            sb.append(subArbolIzquierda.aRepresentacionTextual(prefijo + (esUltimo ? "    " : "│   "), false));
            ArbolSemantico subArbolDerecha = new ArbolSemantico(expresion.getDerecha(), "derecha");
            sb.append(subArbolDerecha.aRepresentacionTextual(prefijo + (esUltimo ? "    " : "│   "), true));
        } else if (nodo instanceof com.example.ast.ExpresionParentizadaNode) {
            com.example.ast.ExpresionParentizadaNode expresion = (com.example.ast.ExpresionParentizadaNode) nodo;
            ArbolSemantico subArbol = new ArbolSemantico(expresion.getExpresionInterna(), "expresion");
            sb.append(subArbol.aRepresentacionTextual(prefijo + (esUltimo ? "    " : "│   "), true));
        } else if (nodo instanceof com.example.ast.IdentificadorNode) {
            // No children
        } else if (nodo instanceof com.example.ast.LiteralNode) {
            // No children
        }

        return sb.toString();
    }
}
