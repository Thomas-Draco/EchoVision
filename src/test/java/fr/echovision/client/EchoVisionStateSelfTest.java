package fr.echovision.client;

public final class EchoVisionStateSelfTest {
    public static void main(String[] args) {
        if (EchoVisionState.isEnabled()) throw new AssertionError("default should be off");
        EchoVisionState.setEnabled(true);
        if (!EchoVisionState.isEnabled()) throw new AssertionError("on command");
        EchoVisionState.setEnabled(false);
        if (EchoVisionState.isEnabled()) throw new AssertionError("off command");
        System.out.println("EchoVisionState: 3 tests OK");
    }
}
