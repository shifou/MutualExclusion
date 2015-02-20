import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList; 
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Logger {
	public LinkedHashMap<String, nodeInfo> nodes = new LinkedHashMap<String, nodeInfo>();
	public static Vector<Message> messageRec = new Vector<Message>();
	public ConcurrentHashMap<String, Socket> sockets = new ConcurrentHashMap<String, Socket>();
	public ConcurrentHashMap<String, ObjectOutputStream> streams= new ConcurrentHashMap<String, ObjectOutputStream>();
	public boolean logicalTime ;
	public String filename;
	public long last;
	public User user;
	public configFileParse config;
	public int size;
	public int port;
	public Logger(String configuration_filename,boolean lt)
	{
		logicalTime=lt;
		try {
			config = new configFileParse(configuration_filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		filename = configuration_filename;
		File hold  = new File(filename);
		last=hold.lastModified();
		System.out.println("last: "+last);
		port = config.getPortbyName("logger");
		if(port==-1)
		{
			System.out.println("can not find the user info in config");
			return;
		}
		nodes= config.getNetMap("logger");
		size = config.getSize();
		user = new User("logger", port,messageRec,sockets, streams,nodes,logicalTime,config);
		new Thread(user).start();
	
	}
	

	
	private static void printLog(Vector<Message> messRecv)
	{
		
		if(messRecv == null || messRecv.size() == 0)
		{
			System.out.println("Nothing has been logged!");
		}
		int len = messRecv.size();
		ArrayList<Message> messageRec = new ArrayList<Message>(messRecv);
		Collections.sort(messageRec);
		
		for(int i = 0; i < len; i++)
		{
			System.out.println(messageRec.get(i).toString());
			
			for(int j = 0; j < len; j++)
			{
				if(j != i)
				{
					if(messageRec.get(i).compareTo(messageRec.get(j)) == 0)
					{
						System.out.println("\t\t\t\t"+messageRec.get(j).toString());
					}
				}
			}
			System.out.println("*************************************************\n");
			
			
		}
	}
	public static void main(String[] args) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		boolean flag=true,vec=false;;
		while(flag)
		{
			System.out.println("input vector or logical: ");
			switch(in.readLine())
			{
			case "vector":
				 vec=true;
				 flag=false;
				 break;
			case "logical":
				flag=false;
				break;
			default:
				System.out.println("wrong input format");
				break;
			}
		}
		Logger logger = new Logger(args[0],vec);
		while(true)
		{
			String command = in.readLine();
			switch (command) {
			case "print":
				printLog(messageRec);																																										
				break;
			default:
				System.err.println("wrong input!");
				break;
			}
		}
	}
}
