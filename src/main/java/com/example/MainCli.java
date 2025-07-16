package com.example;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

public class MainCli {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Uso: java com.example.MainCli <ruta_al_archivo>");
            return;
        }

        String filePath = args[0];
        String codigoFuente = new String(Files.readAllBytes(Paths.get(filePath)));

        // 1. Análisis Léxico
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.analyze(codigoFuente);
        if (!lexer.getErroresLexicos().isEmpty()) {
            System.err.println("Errores léxicos encontrados:");
            lexer.getErroresLexicos().forEach(System.err::println);
            return;
        }

        List<Token> tokensParaParser = new ArrayList<>();
        for (Token t : tokens) {
            if (t.getType() != TokenType.ERROR) {
                tokensParaParser.add(t);
            }
        }


        // 2. Análisis Sintáctico
        SyntaxAnalyzer syntaxAnalyzer = new SyntaxAnalyzer(tokensParaParser);
        syntaxAnalyzer.parse();
        if (!syntaxAnalyzer.getErroresSintacticos().isEmpty()) {
            System.err.println("Errores sintácticos encontrados:");
            syntaxAnalyzer.getErroresSintacticos().forEach(System.err::println);
            return;
        }

        // 3. Generación de Código de Tres Direcciones
        com.example.ast.ProgramaNode astRoot = syntaxAnalyzer.getAST();
        if (astRoot != null) {
            ThreeAddressCodeGenerator codeGenerator = new ThreeAddressCodeGenerator();
            List<String> threeAddressCode = codeGenerator.generate(astRoot);
            for (String line : threeAddressCode) {
                System.out.println(line);
            }
        } else {
            System.err.println("No se pudo generar el AST.");
        }
    }
}
