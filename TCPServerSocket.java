public abstract class TCPServerSocket {
	public int port;
    public TCPServerSocket(int port) throws Exception {
    	this.port = port;
    }

    public abstract TCPSocket accept() throws Exception;

    public abstract void close() throws Exception;
}