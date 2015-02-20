
public class nodeInfo {

	String ip;
	int port;
	public nodeInfo(String ip, int port){
		this.ip = ip;
		this.port = port;
	}
	public String toString(){
		return ip + "," + Integer.toString(port);
	}
}
