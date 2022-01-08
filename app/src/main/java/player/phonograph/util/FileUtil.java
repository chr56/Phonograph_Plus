package player.phonograph.util;

import android.content.Context;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import player.phonograph.database.mediastore.MusicDatabase;
import player.phonograph.database.mediastore.SongDao;
import player.phonograph.helper.SongModelConverterHelper;
import player.phonograph.helper.SortOrder;
import player.phonograph.model.Song;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public final class FileUtil {
    private FileUtil() {
    }

    @NonNull
    public static List<Song> matchFilesWithMediaStore(@NonNull Context context, @Nullable List<File> files) {
        SongDao dao = MusicDatabase.INSTANCE.getSongsDataBase().SongDao();
        String[] paths = toPathArray(files);

        List<Song> songs = null;
        if (paths != null) {
            songs = SongModelConverterHelper.convert(
                    dao.querySongByPath(paths, SortOrder.SongSortOrder.SONG_A_Z)
            );
        }
        if (songs != null)
            return songs;
        else
            return new ArrayList<Song>();

    }

    private static String makePlaceholders(int len) {
        StringBuilder sb = new StringBuilder(len * 2 - 1);
        sb.append("?");
        for (int i = 1; i < len; i++) {
            sb.append(",?");
        }
        return sb.toString();
    }

    @Nullable
    private static String[] toPathArray(@Nullable List<File> files) {
        if (files != null) {
            String[] paths = new String[files.size()];
            for (int i = 0; i < files.size(); i++) {
                paths[i] = safeGetCanonicalPath(files.get(i));
            }
            return paths;
        }
        return null;
    }

    @NonNull
    public static List<File> listFiles(@NonNull File directory, @Nullable FileFilter fileFilter) {
        List<File> fileList = new LinkedList<>();
        File[] found = directory.listFiles(fileFilter);
        if (found != null) {
            Collections.addAll(fileList, found);
        }
        return fileList;
    }

    @NonNull
    public static List<File> listFilesDeep(@NonNull File directory, @Nullable FileFilter fileFilter) {
        List<File> files = new LinkedList<>();
        internalListFilesDeep(files, directory, fileFilter);
        return files;
    }

    @NonNull
    public static List<File> listFilesDeep(@NonNull Collection<File> files, @Nullable FileFilter fileFilter) {
        List<File> resFiles = new LinkedList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                internalListFilesDeep(resFiles, file, fileFilter);
            } else if (fileFilter == null || fileFilter.accept(file)) {
                resFiles.add(file);
            }
        }
        return resFiles;
    }

    private static void internalListFilesDeep(@NonNull Collection<File> files, @NonNull File directory, @Nullable FileFilter fileFilter) {
        File[] found = directory.listFiles(fileFilter);

        if (found != null) {
            for (File file : found) {
                if (file.isDirectory()) {
                    internalListFilesDeep(files, file, fileFilter);
                } else {
                    files.add(file);
                }
            }
        }
    }

    public static boolean fileIsMimeType(File file, String mimeType, MimeTypeMap mimeTypeMap) {
        if (mimeType == null || mimeType.equals("*/*")) {
            return true;
        } else {
            // get the file mime type
            String filename = file.toURI().toString();
            int dotPos = filename.lastIndexOf('.');
            if (dotPos == -1) {
                return false;
            }
            String fileExtension = filename.substring(dotPos + 1).toLowerCase();
            String fileType = mimeTypeMap.getMimeTypeFromExtension(fileExtension);
            if (fileType == null) {
                return false;
            }
            // check the 'type/subtype' pattern
            if (fileType.equals(mimeType)) {
                return true;
            }
            // check the 'type/*' pattern
            int mimeTypeDelimiter = mimeType.lastIndexOf('/');
            if (mimeTypeDelimiter == -1) {
                return false;
            }
            String mimeTypeMainType = mimeType.substring(0, mimeTypeDelimiter);
            String mimeTypeSubtype = mimeType.substring(mimeTypeDelimiter + 1);
            if (!mimeTypeSubtype.equals("*")) {
                return false;
            }
            int fileTypeDelimiter = fileType.lastIndexOf('/');
            if (fileTypeDelimiter == -1) {
                return false;
            }
            String fileTypeMainType = fileType.substring(0, fileTypeDelimiter);
            return fileTypeMainType.equals(mimeTypeMainType);
        }
    }

    public static String stripExtension(String str) {
        if (str == null) return null;
        int pos = str.lastIndexOf('.');
        if (pos == -1) return str;
        return str.substring(0, pos);
    }

    public static String readFromStream(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    public static String read(File file) throws Exception {
        FileInputStream fin = new FileInputStream(file);
        String ret = readFromStream(fin);
        fin.close();
        return ret;
    }

    public static String safeGetCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            return file.getAbsolutePath();
        }
    }

    public static File safeGetCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
            return file.getAbsoluteFile();
        }
    }
}
