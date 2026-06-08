<div align="center">

<img src="https://readme-typing-svg.herokuapp.com?font=Fira+Code&size=28&pause=1000&color=27AE60&center=true&vCenter=true&width=600&lines=🌿+AgriSense+360+Web;Smart+Farm+Management+Platform" alt="Typing SVG" />

# AgriSense 360 — Web Platform

### Full-Stack Farm Management System | Symfony • Twig • MySQL

[![PHP](https://img.shields.io/badge/PHP-8.0+-777BB4?style=for-the-badge&logo=php&logoColor=white)](https://php.net/)
[![Symfony](https://img.shields.io/badge/Symfony-6.x-000000?style=for-the-badge&logo=symfony&logoColor=white)](https://symfony.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://mysql.com/)
[![Twig](https://img.shields.io/badge/Twig-Templates-339933?style=for-the-badge&logo=symfony&logoColor=white)](https://twig.symfony.com/)
[![JavaScript](https://img.shields.io/badge/JavaScript-ES6-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)](https://developer.mozilla.org/en-US/docs/Web/JavaScript)

---

> 🌐 **AgriSense 360 Web** is a responsive, full-stack web application for total farm management — built with Symfony 6. Manage crops, livestock, equipment, workers, and inventory from one centralized dashboard.

</div>

---

## ✨ Features

| Module | Route | Description |
|--------|-------|-------------|
| 🏠 **Dashboard** | `/` | Farm overview with KPIs and quick navigation |
| 🌾 **Culture** | `/management/culture` | Crop & planting management |
| 🐄 **Animals** | `/management/animals` | Livestock tracking and records |
| 🚜 **Equipment** | `/management/equipments` | Machinery, maintenance, status |
| 📦 **Stock** | `/management/stock` | Inventory and resource tracking |
| 👷 **Workers** | `/management/workers` | Staff management and assignments |
| 👤 **Users** | `/management/users` | User roles and access control |

---

## 🏗️ Architecture — Symfony MVC

```
app/
├── src/
│   ├── Controller/             # HTTP controllers — one per module
│   │   ├── HomeController.php
│   │   ├── AnimalController.php
│   │   ├── EquipmentController.php
│   │   ├── StockController.php
│   │   ├── CultureController.php
│   │   ├── WorkerController.php
│   │   └── UserController.php
│   ├── Entity/                 # Doctrine entities (DB models)
│   │   ├── Equipment.php
│   │   ├── Maintenance.php
│   │   └── ...
│   ├── Repository/             # Data access layer
│   ├── Form/                   # Symfony form types & validation
│   └── ...
├── templates/                  # Twig view layer
│   ├── home/                   # Dashboard & landing templates
│   ├── management/             # Shared management layouts
│   ├── equipment/              # Equipment CRUD views
│   └── base.html.twig          # Base layout with sidebar
├── public/
│   ├── index.php               # Front controller
│   └── assets/
│       ├── styles/             # CSS per module/feature
│       └── images/             # Static assets
├── config/                     # Symfony config (routing, services)
└── migrations/                 # Doctrine migrations
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | PHP 8.0+, Symfony 6 |
| **Templating** | Twig |
| **Database** | MySQL via Doctrine |
| **Frontend** | HTML5, CSS3, Vanilla JavaScript |
| **Forms** | Symfony Form Component |
| **Server** | Symfony Local Server / Apache |
| **Deps** | Composer |

---

## 🚀 Getting Started

### Prerequisites

- PHP 8.0+
- Composer
- MySQL 8.0+
- Symfony CLI *(recommended)*

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/Ahd-2005/AgriSense-360.git
cd AgriSense-360/app

# 2. Install dependencies
composer install

# 3. Configure environment
cp .env .env.local
# Edit .env.local — set your DATABASE_URL:
# DATABASE_URL="mysql://root:password@127.0.0.1:3306/agrisense360"

# 4. Create database and run migrations
php bin/console doctrine:database:create
php bin/console doctrine:migrations:migrate

# 5. Start the server
symfony server:start
# OR
php -S 127.0.0.1:8000 -t public/
```

Open **http://127.0.0.1:8000** in your browser.

---

## 🎨 UI & Design

- **Responsive layout** — works on desktop, tablet, and mobile
- **Sidebar navigation** — collapsible, with smooth CSS transitions
- **Dashboard cards** — visual overview of key farm metrics
- **Modular CSS** — each page/feature has its own stylesheet under `public/assets/styles/`

---

## 👨‍💻 Developer

**Ahmed Habouba** — Software Engineering Student @ ESPRIT, Tunisia

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=flat&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/habbouba-ahmed-3a6840408/)
[![GitHub](https://img.shields.io/badge/GitHub-Ahd--2005-181717?style=flat&logo=github)](https://github.com/Ahd-2005)
[![Email](https://img.shields.io/badge/Email-ahmedhabouba.com%40gmail.com-D14836?style=flat&logo=gmail)](mailto:ahmedhabouba.com@gmail.com)

---

<div align="center">

*Built with ❤️ at ESPRIT School of Engineering, Tunisia 🇹🇳*

</div>
