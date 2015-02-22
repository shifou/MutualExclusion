import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Mutex {
	MessagePasser mp;
	MutexState st=MutexState.HOLD;
	int req, reqrec,rel, relrec,vote,enter;
	public LinkedList<Message> holdBackQueueList = new LinkedList<Message>();
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
	
	public void recRequest(Message mes) throws FileNotFoundException
	{
		if(voted == true)
		{
			if(insert(holdBackQueueList,mes) == 1) // insert successfully
			{
				this.reqrec++;
			}
		}else{			//if it hasn't voted yet, vote immediately
			
			this.sendVote(mes);
			this.reqrec++;
			voted = true;
		}	
		
	}
		private void sendVote(Message mes) throws FileNotFoundException
		{
			Message vote = mes.clone(mes);
			vote.set_src(mp.username);
			vote.set_hostname(mp.username);
			vote.des = mes.src;
			vote.data = "OK";
			vote.kind = "Vote";
			vote.action = "normal";
			vote.ms = MutexState.VOTE;
			mp.send(vote);
			
		}
		private int insert(LinkedList<Message> linkedList, Message mes) {
		
		// TODO Auto-generated method stub
		if(linkedList.isEmpty())
		{
			linkedList.add(mes);
			return 1;
		}
		
		for(int i = 0; i < linkedList.size();i++)
		{
			Message tmp = linkedList.get(i);
	
			
			if(mes.lt.getLogical() < tmp.lt.getLogical())
			{
				linkedList.add(i,mes);
				break;
			}else if(mes.lt.getLogical() >= tmp.lt.getLogical())
			{
				if(i != tmp.multicastVector.length-1)
				{
					linkedList.add(i+1,mes);
					break;
				}else{
					linkedList.addLast(mes);
					break;
				}
					
			}
			
			
		}
		return 0;
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
	
	/*
	 * After receive Release Message, which means CS is currently available to other nodes:
	 * 		1. set the voted boolean to false
	 * 			a. vote to the request in the top of Queue
	 * 			b. if queue is empty, don't vote
	 */
	
	public void recRelease(Message mes) throws FileNotFoundException
	{
		this.voted = false;
		this.relrec++;
		if(!holdBackQueueList.isEmpty())
		{
			Message tmp = holdBackQueueList.removeFirst();
			this.sendVote(tmp);
		}
		
		
	}
	
	private void recVote(Message mes) {
		// TODO Auto-generated method stub
		vote++;
		
	}

	public synchronized void receive(Message mes) {
		switch(mes.ms)
		{
		case REQUEST:
			this.recRequest(mes);   //TODO:
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