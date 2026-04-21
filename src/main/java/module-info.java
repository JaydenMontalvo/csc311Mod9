module com.example.csc311_db_ui_semesterlongproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.prefs;
    requires java.net.http;
    requires java.logging;
    requires com.google.gson;
    requires org.apache.pdfbox;

    opens viewmodel;
    exports viewmodel;
    opens dao;
    exports dao;
    opens model;
    exports model;
}
