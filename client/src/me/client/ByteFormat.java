package me.client;

public class ByteFormat {


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


    public String suffix;
    public double bytes;

    private ByteFormat(String suffix, double bytes) {
        this.suffix = suffix;
        this.bytes = bytes;
    }

}
