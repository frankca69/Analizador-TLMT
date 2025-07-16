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
    private Map<String, String> tempVarReplacements;

    public AssemblyGenerator(List<String> optimizedCode, TablaDeSimbolos symbolTable) {
        this.optimizedCode = optimizedCode;
        this.symbolTable = symbolTable;
        this.dataSection = new StringBuilder();
        this.textSection = new StringBuilder();
        this.stringLiterals = new HashMap<>();
        this.stringLiteralCounter = 1;
        this.tempVarReplacements = new HashMap<>();
    }

    public String generate() {
        // Inicializar secciones
        dataSection.append("section .data\n");

        // Declarar variables
        for (Simbolo symbol : symbolTable.getSimbolosAsCollection()) {
            if (symbol.getTipo().equals("Entero")) {
                dataSection.append(String.format("    %s dd 0\n", symbol.getNombre()));
            }
        }

        // Encontrar reemplazos de variables temporales
        for (int i = 0; i < optimizedCode.size() - 1; i++) {
            String line1 = optimizedCode.get(i);
            String line2 = optimizedCode.get(i + 1);

            String[] parts1 = line1.split(" ");
            String[] parts2 = line2.split(" ");

            if (parts1.length == 5 && parts2.length == 3 && parts2[1].equals("=") && parts1[0].equals(parts2[2])) {
                tempVarReplacements.put(parts1[0], parts2[0]);
            }
        }

        textSection.append("section .text\n");
        textSection.append("    global _start\n\n");

        // Código boilerplate para imprimir números
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

        textSection.append("_start:\n");

        // Procesar código optimizado
        for (String line : optimizedCode) {
            processLine(line);
        }

        // Finalizar syscall
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

    private void processLine(String line) {
        for (Map.Entry<String, String> entry : tempVarReplacements.entrySet()) {
            line = line.replace(entry.getKey(), entry.getValue());
        }

        String[] parts = line.split(" ");
        if (parts.length == 0) return;

        if (line.contains("=") && parts.length == 3 && parts[0].equals(parts[2])) {
            // Es una asignación redundante (c = c), la ignoramos
            return;
        }

        if (line.contains("=")) {
            // a = 5
            // c = a + b
            String dest = parts[0];
            if (parts.length == 3) {
                // Asignación simple
                String source = parts[2];
                if (isNumeric(source)) {
                    textSection.append(String.format("    mov dword [%s], %s\n", dest, source));
                } else {
                    textSection.append(String.format("    mov eax, [%s]\n", source));
                    textSection.append(String.format("    mov [%s], eax\n", dest));
                }
            } else if (parts.length == 5) {
                // Operación binaria
                String op1 = parts[2];
                String operator = parts[3];
                String op2 = parts[4];

                if (isNumeric(op1)) {
                    textSection.append(String.format("    mov eax, %s\n", op1));
                } else {
                    textSection.append(String.format("    mov eax, [%s]\n", op1));
                }

                if (isNumeric(op2)) {
                    textSection.append(String.format("    mov ebx, %s\n", op2));
                } else {
                    textSection.append(String.format("    mov ebx, [%s]\n", op2));
                }


                switch (operator) {
                    case "+":
                        textSection.append("    add eax, ebx\n");
                        break;
                    case "-":
                        textSection.append("    sub eax, ebx\n");
                        break;
                    // Faltan otros operadores
                }
                textSection.append(String.format("    mov [%s], eax\n", dest));
            }
        } else if (parts[0].equals("Escribir")) {
            handleEscribir(line);
        }
    }

    private void handleEscribir(String line) {
        // Escribir "La suma de ", a, " y ", b, " es: ", c;
        String content = line.substring("Escribir".length()).trim();
        String[] items = content.split(",");
        for (String item : items) {
            item = item.trim().replace(";", "");
            if (item.startsWith("\"") && item.endsWith("\"")) {
                // Es un literal de cadena
                String literal = item;
                String label = getStringLiteralLabel(literal);
                textSection.append(String.format("    mov rax, 1\n"));
                textSection.append(String.format("    mov rdi, 1\n"));
                textSection.append(String.format("    mov rsi, %s\n", label));
                textSection.append(String.format("    mov rdx, %d\n", literal.length() - 2));
                textSection.append(String.format("    syscall\n"));
            } else {
                // Es una variable
                textSection.append(String.format("    mov eax, [%s]\n", item));
                textSection.append("    call print_number\n");
            }
        }
        textSection.append("    mov rax, 1\n");
        textSection.append("    mov rdi, 1\n");
        textSection.append("    mov rsi, newline\n");
        textSection.append("    mov rdx, 1\n");
        textSection.append("    syscall\n");
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
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
