package com.example;

import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String filePath = "input_syntax_errors.txt"; // Test file with syntax errors
        // String filePath = "input.txt"; // Original file with lexical errors
        // String filePath = "input_syntax_test.txt"; // Correct file
        String pseintCode = "";

        try {
            pseintCode = new String(Files.readAllBytes(Paths.get(filePath)));
            System.out.println("--- Código de entrada (" + filePath + ") ---");
            System.out.println(pseintCode);
            System.out.println("-------------------------------------\n");
        } catch (IOException e) {
            System.err.println("Error al leer el archivo de entrada: " + filePath);
            e.printStackTrace();
            return;
        }

        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.analyze(pseintCode);
        List<ErrorCompilacion> erroresLexicos = lexer.getErroresLexicos();

        System.out.println("\n--- Resultados del Analizador Léxico ---");
        // Display token table (excluding ERROR type tokens if they are separately handled by erroresLexicos)
        displayTokenTable(tokens);

        SyntaxAnalyzer syntaxAnalyzer = null; // Declare here to access its errors later
        List<ErrorCompilacion> erroresSintacticos = new ArrayList<>();

        if (erroresLexicos.isEmpty()) {
            System.out.println("Análisis léxico completado sin errores.");

            // Filter out any remaining ERROR type tokens before parsing, if Lexer still adds them
            // This step might be redundant if Lexer exclusively uses ErrorCompilacion list for errors
            List<Token> tokensParaParser = new ArrayList<>();
            for (Token t : tokens) {
                if (t.getType() != TokenType.ERROR) {
                    tokensParaParser.add(t);
                }
            }

            if (tokensParaParser.isEmpty() && !pseintCode.trim().isEmpty() && !isOnlyComments(pseintCode, tokens)) {
                 // If after filtering errors, no tokens remain but code was not empty/only comments
                 erroresLexicos.add(new ErrorCompilacion(ErrorCompilacion.TipoError.LEXICO, "El código no produjo tokens válidos para el análisis sintáctico.", 1, 1, ""));
            } else if (tokensParaParser.isEmpty() && (pseintCode.trim().isEmpty() || isOnlyComments(pseintCode, tokens))) {
                System.out.println("El archivo está vacío o solo contiene comentarios. No se requiere análisis sintáctico.");
            }
             else {
                System.out.println("\n\n--- Iniciando Análisis Sintáctico ---");
                syntaxAnalyzer = new SyntaxAnalyzer(tokensParaParser);
                syntaxAnalyzer.parse();
                erroresSintacticos = syntaxAnalyzer.getErroresSintacticos();

                if (erroresSintacticos.isEmpty()) {
                    System.out.println("\nAnálisis sintáctico completado sin errores.");
                    System.out.println("\n--- Log del Análisis Sintáctico (Reglas) ---");
                    List<String> syntaxLog = syntaxAnalyzer.getSyntaxLog();
                    for (String logEntry : syntaxLog) {
                        // Only print rule logs, not error messages that might be in syntaxLog too
                        if (logEntry.startsWith("Regla/Acción Sintáctica:")) {
                            System.out.println(logEntry);
                        }
                    }
                    TablaDeSimbolos tabla = syntaxAnalyzer.getTablaDeSimbolos();
                    tabla.imprimirTabla();
                }
            }
        } else {
            System.out.println("\n\n--- Análisis Sintáctico Omitido debido a errores léxicos. ---");
        }

        // --- Impresión Consolidada de Errores ---
        boolean hayErrores = !erroresLexicos.isEmpty() || !erroresSintacticos.isEmpty();

        if (hayErrores) {
            System.out.println("\n\n--- Errores Detectados ---");
            for (ErrorCompilacion err : erroresLexicos) {
                System.out.println(err.toString());
            }
            for (ErrorCompilacion err : erroresSintacticos) {
                System.out.println(err.toString());
            }
            System.out.println("\nCompilación fallida.");
        } else {
            System.out.println("\n\n--- Resumen de Errores ---");
            System.out.println("Compilación completada sin errores.");
        }
    }

    private static boolean isOnlyComments(String code, List<Token> tokens) {
        if (code.trim().isEmpty()) return true; // Empty code is like "only comments"
        for (Token t : tokens) {
            if (t.getType() != TokenType.COMMENT) {
                return false;
            }
        }
        return true; // All tokens are comments
    }

    public static void displayTokenTable(List<Token> tokens) {
        System.out.println("Tabla de Tokens:");
        System.out.println("+-----------------+----------------------+---------------------------+----------+------------+");
        System.out.println("| TOKEN           | LEXEMA               | PATRÓN                    | LÍNEA    | COLUMNA    |");
        System.out.println("+-----------------+----------------------+---------------------------+----------+------------+");

        for (Token token : tokens) {
            // Exclude ERROR type tokens if they are now handled by the ErrorCompilacion list
            if (token.getType() != TokenType.ERROR) {
                System.out.printf("| %-15s | %-20s | %-25s | %-8d | %-10d |\n",
                        token.getType(),
                        token.getLexeme(),
                        truncate(token.getPattern(), 25),
                        token.getLineNumber(),
                        token.getColumnNumber());
            }
        }
        System.out.println("+-----------------+----------------------+---------------------------+----------+------------+");
    }

    // displayLexicalErrors is removed as its functionality is replaced by iterating List<ErrorCompilacion>

    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}