package kr.co.voxelite.exception;

public class InitializeException extends VoxeliteException {

    public InitializeException(String cause) {
        super("Exception while initializing : " + cause);
    }
}
