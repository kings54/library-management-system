package library;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

public class Mainsub extends Application {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/library_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    
    // Tables
    private TableView<Book> booksTable = new TableView<>();
    private TableView<Student> studentsTable = new TableView<>();
    private TableView<Transaction> transactionsTable = new TableView<>();
    
    // Form fields
    private TextField tfBookId, tfBookTitle, tfBookAuthor, tfBookISBN, tfBookYear, tfBookQuantity, tfBookSearch;
    private ComboBox<String> cbBookCategory;
    private TextField tfStudentId, tfStudentName, tfStudentEmail, tfStudentPhone, tfStudentDept, tfStudentSearch;
    private ComboBox<String> cbStudentStatus;
    private TextField tfTransId, tfTransBookSearch, tfTransStudentSearch, tfFine;
    private ComboBox<String> cbTransStatus;
    private DatePicker dpBorrowDate, dpDueDate;
    
    @Override
    public void start(Stage stage) {
        stage.setTitle("📚 Library Management System");
        
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
            new Tab("📖 Books", createBooksTab()),
            new Tab("👨‍🎓 Students", createStudentsTab()),
            new Tab("🔄 Transactions", createTransactionsTab())
        );
        
        Scene scene = new Scene(tabPane, 1200, 750);
        stage.setScene(scene);
        stage.show();
        
        loadBooks();
        loadStudents();
        loadTransactions();
        
        stage.setOnCloseRequest(e -> closeConnection());
    }
    
    // ==================== BOOKS TAB ====================
    
    private VBox createBooksTab() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        
        // Search
        HBox searchBox = new HBox(10);
        tfBookSearch = new TextField();
        tfBookSearch.setPromptText("Search by title, author, ISBN...");
        Button btnSearch = new Button("Search");
        Button btnRefresh = new Button("Refresh");
        btnSearch.setOnAction(e -> searchBooks());
        btnRefresh.setOnAction(e -> loadBooks());
        searchBox.getChildren().addAll(new Label("Search:"), tfBookSearch, btnSearch, btnRefresh);
        
        // Table
        setupBooksTable();
        
        // Form
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        
        tfBookId = new TextField(); tfBookId.setEditable(false);
        tfBookTitle = new TextField();
        tfBookAuthor = new TextField();
        tfBookISBN = new TextField();
        cbBookCategory = new ComboBox<>();
        cbBookCategory.getItems().addAll("Fiction", "Non-Fiction", "Computer Science", "Business");
        tfBookYear = new TextField();
        tfBookQuantity = new TextField();
        
        Button btnAdd = new Button("Add");
        Button btnUpdate = new Button("Update");
        Button btnDelete = new Button("Delete");
        Button btnClear = new Button("Clear");
        
        btnAdd.setOnAction(e -> addBook());
        btnUpdate.setOnAction(e -> updateBook());
        btnDelete.setOnAction(e -> deleteBook());
        btnClear.setOnAction(e -> clearBookForm());
        
        form.add(new Label("ID:"), 0, 0); form.add(tfBookId, 1, 0);
        form.add(new Label("Title:*"), 0, 1); form.add(tfBookTitle, 1, 1);
        form.add(new Label("Author:*"), 0, 2); form.add(tfBookAuthor, 1, 2);
        form.add(new Label("ISBN:"), 2, 0); form.add(tfBookISBN, 3, 0);
        form.add(new Label("Category:*"), 2, 1); form.add(cbBookCategory, 3, 1);
        form.add(new Label("Year:"), 2, 2); form.add(tfBookYear, 3, 2);
        form.add(new Label("Quantity:*"), 0, 3); form.add(tfBookQuantity, 1, 3);
        form.add(btnAdd, 2, 3); form.add(btnUpdate, 3, 3);
        form.add(btnDelete, 0, 4); form.add(btnClear, 1, 4);
        
        vbox.getChildren().addAll(searchBox, booksTable, form);
        return vbox;
    }
    
    private void setupBooksTable() {
        TableColumn<Book, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Book, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        TableColumn<Book, String> colAuthor = new TableColumn<>("Author");
        colAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        TableColumn<Book, String> colISBN = new TableColumn<>("ISBN");
        colISBN.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        TableColumn<Book, String> colCategory = new TableColumn<>("Category");
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        TableColumn<Book, Integer> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<Book, Integer> colAvailable = new TableColumn<>("Available");
        colAvailable.setCellValueFactory(new PropertyValueFactory<>("available"));
        
        booksTable.getColumns().addAll(colId, colTitle, colAuthor, colISBN, colCategory, colTotal, colAvailable);
        booksTable.setPrefHeight(300);
        
        booksTable.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) populateBookForm(val);
        });
    }
    
    private void loadBooks() {
        ObservableList<Book> list = FXCollections.observableArrayList();
        String query = "SELECT * FROM Books";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                list.add(new Book(
                    rs.getInt("book_id"), rs.getString("title"),
                    rs.getString("author"), rs.getString("isbn"),
                    rs.getString("category"), rs.getInt("quantity"),
                    rs.getInt("available")
                ));
            }
            booksTable.setItems(list);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load books: " + e.getMessage());
        }
    }
    
    private void searchBooks() {
        String keyword = tfBookSearch.getText().trim();
        if (keyword.isEmpty()) { loadBooks(); return; }
        
        ObservableList<Book> results = FXCollections.observableArrayList();
        String query = "SELECT * FROM Books WHERE title LIKE ? OR author LIKE ? OR isbn LIKE ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            String pattern = "%" + keyword + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                results.add(new Book(rs.getInt("book_id"), rs.getString("title"),
                    rs.getString("author"), rs.getString("isbn"), rs.getString("category"),
                    rs.getInt("quantity"), rs.getInt("available")));
            }
            booksTable.setItems(results);
        } catch (SQLException e) {
            showAlert("Error", "Search failed");
        }
    }
    
    private void addBook() {
        if (tfBookTitle.getText().isEmpty() || tfBookAuthor.getText().isEmpty() ||
            cbBookCategory.getValue() == null || tfBookQuantity.getText().isEmpty()) {
            showAlert("Validation", "Please fill all required fields (*)");
            return;
        }
        
        String query = "INSERT INTO Books (title, author, isbn, category, publication_year, quantity, available) VALUES (?,?,?,?,?,?,?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            int qty = Integer.parseInt(tfBookQuantity.getText());
            pstmt.setString(1, tfBookTitle.getText());
            pstmt.setString(2, tfBookAuthor.getText());
            pstmt.setString(3, tfBookISBN.getText().isEmpty() ? null : tfBookISBN.getText());
            pstmt.setString(4, cbBookCategory.getValue());
            pstmt.setInt(5, tfBookYear.getText().isEmpty() ? 0 : Integer.parseInt(tfBookYear.getText()));
            pstmt.setInt(6, qty);
            pstmt.setInt(7, qty);
            pstmt.executeUpdate();
            
            showAlert("Success", "Book added!");
            loadBooks();
            clearBookForm();
        } catch (Exception e) {
            showAlert("Error", "Failed to add book: " + e.getMessage());
        }
    }
    
    private void updateBook() {
        if (tfBookId.getText().isEmpty()) {
            showAlert("Warning", "Please select a book");
            return;
        }
        
        String query = "UPDATE Books SET title=?, author=?, isbn=?, category=?, publication_year=?, quantity=?, available=? WHERE book_id=?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            int qty = Integer.parseInt(tfBookQuantity.getText());
            pstmt.setString(1, tfBookTitle.getText());
            pstmt.setString(2, tfBookAuthor.getText());
            pstmt.setString(3, tfBookISBN.getText().isEmpty() ? null : tfBookISBN.getText());
            pstmt.setString(4, cbBookCategory.getValue());
            pstmt.setInt(5, tfBookYear.getText().isEmpty() ? 0 : Integer.parseInt(tfBookYear.getText()));
            pstmt.setInt(6, qty);
            pstmt.setInt(7, qty);
            pstmt.setInt(8, Integer.parseInt(tfBookId.getText()));
            pstmt.executeUpdate();
            
            showAlert("Success", "Book updated!");
            loadBooks();
            clearBookForm();
        } catch (Exception e) {
            showAlert("Error", "Update failed: " + e.getMessage());
        }
    }
    
    private void deleteBook() {
        if (tfBookId.getText().isEmpty()) {
            showAlert("Warning", "Please select a book");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Delete this book?");
        if (confirm.showAndWait().get() == ButtonType.OK) {
            String query = "DELETE FROM Books WHERE book_id=?";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, Integer.parseInt(tfBookId.getText()));
                pstmt.executeUpdate();
                showAlert("Success", "Book deleted!");
                loadBooks();
                clearBookForm();
            } catch (SQLException e) {
                showAlert("Error", "Delete failed: " + e.getMessage());
            }
        }
    }
    
    private void populateBookForm(Book b) {
        tfBookId.setText(String.valueOf(b.getId()));
        tfBookTitle.setText(b.getTitle());
        tfBookAuthor.setText(b.getAuthor());
        tfBookISBN.setText(b.getIsbn());
        cbBookCategory.setValue(b.getCategory());
        tfBookYear.setText("");
        tfBookQuantity.setText(String.valueOf(b.getQuantity()));
    }
    
    private void clearBookForm() {
        tfBookId.clear(); tfBookTitle.clear(); tfBookAuthor.clear();
        tfBookISBN.clear(); cbBookCategory.setValue(null);
        tfBookYear.clear(); tfBookQuantity.clear();
        booksTable.getSelectionModel().clearSelection();
    }
    
    // ==================== STUDENTS TAB ====================
    
    private VBox createStudentsTab() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        
        HBox searchBox = new HBox(10);
        tfStudentSearch = new TextField();
        tfStudentSearch.setPromptText("Search by name or email...");
        Button btnSearch = new Button("Search");
        Button btnRefresh = new Button("Refresh");
        btnSearch.setOnAction(e -> searchStudents());
        btnRefresh.setOnAction(e -> loadStudents());
        searchBox.getChildren().addAll(new Label("Search:"), tfStudentSearch, btnSearch, btnRefresh);
        
        setupStudentsTable();
        
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        
        tfStudentId = new TextField(); tfStudentId.setEditable(false);
        tfStudentName = new TextField();
        tfStudentEmail = new TextField();
        tfStudentPhone = new TextField();
        tfStudentDept = new TextField();
        cbStudentStatus = new ComboBox<>();
        cbStudentStatus.getItems().addAll("Active", "Suspended", "Graduated");
        cbStudentStatus.setValue("Active");
        
        Button btnAdd = new Button("Add");
        Button btnUpdate = new Button("Update");
        Button btnDelete = new Button("Delete");
        Button btnClear = new Button("Clear");
        
        btnAdd.setOnAction(e -> addStudent());
        btnUpdate.setOnAction(e -> updateStudent());
        btnDelete.setOnAction(e -> deleteStudent());
        btnClear.setOnAction(e -> clearStudentForm());
        
        form.add(new Label("ID:"), 0, 0); form.add(tfStudentId, 1, 0);
        form.add(new Label("Name:*"), 0, 1); form.add(tfStudentName, 1, 1);
        form.add(new Label("Email:*"), 0, 2); form.add(tfStudentEmail, 1, 2);
        form.add(new Label("Phone:"), 2, 0); form.add(tfStudentPhone, 3, 0);
        form.add(new Label("Department:"), 2, 1); form.add(tfStudentDept, 3, 1);
        form.add(new Label("Status:"), 2, 2); form.add(cbStudentStatus, 3, 2);
        form.add(btnAdd, 0, 3); form.add(btnUpdate, 1, 3);
        form.add(btnDelete, 2, 3); form.add(btnClear, 3, 3);
        
        vbox.getChildren().addAll(searchBox, studentsTable, form);
        return vbox;
    }
    
    private void setupStudentsTable() {
        TableColumn<Student, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Student, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Student, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        TableColumn<Student, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        TableColumn<Student, String> colDept = new TableColumn<>("Department");
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        TableColumn<Student, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        studentsTable.getColumns().addAll(colId, colName, colEmail, colPhone, colDept, colStatus);
        studentsTable.setPrefHeight(300);
        
        studentsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) populateStudentForm(val);
        });
    }
    
    private void loadStudents() {
        ObservableList<Student> list = FXCollections.observableArrayList();
        String query = "SELECT * FROM Students";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                list.add(new Student(
                    rs.getInt("student_id"), rs.getString("name"),
                    rs.getString("email"), rs.getString("phone"),
                    rs.getString("department"), rs.getString("status")
                ));
            }
            studentsTable.setItems(list);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load students");
        }
    }
    
    private void searchStudents() {
        String keyword = tfStudentSearch.getText().trim();
        if (keyword.isEmpty()) { loadStudents(); return; }
        
        ObservableList<Student> results = FXCollections.observableArrayList();
        String query = "SELECT * FROM Students WHERE name LIKE ? OR email LIKE ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            String pattern = "%" + keyword + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                results.add(new Student(rs.getInt("student_id"), rs.getString("name"),
                    rs.getString("email"), rs.getString("phone"),
                    rs.getString("department"), rs.getString("status")));
            }
            studentsTable.setItems(results);
        } catch (SQLException e) {
            showAlert("Error", "Search failed");
        }
    }
    
    private void addStudent() {
        if (tfStudentName.getText().isEmpty() || tfStudentEmail.getText().isEmpty()) {
            showAlert("Validation", "Name and Email are required");
            return;
        }
        
        String query = "INSERT INTO Students (name, email, phone, department, status) VALUES (?,?,?,?,?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, tfStudentName.getText());
            pstmt.setString(2, tfStudentEmail.getText());
            pstmt.setString(3, tfStudentPhone.getText().isEmpty() ? null : tfStudentPhone.getText());
            pstmt.setString(4, tfStudentDept.getText().isEmpty() ? null : tfStudentDept.getText());
            pstmt.setString(5, cbStudentStatus.getValue());
            pstmt.executeUpdate();
            
            showAlert("Success", "Student added!");
            loadStudents();
            clearStudentForm();
        } catch (SQLException e) {
            showAlert("Error", e.getMessage().contains("Duplicate") ? "Email already exists" : "Add failed");
        }
    }
    
    private void updateStudent() {
        if (tfStudentId.getText().isEmpty()) {
            showAlert("Warning", "Please select a student");
            return;
        }
        
        String query = "UPDATE Students SET name=?, email=?, phone=?, department=?, status=? WHERE student_id=?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, tfStudentName.getText());
            pstmt.setString(2, tfStudentEmail.getText());
            pstmt.setString(3, tfStudentPhone.getText().isEmpty() ? null : tfStudentPhone.getText());
            pstmt.setString(4, tfStudentDept.getText().isEmpty() ? null : tfStudentDept.getText());
            pstmt.setString(5, cbStudentStatus.getValue());
            pstmt.setInt(6, Integer.parseInt(tfStudentId.getText()));
            pstmt.executeUpdate();
            
            showAlert("Success", "Student updated!");
            loadStudents();
            clearStudentForm();
        } catch (SQLException e) {
            showAlert("Error", "Update failed");
        }
    }
    
    private void deleteStudent() {
        if (tfStudentId.getText().isEmpty()) {
            showAlert("Warning", "Please select a student");
            return;
        }
        
        // Check for active borrows
        String check = "SELECT COUNT(*) FROM Transactions WHERE student_id=? AND status='Borrowed'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setInt(1, Integer.parseInt(tfStudentId.getText()));
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                showAlert("Cannot Delete", "Student has active borrowed books");
                return;
            }
        } catch (SQLException e) { }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Delete this student?");
        if (confirm.showAndWait().get() == ButtonType.OK) {
            String query = "DELETE FROM Students WHERE student_id=?";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, Integer.parseInt(tfStudentId.getText()));
                pstmt.executeUpdate();
                showAlert("Success", "Student deleted!");
                loadStudents();
                clearStudentForm();
            } catch (SQLException e) {
                showAlert("Error", "Delete failed");
            }
        }
    }
    
    private void populateStudentForm(Student s) {
        tfStudentId.setText(String.valueOf(s.getId()));
        tfStudentName.setText(s.getName());
        tfStudentEmail.setText(s.getEmail());
        tfStudentPhone.setText(s.getPhone() != null ? s.getPhone() : "");
        tfStudentDept.setText(s.getDepartment() != null ? s.getDepartment() : "");
        cbStudentStatus.setValue(s.getStatus());
    }
    
    private void clearStudentForm() {
        tfStudentId.clear(); tfStudentName.clear(); tfStudentEmail.clear();
        tfStudentPhone.clear(); tfStudentDept.clear();
        cbStudentStatus.setValue("Active");
        studentsTable.getSelectionModel().clearSelection();
    }
    
    // ==================== TRANSACTIONS TAB ====================
    
    private VBox createTransactionsTab() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        
        // Borrow section
        TitledPane borrowPane = new TitledPane("Borrow Book", createBorrowSection());
        TitledPane returnPane = new TitledPane("Return Book", createReturnSection());
        
        // Transaction history table
        setupTransactionsTable();
        
        borrowPane.setExpanded(true);
        returnPane.setExpanded(false);
        
        vbox.getChildren().addAll(borrowPane, returnPane, new Label("Transaction History"), transactionsTable);
        return vbox;
    }
    
    private GridPane createBorrowSection() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        tfTransBookSearch = new TextField();
        tfTransBookSearch.setPromptText("Search book by title or ID");
        tfTransStudentSearch = new TextField();
        tfTransStudentSearch.setPromptText("Search student by name or ID");
        
        Button btnSearchBook = new Button("Search Book");
        Button btnSearchStudent = new Button("Search Student");
        Button btnBorrow = new Button("Borrow Book");
        btnBorrow.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        
        Label lblBook = new Label("Selected Book: ");
        Label lblBookInfo = new Label("-");
        Label lblStudent = new Label("Selected Student: ");
        Label lblStudentInfo = new Label("-");
        
        dpBorrowDate = new DatePicker(LocalDate.now());
        dpDueDate = new DatePicker(LocalDate.now().plusDays(14));
        
        btnSearchBook.setOnAction(e -> {
            String id = tfTransBookSearch.getText().trim();
            if (id.matches("\\d+")) {
                Book b = findBookById(Integer.parseInt(id));
                if (b != null && b.getAvailable() > 0) {
                    lblBookInfo.setText(b.getTitle() + " (Available: " + b.getAvailable() + ")");
                } else {
                    lblBookInfo.setText("Book not found or unavailable");
                }
            } else {
                searchBookByTitle(tfTransBookSearch.getText(), lblBookInfo);
            }
        });
        
        btnSearchStudent.setOnAction(e -> {
            String id = tfTransStudentSearch.getText().trim();
            if (id.matches("\\d+")) {
                Student s = findStudentById(Integer.parseInt(id));
                if (s != null) {
                    lblStudentInfo.setText(s.getName() + " (" + s.getStatus() + ")");
                } else {
                    lblStudentInfo.setText("Student not found");
                }
            } else {
                searchStudentByName(tfTransStudentSearch.getText(), lblStudentInfo);
            }
        });
        
        btnBorrow.setOnAction(e -> {
            String bookInfo = lblBookInfo.getText();
            String studentInfo = lblStudentInfo.getText();
            if (bookInfo.contains("Available") && !studentInfo.contains("not found") && !studentInfo.equals("-")) {
                borrowBook(bookInfo, studentInfo);
                lblBookInfo.setText("-");
                lblStudentInfo.setText("-");
                tfTransBookSearch.clear();
                tfTransStudentSearch.clear();
            } else {
                showAlert("Error", "Please select a valid book and student");
            }
        });
        
        grid.add(new Label("Book ID/Title:"), 0, 0);
        grid.add(tfTransBookSearch, 1, 0);
        grid.add(btnSearchBook, 2, 0);
        grid.add(lblBook, 0, 1);
        grid.add(lblBookInfo, 1, 1, 2, 1);
        
        grid.add(new Label("Student ID/Name:"), 0, 2);
        grid.add(tfTransStudentSearch, 1, 2);
        grid.add(btnSearchStudent, 2, 2);
        grid.add(lblStudent, 0, 3);
        grid.add(lblStudentInfo, 1, 3, 2, 1);
        
        grid.add(new Label("Borrow Date:"), 0, 4);
        grid.add(dpBorrowDate, 1, 4);
        grid.add(new Label("Due Date:"), 0, 5);
        grid.add(dpDueDate, 1, 5);
        grid.add(btnBorrow, 1, 6);
        
        return grid;
    }
    
    private GridPane createReturnSection() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        TextField tfReturnId = new TextField();
        tfReturnId.setPromptText("Enter transaction ID");
        Label lblReturnInfo = new Label("");
        Button btnReturn = new Button("Return Book");
        btnReturn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        
        btnReturn.setOnAction(e -> {
            if (tfReturnId.getText().isEmpty()) {
                showAlert("Warning", "Enter transaction ID");
                return;
            }
            returnBook(Integer.parseInt(tfReturnId.getText()), lblReturnInfo);
            tfReturnId.clear();
        });
        
        grid.add(new Label("Transaction ID:"), 0, 0);
        grid.add(tfReturnId, 1, 0);
        grid.add(btnReturn, 2, 0);
        grid.add(lblReturnInfo, 0, 1, 3, 1);
        
        return grid;
    }
    
    private void setupTransactionsTable() {
        TableColumn<Transaction, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Transaction, String> colBook = new TableColumn<>("Book");
        colBook.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        TableColumn<Transaction, String> colStudent = new TableColumn<>("Student");
        colStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        TableColumn<Transaction, LocalDate> colBorrow = new TableColumn<>("Borrow Date");
        colBorrow.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        TableColumn<Transaction, LocalDate> colDue = new TableColumn<>("Due Date");
        colDue.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        TableColumn<Transaction, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        transactionsTable.getColumns().addAll(colId, colBook, colStudent, colBorrow, colDue, colStatus);
        transactionsTable.setPrefHeight(250);
    }
    
    private void loadTransactions() {
        ObservableList<Transaction> list = FXCollections.observableArrayList();
        String query = "SELECT t.*, b.title as book_title, s.name as student_name " +
                       "FROM Transactions t JOIN Books b ON t.book_id = b.book_id " +
                       "JOIN Students s ON t.student_id = s.student_id ORDER BY t.transaction_id DESC";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                list.add(new Transaction(
                    rs.getInt("transaction_id"),
                    rs.getString("book_title"),
                    rs.getString("student_name"),
                    rs.getDate("borrow_date").toLocalDate(),
                    rs.getDate("due_date").toLocalDate(),
                    rs.getString("status")
                ));
            }
            transactionsTable.setItems(list);
        } catch (SQLException e) {
            // Table might be empty initially
        }
    }
    
    private void borrowBook(String bookInfo, String studentInfo) {
        // Extract book ID from selection
        int bookId = 0, studentId = 0;
        
        // Find book ID from title
        String bookTitle = bookInfo.split(" \\(")[0];
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT book_id FROM Books WHERE title=? AND available>0")) {
            ps.setString(1, bookTitle);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) bookId = rs.getInt(1);
        } catch (SQLException e) { }
        
        // Find student ID from name
        String studentName = studentInfo.split(" \\(")[0];
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT student_id FROM Students WHERE name=? AND status='Active'")) {
            ps.setString(1, studentName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) studentId = rs.getInt(1);
        } catch (SQLException e) { }
        
        if (bookId == 0 || studentId == 0) {
            showAlert("Error", "Invalid book or student");
            return;
        }
        
        // Create transaction
        String insert = "INSERT INTO Transactions (book_id, student_id, borrow_date, due_date, status) VALUES (?,?,?,?,?)";
        String updateBook = "UPDATE Books SET available = available - 1 WHERE book_id=?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insert);
             PreparedStatement pstmt2 = conn.prepareStatement(updateBook)) {
            
            conn.setAutoCommit(false);
            
            pstmt.setInt(1, bookId);
            pstmt.setInt(2, studentId);
            pstmt.setDate(3, Date.valueOf(dpBorrowDate.getValue()));
            pstmt.setDate(4, Date.valueOf(dpDueDate.getValue()));
            pstmt.setString(5, "Borrowed");
            pstmt.executeUpdate();
            
            pstmt2.setInt(1, bookId);
            pstmt2.executeUpdate();
            
            conn.commit();
            showAlert("Success", "Book borrowed successfully!");
            loadBooks();
            loadTransactions();
            
        } catch (SQLException e) {
            showAlert("Error", "Borrow failed: " + e.getMessage());
        }
    }
    
    private void returnBook(int transId, Label info) {
        String select = "SELECT book_id FROM Transactions WHERE transaction_id=? AND status='Borrowed'";
        String update = "UPDATE Transactions SET return_date=?, status='Returned' WHERE transaction_id=?";
        String updateBook = "UPDATE Books SET available = available + 1 WHERE book_id=?";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(select)) {
            
            ps.setInt(1, transId);
            ResultSet rs = ps.executeQuery();
            
            if (!rs.next()) {
                info.setText("Transaction not found or already returned");
                return;
            }
            
            int bookId = rs.getInt("book_id");
            
            conn.setAutoCommit(false);
            
            PreparedStatement pstmt = conn.prepareStatement(update);
            pstmt.setDate(1, Date.valueOf(LocalDate.now()));
            pstmt.setInt(2, transId);
            pstmt.executeUpdate();
            
            PreparedStatement pstmt2 = conn.prepareStatement(updateBook);
            pstmt2.setInt(1, bookId);
            pstmt2.executeUpdate();
            
            conn.commit();
            info.setText("Book returned successfully!");
            loadBooks();
            loadTransactions();
            
        } catch (SQLException e) {
            info.setText("Return failed: " + e.getMessage());
        }
    }
    
    private Book findBookById(int id) {
        String query = "SELECT * FROM Books WHERE book_id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Book(rs.getInt("book_id"), rs.getString("title"),
                    rs.getString("author"), rs.getString("isbn"), rs.getString("category"),
                    rs.getInt("quantity"), rs.getInt("available"));
            }
        } catch (SQLException e) { }
        return null;
    }
    
    private Student findStudentById(int id) {
        String query = "SELECT * FROM Students WHERE student_id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Student(rs.getInt("student_id"), rs.getString("name"),
                    rs.getString("email"), rs.getString("phone"),
                    rs.getString("department"), rs.getString("status"));
            }
        } catch (SQLException e) { }
        return null;
    }
    
    private void searchBookByTitle(String title, Label label) {
        String query = "SELECT * FROM Books WHERE title LIKE ? AND available>0 LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, "%" + title + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                label.setText(rs.getString("title") + " (Available: " + rs.getInt("available") + ")");
            } else {
                label.setText("Book not found or unavailable");
            }
        } catch (SQLException e) {
            label.setText("Search failed");
        }
    }
    
    private void searchStudentByName(String name, Label label) {
        String query = "SELECT * FROM Students WHERE name LIKE ? AND status='Active' LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, "%" + name + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                label.setText(rs.getString("name") + " (" + rs.getString("status") + ")");
            } else {
                label.setText("Student not found or inactive");
            }
        } catch (SQLException e) {
            label.setText("Search failed");
        }
    }
    
    private Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (Exception e) {
            showAlert("Database Error", "Cannot connect to database. Make sure MySQL is running.");
            return null;
        }
    }
    
    private void closeConnection() {
        try {
            Connection conn = getConnection();
            if (conn != null) conn.close();
        } catch (SQLException e) { }
    }
    
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
    // ==================== MODEL CLASSES ====================
    
    public static class Book {
        private int id, quantity, available;
        private String title, author, isbn, category;
        
        public Book(int id, String title, String author, String isbn, String category, int quantity, int available) {
            this.id = id; this.title = title; this.author = author; this.isbn = isbn;
            this.category = category; this.quantity = quantity; this.available = available;
        }
        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getIsbn() { return isbn; }
        public String getCategory() { return category; }
        public int getQuantity() { return quantity; }
        public int getAvailable() { return available; }
    }
    
    public static class Student {
        private int id;
        private String name, email, phone, department, status;
        
        public Student(int id, String name, String email, String phone, String department, String status) {
            this.id = id; this.name = name; this.email = email;
            this.phone = phone; this.department = department; this.status = status;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getDepartment() { return department; }
        public String getStatus() { return status; }
    }
    
    public static class Transaction {
        private int id;
        private String bookTitle, studentName, status;
        private LocalDate borrowDate, dueDate;
        
        public Transaction(int id, String bookTitle, String studentName, LocalDate borrowDate, LocalDate dueDate, String status) {
            this.id = id; this.bookTitle = bookTitle; this.studentName = studentName;
            this.borrowDate = borrowDate; this.dueDate = dueDate; this.status = status;
        }
        public int getId() { return id; }
        public String getBookTitle() { return bookTitle; }
        public String getStudentName() { return studentName; }
        public LocalDate getBorrowDate() { return borrowDate; }
        public LocalDate getDueDate() { return dueDate; }
        public String getStatus() { return status; }
    }
}