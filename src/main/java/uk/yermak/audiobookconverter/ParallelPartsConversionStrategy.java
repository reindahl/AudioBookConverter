package uk.yermak.audiobookconverter;

import com.freeipodsoftware.abc.Messages;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class ParallelPartsConversionStrategy extends AbstractConversionStrategy implements Runnable {

    private ExecutorService executorService = Executors.newWorkStealingPool();

    protected void startConversion() {
        executorService.execute(this);
    }

    public void run() {
        long jobId = System.currentTimeMillis();

        int totalParts = 3;
        MediaInfo maxMedia = maximiseEncodingParameters();

        try {
            for (int partNo = 1; partNo <= totalParts; partNo++) {
                long subJobId = jobId + partNo;
                String tempFile = Utils.getTmp(subJobId, 999999, ".m4b");
                encodePart(subJobId, tempFile, partNo, maxMedia, totalParts);
            }
        } finally {
            finilize();
            for (MediaInfo mediaInfo : media) {
                Utils.deleteQuietly(getTempFileName(jobId, mediaInfo.hashCode(), ".m4b"));
            }
        }
    }

    private void encodePart(long jobId, String tempFile, int partNo, MediaInfo maxMedia, int totalParts) {
        List<Future<ConverterOutput>> futures = new ArrayList<>();

        File fileListFile = null;
        File metaFile = null;

        try {
            fileListFile = prepareFiles(jobId);
            metaFile = prepareMeta(jobId);

            List<MediaInfo> prioritizedMedia = splitAndPrioritiseMedia(maxMedia, partNo, totalParts);
            for (MediaInfo mediaInfo : prioritizedMedia) {
                String tempOutput = getTempFileName(jobId, mediaInfo.hashCode(), ".m4b");
                ProgressCallback callback = progressCallbacks.get(mediaInfo.getFileName());
                Future<ConverterOutput> converterFuture = executorService.submit(new FFMpegConverter(mediaInfo, tempOutput, callback));
                futures.add(converterFuture);
            }

            for (Future<ConverterOutput> future : futures) {
                if (canceled) return;
                future.get();
            }

            if (canceled) return;
            Concatenator concatenator = new FFMpegConcatenator(tempFile, metaFile.getAbsolutePath(), fileListFile.getAbsolutePath(), progressCallbacks.get("output"));
            concatenator.concat();

            if (canceled) return;
            Mp4v2ArtBuilder artBuilder = new Mp4v2ArtBuilder();
            artBuilder.coverArt(media, tempFile);

            if (canceled) return;
            String partName = outputDestination.replace(".m4b", " ,Part " + partNo + ".m4b");
            FileUtils.moveFile(new File(tempFile), new File(partName));

        } catch (InterruptedException | ExecutionException | IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            StateDispatcher.getInstance().finishedWithError(e.getMessage() + "; " + sw.getBuffer().toString());
        } finally {
            Utils.deleteQuietly(metaFile);
            Utils.deleteQuietly(fileListFile);
        }
    }

    //TODO Test it properly
    private List<MediaInfo> splitAndPrioritiseMedia(MediaInfo maxMedia, int partNo, int totalParts) {
        List<MediaInfo> sortedMedia = new ArrayList<>();
        int chunk = media.size() / totalParts;
        int fromIndex = (partNo - 1) * chunk;
        int toIndex = partNo == totalParts ? media.size() - 1 : partNo * chunk;
        List<MediaInfo> subMedia = media.subList(fromIndex, toIndex);
        for (MediaInfo mediaInfo : subMedia) {
            sortedMedia.add(mediaInfo);
            mediaInfo.setFrequency(maxMedia.getFrequency());
            mediaInfo.setChannels(maxMedia.getChannels());
            mediaInfo.setBitrate(maxMedia.getBitrate());
        }
        Collections.sort(sortedMedia, (o1, o2) -> (int) (o2.getDuration() - o1.getDuration()));
        return sortedMedia;
    }


    protected String getTempFileName(long jobId, int index, String extension, int partNo) {
        return Utils.getTmp(jobId, index, extension);
    }

    protected String getTempFileName(long jobId, int index, String extension) {
        return getTempFileName(jobId, index, extension, 0);
    }

    public String getAdditionalFinishedMessage() {
        return Messages.getString("JoiningConversionStrategy.outputFilename") + ":\n" + this.outputDestination;
    }

    @Override
    public void canceled() {
        canceled = true;
        Utils.closeSilently(executorService);
    }

    @Override
    public void setOutputDestination(String outputDestination) {
        if (new File(outputDestination).exists()) {
            this.outputDestination = Utils.makeFilenameUnique(outputDestination);
        } else {
            this.outputDestination = outputDestination;
        }
    }

    protected File prepareFiles(long jobId) throws IOException {
        File fileListFile = new File(System.getProperty("java.io.tmpdir"), "filelist." + jobId + ".txt");
        List<String> outFiles = media.stream().map(mediaInfo -> "file '" + getTempFileName(jobId, mediaInfo.hashCode(), ".m4b") + "'").collect(Collectors.toList());
        FileUtils.writeLines(fileListFile, "UTF-8", outFiles);
        return fileListFile;
    }
}
