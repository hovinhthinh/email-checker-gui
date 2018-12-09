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

    public static Email parseFromLine(String line, String separator, int columnIndex, boolean isRecheck) {
        String[] data = line.split(separator);
        if (columnIndex >= data.length) {
            return null;
        }
        Email e = new Email();
        if (!isRecheck) {
            e.lineInfo = line;
        } else {
            int p = line.lastIndexOf(separator);
            e.lineInfo = line.substring(0, p);
            String status = line.substring(p + 1);
            if (status.equals(Main.INVALID)) {
                e.status = 0;
            } else if (status.equals(Main.VALID)) {
                e.status = 1;
            }
        }

        e.email = data[columnIndex];
        return e;
    }

    @Override
    public int compareTo(Email o) {
        if (status != o.status) {
            return Integer.compare(o.status, status);
        }
        return email.compareTo(o.email);
    }

    public String getStatusString() {
        if (status == -2) {
            return Main.UNKNOWN;
        }
        if (status == -1) {
            return "to-be-checked";
        }
        if (status == 0) {
            return Main.INVALID;
        }
        if (status == 1) {
            return Main.VALID;
        }

        return "Undefined";
    }

    @Override
    public String toString() {
        return email;
    }
}
