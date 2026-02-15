# SmartFarm

JavaFX desktop application for managing work assignments (affectations) and performance evaluations, with MySQL persistence.  
**Structure follows the AgriSense-360 template:** app package, `entity`, `controllers`, `services`, `utils`, FXML under `resources/fxml`, and Java modules.

---

## Template-aligned project structure

```
smartfarm/
├── .mvn/wrapper/                    # Maven wrapper
├── src/main/
│   ├── java/
│   │   ├── module-info.java         # Module descriptor (opens/exports for JavaFX)
│   │   ├── org/example/smartfarm/   # Application entry (like com.example.agrisens360)
│   │   │   ├── Main.java            # JavaFX Application
│   │   │   └── MainController.java  # Main menu controller
│   │   ├── controllers/             # JavaFX FXML controllers
│   │   ├── entity/                  # Domain entities (template uses entity, not models)
│   │   ├── services/                # JDBC CRUD layer
│   │   └── utils/                  # MyConnection, DbInit
│   └── resources/
│       ├── fxml/                    # All FXML views
│       ├── css/                     # Stylesheets
│       └── images/                  # Image assets
├── mvnw.cmd
├── pom.xml
└── README.md
```

## Main components (template style)

| Package / folder      | Role |
|-----------------------|------|
| **org.example.smartfarm** | Application entry and main window; mirrors template’s `com.example.agrisens360`. |
| **controllers**       | JavaFX controllers for affectations and evaluations. |
| **entity**            | Domain objects (AffectationTravail, EvaluationPerformance). |
| **services**          | JDBC services; no UI. |
| **utils**             | DB connection singleton and table init. |
| **resources/fxml**    | FXML files; loaded as `/fxml/...`. |

## Build and run

- **IDE:** Run `org.example.smartfarm.Main` with JavaFX on the module path (e.g. `--module-path` and `--add-modules javafx.controls,javafx.fxml`), or use Maven.
- **Maven:** `mvn javafx:run` or `mvnw.cmd javafx:run`.
- **Database:** MySQL database `smartfarm`; connection in `utils.MyConnection`; tables created on startup via `utils.DbInit`.

## Conventions (from AgriSense-360 template)

- **App package:** `org.example.smartfarm` (template: `com.example.agrisens360`).
- **Domain package:** `entity` (template uses `entity`, not `models`).
- **Root-level packages:** `controllers`, `entity`, `services`, `utils`.
- **Resources:** FXML in `src/main/resources/fxml/`; paths like `/fxml/main_view.fxml`.
- **Modules:** `module-info.java` declares `requires` and `opens`/`exports` for JavaFX and DB.
