package org.example;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainFrame extends JFrame {

    private final StudentManager manager;
    private JTable studentTable;
    private DefaultTableModel tableModel;
    private JTextArea logArea;
    private JPanel chartContainer;
    private JTextField nameField;
    private JSpinner ageSpinner;
    private JTextField gradeField;
    private JLabel statsLabel;
    private Map<String, JCheckBox> courseCheckboxes; // For adding students

    public MainFrame() {
        this.manager = StudentManagerImpl.getInstance();
        initUI();
        refreshData();
    }

    private void initUI() {
        setTitle("Student Management System");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createInputPanel(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Student List", createListPanel());

        chartContainer = new JPanel(new BorderLayout());
        tabbedPane.addTab("Analytics", chartContainer);

        logArea = new JTextArea();
        logArea.setEditable(false);
        tabbedPane.addTab("System Logs", new JScrollPane(logArea));

        add(tabbedPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statsLabel = new JLabel(" Ready");
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        statusPanel.add(statsLabel, BorderLayout.WEST);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createInputPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createTitledBorder("Add New Student"));

        JPanel dataPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        nameField = new JTextField(12);
        ageSpinner = new JSpinner(new SpinnerNumberModel(18, 18, 100, 1));
        gradeField = new JTextField(4);
        gradeField.setToolTipText("0.0 - 100.0");

        dataPanel.add(new JLabel("Name:"));
        dataPanel.add(nameField);
        dataPanel.add(new JLabel("Age:"));
        dataPanel.add(ageSpinner);
        dataPanel.add(new JLabel("Grade:"));
        dataPanel.add(gradeField);

        JPanel coursesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        coursesPanel.setBorder(BorderFactory.createTitledBorder("Enroll in Courses:"));
        courseCheckboxes = new HashMap<>();
        Map<String, String> dbCourses = manager.getAllCourses();

        for (Map.Entry<String, String> entry : dbCourses.entrySet()) {
            JCheckBox cb = new JCheckBox(entry.getValue());
            courseCheckboxes.put(entry.getKey(), cb);
            coursesPanel.add(cb);
        }

        JButton addButton = new JButton("Add Student");
        addButton.addActionListener(e -> addStudentAction());

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(dataPanel, BorderLayout.NORTH);
        centerPanel.add(coursesPanel, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(addButton, BorderLayout.EAST);

        return mainPanel;
    }

    private JPanel createListPanel() {
        JPanel listPanel = new JPanel(new BorderLayout());
        String[] columns = {"ID", "Name", "Age", "Grade", "Enrollment Date", "Courses"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        studentTable = new JTable(tableModel);

        studentTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == 3) {
                    try {
                        double val = Double.parseDouble(value.toString().replace(",", "."));
                        if (val < 50) c.setForeground(Color.RED);
                        else if (val >= 90) c.setForeground(new Color(0, 128, 0));
                        else c.setForeground(Color.BLACK);
                    } catch (Exception e) {/*ignore*/}
                } else c.setForeground(Color.BLACK);
                return c;
            }
        });

        listPanel.add(new JScrollPane(studentTable), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(15);
        searchField.setBorder(BorderFactory.createTitledBorder("Search"));
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                if(searchField.getText().isEmpty()) refreshData();
                else searchStudents(searchField.getText());
            }
        });

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshData());

        JButton editBtn = new JButton("Edit Selected");
        editBtn.addActionListener(e -> editSelectedStudent());

        JButton delBtn = new JButton("Remove");
        delBtn.setBackground(new Color(255, 200, 200));
        delBtn.addActionListener(e -> deleteSelectedStudent());

        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(e -> exportToCSV());

        controls.add(searchField);
        controls.add(refreshBtn);
        controls.add(editBtn);
        controls.add(delBtn);
        controls.add(exportBtn);

        listPanel.add(controls, BorderLayout.SOUTH);
        return listPanel;
    }

    private void addStudentAction() {
        try {
            String name = nameField.getText();
            int age = (int) ageSpinner.getValue();
            double grade = Double.parseDouble(gradeField.getText().replace(",", "."));
            if (name.isEmpty()) throw new IllegalArgumentException("Name is required");

            Student s = new Student(name, age, grade);
            for (Map.Entry<String, JCheckBox> entry : courseCheckboxes.entrySet()) {
                if (entry.getValue().isSelected()) s.addCourse(entry.getKey());
            }

            setEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() { manager.addStudent(s); return null; }
                @Override protected void done() {
                    try { get(); log("Added: " + name); refreshData(); clearInputs(); }
                    catch (Exception ex) { handleException(ex); }
                    finally { setEnabled(true); }
                }
            }.execute();
        } catch (Exception e) { handleException(e); }
    }

    private void editSelectedStudent() {
        int row = studentTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a student."); return; }

        String id = (String) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        int age = (int) tableModel.getValueAt(row, 2);
        String grade = (String) tableModel.getValueAt(row, 3);
        Object dateObj = tableModel.getValueAt(row, 4);

        // Get current courses from table model
        Object coursesObj = tableModel.getValueAt(row, 5);
        List<?> currentCourses = (coursesObj instanceof List) ? (List<?>) coursesObj : new ArrayList<>();

        // Edit UI
        JTextField nameIn = new JTextField(name);
        JSpinner ageIn = new JSpinner(new SpinnerNumberModel(age, 18, 100, 1));
        JTextField gradeIn = new JTextField(grade.replace(",", "."));

        // --- NEW PART: Course panel for editing ---
        JPanel coursesPanel = new JPanel(new GridLayout(0, 2));
        coursesPanel.setBorder(BorderFactory.createTitledBorder("Edit Courses"));
        Map<String, JCheckBox> editCheckboxes = new HashMap<>();
        Map<String, String> allCourses = manager.getAllCourses();

        for (Map.Entry<String, String> entry : allCourses.entrySet()) {
            JCheckBox cb = new JCheckBox(entry.getValue());
            // If student is already enrolled - check the box
            if (currentCourses.contains(entry.getKey())) {
                cb.setSelected(true);
            }
            editCheckboxes.put(entry.getKey(), cb);
            coursesPanel.add(cb);
        }
        // -----------------------------------------------------

        Object[] msg = {"Name:", nameIn, "Age:", ageIn, "Grade:", gradeIn, coursesPanel};

        if (JOptionPane.showConfirmDialog(this, msg, "Edit Student", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                double newGrade = Double.parseDouble(gradeIn.getText());

                // Collect new selected courses
                ArrayList<String> newCoursesList = new ArrayList<>();
                for (Map.Entry<String, JCheckBox> entry : editCheckboxes.entrySet()) {
                    if (entry.getValue().isSelected()) newCoursesList.add(entry.getKey());
                }

                LocalDate regDate = (dateObj instanceof LocalDate) ? (LocalDate) dateObj : LocalDate.parse(dateObj.toString());
                Student updated = new Student(id, nameIn.getText(), (int) ageIn.getValue(), newGrade, regDate, newCoursesList);

                manager.updateStudent(id, updated);
                log("Updated: " + id);
                refreshData();
            } catch (Exception e) { handleException(e); }
        }
    }

    private void deleteSelectedStudent() {
        int row = studentTable.getSelectedRow();
        if (row == -1) return;
        String id = (String) tableModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "Delete " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            manager.removeStudent(id);
            log("Deleted: " + id);
            refreshData();
        }
    }

    private void refreshData() {
        List<Student> list = manager.displayAllStudents();
        updateTable(list);
        updateChart(list);
        statsLabel.setText(" Total: " + list.size() + " | Avg Grade: " + String.format("%.2f", manager.calculateAverageGrade()));
    }

    private void updateTable(List<Student> list) {
        tableModel.setRowCount(0);
        for (Student s : list) {
            tableModel.addRow(new Object[]{
                    s.getStudentID(), s.getName(), s.getAge(), String.format("%.2f", s.getGrade()),
                    s.getEnrollmentDate(), s.getCourses()
            });
        }
    }

    private void updateChart(List<Student> students) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int[] counts = new int[5];
        for (Student s : students) {
            double g = s.getGrade();
            if (g >= 90) counts[0]++;
            else if (g >= 80) counts[1]++;
            else if (g >= 70) counts[2]++;
            else if (g >= 60) counts[3]++;
            else counts[4]++;
        }
        dataset.addValue(counts[0], "Grades", "A");
        dataset.addValue(counts[1], "Grades", "B");
        dataset.addValue(counts[2], "Grades", "C");
        dataset.addValue(counts[3], "Grades", "D");
        dataset.addValue(counts[4], "Grades", "F");

        JFreeChart chart = ChartFactory.createBarChart("Grade Distribution", "Grade", "Count", dataset, PlotOrientation.VERTICAL, false, true, false);
        chartContainer.removeAll();
        chartContainer.add(new ChartPanel(chart));
        chartContainer.validate();
    }

    private void searchStudents(String q) { updateTable(manager.searchStudents(q)); }
    private void exportToCSV() {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try { manager.exportStudentsToCSV(fc.getSelectedFile().getAbsolutePath()); }
            catch(Exception e) { handleException(e); }
        }
    }
    private void clearInputs() { nameField.setText(""); gradeField.setText(""); courseCheckboxes.values().forEach(c->c.setSelected(false)); }
    private void log(String s) { logArea.append(new java.util.Date() + ": " + s + "\n"); }
    private void handleException(Throwable t) {
        if(t instanceof ExecutionException) t = t.getCause();
        JOptionPane.showMessageDialog(this, "Error: " + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}