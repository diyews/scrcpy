package com.genymobile.scrcpy;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class DesktopConnection implements Closeable {

    private static final int DEVICE_NAME_FIELD_LENGTH = 64;

    private static final String SOCKET_NAME = "scrcpy";

    private final Socket videoSocket;
    private final FileDescriptor videoFd;

    private final Socket controlSocket;
    private final InputStream controlInputStream;
    private final OutputStream controlOutputStream;

    private final ControlMessageReader reader = new ControlMessageReader();
    private final DeviceMessageWriter writer = new DeviceMessageWriter();

    private DesktopConnection(Socket videoSocket, Socket controlSocket) throws IOException {
        this.videoSocket = videoSocket;
        this.controlSocket = controlSocket;
        controlInputStream = controlSocket.getInputStream();
        controlOutputStream = controlSocket.getOutputStream();
        videoFd = ParcelFileDescriptor.fromSocket(videoSocket).getFileDescriptor();
    }

    public static DesktopConnection open(Device device, boolean tunnelForward) throws IOException {
        Socket videoSocket;
        Socket controlSocket;
            ServerSocket localServerSocket = new ServerSocket(7007);
            try {
                videoSocket = localServerSocket.accept();
                // send one byte so the client may read() to detect a connection error
                videoSocket.getOutputStream().write(0);
                try {
                    controlSocket = localServerSocket.accept();
                } catch (IOException | RuntimeException e) {
                    videoSocket.close();
                    throw e;
                }
            } finally {
                localServerSocket.close();
            }

        DesktopConnection connection = new DesktopConnection(videoSocket, controlSocket);
        Size videoSize = device.getScreenInfo().getVideoSize();
        connection.send(Device.getDeviceName(), videoSize.getWidth(), videoSize.getHeight());
        return connection;
    }

    public void close() throws IOException {
        try {
            videoSocket.shutdownInput();
            videoSocket.shutdownOutput();
            videoSocket.close();
            controlSocket.shutdownInput();
            controlSocket.shutdownOutput();
            controlSocket.close();
        } catch (Throwable e) {
            Ln.i(e.toString());
        }
    }

    private void send(String deviceName, int width, int height) throws IOException {
        byte[] buffer = new byte[DEVICE_NAME_FIELD_LENGTH + 4];

        byte[] deviceNameBytes = deviceName.getBytes(StandardCharsets.UTF_8);
        int len = StringUtils.getUtf8TruncationIndex(deviceNameBytes, DEVICE_NAME_FIELD_LENGTH - 1);
        System.arraycopy(deviceNameBytes, 0, buffer, 0, len);
        // byte[] are always 0-initialized in java, no need to set '\0' explicitly

        buffer[DEVICE_NAME_FIELD_LENGTH] = (byte) (width >> 8);
        buffer[DEVICE_NAME_FIELD_LENGTH + 1] = (byte) width;
        buffer[DEVICE_NAME_FIELD_LENGTH + 2] = (byte) (height >> 8);
        buffer[DEVICE_NAME_FIELD_LENGTH + 3] = (byte) height;
        IO.writeFully(videoFd, buffer, 0, buffer.length);
    }

    public FileDescriptor getVideoFd() {
        return videoFd;
    }

    public ControlMessage receiveControlMessage() throws IOException {
        ControlMessage msg = reader.next();
        while (msg == null) {
            reader.readFrom(controlInputStream);
            msg = reader.next();
        }
        return msg;
    }

    public void sendDeviceMessage(DeviceMessage msg) throws IOException {
        writer.writeTo(msg, controlOutputStream);
    }
}
