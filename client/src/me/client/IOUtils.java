package me.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class IOUtils {

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

    public static byte[] getMods(File parent) throws IOException {
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

    public static String getDefaultMinecraftDirectory() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        if (osName.contains("windows")) {
            return "%appdata%/.minecraft/mods";
        } else if (osName.contains("linux")) {
            return System.getProperty("user.home") + "/.minecraft/mods";
        }

        throw new RuntimeException("Operating System is not supported");
    }

}
