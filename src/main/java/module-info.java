module com.example.agrisens360 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    //requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.sql;
   // requires com.example.agrisens360;

    opens com.example.agrisens360 to javafx.fxml;
    opens controllers to javafx.fxml;
    exports com.example.agrisens360;
    opens entity to javafx.base;
    exports controllers;

   // opens com.example.agrisens360.controllers to javafx.fxml;

}



