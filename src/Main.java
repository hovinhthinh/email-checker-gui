
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author hovinhthinh
 */
public class Main extends javax.swing.JFrame {

    private String separator;
    private File input, output;

    /**
     * Creates new form EmailChecker
     */
    public Main() {
        initComponents();

        dataTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (dataTable.getSelectedRow() > -1) {
                    logTextArea.setText(((Email) ((DefaultTableModel) dataTable.getModel()).getValueAt(dataTable.getSelectedRow(), 1)).log.toString());
                }
            }
        });
    }

    public void log(String info) {
        systemLogTextArea.append(info + "\r\n");
    }

    private boolean outputFileIsValid(File outputFile) {
        if (outputFile.exists()) {
            int result = JOptionPane.showConfirmDialog(
                    outputFileButton.getParent(),
                    "File exists, overwrite?", "File exists",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            switch (result) {
                case JOptionPane.YES_OPTION:
                    return true;
                default:
                    return false;
            }
        }
        return true;
    }

    public synchronized void updateEmail(Email e) {
        DefaultTableModel tableModel = (DefaultTableModel) dataTable.getModel();
        tableModel.setValueAt(e.getStatusString(), e.tableIndex, 2);
    }

    private void resetGUI() {
        input = output = null;
        inputFileLabel.setText("No chosen file");
        outputFileLabel.setText("No chosen file");
        inputFileButton.setEnabled(true);
        outputFileButton.setEnabled(false);
        googleMXCheckOnly.setEnabled(true);
        numberOfWorkersComboBox.setEnabled(true);

        startButton.setText("Start");
        startButton.setEnabled(false);
    }

    private boolean check() {
        log("Reading data.");
        log("Charset decoder: UTF-8");
        ArrayList<String> lines = Utils.getLinesFromFile(input);
        int columnIndex;
        if (lines.size() > 0) {
            String[] columns = lines.get(0).split(separator);
            for (columnIndex = 0; columnIndex < columns.length; ++columnIndex) {
                if (columns[columnIndex].contains("@")) {
                    break;
                }
            }
            if (columnIndex == columns.length) {
                log("Cannot detect emails in data.");
                return false;
            }
        } else {
            log("Input file is empty.");
            return false;
        }
        TreeSet<Email> emails = new TreeSet<>();
        for (String line : lines) {
            emails.add(Email.parseFromLine(line, separator, columnIndex));
        }
        log("Removed total " + (lines.size() - emails.size()) + " duplicated emails.");

        BlockingQueue<Email> queue = new LinkedBlockingQueue<>();

        ArrayList<Email> toShuffle = new ArrayList<>();
        toShuffle.addAll(emails);
        Collections.shuffle(toShuffle);

        DefaultTableModel tableModel = (DefaultTableModel) dataTable.getModel();
        tableModel.setRowCount(0);
        for (int i = 0; i < toShuffle.size(); ++i) {
            Email e = toShuffle.get(i);
            e.tableIndex = i;
            tableModel.addRow(new Object[]{e.lineInfo, e, e.getStatusString()});
        }
        queue.addAll(toShuffle);

        log("Checking total " + emails.size() + " emails.");
        try {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), "UTF-8"));
            int total = queue.size();
            AtomicInteger validCount = new AtomicInteger(0), invalidCount = new AtomicInteger(0), errCount = new AtomicInteger(0);

            ArrayList<Thread> threads = new ArrayList<>();

            for (int i = 0; i < Integer.parseInt(numberOfWorkersComboBox.getSelectedItem().toString()); ++i) {
                threads.add(new Worker(queue, out, validCount, invalidCount, errCount, googleMXCheckOnly.isSelected(), this, separator));
            }

            // Monitor.
            threads.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    log("Monitor started.");
                    int lastDone = 0;
                    LinkedList<Double> speedLog = new LinkedList<>();
                    double sumSpeed = 0;
                    try {
                        int done;
                        do {
                            Thread.sleep(10000);
                            done = (validCount.get() + invalidCount.get() + errCount.get());
                            double percent = (double) done / total;
                            double speed = (double) (done - lastDone) / 10;
                            lastDone = done;

                            String logString = String.format("Progress: %.2f%%               Speed: %.1f/s               Valid: %d Invalid: %d Unknown: %d", percent * 100,
                                    speed, validCount.get(), invalidCount.get(), errCount.get());
                            sumSpeed += speed;
                            speedLog.add(speed);
                            if (speedLog.size() > 10) {
                                sumSpeed -= speedLog.removeFirst();
                            }

                            int etr = (Math.abs(sumSpeed) < 1e-3) ? -1
                                    : (int) ((total - done) / (sumSpeed / speedLog.size()));
                            if (etr != -1) {
                                logString += String.format("               ETR: %02d:%02d:%02d", etr / 3600, (etr % 3600) / 60, etr % 60);
                            } else {
                                logString += String.format("               ETR: --:--:--");
                            }
                            progressLabel.setText(logString);
                            if (done == total) {
                                log("Monitor stopped.");
                                log("Done.");
                                log("Output: " + output.getAbsolutePath());
                                resetGUI();
                            }
                        } while (done != total);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }));

            for (Thread t : threads) {
                t.start();
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        inputFileButton = new javax.swing.JButton();
        outputFileButton = new javax.swing.JButton();
        inputFileLabel = new javax.swing.JLabel();
        outputFileLabel = new javax.swing.JLabel();
        googleMXCheckOnly = new javax.swing.JCheckBox();
        numberOfWorkersComboBox = new javax.swing.JComboBox();
        numberOfWorkersLabel = new javax.swing.JLabel();
        dataPanel = new javax.swing.JScrollPane();
        dataTable = new javax.swing.JTable();
        progressLabel = new javax.swing.JLabel();
        startButton = new javax.swing.JButton();
        logPanel = new javax.swing.JScrollPane();
        logTextArea = new javax.swing.JTextArea();
        systemLogLabel = new javax.swing.JLabel();
        systemLogPanel = new javax.swing.JScrollPane();
        systemLogTextArea = new javax.swing.JTextArea();
        checkLogLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("email-checker (designed for ngocle6993)");
        setPreferredSize(new java.awt.Dimension(1024, 768));

        inputFileButton.setText("Choose Input File");
        inputFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputFileButtonActionPerformed(evt);
            }
        });

        outputFileButton.setText("Choose Output File");
        outputFileButton.setEnabled(false);
        outputFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                outputFileButtonActionPerformed(evt);
            }
        });

        inputFileLabel.setText("No chosen file");

        outputFileLabel.setText("No chosen file");

        googleMXCheckOnly.setSelected(true);
        googleMXCheckOnly.setText("Check GG MX records only");
        googleMXCheckOnly.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                googleMXCheckOnlyActionPerformed(evt);
            }
        });

        numberOfWorkersComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "4", "8", "16", "32", "64" }));
        numberOfWorkersComboBox.setSelectedIndex(4);

        numberOfWorkersLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        numberOfWorkersLabel.setText("Number of threads");

        dataTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Data", "Email", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        dataTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        dataPanel.setViewportView(dataTable);
        if (dataTable.getColumnModel().getColumnCount() > 0) {
            dataTable.getColumnModel().getColumn(0).setPreferredWidth(600);
            dataTable.getColumnModel().getColumn(1).setPreferredWidth(300);
            dataTable.getColumnModel().getColumn(2).setPreferredWidth(150);
            dataTable.getColumnModel().getColumn(2).setMaxWidth(150);
        }

        progressLabel.setText("Progress: --               Speed: --               Valid: -- Invalid: -- Unknown: --               ETR: --:--:--");

        startButton.setFont(new java.awt.Font("Lucida Grande", 0, 36)); // NOI18N
        startButton.setText("Start");
        startButton.setEnabled(false);
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        logTextArea.setEditable(false);
        logTextArea.setColumns(20);
        logTextArea.setRows(5);
        logPanel.setViewportView(logTextArea);

        systemLogLabel.setText("System log");

        systemLogTextArea.setEditable(false);
        systemLogTextArea.setColumns(20);
        systemLogTextArea.setRows(5);
        systemLogPanel.setViewportView(systemLogTextArea);

        checkLogLabel.setText("Check log");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(outputFileButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(inputFileButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(inputFileLabel)
                                    .addComponent(outputFileLabel))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(dataPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 565, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(checkLogLabel)
                            .addComponent(logPanel, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(startButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addComponent(numberOfWorkersLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(numberOfWorkersComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(googleMXCheckOnly, javax.swing.GroupLayout.Alignment.TRAILING))
                            .addComponent(systemLogPanel)
                            .addComponent(systemLogLabel))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(inputFileButton)
                    .addComponent(inputFileLabel)
                    .addComponent(numberOfWorkersComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(numberOfWorkersLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(outputFileButton)
                    .addComponent(outputFileLabel)
                    .addComponent(googleMXCheckOnly))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(systemLogLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(systemLogPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(checkLogLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(logPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 297, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(startButton))
                    .addComponent(dataPanel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressLabel)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void inputFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputFileButtonActionPerformed
        final JFileChooser fc = new JFileChooser();
        fc.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                String extension = Utils.getFileExtension(f);
                if (extension != null) {
                    return extension.equals(Utils.CSV) || extension.equals(Utils.TSV);
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "CSV, TSV";
            }
        });
        fc.setAcceptAllFileFilterUsed(false);

        int returnVal = fc.showOpenDialog(Main.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            input = file;
            String inputName = input.getAbsolutePath();
            if (inputName.length() > 50) {
                inputFileLabel.setText("..." + inputName.substring(inputName.length() - 47));
            } else {
                inputFileLabel.setText(inputName);
            }

            if (Utils.getFileExtension(input).equals(Utils.CSV)) {
                log("Input file uses CSV format.");
                separator = ",";
            } else if (Utils.getFileExtension(input).equals(Utils.TSV)) {
                log("Input file uses TSV format.");
                separator = "\t";
            }

            String outputName = input.getName();
            outputName = outputName.substring(0, outputName.length() - 4) + "_(out)" + outputName.substring(outputName.length() - 4);
            output = new File(input.getParentFile(), outputName);
            log("Automatically set output file path.");
            outputName = output.getAbsolutePath();
            if (outputName.length() > 50) {
                outputFileLabel.setText("..." + outputName.substring(outputName.length() - 47));
            } else {
                outputFileLabel.setText(outputName);
            }
            outputFileButton.setEnabled(true);
            startButton.setEnabled(true);
        } else {
        }
    }//GEN-LAST:event_inputFileButtonActionPerformed

    private void outputFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_outputFileButtonActionPerformed
        final JFileChooser fc = new JFileChooser();

        if (Utils.getFileExtension(input).equals(Utils.CSV)) {
            fc.addChoosableFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        return true;
                    }
                    String extension = Utils.getFileExtension(f);
                    if (extension != null) {
                        return extension.equals(Utils.CSV);
                    }
                    return false;
                }

                @Override
                public String getDescription() {
                    return "CSV";
                }
            });
        } else if (Utils.getFileExtension(input).equals(Utils.TSV)) {
            fc.addChoosableFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        return true;
                    }
                    String extension = Utils.getFileExtension(f);
                    if (extension != null) {
                        return extension.equals(Utils.TSV);
                    }
                    return false;
                }

                @Override
                public String getDescription() {
                    return "TSV";
                }
            });
        }
        fc.setAcceptAllFileFilterUsed(false);

        int returnVal = fc.showSaveDialog(Main.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String extension = Utils.getFileExtension(file);
            if (extension == null || !extension.equals(Utils.getFileExtension(input))) {
                file = new File(file.toString() + "." + Utils.getFileExtension(input));
            }
            if (outputFileIsValid(file)) {
                output = file;
                String outputName = output.getAbsolutePath();
                if (outputName.length() > 50) {
                    outputFileLabel.setText("..." + outputName.substring(outputName.length() - 47));
                } else {
                    outputFileLabel.setText(outputName);
                }
                log("Manually set output file path.");
            }
        }
    }//GEN-LAST:event_outputFileButtonActionPerformed

    private void googleMXCheckOnlyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_googleMXCheckOnlyActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_googleMXCheckOnlyActionPerformed

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        inputFileButton.setEnabled(false);
        outputFileButton.setEnabled(false);
        googleMXCheckOnly.setEnabled(false);
        numberOfWorkersComboBox.setEnabled(false);

        startButton.setText("Checking...");
        startButton.setEnabled(false);

        check();
    }//GEN-LAST:event_startButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;

                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Main.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Main.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Main.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Main().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel checkLogLabel;
    private javax.swing.JScrollPane dataPanel;
    private javax.swing.JTable dataTable;
    private javax.swing.JCheckBox googleMXCheckOnly;
    private javax.swing.JButton inputFileButton;
    private javax.swing.JLabel inputFileLabel;
    private javax.swing.JScrollPane logPanel;
    private javax.swing.JTextArea logTextArea;
    private javax.swing.JComboBox numberOfWorkersComboBox;
    private javax.swing.JLabel numberOfWorkersLabel;
    private javax.swing.JButton outputFileButton;
    private javax.swing.JLabel outputFileLabel;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JButton startButton;
    private javax.swing.JLabel systemLogLabel;
    private javax.swing.JScrollPane systemLogPanel;
    private javax.swing.JTextArea systemLogTextArea;
    // End of variables declaration//GEN-END:variables
}
