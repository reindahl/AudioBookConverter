package uk.yermak.audiobookconverter;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.yermak.audiobookconverter.fx.ConversionProgress;

import java.util.LinkedList;

/**
 * Created by yermak on 06-Feb-18.
 */

//Mediator pattern implementation
public class ConversionContext {

    private LinkedList<Conversion> conversionQueue = new LinkedList<>();
    private Subscriber subscriber;
    private ObservableList<MediaInfo> selectedMedia = FXCollections.observableArrayList();

    private AudioBookInfo bookInfo = new AudioBookInfo();
    private ObservableList<MediaInfo> media = FXCollections.observableArrayList();
    private SimpleObjectProperty<ConversionMode> mode = new SimpleObjectProperty<>(ConversionMode.PARALLEL);

    public Conversion getConversion() {
        return conversionQueue.getLast();
    }

    public ConversionContext() {
    }

    public void setMode(ConversionMode mode) {
        getConversion().setMode(mode);
    }

    public void setBookInfo(AudioBookInfo bookInfo) {
        this.bookInfo = bookInfo;
    }

    public AudioBookInfo getBookInfo() {
        return bookInfo;
    }


    public SimpleObjectProperty<ConversionMode> getMode() {
        return mode;
    }

    public void startConversion(String outputDestination, ObservableList<MediaInfo> media) {
        Conversion conversion = new Conversion(media, bookInfo);


        //TODO make it lazy via feature
        long totalDuration = media.stream().mapToLong(MediaInfo::getDuration).sum();
        ConversionProgress conversionProgress = new ConversionProgress(media.size(), totalDuration);

        subscriber.addConversionProgress(conversionProgress);
        conversion.start(outputDestination, conversionProgress);
    }

    public void pauseConversion() {
        getConversion().pause();
    }

    public void stopConversion() {
        getConversion().stop();
    }

    public void subscribeForStart(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    public void finishedConversion() {
        getConversion().finished();
    }

    public void error(String message) {
        getConversion().error(message);
    }

    public void resumeConversion() {
        getConversion().resume();
    }

    public ObservableList<MediaInfo> getMedia() {
        return media;
    }

    public void setMedia(ObservableList<MediaInfo> media) {
        this.media = media;
    }

    public void addModeChangeListener(ChangeListener<ConversionMode> listener) {
        mode.addListener(listener);
    }

    public void setOutputParameters(OutputParameters params) {
        getConversion().setOutputParameters(params);
    }

    public OutputParameters getOutputParameters() {
        return getConversion().getOutputParameters();
    }

    public ObservableList<MediaInfo> getSelectedMedia() {
        return selectedMedia;
    }
}
