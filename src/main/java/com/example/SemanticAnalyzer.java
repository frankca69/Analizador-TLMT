package com.example;

import com.example.ast.ProgramaNode;

public class SemanticAnalyzer {

    private ProgramaNode ast;

    public SemanticAnalyzer(ProgramaNode ast) {
        this.ast = ast;
    }

    public String analyze() {
        // For now, we'll just return a placeholder string.
        // In the future, this method will traverse the AST and build the semantic tree.
        return "Semantic analysis not yet implemented.";
    }
}
