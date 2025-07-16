package com.example;

import java.util.ArrayList;
import java.util.List;

public class AssemblyGenerator {

    private List<String> threeAddressCode;
    private List<String> assemblyCode;

    public AssemblyGenerator(List<String> threeAddressCode) {
        this.threeAddressCode = threeAddressCode;
        this.assemblyCode = new ArrayList<>();
    }


    private void generateHeader() {
        assemblyCode.add("section .bss");
        declareVariables();
        assemblyCode.add("section .data");
        // Aquí se podrían definir datos, como strings para Escribir.
        assemblyCode.add("section .text");
        assemblyCode.add("global _start");
    }

    private void declareVariables() {
        for (String instruction : threeAddressCode) {
            String[] parts = instruction.split(" ");
            if (parts.length > 0) {
                String var = parts[0].trim();
                if (isVariable(var)) {
                    // Asegurarse de no declarar la misma variable dos veces
                    if (assemblyCode.stream().noneMatch(line -> line.startsWith(var + " "))) {
                        assemblyCode.add(var + " resd 1"); // Reservar 1 dword (4 bytes) para cada variable
                    }
                }
            }
            if (parts.length > 2 && isVariable(parts[2].trim())) {
                 String var = parts[2].trim();
                 if (assemblyCode.stream().noneMatch(line -> line.startsWith(var + " "))) {
                        assemblyCode.add(var + " resd 1");
                 }
            }
        }
    }

    private boolean isVariable(String s) {
        // Una suposición simple: si no es un número y no es una palabra clave, es una variable.
        // Esto debería ser más robusto en un compilador real.
        return !isNumeric(s) && !s.endsWith(":") && !s.equals("goto") && !s.equals("if_false") && !s.equals("write") && !s.equals("read") && !s.equals("start_program") && !s.equals("end_program");
    }

    private void translateInstructions() {
        assemblyCode.add("_start:");
        for (String instruction : threeAddressCode) {
            instruction = instruction.trim();
            if (instruction.endsWith(":")) {
                assemblyCode.add(instruction); // Añade la etiqueta
            } else if (instruction.startsWith("start_program")) {
                // No se necesita acción específica para start_program en este punto
            } else if (instruction.startsWith("end_program")) {
                // La salida ya se maneja en el pie de página
            } else if (instruction.contains("=")) {
                if (instruction.contains(">") || instruction.contains("<") || instruction.contains("==")) {
                    handleComparison(instruction);
                } else {
                    handleAssignment(instruction);
                }
            } else if (instruction.startsWith("write")) {
                handleWrite(instruction);
            } else if (instruction.startsWith("read")) {
                handleRead(instruction);
            } else if (instruction.startsWith("goto")) {
                assemblyCode.add("    jmp " + instruction.substring(5).trim());
            } else if (instruction.startsWith("if_false")) {
                handleConditionalJump(instruction);
            }
        }
    }

    private void handleAssignment(String instruction) {
        String[] parts = instruction.split("=");
        String dest = parts[0].trim();
        String expr = parts[1].trim();
        String[] exprParts = expr.split(" ");

        if (exprParts.length == 1) { // Asignación simple: a = 10 o a = b
            if (isNumeric(exprParts[0])) {
                assemblyCode.add("    mov dword [" + dest + "], " + exprParts[0]);
            } else {
                assemblyCode.add("    mov eax, [" + exprParts[0] + "]");
                assemblyCode.add("    mov [" + dest + "], eax");
            }
        } else if (exprParts.length == 3) { // Expresión binaria: t0 = a + 5
            String op1 = exprParts[0];
            String op = exprParts[1];
            String op2 = exprParts[2];

            // Mover el primer operando a un registro
            if(isNumeric(op1)) {
                assemblyCode.add("    mov eax, " + op1);
            } else {
                assemblyCode.add("    mov eax, [" + op1 + "]");
            }

            // Realizar la operación con el segundo operando
            if (op.equals("+")) {
                if(isNumeric(op2)) {
                    assemblyCode.add("    add eax, " + op2);
                } else {
                    assemblyCode.add("    add eax, [" + op2 + "]");
                }
            } else if (op.equals("-")) {
                 if(isNumeric(op2)) {
                    assemblyCode.add("    sub eax, " + op2);
                } else {
                    assemblyCode.add("    sub eax, [" + op2 + "]");
                }
            }
            // ... (añadir más operaciones como *, /)

            // Guardar el resultado
            assemblyCode.add("    mov [" + dest + "], eax");
        }
    }

    private int strCount = 0;

    private void handleWrite(String instruction) {
        String content = instruction.substring(5).trim();
        String[] parts = content.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("\"")) { // Es una cadena
                String str = part.substring(1, part.length() - 1);
                String strLabel = "str" + strCount++;
                // La declaración de la cadena se manejará en la pasada de recopilación de datos
                assemblyCode.add("    mov rax, 1 ; sys_write");
                assemblyCode.add("    mov rdi, 1 ; stdout");
                assemblyCode.add("    mov rsi, " + strLabel);
                assemblyCode.add("    mov rdx, " + str.length()); // Longitud exacta de la cadena
                assemblyCode.add("    syscall");
            } else { // Es una variable
                assemblyCode.add("    ; Escribiendo valor de " + part);
                assemblyCode.add("    mov eax, [" + part + "]");
                assemblyCode.add("    call _printRAX");
            }
        }
    }

    private void handleRead(String instruction) {
        // Similar a write, leer de la entrada estándar es complejo.
        String var = instruction.substring(5).trim();
        assemblyCode.add("    ; Leyendo valor para " + var);
        assemblyCode.add("    ; ... (código para leer de stdin y guardarlo en la variable)");
    }

    private void handleConditionalJump(String instruction) {
        String[] parts = instruction.split(" ");
        String conditionVar = parts[1];
        String label = parts[3];
        assemblyCode.add("    mov al, [" + conditionVar + "]");
        assemblyCode.add("    cmp al, 0");
        assemblyCode.add("    je " + label); // Saltar si el resultado de la condición es falso (0)
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void generateFooter() {
        // Salida del programa
        assemblyCode.add("    mov rax, 60  ; syscall para exit");
        assemblyCode.add("    xor rdi, rdi ; código de salida 0");
        assemblyCode.add("    syscall");

        // Función para imprimir un número en EAX
        assemblyCode.add("_printRAX:");
        assemblyCode.add("    mov rsi, print_buffer + 10");
        assemblyCode.add("    mov byte [print_buffer + 11], 0xa ; newline");
        assemblyCode.add("    mov r10, 10");
        assemblyCode.add("_printRAX_loop:");
        assemblyCode.add("    xor rdx, rdx");
        assemblyCode.add("    div r10");
        assemblyCode.add("    add dl, '0'");
        assemblyCode.add("    mov [rsi], dl");
        assemblyCode.add("    dec rsi");
        assemblyCode.add("    test rax, rax");
        assemblyCode.add("    jnz _printRAX_loop");
        assemblyCode.add("    inc rsi");
        assemblyCode.add("    mov rdx, print_buffer + 12");
        assemblyCode.add("    sub rdx, rsi");
        assemblyCode.add("    mov rax, 1");
        assemblyCode.add("    mov rdi, 1");
        assemblyCode.add("    syscall");
        assemblyCode.add("    ret");
    }

    public List<String> generate() {
        List<String> dataSection = new ArrayList<>();
        strCount = 0;

        // Primera pasada: encontrar todas las cadenas para la sección .data
        for (String instruction : threeAddressCode) {
            if (instruction.trim().startsWith("write")) {
                String content = instruction.substring(5).trim();
                String[] parts = content.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("\"")) {
                        String str = part.substring(1, part.length() - 1);
                        String strLabel = "str" + strCount++;
                        dataSection.add(strLabel + " db '" + str + "'");
                    }
                }
            }
        }

        generateHeader();
        assemblyCode.addAll(dataSection);
        translateInstructions();
        generateFooter();
        // Asegurarse de que el búfer de impresión esté en .bss
        assemblyCode.add(1, "print_buffer resb 12");
        return assemblyCode;
    }

    private void handleComparison(String instruction) {
        String[] parts = instruction.split(" ");
        String dest = parts[0].trim();
        String op1 = parts[2].trim();
        String op = parts[3].trim();
        String op2 = parts[4].trim();

        if(isNumeric(op1)) {
            assemblyCode.add("    mov eax, " + op1);
        } else {
            assemblyCode.add("    mov eax, [" + op1 + "]");
        }

        if(isNumeric(op2)) {
            assemblyCode.add("    cmp eax, " + op2);
        } else {
            assemblyCode.add("    cmp eax, [" + op2 + "]");
        }

        String trueLabel = newLabel();
        String endLabel = newLabel();

        if (op.equals(">")) {
            assemblyCode.add("    jg " + trueLabel);
        } else if (op.equals("<")) {
            assemblyCode.add("    jl " + trueLabel);
        } else if (op.equals("==")) {
            assemblyCode.add("    je " + trueLabel);
        }
        // ... (añadir más comparaciones)

        assemblyCode.add("    mov byte [" + dest + "], 0 ; false");
        assemblyCode.add("    jmp " + endLabel);
        assemblyCode.add(trueLabel + ":");
        assemblyCode.add("    mov byte [" + dest + "], 1 ; true");
        assemblyCode.add(endLabel + ":");
    }

    private String newLabel() {
        return "L" + (assemblyCode.size() + 100); // Un método simple para generar etiquetas únicas
    }
}
