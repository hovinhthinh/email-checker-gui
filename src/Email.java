/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author hovinhthinh
 */
public class Email implements Comparable<Email> {

    public String email;
    public String lineInfo;

    public int tableIndex = -1; // Index in the table.
    public int status = -1; // -1: not_checked, 0: invalid, 1: valid, -2: connection-error

    public StringBuilder log = new StringBuilder();

    private Email() {
    }

    public static Email parseFromLine(String line, String separator, int columnIndex) {
        String[] data = line.split(separator);
        if (columnIndex >= data.length) {
            return null;
        }
        Email e = new Email();
        e.lineInfo = line;
        e.email = data[columnIndex];
        return e;
    }

    @Override
    public int compareTo(Email o) {
        return email.compareTo(o.email);
    }

    public String getStatusString() {
        if (status == -2) {
            return "connection-error";
        }
        if (status == -1) {
            return "to-be-checked";
        }
        if (status == 0) {
            return "invalid";
        }
        if (status == 1) {
            return "valid";
        }

        return "Undefined";
    }

    @Override
    public String toString() {
        return email;
    }
}
