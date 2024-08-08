module com.risonna.scmdautomated {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires org.jsoup;
    requires java.desktop;
    requires org.json;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires pty4j;

    opens com.risonna.scmdautomated to javafx.fxml;
    exports com.risonna.scmdautomated;
    exports com.risonna.scmdautomated.controllers;
    opens com.risonna.scmdautomated.controllers to javafx.fxml;
}