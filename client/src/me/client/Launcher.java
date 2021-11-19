package me.client;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DecimalFormat;

public class Launcher {

    public static void main(String[] args) throws Exception {

        boolean clearModsFolder = findArguments(args, "--clear-mods-folder");
        boolean updateAll = findArguments(args, "--update-all");

        String modsFolder = args.length < 2 ? IOUtils.getDefaultMinecraftDirectory() : args[1];
        System.out.println("Mods folder: " + modsFolder);

        File targetDirectory = new File(modsFolder);

        final int port = 3750;
        Socket socket = new Socket();

        try {
            socket.connect(new InetSocketAddress("localhost", port));
        } catch (ConnectException e) {
            System.err.println("Failed to connect to the server, is it up?");
            return;
        }

        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

        if (!targetDirectory.exists()) {
            DataStream.sendClientMessage(out, "NO_FOLDER");
            socket.close();
            return;
        }

        if (clearModsFolder) {
            System.out.println("Clearing mod folder...");
            if (!IOUtils.clearModFolder(targetDirectory)) {
                System.err.println("Failed to empty " + targetDirectory.getAbsolutePath());
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
                    System.out.println("All mods are in sync with the server, no update needed");
                    break;
                }
                long e = System.currentTimeMillis() - start;
                ByteFormat byteFormat = ByteFormat.getBytesFormat(bytesDownloaded);
                System.out.println("Sync completed. Downloaded " + new DecimalFormat("##.##").format(byteFormat.bytes) + " " + byteFormat.suffix + " in " + (e / 1000.0d) + "s");
                break;
            }

            bytesDownloaded += DataStream.download(in, read, targetDirectory);
        }

        socket.close();
    }

    public static boolean findArguments(String[] args, String arg) {
        for (String str : args) {
            if (str.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }


}
