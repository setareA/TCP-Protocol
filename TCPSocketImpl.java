import java.util.Random;
import java.io.*;

public class TCPSocketImpl extends TCPSocket {
    public EnhancedDatagramSocket socket;
    private String IP;
    private int PORT;
    private int SEQ_NO;
    private int ACK_NO;

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        this.log("Constructor Called");
        System.out.println(port);
        this.IP = ip;
        this.PORT = port;
        this.SEQ_NUM = 0;
        this.ACK_NUM = 0;
        this.socket= new EnhancedDatagramSocket(this.PORT);

    }

    public void log(String log_Data){
        System.out.println("TCPSocketImpl : " + log_Data);
    }

    @Override
    public void send(String pathToFile) throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void close() throws Exception {
        this.socket.close();
        //throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getSSThreshold() {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getWindowSize() {
        throw new RuntimeException("Not implemented!");
    }
}