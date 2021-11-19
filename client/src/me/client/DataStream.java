package me.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DataStream {

    public static int download(BufferedInputStream in, int bytesToRead, File targetDirectory) throws IOException {

        byte[] bytes = in.readNBytes(bytesToRead);
        String modName = new String(bytes);

        if (!ApplicationParams.S_APPLICATION_SILENT)
            System.out.printf("Downloading %s...", modName);

        int contentLength = decodeLengthBytes(in.readNBytes(4));

        if (!ApplicationParams.S_APPLICATION_SILENT)
            System.out.printf("Content length: %d bytes", contentLength);

        byte[] content = in.readNBytes(contentLength);

        IOUtils.saveFile(targetDirectory, modName, content);

        return contentLength;
    }

    public static int decodeLengthBytes(byte[] data) {
        int length = 0;
        for (int i = 0; i < data.length; ++i) {
            length += (data[i] & 255) << i * 8;
        }

        return length;
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

}
