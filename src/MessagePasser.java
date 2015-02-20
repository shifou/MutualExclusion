import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class MessagePasser {
	public String username;
	public String filename;
	public User user;
	public configFileParse config;
	public int port;
	public boolean logicalTime;
	public LogicalTimeStamp lt;
	public VectorTimeStamp vt;
	public long last;
	public int nodeNum;
	public int id;
	public boolean log;
	public LinkedHashMap<String,Integer> u2i =new LinkedHashMap<String,Integer>();
	public LinkedHashMap<String, nodeInfo> nodes = new LinkedHashMap<String, nodeInfo>();
	public ConcurrentLinkedQueue<Message> messageRec = new ConcurrentLinkedQueue<Message>();
	public ConcurrentHashMap<String, Socket> sockets = new ConcurrentHashMap<String, Socket>();
	public ConcurrentHashMap<String, ObjectOutputStream> streams= new ConcurrentHashMap<String, ObjectOutputStream>();
	public ConcurrentLinkedQueue<Message> delaySend = new ConcurrentLinkedQueue<Message>();
	public ConcurrentLinkedQueue<Message> delayRec = new ConcurrentLinkedQueue<Message>();
	public ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<Message>();
	public LinkedHashMap<String,ArrayList<String>> groups = new LinkedHashMap<String,ArrayList<String>>();
	public Multicast multicast;	
	public HashMap<String, HashMap<String,Integer>> gid= new HashMap<String, HashMap<String,Integer>>();
	public MessagePasser(String configuration_filename, String local_name,boolean lg) throws FileNotFoundException {
		config = new configFileParse(configuration_filename);
		filename = configuration_filename;
		File hold  = new File(filename);
		last=hold.lastModified();
		System.out.println("last: "+last);
		username = local_name;
		port = config.getPortbyName(username);
		if(port==-1)
		{
			System.out.println("can not find the user info in config");
			return;
		}
		log=false;
		groups = config.getGroups();
		nodeNum = config.getSize();  // starts from 1;
		//System.out.println("---"+nodeNum);
		id = config.getId(username); // ID starts from 0, if can't find return -1
		if(id==-1)
		{
			System.out.println("can not find the user");
			return;
		}
		u2i=config.getAllID();
		nodes= config.getNetMap(username);
		//sockets = getSocketMap(nodes);
		logicalTime=lg;
		//System.out.println(logicalTime);
		if(this.logicalTime)
			 lt =new LogicalTimeStamp();
		else
			vt = new VectorTimeStamp(nodeNum);
		//System.out.println(vt.toString());
		multicast = new Multicast(this); 
		for(String hh:groups.keySet())
		{
			HashMap<String,Integer> fk= new HashMap<String,Integer>();
			int oo=0;
			for(String tt:groups.get(hh))
			{
				fk.put(tt, oo++);
			}
			gid.put(hh, fk);
		}
		user = new User(username, port,messageRec,sockets, streams,nodes,multicast,config);
		new Thread(user).start();
		
	}

	public boolean reconfig() throws FileNotFoundException {
		// TODO Auto-generated method stub
		if(last>=new File(filename).lastModified())
		{

			//System.out.println("last: "+new File(filename).lastModified());
			 return false;
		
		}	 
		last=new File(filename).lastModified();
		configFileParse fk= new configFileParse(filename);
		config = fk;
		port = config.getPortbyName(username);
		if(port==-1)
		{
			System.out.println("can not find the user info in config");
			return false;
		}
		id = config.getId(username); // ID starts from 0, if can't find return -1
		if(id==-1)
		{
			System.out.println("can not find the user");
			return false;
		}
		u2i=config.getAllID();
		nodes= config.getNetMap(username);
		//System.out.println(nodes);
		//sockets = getSocketMap(nodes);
		//user = new User(username, port,messageRec);
		return true;
	}

	void send(Message mes) throws FileNotFoundException {
		//System.out.println("config changed: "+reconfig());
		//System.out.println(mes.des);
		if(this.nodes.containsKey(mes.des)==false)
		{
			System.out.println("can not find this node information in the config");
			return;
		}
		
		//System.out.println(hold+"-----");
		
		mes.id=u2i.get(mes.src);
		//System.out.println("-----"+mes.id+"\t"+id);
		mes.logicalTime=this.logicalTime;
		if(this.logicalTime)
		{
			this.lt.Increment();
			//System.out.println("send timestamp from: "+username+" with id "+id+" "+this.lt.toString());
			
			mes.lt=this.lt;
		}
		else
		{
			this.vt.Increment(id);
			//System.out.println("send timestamp from: "+username+" with id "+id+" "+this.vt.toString());
			mes.vt=this.vt;
		}
		//.out.println("???"+this.vt.toString());
		String hold = config.sendRule(mes);
		switch(hold){
			case "drop":
				System.out.println("drop send");
				break;
			case "duplicate":
				sendMessage(mes);
				mes.set_duplicate();
				sendMessage(mes);
				break;
			case "delay":
				System.out.println("send delay");
				delaySend.offer(mes);
				break;
			default:
				//System.out.println("send");
				if(mes.multicast)
					System.out.println("multicast: "+hold.toString());
				sendMessage(mes);
				break;
		}
		if(this.log)
			LogSendEvent(mes,this.logicalTime);
	
	}


	private void LogSendEvent(Message mes,boolean lt) {

			//mes.id=u2i.get(mes.src);
			mes.src=mes.src+" "+mes.des;
			mes.des="logger";
			
			//mes.logicalTime=lt;
				sendMessage(mes);
				if(this.logicalTime)
				{
					this.lt.Increment();
					System.out.println("increase timestamp due to log as: "+username+" with id "+id+" "+this.lt.toString());
					mes.lt=this.lt;
				}
				else
				{
					this.vt.Increment(id);
					System.out.println("increase timestamp due to log as: "+username+" with id "+id+" "+this.vt.toString());
					mes.vt=this.vt;
				}
			
	}

	private void sendMessage(Message mes) {
		// TODO Auto-generated method stub
		if(this.sockets.containsKey(mes.des)==false)
		{
			System.out.println("add "+mes.des+" to socket list");
			nodeInfo hold= nodes.get(mes.des);
			try {
				//System.out.println(hold.ip+"\t"+hold.port);
				Socket sendd = new Socket(hold.ip, hold.port);
				ObjectOutputStream out=new ObjectOutputStream(sendd.getOutputStream());
				 ObjectInputStream objInput = new ObjectInputStream(sendd.getInputStream());
		           
				sockets.put(mes.des, sendd);
				streams.put(mes.des, out);
				 Connection handler;
				 out.writeObject(username);
				 //out.writeChars(this.username);
	             handler = new Connection(mes.des,sendd,out,objInput,messageRec,sockets,streams,multicast,config);
	             new Thread(handler).start();
		         
			} catch (UnknownHostException e) {
				System.out.println("can not establish connection");
				//e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("can not establish connection");
				
			}
			
		}
		try{
			//System.out.println("des: "+mes.des);
		ObjectOutputStream out= streams.get(mes.des);
		System.out.println("-----------sending "+mes.toString());
		out.writeObject(mes);
		out.flush();
		out.reset();
		//System.out.println("###"+delaySend.size());
		}catch(IOException e){
			System.err.println("send fail");
			return;
		}
		if(mes.des.equals("logger")==false)
		{
		while(!delaySend.isEmpty())
		{
			sendMessage(delaySend.poll());
		}
		}
		
	}

	private void logRecEvent(Message mes) {
		mes.src=mes.src+" "+mes.des;
		mes.des="logger";
		sendMessage(mes);
		if(this.logicalTime)
		{
			this.lt.Increment();
			System.out.println("increase timestamp due to log as: "+username+" with id "+id+" "+this.lt.toString());
			mes.lt=this.lt;
		}
		else
		{
			this.vt.Increment(id);
			System.out.println("increase timestamp due to log as: "+username+" with id "+id+" "+this.vt.toString());
			mes.vt=this.vt;
		}
	}
	Message receive() throws FileNotFoundException {
		//System.out.println(log);
		
		//System.out.println("reread: "+reconfig());
		receiveMessage();
		if(!messages.isEmpty())
		{
			//System.out.println("check queue: "+messages.isEmpty());
			Message mes = messages.poll();
			if(this.logicalTime)
				System.out.println(username+" rec timestamp: "+this.lt.toString());
			else
				System.out.println(username+" rec timestamp: "+this.vt.toString());
			if(log)
			{
				//System.out.println("-------");
				logRecEvent(mes);
			}
			return mes;
		}
		else{
			return new Message(null,null, null, null,"no message received");
		}
	}
	private void receiveMessage() {
		Message mes;
		if(!messageRec.isEmpty()){
			mes = messageRec.poll();
			if(mes.logicalTime)
			{
				this.lt.updateTimeStamp(mes.lt);
				this.lt.Increment();
				mes.lt=this.lt;
			}
			else
			{
				this.vt.updateTimeStamp(mes.vt);
				this.vt.Increment(id);
				mes.vt=this.vt;
			}
			String action = this.config.recvRule(mes);
			switch(action){
			case "drop":
				break;
			case "duplicate":
				//System.out.println("receive: duplicate");
				messages.offer(mes);
				messages.offer(mes);
				while(!delayRec.isEmpty()){
					messages.offer(delayRec.poll());
				}
				break;
			case "delay":
				//System.out.println("receive: delay");
				delayRec.offer(mes);
				receiveMessage();
				break;
			default:
				//default action
				//System.out.println("receive: default");
				messages.offer(mes);
				while(!delayRec.isEmpty()){
					messages.offer(delayRec.poll());
				}
			}
	}
	}

	public String printTimestamp() {
		if(this.logicalTime)
			return this.lt.toString();
			else
				return this.vt.toString();
				
	}

	public void issueTimestamp() {
		if(this.logicalTime)
			this.lt.Increment();
			else
				this.vt.Increment(id);
		System.out.print("now timestamp: ");
		if(this.logicalTime)
			System.out.println(this.lt.toString());
		else
			System.out.println(this.vt.toString());
	}


}

