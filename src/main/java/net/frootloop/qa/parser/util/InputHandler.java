package net.frootloop.qa.parser.util;

import net.frootloop.qa.parser.util.files.FilePathHandler;
import net.frootloop.qa.parser.util.files.GitGudder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public interface InputHandler extends GitGudder, FilePathHandler {

    enum RequestType {
        ANALYSE_SOURCE_FILE,
        ANALYSE_GIT_REPO,
        PRINT_SOURCE_FILE_CONTENTS,
        PRINT_AMOUNT_COMMITS
    }

    static void wait(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (Exception e) {
            // Do nothing.
        }
    }

    static String readInputLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static RequestType promptWelcome() {

        System.out.println("[ JAVA BOOTLEG SOFTWARE METRICS ]" +
                "\nWelcome to JBSM, my software metrics project and homework on steroids that genuinely " +
                "\nparses and analyses the quality of Java source code. The goal is to measure complexity, " +
                "\nmaintainability and more of uncompiled Java source files and repositories, and to do it " +
                "\nall from scratch.");

        System.out.println("\nPlease select an option:" +
                "\n1 - Parse and analyse a local Java source file" +
                "\n2 - Parse and analyse a local Java git repository" +
                "\n3 - Print the parsed contents of a local Java source file" +
                "\n4 - Print the amounts of commits made to a local Java source file");

        String input = InputHandler.readInputLine();
        while(true) {
            if (input.equals("1")) return RequestType.ANALYSE_SOURCE_FILE;
            if (input.equals("2")) return RequestType.ANALYSE_GIT_REPO;
            if (input.equals("3")) return RequestType.PRINT_SOURCE_FILE_CONTENTS;
            if (input.equals("4")) return RequestType.PRINT_AMOUNT_COMMITS;

            System.out.println("Please select an option between either 1 and 4.\r");
            input = InputHandler.readInputLine();
        }
    }

    static Path promptForDirectoryToStartLookingFrom() {

        // Ask the user from where we need to start looking for repositories:
        System.out.println("\n[ INPUT EXPECTED ]\nFrom where should I start looking?");

        // Generate a list of up to 5 options:
        Path rootToStartFrom = FilePathHandler.getWorkingDirectory();
        int numOptions;
        for(numOptions = 1; numOptions < 5; numOptions++) {
            System.out.println(numOptions + " - \'" + rootToStartFrom + "\'");
            if(rootToStartFrom.getParent() == null) break;
            if(rootToStartFrom.getParent().getParent() == null) rootToStartFrom = rootToStartFrom.getParent();
            else rootToStartFrom = rootToStartFrom.getParent().getParent();
        }

        // Read and validate what the user inputted;
        String input = InputHandler.readInputLine();
        while(!input.matches("[1-" + numOptions + "]")) {
            System.out.println("Please select an option between 1 and " + numOptions + ".");
            input = InputHandler.readInputLine();
        }

        // We now have a slightly more narrow path to search through:
        rootToStartFrom = FilePathHandler.getWorkingDirectory();
        for(int i = 1; i < Integer.parseInt(input); i++) {
            if(rootToStartFrom.getParent() == null) break;
            if(rootToStartFrom.getParent().getParent() == null) rootToStartFrom = rootToStartFrom.getParent();
            else rootToStartFrom = rootToStartFrom.getParent().getParent();
        }
        return rootToStartFrom;
    }

    static Path promptForSourceFilePath() {

        InputHandler.wait(100);
        System.out.println("\n[ INPUT EXPECTED ]\nPlease enter either the name or file path of a .java file you wish to analyze:");
        String input = InputHandler.readInputLine();

        // Step 1: Check if the given input is valid
        input = input.replaceAll("\"","");
        while(!input.matches("((C:\\\\)?([^\\\\]+\\\\)*[^\\\\]+\\\\|(\\.)?(\\/[^\\/]+)*\\/)?([^\\\\\\/#%&\\{\\}\\<\\>\\*\\$\\!\\'\\+\\|\\=]+)")) {
            System.out.println("The file name you've entered, \'" + input + "\', doesn't seem valid. \nPlease try again.");
            input = InputHandler.readInputLine();
        }

        // Step 2: Check if file exists
        File file = new File(input);
        if(input.endsWith(".java") && file.exists() && !file.isDirectory()) {
            System.out.println("Java source file found at location: \'" + input + "\'");
            return Path.of(input);
        }

        String fileName = input.replaceAll("((C:\\\\)?([^\\\\]+\\\\)*[^\\\\]+\\\\|(\\.)?(\\/[^\\/]+)*\\/)", "");
        fileName = fileName.replaceAll("\\.\\w+$", "");
        System.out.println("No Java source file found at location: \'" + input + "\'");
        System.out.println("Searching instead for '.java' files with similar names to: \'" + fileName + "\'");


        // Step 3: If not, prompt for directory to look for:
        Path rootToStartFrom = InputHandler.promptForDirectoryToStartLookingFrom();

        // Step 4: List all similar
        ArrayList<Path> candidates = FilePathHandler.getPathsToFile(fileName, rootToStartFrom);
        if(candidates.size() == 0) {
            System.out.println("\nSorry, I couldn't find anything matching the input you've given me. Make sure to check your capitalization and spaces.\nLet's try again.");
            return InputHandler.promptForSourceFilePath();
        }
        else if(candidates.size() == 1) {
            System.out.println("\nI've found the following file: \'" + candidates.get(0) + "\'");
            return candidates.get(0);
        }
        else {
            System.out.println("\nPlease select which of these files you wish to parse by typing the number associated.\n");
            int numOptions = Math.min(candidates.size(), 9);
            for (int i = 0; i < numOptions; i++)
                System.out.println((i + 1) + " : \'" + candidates.get(i) + "\'");

            input = InputHandler.readInputLine();
            while (!input.matches("[1-" + numOptions + "]")) {
                System.out.println("Please enter a number between 1 and " + numOptions + ".");
                input = InputHandler.readInputLine();
            }

            Path selected = candidates.get(Integer.parseInt(input) - 1);
            System.out.println("Selected option " + Integer.parseInt(input) + ": \'" + selected + "\'");
            return  selected;
        }
    }

    static Path promptForGitRepositoryPath() {

        // Ask the user from where we need to start looking for repositories:
        Path rootToStartFrom = InputHandler.promptForDirectoryToStartLookingFrom();

        // Get a list of Java Git Repositories hidden in the given folder/directory:
        ArrayList<Path> candidates = GitGudder.getLocalGitRepositories(rootToStartFrom);
        if(candidates.size() == 0) {
            System.out.println("\nSorry, I couldn't find git repositories in the current working directory.");
            System.out.println("As a tip, we're simply looking for .java files in directories containing a \'\\.git\' subfolder and a \'\\src\' subfolder. If you have a codebase without these, make sure to create the necessary folders.");
            return null;
        }
        else if(candidates.size() == 1) {
            return candidates.get(0);
        }
        else {
            System.out.println("\nPlease select which of the following repositories you wish to parse by typing the number associated.\n");
            int numOptions = Math.min(candidates.size(), 9);
            for (int i = 0; i < numOptions; i++)
                System.out.println((i + 1) + " : \'" + candidates.get(i) + "\'");

            String input = InputHandler.readInputLine();
            while (!input.matches("[1-" + numOptions + "]")) {
                System.out.println("Please enter a number between 1 and " + numOptions + ".");
                input = InputHandler.readInputLine();
            }

            Path selected = candidates.get(Integer.parseInt(input) - 1);
            System.out.println("Selected option " + Integer.parseInt(input) + ": \'" + selected + "\'");
            return  selected;
        }
    }


    static Path promptForFileInRepository(Path repositoryPath) {

        InputHandler.wait(100);
        System.out.println("\nPlease enter either the name or file path of the .java file you wish to analyze:");
        String input = InputHandler.readInputLine();

        // Step 1: Check if the given input is valid
        input = input.replaceAll("\"","");
        while(!input.matches("((C:\\\\)?([^\\\\]+\\\\)*[^\\\\]+\\\\|(\\.)?(\\/[^\\/]+)*\\/)?([^\\\\\\/#%&\\{\\}\\<\\>\\*\\$\\!\\'\\+\\|\\=]+)")) {
            System.out.println("The file name you've entered, \'" + input + "\', doesn't seem valid. \nPlease try again.");
            input = InputHandler.readInputLine();
        }

        // Step 2: Check if file exists
        File file = new File(input);
        if(input.endsWith(".java") && file.exists() && !file.isDirectory()) {
            System.out.println("Java source file found at location: \'" + input + "\'");
            return Path.of(input);
        }

        // Step 3: List all similar
        String fileName = input.replaceAll("((C:\\\\)?([^\\\\]+\\\\)*[^\\\\]+\\\\|(\\.)?(\\/[^\\/]+)*\\/)", "");
        fileName = fileName.replaceAll("\\.\\w+$", "");
        System.out.println("No Java source file found at location: \'" + input + "\'");
        System.out.println("Searching instead for '.java' files with similar names to: \'" + fileName + "\'");

        ArrayList<Path> candidates = FilePathHandler.getPathsToFile(fileName, repositoryPath);
        if(candidates.size() == 0) {
            System.out.println("\nSorry, I couldn't find anything matching the input you've given me. Make sure to check your capitalization and spaces.\nLet's try again.");
            return InputHandler.promptForSourceFilePath();
        }
        else if(candidates.size() == 1) {
            System.out.println("\nI've found the following file: \'" + candidates.get(0) + "\'");
            return candidates.get(0);
        }
        else {
            System.out.println("\nPlease select which of these files you wish to parse by typing the number associated.\n");
            int numOptions = Math.min(candidates.size(), 9);
            for (int i = 0; i < numOptions; i++)
                System.out.println((i + 1) + " : \'" + candidates.get(i) + "\'");

            input = InputHandler.readInputLine();
            while (!input.matches("[1-" + numOptions + "]")) {
                System.out.println("Please enter a number between 1 and " + numOptions + ".");
                input = InputHandler.readInputLine();
            }

            Path selected = candidates.get(Integer.parseInt(input) - 1);
            System.out.println("Selected option " + Integer.parseInt(input) + ": \'" + selected + "\'");
            return  selected;
        }
    }
}
