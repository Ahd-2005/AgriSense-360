package com.example.agrisense360.controllers;

import com.example.agrisense360.services.EquipmentStoreScraperService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class EquipmentStoreController implements Initializable {

    @FXML private TextField searchField;
    @FXML private TextField minPriceField;
    @FXML private TextField maxPriceField;
    @FXML private Button searchBtn;
    @FXML private Label storeStatusLabel;
    @FXML private TableView<StoreRow> storeTable;
    @FXML private TableColumn<StoreRow, String> marketCol;
    @FXML private TableColumn<StoreRow, String> titleCol;
    @FXML private TableColumn<StoreRow, String> priceCol;
    @FXML private TableColumn<StoreRow, String> sourceCol;
    @FXML private TableColumn<StoreRow, String> linkCol;

    private final EquipmentStoreScraperService scraperService = new EquipmentStoreScraperService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        marketCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().market()));
        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().title()));
        priceCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().price()));
        sourceCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().source()));
        linkCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().url()));

        titleCol.setCellFactory(column -> new TableCell<>() {
            private final Text text = new Text();
            {
                text.wrappingWidthProperty().bind(column.widthProperty().subtract(12));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    text.setText(item);
                    setGraphic(text);
                    setText(null);
                }
            }
        });

        linkCol.setCellFactory(column -> new TableCell<>() {
            private final Hyperlink link = new Hyperlink("Open Listing");

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                link.setOnAction(e -> openListing(item));
                link.setTooltip(new Tooltip(item));
                setGraphic(link);
                setText(null);
            }
        });

        if (searchField != null) {
            searchField.setOnAction(e -> onSearch());
        }
        if (searchBtn != null) {
            searchBtn.setOnAction(e -> onSearch());
        }

        storeTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                StoreRow selected = storeTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openListing(selected.url());
                }
            }
        });

        if (searchField != null) {
            searchField.setText("tractor");
        }
        storeTable.setFixedCellSize(74);
        setStatus("Search agri equipment from Agriaffaires, TractorHouse, Mascus, and Fastline. Examples: tractor, combine harvester, seed drill.");
    }

    @FXML
    private void backToEquipment() {
        MainController controller = MainController.getInstance();
        if (controller != null) {
            controller.showEquipment();
        }
    }

    @FXML
    private void onSearch() {
        String query = searchField != null && searchField.getText() != null ? searchField.getText().trim() : "";
        if (query.isBlank()) {
            setStatus("Enter an item to search (e.g., tractor, plow, sprayer).");
            return;
        }

        setStatus("Scraping real equipment marketplaces...");
        Thread worker = new Thread(() -> {
            List<EquipmentStoreScraperService.StoreListing> listings;
            try {
                listings = scraperService.search(query);
            } catch (Exception e) {
                listings = List.of();
            }

            List<EquipmentStoreScraperService.StoreListing> filtered = filterByPrice(listings,
                parsePriceOrNull(minPriceField != null ? minPriceField.getText() : ""),
                parsePriceOrNull(maxPriceField != null ? maxPriceField.getText() : ""));

            ObservableList<StoreRow> rows = FXCollections.observableArrayList();
            for (EquipmentStoreScraperService.StoreListing listing : filtered) {
                rows.add(new StoreRow(
                    listing.market(),
                    listing.title(),
                    listing.priceText(),
                    listing.url(),
                        listing.market() + " Listing"
                ));
            }

            Platform.runLater(() -> {
                storeTable.setItems(rows);
                setStatus(rows.isEmpty()
                    ? "No products found right now. Try: tractor, john deere, combine harvester, seed drill, sprayer."
                    : rows.size() + " product(s) found with name, price, and real market links.");
            });
        });
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void onOpenSelected() {
        StoreRow selected = storeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Select a listing first.");
            return;
        }
        openListing(selected.url());
    }

    private void openListing(String url) {
        if (url == null || url.isBlank()) {
            setStatus("Listing URL unavailable for this item.");
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
                setStatus("Opened listing in browser.");
            } else {
                setStatus("Desktop browser is not supported in this environment.");
            }
        } catch (Exception e) {
            setStatus("Failed to open listing: " + e.getMessage());
        }
    }

    private List<EquipmentStoreScraperService.StoreListing> filterByPrice(
        List<EquipmentStoreScraperService.StoreListing> listings, Double minPrice, Double maxPrice
    ) {
        if (listings == null || listings.isEmpty()) {
            return List.of();
        }
        List<EquipmentStoreScraperService.StoreListing> filtered = new ArrayList<>();
        for (EquipmentStoreScraperService.StoreListing listing : listings) {
            Double price = listing.priceValue();
            if (price == null) {
                if (minPrice == null && maxPrice == null) {
                    filtered.add(listing);
                }
                continue;
            }
            boolean validMin = minPrice == null || price >= minPrice;
            boolean validMax = maxPrice == null || price <= maxPrice;
            if (validMin && validMax) {
                filtered.add(listing);
            }
        }
        return filtered;
    }

    private Double parsePriceOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String clean = value.trim().replace(',', '.').replaceAll("[^0-9.]", "");
            if (clean.isBlank()) {
                return null;
            }
            return Double.parseDouble(clean);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void setStatus(String text) {
        if (storeStatusLabel != null) {
            storeStatusLabel.setText(text);
        }
    }

    public record StoreRow(String market, String title, String price, String url, String source) {
        public String market() {
            return market;
        }

        public String title() {
            return title;
        }

        public String price() {
            return price;
        }

        public String url() {
            return url;
        }

        public String source() {
            return source;
        }
    }
}