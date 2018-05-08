import java.util.Random;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.file.*;


public class TCPSocketImpl extends TCPSocket {
    public EnhancedDatagramSocket socket;
    private String IP;
    private int PORT;
    private int SEQ_NUM = 0;
    private int ACK_NUM = 0;
    private int SERVER_PORT = 1239;
    private int SERVER_PORT_2 = 1236;
    private State STATE = State.NONE;
    private CngsState cngsState = CngsState.NONE;
    private int NumBytes = 1024;
    /* Congestion Control Variables */
    private int cwnd = 0;
    private int ssThresh = 0;
    private int lastAcked = 0;
    private int dupAckCount = 0;
    /* End */
    private int MSS = 1024;
    private int WINDOW_SIZE = 50;
    private int TIMER = 100;
    private int rcvBase = 0;
    private int firstUnAcked = 0;
    public static final double PROBABILITY  = 0.1;


    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        this.log("Constructor Called");
        System.out.println(port);
        this.IP = ip;
        this.PORT = port;
        this.cngsState = CngsState.SLOW_START;
        this.socket= new EnhancedDatagramSocket(this.PORT);
        this.log("Socket Created");
        // Congestion Control Initial Values
        cwnd = MSS ;
        ssThresh =  65536 ;
    }

    public void log(String log_Data){
        System.out.println("TCPSocketImpl : " + log_Data);
    }

    public void establishConnection(){

        try{
            this.log("Handshaking 0/3");
            InetAddress ip_addr = InetAddress.getByName(this.IP);
            byte[] sendDataBytes = new byte[NumBytes];
            String SEQ_NUM_Str= Integer.toString(this.SEQ_NUM);
            String ACK_NUM_Str = Integer.toString(this.ACK_NUM);
            String SendData_Str="SYN" + " "+ SEQ_NUM_Str + " " + ACK_NUM_Str;
            sendDataBytes = SendData_Str.getBytes();
            this.log("Sending" + SendData_Str);
            System.out.println("Here");
            DatagramPacket sendPacket = new DatagramPacket(sendDataBytes, sendDataBytes.length,ip_addr, SERVER_PORT);
            this.socket.send(sendPacket);
            this.STATE = State.SYN_SEND;
            this.log("State Change : SYN_SEND");

            boolean connection_completed = false;
            while(connection_completed == false){

                byte[] receivedData = new byte[NumBytes];
                DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
                this.socket.receive(receivedPacket);
                String receivedStr = new String(receivedPacket.getData());
                this.log("Received_Data "+ receivedStr);
                
                String[] received_splited = receivedStr.split("\\s+");
                int receivedSeqNum = Integer.parseInt(received_splited[1].trim());
                int receivedAckNum = Integer.parseInt(received_splited[2].trim());
                //System.out.println(receivedAckNum);
                //System.out.println(receivedSeqNum);

                if(this.STATE == State.SYN_SEND){
                    if(receivedAckNum == this.SEQ_NUM + 1 && received_splited[0].equals("SYN-ACK")){
                        this.log("3-way handshake 2/3");

                        this.SEQ_NUM = receivedAckNum;
                        this.ACK_NUM = receivedSeqNum + 1;
                        String SeqNumStr = Integer.toString(this.SEQ_NUM);
                        String AckNumStr = Integer.toString(this.ACK_NUM);
                        String sendDataStr = "ACK" + " " + SeqNumStr +" "+ AckNumStr;
                        sendDataBytes = sendDataStr.getBytes();
                        
                        sendPacket = new DatagramPacket(sendDataBytes, sendDataBytes.length, ip_addr, this.SERVER_PORT);
                        this.socket.send(sendPacket);
                        
                        this.STATE = State.ESTABLISHED;
                        this.log("State Change : ESTABLISHED");
                        connection_completed = true;
                    }
                }
            }

            if(this.STATE == State.ESTABLISHED){

                return;
            }
            }catch(Exception ex){
                System.out.println("Unknown Exception!!!!!!");
            }
    }

    @Override
    public void send(String pathToFile) throws Exception {
        this.log("Send Called");
        this.establishConnection();
        this.log("Start Sending...");

        byte[]  fileToSend = readFile(pathToFile);
        System.out.println(fileToSend.length);
        this.sendData(fileToSend);
    }

    @Override
    public void receive(String pathToFile) throws Exception {

        int waitingFor = 0;
        ArrayList<String> received = new ArrayList<String>();
        //boolean end = false;
        InetAddress ip_addr = InetAddress.getByName(this.IP);
        byte[] receivedData = new byte[NumBytes];
        
        while(true){
            
            this.log("Waiting for packet");
            // Receive packet
            DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
            this.socket.receive(receivedPacket);
            this.log("received a packet in receive");
            
            String receivedStr = new String(receivedPacket.getData());
            //this.log("Received_Data "+ receivedStr);

            
                
            String[] received_splited = receivedStr.split("\\s+");

            if(received_splited[0].equals("END")){
                this.log("Last packet received");
                break;
            }
            int receivedSeqNum = Integer.parseInt(received_splited[1].trim());

            String data = received_splited[2].trim();
            
            System.out.println("Packet with sequence number " + receivedSeqNum + " received.");
        
            if(receivedSeqNum == waitingFor){
                waitingFor++;
                received.add(data);
                //System.out.println("Packed stored in buffer");
            }else{
                System.out.println("Packet discarded (not in order)");
            }
            
            // Create an RDTAck object
            String ackStr = "ACK" + " " + waitingFor;
            byte[] sendDataBytes = ackStr.getBytes();
            
            DatagramPacket ackPacket = new DatagramPacket(sendDataBytes, sendDataBytes.length, ip_addr, 8088);
            
            // Send with some probability of loss
            if(Math.random() > PROBABILITY){
                this.socket.send(ackPacket);
            }else{
                System.out.println("[X] Lost ack with sequence number " + waitingFor);
            }
            
            //System.out.println("Sending ACK to seq " + waitingFor + " with " + sendDataBytes.length  + " bytes");
            
        }
        
        this.log("writing to  file.");
        
        // Write to file!!!
        writeFile(pathToFile, received);
    }

    @Override
    public void close() throws Exception {
        this.socket.close();
        //throw new RuntimeException("Not implemented!");
    }
    public void ssToCaSetter(){

        this.ssThresh = this.cwnd / 2;
        this.cwnd = 1;
        onWindowChange();
    }

    public void ssAckEventSetter(){
        this.cwnd = this.cwnd * 2;
        onWindowChange();
    }

    public void caAckEventSetter(){
        this.cwnd = this.cwnd + MSS*(MSS/cwnd);
        onWindowChange();
    }

    private void incIfDuplicateAck(int ackNum){
        if(ackNum == this.lastAcked)
            dupAckCount++;
    }

    private boolean tripleDuplicateAck(){
        if(dupAckCount >= 3)
            return true;
        return false;
    }

    private void resetDupAckCount(){
        this.dupAckCount = 0;
    }

    @Override
    public long getSSThreshold() {
        return this.ssThresh;
    }

    @Override
    public long getWindowSize() {
        return this.cwnd;
    }

    public byte[] readFile(String fileName){

        Path fileLocation;
        byte[] data = hexStringToByteArray("e04fd020ea3a6910a2d808002b30309d");
        try{
           
            fileLocation = Paths.get(fileName);
            data = Files.readAllBytes(fileLocation);
            //System.out.println(data.length);
        }catch(IOException ex){
            this.log("IO Exception in readFile");
        }
        this.log("File successfully read");
        return data;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                             + Character.digit(s.charAt(i+1), 16));
        }
    return data;
    }
 
    public void writeFile(String fileName, ArrayList<String> dataToWrite){

        BufferedWriter bw = null;
        FileWriter fw = null;

        try {
            String content = "This is the content to write into file\n";
            fw = new FileWriter(fileName);
            bw = new BufferedWriter(fw);
            for(int i = 0; i < dataToWrite.size(); i++){
                bw.write(dataToWrite.get(i));
            }
            //bw.write(content);
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

public void sendData(byte[] sendDataBytes) throws IOException{
        
        // List of all the packets sent
         ArrayList<byte[]> sentPackets = new ArrayList<byte[]>();
                
        InetAddress ip_addr;
        //try {
            ip_addr = InetAddress.getByName(this.IP);
        
            int numPackets = (int) Math.ceil( (double) sendDataBytes.length / MSS);
            this.log("Number of packets to send = " +  Integer.toString(numPackets));
            
            while(true){
                this.log("here");
                while( (rcvBase - firstUnAcked) < WINDOW_SIZE && ( rcvBase < numPackets) ) {
                    System.out.println("kkkkk");
                    if ( rcvBase < numPackets) {
                        byte[] filePacketBytes = new byte[MSS];
                        filePacketBytes = Arrays.copyOfRange(sendDataBytes, rcvBase * MSS, rcvBase * MSS + MSS);
                  
                        String SEQ_NUM = Integer.toString(rcvBase);
                        String SendData_Str="SEQ" + " "+ SEQ_NUM + " ";
                        System.out.println("Sending SEQ" + SEQ_NUM);
                        byte[] combined = new byte[filePacketBytes.length + SendData_Str.getBytes().length];
                        byte a[];
                        byte b[];

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        outputStream.write(SendData_Str.getBytes());
                        outputStream.write(filePacketBytes);
                        combined = outputStream.toByteArray();
                        this.log("Sending" + SendData_Str);
                        this.log(ip_addr.toString());
                        DatagramPacket sendPacket = new DatagramPacket(combined, combined.length, ip_addr, SERVER_PORT_2);
                        sentPackets.add(combined);

                        if(Math.random() > PROBABILITY){
                            
                            this.socket.send(sendPacket);
                            this.log("Packet sent");
                        }
                        else{
                        
                            System.out.println("[X] Lost packet with sequence number " + rcvBase);
                        }
                           rcvBase++;
                    }
                }

                    byte[] ackBytes = new byte[NumBytes];
                    DatagramPacket ack = new DatagramPacket(ackBytes, ackBytes.length);
                        
                    try{
                        // If an ACK was not received in the time specified (continues on the catch clausule)
                        this.log("Start Timer");
                        this.socket.setSoTimeout(TIMER);
                        // Receive the packet
                        this.socket.receive(ack);
                        String receivedStr = new String(ack.getData());
                        String[] tokens = receivedStr.split("\\s+");
                        this.log("Received_Data "+ receivedStr);
                        int ackNum = Integer.parseInt(tokens[1].trim());
                        // If this ack is for the last packet, stop the sender 
                        if(ackNum == numPackets){
                            break;
                        }

                        firstUnAcked = Math.max(firstUnAcked, ackNum);
                            
                        }catch(SocketTimeoutException e){
                            this.log("Timer Expired");
                            // then send all the sent but non-acked packets
                            for(int i = firstUnAcked; i < rcvBase; i++){
                                byte[] sendData = sentPackets.get(i);
                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip_addr, SERVER_PORT_2);

                                if(Math.random() > PROBABILITY){

                                    this.socket.send(sendPacket);
                                }
                                else{
                                    this.log("[X] Lost packet with sequence number " + i);
                                }

                            }
                        }

            }
            String endStr = "END";
            byte[] endBytes = endStr.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(endBytes, endBytes.length, ip_addr, SERVER_PORT_2);
            this.socket.send(sendPacket);
            this.log("Finished transmission");
        } 
}