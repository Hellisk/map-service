package mapupdate.util.io;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.apache.log4j.Logger;

/**
 * A thread-safe service to manage files I/O locally.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public final class IOService implements Serializable {
    // system log
    private static Logger log =
            Logger.getLogger(IOService.class);

    /**
     * Create and save file to the given output directory.
     *
     * @param fileContent The content of the file.
     * @param outputPath  Path to output the file.
     * @param fileName    Name of the file, with extension.
     */
    public static synchronized void writeFile(
            final String fileContent,
            final String outputPath,
            final String fileName) {
        try {
            File file = new File(outputPath, fileName);
            FileWriter writer = new FileWriter(file);
            writer.append(fileContent);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.error("Error writing output file.", e);
        }
    }

    /**
     * Create and save file to the given output directory.
     * File content given as a collection of file lines.
     *
     * @param fileLines  File content. List of file lines.
     * @param outputPath Path to output the file.
     * @param fileName   Name of the file, with extension.
     */
    public static synchronized void writeFile(
            final Collection<String> fileLines,
            final String outputPath,
            final String fileName) {
        try {
            File file = new File(outputPath, fileName);
            FileWriter writer = new FileWriter(file);
            fileLines.forEach(line -> {
                try {
                    if (line.length() > 0) {
                        writer.append(line).append("\n");
                        writer.flush();
                    }
                } catch (IOException e) {
                    log.error("Error writing output file.", e);
                }
            });
            writer.close();
        } catch (IOException e) {
            log.error("Error writing output file.", e);
        }
    }

    /**
     * Create and save file to the given output directory.
     * File content given as file lines stream (useful files
     * with large content - number of lines)
     *
     * @param fileLinesStream File content. Stream of file lines.
     * @param outputPath      Path to output the file.
     * @param fileName        Name of the file, with extension.
     */
    public static synchronized void writeFile(
            final Stream<String> fileLinesStream,
            final String outputPath,
            final String fileName) {
        try {
            File file = new File(outputPath, fileName);
            FileWriter writer = new FileWriter(file);
            fileLinesStream.iterator().forEachRemaining(line -> {
                try {
                    if (line.length() > 0) {
                        writer.append(line).append("\n");
                        writer.flush();
                    }
                } catch (IOException e) {
                    log.error("Error writing output file.", e);
                }
            });
            writer.close();
        } catch (IOException e) {
            log.error("Error writing output file.", e);
        }
    }

    /**
     * Read the file in the given path name as a List of file lines.
     *
     * @param pathName The absolute path to the file to read.
     * @return A list with the file lines.
     */
    public static synchronized List<String> readFile(final String pathName) {
        return readFile(Paths.get(pathName).toAbsolutePath());
    }

    /**
     * Read the file in the given Path as a List of file lines.
     *
     * @param filePath Path to the file to read.
     * @return A list with the file lines.
     */
    private static synchronized List<String> readFile(final Path filePath) {
        return readFile(filePath.toFile());
    }

    /**
     * Read the given File as a List of file lines.
     *
     * @param file The file to read.
     * @return A list with the file lines.
     */
    private static synchronized List<String> readFile(final File file) {
        List<String> fileLines = new ArrayList<String>();
        BufferedReader bufferReader = null;
        try {
            bufferReader = new BufferedReader(
                    new FileReader(file));
            while (bufferReader.ready()) {
                // read lines
                fileLines.add(bufferReader.readLine());
            }
            bufferReader.close();
        } catch (IOException e) {
            log.error("Error reading input file.", e);
        } finally {
            close(bufferReader);
        }

        return fileLines;
    }

    /**
     * Read the file in the given path as a Stream of file lines.
     *
     * @param pathName The absolute path to the file to read.
     * @return A Stream with the file lines.
     */
    public static synchronized Stream<String> readFileAsStream(final String pathName) {
        Stream<String> linesStream = null;
        try {
            linesStream = Files.lines(Paths.get(pathName));
        } catch (IOException e) {
            log.error("Error reading input data files.", e);
        }
        return linesStream;
    }

    /**
     * Read the given file as a Stream of file lines.
     *
     * @param file The file to read.
     * @return A Stream with the file lines.
     */
    public static synchronized Stream<String> readFileAsStream(final File file) {
        Stream<String> linesStream = null;
        try {
            linesStream = Files.lines(file.toPath());
        } catch (IOException e) {
            log.error("Error reading input data files.", e);
        }
        return linesStream;
    }

    /**
     * Read the content of the file in the given Path.
     *
     * @param pathName The absolute path to the file to read.
     * @return The file content as a String.
     */
    public static synchronized String readFileContent(final String pathName) {
        StringBuilder fileContent = new StringBuilder();
        BufferedReader bufferReader = null;
        try {
            bufferReader = new BufferedReader(new FileReader(pathName));
            while (bufferReader.ready()) {
                // read lines
                fileContent.append(bufferReader.readLine()).append("\n");
            }
            bufferReader.close();
        } catch (IOException e) {
            log.error("Error reading input file.", e);
        } finally {
            close(bufferReader);
        }

        return fileContent.toString();
    }

    /**
     * Read the content of a file within the resources folder
     * of this project.
     * <p>
     * Use this method to read a file in the application 'resources'
     * folder (using the .jar).
     *
     * @param resourceName Name of the resource file (within the project build path),
     *                     e.g. "file.txt"
     * @return The resource file content as a String.
     */
    public static synchronized String readResourcesFileContent(final String resourceName) {
        StringBuilder fileContent = new StringBuilder();
        BufferedReader bufferReader = null;
        try {
            InputStream in = ClassLoader.getSystemResourceAsStream(resourceName);
            assert in != null;
            bufferReader = new BufferedReader(new InputStreamReader(in));
            while (bufferReader.ready()) {
                // read lines
                fileContent.append(bufferReader.readLine()).append("\n");
            }

            bufferReader.close();
        } catch (IOException e) {
            log.error("Error reading input file.", e);
        } finally {
            close(bufferReader);
        }

        return fileContent.toString();
    }

    /**
     * Returns a list with the absolute paths of all files in
     * the given path. Open directories recursively.
     *
     * @param path The path to the root directory to read.
     * @return A list with the files path.
     */
    private static synchronized List<String> getFilesPathList(final Path path) {
        List<String> filePathList = new ArrayList<String>();
        try {
            // a stream with the paths of all files and
            // folders in the given directory
            DirectoryStream<Path> pathStream =
                    Files.newDirectoryStream(path);
            for (Path currentPath : pathStream) {
                if (Files.isDirectory(currentPath, LinkOption.NOFOLLOW_LINKS)) {
                    // add in the list the path of files found in 'currentPath' folder
                    List<String> recFiles = getFilesPathList(currentPath);
                    filePathList.addAll(recFiles);
                } else {
                    // add in the list the file found in 'directory'
                    filePathList.add(currentPath.toString());
                }
            }
        } catch (IOException e) {
            log.error("Error opening input data directory.", e);
        }

        return filePathList;
    }

    /**
     * Return a stream with all the files in the given
     * directory path. Read files inside folders recursively.
     *
     * @param pathName The path to the root directory to read.
     * @return A Stream with all files in the given path.
     */
    static synchronized Stream<File> getFiles(final String pathName) {
        List<String> filePathList = getFilesPathList(Paths.get(pathName));

        Builder<File> fileStreamBuilder = Stream.builder();
        for (String path : filePathList) {
            fileStreamBuilder.accept(new File(path));
        }

        return fileStreamBuilder.build();
    }

    /**
     * Close the buffered reader..
     *
     * @param bufferReader
     */
    private static void close(BufferedReader bufferReader) {
        if (bufferReader != null) {
            try {
                bufferReader.close();
            } catch (Throwable e) {
                log.error("Error closing file service.", e);
            }
        }
    }

    /**
     * Return a stream with all the files in the given
     * directory path. Read files inside folders recursively.
     *
     * @param pathName The path to the root directory to read.
     * @param idSet    The set of ids which contained in the file.
     * @return A Stream with all files in the given path.
     */
    synchronized static Stream<File> getFilesWithIDs(String pathName, Set<String> idSet) {
        List<String> filePathList = getFilesPathList(Paths.get(pathName));

        Builder<File> fileStreamBuilder = Stream.builder();
        for (String path : filePathList) {
            String id = path.substring(path.lastIndexOf('_') + 1, path.indexOf('.'));
            if (idSet.contains(id))
                fileStreamBuilder.accept(new File(path));
        }

        return fileStreamBuilder.build();
    }
}
