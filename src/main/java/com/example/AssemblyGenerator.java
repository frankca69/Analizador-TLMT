package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssemblyGenerator {

    private List<String> threeAddressCode;
    private List<String> assemblyCode;
    private Map<String, String> variables = new HashMap<>();
    private List<String> strings = new ArrayList<>();

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
        // Ya no se declaran en .bss. Se declararán en .data sobre la marcha.
    }

    private boolean isVariable(String s) {
        // Una suposición simple: si no es un número y no es una palabra clave, es una variable.
        // Esto debería ser más robusto en un compilador real.
        return !isNumeric(s) && !s.endsWith(":") && !s.equals("goto") && !s.equals("if_false") && !s.equals("write") && !s.equals("read") && !s.equals("start_program") && !s.equals("end_program");
    }

    private int stringIndex = 0;
    private void translateInstructions() {
        assemblyCode.add("_start:");
        stringIndex = 0;
        for (String instruction : threeAddressCode) {
            instruction = instruction.trim();
            String[] parts = instruction.split(" ");

            if (parts.length > 2 && parts[1].equals("=")) { // Asignación
                handleAssignment(instruction);
            } else if (instruction.startsWith("write")) {
                handleWrite(instruction);
            }
        }
    }

    private List<String> declaredVariables = new ArrayList<>();

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
        assemblyCode.add("    mov rax, 60         ; sys_exit");
        assemblyCode.add("    mov rdi, 0          ; código de salida");
        assemblyCode.add("    syscall");
        assemblyCode.add("");
        assemblyCode.add("print_number:");
        assemblyCode.add("    push rbx");
        assemblyCode.add("    push rcx");
        assemblyCode.add("    push rdx");
        assemblyCode.add("    push rsi");
        assemblyCode.add("    ");
        assemblyCode.add("    mov ebx, 10");
        assemblyCode.add("    mov ecx, 0");
        assemblyCode.add("    mov rsi, buffer + 11");
        assemblyCode.add("    mov byte [rsi], 0");
        assemblyCode.add("    ");
        assemblyCode.add("    cmp eax, 0");
        assemblyCode.add("    jne convert_loop");
        assemblyCode.add("    dec rsi");
        assemblyCode.add("    mov byte [rsi], '0'");
        assemblyCode.add("    inc ecx");
        assemblyCode.add("    jmp print_it");
        assemblyCode.add("    ");
        assemblyCode.add("convert_loop:");
        assemblyCode.add("    cmp eax, 0");
        assemblyCode.add("    je print_it");
        assemblyCode.add("    ");
        assemblyCode.add("    xor edx, edx");
        assemblyCode.add("    div ebx");
        assemblyCode.add("    add dl, '0'");
        assemblyCode.add("    dec rsi");
        assemblyCode.add("    mov [rsi], dl");
        assemblyCode.add("    inc ecx");
        assemblyCode.add("    jmp convert_loop");
        assemblyCode.add("    ");
        assemblyCode.add("print_it:");
        assemblyCode.add("    mov rax, 1");
        assemblyCode.add("    mov rdi, 1");
        assemblyCode.add("    mov rdx, rcx");
        assemblyCode.add("    syscall");
        assemblyCode.add("    ");
        assemblyCode.add("    pop rsi");
        assemblyCode.add("    pop rdx");
        assemblyCode.add("    pop rcx");
        assemblyCode.add("    pop rbx");
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
