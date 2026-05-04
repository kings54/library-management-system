# 📚 Library Management System

A comprehensive desktop application for managing library books, students, and borrowing transactions. Built with JavaFX and MySQL, this system streamlines library operations including book inventory management, student registration, and book borrowing/return processes.

## 📋 Features

### Book Management
- Add, update, and delete book records
- Search books by title, author, or ISBN
- Track total quantity and available copies
- Categorize books (Fiction, Non-Fiction, Computer Science, Business)
- Real-time availability updates when books are borrowed/returned

### Student Management
- Register and manage student records
- Track student status (Active/Suspended/Graduated)
- Store contact information and department details
- Search students by name or email
- Prevent deletion of students with active borrowed books

### Transaction Management
- **Borrow Books** – Issue books to active students with automatic due date calculation (14-day default)
- **Return Books** – Process returns by transaction ID, automatically updating book availability
- **Transaction History** – View complete borrowing/return history
- **Prevents borrowing** when no copies are available or student is inactive
- **Fine tracking** placeholder ready for implementation

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| **Frontend** | JavaFX 17+ |
| **Database** | MySQL 8.0 |
| **JDBC Driver** | MySQL Connector/J |
| **Java Version** | Java 11 or higher |

## 📦 Prerequisites

Before running this application, ensure you have:

1. **Java 11 or higher** installed
   ```bash
   java --version
