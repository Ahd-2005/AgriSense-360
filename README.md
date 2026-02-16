# AgriSense Equipment (JavaFX)

This project is a JavaFX CRUD app for managing equipment. It is designed to open and run in IntelliJ IDEA using Maven.

## Project Structure

- src/main/java/tn/esprit/pidev: application code
- src/main/resources/styles: CSS theme
- src/main/resources/sql: database setup script
- src/main/resources/config.properties: database connection settings

## Setup

1) Create the database tables and sequences:

- Run the script at `src/main/resources/sql/create_equipment_tables.sql` in Oracle.

2) Configure the database connection:

- Update `src/main/resources/config.properties` with your Oracle credentials.
- Do not commit real credentials to GitHub.

3) Run the app:

- Open the project root in IntelliJ.
- Use the Maven run configuration for `javafx:run`.
- Or run the `tn.esprit.pidev.App` class directly.

## Notes

- Java version: 17+
- JavaFX version: 21.0.4
