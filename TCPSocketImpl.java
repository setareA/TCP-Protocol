import java.util.Random;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;
import java.nio.file.*;


public class TCPSocketImpl extends TCPSocket {
    public EnhancedDatagramSocket socket;
    private String IP;
    private int PORT;
    private int SEQ_NUM = 0;
    private int ACK_NUM = 0;
    private int BASE = 0;
    private int SERVER_PORT = 8888;
    private State STATE = State.NONE;
    private CngsState cngsState = CngsState.NONE;
    private int NumBytes = 1024;
    private int cwnd = 0;
    private int ssThresh = 0;
    private int MSS = 1024;
    private int WINDOW_SIZE = 4;
    private int TIMER = 30;
    private int rcvBase = 0;
    private int firstUnAcked = 0;


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
        //System.out.println("kharoo");
        System.out.println(fileToSend.length);
        this.sendData(fileToSend);
        byte[] sendDataBytes = new byte[NumBytes];
        this.log("File Successfully read");

        // TODO : Send File!!!

        //throw new RuntimeException("Not implemented!");
    }

    @Override
    public void receive(String pathToFile) throws Exception {

        this.log("Receive called");
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

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                             + Character.digit(s.charAt(i+1), 16));
        }
    return data;
    }

    public void sendData(byte[] sendDataBytes){
        int numPackets = (int) Math.ceil( (double) sendDataBytes.length / MSS);
        while( (rcvBase - firstUnAcked) < WINDOW_SIZE) {
            
          if ( rcvBase < numPackets) {
              byte[] filePacketBytes = new byte[MSS];
              filePacketBytes = Arrays.copyOfRange(sendDataBytes, rcvBase*MSS, rcvBase*MSS + MSS);
           }
        
        }
    }

}
                System.out.println("Unknown Exception!!!!!!");
            }
    }

    @Override
    public void send(String pathToFile) throws Exception {
        this.log("Send Called");
        this.establishConnection();
        this.log("Start Sending...");

        byte[]  fileToSend = readFile(pathToFile);
        //System.out.println("kharoo");
        System.out.println(fileToSend.length);
        this.sendData(fileToSend);
        byte[] sendDataBytes = new byte[NumBytes];
        this.log("File Successfully read");

        // TODO : Send File!!!

        //throw new RuntimeException("Not implemented!");
    }

    @Override
    public void receive(String pathToFile) throws Exception {

        this.log("Receive called");
    }

    @Override
    public void close() throws Exception {
        this.socket.close();
        //throw new RuntimeException("Not implemented!");
    }
    public void ssToCaSetter(){

        this.ssThresh = cwnd / 2;
        this.cwnd = 1;
    }

    public void ssAckEventSetter(){
        cwnd = cwnd * 2;
    }

    public void caAckEventSetter(){
        cwnd = cwnd + MSS*(MSS/cwnd);
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

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                             + Character.digit(s.charAt(i+1), 16));
        }
    return data;
    }

    public void sendData(byte[] sendDataBytes){
    int numPackets = (int) Math.ceil( (double) sendDataBytes.length / MSS);
        while( (rcvBase - firstUnAcked) < WINDOW_SIZE) {
            
          if ( rcvBase < numPackets) {
              byte[] filePacketBytes = new byte[MSS];
              filePacketBytes = Arrays.copyOfRange(sendDataBytes, rcvBase*MSS, rcvBase*MSS + MSS);
           }
        
        }
        
    }

}