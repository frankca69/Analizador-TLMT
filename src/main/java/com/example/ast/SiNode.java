package com.example.ast;

import java.util.List;
import com.example.Token; // Para el token "Si"

public class SiNode implements NodoAST { // Es un tipo de NodoSentencia
    private Token tokenSi;
    private NodoAST condicion;
    private List<NodoAST> bloqueEntonces;
    private List<NodoAST> bloqueSiNo; // Puede ser null si no hay cláusula SiNo

    public SiNode(Token tokenSi, NodoAST condicion, List<NodoAST> bloqueEntonces, List<NodoAST> bloqueSiNo) {
        this.tokenSi = tokenSi;
        this.condicion = condicion;
        this.bloqueEntonces = bloqueEntonces;
        this.bloqueSiNo = bloqueSiNo;
    }

    public NodoAST getCondicion() {
        return condicion;
    }

    public List<NodoAST> getBloqueEntonces() {
        return bloqueEntonces;
    }

    public List<NodoAST> getBloqueSiNo() {
        return bloqueSiNo;
    }

    public boolean tieneSiNo() {
        return bloqueSiNo != null && !bloqueSiNo.isEmpty();
    }

    @Override
    public String aRepresentacionTextual(String indentacion, boolean esUltimo) {
        StringBuilder sb = new StringBuilder();
        sb.append(indentacion);
        sb.append(esUltimo ? "└── " : "├── ");
        sb.append("Si");
        sb.append(" (L:").append(tokenSi.getLineNumber()).append(")\n");

        String indentacionHijos = indentacion + (esUltimo ? "    " : "│   ");

        sb.append(indentacionHijos).append("├── Condicion:\n");
        sb.append(condicion.aRepresentacionTextual(indentacionHijos + "│   ", true)); // Condición es el último de su "sub-bloque"

        sb.append(indentacionHijos).append("├── Bloque Entonces:\n");
        for (int i = 0; i < bloqueEntonces.size(); i++) {
            sb.append(bloqueEntonces.get(i).aRepresentacionTextual(indentacionHijos + "│   ", i == bloqueEntonces.size() - 1));
        }

        if (tieneSiNo()) {
            sb.append(indentacionHijos).append("└── Bloque SiNo:\n"); // SiNo es el último si existe
            for (int i = 0; i < bloqueSiNo.size(); i++) {
                sb.append(bloqueSiNo.get(i).aRepresentacionTextual(indentacionHijos + "    ", i == bloqueSiNo.size() - 1));
            }
        }
        return sb.toString();
    }
}
