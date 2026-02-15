module org.example.smartfarm {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens org.example.smartfarm to javafx.fxml;
    opens controllers to javafx.fxml;
    opens entity to javafx.base;

    exports org.example.smartfarm;
    exports controllers;
}
