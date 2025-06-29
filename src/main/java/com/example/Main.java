package com.example;

import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String filePath = "input_syntax_test.txt"; // TEMPORARY: Use new file for syntax testing
        // String filePath = "input.txt"; // Original file with lexical errors
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

        System.out.println("\n--- Resultados del Analizador Léxico ---");
        displayTokenTable(tokens);
        displayLexicalErrors(tokens);

        // --- Integración del Analizador Sintáctico ---
        if (!tokens.stream().anyMatch(t -> t.getType() == TokenType.ERROR)) { // Solo parsear si no hay errores léxicos graves
            System.out.println("\n\n--- Iniciando Análisis Sintáctico ---");
            SyntaxAnalyzer syntaxAnalyzer = new SyntaxAnalyzer(tokens);
            syntaxAnalyzer.parse();

            System.out.println("\n--- Log del Análisis Sintáctico ---");
            List<String> syntaxLog = syntaxAnalyzer.getSyntaxLog();
            if (syntaxLog.isEmpty()) {
                System.out.println("El log del análisis sintáctico está vacío.");
            } else {
                for (String logEntry : syntaxLog) {
                    // Errors are already printed to System.err by the logger,
                    // but we can choose to print them here again or filter.
                    // For now, print all log entries.
                    System.out.println(logEntry);
                }
            }
            // Separate display for syntax errors if desired, though they are in the log.
            // long syntaxErrorCount = syntaxLog.stream().filter(log -> log.startsWith("Error Sintáctico")).count();
            // System.out.println("\nTotal de errores sintácticos encontrados: " + syntaxErrorCount);

            // --- Mostrar Tabla de Símbolos ---
            TablaDeSimbolos tabla = syntaxAnalyzer.getTablaDeSimbolos();
            tabla.imprimirTabla();

        } else {
            System.out.println("\n\n--- Análisis Sintáctico Omitido ---");
            System.out.println("No se procederá con el análisis sintáctico debido a errores léxicos previos.");
        }
    }

    public static void displayTokenTable(List<Token> tokens) {
        System.out.println("Tabla de Tokens:");
        System.out.println("+-----------------+----------------------+---------------------------+----------+");
        System.out.println("| TOKEN           | LEXEMA               | PATRÓN                    | LÍNEA    |");
        System.out.println("+-----------------+----------------------+---------------------------+----------+");

        for (Token token : tokens) {
            if (token.getType() != TokenType.ERROR) {
                System.out.printf("| %-15s | %-20s | %-25s | %-8d |\n",
                        token.getType(),
                        token.getLexeme(),
                        truncate(token.getPattern(), 25), // Truncar patrón si es muy largo
                        token.getLineNumber());
            }
        }
        System.out.println("+-----------------+----------------------+---------------------------+----------+");
    }

    public static void displayLexicalErrors(List<Token> tokens) {
        List<Token> errorTokens = new ArrayList<>();
        for (Token token : tokens) {
            if (token.getType() == TokenType.ERROR) {
                errorTokens.add(token);
            }
        }

        if (!errorTokens.isEmpty()) {
            System.out.println("\nErrores Léxicos Encontrados:");
            System.out.println("+-----------------+----------------------+---------------------------+----------+");
            System.out.println("| TOKEN           | LEXEMA               | DESCRIPCIÓN               | LÍNEA    |");
            System.out.println("+-----------------+----------------------+---------------------------+----------+");
            for (Token error : errorTokens) {
                System.out.printf("| %-15s | %-20s | %-25s | %-8d |\n",
                        error.getType(),
                        error.getLexeme(),
                        error.getPattern(), // Para errores, el "patrón" es la descripción del error
                        error.getLineNumber());
            }
            System.out.println("+-----------------+----------------------+---------------------------+----------+");
        } else {
            System.out.println("\nNo se encontraron errores léxicos.");
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}