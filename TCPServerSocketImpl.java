import java.net.*;
import java.io.*;

public class TCPServerSocketImpl extends TCPServerSocket {

	public EnhancedDatagramSocket socket;
	private static int ACK_NUM = 0;
	private static int SEQ_NUM = 0;
	private static int PORT;
	private int NumBytes = 1024;
	private static State STATE = State.NONE;
	private static boolean servingClient = false;  //Blocking 

    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
        this.PORT = port;
        this.socket = new EnhancedDatagramSocket(this.PORT);
        this.log("TCP: New socket created.");
    }

    @Override
    public TCPSocket accept() throws Exception {
        System.out.println("Accept");
        byte[] receivedData= new byte[NumBytes];
        while(!servingClient){
	        DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
	        this.socket.receive(receivedPacket);
	        String receivedString = new String(receivedPacket.getData());
	        InetAddress client_ip = receivedPacket.getAddress();
	        int client_port = receivedPacket.getPort();
	        System.out.println("Client port : " + client_port);
	        System.out.println("TCP Received : " + receivedString);
	        String[] receivedSplited = receivedString.split("\\s+");
	        System.out.println("After Parse");
	        System.out.println(receivedSplited[2]);
	        int receivedAckNum = Integer.parseInt(receivedSplited[2].trim());
	        int receivedSeqNum = Integer.parseInt(receivedSplited[1].trim());
	        //System.out.println("yoohoooo");
	        ///////
	        
	        if(this.STATE == State.NONE){
	        	if(receivedSplited[0].equals("SYN")){ //first packet must be a SYN packet
								this.log("3-way handshaking 1/3");
								this.ACK_NUM = receivedSeqNum + 1;
								String ACK_NUM_Str = Integer.toString(this.ACK_NUM);
								String SEQ_NUM_Str = Integer.toString(this.SEQ_NUM);
								String sendDataStr = "SYN-ACK" + " " + SEQ_NUM_Str + " "+ ACK_NUM_Str;
								byte[] sendDataBytes = new byte[NumBytes];
								sendDataBytes = sendDataStr.getBytes();

								DatagramPacket sendPacket = new DatagramPacket(sendDataBytes, sendDataBytes.length, client_ip, client_port);
								this.socket.send(sendPacket);

								this.STATE = State.SYN_RECV;
								this.log("State Change : SYN_RECV");
							}
	        }
	        else if(this.STATE == State.SYN_RECV){
	        	//System.out.println(this.SEQ_NUM + 1);
	        	//System.out.println(receivedAckNum);
	        	if(receivedSplited[0].equals("ACK") && (receivedAckNum == (this.SEQ_NUM + 1))){
	        		this.STATE = State.ESTABLISHED;
	        		servingClient = true;
	        		this.log("State Change : ESTABLISHED");
	       	    }
	       	}
	    }
	       	
        TCPSocketImpl tcpSocket = new TCPSocketImpl("127.0.0.1",1234);
        return tcpSocket;
    }

    public void log(String log_data){
    	System.out.println(log_data);
    }

    @Override
    public void close() throws Exception {
         this.socket.close();
    }

    }
