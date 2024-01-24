module com.ajikhoji.maze {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;


    opens com.ajikhoji.maze to javafx.fxml;
    exports com.ajikhoji.maze;
}