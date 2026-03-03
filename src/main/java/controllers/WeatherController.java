package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

public class WeatherController implements Initializable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d");
    private JSONObject weatherData;
    private JSONArray forecastDays;
    private List<DayWeatherData> dayDataList = new ArrayList<>();

    @FXML private Label locationLabel;
    @FXML private Label updatedLabel;
    @FXML private TextField locationField;
    @FXML private Button loadLocationBtn;
    @FXML private ComboBox<String> daySelector;
    @FXML private ToggleGroup viewMode;
    @FXML private ToggleButton dayViewBtn;
    @FXML private ToggleButton weeklyViewBtn;
    @FXML private StackPane chartsContainer;
    @FXML private VBox dayViewPanel;
    @FXML private VBox weeklyViewPanel;
    @FXML private Button exportBtn;
    
    // Day view labels
    @FXML private Label dayDateLabel;
    @FXML private Label dayConditionLabel;
    @FXML private Label dayTempHighLabel;
    @FXML private Label dayTempLowLabel;
    @FXML private Label dayAvgTempLabel;
    @FXML private Label dayHumidityLabel;
    @FXML private Label dayRainChanceLabel;
    @FXML private Label daySnowChanceLabel;
    @FXML private Label dayPrecipLabel;
    @FXML private Label dayWindLabel;
    @FXML private Label dayUvIndexLabel;
    @FXML private VBox dayChartsBox;
    
    // Weekly view
    @FXML private VBox weeklyChartsBox;
    private WeatherConfig weatherConfig;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        weatherConfig = WeatherConfig.load();
        setupUi();
        loadWeather();
    }

    private void setupUi() {
        if (viewMode != null) {
            viewMode.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == dayViewBtn) {
                    showDayView();
                } else if (newVal == weeklyViewBtn) {
                    showWeeklyView();
                }
            });
            if (dayViewBtn != null) {
                dayViewBtn.setSelected(true);
            }
        }

        if (daySelector != null) {
            daySelector.setOnAction(e -> onDaySelected());
        }

        if (locationField != null) {
            locationField.setOnAction(e -> onLoadLocation());
            String defaultLocation = weatherConfig != null ? weatherConfig.location : "Tunisia";
            locationField.setText(defaultLocation);
        }

        if (loadLocationBtn != null) {
            loadLocationBtn.setOnAction(e -> onLoadLocation());
        }

        if (exportBtn != null) {
            exportBtn.setOnAction(e -> exportWeatherData());
        }
    }

    private void loadWeather() {
        if (weatherConfig == null) {
            weatherConfig = WeatherConfig.load();
        }
        if (!weatherConfig.hasApiKey()) {
            setError("Missing weather API config.");
            return;
        }

        String selectedLocation = weatherConfig.location;
        if (locationField != null && locationField.getText() != null && !locationField.getText().isBlank()) {
            selectedLocation = locationField.getText().trim();
        }

        String requestLocation = selectedLocation;
        Thread thread = new Thread(() -> fetchWeather(requestLocation));
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onLoadLocation() {
        if (locationField == null) {
            return;
        }
        String selectedLocation = locationField.getText() != null ? locationField.getText().trim() : "";
        if (selectedLocation.isEmpty()) {
            setError("Please enter a location.");
            return;
        }
        loadWeatherForLocation(selectedLocation);
    }

    private void loadWeatherForLocation(String selectedLocation) {
        if (weatherConfig == null) {
            weatherConfig = WeatherConfig.load();
        }
        if (!weatherConfig.hasApiKey()) {
            setError("Missing weather API config.");
            return;
        }

        Platform.runLater(() -> {
            updatedLabel.setText("Loading weather for " + selectedLocation + "...");
            locationLabel.setText("Weather Forecast");
        });

        Thread thread = new Thread(() -> fetchWeather(selectedLocation));
        thread.setDaemon(true);
        thread.start();
    }

    private void fetchWeather(String selectedLocation) {
        try {
            String query = URLEncoder.encode(selectedLocation, StandardCharsets.UTF_8);
            String url = "https://api.weatherapi.com/v1/forecast.json?key=" + weatherConfig.apiKey
                + "&q=" + query + "&days=" + weatherConfig.days + "&aqi=no&alerts=no";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String message = "Weather API error: " + response.statusCode();
                try {
                    JSONObject errorObj = new JSONObject(response.body()).optJSONObject("error");
                    if (errorObj != null) {
                        message = errorObj.optString("message", message);
                    }
                } catch (Exception ignored) {
                }
                setError(message);
                return;
            }
            weatherData = new JSONObject(response.body());
            processForecastData();
            updateUi();
        } catch (Exception e) {
            setError("Failed to load weather: " + e.getMessage());
        }
    }

    private void processForecastData() {
        JSONObject forecast = weatherData.getJSONObject("forecast");
        forecastDays = forecast.getJSONArray("forecastday");
        dayDataList.clear();

        for (int i = 0; i < forecastDays.length(); i++) {
            JSONObject forecastDay = forecastDays.getJSONObject(i);
            DayWeatherData data = new DayWeatherData(forecastDay);
            dayDataList.add(data);
        }
    }

    private void updateUi() {
        Platform.runLater(() -> {
            JSONObject location = weatherData.getJSONObject("location");
            String place = location.optString("name", "");
            String region = location.optString("country", "");
            locationLabel.setText("Weather for " + place + ", " + region);
            
            JSONObject current = weatherData.getJSONObject("current");
            updatedLabel.setText("Updated: " + current.optString("last_updated", "--"));

            // Populate day selector
            if (daySelector != null) {
                ObservableList<String> dates = FXCollections.observableArrayList();
                for (DayWeatherData day : dayDataList) {
                    dates.add(day.dateStr);
                }
                daySelector.setItems(dates);
                if (!dates.isEmpty()) {
                    daySelector.getSelectionModel().selectFirst();
                    onDaySelected();
                }
            }

            showDayView();
        });
    }

    private void onDaySelected() {
        if (daySelector == null || daySelector.getValue() == null) return;
        String selectedDate = daySelector.getValue();
        DayWeatherData selectedDay = dayDataList.stream()
            .filter(d -> d.dateStr.equals(selectedDate))
            .findFirst()
            .orElse(null);
        
        if (selectedDay != null) {
            updateDayViewDetails(selectedDay);
        }
    }

    private void updateDayViewDetails(DayWeatherData day) {
        if (dayDateLabel != null) dayDateLabel.setText(day.dateStr);
        if (dayConditionLabel != null) dayConditionLabel.setText(day.condition);
        if (dayTempHighLabel != null) dayTempHighLabel.setText(String.format("High: %.1f°C", day.tempMax));
        if (dayTempLowLabel != null) dayTempLowLabel.setText(String.format("Low: %.1f°C", day.tempMin));
        if (dayAvgTempLabel != null) dayAvgTempLabel.setText(String.format("Avg: %.1f°C", day.tempAvg));
        if (dayHumidityLabel != null) dayHumidityLabel.setText(String.format("Humidity: %d%%", day.humidity));
        if (dayRainChanceLabel != null) dayRainChanceLabel.setText(String.format("Rain Chance: %d%%", day.rainChance));
        if (daySnowChanceLabel != null) daySnowChanceLabel.setText(String.format("Snow Chance: %d%%", day.snowChance));
        if (dayPrecipLabel != null) dayPrecipLabel.setText(String.format("Precipitation: %.1f mm", day.precipitation));
        if (dayWindLabel != null) dayWindLabel.setText(String.format("Max Wind: %.1f kph", day.windMax));
        if (dayUvIndexLabel != null) dayUvIndexLabel.setText(String.format("UV Index: %.1f", day.uvIndex));

        // Generate and display hourly charts for the selected day
        if (dayChartsBox != null) {
            dayChartsBox.getChildren().clear();
            dayChartsBox.getChildren().addAll(
                createHourlyTemperatureChart(day),
                createHourlyHumidityChart(day),
                createHourlyPrecipitationChart(day)
            );
        }
    }

    private void showDayView() {
        if (dayViewPanel != null && weeklyViewPanel != null) {
            dayViewPanel.setStyle("-fx-visible: true;");
            weeklyViewPanel.setStyle("-fx-visible: false;");
        }
    }

    private void showWeeklyView() {
        if (dayViewPanel != null && weeklyViewPanel != null) {
            dayViewPanel.setStyle("-fx-visible: false;");
            weeklyViewPanel.setStyle("-fx-visible: true;");
        }

        if (weeklyChartsBox != null) {
            weeklyChartsBox.getChildren().clear();
            weeklyChartsBox.getChildren().addAll(
                createWeeklyTemperatureChart(),
                createWeeklyPrecipitationChart(),
                createWeeklyWindChart(),
                createWeeklyHumidityChart()
            );
        }
    }

    private VBox createHourlyTemperatureChart(DayWeatherData day) {
        LineChart<String, Number> chart = new LineChart<>(
            new CategoryAxis(), new NumberAxis()
        );
        chart.setTitle("Hourly Temperature");
        chart.setCreateSymbols(false);
        chart.setPrefHeight(300);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Temperature (°C)");

        JSONObject dayObj = forecastDays.getJSONObject(dayDataList.indexOf(day));
        JSONArray hours = dayObj.getJSONArray("hour");

        for (int i = 0; i < hours.length(); i++) {
            JSONObject hour = hours.getJSONObject(i);
            String time = hour.getString("time").substring(11, 16);
            double temp = hour.getDouble("temp_c");
            series.getData().add(new XYChart.Data<>(time, temp));
        }

        chart.getData().add(series);
        return new VBox(chart);
    }

    private VBox createHourlyHumidityChart(DayWeatherData day) {
        AreaChart<String, Number> chart = new AreaChart<>(
            new CategoryAxis(), new NumberAxis()
        );
        chart.setTitle("Hourly Humidity");
        chart.setPrefHeight(300);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Humidity (%)");

        JSONObject dayObj = forecastDays.getJSONObject(dayDataList.indexOf(day));
        JSONArray hours = dayObj.getJSONArray("hour");

        for (int i = 0; i < hours.length(); i++) {
            JSONObject hour = hours.getJSONObject(i);
            String time = hour.getString("time").substring(11, 16);
            int humidity = hour.getInt("humidity");
            series.getData().add(new XYChart.Data<>(time, humidity));
        }

        chart.getData().add(series);
        return new VBox(chart);
    }

    private VBox createHourlyPrecipitationChart(DayWeatherData day) {
        LineChart<String, Number> chart = new LineChart<>(
            new CategoryAxis(), new NumberAxis()
        );
        chart.setTitle("Hourly Precipitation Chance");
        chart.setCreateSymbols(false);
        chart.setPrefHeight(300);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Chance of Precip (%)");

        JSONObject dayObj = forecastDays.getJSONObject(dayDataList.indexOf(day));
        JSONArray hours = dayObj.getJSONArray("hour");

        for (int i = 0; i < hours.length(); i++) {
            JSONObject hour = hours.getJSONObject(i);
            String time = hour.getString("time").substring(11, 16);
            double chance = hour.getDouble("chance_of_rain");
            series.getData().add(new XYChart.Data<>(time, chance));
        }

        chart.getData().add(series);
        return new VBox(chart);
    }

    private VBox createWeeklyTemperatureChart() {
        LineChart<String, Number> chart = new LineChart<>(
            new CategoryAxis(), new NumberAxis()
        );
        chart.setTitle("Weekly Temperature Range");
        chart.setPrefHeight(350);

        XYChart.Series<String, Number> maxSeries = new XYChart.Series<>();
        maxSeries.setName("Max Temp (°C)");
        XYChart.Series<String, Number> minSeries = new XYChart.Series<>();
        minSeries.setName("Min Temp (°C)");

        for (DayWeatherData day : dayDataList) {
            maxSeries.getData().add(new XYChart.Data<>(day.dateStr, day.tempMax));
            minSeries.getData().add(new XYChart.Data<>(day.dateStr, day.tempMin));
        }

        chart.getData().addAll(maxSeries, minSeries);
        return new VBox(chart);
    }

    private VBox createWeeklyPrecipitationChart() {
        LineChart<String, Number> chart = new LineChart<>(
            new CategoryAxis(), new NumberAxis()
        );
        chart.setTitle("Weekly Precipitation & Rain Chance");
        chart.setPrefHeight(350);

        XYChart.Series<String, Number> precipSeries = new XYChart.Series<>();
        precipSeries.setName("Precipitation (mm)");
        XYChart.Series<String, Number> rainSeries = new XYChart.Series<>();
        rainSeries.setName("Rain Chance (%)");

        for (DayWeatherData day : dayDataList) {
            precipSeries.getData().add(new XYChart.Data<>(day.dateStr, day.precipitation));
            rainSeries.getData().add(new XYChart.Data<>(day.dateStr, day.rainChance));
        }

        chart.getData().addAll(precipSeries, rainSeries);
        return new VBox(chart);
    }

    private VBox createWeeklyWindChart() {
        LineChart<String, Number> chart = new LineChart<>(
            new CategoryAxis(), new NumberAxis()
        );
        chart.setTitle("Weekly Max Wind Speed");
        chart.setPrefHeight(350);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Wind Speed (kph)");

        for (DayWeatherData day : dayDataList) {
            series.getData().add(new XYChart.Data<>(day.dateStr, day.windMax));
        }

        chart.getData().add(series);
        return new VBox(chart);
    }

    private VBox createWeeklyHumidityChart() {
        AreaChart<String, Number> chart = new AreaChart<>(
            new CategoryAxis(), new NumberAxis()
        );
        chart.setTitle("Weekly Average Humidity");
        chart.setPrefHeight(350);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Humidity (%)");

        for (DayWeatherData day : dayDataList) {
            series.getData().add(new XYChart.Data<>(day.dateStr, day.humidity));
        }

        chart.getData().add(series);
        return new VBox(chart);
    }

    private void exportWeatherData() {
        if (weatherData == null) {
            setError("No weather data to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Weather Data");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        Stage stage = (Stage) exportBtn.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                if (file.getName().endsWith(".csv")) {
                    exportToCsv(file);
                } else if (file.getName().endsWith(".json")) {
                    exportToJson(file);
                }
            } catch (IOException e) {
                setError("Export failed: " + e.getMessage());
            }
        }
    }

    private void exportToCsv(File file) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("Date,Condition,Temp Max,Temp Min,Temp Avg,Humidity,Rain Chance,Snow Chance,Precipitation,Max Wind,UV Index\n");

        for (DayWeatherData day : dayDataList) {
            csv.append(String.format("%s,%s,%.1f,%.1f,%.1f,%d,%d,%d,%.1f,%.1f,%.1f\n",
                day.dateStr, day.condition, day.tempMax, day.tempMin, day.tempAvg,
                day.humidity, day.rainChance, day.snowChance, day.precipitation,
                day.windMax, day.uvIndex));
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(csv.toString());
        }
    }

    private void exportToJson(File file) throws IOException {
        JSONObject exportData = new JSONObject();
        JSONObject location = weatherData.getJSONObject("location");
        exportData.put("location", location);

        JSONArray daysArray = new JSONArray();
        for (DayWeatherData day : dayDataList) {
            JSONObject dayObj = new JSONObject();
            dayObj.put("date", day.dateStr);
            dayObj.put("condition", day.condition);
            dayObj.put("temp_max", day.tempMax);
            dayObj.put("temp_min", day.tempMin);
            dayObj.put("temp_avg", day.tempAvg);
            dayObj.put("humidity", day.humidity);
            dayObj.put("rain_chance", day.rainChance);
            dayObj.put("snow_chance", day.snowChance);
            dayObj.put("precipitation", day.precipitation);
            dayObj.put("max_wind", day.windMax);
            dayObj.put("uv_index", day.uvIndex);
            daysArray.put(dayObj);
        }
        exportData.put("forecast", daysArray);

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(exportData.toString(4));
        }
    }

    private void setError(String message) {
        Platform.runLater(() -> {
            locationLabel.setText("Weather Forecast");
            updatedLabel.setText(message);
        });
    }

    private static class DayWeatherData {
        String dateStr;
        String condition;
        double tempMax;
        double tempMin;
        double tempAvg;
        int humidity;
        int rainChance;
        int snowChance;
        double precipitation;
        double windMax;
        double uvIndex;

        DayWeatherData(JSONObject dayObj) {
            String date = dayObj.getString("date");
            dateStr = LocalDate.parse(date).format(DATE_FORMAT);
            
            JSONObject day = dayObj.getJSONObject("day");
            condition = day.getJSONObject("condition").getString("text");
            tempMax = day.getDouble("maxtemp_c");
            tempMin = day.getDouble("mintemp_c");
            tempAvg = day.getDouble("avgtemp_c");
            humidity = day.getInt("avghumidity");
            rainChance = day.getInt("daily_chance_of_rain");
            snowChance = day.getInt("daily_chance_of_snow");
            precipitation = day.getDouble("totalprecip_mm");
            windMax = day.getDouble("maxwind_kph");
            uvIndex = day.getDouble("uv");
        }
    }

    private static class WeatherConfig {
        private final String apiKey;
        private final String location;
        private final int days;

        private WeatherConfig(String apiKey, String location, int days) {
            this.apiKey = apiKey;
            this.location = location;
            this.days = days;
        }

        private boolean isValid() {
            return apiKey != null && !apiKey.isBlank() && location != null && !location.isBlank();
        }

        private boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }

        private static WeatherConfig load() {
            Properties props = new Properties();
            try (InputStream input = WeatherController.class.getResourceAsStream("/config.properties")) {
                if (input != null) {
                    props.load(input);
                }
            } catch (IOException ignored) {
                return new WeatherConfig("", "", 3);
            }
            String apiKey = props.getProperty("weather.apiKey", "").trim();
            String location = props.getProperty("weather.location", "Tunisia").trim();
            int days = parseDays(props.getProperty("weather.days", "3"));
            return new WeatherConfig(apiKey, location, days);
        }

        private static int parseDays(String value) {
            try {
                int parsed = Integer.parseInt(value);
                return Math.max(1, Math.min(parsed, 7));
            } catch (NumberFormatException e) {
                return 3;
            }
        }
    }
}
