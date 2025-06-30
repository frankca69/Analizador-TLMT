package com.example.ast;

import java.util.List;
import com.example.Token; // Para la línea/columna de inicio del programa

public class ProgramaNode implements NodoAST {
    private IdentificadorNode nombreProceso;
    private List<NodoAST> sentencias; // Lista de nodos de sentencia
    private Token tokenProceso; // Token "Proceso" para línea/columna

    public ProgramaNode(Token tokenProceso, IdentificadorNode nombreProceso, List<NodoAST> sentencias) {
        this.tokenProceso = tokenProceso;
        this.nombreProceso = nombreProceso;
        this.sentencias = sentencias;
    }

    public IdentificadorNode getNombreProceso() {
        return nombreProceso;
    }

    public List<NodoAST> getSentencias() {
        return sentencias;
    }

    @Override
    public String aRepresentacionTextual(String indentacion, boolean esUltimo) {
        StringBuilder sb = new StringBuilder();
        sb.append(indentacion);
        sb.append(esUltimo ? "└── " : "├── ");
        sb.append("Programa: ").append(nombreProceso.getNombre());
        if (tokenProceso != null) {
            sb.append(" (Línea: ").append(tokenProceso.getLineNumber()).append(")\n");
        } else {
            sb.append("\n");
        }

        String indentacionHijos = indentacion + (esUltimo ? "    " : "│   ");
        for (int i = 0; i < sentencias.size(); i++) {
            sb.append(sentencias.get(i).aRepresentacionTextual(indentacionHijos, i == sentencias.size() - 1));
        }
        return sb.toString();
    }
}
