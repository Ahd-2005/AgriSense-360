<div align="center">

<img src="https://readme-typing-svg.herokuapp.com?font=Fira+Code&size=28&pause=1000&color=4CAF50&center=true&vCenter=true&width=600&lines=🌿+AgriSense+360+Desktop;Farm+Management+Made+Smart" alt="Typing SVG" />

# AgriSense 360 — Desktop Application

### Complete Farm Management System | JavaFX • MVC • MySQL

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17-blue?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://mysql.com/)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)

---

> 🚜 **AgriSense 360** is a full-featured desktop application for complete farm management — handling crops, animals, equipment, workers, stock, and land parcels from a single, modern JavaFX interface.

</div>

---

## ✨ Features

| Module | Description |
|--------|-------------|
| 🌾 **Culture Management** | Track crops, planting cycles, and harvest schedules |
| 🐄 **Animal Management** | Monitor livestock — health records, count, category |
| 🚜 **Equipment Management** | Manage machinery, maintenance logs, availability |
| 📦 **Stock Management** | Inventory of seeds, fertilizers, tools, and resources |
| 👷 **Worker Management** | Assign tasks, track attendance, manage farm staff |
| 🗺️ **Parcel Management** | Map and monitor land parcels and zone allocation |
| 📊 **Dashboard** | Real-time overview of farm operations and KPIs |

---

## 🏗️ Architecture — MVC Pattern

This project strictly follows the **Model-View-Controller** design pattern:

```
src/
├── main/
│   ├── java/
│   │   └── com/agrisense/
│   │       ├── controllers/        # JavaFX Controllers (UI logic)
│   │       │   ├── DashboardController.java
│   │       │   ├── CultureController.java
│   │       │   ├── AnimalController.java
│   │       │   ├── EquipmentController.java
│   │       │   ├── StockController.java
│   │       │   └── WorkerController.java
│   │       ├── models/             # Data models & entities
│   │       │   ├── Culture.java
│   │       │   ├── Animal.java
│   │       │   ├── Equipment.java
│   │       │   ├── Stock.java
│   │       │   └── Worker.java
│   │       ├── services/           # Business logic layer
│   │       └── utils/              # DB connection, helpers
│   └── resources/
│       ├── fxml/                   # JavaFX FXML Views
│       │   ├── dashboard.fxml
│       │   ├── culture.fxml
│       │   └── ...
│       └── css/                    # Stylesheets
└── test/
```

---

## 🛠️ Tech Stack

- **Language:** Java 17+
- **UI Framework:** JavaFX 17 + FXML
- **Styling:** CSS (JavaFX stylesheet)
- **Database:** MySQL 8.0 via JDBC
- **Build Tool:** Maven
- **IDE:** IntelliJ IDEA / Eclipse

---

## 🚀 Getting Started

### Prerequisites

- Java JDK 17 or higher
- MySQL Server 8.0+
- Maven 3.8+

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/ahmed-habouba/AgriSense360-javaFX.git
cd AgriSense360-javaFX

# 2. Set up the database
mysql -u root -p < MySQLqueries.txt

# 3. Configure DB connection
# Edit src/main/java/com/agrisense/utils/DBConnection.java
# and set your MySQL credentials

# 4. Build and run
mvn clean javafx:run
```

---

## 🗄️ Database Setup

The `MySQLqueries.txt` file contains all SQL scripts to:
- Create the `agrisense360` database
- Create all required tables (cultures, animals, equipment, stock, workers, parcels)
- Insert sample data for testing

---

## 👨‍💻 Developer

**Ahmed Habouba** — Software Engineering Student @ ESPRIT, Tunisia

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=flat&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/habbouba-ahmed-3a6840408/)
[![GitHub](https://img.shields.io/badge/GitHub-ahmed--habouba-181717?style=flat&logo=github)](https://github.com/ahmed-habouba)
[![Email](https://img.shields.io/badge/Email-ahmedhabouba.com%40gmail.com-D14836?style=flat&logo=gmail)](mailto:ahmedhabouba.com@gmail.com)

---

<div align="center">

*Built with ❤️ at ESPRIT School of Engineering, Tunisia 🇹🇳*

</div>
