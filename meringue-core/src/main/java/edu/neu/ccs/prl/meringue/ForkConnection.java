package edu.neu.ccs.prl.meringue;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public final class ForkConnection implements Closeable {
    private final Socket socket;
    private final ObjectInputStream ois;
    private final ObjectOutputStream oos;

    /**
     * Creates a new connection at the specified port on the loopback interface.
     *
     * @param port the port at which connection should be made
     * @throws IOException              if an I/O error occurs establishing the connection
     * @throws SecurityException        if a security manager exists and does not allow the connection
     * @throws IllegalArgumentException if the specified port value is invalid
     */
    public ForkConnection(int port) throws IOException {
        this(new Socket(InetAddress.getByName(null), port));
    }

    public ForkConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.oos = new ObjectOutputStream(socket.getOutputStream());
        // Flush the header for the ObjectOutputStream so that the ObjectInputStream on the other end can
        // read it and does not hang
        this.oos.flush();
        this.ois = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void close() {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            //
        }
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public void send(Object o) throws IOException {
        oos.writeObject(o);
        oos.flush();
    }

    public <T> T receive(Class<T> messageType) throws IOException, ClassNotFoundException {
        return messageType.cast(ois.readObject());
    }
}