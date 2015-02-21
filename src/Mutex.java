import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Mutex {
	MessagePasser mp;
	MutexState st=MutexState.HOLD;
	int req, reqrec,rel, relrec,vote,enter;
	public HashMap<String,LinkedList<Message>> holdBackQueueList = new HashMap<String,LinkedList<Message>>();
	public boolean voted;
	public HashSet<String> voteMem;
	public int groupSize;
	public Mutex(MessagePasser messagePasser){
		voteMem=new HashSet<String>();
		mp = messagePasser;
		req=reqrec=rel=relrec=vote=enter=0;
		voted=false;
		groupSize = mp.groups.get("group_"+mp.username).size();
	}
	
	public void request() 
	{
		if(st == MutexState.HOLD){
			System.out.println("in CS now");
			return;
		}
		if(st == MutexState.REQUEST){
			System.out.println("already request");
			return;
		}
		req++;
		Message message = new Message(mp.username,"","action", "kind","ME");
		message.multicast=true;
		message.mutex=true;
		message.ms=MutexState.REQUEST;
		message.logicalTime=true;
		message.groupName="group_"+mp.username;
		message.groupSize=mp.groups.get(message.groupName).size();

		try {
			mp.multicast.send(message);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("sending request error");
		}
		new LockWatcher(this).start();
	}
	public void recRequest(Message mes)
	{
		this.reqrec++;
		
	}
	public void release() {
		if(this.st == MutexState.RELEASE){
			System.out.println("exit CS already");
			return;
		}
		if(st == MutexState.REQUEST){
			System.err.println("not in CS still wait for vote");
			return;
		}
		this.rel++;
		Message message = new Message(mp.username,"","action", "kind","ME");
		message.multicast=true;
		message.mutex=true;
		message.ms=MutexState.RELEASE;
		message.logicalTime=true;
		message.groupName="group_"+mp.username;
		message.groupSize=groupSize;
		this.voteMem.clear();
		try {
			mp.multicast.send(message);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("sending request error");
		}
	}
	public void recRelease(Message mes)
	{
		this.relrec++;
	}
	private void recVote(Message mes) {
		// TODO Auto-generated method stub
		vote++;
		
	}

	public synchronized void receive(Message mes) {
		switch(mes.ms)
		{
		case REQUEST:
			this.recRequest(mes);
			break;
		case  RELEASE:
			this.recRelease(mes);
			break;
		case VOTE:
			this.recVote(mes);
			break;
			default:
				System.out.println("miss Mutex state");
				break;
		}
	
	}


	public String stat() {
		String s="request send: "+req+"\trequest rec: "+reqrec+"\trelease send: "+rel+"\trelease rec: "+relrec+"\tvote num: "+vote+"\tenter cs: "+enter;
		if(this.st==MutexState.HOLD)
			s+="\nin cs now";
		if(this.st==MutexState.RELEASE)
			s+="\nexit cs wait for vote";
		if(this.st==MutexState.REQUEST)
			s+="\nrequest for cs";
		if(this.st==MutexState.VOTE)
			s+="\nvote for cs";
		return s;
	}
}
class LockWatcher extends Thread{
	
	Mutex mutex;
	public LockWatcher(Mutex mutex){
		this.mutex = mutex;
	}
	
	public void run(){
		boolean wait = false;
		while(this.mutex.st != MutexState.HOLD){
			if(!wait){
				System.out.println("BLOCKED!");
				wait = true;
			}
			
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("GET THE LOCK!");
		wait = false;
	}
}