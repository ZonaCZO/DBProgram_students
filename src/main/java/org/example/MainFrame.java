package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Основное окно приложения (GUI).
 * Реализует паттерн MVC (как View и Controller).
 */
public class MainFrame extends JFrame {

    private final StudentManager manager;
    
    // Компоненты UI
    private JTable studentTable;
    private DefaultTableModel tableModel;
    private JTextArea logArea;
    
    // Поля ввода
    private JTextField nameField;
    private JSpinner ageSpinner;
    private JTextField gradeField; // Используем Field с валидацией вместо слайдера для точности
    private JButton refreshButton;
    private JLabel statsLabel;

    public MainFrame() {
        // Получаем экземпляр менеджера (Singleton)
        this.manager = StudentManagerImpl.getInstance();
        
        initUI();
        refreshTableData(); // Загрузка данных при старте
    }

    private void initUI() {
        setTitle("Student Management System");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Верхняя панель (Ввод данных) [cite: 73] ---
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.NORTH);

        // --- Центральная панель (Таблицы и Вкладки) [cite: 96] ---
        JTabbedPane tabbedPane = new JTabbedPane();

        // Вкладка 1: Список студентов
        JPanel listPanel = new JPanel(new BorderLayout());
        
        // Настройка таблицы
        String[] columns = {"ID", "Name", "Age", "Grade", "Enrollment Date"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Запрет прямого редактирования ячеек
            }
        };
        studentTable = new JTable(tableModel);
        
        // Кастомный рендерер для цветового кодирования оценок [cite: 97]
        studentTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == 3) { // Колонка Grade
                    try {
                        double val = Double.parseDouble(value.toString().replace(",", "."));
                        if (val < 50) c.setForeground(Color.RED);
                        else if (val >= 90) c.setForeground(new Color(0, 128, 0)); // Green
                        else c.setForeground(Color.BLACK);
                    } catch (Exception e) { /* ignore */ }
                } else {
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });

        listPanel.add(new JScrollPane(studentTable), BorderLayout.CENTER);
        
        // Панель поиска и управления таблицей
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(20);
        searchField.setBorder(BorderFactory.createTitledBorder("Search (Name/ID)"));
        // "Живой" поиск [cite: 90]
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String query = searchField.getText();
                if (query.isEmpty()) refreshTableData();
                else searchStudents(query);
            }
        });
        
        refreshButton = new JButton("Refresh / Show All");
        refreshButton.addActionListener(e -> refreshTableData());
        
        JButton deleteButton = new JButton("Remove Selected");
        deleteButton.setBackground(new Color(255, 200, 200));
        deleteButton.addActionListener(e -> deleteSelectedStudent());
        
        JButton exportButton = new JButton("Export CSV");
        exportButton.addActionListener(e -> exportToCSV());

        controlPanel.add(searchField);
        controlPanel.add(refreshButton);
        controlPanel.add(deleteButton);
        controlPanel.add(exportButton);
        
        listPanel.add(controlPanel, BorderLayout.SOUTH);
        tabbedPane.addTab("Student List", listPanel);

        // Вкладка 2: Логи [cite: 98]
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tabbedPane.addTab("System Logs", new JScrollPane(logArea));

        add(tabbedPane, BorderLayout.CENTER);

        // --- Нижняя панель (Статус и Аналитика) ---
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        statsLabel = new JLabel(" Ready");
        statusPanel.add(statsLabel, BorderLayout.WEST);
        
        JButton calcAvgButton = new JButton("Calculate Average Grade");
        calcAvgButton.addActionListener(e -> calculateStats());
        statusPanel.add(calcAvgButton, BorderLayout.EAST);
        
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Add New Student"));

        nameField = new JTextField(15);
        // Добавляем подсказки [cite: 82]
        nameField.setToolTipText("Enter full name (letters only)");
        
        ageSpinner = new JSpinner(new SpinnerNumberModel(18, 18, 100, 1)); // Валидация возраста в UI
        
        gradeField = new JTextField(5);
        gradeField.setToolTipText("0.0 - 100.0");

        JButton addButton = new JButton("Add Student");
        addButton.setFont(new Font("Arial", Font.BOLD, 12));
        addButton.addActionListener(e -> addStudentAction());

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Age:"));
        panel.add(ageSpinner);
        panel.add(new JLabel("Grade:"));
        panel.add(gradeField);
        panel.add(addButton);

        return panel;
    }

    // --- Actions & Logic ---

    /**
     * Добавление студента с использованием SwingWorker для фонового выполнения.
     * Требование [cite: 84, 101]
     */
    private void addStudentAction() {
        try {
            String name = nameField.getText();
            int age = (int) ageSpinner.getValue();
            double grade = Double.parseDouble(gradeField.getText().replace(",", "."));

            // Предварительная валидация UI
            if (name.isEmpty()) throw new IllegalArgumentException("Name cannot be empty");

            // Блокируем кнопку на время операции
            setEnabled(false);
            log("Starting background task: Add Student...");

            // SwingWorker <ResultType, ProgressType>
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    // Тяжелая операция в базе данных
                    Student newStudent = new Student(name, age, grade);
                    manager.addStudent(newStudent);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // Проверяем на исключения
                        log("Success: Student added.");
                        JOptionPane.showMessageDialog(MainFrame.this, "Student added successfully!");
                        
                        // Очистка полей
                        nameField.setText("");
                        gradeField.setText("");
                        refreshTableData();
                        
                    } catch (ExecutionException ex) {
                        handleException(ex.getCause());
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } finally {
                        setEnabled(true);
                    }
                }
            };
            worker.execute();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Grade format. Use numbers (e.g., 85.5).", "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void deleteSelectedStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a student to remove.");
            return;
        }

        String id = (String) tableModel.getValueAt(selectedRow, 0);
        
        // Подтверждение удаления [cite: 85]
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete student ID: " + id + "?", 
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);
                
        if (confirm == JOptionPane.YES_OPTION) {
            manager.removeStudent(id);
            log("Deleted student: " + id);
            refreshTableData();
        }
    }

    private void searchStudents(String query) {
        List<Student> results = manager.searchStudents(query);
        updateTableModel(results);
    }

    private void refreshTableData() {
        List<Student> students = manager.displayAllStudents();
        updateTableModel(students);
        log("Data refreshed. Total records: " + students.size());
    }

    private void updateTableModel(List<Student> students) {
        tableModel.setRowCount(0); // Очистка
        for (Student s : students) {
            tableModel.addRow(new Object[]{
                    s.getStudentID(),
                    s.getName(),
                    s.getAge(),
                    String.format("%.2f", s.getGrade()),
                    s.getEnrollmentDate()
            });
        }
    }
    
    private void calculateStats() {
        double avg = manager.calculateAverageGrade();
        statsLabel.setText(String.format(" Class Average Grade: %.2f", avg));
        JOptionPane.showMessageDialog(this, "Average Grade of all students: " + String.format("%.2f", avg));
    }

    private void exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getAbsolutePath();
            if (!path.endsWith(".csv")) path += ".csv";
            
            try {
                manager.exportStudentsToCSV(path);
                log("Exported to: " + path);
                JOptionPane.showMessageDialog(this, "Export Successful!");
            } catch (Exception e) {
                handleException(e);
            }
        }
    }

    private void log(String message) {
        logArea.append(new java.util.Date() + ": " + message + "\n");
        // Автопрокрутка вниз
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void handleException(Throwable t) {
        log("Error: " + t.getMessage());
        JOptionPane.showMessageDialog(this, t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}