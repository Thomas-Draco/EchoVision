package fr.echovision.client;

public final class EchoVisionState {
    private static volatile boolean enabled;

    private EchoVisionState() {}

    public static boolean isEnabled() { return enabled; }

    public static void setEnabled(boolean value) { enabled = value; }
}
