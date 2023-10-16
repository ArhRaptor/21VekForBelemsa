module by.parser.parser21vekbelemsa {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.jsoup;
    requires org.apache.poi.ooxml;


    opens by.parser.parser21vekbelemsa to javafx.fxml;
    exports by.parser.parser21vekbelemsa;
}