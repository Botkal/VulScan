
package com.vulscan.dashboard.dto;

import java.util.List;

public class ImportResultDto {
    private int rowsRead;
    private int rowsInserted;
    private int rowsRejected;
    private List<String> errors;

    public ImportResultDto() {}

    public ImportResultDto(int rowsRead, int rowsInserted, int rowsRejected, List<String> errors) {
        this.rowsRead = rowsRead;
        this.rowsInserted = rowsInserted;
        this.rowsRejected = rowsRejected;
        this.errors = errors;
    }

    public int getRowsRead() {
        return rowsRead;
    }

    public void setRowsRead(int rowsRead) {
        this.rowsRead = rowsRead;
    }

    public int getRowsInserted() {
        return rowsInserted;
    }

    public void setRowsInserted(int rowsInserted) {
        this.rowsInserted = rowsInserted;
    }

    public int getRowsRejected() {
        return rowsRejected;
    }

    public void setRowsRejected(int rowsRejected) {
        this.rowsRejected = rowsRejected;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
