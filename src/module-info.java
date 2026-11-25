module BUKONEK {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;

    opens app to javafx.graphics, javafx.fxml;
}