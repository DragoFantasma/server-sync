package me.client;

public class ApplicationParams {

    public static final String S_PROGRAM_NAME = "ClientDownloader";
    public static final int S_ARGUMENTS_REVERSED = 1;
    public static String S_JAR_NAME;
    public static boolean S_APPLICATION_SILENT = false;

    static {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace ();
        StackTraceElement main = stack[stack.length - 1];
        S_JAR_NAME = main.getClassName();
    }

}
