package uk.yermak.audiobookconverter.fx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import uk.yermak.audiobookconverter.ConversionContext;

import static uk.yermak.audiobookconverter.ConversionMode.*;

/**
 * Created by Yermak on 04-Feb-18.
 */
public class ConversionModeController {

    @FXML
    public RadioButton parallel;
    @FXML
    public RadioButton batch;
    @FXML
    public RadioButton join;
    @FXML
    private ToggleGroup modeGroup;

    @FXML
    public void initialize() {
        ConversionContext context = ConverterApplication.getContext();
        context.addModeChangeListener((observable, oldValue, newValue) -> ConverterApplication.getContext().setMode(newValue));
        /*ConverterApplication.getContext().getConversion().addStatusChangeListener((observable, oldValue, newValue) -> {
            boolean disable = newValue.equals(ProgressStatus.IN_PROGRESS);
            parallel.setDisable(disable);
            batch.setDisable(disable);
            join.setDisable(disable);
        });*/
    }

    public void parallelMode(ActionEvent actionEvent) {
        ConverterApplication.getContext().getMode().set(PARALLEL);
    }

    public void batchMode(ActionEvent actionEvent) {
        ConverterApplication.getContext().getMode().set(BATCH);
    }

    public void joinMode(ActionEvent actionEvent) {
        ConverterApplication.getContext().getMode().set(SINGLE);
    }
}
