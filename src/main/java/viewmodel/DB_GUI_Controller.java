package viewmodel;

import dao.DbConnectivityClass;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Major;
import model.Person;
import service.MyLogger;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DB_GUI_Controller implements Initializable {

    private static final String FIRST_NAME_REGEX = "^[A-Za-z'-]{2,25}$";
    private static final String LAST_NAME_REGEX  = "^[A-Za-z'-]{2,25}$";
    private static final String DEPARTMENT_REGEX = "^[A-Za-z &-]{2,50}$";
    private static final String EMAIL_REGEX      = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,6}$";
    private static final String IMAGE_URL_REGEX  = "^(https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}([-a-zA-Z0-9()@:%_+.~#?&/=]*))?$";

    @FXML
    TextField first_name, last_name, department, email, imageURL;
    @FXML
    ComboBox<Major> major;
    @FXML
    private Button addBtn, editBtn, deleteBtn;
    @FXML
    private MenuItem editItem, deleteItem;
    @FXML
    private Label statusLabel, countLabel;
    @FXML
    ImageView img_view;
    @FXML
    MenuBar menuBar;
    @FXML
    private TableView<Person> tv;
    @FXML
    private TableColumn<Person, Integer> tv_id;
    @FXML
    private TableColumn<Person, String> tv_fn, tv_ln, tv_department, tv_major, tv_email;
    private final DbConnectivityClass cnUtil = new DbConnectivityClass();
    private final ObservableList<Person> data = cnUtil.getData();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            tv_id.setCellValueFactory(new PropertyValueFactory<>("id"));
            tv_fn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
            tv_ln.setCellValueFactory(new PropertyValueFactory<>("lastName"));
            tv_department.setCellValueFactory(new PropertyValueFactory<>("department"));
            tv_major.setCellValueFactory(new PropertyValueFactory<>("major"));
            tv_email.setCellValueFactory(new PropertyValueFactory<>("email"));
            tv.setItems(data);
            major.setItems(FXCollections.observableArrayList(Major.values()));
            addBtn.setDisable(true);
            editBtn.setDisable(true);
            deleteBtn.setDisable(true);
            editItem.setDisable(true);
            deleteItem.setDisable(true);
            first_name.textProperty().addListener((obs, o, n) -> validateForm());
            last_name.textProperty().addListener((obs, o, n)  -> validateForm());
            department.textProperty().addListener((obs, o, n) -> validateForm());
            email.textProperty().addListener((obs, o, n)      -> validateForm());
            imageURL.textProperty().addListener((obs, o, n)   -> validateForm());
            major.valueProperty().addListener((obs, o, n)     -> validateForm());
            data.addListener((javafx.collections.ListChangeListener<Person>) c -> updateCount());
            updateCount();
            tv.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                boolean selected = n != null;
                editBtn.setDisable(!selected);
                deleteBtn.setDisable(!selected);
                editItem.setDisable(!selected);
                deleteItem.setDisable(!selected);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void validateForm() {
        boolean fnOk  = first_name.getText().matches(FIRST_NAME_REGEX);
        boolean lnOk  = last_name.getText().matches(LAST_NAME_REGEX);
        boolean deptOk = department.getText().matches(DEPARTMENT_REGEX);
        boolean majOk = major.getValue() != null;
        boolean emOk  = email.getText().matches(EMAIL_REGEX);
        boolean urlOk  = imageURL.getText().matches(IMAGE_URL_REGEX);

        setFieldError(first_name, !fnOk);
        setFieldError(last_name,  !lnOk);
        setFieldError(department, !deptOk);
        setFieldError(email,      !emOk);
        setFieldError(imageURL,   !urlOk);

        addBtn.setDisable(!(fnOk && lnOk && deptOk && majOk && emOk && urlOk));
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void updateCount() {
        countLabel.setText("Records: " + data.size());
    }

    private void setFieldError(TextField field, boolean hasError) {
        if (hasError) {
            if (!field.getStyleClass().contains("error")) field.getStyleClass().add("error");
        } else {
            field.getStyleClass().remove("error");
        }
    }

    @FXML
    protected void addNewRecord() {
        Person p = new Person(first_name.getText(), last_name.getText(), department.getText(),
                major.getValue().name(), email.getText(), imageURL.getText());
        cnUtil.insertUser(p);
        cnUtil.retrieveId(p);
        p.setId(cnUtil.retrieveId(p));
        data.add(p);
        clearForm();
        setStatus("Record added successfully for " + p.getFirstName() + " " + p.getLastName() + ".");
    }

    @FXML
    protected void clearForm() {
        first_name.setText("");
        last_name.setText("");
        department.setText("");
        major.getSelectionModel().clearSelection();
        email.setText("");
        imageURL.setText("");
        for (TextField f : new TextField[]{first_name, last_name, department, email, imageURL})
            f.getStyleClass().remove("error");
        addBtn.setDisable(true);
    }

    @FXML
    protected void logOut(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").getFile());
            Stage window = (Stage) menuBar.getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void closeApplication() {
        System.exit(0);
    }

    @FXML
    protected void displayAbout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/about.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(root, 600, 500);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void editRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();
        int index = data.indexOf(p);
        Person p2 = new Person(index + 1, first_name.getText(), last_name.getText(), department.getText(),
                major.getValue().name(), email.getText(), imageURL.getText());
        cnUtil.editUser(p.getId(), p2);
        data.remove(p);
        data.add(index, p2);
        tv.getSelectionModel().select(index);
        setStatus("Record updated for " + p2.getFirstName() + " " + p2.getLastName() + ".");
    }

    @FXML
    protected void deleteRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Record");
        confirm.setHeaderText("Delete " + p.getFirstName() + " " + p.getLastName() + "?");
        confirm.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        int index = data.indexOf(p);
        cnUtil.deleteRecord(p);
        data.remove(index);
        tv.getSelectionModel().select(index);
        setStatus("Record deleted for " + p.getFirstName() + " " + p.getLastName() + ".");
    }

    @FXML
    protected void showImage() {
        File file = (new FileChooser()).showOpenDialog(img_view.getScene().getWindow());
        if (file != null) {
            img_view.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    protected void addRecord() {
        showSomeone();
    }

    @FXML
    protected void selectedItemTV(MouseEvent mouseEvent) {
        Person p = tv.getSelectionModel().getSelectedItem();
        if (p == null) return;
        if (mouseEvent.getClickCount() == 2) {
            first_name.setText(p.getFirstName());
            last_name.setText(p.getLastName());
            department.setText(p.getDepartment());
            try {
                major.setValue(Major.valueOf(p.getMajor()));
            } catch (IllegalArgumentException e) {
                major.getSelectionModel().clearSelection();
            }
            email.setText(p.getEmail());
            imageURL.setText(p.getImageURL());
        }
    }

    public void lightTheme(ActionEvent actionEvent) {
        try {
            Scene scene = menuBar.getScene();
            Stage stage = (Stage) scene.getWindow();
            stage.getScene().getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
            System.out.println("light " + scene.getStylesheets());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void darkTheme(ActionEvent actionEvent) {
        try {
            Stage stage = (Stage) menuBar.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/darkTheme.css").toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void generatePDFReport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save PDF Report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName("report_by_major.pdf");
        File file = chooser.showSaveDialog(menuBar.getScene().getWindow());
        if (file == null) return;

        Map<String, Long> countByMajor = data.stream()
                .collect(Collectors.groupingBy(Person::getMajor, Collectors.counting()));

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            PDType1Font bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 720;

                cs.beginText();
                cs.setFont(bold, 20);
                cs.newLineAtOffset(72, y);
                cs.showText("Jayden Student Registry - Report by Major");
                cs.endText();
                y -= 24;

                cs.beginText();
                cs.setFont(regular, 11);
                cs.newLineAtOffset(72, y);
                cs.showText("Generated: " + LocalDate.now());
                cs.endText();
                y -= 30;

                cs.beginText();
                cs.setFont(bold, 13);
                cs.newLineAtOffset(72, y);
                cs.showText(String.format("%-20s %s", "Major", "Students"));
                cs.endText();
                y -= 6;

                cs.moveTo(72, y);
                cs.lineTo(400, y);
                cs.stroke();
                y -= 18;

                for (Map.Entry<String, Long> entry : countByMajor.entrySet()) {
                    cs.beginText();
                    cs.setFont(regular, 12);
                    cs.newLineAtOffset(72, y);
                    cs.showText(String.format("%-20s %d", entry.getKey(), entry.getValue()));
                    cs.endText();
                    y -= 18;
                }

                y -= 10;
                cs.moveTo(72, y);
                cs.lineTo(400, y);
                cs.stroke();
                y -= 18;

                cs.beginText();
                cs.setFont(bold, 12);
                cs.newLineAtOffset(72, y);
                cs.showText(String.format("%-20s %d", "Total", data.size()));
                cs.endText();
            }

            doc.save(file);
            setStatus("PDF report saved to " + file.getName() + ".");
        } catch (IOException e) {
            setStatus("PDF generation failed: " + e.getMessage());
        }
    }

    @FXML
    protected void importCSV() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showOpenDialog(menuBar.getScene().getWindow());
        if (file == null) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;
            int imported = 0;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                String[] parts = line.split(",", -1);
                if (parts.length < 6) continue;
                Person p = new Person(parts[0].trim(), parts[1].trim(), parts[2].trim(),
                        parts[3].trim(), parts[4].trim(), parts[5].trim());
                cnUtil.insertUser(p);
                p.setId(cnUtil.retrieveId(p));
                data.add(p);
                imported++;
            }
            setStatus("Imported " + imported + " record(s) from " + file.getName() + ".");
        } catch (IOException e) {
            setStatus("Import failed: " + e.getMessage());
        }
    }

    @FXML
    protected void exportCSV() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("students.csv");
        File file = chooser.showSaveDialog(menuBar.getScene().getWindow());
        if (file == null) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("First Name,Last Name,Department,Major,Email,Image URL");
            for (Person p : data) {
                pw.printf("%s,%s,%s,%s,%s,%s%n",
                        p.getFirstName(), p.getLastName(), p.getDepartment(),
                        p.getMajor(), p.getEmail(), p.getImageURL());
            }
            setStatus("Exported " + data.size() + " record(s) to " + file.getName() + ".");
        } catch (IOException e) {
            setStatus("Export failed: " + e.getMessage());
        }
    }

    public void showSomeone() {
        Dialog<Results> dialog = new Dialog<>();
        dialog.setTitle("New User");
        dialog.setHeaderText("Please specify…");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField textField1 = new TextField("Name");
        TextField textField2 = new TextField("Last Name");
        TextField textField3 = new TextField("Email ");
        ObservableList<Major> options =
                FXCollections.observableArrayList(Major.values());
        ComboBox<Major> comboBox = new ComboBox<>(options);
        comboBox.getSelectionModel().selectFirst();
        dialogPane.setContent(new VBox(8, textField1, textField2,textField3, comboBox));
        Platform.runLater(textField1::requestFocus);
        dialog.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK) {
                return new Results(textField1.getText(),
                        textField2.getText(), comboBox.getValue());
            }
            return null;
        });
        Optional<Results> optionalResult = dialog.showAndWait();
        optionalResult.ifPresent((Results results) -> {
            MyLogger.makeLog(
                    results.fname + " " + results.lname + " " + results.major);
        });
    }

    private static class Results {
        String fname;
        String lname;
        Major major;

        public Results(String name, String date, Major venue) {
            this.fname = name;
            this.lname = date;
            this.major = venue;
        }
    }

}