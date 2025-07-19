package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssemblyGenerator {

    private List<String> optimizedCode;
    private TablaDeSimbolos symbolTable;
    private StringBuilder dataSection;
    private StringBuilder textSection;
    private Map<String, String> stringLiterals;
    private int stringLiteralCounter;
    private Map<String, String> tempVarValues; // Para almacenar valores de variables temporales

    public AssemblyGenerator(List<String> optimizedCode, TablaDeSimbolos symbolTable) {
        this.optimizedCode = optimizedCode;
        this.symbolTable = symbolTable;
        this.dataSection = new StringBuilder();
        this.textSection = new StringBuilder();
        this.stringLiterals = new HashMap<>();
        this.stringLiteralCounter = 1;
        this.tempVarValues = new HashMap<>();
    }

    public String generate() {
        // Inicializar secciones
        dataSection.append("section .data\n");

        // Declarar variables (solo las declaradas explícitamente, no temporales)
        for (Simbolo symbol : symbolTable.getSimbolosAsCollection()) {
            if (symbol.getTipo().equals("Entero") || symbol.getTipo().equals("ENTERO")) {
                dataSection.append(String.format("    %s dd 0\n", symbol.getNombre()));
            } else if (symbol.getTipo().equals("Boolean") || symbol.getTipo().equals("BOOLEAN")) {
                dataSection.append(String.format("    %s dd 0\n", symbol.getNombre()));
            }
        }

        textSection.append("section .text\n");
        textSection.append("    global _start\n\n");

        // Código boilerplate para imprimir números
        addPrintNumberFunction();

        textSection.append("_start:\n");

        // Procesar código optimizado línea por línea
        for (String line : optimizedCode) {
            processLine(line.trim());
        }

        // Finalizar programa
        textSection.append("    mov rax, 60\n");
        textSection.append("    mov rdi, 0\n");
        textSection.append("    syscall\n");

        // Añadir literales de cadena y buffer a .data
        for (Map.Entry<String, String> entry : stringLiterals.entrySet()) {
            dataSection.append(String.format("    %s db %s, 0\n", entry.getValue(), entry.getKey()));
        }
        dataSection.append("    newline db 10, 0\n");
        dataSection.append("    buffer db 12 dup(0)\n");

        return dataSection.toString() + "\n" + textSection.toString();
    }

    private void addPrintNumberFunction() {
        textSection.append("print_number:\n");
        textSection.append("    push rbx\n");
        textSection.append("    push rcx\n");
        textSection.append("    push rdx\n");
        textSection.append("    push rsi\n");
        textSection.append("    mov ebx, 10\n");
        textSection.append("    mov ecx, 0\n");
        textSection.append("    mov rsi, buffer + 11\n");
        textSection.append("    mov byte [rsi], 0\n");
        textSection.append("    cmp eax, 0\n");
        textSection.append("    jne convert_loop\n");
        textSection.append("    dec rsi\n");
        textSection.append("    mov byte [rsi], '0'\n");
        textSection.append("    inc ecx\n");
        textSection.append("    jmp print_it\n");
        textSection.append("convert_loop:\n");
        textSection.append("    cmp eax, 0\n");
        textSection.append("    je print_it\n");
        textSection.append("    xor edx, edx\n");
        textSection.append("    div ebx\n");
        textSection.append("    add dl, '0'\n");
        textSection.append("    dec rsi\n");
        textSection.append("    mov [rsi], dl\n");
        textSection.append("    inc ecx\n");
        textSection.append("    jmp convert_loop\n");
        textSection.append("print_it:\n");
        textSection.append("    mov rax, 1\n");
        textSection.append("    mov rdi, 1\n");
        textSection.append("    mov rdx, rcx\n");
        textSection.append("    syscall\n");
        textSection.append("    pop rsi\n");
        textSection.append("    pop rdx\n");
        textSection.append("    pop rcx\n");
        textSection.append("    pop rbx\n");
        textSection.append("    ret\n\n");
    }

    private void processLine(String line) {
        if (line.isEmpty())
            return;

        String[] parts = line.split(" ");
        if (parts.length == 0)
            return;

        // Manejar declaraciones
        if (parts[0].equals("DECLARE")) {
            // Ya se procesaron en la sección .data
            return;
        }

        // Manejar etiquetas
        if (line.endsWith(":")) {
            textSection.append(line + "\n");
            return;
        }

        // Manejar asignaciones
        if (line.contains(" = ")) {
            handleAssignment(line);
        }
        // Manejar PRINT
        else if (parts[0].equals("PRINT")) {
            handlePrint(line);
        }
        // Manejar IF GOTO
        else if (parts[0].equals("IF")) {
            handleIfGoto(line);
        }
        // Manejar GOTO simple
        else if (parts[0].equals("GOTO")) {
            handleGoto(line);
        }
        // Manejar END
        else if (parts[0].equals("END")) {
            // No necesita acción especial, el programa termina naturalmente
        }
    }

    private void handleAssignment(String line) {
        String[] parts = line.split(" = ");
        if (parts.length != 2)
            return;

        String dest = parts[0].trim();
        String source = parts[1].trim();

        // Si la fuente es una variable temporal, usar su valor conocido
        if (tempVarValues.containsKey(source)) {
            source = tempVarValues.get(source);
        }

        // Si el destino es una variable temporal, almacenar su valor
        if (source.matches("\\d+")) { // Es un número
            tempVarValues.put(dest, source);

            // Si también es una variable declarada, generar código assembly
            if (isVariableDeclared(dest)) {
                textSection.append(String.format("    mov dword [%s], %s\n", dest, source));
            }
        } else {
            // Es una variable
            if (isVariableDeclared(source)) {
                textSection.append(String.format("    mov eax, [%s]\n", source));
                if (isVariableDeclared(dest)) {
                    textSection.append(String.format("    mov [%s], eax\n", dest));
                }
            }
        }
    }

    private void handlePrint(String line) {
        // PRINT "texto", variable, "más texto"
        String content = line.substring(5).trim(); // Quitar "PRINT"

        // Dividir por comas pero preservar las comillas
        List<String> items = parseCommaSeparated(content);

        for (String item : items) {
            item = item.trim();
            if (item.startsWith("\"") && item.endsWith("\"")) {
                // Es un literal de cadena
                String literal = item;
                String label = getStringLiteralLabel(literal);
                int length = literal.length() - 2; // Quitar las comillas
                textSection.append("    mov rax, 1\n");
                textSection.append("    mov rdi, 1\n");
                textSection.append(String.format("    mov rsi, %s\n", label));
                textSection.append(String.format("    mov rdx, %d\n", length));
                textSection.append("    syscall\n");
            } else if (isNumeric(item)) {
                // Es un número literal
                textSection.append(String.format("    mov eax, %s\n", item));
                textSection.append("    call print_number\n");
            } else {
                // Es una variable o temporal
                String value = tempVarValues.get(item);
                if (value != null && isNumeric(value)) {
                    // Variable temporal con valor conocido
                    textSection.append(String.format("    mov eax, %s\n", value));
                    textSection.append("    call print_number\n");
                } else if (isVariableDeclared(item)) {
                    // Variable declarada
                    textSection.append(String.format("    mov eax, [%s]\n", item));
                    textSection.append("    call print_number\n");
                }
            }
        }

        // Imprimir nueva línea
        textSection.append("    mov rax, 1\n");
        textSection.append("    mov rdi, 1\n");
        textSection.append("    mov rsi, newline\n");
        textSection.append("    mov rdx, 1\n");
        textSection.append("    syscall\n");
    }

    private void handleIfGoto(String line) {
        // IF condicion GOTO L0
        String[] parts = line.split(" ");
        if (parts.length >= 4) {
            String condition = parts[1];
            String label = parts[3];

            // Evaluar la condición
            String value = tempVarValues.get(condition);
            if (value != null) {
                if (value.equals("1") || value.equalsIgnoreCase("true")) {
                    // Condición siempre verdadera
                    textSection.append(String.format("    jmp %s\n", label));
                } else {
                    // Condición siempre falsa - no generar salto
                }
            } else if (isVariableDeclared(condition)) {
                // Variable declarada - verificar en tiempo de ejecución
                textSection.append(String.format("    mov eax, [%s]\n", condition));
                textSection.append("    cmp eax, 0\n");
                textSection.append(String.format("    jne %s\n", label));
            }
        }
    }

    private void handleGoto(String line) {
        // GOTO L1
        String[] parts = line.split(" ");
        if (parts.length >= 2) {
            String label = parts[1];
            textSection.append(String.format("    jmp %s\n", label));
        }
    }

    private List<String> parseCommaSeparated(String content) {
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == ',' && !inQuotes) {
                items.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            items.add(current.toString().trim());
        }

        return items;
    }

    private String getStringLiteralLabel(String literal) {
        if (stringLiterals.containsKey(literal)) {
            return stringLiterals.get(literal);
        }
        String label = "msg" + (stringLiteralCounter++);
        stringLiterals.put(literal, label);
        return label;
    }

    private boolean isNumeric(String str) {
        if (str == null)
            return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isVariableDeclared(String varName) {
        for (Simbolo symbol : symbolTable.getSimbolosAsCollection()) {
            if (symbol.getNombre().equals(varName)) {
                return true;
            }
        }
        return false;
    }
}