package com.example.agrisense360.controllers;

import com.example.agrisense360.entity.Camera;
import com.example.agrisense360.entity.Equipment;
import com.example.agrisense360.entity.Maintenance;
import com.example.agrisense360.entity.MotionEvent;
import com.example.agrisense360.services.GeminiBriefingService;
import com.example.agrisense360.services.ResendEmailService;
import com.example.agrisense360.services.ServiceEquipment;
import com.example.agrisense360.services.ServiceMaintenance;
import com.example.agrisense360.services.ServiceMotionEvent;
import com.example.agrisense360.services.TwilioSmsService;
import com.example.agrisense360.utils.SessionCameraManager;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WeatherController implements Initializable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d");
    private JSONObject weatherData;
    private JSONArray forecastDays;
    private List<DayWeatherData> dayDataList = new ArrayList<>();

    @FXML private Label locationLabel;
    @FXML private Label updatedLabel;
    @FXML private TextField locationField;
    @FXML private Button loadLocationBtn;
    @FXML private VBox aiSection;
    @FXML private Button generateAiBriefingBtn;
    @FXML private Button exportAiBriefingBtn;
    @FXML private ImageView aiWeatherIconView;
    @FXML private Label aiBriefingMetaLabel;
    @FXML private Label riskBadgeLabel;
    @FXML private Label patternBadgeLabel;
    @FXML private Label loopholeBadgeLabel;
    @FXML private TextArea aiBriefingArea;
    @FXML private VBox smsSection;
    @FXML private TextField smsToField;
    @FXML private TextField smsFromField;
    @FXML private TextField smsTimeField;
    @FXML private CheckBox smsDailyCheck;
    @FXML private Button saveSmsScheduleBtn;
    @FXML private Button sendSmsNowBtn;
    @FXML private Label smsStatusLabel;
    @FXML private VBox emailSection;
    @FXML private TextField emailToField;
    @FXML private TextField emailSubjectField;
    @FXML private Button sendEmailNowBtn;
    @FXML private Label emailStatusLabel;
    @FXML private Label daySelectorLabel;
    @FXML private ComboBox<String> daySelector;
    @FXML private ToggleGroup viewMode;
    @FXML private ToggleButton dayViewBtn;
    @FXML private ToggleButton weeklyViewBtn;
    @FXML private StackPane chartsContainer;
    @FXML private VBox dayViewPanel;
    @FXML private VBox weeklyViewPanel;
    @FXML private VBox forecastSection;
    @FXML private HBox viewOptionsBox;
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
    private final GeminiBriefingService geminiBriefingService = new GeminiBriefingService();
    private final TwilioSmsService twilioSmsService = new TwilioSmsService();
    private final ResendEmailService resendEmailService = new ResendEmailService();
    private boolean aiDashboardMode;
    private volatile String lastAiFullText = "";
    private final ScheduledExecutorService smsScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "ai-sms-scheduler");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean dailySmsEnabled;
    private volatile LocalTime dailySmsTime;
    private volatile String dailySmsToNumber = "";
    private volatile String dailySmsFromNumber = "";
    private volatile LocalDate lastDailySmsSentDate;


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

        if (generateAiBriefingBtn != null) {
            generateAiBriefingBtn.setOnAction(e -> onGenerateAiBriefing());
        }
        if (exportAiBriefingBtn != null) {
            exportAiBriefingBtn.setOnAction(e -> onExportAiBriefing());
        }

        if (aiBriefingArea != null) {
            aiBriefingArea.setWrapText(true);
            aiBriefingArea.setEditable(false);
            aiBriefingArea.setText("Generate a daily AI briefing after weather is loaded.");
        }
        if (aiBriefingMetaLabel != null) {
            aiBriefingMetaLabel.setText("Cross-checks weather, equipment, maintenance, and camera events to detect risks and loopholes.");
        }
        if (riskBadgeLabel != null) {
            riskBadgeLabel.setText("Risk: --");
        }
        if (patternBadgeLabel != null) {
            patternBadgeLabel.setText("Patterns: --");
        }
        if (loopholeBadgeLabel != null) {
            loopholeBadgeLabel.setText("Loopholes: --");
        }

        if (emailSubjectField != null) {
            emailSubjectField.setText("AgriSense AI Daily Briefing");
        }
        if (sendEmailNowBtn != null) {
            sendEmailNowBtn.setOnAction(e -> onSendEmailNow());
        }
        setEmailStatus(resendEmailService.isConfigured()
            ? "Email API ready. Configure recipient and send."
            : "Email API not configured yet (Resend key + sender needed).");

        if (smsFromField != null && twilioSmsService.getDefaultFromNumber() != null && !twilioSmsService.getDefaultFromNumber().isBlank()) {
            smsFromField.setText(twilioSmsService.getDefaultFromNumber());
        }
        if (smsTimeField != null) {
            smsTimeField.setText("08:30");
        }
        setSmsStatus(twilioSmsService.isConfigured()
            ? "Twilio ready. Configure recipient and send."
            : "Twilio credentials are not configured yet.");
        startSmsScheduler();
    }

    @FXML
    private void backToEquipment() {
        MainController controller = MainController.getInstance();
        if (controller != null) {
            controller.showEquipment();
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
            applyAiModeUi();
        });
    }

    public void setAiDashboardMode(boolean enabled) {
        this.aiDashboardMode = enabled;
        Platform.runLater(this::applyAiModeUi);
    }

    private void applyAiModeUi() {
        boolean forecastVisible = !aiDashboardMode;
        if (forecastSection != null) {
            forecastSection.setManaged(forecastVisible);
            forecastSection.setVisible(forecastVisible);
        }
        if (daySelector != null) {
            daySelector.setManaged(forecastVisible);
            daySelector.setVisible(forecastVisible);
        }
        if (daySelectorLabel != null) {
            daySelectorLabel.setManaged(forecastVisible);
            daySelectorLabel.setVisible(forecastVisible);
        }
        if (viewOptionsBox != null) {
            viewOptionsBox.setManaged(forecastVisible);
            viewOptionsBox.setVisible(forecastVisible);
        }
        if (exportBtn != null) {
            exportBtn.setManaged(forecastVisible);
            exportBtn.setVisible(forecastVisible);
        }
        if (aiSection != null) {
            aiSection.setManaged(aiDashboardMode);
            aiSection.setVisible(aiDashboardMode);
        }
        if (locationLabel != null && aiDashboardMode) {
            locationLabel.setText("AI Dashboard - Today's Summary");
        }
        if (smsSection != null) {
            smsSection.setManaged(aiDashboardMode);
            smsSection.setVisible(aiDashboardMode);
        }
        if (emailSection != null) {
            emailSection.setManaged(aiDashboardMode);
            emailSection.setVisible(aiDashboardMode);
        }
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

    @FXML
    private void onGenerateAiBriefing() {
        if (!aiDashboardMode) {
            setError("AI summary is available in the AI Dashboard page.");
            return;
        }
        if (weatherData == null || dayDataList.isEmpty()) {
            setError("Load weather first before generating AI briefing.");
            return;
        }
        if (!geminiBriefingService.isConfigured()) {
            setError("Groq API key is missing. Set GROQ_API_KEY or ai.groqApiKey in config.properties.");
            return;
        }

        if (aiBriefingArea != null) {
            aiBriefingArea.setText("Generating AI briefing...");
        }
        setAiMeta("Analyzing cross-signals and pattern correlations...");

        generateAiBriefingAsync(null, null);
    }

    @FXML
    private void onExportAiBriefing() {
        String content = aiBriefingArea != null && aiBriefingArea.getText() != null ? aiBriefingArea.getText().trim() : "";
        if (content.isBlank()) {
            setAiMeta("Generate briefing first before export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export AI Briefing");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("HTML Files", "*.html"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        chooser.setInitialFileName("ai-briefing-" + LocalDate.now() + ".html");

        Stage stage = null;
        if (aiSection != null && aiSection.getScene() != null) {
            stage = (Stage) aiSection.getScene().getWindow();
        }

        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try {
            if (file.getName().toLowerCase(Locale.ROOT).endsWith(".txt")) {
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                    writer.write(content);
                }
            } else {
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                    writer.write(buildBriefingHtmlDocument(content));
                }
            }
            setAiMeta("Briefing exported: " + file.getName());
        } catch (Exception e) {
            setAiMeta("Export failed: " + e.getMessage());
        }
    }

    @FXML
    private void onSendEmailNow() {
        if (!aiDashboardMode) {
            setEmailStatus("Email sending is available only in AI Dashboard mode.");
            return;
        }
        if (!resendEmailService.isConfigured()) {
            setEmailStatus("Configure email.resendApiKey and email.sender first.");
            return;
        }
        String toEmail = emailToField != null && emailToField.getText() != null ? emailToField.getText().trim() : "";
        if (toEmail.isBlank()) {
            setEmailStatus("Recipient email is required.");
            return;
        }

        String subject = emailSubjectField != null && emailSubjectField.getText() != null
            ? emailSubjectField.getText().trim()
            : "AgriSense AI Daily Briefing";

        String content = aiBriefingArea != null && aiBriefingArea.getText() != null ? aiBriefingArea.getText().trim() : "";
        if (content.isBlank() || content.startsWith("Generate a daily AI briefing")) {
            setEmailStatus("Generating AI summary before email send...");
            generateAiBriefingAsync(
                generated -> sendAiEmail(toEmail, subject, generated),
                error -> setEmailStatus("Email canceled. AI generation failed: " + error)
            );
            return;
        }
        sendAiEmail(toEmail, subject, content);
    }

    @FXML
    private void onSendSmsNow() {
        if (!aiDashboardMode) {
            setSmsStatus("SMS sending is available only in AI Dashboard mode.");
            return;
        }
        if (!twilioSmsService.isConfigured()) {
            setSmsStatus("Twilio credentials are missing. Configure TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN.");
            return;
        }

        String toNumber = smsToField != null && smsToField.getText() != null ? smsToField.getText().trim() : "";
        String fromNumber = smsFromField != null && smsFromField.getText() != null ? smsFromField.getText().trim() : "";
        if (toNumber.isBlank()) {
            setSmsStatus("Recipient number is required.");
            return;
        }

        String summaryText = aiBriefingArea != null && aiBriefingArea.getText() != null ? aiBriefingArea.getText().trim() : "";
        if (summaryText.isBlank() || summaryText.startsWith("Generate a daily AI briefing")) {
            setSmsStatus("Generating AI summary, then sending SMS...");
            generateAiBriefingAsync(
                generated -> sendSummarySms(generated, toNumber, fromNumber, false),
                error -> setSmsStatus("SMS canceled. AI generation failed: " + error)
            );
            return;
        }

        sendSummarySms(summaryText, toNumber, fromNumber, false);
    }

    @FXML
    private void onSaveSmsSchedule() {
        if (!twilioSmsService.isConfigured()) {
            setSmsStatus("Twilio credentials are missing. Configure credentials first.");
            return;
        }
        if (smsDailyCheck == null || smsTimeField == null || smsToField == null) {
            setSmsStatus("SMS schedule controls are unavailable.");
            return;
        }

        String toNumber = smsToField.getText() != null ? smsToField.getText().trim() : "";
        String fromNumber = smsFromField != null && smsFromField.getText() != null ? smsFromField.getText().trim() : "";
        if (toNumber.isBlank()) {
            setSmsStatus("Recipient number is required for scheduling.");
            return;
        }

        if (!smsDailyCheck.isSelected()) {
            dailySmsEnabled = false;
            setSmsStatus("Daily SMS disabled.");
            return;
        }

        String timeText = smsTimeField.getText() != null ? smsTimeField.getText().trim() : "";
        try {
            LocalTime parsedTime = LocalTime.parse(timeText, DateTimeFormatter.ofPattern("H:mm"));
            dailySmsTime = parsedTime;
            dailySmsToNumber = toNumber;
            dailySmsFromNumber = fromNumber;
            dailySmsEnabled = true;
            setSmsStatus("Daily SMS scheduled at " + parsedTime + " to " + toNumber + ".");
        } catch (DateTimeParseException e) {
            setSmsStatus("Invalid time format. Use HH:mm (example: 08:30).");
        }
    }

    private void generateAiBriefingAsync(Consumer<String> onSuccess, Consumer<String> onError) {
        if (weatherData == null || dayDataList.isEmpty()) {
            if (onError != null) {
                onError.accept("Weather data is not loaded.");
            }
            return;
        }
        if (!geminiBriefingService.isConfigured()) {
            if (onError != null) {
                onError.accept("Groq API key is missing.");
            }
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                JSONObject context = buildAiContext();
                String liveSnapshot = formatLiveSnapshotText(context);
                Platform.runLater(() -> {
                    if (aiBriefingArea != null) {
                        aiBriefingArea.setText(liveSnapshot + "\n\nGenerating AI narrative...");
                    }
                });

                String briefing = geminiBriefingService.generateDailyBriefing(context.toString(2));
                String fullText = liveSnapshot + "\n\nAI Narrative\n" + briefing;
                Platform.runLater(() -> {
                    if (aiBriefingArea != null) {
                        aiBriefingArea.setText(fullText);
                    }
                    lastAiFullText = fullText;
                    updateAiWeatherIcon();
                    updateAiBadges(context);
                    setAiMeta(buildAiMetaSummary(context));
                });
                if (onSuccess != null) {
                    onSuccess.accept(fullText);
                }
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Unknown error";
                Platform.runLater(() -> setError("AI briefing failed: " + message));
                if (onError != null) {
                    onError.accept(message);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void startSmsScheduler() {
        smsScheduler.scheduleAtFixedRate(this::checkDailySmsTrigger, 20, 20, TimeUnit.SECONDS);
    }

    private void checkDailySmsTrigger() {
        if (!dailySmsEnabled || dailySmsTime == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (lastDailySmsSentDate != null && lastDailySmsSentDate.equals(now.toLocalDate())) {
            return;
        }

        if (now.getHour() == dailySmsTime.getHour() && now.getMinute() == dailySmsTime.getMinute()) {
            lastDailySmsSentDate = now.toLocalDate();
            String toNumber = dailySmsToNumber;
            String fromNumber = dailySmsFromNumber;
            setSmsStatus("Daily trigger reached. Generating AI summary for SMS...");
            generateAiBriefingAsync(
                generated -> sendSummarySms(generated, toNumber, fromNumber, true),
                error -> setSmsStatus("Daily SMS failed during AI generation: " + error)
            );
        }
    }

    private void sendSummarySms(String summaryText, String toNumber, String fromNumber, boolean scheduled) {
        String message = compactSmsBody(summaryText);
        Thread thread = new Thread(() -> {
            try {
                String sid = twilioSmsService.sendSms(toNumber, fromNumber, message);
                String prefix = scheduled ? "Daily SMS accepted" : "SMS accepted";
                String suffix = sid != null && !sid.isBlank() ? " (SID: " + sid + ")" : "";
                setSmsStatus(prefix + suffix + ". Checking delivery status...");

                if (sid != null && !sid.isBlank()) {
                    Thread.sleep(3500);
                    JSONObject status = twilioSmsService.getMessageStatus(sid);
                    String twilioStatus = status.optString("status", "unknown");
                    int errorCode = status.optInt("error_code", 0);
                    if ("delivered".equalsIgnoreCase(twilioStatus) || "sent".equalsIgnoreCase(twilioStatus)) {
                        setSmsStatus("SMS delivered (SID: " + sid + ")");
                    } else if ("failed".equalsIgnoreCase(twilioStatus) || "undelivered".equalsIgnoreCase(twilioStatus)) {
                        if (errorCode == 30454) {
                            dailySmsEnabled = false;
                            setSmsStatus("Twilio limit reached (30454). Upgrade account or wait for quota reset; daily SMS paused.");
                            return;
                        }
                        String fallbackMessage = buildEmergencySms(summaryText);
                        String fallbackSid = twilioSmsService.sendSms(toNumber, fromNumber, fallbackMessage);
                        Thread.sleep(2500);
                        JSONObject fallbackStatus = twilioSmsService.getMessageStatus(fallbackSid);
                        String fallbackTwilioStatus = fallbackStatus.optString("status", "unknown");
                        if ("delivered".equalsIgnoreCase(fallbackTwilioStatus) || "sent".equalsIgnoreCase(fallbackTwilioStatus)) {
                            setSmsStatus("Primary SMS blocked (error " + errorCode + "), fallback delivered.");
                        } else {
                            int fallbackError = fallbackStatus.optInt("error_code", 0);
                            setSmsStatus("SMS failed. Primary error " + errorCode + ", fallback error " + fallbackError + ".");
                        }
                    } else {
                        setSmsStatus("SMS status: " + twilioStatus + " (SID: " + sid + ")");
                    }
                }
            } catch (Exception e) {
                String prefix = scheduled ? "Daily SMS failed" : "SMS failed";
                setSmsStatus(prefix + ": " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private String compactSmsBody(String text) {
        String cleaned = text == null ? "" : text.replace("\r", "").trim();
        if (cleaned.isBlank()) {
            return "AI: no data";
        }

        String summary = firstMatchingLine(cleaned, "Risk Level:");
        String riskToken = extractRiskToken(summary);

        String message = "AI " + riskToken;
        message = message.replaceAll("\\s+", " ").trim();

        int maxLen = 24;
        if (message.length() <= maxLen) {
            return message;
        }
        return message.substring(0, maxLen);
    }

    private String buildEmergencySms(String text) {
        String cleaned = text == null ? "" : text.replace("\r", "").trim();
        String summary = firstMatchingLine(cleaned, "Risk Level:");
        String riskToken = extractRiskToken(summary).replace("risk:", "");
        String message = "AI " + riskToken;
        int maxLen = 12;
        if (message.length() <= maxLen) {
            return message;
        }
        return message.substring(0, maxLen);
    }

    private String extractRiskToken(String summaryLine) {
        if (summaryLine == null) {
            return "risk?";
        }
        String upper = summaryLine.toUpperCase(Locale.ROOT);
        if (upper.contains("HIGH")) {
            return "risk:HIGH";
        }
        if (upper.contains("MEDIUM")) {
            return "risk:MED";
        }
        if (upper.contains("LOW")) {
            return "risk:LOW";
        }
        return "risk:?";
    }

    private String firstMatchingLine(String text, String startsWith) {
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(startsWith)) {
                return trimmed;
            }
        }
        return null;
    }

    private String firstBulletLine(String text) {
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ")) {
                return trimmed.substring(2).trim();
            }
        }
        return null;
    }

    private void setSmsStatus(String message) {
        Platform.runLater(() -> {
            if (smsStatusLabel != null) {
                smsStatusLabel.setText(message);
            }
        });
    }

    private void setEmailStatus(String message) {
        Platform.runLater(() -> {
            if (emailStatusLabel != null) {
                emailStatusLabel.setText(message);
            }
        });
    }

    private void sendAiEmail(String toEmail, String subject, String content) {
        Thread thread = new Thread(() -> {
            try {
                String messageId = resendEmailService.sendEmail(toEmail, subject, buildBriefingHtmlDocument(content), content);
                setEmailStatus("Email sent" + (messageId != null && !messageId.isBlank() ? " (ID: " + messageId + ")" : "") + ".");
            } catch (Exception e) {
                setEmailStatus("Email failed: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private String buildBriefingHtmlDocument(String content) {
        String escaped = content == null ? "" : content
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "<br/>");
        String generatedAt = LocalDateTime.now().toString();
        return "<!DOCTYPE html>"
            + "<html><head><meta charset=\"UTF-8\"><title>AgriSense AI Briefing</title>"
            + "<style>"
            + "body{font-family:Segoe UI,Arial,sans-serif;background:#f3f8ee;margin:0;padding:24px;color:#20301b;}"
            + ".card{background:#fff;border-radius:14px;padding:18px 20px;box-shadow:0 6px 18px rgba(0,0,0,.08);}"
            + ".title{font-size:24px;font-weight:700;color:#2f7d32;margin-bottom:8px;}"
            + ".meta{color:#5f7256;font-size:12px;margin-bottom:14px;}"
            + ".content{line-height:1.6;font-size:14px;white-space:normal;}"
            + "</style></head><body>"
            + "<div class=\"card\">"
            + "<div class=\"title\">🌿 AgriSense AI Daily Briefing</div>"
            + "<div class=\"meta\">Generated at " + generatedAt + "</div>"
            + "<div class=\"content\">" + escaped + "</div>"
            + "</div></body></html>";
    }

    private void updateAiWeatherIcon() {
        if (aiWeatherIconView == null || weatherData == null) {
            return;
        }
        try {
            String iconPath = weatherData.getJSONObject("current").getJSONObject("condition").optString("icon", "");
            if (iconPath == null || iconPath.isBlank()) {
                return;
            }
            String iconUrl = iconPath.startsWith("http") ? iconPath : "https:" + iconPath;
            aiWeatherIconView.setImage(new Image(iconUrl, true));
        } catch (Exception ignored) {
        }
    }

    private void updateAiBadges(JSONObject context) {
        JSONObject live = context.optJSONObject("live_snapshot");
        JSONArray patterns = context.optJSONArray("pattern_insights");
        JSONArray loopholes = context.optJSONArray("operational_loopholes");
        String risk = live != null ? live.optString("risk_level", "UNKNOWN") : "UNKNOWN";
        if (riskBadgeLabel != null) {
            riskBadgeLabel.setText("Risk: " + risk);
        }
        if (patternBadgeLabel != null) {
            patternBadgeLabel.setText("Patterns: " + (patterns != null ? patterns.length() : 0));
        }
        if (loopholeBadgeLabel != null) {
            loopholeBadgeLabel.setText("Loopholes: " + (loopholes != null ? loopholes.length() : 0));
        }
    }

    private JSONObject buildAiContext() {
        JSONObject context = new JSONObject();
        LocalDate today = LocalDate.now();

        JSONObject location = weatherData.optJSONObject("location");
        context.put("location", location != null ? location.optString("name", "Unknown") : "Unknown");
        context.put("country", location != null ? location.optString("country", "") : "");
        context.put("generated_at", LocalDateTime.now().toString());

        DayWeatherData todayWeather = dayDataList.get(0);
        JSONObject weather = new JSONObject();
        weather.put("date", todayWeather.dateStr);
        weather.put("condition", todayWeather.condition);
        weather.put("temp_max_c", todayWeather.tempMax);
        weather.put("temp_min_c", todayWeather.tempMin);
        weather.put("avg_temp_c", todayWeather.tempAvg);
        weather.put("humidity_pct", todayWeather.humidity);
        weather.put("rain_chance_pct", todayWeather.rainChance);
        weather.put("wind_max_kph", todayWeather.windMax);
        weather.put("uv_index", todayWeather.uvIndex);
        context.put("today_weather", weather);

        List<Equipment> equipmentList = new ArrayList<>();
        Map<Integer, Equipment> equipmentById = new HashMap<>();
        JSONObject equipmentSummary = new JSONObject();
        long nonActiveCount = 0;
        try {
            ServiceEquipment serviceEquipment = new ServiceEquipment();
            equipmentList = serviceEquipment.getAll();
            JSONObject statusBreakdown = new JSONObject();
            JSONObject typeBreakdown = new JSONObject();
            JSONArray flaggedWeatherEquipment = new JSONArray();
            boolean weatherRisk = todayWeather.rainChance >= 60 || todayWeather.windMax >= 45 || todayWeather.precipitation >= 8 || todayWeather.uvIndex >= 8;

            for (Equipment equipment : equipmentList) {
                if (equipment.getId() != null) {
                    equipmentById.put(equipment.getId(), equipment);
                }

                String status = equipment.getStatus() != null && !equipment.getStatus().isBlank()
                    ? equipment.getStatus().trim().toLowerCase(Locale.ROOT)
                    : "unknown";
                incrementJsonCount(statusBreakdown, status);

                String type = equipment.getType() != null && !equipment.getType().isBlank()
                    ? equipment.getType().trim().toLowerCase(Locale.ROOT)
                    : "unknown";
                incrementJsonCount(typeBreakdown, type);

                if (weatherRisk && isActive(equipment) && isHeavyFieldVehicle(equipment)) {
                    flaggedWeatherEquipment.put(new JSONObject()
                        .put("equipment_id", equipment.getId())
                        .put("name", safeName(equipment))
                        .put("type", equipment.getType() != null ? equipment.getType() : "")
                        .put("status", equipment.getStatus() != null ? equipment.getStatus() : ""));
                }
            }
            nonActiveCount = equipmentList.stream()
                .filter(eq -> eq.getStatus() != null)
                .filter(eq -> {
                    String status = eq.getStatus().toLowerCase(Locale.ROOT);
                    return status.contains("inactive") || status.contains("maintenance") || status.contains("fault");
                })
                .count();
            equipmentSummary.put("total", equipmentList.size());
            equipmentSummary.put("non_active_or_fault", nonActiveCount);
            equipmentSummary.put("status_breakdown", statusBreakdown);
            equipmentSummary.put("type_breakdown", typeBreakdown);
            equipmentSummary.put("weather_sensitive_active", flaggedWeatherEquipment);
        } catch (Exception e) {
            equipmentSummary.put("db_error", e.getMessage());
        }
        context.put("equipment_summary", equipmentSummary);

        JSONObject motionSummary = new JSONObject();
        JSONArray motionTimeline = new JSONArray();
        JSONObject motionPatternAnalysis = new JSONObject();
        try {
            SessionCameraManager sessionCameraManager = SessionCameraManager.getInstance();
            Map<Integer, Camera> sessionCameraById = new HashMap<>();
            List<MotionEvent> todayEvents = new ArrayList<>();

            for (Camera camera : sessionCameraManager.getAllCameras()) {
                sessionCameraById.put(camera.getId(), camera);
                List<MotionEvent> cameraEvents = sessionCameraManager.getMotionEventsForCamera(camera.getId());
                for (MotionEvent event : cameraEvents) {
                    if (event.getDetectionTime() != null && event.getDetectionTime().toLocalDate().equals(today)) {
                        todayEvents.add(event);
                    }
                }
            }

            String source = "session";
            if (todayEvents.isEmpty()) {
                source = "database_fallback";
                ServiceMotionEvent serviceMotionEvent = new ServiceMotionEvent();
                List<MotionEvent> events = serviceMotionEvent.getAll();
                for (MotionEvent event : events) {
                    if (event.getDetectionTime() != null && event.getDetectionTime().toLocalDate().equals(today)) {
                        todayEvents.add(event);
                    }
                }
            }

            todayEvents = todayEvents.stream()
                .sorted(Comparator.comparing(MotionEvent::getDetectionTime).reversed())
                .toList();

            long highCount = todayEvents.stream().filter(e -> "HIGH".equalsIgnoreCase(e.getSeverity())).count();
            long mediumCount = todayEvents.stream().filter(e -> "MEDIUM".equalsIgnoreCase(e.getSeverity())).count();
            long lowCount = todayEvents.stream().filter(e -> "LOW".equalsIgnoreCase(e.getSeverity())).count();

            motionSummary.put("source", source);
            motionSummary.put("today_events_total", todayEvents.size());
            motionSummary.put("high_severity", highCount);
            motionSummary.put("medium_severity", mediumCount);
            motionSummary.put("low_severity", lowCount);
            motionSummary.put("all_severities_total", highCount + mediumCount + lowCount);
            motionSummary.put("session_cameras", sessionCameraById.size());
            if (!todayEvents.isEmpty()) {
                MotionEvent last = todayEvents.get(0);
                motionSummary.put("last_event_time", last.getDetectionTime().toString());
                motionSummary.put("last_event_severity", last.getSeverity());
            }

            Map<String, Integer> byCamera = new TreeMap<>();
            Map<Integer, Integer> byHour = new TreeMap<>();
            for (MotionEvent event : todayEvents) {
                Camera cam = sessionCameraById.get(event.getCameraId());
                String cameraLabel = cam != null && cam.getCameraName() != null && !cam.getCameraName().isBlank()
                    ? cam.getCameraName()
                    : "Camera #" + event.getCameraId();
                byCamera.put(cameraLabel, byCamera.getOrDefault(cameraLabel, 0) + 1);
                if (event.getDetectionTime() != null) {
                    int hour = event.getDetectionTime().getHour();
                    byHour.put(hour, byHour.getOrDefault(hour, 0) + 1);
                }
            }

            JSONArray cameraDistribution = new JSONArray();
            byCamera.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(6)
                .forEach(entry -> cameraDistribution.put(new JSONObject()
                    .put("camera", entry.getKey())
                    .put("events", entry.getValue())));

            JSONArray hourlyDistribution = new JSONArray();
            byHour.forEach((hour, count) -> hourlyDistribution.put(new JSONObject()
                .put("hour", hour)
                .put("events", count)));

            String dominantSeverity = highCount >= mediumCount && highCount >= lowCount ? "HIGH"
                : (mediumCount >= lowCount ? "MEDIUM" : "LOW");
            motionPatternAnalysis.put("dominant_severity", dominantSeverity);
            motionPatternAnalysis.put("camera_distribution", cameraDistribution);
            motionPatternAnalysis.put("hourly_distribution", hourlyDistribution);

            if (!byHour.isEmpty()) {
                int peakHour = -1;
                int peakCount = 0;
                int offHoursCount = 0;
                for (Map.Entry<Integer, Integer> entry : byHour.entrySet()) {
                    int hour = entry.getKey();
                    int count = entry.getValue();
                    if (count > peakCount) {
                        peakCount = count;
                        peakHour = hour;
                    }
                    if (hour < 6 || hour >= 22) {
                        offHoursCount += count;
                    }
                }
                motionPatternAnalysis.put("peak_hour", peakHour);
                motionPatternAnalysis.put("peak_hour_events", peakCount);
                motionPatternAnalysis.put("off_hours_events", offHoursCount);
            }

            int maxTimeline = Math.min(todayEvents.size(), 12);
            for (int i = 0; i < maxTimeline; i++) {
                MotionEvent event = todayEvents.get(i);
                JSONObject eventJson = new JSONObject()
                    .put("time", event.getDetectionTime() != null ? event.getDetectionTime().toString() : "")
                    .put("severity", event.getSeverity() != null ? event.getSeverity() : "UNKNOWN")
                    .put("motion_frames", event.getMotionFrameCount())
                    .put("camera_id", event.getCameraId())
                    .put("elapsed_seconds", event.getElapsedSeconds());

                Camera camera = sessionCameraById.get(event.getCameraId());
                if (camera != null) {
                    eventJson.put("camera_name", camera.getCameraName() != null ? camera.getCameraName() : "");
                    eventJson.put("camera_location", camera.getLocation() != null ? camera.getLocation() : "");
                }
                motionTimeline.put(eventJson);
            }
        } catch (Exception e) {
            motionSummary.put("source", "unavailable");
            motionSummary.put("error", e.getMessage());
            motionPatternAnalysis.put("error", e.getMessage());
        }
        context.put("motion_summary", motionSummary);
        context.put("motion_timeline", motionTimeline);
        context.put("motion_pattern_analysis", motionPatternAnalysis);

        JSONArray upcomingMaintenance = new JSONArray();
        JSONArray maintenanceToday = new JSONArray();
        JSONObject maintenanceSummary = new JSONObject();
        List<Maintenance> nextWeekMaintenance = new ArrayList<>();
        int overdueCount = 0;
        int dueSoonCount = 0;
        double todayCostTotal = 0;
        try {
            ServiceMaintenance serviceMaintenance = new ServiceMaintenance();
            List<Maintenance> maintenances = serviceMaintenance.getAll();
            List<Maintenance> todayMaintenance = maintenances.stream()
                .filter(m -> m.getMaintenanceDate() != null && m.getMaintenanceDate().isEqual(today))
                .sorted(Comparator.comparing(Maintenance::getMaintenanceDate))
                .toList();

            nextWeekMaintenance = maintenances.stream()
                .filter(m -> m.getMaintenanceDate() != null && !m.getMaintenanceDate().isBefore(today) && !m.getMaintenanceDate().isAfter(today.plusDays(7)))
                .sorted(Comparator.comparing(Maintenance::getMaintenanceDate))
                .toList();

            maintenanceSummary.put("today_count", todayMaintenance.size());
            maintenanceSummary.put("upcoming_7_days", nextWeekMaintenance.size());

            overdueCount = (int) maintenances.stream()
                .filter(m -> m.getMaintenanceDate() != null && m.getMaintenanceDate().isBefore(today))
                .count();
            dueSoonCount = (int) maintenances.stream()
                .filter(m -> m.getMaintenanceDate() != null && !m.getMaintenanceDate().isBefore(today) && !m.getMaintenanceDate().isAfter(today.plusDays(2)))
                .count();
            todayCostTotal = todayMaintenance.stream()
                .filter(m -> m.getCost() != null)
                .map(m -> m.getCost().doubleValue())
                .reduce(0.0, Double::sum);
            maintenanceSummary.put("overdue_count", overdueCount);
            maintenanceSummary.put("due_within_48h", dueSoonCount);
            maintenanceSummary.put("today_estimated_cost", todayCostTotal);

            int maxToday = Math.min(todayMaintenance.size(), 4);
            for (int i = 0; i < maxToday; i++) {
                Maintenance maintenance = todayMaintenance.get(i);
                JSONObject maintenanceObj = new JSONObject()
                    .put("date", maintenance.getMaintenanceDate().toString())
                    .put("equipment_id", maintenance.getEquipmentId())
                    .put("type", maintenance.getMaintenanceType())
                    .put("cost", maintenance.getCost() != null ? maintenance.getCost().toString() : "");
                Equipment equipment = equipmentById.get(maintenance.getEquipmentId());
                if (equipment != null) {
                    maintenanceObj.put("equipment_name", equipment.getName() != null ? equipment.getName() : "");
                    maintenanceObj.put("equipment_type", equipment.getType() != null ? equipment.getType() : "");
                    maintenanceObj.put("equipment_status", equipment.getStatus() != null ? equipment.getStatus() : "");
                }
                maintenanceToday.put(maintenanceObj);
            }

            int maxMaint = Math.min(nextWeekMaintenance.size(), 4);
            for (int i = 0; i < maxMaint; i++) {
                Maintenance maintenance = nextWeekMaintenance.get(i);
                JSONObject maintenanceObj = new JSONObject()
                    .put("date", maintenance.getMaintenanceDate().toString())
                    .put("equipment_id", maintenance.getEquipmentId())
                    .put("type", maintenance.getMaintenanceType())
                    .put("cost", maintenance.getCost() != null ? maintenance.getCost().toString() : "");
                Equipment equipment = equipmentById.get(maintenance.getEquipmentId());
                if (equipment != null) {
                    maintenanceObj.put("equipment_name", equipment.getName() != null ? equipment.getName() : "");
                    maintenanceObj.put("equipment_type", equipment.getType() != null ? equipment.getType() : "");
                    maintenanceObj.put("equipment_status", equipment.getStatus() != null ? equipment.getStatus() : "");
                }
                upcomingMaintenance.put(maintenanceObj);
            }
        } catch (Exception e) {
            maintenanceSummary.put("db_error", e.getMessage());
        }

        JSONObject weatherMotionCorrelation = new JSONObject();
        long highMotion = motionSummary.optLong("high_severity", 0);
        long mediumMotion = motionSummary.optLong("medium_severity", 0);
        long lowMotion = motionSummary.optLong("low_severity", 0);
        long allMotion = motionSummary.optLong("all_severities_total", highMotion + mediumMotion + lowMotion);
        boolean harshWeather = todayWeather.rainChance >= 60 || todayWeather.windMax >= 45 || todayWeather.precipitation >= 8;
        weatherMotionCorrelation.put("harsh_weather", harshWeather);
        weatherMotionCorrelation.put("high_motion_count", highMotion);
        weatherMotionCorrelation.put("all_motion_count", allMotion);
        weatherMotionCorrelation.put("camera_count", motionPatternAnalysis.optJSONArray("camera_distribution") != null
            ? motionPatternAnalysis.optJSONArray("camera_distribution").length() : 0);

        String correlationLevel;
        if (harshWeather && highMotion >= 2) {
            correlationLevel = "strong";
        } else if (harshWeather && allMotion > 0) {
            correlationLevel = "moderate";
        } else if (!harshWeather && highMotion == 0 && allMotion <= 2) {
            correlationLevel = "low";
        } else {
            correlationLevel = "neutral";
        }
        weatherMotionCorrelation.put("correlation_level", correlationLevel);

        JSONArray operationalLoopholes = new JSONArray();
        int cameraCount = weatherMotionCorrelation.optInt("camera_count", 0);
        if (harshWeather && cameraCount == 0) {
            operationalLoopholes.put(new JSONObject()
                .put("severity", "HIGH")
                .put("topic", "camera_coverage")
                .put("message", "Harsh weather risk detected but camera coverage data is unavailable. Blind spot for operational response."));
        }
        if (overdueCount > 0) {
            operationalLoopholes.put(new JSONObject()
                .put("severity", harshWeather ? "HIGH" : "MEDIUM")
                .put("topic", "maintenance_backlog")
                .put("message", String.format("%d maintenance task(s) are overdue, which may amplify weather and breakdown risk.", overdueCount)));
        }
        int totalEquipment = equipmentSummary.optInt("total", 0);
        if (totalEquipment > 0 && nonActiveCount >= Math.max(2, totalEquipment / 2)) {
            operationalLoopholes.put(new JSONObject()
                .put("severity", "MEDIUM")
                .put("topic", "fleet_availability")
                .put("message", "A large share of equipment is non-active/faulty, reducing resilience if weather conditions shift."));
        }
        if (motionPatternAnalysis.optInt("off_hours_events", 0) >= 2) {
            operationalLoopholes.put(new JSONObject()
                .put("severity", "MEDIUM")
                .put("topic", "off_hours_activity")
                .put("message", "Repeated off-hours motion suggests possible perimeter or scheduling blind spots."));
        }

        context.put("maintenance_summary", maintenanceSummary);
        context.put("maintenance_today", maintenanceToday);
        context.put("upcoming_maintenance", upcomingMaintenance);
        context.put("weather_motion_correlation", weatherMotionCorrelation);
        context.put("operational_loopholes", operationalLoopholes);

        JSONArray patternInsights = buildPatternInsights(todayWeather, equipmentList, nextWeekMaintenance, motionSummary, weatherMotionCorrelation, operationalLoopholes);
        context.put("pattern_insights", patternInsights);
        context.put("live_snapshot", buildLiveSnapshot(todayWeather, patternInsights, motionSummary, maintenanceSummary));

        return context;
    }

    private JSONArray buildPatternInsights(DayWeatherData todayWeather, List<Equipment> equipments,
                                           List<Maintenance> upcomingMaintenance, JSONObject motionSummary,
                                           JSONObject weatherMotionCorrelation,
                                           JSONArray operationalLoopholes) {
        JSONArray insights = new JSONArray();

        boolean harshWeather = todayWeather.rainChance >= 60 || todayWeather.windMax >= 45 || todayWeather.precipitation >= 8;
        boolean heatStress = todayWeather.tempMax >= 35 || todayWeather.uvIndex >= 8;

        if (harshWeather) {
            for (Equipment equipment : equipments) {
                if (isActive(equipment) && isHeavyFieldVehicle(equipment)) {
                    insights.put(new JSONObject()
                        .put("priority", "HIGH")
                        .put("type", "weather_equipment_conflict")
                        .put("message", String.format("%s is active, but today's weather is risky for field usage (rain %d%%, wind %.1f kph).", safeName(equipment), todayWeather.rainChance, todayWeather.windMax)));
                }
                if (insights.length() >= 3) {
                    break;
                }
            }
        }

        for (Maintenance maintenance : upcomingMaintenance) {
            if (maintenance.getMaintenanceDate() == null) {
                continue;
            }
            long daysUntil = LocalDate.now().until(maintenance.getMaintenanceDate()).getDays();
            if (daysUntil <= 3) {
                insights.put(new JSONObject()
                    .put("priority", harshWeather ? "HIGH" : "MEDIUM")
                    .put("type", "maintenance_window")
                    .put("message", String.format("Maintenance for equipment #%d is due in %d day(s). Plan downtime early to avoid weather disruption.", maintenance.getEquipmentId(), daysUntil)));
            }
            if (insights.length() >= 6) {
                break;
            }
        }

        long highMotion = motionSummary.optLong("high_severity", 0);
        long mediumMotion = motionSummary.optLong("medium_severity", 0);
        long lowMotion = motionSummary.optLong("low_severity", 0);
        long allMotion = motionSummary.optLong("all_severities_total", highMotion + mediumMotion + lowMotion);
        if (highMotion >= 2) {
            insights.put(new JSONObject()
                .put("priority", harshWeather ? "HIGH" : "MEDIUM")
                .put("type", "security_pattern")
                .put("message", "Multiple high-severity motion events detected today; increase monitoring around active equipment and storage zones."));
        }

        if (allMotion > 0 && (mediumMotion > 0 || lowMotion > 0)) {
            insights.put(new JSONObject()
                .put("priority", "MEDIUM")
                .put("type", "motion_activity_recap")
                .put("message", String.format("Overall camera activity today: HIGH=%d, MEDIUM=%d, LOW=%d. Review recurring medium/low zones before they escalate.", highMotion, mediumMotion, lowMotion)));
        }

        String correlationLevel = weatherMotionCorrelation != null ? weatherMotionCorrelation.optString("correlation_level", "") : "";
        if (!correlationLevel.isBlank() && !"low".equalsIgnoreCase(correlationLevel) && !"neutral".equalsIgnoreCase(correlationLevel)) {
            insights.put(new JSONObject()
                .put("priority", "HIGH".equalsIgnoreCase(correlationLevel) || "strong".equalsIgnoreCase(correlationLevel) ? "HIGH" : "MEDIUM")
                .put("type", "weather_motion_correlation")
                .put("message", "Weather and camera signals appear linked today; align patrols and equipment checks with risky weather windows."));
        }

        if (heatStress) {
            insights.put(new JSONObject()
                .put("priority", "MEDIUM")
                .put("type", "heat_risk")
                .put("message", String.format("High heat/UV pattern detected (max %.1f°C, UV %.1f). Shift heavy outdoor operations to cooler hours.", todayWeather.tempMax, todayWeather.uvIndex)));
        }

        if (operationalLoopholes != null && !operationalLoopholes.isEmpty()) {
            int max = Math.min(operationalLoopholes.length(), 2);
            for (int i = 0; i < max; i++) {
                JSONObject loophole = operationalLoopholes.getJSONObject(i);
                insights.put(new JSONObject()
                    .put("priority", loophole.optString("severity", "MEDIUM"))
                    .put("type", "operational_loophole")
                    .put("message", loophole.optString("message", "Operational loophole detected.")));
            }
        }

        if (insights.isEmpty()) {
            insights.put(new JSONObject()
                .put("priority", "LOW")
                .put("type", "stable_day")
                .put("message", "No major cross-signal conflicts detected today. Continue normal operations with routine checks."));
        }

        return insights;
    }

    private JSONObject buildLiveSnapshot(DayWeatherData todayWeather, JSONArray patternInsights,
                                         JSONObject motionSummary, JSONObject maintenanceSummary) {
        int riskScore = 0;
        riskScore += Math.min(todayWeather.rainChance / 2, 35);
        riskScore += todayWeather.windMax >= 45 ? 20 : (todayWeather.windMax >= 30 ? 10 : 0);
        riskScore += todayWeather.uvIndex >= 8 ? 10 : 0;
        riskScore += Math.min((int) motionSummary.optLong("high_severity", 0) * 8, 24);
        riskScore += Math.min(maintenanceSummary.optInt("upcoming_7_days", 0) * 3, 12);
        riskScore = Math.max(0, Math.min(100, riskScore));

        String level;
        if (riskScore >= 70) {
            level = "HIGH";
        } else if (riskScore >= 40) {
            level = "MEDIUM";
        } else {
            level = "LOW";
        }

        String topAlert = patternInsights.getJSONObject(0).optString("message", "No specific alert.");
        return new JSONObject()
            .put("risk_score", riskScore)
            .put("risk_level", level)
            .put("top_alert", topAlert)
            .put("updated_at", LocalDateTime.now().toString());
    }

    private String formatLiveSnapshotText(JSONObject context) {
        JSONObject snapshot = context.optJSONObject("live_snapshot");
        JSONArray insights = context.optJSONArray("pattern_insights");
        JSONObject weather = context.optJSONObject("today_weather");
        JSONArray motionTimeline = context.optJSONArray("motion_timeline");
        JSONObject motionSummary = context.optJSONObject("motion_summary");
        JSONObject maintenanceSummary = context.optJSONObject("maintenance_summary");
        JSONArray loopholes = context.optJSONArray("operational_loopholes");

        StringBuilder builder = new StringBuilder();
        builder.append("Live Snapshot\n");
        if (snapshot != null) {
            builder.append("Risk Level: ").append(snapshot.optString("risk_level", "UNKNOWN"))
                .append(" (").append(snapshot.optInt("risk_score", 0)).append("/100)\n");
        }
        if (weather != null) {
            builder.append("Today: ").append(weather.optString("condition", "N/A"))
                .append(" | Rain ").append(weather.optInt("rain_chance_pct", 0)).append("%")
                .append(" | Wind ").append(String.format("%.1f", weather.optDouble("wind_max_kph", 0))).append(" kph")
                .append("\n");
        }
        if (insights != null) {
            int max = Math.min(insights.length(), 3);
            for (int i = 0; i < max; i++) {
                builder.append("- ").append(insights.getJSONObject(i).optString("message", "")).append("\n");
            }
        }

        if (maintenanceSummary != null) {
            builder.append("Maintenance today: ")
                .append(maintenanceSummary.optInt("today_count", 0))
                .append(" | Upcoming 7 days: ")
                .append(maintenanceSummary.optInt("upcoming_7_days", 0))
                .append("\n");
        }

        if (motionSummary != null) {
            builder.append("Motion recap (H/M/L): ")
                .append(motionSummary.optLong("high_severity", 0)).append("/")
                .append(motionSummary.optLong("medium_severity", 0)).append("/")
                .append(motionSummary.optLong("low_severity", 0))
                .append(" (total ")
                .append(motionSummary.optLong("all_severities_total", 0))
                .append(")\n");
        }

            if (loopholes != null) {
                builder.append("Loopholes flagged: ")
                .append(loopholes.length())
                .append("\n");
            }

        if (motionTimeline != null && !motionTimeline.isEmpty()) {
            JSONObject latest = motionTimeline.getJSONObject(0);
            String cameraName = latest.optString("camera_name", "Camera #" + latest.optInt("camera_id", 0));
            String severity = latest.optString("severity", "UNKNOWN");
            String time = latest.optString("time", "");
            builder.append("Latest motion: ").append(severity)
                .append(" at ").append(cameraName)
                .append(time.isBlank() ? "" : " (" + time + ")");
        }
        return builder.toString().trim();
    }

    private boolean isActive(Equipment equipment) {
        if (equipment == null || equipment.getStatus() == null) {
            return false;
        }
        String status = equipment.getStatus().toLowerCase(Locale.ROOT);
        return status.contains("active") || status.contains("operational") || status.contains("ready");
    }

    private boolean isHeavyFieldVehicle(Equipment equipment) {
        String name = equipment != null && equipment.getName() != null ? equipment.getName().toLowerCase(Locale.ROOT) : "";
        String type = equipment != null && equipment.getType() != null ? equipment.getType().toLowerCase(Locale.ROOT) : "";
        return name.contains("tractor") || name.contains("camion") || name.contains("truck")
            || type.contains("tractor") || type.contains("camion") || type.contains("truck")
            || type.contains("harvester") || type.contains("vehicle");
    }

    private String safeName(Equipment equipment) {
        if (equipment == null) {
            return "Equipment";
        }
        if (equipment.getName() != null && !equipment.getName().isBlank()) {
            return equipment.getName();
        }
        return "Equipment #" + (equipment.getId() != null ? equipment.getId() : "?");
    }

    private void setError(String message) {
        Platform.runLater(() -> {
            locationLabel.setText("Weather Forecast");
            updatedLabel.setText(message);
        });
    }

    private void setAiMeta(String message) {
        Platform.runLater(() -> {
            if (aiBriefingMetaLabel != null) {
                aiBriefingMetaLabel.setText(message);
            }
        });
    }

    private String buildAiMetaSummary(JSONObject context) {
        JSONObject live = context.optJSONObject("live_snapshot");
        JSONArray patterns = context.optJSONArray("pattern_insights");
        JSONArray loopholes = context.optJSONArray("operational_loopholes");
        JSONObject corr = context.optJSONObject("weather_motion_correlation");

        String risk = live != null ? live.optString("risk_level", "UNKNOWN") : "UNKNOWN";
        int score = live != null ? live.optInt("risk_score", 0) : 0;
        int patternCount = patterns != null ? patterns.length() : 0;
        int loopholeCount = loopholes != null ? loopholes.length() : 0;
        String correlation = corr != null ? corr.optString("correlation_level", "n/a") : "n/a";

        return String.format("Risk %s (%d/100) • Patterns %d • Loopholes %d • Weather-Motion %s",
            risk, score, patternCount, loopholeCount, correlation);
    }

    private void incrementJsonCount(JSONObject target, String key) {
        String safeKey = (key == null || key.isBlank()) ? "unknown" : key;
        target.put(safeKey, target.optInt(safeKey, 0) + 1);
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
