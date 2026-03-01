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


---

External Agriculture Advice (Agromonitoring API)

This app can fetch real, location-based agriculture advice using the public Agromonitoring API. You only need an external API key — no OpenAI or other LLM keys are required.

Where to get the API key
- Create a free account at: https://agromonitoring.com/
- After logging in, go to your dashboard → API keys (sometimes called "appid"). Copy the key string.

How to set the API key (choose ONE of the three options)
1) As an environment variable (recommended)
   - Variable name: AGROMONITORING_API_KEY
   - Windows (PowerShell, applies to current session):
     $Env:AGROMONITORING_API_KEY = "YOUR_API_KEY_HERE"
   - Windows (set permanently for your user):
     setx AGROMONITORING_API_KEY "YOUR_API_KEY_HERE"
   - macOS/Linux (bash/zsh):
     export AGROMONITORING_API_KEY="YOUR_API_KEY_HERE"

2) As a local user properties file
   - Create a file: %USERPROFILE%\.agrisense\api-keys.properties (Windows)
     On macOS/Linux: ~/.agrisense/api-keys.properties
   - Put this line inside the file:
     agromonitoring.apiKey=YOUR_API_KEY_HERE

3) In the project config file (this repo already .gitignores it)
   - Edit src/main/resources/config.properties and add:
     agromonitoring.apiKey=YOUR_API_KEY_HERE
   - This key will be picked up by the app if the environment variable is not set.

Set your location (optional)
- Quick override using environment variables:
  - AGRI_LAT (e.g., 36.8065)
  - AGRI_LON (e.g., 10.1815)
  Examples (PowerShell):
    $Env:AGRI_LAT = "36.8065"
    $Env:AGRI_LON = "10.1815"
- Or set defaults in src/main/resources/config.properties:
    advice.lat=36.8065
    advice.lon=10.1815
- If nothing is set, the app defaults to Tunis, Tunisia (36.8065, 10.1815).

How the advice feature works (code explained in blocks)
- File: src/main/java/services/AgromonitoringAdviceService.java

1) API key and coordinates resolution
   - API key order:
     1) Environment variable AGROMONITORING_API_KEY
     2) %USERPROFILE%\.agrisense\api-keys.properties → agromonitoring.apiKey
   - Coordinates order:
     1) AGRI_LAT / AGRI_LON environment variables
     2) advice.lat / advice.lon in config.properties
     3) Built-in default: 36.8065, 10.1815

2) External API calls (real endpoints)
   - Weather:
     https://api.agromonitoring.com/agro/1.0/weather?lat={lat}&lon={lon}&appid={API_KEY}
   - Soil:
     https://api.agromonitoring.com/agro/1.0/soil?lat={lat}&lon={lon}&appid={API_KEY}
   The service uses Java HttpClient with a 10–15s timeout and expects JSON.

3) Building the advice text
   - The service reads from weather JSON: temp (K → °C), humidity, wind speed, description, and rain (1h).
   - From soil JSON: moisture (m³/m³) and top soil temperature (t0, °C, if provided).
   - It then generates concise, actionable tips, e.g., irrigation adjustments for heat/dryness, disease risk in high humidity, wind cautions, rainfall-driven irrigation reduction, soil moisture/temperature actions.

4) Fallbacks and errors
   - If no API key is configured, the service returns a friendly message explaining the missing key.
   - If the API returns no data or invalid JSON, it safely falls back to a generic tip.

Where it appears in the UI
- The DashboardCultureController schedules a background fetch and shows the tip as a small advice card on the culture dashboard. If you don’t see a tip, verify your API key and internet connectivity.

Troubleshooting
- 401 Unauthorized: Your API key is missing or invalid. Re-check the key string and how you set it.
- 429 Too Many Requests: You exceeded the free plan limits. Wait or upgrade your plan.
- Empty advice: The API might have returned empty/invalid JSON. Try again later and confirm your coordinates.
- Corporate proxy/firewall: Ensure api.agromonitoring.com is reachable from your network.

Security notes
- Never commit real keys. Prefer environment variables. The optional user-level properties file is read from your home folder and is not part of the repository.
