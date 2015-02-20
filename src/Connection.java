import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
public class Connection implements Runnable {
	private Socket socket;
	public ConcurrentLinkedQueue<Message> messageQueue;
	private volatile boolean running;
	public Vector<Message> messageRec = new Vector<Message>();
	private ObjectInputStream objInput;
	private ObjectOutputStream objOutput;
	public ConcurrentHashMap<String, Socket> sk;
	public ConcurrentHashMap<String, ObjectOutputStream> st;
	public ArrayList<LogicalTimeStamp> logMat;
	public ArrayList<VectorTimeStamp> vecMat;
	public boolean logicalTime;
	public String name;
	public boolean log;
	public configFileParse config;
	public Multicast multicast=null;
	public Connection(String name,Socket slaveSocket, ObjectOutputStream out, ObjectInputStream objInput2, ConcurrentLinkedQueue mq, ConcurrentHashMap<String, Socket> sk, ConcurrentHashMap<String, ObjectOutputStream> st, Multicast multicast,configFileParse config) throws IOException {
		// TODO Auto-generated constructor stub
		this.config=config;
		socket = slaveSocket;
		objOutput = out;
		this.name=name;
		objOutput.flush();
		objInput = objInput2;
		running=true;
		log=false;
		this.sk=sk;
		this.st=st;
		messageQueue=mq;
		this.multicast=multicast;
	}
	public Connection(String name,Socket slaveSocket, ObjectOutputStream out, ObjectInputStream objInput2, Vector<Message> messageRec,boolean logicalTime, ConcurrentHashMap<String, Socket> sk2, ConcurrentHashMap<String, ObjectOutputStream> st2, configFileParse config) throws IOException 
	{
		this.config=config;
		sk=sk2;
		messageQueue=new ConcurrentLinkedQueue<Message>();
		st=st2;
		this.name=name;
		socket = slaveSocket;
		objOutput = out;
		objOutput.flush();
		objInput = objInput2;
		running=true;
		this.messageRec=messageRec;
		this.logicalTime=logicalTime;
		log=true;
		
	}
	public void run() {
		try {
			Message mes;
			while (running) {
				try {
					mes = (Message) objInput.readObject();

					if(log==false)
					{
						if(mes.multicast)
						{
							String hold = config.recvRule(mes);
							 
							switch(hold){
							case "drop":
								break;
							case "duplicate":
								//System.out.println("receive: duplicate");
								if(mes.duplicate)
									this.multicast.receive(mes);
								//messageRec.add(mes);
								while(!this.multicast.delayQueue.isEmpty()){
									this.multicast.receive(this.multicast.delayQueue.poll());
								}
								break;
							case "delay":
								System.out.println("receive: delay");
								this.multicast.delayQueue.offer(mes);
								break;
							default:
								this.multicast.receive(mes);
								while(!this.multicast.delayQueue.isEmpty()){
									this.multicast.receive(this.multicast.delayQueue.poll());
								}
							}
						}
						else
							messageQueue.offer(mes);
					}
					else
					{
						String hold = config.recvRule(mes);
						switch(hold){
						case "drop":
							break;
						case "duplicate":
							//System.out.println("receive: duplicate");
							messageRec.add(mes);
							//messageRec.add(mes);
							while(!messageQueue.isEmpty()){
								messageRec.add(messageQueue.poll());
							}
							break;
						case "delay":
							//System.out.println("receive: delay");
							messageQueue.offer(mes);
							break;
						default:
							//default action
							//System.out.println("receive: default");
							messageRec.add(mes);
							System.out.println("logger rec: "+mes.toString());
							while(!messageQueue.isEmpty()){
								messageRec.add(messageQueue.poll());
							}
						}
					}
					
				} catch (ClassNotFoundException e) {
					System.out.println("read disconnected message");
					return;
				}
				catch(EOFException e)
				{
					sk.remove(name);
		            st.remove(name);
					System.out.println("detect disconnected message remove "+name);
					return;
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
			System.out.println("message error in handle");

		}
	}
	
}
