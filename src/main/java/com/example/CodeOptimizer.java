package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeOptimizer {

    private List<String> originalCode;
    private List<String> optimizedCode;

    public CodeOptimizer(List<String> threeAddressCode) {
        this.originalCode = threeAddressCode;
        this.optimizedCode = new ArrayList<>(threeAddressCode);
    }

    public List<String> optimize() {
        propagateConstants();
        removeRedundantJumps();
        eliminateTemporaryVariables();
        return optimizedCode;
    }

    private void removeRedundantJumps() {
        List<String> tempCode = new ArrayList<>();
        Map<String, Integer> labelLocations = new HashMap<>();

        // Primero, encontrar todas las ubicaciones de las etiquetas
        for (int i = 0; i < optimizedCode.size(); i++) {
            String instruction = optimizedCode.get(i).trim();
            if (instruction.endsWith(":")) {
                labelLocations.put(instruction.substring(0, instruction.length() - 1), i);
            }
        }

        for (int i = 0; i < optimizedCode.size(); i++) {
            String instruction = optimizedCode.get(i).trim();
            boolean isRedundant = false;

            if (instruction.startsWith("goto ")) {
                String label = instruction.substring(5).trim();
                if (labelLocations.containsKey(label)) {
                    int labelIndex = labelLocations.get(label);
                    // Si la etiqueta está en la siguiente línea, el salto es redundante.
                    if (labelIndex == i + 1) {
                        isRedundant = true;
                    }
                }
            }

            if (!isRedundant) {
                tempCode.add(optimizedCode.get(i));
            }
        }
        optimizedCode = tempCode;
    }

    private void propagateConstants() {
        Map<String, String> constants = new HashMap<>();
        List<String> newCode = new ArrayList<>();

        for (String instruction : optimizedCode) {
            String[] parts = instruction.split(" ");
            if (parts.length == 3 && parts[1].equals("=")) {
                // Es una asignación: var = valor
                String var = parts[0].trim();
                String val = parts[2].trim();
                // Si el valor es un número, lo consideramos una constante.
                if (isNumeric(val)) {
                    constants.put(var, val);
                    // La instrucción de asignación se mantiene por ahora,
                    // podría ser eliminada si la variable no se usa más adelante (eliminación de código muerto).
                } else {
                    // Si se asigna desde otra variable, propagamos si es constante.
                    if (constants.containsKey(val)) {
                        val = constants.get(val);
                    }
                    constants.put(var, val);
                }
            } else if (parts.length > 3) { // Ej: t0 = a + 5
                // Reemplazar operandos si son constantes conocidas.
                for (int i = 2; i < parts.length; i++) {
                    if (constants.containsKey(parts[i])) {
                        parts[i] = constants.get(parts[i]);
                    }
                }
                instruction = String.join(" ", parts);
            }
            newCode.add(instruction);
        }
        optimizedCode = newCode;
    }

    private void eliminateTemporaryVariables() {
        List<String> newCode = new ArrayList<>();
        for (String instruction : optimizedCode) {
            if (!instruction.startsWith("t")) {
                newCode.add(instruction);
            }
        }
        optimizedCode = newCode;
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
