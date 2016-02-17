package jacz.util.files;

import jacz.util.lists.tuple.Duple;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Utility operations for copying, moving, etc files.
 * <p/>
 * todo use http://www.java7developer.com/blog/?p=334
 * <p/>
 * todo remove unnecessary methods that are available in apache commons
 * <p/>
 * todo move package to io
 */
public class FileUtil {

    public static final String FILE_EXTENSION_SEPARATOR = ".";

    public static final Character FILE_EXTENSION_SEPARATOR_CHAR = '.';

    private final static int[] ILLEGAL_FILENAME_CHARS = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};

    private final static int LEGAL_FILENAME_CHAR_REPLACEMENT = (int) '_';

    static {
        Arrays.sort(ILLEGAL_FILENAME_CHARS);
    }

    public static void copy(String sourceFile, String destinationFile) throws IOException {
        copy(sourceFile, destinationFile, true);
    }


    public static void copy(String sourcePath, String destinationPath, boolean overwrite) throws IOException {
        // sourcePath must point to an existing file, destinationPath can point to an existing directory.
        // an existing file, or can be the name of a file that does not exist yet, but whose parent is an
        // existing directory (we check this at the beginning)
        // if overwriting os not enabled, a FileAlreadyExists exception can be raised (if the destination file
        // already exists)
        // the transferTo method of the FileChannel class is used to copy the data. This method is supposed to perform
        // the copy of the data as optimally as it is possible
        String finalDestinationPath = checkCopyParameters(sourcePath, destinationPath, overwrite);
        File sourceFile = new File(sourcePath).getCanonicalFile();
        File destination = new File(finalDestinationPath);

        destination.createNewFile();
        FileChannel inChannel = new FileInputStream(sourceFile).getChannel();
        FileChannel outChannel = new FileOutputStream(destination).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    /**
     * Checks a series of parameters for a copy or move operation. It returns the path for the destination file.
     * It checks the source file exists and is a file and that the destination is either:
     * <ul>
     * <li>An existing file an overwrite is enabled (in which case the file will be erased and the same destination will be returned)</li>
     * <li>An existing directory (in which case the name of the source file will be added to this directory and returned)</li>
     * <li>An non existing file in an existing directory (in which case the same destination will be returned)</li>
     * </ul>
     *
     * @param sourcePath      source of the copy/move operation
     * @param destinationPath destination of the copy/move operation
     * @param overwrite       whether overwrite or not the destination file in case it exists
     * @return the path of the resulting destination file
     * @throws IOException the source file does not exist, the destination path is invalid (not any of the described
     *                     options, or the destination file cannot be deleted)
     */
    private static String checkCopyParameters(String sourcePath, String destinationPath, boolean overwrite) throws IOException {
        File source = new File(sourcePath).getAbsoluteFile();
        File destination = new File(destinationPath).getAbsoluteFile();
        if (!source.isFile()) {
            throw new FileNotFoundException("Cannot find file " + source.getPath());
        } else if (!destination.isFile() && destination.isDirectory()) {
            String destinationFileName = getFileName(sourcePath);
            return generatePath(destinationFileName, destinationPath);
        } else if (!destination.isFile() && !isDirectory(destination.getParent())) {
            throw new FileNotFoundException("Invalid path " + destination.getPath());
        } else if (!destination.isFile() && isDirectory(destination.getParent())) {
            return destinationPath;
        } else if (destination.isFile() && !overwrite) {
            throw new FileAlreadyExistsException("Destination file already exists " + destination.getPath());
        } else if (destination.isFile() && overwrite) {
            if (!destination.delete()) {
                throw new IOException("Cannot delete file " + destination.getPath());
            }
            return destinationPath;
        } else {
            // cannot reach here
            throw new RuntimeException("Unexpected state");
        }
    }

    public static void rename(String sourceFile, String destinationFile) throws IOException {
        rename(sourceFile, destinationFile, true);
    }

    public static void rename(String sourceFile, String destinationFile, boolean overwrite) throws IOException {
        File source = new File(sourceFile).getAbsoluteFile();
        File destination = new File(destinationFile).getAbsoluteFile();

        if (!source.isFile()) {
            throw new FileNotFoundException("Cannot find file " + source.getPath());
        } else if (destination.isDirectory()) {
            throw new FileNotFoundException("Specified file " + destination.getPath() + " is a directory");
        } else if (destination.isFile() && !overwrite) {
            throw new FileAlreadyExistsException("Destination file already exists " + destination.getPath());
        } else if (destination.isFile() && overwrite) {
            if (!destination.delete()) {
                throw new IOException("Cannot delete file " + destination.getPath());
            }
        }
        if (!source.renameTo(destination)) {
            throw new IOException("Cannot rename file " + source.getPath() + " to " + destination.getPath());
        }
    }

    public static void move(String sourcePath, String destinationPath) throws IOException {
        move(sourcePath, destinationPath, true);
    }

    public static void move(String sourcePath, String destinationPath, boolean overwrite) throws IOException {
        // first try to rename, then try to copy
        try {
            rename(sourcePath, destinationPath);
        } catch (Exception e) {
            copy(sourcePath, destinationPath, overwrite);
            if (!deleteFile(sourcePath)) {
                throw new IOException("Could not delete source file " + sourcePath);
            }
        }
    }

    public static boolean deleteFile(String path) throws FileNotFoundException {
        File file = new File(path);
        if (!file.isFile()) {
            throw new FileNotFoundException();
        }
        return file.delete();
    }

    public static boolean isFile(String path) {
        File file = new File(path);
        return file.isFile();
    }

    public static long getFileSize(String path) throws FileNotFoundException {
        if (isFile(path)) {
            File f = new File(path);
            return f.length();
        } else {
            throw new FileNotFoundException("path " + path + " does not correspond to a correct file");
        }
    }

    public static boolean isDirectory(String path) {
        File file = new File(path);
        return file.isDirectory();
    }

    public static void clearDirectory(String path) throws FileNotFoundException {
        String[] files = getDirectoryContents(path);
        if (files != null) {
            for (String f : files) {
                if (isDirectory(joinPaths(path, f))) {
                    clearDirectory(joinPaths(path, f));
                    new File(joinPaths(path, f)).delete();
                } else {
                    deleteFile(joinPaths(path, f));
                }
            }
        }
    }

    public static String[] getDirectoryContents(String path) throws FileNotFoundException {
        if (!isDirectory(path)) {
            throw new FileNotFoundException("Received path is not a directory: " + path);
        } else {
            File directoryFile = new File(path);
            return directoryFile.list();
        }
    }

    public static String getFileName(String path) {
        File file = new File(path);
        return file.getName();
    }

    public static String getFileNameWithoutExtension(String fileName) {
        if (extensionSeparatorIndex(fileName) >= 0) {
            return fileName.substring(0, extensionSeparatorIndex(fileName));
        } else {
            return null;
        }
    }

    public static String getFileExtension(String fileName) {
        if (extensionSeparatorIndex(fileName) >= 0) {
            return fileName.substring(extensionSeparatorIndex(fileName) + 1, fileName.length());
        } else {
            return null;
        }
    }

    private static int extensionSeparatorIndex(String path) {
        return path.lastIndexOf(FILE_EXTENSION_SEPARATOR_CHAR);
    }

    public static String getFileDirectory(String path) {
        String dir = getDirectoryParent(path);
        return dir == null ? "." : dir;
    }

    public static String getDirectoryParent(String path) {
        File d = new File(path);
        return d.getParent();
    }

    public static String joinPaths(String firstPath, String... additionalPaths) {
        if (additionalPaths.length == 0) {
            return firstPath;
        } else {
            File file = new File(firstPath);
            for (String additionalPath : additionalPaths) {
                file = new File(file, additionalPath);
            }
            return file.getPath();
        }
    }

    public static boolean isFileContainedIn(String filePath, String dirPath) throws IOException {
        String fileDir = getFileDirectory(filePath);
        return isDirectoryContainedIn(fileDir, dirPath);
    }

    public static boolean isDirectoryContainedIn(String containedDir, String containerDir) throws IOException {
        return pathFromDirectory(containerDir, containedDir) != null;
    }

    public static String pathFromDirectory(String sourceDir, String destDir) {
        if (sourceDir == null || destDir == null) {
            return null;
        } else {
            File sF = new File(sourceDir);
            File dF = new File(destDir);
            if (sF.equals(dF)) {
                return "";
            } else {
                String pathToDestParent = pathFromDirectory(sourceDir, getDirectoryParent(destDir));
                if (pathToDestParent == null) {
                    return null;
                } else {
                    return pathFromDirectory(sourceDir, getDirectoryParent(destDir)) +
                            File.separator +
                            getFileName(destDir);
                }
            }
        }

    }

    public static String generatePath(String fileName, String... dirs) throws IllegalArgumentException {
        if (dirs.length == 0) {
            return fileName;
        } else {
            boolean first = true;
            String totalDir = "";
            for (String dir : dirs) {
                if (dir.startsWith(File.separator)) {
                    dir = dir.substring(File.separator.length());
                }
                if (!dir.endsWith(File.separator)) {
                    dir = dir + File.separator;
                }
                if (first) {
                    first = false;
                } else {
                    if (new File(dir).isAbsolute()) {
                        throw new IllegalArgumentException("Middle directory cannot be absolute: " + dir);
                    }
                }
                totalDir += dir;
            }
            if (fileName.startsWith(File.separator)) {
                fileName = fileName.substring(File.separator.length());
            }
            return totalDir + fileName;
        }
    }

    public static boolean createDirectory(String path) throws IOException {
        boolean didNotExist = !isDirectory(path);
        // divide the simple directories and create them one by one
        StringTokenizer strTok = new StringTokenizer(path, File.separator);
        // if there is more than one directory (drive or current dir + at least one more directory) -> create them
        if (strTok.countTokens() > 1) {
            StringBuilder directory = new StringBuilder(strTok.nextToken());
            while (strTok.hasMoreTokens()) {
                directory.append(File.separator).append(strTok.nextToken());
                File f = new File(directory.toString());
                try {
                    f.mkdir();
                } catch (Exception e) {
                    throw new IOException("Could not create directory " + directory);
                }
            }
        }
        return didNotExist && isDirectory(path);
    }

    public static Duple<String, String> createDirectoryWithIndex(String containerDir, String baseDirName, String preIndex, String postIndex, boolean startWithoutIndex) throws IOException {
        if (!isDirectory(containerDir)) {
            throw new IOException(containerDir + " is not a valid directory");
        }
        int index = startWithoutIndex ? -1 : 0;
//        if (startWithoutIndex) {
//            String dirPath = generatePath(baseDirName, containerDir);
//            if (!isDirectory(dirPath)) {
//                createDirectory(dirPath);
//                return new Duple<>(dirPath, getFileName(dirPath));
//            }
//        }
        // we must generate a path with index (either required by user, or already exists without index)
        while (true) {
            String dirPath;
            if (index == -1) {
                dirPath = generatePath(baseDirName, containerDir);
            } else {
                dirPath = generatePath(baseDirName + preIndex + index + postIndex, containerDir);
            }
            if (!isDirectory(dirPath)) {
                createDirectory(dirPath);
                return new Duple<>(dirPath, getFileName(dirPath));
            } else {
                index++;
            }
        }
    }

    public static Duple<String, String> createFile(String dir, String baseFileName, String extension, boolean startWithoutIndex) throws IOException {
        return createFile(dir, baseFileName, extension, "", "", startWithoutIndex);
    }

    public static Duple<String, String> createFile(String dir, String baseFileName, String extension, String preIndex, String postIndex, boolean startWithoutIndex) throws IOException {
        List<String> baseFileNameList = new ArrayList<>(1);
        baseFileNameList.add(baseFileName);
        List<String> extensionList = new ArrayList<>(1);
        extensionList.add(extension);
        return createFiles(dir, baseFileNameList, extensionList, preIndex, postIndex, startWithoutIndex).get(0);
    }

    public static List<Duple<String, String>> createFiles(String dir, List<String> baseFileNameList, List<String> extensionList, boolean startWithoutIndex) throws IOException, IllegalArgumentException {
        return createFiles(dir, baseFileNameList, extensionList, "", "", startWithoutIndex);
    }

    public static List<Duple<String, String>> createFiles(String dir, List<String> baseFileNameList, List<String> extensionList, String preIndex, String postIndex, boolean startWithoutIndex) throws IOException, IllegalArgumentException {
        if (!isDirectory(dir)) {
            throw new IOException(dir + " is not a valid directory");
        }
        if (baseFileNameList.size() != extensionList.size()) {
            throw new IllegalArgumentException("Invalid lists");
        }
        for (int i = 0; i < extensionList.size(); i++) {
            if (extensionList.get(i).startsWith(FILE_EXTENSION_SEPARATOR)) {
                extensionList.set(i, extensionList.get(i).substring(1));
            }
        }
        int index = startWithoutIndex ? -1 : 0;
        List<Duple<String, String>> generatedPaths = new ArrayList<>(baseFileNameList.size());
//        if (startWithoutIndex) {
//            boolean allPathsGood = true;
//            for (int i = 0; i < baseFileNameList.size(); i++) {
//                String filePath = generatePath(baseFileNameList.get(i) + FILE_EXTENSION_SEPARATOR + extensionList.get(i), dir);
//                generatedPaths.add(filePath);
//                if (isFile(filePath)) {
//                    allPathsGood = false;
//                    break;
//                }
//            }
//            if (allPathsGood) {
//                for (String path : generatedPaths) {
//                    File file = new File(path);
//                    if (!file.createNewFile()) {
//                        throw new IOException("could not create file: " + path);
//                    }
//                }
//                return generatedPaths;
//            }
//        }
        while (true) {
            try {
                generatedPaths.clear();
                boolean allPathsGood = true;
                for (int i = 0; i < baseFileNameList.size(); i++) {
                    String filePath;
                    if (index == -1) {
                        filePath = generatePath(baseFileNameList.get(i) + FILE_EXTENSION_SEPARATOR + extensionList.get(i), dir);
                    } else {
                        filePath = generatePath(baseFileNameList.get(i) + preIndex + index + postIndex + FILE_EXTENSION_SEPARATOR + extensionList.get(i), dir);
                    }
//                    generatedPaths.add(filePath);
                    generatedPaths.add(new Duple<>(filePath, getFileName(filePath)));
                    if (isFile(filePath)) {
                        allPathsGood = false;
                        break;
                    }
                }
                if (allPathsGood) {
                    for (Duple<String, String> pathAndFile : generatedPaths) {
                        File file = new File(pathAndFile.element1);
                        if (!file.createNewFile()) {
                            throw new IOException("could not create file: " + pathAndFile.element1);
                        }
                    }
                    return generatedPaths;
                } else {
                    index++;
                }
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid dir: " + dir);
            }
        }
    }

    /**
     * Sanitizes a file name for any OS. Illegal characters are replaced by an underscore.
     * (algorithm copied from http://stackoverflow.com/questions/1155107/is-there-a-cross-platform-java-method-to-remove-filename-special-chars)
     *
     * @param filename original filename
     * @return sanitized filename
     */
    public static String sanitizeFilenameXPlatform(String filename) {
        StringBuilder cleanName = new StringBuilder();
        for (int i = 0; i < filename.length(); i++) {
            int c = (int) filename.charAt(i);
            if (Arrays.binarySearch(ILLEGAL_FILENAME_CHARS, c) < 0) {
                // legal char
                cleanName.append((char) c);
            } else {
                cleanName.append(LEGAL_FILENAME_CHAR_REPLACEMENT);
            }
        }
        return cleanName.toString();
    }

//    public static String createNonExistingFileNameWithIndex(String dir, String baseFileName, String extension, boolean startWithoutIndex) throws IOException {
//        String filePath = createNonExistingFilePathWithIndex(dir, baseFileName, extension, startWithoutIndex);
//        return getFileName(filePath);
//    }
//
//    public static String createNonExistingFileNameWithIndex(String dir, String baseFileName, String extension, String preIndex, String postIndex, boolean startWithoutIndex) throws IOException {
//        String filePath = createNonExistingFilePathWithIndex(dir, baseFileName, extension, preIndex, postIndex, startWithoutIndex);
//        return getFileName(filePath);
//    }
//
//    public static List<String> createNonExistingFileNameWithIndex(String dir, List<String> baseFileNameList, List<String> extensionList, boolean startWithoutIndex) throws IOException, IllegalArgumentException {
//        List<String> filePathList = createNonExistingFilePathWithIndex(dir, baseFileNameList, extensionList, startWithoutIndex);
//        for (int i = 0; i < filePathList.size(); i++) {
//            filePathList.set(i, getFileName(filePathList.get(i)));
//        }
//        return filePathList;
//    }
//
//    public static List<String> createNonExistingFileNameWithIndex(String dir, List<String> baseFileNameList, List<String> extensionList, String preIndex, String postIndex, boolean startWithoutIndex) throws IOException, IllegalArgumentException {
//        List<String> filePathList = createNonExistingFilePathWithIndex(dir, baseFileNameList, extensionList, preIndex, postIndex, startWithoutIndex);
//        for (int i = 0; i < filePathList.size(); i++) {
//            filePathList.set(i, getFileName(filePathList.get(i)));
//        }
//        return filePathList;
//    }


    public static void main(String args[]) {

        try {
            boolean is = isDirectoryContainedIn("G:\\", "G:\\");
            System.out.println(is);
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            System.out.println(createDirectory("D:\\a\\b\\d"));
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        String dir1 = "c:\\test";
        String dir2 = "d:\\\\TEST";

        File d1 = new File(dir1);
        File d2 = new File(dir2);
        if (d1.equals(d2)) {
            System.out.println("YES");
        } else {
            System.out.println("NO");
        }
        System.out.println(pathFromDirectory(dir1, dir2));
    }
}
