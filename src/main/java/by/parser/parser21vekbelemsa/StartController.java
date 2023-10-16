package by.parser.parser21vekbelemsa;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class StartController {

    @FXML
    private CheckBox cbMiu;

    @FXML
    private CheckBox cbNihonBaby;

    @FXML
    private CheckBox cbSenso;

    @FXML
    private CheckBox cbSensoBaby;

    @FXML
    private CheckBox cbSensoMed;

    @FXML
    private Button chooseButton;

    @FXML
    private Label labelProgress;

    @FXML
    private TextField pathToSaveFile;

    @FXML
    private ProgressBar pb;

    @FXML
    private Button startButton;

    @FXML
    void initialize(){
        File iniFile = new File(Const.CONFIG_FILE_NAME);
        if (!iniFile.exists()) {
            try {
                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(iniFile));
                writer.write(Const.DEFAULT_INI);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        setDefaultDisable();
    }

    @FXML
    void startApp(ActionEvent event) throws RuntimeException {
        if (!cbSenso.isSelected() && !cbSensoBaby.isSelected() && !cbMiu.isSelected() && !cbNihonBaby.isSelected() && !cbSensoMed.isSelected()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Выберите хотя бы один бренд!");
            alert.setTitle("Внимание!!!");
            alert.setHeaderText(null);
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/logo_21vek.by.png"))));
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) event.consume();
        } else {
            if (pathToSaveFile.getText().trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Не выбран путь к файлу и наименование файла.");
                alert.setTitle("Внимание!!!");
                alert.setHeaderText(null);
                Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/logo_21vek.by.png"))));
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) event.consume();
            } else {
                String path = choseFileRRC(event);
                if (!Objects.requireNonNull(path).isBlank()) {

                    setDisable();

                    ParseTask pt = getParseTask(getBrandsValue(), path);

                    Thread thread = new Thread(pt);
                    thread.setDaemon(true);
                    thread.start();

                    pb.progressProperty().bind(pt.progressProperty());
                    labelProgress.textProperty().bind(pt.messageProperty());
                }
            }
        }
    }

    @FXML
    void bntChoose(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Книга Excel", "*.xlsx", "*.xls"));
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            pathToSaveFile.setText(file.getAbsoluteFile().toString());
        } else {
            if (pathToSaveFile.getText().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Не выбран путь к файлу и наименование файла.");
                alert.setTitle("Внимание!!!");
                alert.setHeaderText(null);
                Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/logo_21vek.by.png"))));
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) event.consume();
            }
        }
    }

    private String choseFileRRC(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Книга Excel", "*.xlsx", "*.xls"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            return file.getAbsoluteFile().toString();
        } else {

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Файл с РРЦ не выбран. Повторите попытку.");
            alert.setTitle("Внимание!!!");
            alert.setHeaderText(null);
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/logo_21vek.by.png"))));
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) event.consume();

            return null;
        }
    }

    private ArrayList<String> getBrandsValue(){
        ArrayList<String> brandList = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new FileReader("config.ini"))) {
            String line = reader.readLine();
            while (line != null){
                if (line.contains("=")){
                    String trimmed = line.substring(line.indexOf("=") + 1).trim();
                    if (line.startsWith(Const.SENSO) && cbSenso.isSelected()){
                        brandList.add(trimmed);
                        line = reader.readLine();
                        continue;
                    }
                    if (line.startsWith(Const.SENSO_BABY) && cbSensoBaby.isSelected()) {
                        brandList.add(trimmed);
                        line = reader.readLine();
                        continue;
                    }
                    if (line.startsWith(Const.MIU) && cbMiu.isSelected()){
                        brandList.add(trimmed);
                        line = reader.readLine();
                        continue;
                    }
                    if (line.startsWith(Const.NIHON_BABY) && cbNihonBaby.isSelected()){
                        brandList.add(trimmed);
                        line = reader.readLine();
                        continue;
                    }
                    if (line.startsWith(Const.SENSO_MED) && cbSensoMed.isSelected()){
                        brandList.add(trimmed);
                    }
                }
                line = reader.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return brandList;
    }

    private ParseTask getParseTask(ArrayList<String> brandList, String pathToRRC) {
        ParseTask parseTask = new ParseTask(pathToSaveFile.getText().trim(), pathToRRC, brandList);

        parseTask.setOnSucceeded(myEvent -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Файл успешно сохранен по указанному пути: " + pathToSaveFile.getText().trim());
            alert.setTitle("Создание файла");
            alert.setHeaderText(null);
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/logo_21vek.by.png"))));
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) myEvent.consume();

            setDefaultDisable();
        });

        parseTask.setOnFailed(myEvent -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Работа прервана по причине возникновения ошибки. Для устранения обратитесь к разработчику.");
            alert.setTitle("Ошибка!!!");
            alert.setHeaderText(null);
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/logo_21vek.by.png"))));
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) myEvent.consume();

            setDefaultDisable();

        });
        return parseTask;
    }

    private void setDisable(){
        labelProgress.setVisible(true);
        pb.setVisible(true);

        cbSenso.setDisable(true);
        cbSensoBaby.setDisable(true);
        cbMiu.setDisable(true);
        cbNihonBaby.setDisable(true);
        cbSensoMed.setDisable(true);
        startButton.setDisable(true);
        chooseButton.setDisable(true);
    }

    private void setDefaultDisable(){
        try(BufferedReader reader = new BufferedReader(new FileReader(Const.CONFIG_FILE_NAME))){
            String line = reader.readLine();
            while (line != null) {
                if (line.contains("=")){
                    String trimmed = line.substring(line.indexOf("=") + 1).trim();
                    if (line.startsWith(Const.SENSO_MED) && !trimmed.isEmpty()) {
                        cbSensoMed.setDisable(false);
                        line = reader.readLine();
                        continue;
                    }
                    if (line.startsWith(Const.SENSO) && !trimmed.isEmpty()) {
                        cbSenso.setDisable(false);
                        line = reader.readLine();
                        continue;
                    }
                    if (line.startsWith(Const.SENSO_BABY) && !trimmed.isEmpty()) {
                        cbSensoBaby.setDisable(false);
                        line = reader.readLine();
                        continue;
                    }
                    if (line.startsWith(Const.NIHON_BABY) && !trimmed.isEmpty()) {
                        cbNihonBaby.setDisable(false);
                        line = reader.readLine();
                        continue;
                    }
                    if (line.startsWith(Const.MIU) && !trimmed.isEmpty()) {
                        cbMiu.setDisable(false);
                    }
                }
                line = reader.readLine();
            }

            labelProgress.setVisible(false);
            pb.setVisible(false);
            startButton.setDisable(false);
            chooseButton.setDisable(false);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
