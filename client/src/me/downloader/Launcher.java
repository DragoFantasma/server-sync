package me.downloader;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;

public class Launcher {

    private static byte[] getMods(File parent) throws IOException {
        /* We don't need to check that parent exists, we already did in main */

        StringBuilder sb = new StringBuilder();

        File[] files = parent.listFiles();
        if (files == null) {
            System.out.println("Failed to retrieve file in " + parent.getAbsolutePath());
            return null;
        }

        for (File f : files)
            sb.append(f.getName()).append(",");

        return sb.toString().getBytes();
    }

    public static int getContentLength(byte[] data) {
        int length = 0;
        for (int i = 0; i < data.length; ++i) {
            length += (data[i] & 255) << i * 8;
        }

        return length;
    }

    public static void main(String[] args) throws Exception {

        boolean clearModsFolder = findArguments(args, "--clear-mods-folder");
        boolean updateAll = findArguments(args, "--update-all");

        //String modsFolder = args.length < 1 ? "%appdata%/.minecraft/mods" : args[1];
        String modsFolder = args.length < 1 ? "/home/sthat/downloaded_mods/" : args[1];
        System.out.println("Mods folder: " + modsFolder);

        File targetDirectory = new File(modsFolder);

        final int port = 3750;
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("localhost", port));

        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

        if (!targetDirectory.exists()) {
            sendClientMessage(out, "NO_FOLDER");
            socket.close();
            return;
        }

        if (clearModsFolder && !clearModFolder(targetDirectory)) {
            System.err.println("Failed to empty " + targetDirectory.getAbsolutePath());
            return;
        }

        byte[] mods;

        if (!updateAll) {
            try {
                mods = getMods(targetDirectory);
            } catch (IOException e) {
                e.printStackTrace();
                sendClientMessage(out, "NO_FOLDER");
                socket.close();
                return;
            }

            if (mods == null) {
                sendClientMessage(out, "NO_MODS");
                return;
            }
        } else {
            /*
             * If the user requests to update each mod, we send an empty mods list to the server.
             * The server will give us back each mod in the list
             */
            mods = new byte[1];
        }


        sendClientMessage(out, "FOLDER_OK");

        /* Make sure not to send an empty message if the mods folder is empty */
        if (mods.length == 0)
            mods = new byte[1];

        /* Sending mods info to the server */
        sendClientMessage(out, mods);


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
                ByteFormat byteFormat = getBytesFormat(bytesDownloaded);
                System.out.println("Sync completed. Downloaded " + new DecimalFormat("##.##").format(byteFormat.bytes) + " " + byteFormat.suffix + " in " + (e / 1000.0d) + "s");
                break;
            }

            byte[] bytes = in.readNBytes(read);

            String modName = new String(bytes);

            System.out.println("Downloading " + modName + "...");

            int contentLength = getContentLength(in.readNBytes(4));
            System.out.println("Content length: " + contentLength + " bytes");

            byte[] content = in.readNBytes(contentLength);

            bytesDownloaded += contentLength;

            saveFile(targetDirectory, modName, content);
        }

        socket.close();
    }

    public static void saveFile(File directory, String filename, byte[] content) throws IOException {
        File file = new File(directory, filename);
        if (!file.createNewFile()) {
            System.out.println(filename + " was downloaded but the file cannot be created. Please, check if " + directory.getAbsolutePath() + " still exists and the writing permissions");
            return;
        }

        FileOutputStream stream = new FileOutputStream(file);
        stream.write(content);
        stream.flush();
        stream.close();

        System.out.println("Download of " + filename + " completed");
    }

    public static void sendClientMessage(BufferedOutputStream stream, String message) {
        sendClientMessage(stream, message.getBytes(StandardCharsets.UTF_8));
    }

    public static void sendClientMessage(BufferedOutputStream stream, byte[] bytes) {
        try {
            stream.write(bytes);
            stream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean findArguments(String[] args, String arg) {
        for (String str : args) {
            if (str.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    public static boolean clearModFolder(File folder) {
        File[] list = folder.listFiles();
        if (list == null) return false;

        for (File f : list) {
            if (!f.delete()) {
                return false;
            }
        }

        return true;
    }

    public static ByteFormat getBytesFormat(int bytes) {
        double b = bytes;

        final String[] suffix = {
            "bytes",
            "KB",
            "MB",
            "GB"
        };

        int index = 0;
        while (b >= 1024) {
            ++index;
            b /= 1024;
        }

        if (index >= suffix.length) {
            index = 0;
            b = bytes;
        }

        return new ByteFormat(suffix[index], b);
    }

    private static class ByteFormat {

        public String suffix;
        public double bytes;

        public ByteFormat(String suffix, double bytes) {
            this.suffix = suffix;
            this.bytes = bytes;
        }

    }

}
