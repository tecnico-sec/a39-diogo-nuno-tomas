package pt.ulisboa.tecnico;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pt.ulisboa.tecnico.auxTests.TestConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CheckTest {
    @TempDir
    static File tempFolder;
    static String tempPath;

    static Path secretKeyFile;

    @BeforeAll
    static void init() throws Exception {
        tempPath = tempFolder.getAbsolutePath() + "/";

        Path tempFile = Files.createFile(tempFolder.toPath().resolve(TestConfig.SOURCE_TEST_PATH_1));
        Files.write(tempFile, TestConfig.SOURCE_1_JSON.getBytes());

        secretKeyFile = Files.createFile(tempFolder.toPath().resolve(TestConfig.SECRET_KEY_TEST_PATH_1));
        Files.write(secretKeyFile, TestConfig.SECRET_KEY_1.getBytes());
    }

    @Test
    void checkTest() {
        String command = "protect " + tempPath + TestConfig.SOURCE_TEST_PATH_1 + " " +
                         tempPath + TestConfig.DEST_TEST_PATH_1 + "\n" +
                         "check " + tempPath + TestConfig.DEST_TEST_PATH_1 + "\n" +
                         "exit";

        ByteArrayInputStream is = new ByteArrayInputStream(command.getBytes());
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        System.setIn(is);
        System.setOut(new PrintStream(os));

        Cli.main(new String[] { secretKeyFile.toString() });

        assertTrue(Files.exists(Path.of(tempPath + TestConfig.DEST_TEST_PATH_1)));
        assertTrue(os.toString().contains("File protected"));
    }

    @Test
    void checkNonExistentFileTest() {
        String command = "check " + tempPath + TestConfig.NON_EXISTENT_FILE + "\n" +
                         "exit";

        ByteArrayInputStream is = new ByteArrayInputStream(command.getBytes());
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        System.setIn(is);
        System.setOut(new PrintStream(os));

        Cli.main(new String[] { secretKeyFile.toString() });

        String output = os.toString();
        assertTrue(output.contains("Could not check file"));
        assertTrue(output.contains("Error: Could not read file"));
    }

    @Test
    void checkMultipleFilesTest() {
        String command = "protect " + tempPath + TestConfig.SOURCE_TEST_PATH_1 + " " +
                         tempPath + TestConfig.DEST_TEST_PATH_1 + "\n" +
                         "check " + tempPath + TestConfig.DEST_TEST_PATH_1 + "\n" +
                         "check " + tempPath + TestConfig.DEST_TEST_PATH_1 + "\n" +
                         "check " + tempPath + TestConfig.DEST_TEST_PATH_1 + "\n" +
                         "exit";

        ByteArrayInputStream is = new ByteArrayInputStream(command.getBytes());
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        System.setIn(is);
        System.setOut(new PrintStream(os));

        Cli.main(new String[] { secretKeyFile.toString() });

        String output = os.toString();
        assertTrue(output.contains("File protected"));
    }

    @Test
    void checkWrongNumArgsTest() {
        String command = "check" + "\n" +
                         "exit";

        ByteArrayInputStream is = new ByteArrayInputStream(command.getBytes());
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        System.setIn(is);
        System.setOut(new PrintStream(os));

        Cli.main(new String[] { secretKeyFile.toString() });

        String output = os.toString();
        assertTrue(output.contains("Usage: (blingbank) check <input-file>"));
    }
}