package org.jmeterplugins.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

public class SafeDeleter {

    public static void main(String[] argsRaw) throws Throwable {
        ListIterator<String> args = Arrays.asList(argsRaw).listIterator();

        while (args.hasNext()) {
            String nextArg = args.next();
            if (nextArg.equalsIgnoreCase("--move-list")) {
                if (!args.hasNext()) {
                    throw new IllegalArgumentException("Missing delete list file name");
                }

                File fCopy = new File(args.next());
                moveFiles(fCopy);
                fCopy.delete();
            } else if (nextArg.equalsIgnoreCase("--install-list")) {
                if (!args.hasNext()) {
                    throw new IllegalArgumentException("Missing install list file");
                }

                File file = new File(args.next());
                installsFromFile(file);
                file.delete();
            } else if (nextArg.equalsIgnoreCase("--restart-command")) {
                if (!args.hasNext()) {
                    throw new IllegalArgumentException("Missing restart command file");
                }

                File file = new File(args.next());
                restartFromFile(file);
                file.delete();
            } else {
                throw new IllegalArgumentException("Unknown option: " + nextArg);
            }
        }
    }

    private static void installsFromFile(File file) throws IOException {
        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)) {
            File log = File.createTempFile("jpgc-installers-", ".log");
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length != 2) {
                    System.out.println("Invalid line: " + line);
                    continue;
                }

                String jar = parts[0];
                String cls = parts[1];

                final ArrayList<String> command = new ArrayList<>();
                command.add(getJVM());
                command.add("-classpath");
                command.add(jar);
                command.add(cls);

                final ProcessBuilder builder = new ProcessBuilder(command);
                System.out.print("Starting: " + command + "\n");
                builder.redirectError(log);
                builder.redirectOutput(log);
                Process p = builder.start();
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.out);
                }
            }
            System.out.println("Done running installers");
        }
    }

    private static void restartFromFile(File file) throws IOException {
        final ArrayList<String> command = new ArrayList<>();
        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                command.add(line);
            }

            final ProcessBuilder builder = new ProcessBuilder(command);
            System.out.print("Starting: " + command + "\n");
            File restarterLog = File.createTempFile("jpgc-restarter-", ".log");
            builder.redirectError(restarterLog);
            builder.redirectOutput(restarterLog);
            builder.start();
        }
    }

    private static void moveFiles(File list) throws IOException, InterruptedException {
        try (FileReader fr = new FileReader(list);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length != 2) {
                    System.out.println("Invalid line: " + line);
                    continue;
                }

                File src = new File(parts[0]);
                File dst = new File(parts[1]);

                System.out.println("Moving " + src + " to " + dst);

                if (!src.exists()) {
                    System.out.println("Cannot move, file not exists: " + src);
                }

                try {
                    Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    System.out.println("Cannot move " + src + " because of " + e.toString());
                    e.printStackTrace(System.out);
                }
            }
            System.out.println("Done moving files");
        }
    }

    public static String getJVM() {
        String jvm_location;
        if (System.getProperty("os.name").startsWith("Win")) {
            jvm_location = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
        } else {
            jvm_location = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        }
        return jvm_location;
    }


}
