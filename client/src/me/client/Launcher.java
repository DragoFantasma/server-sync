package me.client;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

public class Launcher {

    public static void main(String[] args) throws Exception {

        boolean clearModsFolder = findArguments(args, "--clear-mods-folder");
        boolean updateAll = findArguments(args, "--update-all");

        ApplicationParams.S_APPLICATION_SILENT = findArguments(args, "--silent");

        /* java -jar client.jar <host> [folder] */
        if (args.length < 1) {
            System.out.printf("Usage: java .jar [<jvm-args...>] %s <host> [folder] [<args...>]]%n", ApplicationParams.S_JAR_NAME);
            return;
        }

        String host = args[0];

        Pattern pattern = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
        if (!pattern.matcher(host).find()) {
            System.out.println("Invalid host");
            return;
        }

        String modsFolder = args.length < 2 || args[1].startsWith("--") ? IOUtils.getDefaultMinecraftDirectory() : args[1];

        if (!ApplicationParams.S_APPLICATION_SILENT)
            System.out.println("Mods folder: " + modsFolder);

        File targetDirectory = new File(modsFolder);

        if (!targetDirectory.exists()) {
            System.out.printf("Folder %s doesn't exists%n", modsFolder);
            return;
        }

        final int port = 3750;
        Socket socket = new Socket();

        try {
            if (!ApplicationParams.S_APPLICATION_SILENT)
                System.out.printf("Trying to connect to %s:%d%n", host, port);
            socket.connect(new InetSocketAddress(host, port));
        } catch (ConnectException e) {
            if (!ApplicationParams.S_APPLICATION_SILENT)
                System.err.println("Failed to connect to the server, is it up?");
            return;
        }

        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

        if (clearModsFolder) {
            if (!ApplicationParams.S_APPLICATION_SILENT)
                System.out.println("Clearing mod folder...");
            if (!IOUtils.clearModFolder(targetDirectory)) {
                if (!ApplicationParams.S_APPLICATION_SILENT)
                    System.err.printf("Failed to empty %s%n", targetDirectory.getAbsolutePath());
                return;
            }

            System.out.println("Done");
        }

        byte[] mods;

        if (!updateAll) {
            try {
                mods = IOUtils.getMods(targetDirectory);
            } catch (IOException e) {
                e.printStackTrace();
                DataStream.sendClientMessage(out, "NO_FOLDER");
                socket.close();
                return;
            }

            if (mods == null) {
                DataStream.sendClientMessage(out, "NO_MODS");
                return;
            }
        } else {
            /*
             * If the user requests to update each mod, we send an empty mods list to the server.
             * The server will give us back each mod in the list
             */
            mods = new byte[1];
        }


        DataStream.sendClientMessage(out, "FOLDER_OK");

        /* Make sure not to send an empty message if the mods folder is empty */
        if (mods.length == 0)
            mods = new byte[1];

        /* Sending mods info to the server */
        DataStream.sendClientMessage(out, mods);


        BufferedInputStream in = new BufferedInputStream(socket.getInputStream());

        int bytesDownloaded = 0;
        long start = System.currentTimeMillis();

        while (true) {
            int read = in.read();
            if (read <= 0) {
                /* Sync completed */
                if (bytesDownloaded == 0) {
                    if (!ApplicationParams.S_APPLICATION_SILENT)
                        System.out.println("All mods are in sync with the server, no update needed");
                    break;
                }
                long e = System.currentTimeMillis() - start;
                ByteFormat byteFormat = ByteFormat.getBytesFormat(bytesDownloaded);
                DecimalFormat df = new DecimalFormat("##.##");
                if (!ApplicationParams.S_APPLICATION_SILENT)
                    System.out.printf("Sync completed. Downloaded %s %s in %fs%n", df.format(byteFormat.bytes), byteFormat.suffix, (e / 1000.0d));
                break;
            }

            bytesDownloaded += DataStream.download(in, read, targetDirectory);
        }

        socket.close();
    }

    public static boolean findArguments(String[] args, String arg) {
        for (int i = 0; i < args.length; ++i) {
            /* First argument must be the host */
            if (i < ApplicationParams.S_ARGUMENTS_REVERSED && args[i].startsWith("--")) {
                System.err.printf("Found argument \"%s\" at index %d while expecting a valid host%n", args[i], i);
                System.exit(1);
            }

            if (args[i].equalsIgnoreCase(arg)) {
                return true;
            }
        }

        return false;
    }
}
