import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.jbake.app.Oven;
import org.jbake.launcher.JettyServer;
import org.jbake.app.configuration.JBakeConfigurationFactory;
import org.jbake.app.configuration.JBakeConfiguration;
import java.nio.file.attribute.BasicFileAttributes;

import org.zakharchenko.taras.blogutils.*;

import java.nio.file.*;

public class Main {

    public static JBakeConfiguration getConfiguration(String source, String destination, boolean recompile) throws ConfigurationException {
        JBakeConfigurationFactory fucktory = new JBakeConfigurationFactory();
        JBakeConfiguration config = fucktory.createDefaultJbakeConfiguration(new File(source), new File(destination), recompile);

        return config;
    }

    private static void printHelp()
    {
        System.out.println("Available commands:");
        System.out.println("clean - delete destination repository");
        System.out.println("update - update/clone source repository, checkout/clone destination repo as well");
        System.out.println("compile - incremental render site");
        System.out.println("recompile - render site from scratch");
        System.out.println("push - commit changes and push them to server");
        System.out.println("run - start server with blog");
        System.out.println("Multiple commands allowed. Ordered as they printed here. Limited by common sense");
    }

    private static void cleanCommand()
    {
        Path directoryPath = Paths.get("blog_publish");

        try {
            Files.walkFileTree(directoryPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Cannot delete blog_publish");
            System.exit(-1);
        }
    }

    private static void updateCommand(BlogConfiguration config) {
        BlogSsh ssh = new BlogSsh(config);
        try {
            ssh.updateSourceRepo();
            ssh.updateDestinationRepo();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Update failed");
            System.exit(-1);
        }
    }

    private static void pushCommand(BlogConfiguration config) {
        BlogSsh ssh = new BlogSsh(config);
        try {
            ssh.pushSourceRepo();
            ssh.pushDestinationRepo();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Push failed");
            System.exit(-1);
        }
    }

    private static void compileCommand(boolean recompile) {
        JBakeConfiguration config = null;
        try {
            config = getConfiguration("blog", "blog_publish", recompile);
        } catch (ConfigurationException e) {
            System.err.println("Failed to read configuration");
            System.exit(-1);
        }

        if (config != null) {
            Oven oven = new Oven(config);
            oven.bake();
        }
    }

    private static void runCommand() {
        try {
            JettyServer jettyServer = new JettyServer();
            JBakeConfiguration config = getConfiguration("blog", "blog_publish", false);
            jettyServer.run("blog_publish", config);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to run server");
            System.exit(-1);
        }
    }

    private static void processCommand(BlogConfiguration config, String command)
    {
        System.out.print("Invoking command ");
        System.out.println(command);
        switch (command) {
            case "clean":
                cleanCommand();
                break;
            case "update":
                updateCommand(config);
                break;
            case "push":
                pushCommand(config);
                break;
            case "compile":
                compileCommand(false);
                break;
            case "recompile":
                compileCommand(true);
                break;
            case "run":
                runCommand();
                break;
            default:
                System.err.print("Unknown command: ");
                System.err.println(command);
                System.out.println("Skipping");
        }
    }

    public static void main(String[] args) {
        BlogConfiguration config = null;
        try {
            FileInputStream fs = new FileInputStream("config.xml");
            config = new BlogConfiguration(fs);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to read config.xml from the working directory");
            System.exit(-1);
        }


        if (args.length == 0) {
            System.err.println("Empty command list supplied");
            printHelp();
        } else {
            for(String command: args) {
                processCommand(config, command);
            }
        }
    }
}