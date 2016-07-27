/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool 
          with fuzzy matching, translation memory, keyword search, 
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2008 Alex Buloichik
               2009 Didier Briel
               2012 Alex Buloichik, Didier Briel
               2014 Alex Buloichik, Aaron Madlon-Kay
               2015-2016 Aaron Madlon-Kay
               Home page: http://www.omegat.org/
               Support center: http://groups.yahoo.com/group/OmegaT/

 This file is part of OmegaT.

 OmegaT is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 OmegaT is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.omegat.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Files processing utilities.
 * 
 * @author Alex Buloichik (alex73mail@gmail.com)
 * @author Didier Briel
 * @author Aaron Madlon-Kay
 */
public class FileUtil {
    public static String LINE_SEPARATOR = System.lineSeparator();
    public static long RENAME_RETRY_TIMEOUT = 3000;

    /**
     * Removes old backups so that only 10 last are there.
     */
    public static void removeOldBackups(final File originalFile, int maxBackups) {
        try {
            File[] bakFiles = originalFile.getParentFile().listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return !f.isDirectory() && f.getName().startsWith(originalFile.getName())
                            && f.getName().endsWith(OConsts.BACKUP_EXTENSION);
                }
            });

            if (bakFiles != null && bakFiles.length > maxBackups) {
                Arrays.sort(bakFiles, new Comparator<File>() {
                    public int compare(File f1, File f2) {
                        if (f2.lastModified() < f1.lastModified()) {
                            return -1;
                        } else if (f2.lastModified() > f1.lastModified()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
                for (int i = maxBackups; i < bakFiles.length; i++) {
                    bakFiles[i].delete();
                }
            }
        } catch (Exception e) {
            // we don't care
        }
    }

    /**
     * Create file backup with datetime suffix.
     */
    public static void backupFile(File f) throws IOException {
        long fileMillis = f.lastModified();
        String str = new SimpleDateFormat("yyyyMMddHHmm").format(new Date(fileMillis));
        FileUtils.copyFile(f, new File(f.getPath() + "." + str + OConsts.BACKUP_EXTENSION));
    }

    /**
     * Renames file, with checking errors and 3 seconds retry against external programs (like antivirus or
     * TortoiseSVN) locking.
     */
    public static void rename(File from, File to) throws IOException {
        if (!from.exists()) {
            throw new IOException("Source file to rename (" + from + ") doesn't exist");
        }
        if (to.exists()) {
            throw new IOException("Target file to rename (" + to + ") already exists");
        }
        long b = System.currentTimeMillis();
        while (!from.renameTo(to)) {
            long e = System.currentTimeMillis();
            if (e - b > RENAME_RETRY_TIMEOUT) {
                throw new IOException("Error renaming " + from + " to " + to);
            }
        }
    }

    /**
     * Copy file and create output directory if need. EOL will be converted into target-specific or into
     * platform-specific if target doesn't exist.
     */
    public static void copyFileWithEolConversion(File inFile, File outFile, String eolConversionCharset)
            throws IOException {
        File dir = outFile.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String eol;
        if (outFile.exists()) {
            // file exist - read EOL from file
            eol = getEOL(outFile, eolConversionCharset);
        } else {
            // file not exist - use system-dependent
            eol = LINE_SEPARATOR;
        }
        if (eol == null) {
            // EOL wasn't detected - just copy
            FileUtils.copyFile(inFile, outFile, false);
            return;
        }
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader in = null;
        try {
            fis = new FileInputStream(inFile);
            isr = new InputStreamReader(fis, eolConversionCharset);
            in = new BufferedReader(isr);
            FileOutputStream fos = null;
            OutputStreamWriter osw = null;
            BufferedWriter out = null;
            try {
                fos = new FileOutputStream(outFile);
                osw = new OutputStreamWriter(fos, eolConversionCharset);
                out = new BufferedWriter(osw);
                String s;
                while ((s = in.readLine()) != null) {
                    // copy using known EOL
                    out.write(s);
                    out.write(eol);
                }
                out.close();
                osw.close();
                fos.close();
            } finally {
                IOUtils.closeQuietly(out);
                IOUtils.closeQuietly(osw);
                IOUtils.closeQuietly(fos);
            }
            in.close();
            isr.close();
            fis.close();
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(isr);
            IOUtils.closeQuietly(fis);
        }
    }

    public static String getEOL(File file, String eolConversionCharset) throws IOException {
        String r = null;
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader in = null;
        try {
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis, eolConversionCharset);
            in = new BufferedReader(isr);
            while (true) {
                int ch = in.read();
                if (ch < 0) {
                    break;
                }
                if (ch == '\n' || ch == '\r') {
                    r = Character.toString((char) ch);
                    int ch2 = in.read();
                    if (ch2 == '\n' || ch2 == '\r') {
                        r += Character.toString((char) ch2);
                    }
                    break;
                }
            }
            in.close();
            isr.close();
            fis.close();
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(isr);
            IOUtils.closeQuietly(fis);
        }
        return r;
    }

    /**
     * Find files in subdirectories.
     * 
     * @param dir
     *            directory to start find
     * @param filter
     *            filter for found files
     * @return list of filtered found files
     */
    public static List<File> findFiles(final File dir, final FileFilter filter) {
        final List<File> result = new ArrayList<File>();
        Set<String> knownDirs = new HashSet<String>();
        findFiles(dir, filter, result, knownDirs);
        return result;
    }

    /**
     * Internal find method, which calls himself recursively.
     * 
     * @param dir
     *            directory to start find
     * @param filter
     *            filter for found files
     * @param result
     *            list of filtered found files
     */
    private static void findFiles(final File dir, final FileFilter filter, final List<File> result,
            final Set<String> knownDirs) {
        String curr_dir;
        try {
            // check for recursive
            curr_dir = dir.getCanonicalPath();
            if (!knownDirs.add(curr_dir)) {
                return;
            }
        } catch (IOException ex) {
            Log.log(ex);
            return;
        }
        File[] list = dir.listFiles();
        if (list != null) {
            for (File f : list) {
                if (f.isDirectory()) {
                    findFiles(f, filter, result, knownDirs);
                } else {
                    if (filter.accept(f)) {
                        result.add(f);
                    }
                }
            }
        }
    }

    /**
     * Compute relative path of file.
     * 
     * @param rootDir
     *            root directory
     * @param filePath
     *            file path
     * @return
     */
    public static String computeRelativePath(File rootDir, File file) throws IOException {
        String rootAbs = rootDir.getAbsolutePath().replace('\\', '/') + '/';
        String fileAbs = file.getAbsolutePath().replace('\\', '/');

        switch (Platform.getOsType()) {
        case WIN32:
        case WIN64:
            if (!fileAbs.toUpperCase().startsWith(rootAbs.toUpperCase())) {
                throw new IOException("File '" + file + "' is not under dir '" + rootDir + "'");
            }
            break;
        default:
            if (!fileAbs.startsWith(rootAbs)) {
                throw new IOException("File '" + file + "' is not under dir '" + rootDir + "'");
            }
            break;
        }
        return fileAbs.substring(rootAbs.length());
    }
    
    public interface ICollisionCallback {
        public boolean isCanceled();
        public boolean shouldReplace(File file, int thisFile, int totalFiles);
    }
    
    public interface ITreeIteratorCallback {
        public void processFile(File file) throws Exception;
    }

    /**
     * Copy a collection of files to a destination. Recursively copies contents of directories
     * while preserving relative paths. Provide an {@link ICollisionCallback} to determine
     * what to do with files with conflicting names; they will be overwritten if the callback is null.
     * @param destination Directory to copy to
     * @param toCopy Files to copy
     * @param onCollision Callback that determines what to do in case files with the same name
     * already exist
     * @throws IOException
     */
    public static void copyFilesTo(File destination, File[] toCopy, ICollisionCallback onCollision) throws IOException {
        if (destination.exists() && !destination.isDirectory()) {
            throw new IOException("Copy-to destination exists and is not a directory.");
        }
        Map<File, File> collisions = copyFilesTo(destination, toCopy, (File) null);
        if (collisions.isEmpty()) {
            return;
        }
        List<File> toReplace = new ArrayList<File>();
        List<File> toDelete = new ArrayList<File>();
        int count = 0;
        for (Entry<File, File> e : collisions.entrySet()) {
            if (onCollision != null && onCollision.isCanceled()) {
                break;
            }
            if (onCollision == null || onCollision.shouldReplace(e.getValue(), count, collisions.size())) {
                toReplace.add(e.getKey());
                toDelete.add(e.getValue());
            }
            count++;
        }
        if (onCollision == null || !onCollision.isCanceled()) {
            for (File file : toDelete) {
                FileUtils.deleteDirectory(file);
            }
            copyFilesTo(destination, toReplace.toArray(new File[toReplace.size()]), (File) null);
        }
    }
    
    private static Map<File, File> copyFilesTo(File destination, File[] toCopy, File root) throws IOException {
        Map<File, File> collisions = new LinkedHashMap<File, File>();
        for (File file : toCopy) {
            if (destination.getPath().startsWith(file.getPath())) {
                // Trying to copy something into its own subtree
                continue;
            }
            File thisRoot = root == null ? file.getParentFile() : root;
            String filePath = file.getPath();
            String relPath = filePath.substring(thisRoot.getPath().length(), filePath.length());
            File dest = new File(destination, relPath);
            if (file.equals(dest)) {
                // Trying to copy file to itself. Skip.
                continue;
            }
            if (dest.exists()) {
                collisions.put(file, dest);
                continue;
            }
            if (file.isDirectory()) {
                copyFilesTo(destination, file.listFiles(), thisRoot);
            } else {
                FileUtils.copyFile(file, dest);
            }
        }
        return collisions;
    }

    private static final Pattern RE_ABSOLUTE_WINDOWS = Pattern.compile("[A-Za-z]\\:(/.*)");
    private static final Pattern RE_ABSOLUTE_LINUX = Pattern.compile("/.*");

    /**
     * Checks if path starts with possible root on the Linux, MacOS, Windows.
     */
    public static boolean isRelative(String path) {
        path = path.replace('\\', '/');
        if (RE_ABSOLUTE_LINUX.matcher(path).matches()) {
            return false;
        } else if (RE_ABSOLUTE_WINDOWS.matcher(path).matches()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Converts Windows absolute path into current system's absolute path. It required for conversion like
     * 'C:\zzz' into '/zzz' for be real absolute in Linux.
     */
    public static String absoluteForSystem(String path, Platform.OsType currentOsType) {
        path = path.replace('\\', '/');
        Matcher m = RE_ABSOLUTE_WINDOWS.matcher(path);
        if (m.matches()) {
            if (currentOsType != Platform.OsType.WIN32 && currentOsType != Platform.OsType.WIN64) {
                // Windows' absolute file on non-Windows system
                return m.group(1);
            }
        }
        return path;
    }

    /**
     * Returns a list of all files under the root directory by absolute path.
     * 
     * @throws IOException
     */
    public static List<File> buildFileList(File rootDir, boolean recursive) throws IOException {
        int depth = recursive ? Integer.MAX_VALUE : 0;
        try (Stream<Path> stream = Files.find(rootDir.toPath(), depth, (p, attr) -> p.toFile().isFile(),
                FileVisitOption.FOLLOW_LINKS)) {
            return stream.map(Path::toFile).sorted(StreamUtil.localeComparator(File::getPath))
                    .collect(Collectors.toList());
        }
    }

    public static List<String> buildRelativeFilesList(File rootDir, List<String> includes, List<String> excludes)
            throws IOException {
        Path root = rootDir.toPath();
        Pattern[] includeMasks = FileUtil.compileFileMasks(includes);
        Pattern[] excludeMasks = FileUtil.compileFileMasks(excludes);
        BiPredicate<Path, BasicFileAttributes> pred = (p, attr) -> {
            return p.toFile().isFile() && FileUtil.checkFileInclude(root.relativize(p).toString(), includeMasks, excludeMasks);
        };
        try (Stream<Path> stream = Files.find(root, Integer.MAX_VALUE, pred, FileVisitOption.FOLLOW_LINKS)) {
            return stream.map(p -> root.relativize(p).toString().replace('\\', '/'))
                .sorted(StreamUtil.localeComparator(Function.identity()))
                .collect(Collectors.toList());
        }
    }

    public static boolean checkFileInclude(String filePath, Pattern[] includes, Pattern[] excludes) {
        String normalized = filePath.replace('\\', '/');
        String checkPath = normalized.startsWith("/") ? normalized : '/' + normalized;
        boolean included = Stream.of(includes).map(p -> p.matcher(checkPath)).anyMatch(Matcher::matches);
        boolean excluded = false;
        if (!included) {
            excluded = Stream.of(excludes).map(p -> p.matcher(checkPath)).anyMatch(Matcher::matches);
        }
        return included || !excluded;
    }

    static Pattern[] compileFileMasks(List<String> masks) {
        if (masks == null) {
            return FileUtil.NO_PATTERNS;
        }
        return masks.stream().map(FileUtil::compileFileMask).toArray(Pattern[]::new);
    }

    private static final Pattern[] NO_PATTERNS = new Pattern[0];

    static Pattern compileFileMask(String mask) {
        StringBuilder m = new StringBuilder();
        // "Relative" masks can match at any directory level
        if (!mask.startsWith("/")) {
            mask = "**/" + mask;
        }
        // Masks ending with a slash match everything in subtree
        if (mask.endsWith("/")) {
            mask += "**";
        }
        for (int cp, i = 0; i < mask.length(); i += Character.charCount(cp)) {
            cp = mask.codePointAt(i);
            if (cp >= 'A' && cp <= 'Z') {
                m.appendCodePoint(cp);
            } else if (cp >= 'a' && cp <= 'z') {
                m.appendCodePoint(cp);
            } else if (cp >= '0' && cp <= '9') {
                m.appendCodePoint(cp);
            } else if (cp == '/') {
                if (mask.regionMatches(i, "/**/", 0, 4)) {
                    // The sequence /**/ matches *zero* or more levels
                    m.append("(?:/|/.*/)");
                    i += 3;
                } else if (mask.regionMatches(i, "/**", 0, 3)) {
                    // The sequence /** matches *zero* or more levels
                    m.append("(?:|/.*)");
                    i += 2;
                } else {
                    m.appendCodePoint(cp);
                }
            } else if (cp == '?') {
                // ? matches anything but a directory separator
                m.append("[^/]");
            } else if (cp == '*') {
                if (mask.regionMatches(i, "**/", 0, 3)) {
                    // The sequence **/ matches *zero* or more levels
                    m.append("(?:|.*/)");
                    i += 2;
                } else if (mask.regionMatches(i, "**", 0, 2)) {
                    // **
                    m.append(".*");
                    i++;
                } else {
                    // *
                    m.append("[^/]*");
                }
            } else {
                m.append('\\').appendCodePoint(cp);
            }
        }
        return Pattern.compile(m.toString());
    }

    public static void iterateFileTree(File rootDir, boolean recursive, ITreeIteratorCallback cb) throws Exception {
        iterateFileTree(rootDir, recursive, new HashSet<File>(), cb);
    }

    private static void iterateFileTree(File rootDir, boolean recursive, Set<File> visited, ITreeIteratorCallback cb)
            throws Exception {
        if (!rootDir.isDirectory()) {
            return;
        }
        File canonical = rootDir.getCanonicalFile();
        if (visited.contains(canonical)) {
            return;
        }
        visited.add(canonical);
        for (File file : rootDir.listFiles()) {
            if (file.isDirectory() && recursive) {
                iterateFileTree(file.getAbsoluteFile(), recursive, visited, cb);
            }
            if (file.isFile()) {
                cb.processFile(file.getAbsoluteFile());
            }
        }
    }
}
