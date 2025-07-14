package com.example;

import com.example.ast.ProgramaNode;
import com.example.semantico.ArbolSemantico;

public class SemanticAnalyzer {

    private ProgramaNode ast;

    public SemanticAnalyzer(ProgramaNode ast) {
        this.ast = ast;
    }

    public String analyze() {
        if (ast == null) {
            return "No AST available for semantic analysis.";
        }
        ArbolSemantico arbolSemantico = new ArbolSemantico(ast, "programa");
        return arbolSemantico.aRepresentacionTextual("", true);
    }
}
