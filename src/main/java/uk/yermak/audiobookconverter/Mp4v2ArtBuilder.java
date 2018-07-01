package uk.yermak.audiobookconverter;

import org.apache.commons.io.FileUtils;
import uk.yermak.audiobookconverter.fx.ConverterApplication;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Yermak on 04-Jan-18.
 */
public class Mp4v2ArtBuilder {

    private static final String MP4ART = new File("external/x64/mp4art.exe").getAbsolutePath();
    private final StatusChangeListener listener;

    public Mp4v2ArtBuilder() {
        listener = new StatusChangeListener();
        ConverterApplication.getContext().getConversion().addStatusChangeListener(listener);
    }

    private Collection<File> findPictures(File dir) {
        return FileUtils.listFiles(dir, new String[]{"jpg", "jpeg", "png", "bmp"}, true);
    }


    public void coverArt(List<MediaInfo> media, String outputFileName) throws IOException, InterruptedException {
        Map<Long, String> posters = new HashMap<>();
        Set<String> tempPosters = new HashSet<>();

        searchForPosters(media, posters, tempPosters);

        try {
            int i = 0;
            for (String poster : posters.values()) {
                if (listener.isCancelled()) break;
                updateSinglePoster(poster, i++, outputFileName);
            }
        } finally {
            tempPosters.forEach(Utils::deleteQuietly);
        }
    }

    private void searchForPosters(List<MediaInfo> media, Map<Long, String> posters, Set<String> tempPosters) {
        Set<File> searchDirs = new HashSet<>();
        media.forEach(mi -> searchDirs.add(new File(mi.getFileName()).getParentFile()));

        searchDirs.forEach(d -> findPictures(d).forEach(f -> posters.putIfAbsent(Utils.checksumCRC32(f), f.getPath())));

        media.forEach(m -> {
            if (m.getArtWork() != null) {
                posters.putIfAbsent(m.getArtWork().getCrc32(), m.getArtWork().getFileName());
                tempPosters.add(m.getArtWork().getFileName());
            }
        });
    }

    public void updateSinglePoster(String poster, int index, String outputFileName) throws IOException, InterruptedException {
        Process process = null;
        try {
            ProcessBuilder artProcessBuilder = new ProcessBuilder(MP4ART,
                    "--art-index", String.valueOf(index),
                    "--add", "\"" + poster + "\"",
                    outputFileName);

//            artProcessBuilder.redirectErrorStream();
            process = artProcessBuilder.start();

            StreamCopier.copy(process.getInputStream(), System.out);
            StreamCopier.copy(process.getErrorStream(), System.err);
            boolean finished = false;
            while (!listener.isCancelled() && !finished) {
                finished = process.waitFor(500, TimeUnit.MILLISECONDS);
            }
        } finally {
            Utils.closeSilently(process);
        }
    }
}
