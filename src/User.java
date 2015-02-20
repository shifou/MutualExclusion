
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;




public class User implements Runnable{
	public ServerSocket serverSocket;
	int port;
	String username;
	public Multicast multicast=null;	
	private volatile boolean running;
	public ConcurrentLinkedQueue messageQueue;
	public ConcurrentHashMap<String, Socket> sk;
	public Vector<Message> messageRec = new Vector<Message>();
	public boolean logicalTime;
	public ConcurrentHashMap<String, ObjectOutputStream> st;
	public LinkedHashMap<String, nodeInfo> nodes;
	public boolean log;
	public configFileParse config;
	public User(String name,int port,ConcurrentLinkedQueue messageRec, ConcurrentHashMap<String, Socket> sockets, ConcurrentHashMap<String, ObjectOutputStream> streams, LinkedHashMap<String, nodeInfo> nodes,Multicast multicast,configFileParse config)
	{
		this.config=config;
		this.multicast=multicast;
		log=false;
		this.nodes = nodes;
		sk = sockets;
		st= streams;
		messageQueue=messageRec;
		this.username=name;
		running = true;
        try {
         serverSocket = new ServerSocket((short)port);
     } catch (IOException e) {
         e.printStackTrace();
         System.out.println("failed to create the user "+name+" socket");
         System.exit(0);
     }
        System.out.println("start User "+name+" at: "+port);
	}
	public User(String name,int port,Vector messageRec, ConcurrentHashMap<String, Socket> sockets, ConcurrentHashMap<String, ObjectOutputStream> streams, LinkedHashMap<String, nodeInfo> nodes,boolean logicalTime, configFileParse config)
	{
		log=true;
		this.logicalTime=logicalTime;
		this.nodes = nodes;
		sk = sockets;
		st= streams;
		this.messageRec=messageRec;
		this.username=name;
		running = true;
		this.config=config;
        try {
         serverSocket = new ServerSocket((short)port);
     } catch (IOException e) {
         e.printStackTrace();
         System.out.println("failed to create the user "+name+" socket");
         System.exit(0);
     }
        System.out.println("start User "+name+" at: "+port);
	}
	public void shutdown()
	{
		running=false;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		 System.out.println("waiting for messages");
         while(running)
         {
        	 Socket slaveSocket;
        	 try{
             slaveSocket = serverSocket.accept();
             ObjectOutputStream out=new ObjectOutputStream(slaveSocket.getOutputStream());
             ObjectInputStream objInput = new ObjectInputStream(slaveSocket.getInputStream());
             String name="";
			try {
				name = (String)objInput.readObject();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("estab con from : "+name);
            if(nodes.containsKey(name)==false&&name.indexOf(" ")==-1)
            {
            		System.out.println("receive connection from unknown host");
            		continue;
            }
            sk.put(name, slaveSocket);
            st.put(name, out);
			Connection handler;
			if(log==false)
             handler = new Connection(name,slaveSocket,out,objInput,messageQueue,sk,st,this.multicast,this.config);
			else
				handler = new Connection(name,slaveSocket,out,objInput,this.messageRec,logicalTime,sk,st,config);
             //System.out.println(slaveSocket.getInetAddress()+"\t"+slaveSocket.getPort());
				new Thread(handler).start();
	           // System.out.println("begin send");
	            /*
	            Manager.manager.con.put(conId, slaveService);
	            Message msg= new Message(msgType.CONNECT,conId);
	            Manager.manager.send(conId, msg);
	             */
			//	System.out.println("receive: "+slaveSocket.getInetAddress()+":"+slaveSocket.getPort()+" join in");
        	  }
        	 catch(IOException e){
        	         e.printStackTrace();
        	         System.out.println("socket user "+this.username+" accept failed");
        	         continue;
        	     }
		}
	}
	private String checkName(InetAddress ip, int port2) {
		System.out.println("---"+ip.getHostAddress()+"\t"+port2);
		for(String hold : nodes.keySet())
		{
			nodeInfo tmp = nodes.get(hold);
			System.out.println(tmp.ip+"\t"+tmp.port);
			if(tmp.ip.equals(ip.getHostAddress())&& tmp.port==port2)
				return hold;
		}
		return "";
	}
}
