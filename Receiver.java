import java.io.IOException;
import java.net.DatagramPacket;

public class Receiver {
    public static void main(String[] args) throws Exception {
        TCPServerSocket tcpServerSocket = new TCPServerSocketImpl(1239);
        TCPSocket tcpSocket = tcpServerSocket.accept();
        tcpSocket.receive("test1.txt");
        tcpSocket.close();
        tcpServerSocket.close();
    }
}
