package com.example;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection; // Para aceptar Collection de la tabla de símbolos

public class SimboloTableModel extends AbstractTableModel {
    private final String[] columnNames = {"Nombre", "Tipo", "Categoría", "Alcance", "Línea", "Valor"};
    private List<Simbolo> simbolos;

    public SimboloTableModel() {
        this.simbolos = new ArrayList<>();
    }

    public void setSimbolos(Collection<Simbolo> simbolosCollection) {
        this.simbolos = new ArrayList<>(simbolosCollection);
        fireTableDataChanged(); // Notificar a la JTable que los datos cambiaron
    }

    public void clearData() {
        this.simbolos.clear();
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return simbolos.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Simbolo simbolo = simbolos.get(rowIndex);
        switch (columnIndex) {
            case 0: return simbolo.getNombre();
            case 1: return simbolo.getTipo();
            case 2: return simbolo.getCategoria();
            case 3: return simbolo.getAlcance();
            case 4: return simbolo.getLinea();
            case 5: return simbolo.getValor() != null ? simbolo.getValor().toString() : "<indefinido>";
            default: return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0: return String.class;
            case 1: return String.class;
            case 2: return String.class;
            case 3: return String.class;
            case 4: return Integer.class;
            case 5: return String.class; // Valor se formatea como String
            default: return Object.class;
        }
    }
}
