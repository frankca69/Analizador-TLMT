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

    public List<String> generate() {
        // Aquí se llamarán a los métodos para traducir cada instrucción
        // de tres direcciones a ensamblador.
        // Por ahora, solo es una estructura básica.

        generateHeader();
        translateInstructions();
        generateFooter();

        return assemblyCode;
    }

    private void generateHeader() {
        assemblyCode.add("section .data");
        // Aquí se podrían definir datos, como strings para Escribir.
        assemblyCode.add("section .text");
        assemblyCode.add("global _start");
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
                handleAssignment(instruction);
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

    private void handleWrite(String instruction) {
        // Esta es una implementación muy simplificada para escribir números.
        // Escribir strings requeriría más código para manejar syscalls de escritura.
        String var = instruction.substring(6).trim();
        assemblyCode.add("    ; Escribiendo valor de " + var);
        assemblyCode.add("    mov eax, [" + var + "]");
        assemblyCode.add("    ; ... (código para imprimir el número en EAX)");
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
    }
}
