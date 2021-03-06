import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Mutex {
	MessagePasser mp;
	public volatile MutexState st=MutexState.RELEASE;
	int req, reqrec,rel, relrec,vote,enter;
	public LinkedList<Message> holdBackQueueList = new LinkedList<Message>();
	public boolean voted;
	public HashSet<String> voteMem;
	public HashSet<String> votes;
	public int groupSize;
	public Mutex(MessagePasser messagePasser){
		voteMem=new HashSet<String>();
		mp = messagePasser;
		req=reqrec=rel=relrec=vote=enter=0;
		voted=false;
		groupSize = mp.groups.get("Group_"+mp.username).size();
		votes=new HashSet<String>();
		for(String hold: mp.groups.get("Group_"+mp.username))
		{
			voteMem.add(hold);
		}
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
		this.st=MutexState.REQUEST;
		Message message = new Message(mp.username,"","Request", "Request","ME");
		message.multicast=true;
		message.mutex=true;
		message.ms=MutexState.REQUEST;
		message.logicalTime=true;
		message.groupName="Group_"+mp.username;
		message.groupSize=mp.groups.get(message.groupName).size();
		message.lt=mp.lt.clone(mp.lt);
		message.seq=mp.seq;
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
			insert(holdBackQueueList,mes); // insert successfully
			this.reqrec++;
		
		}else{			//if it hasn't voted yet, vote immediately
			System.out.println("send vote to: "+mes.src);
			if(mes.src.equals(mp.username))
			{
				recVote(mes);
			}
			else
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
			vote.multicast=false;
			vote.mutex=false;
			vote.logicalTime=true;
			vote.des = mes.src;
			vote.data = "OK";
			vote.seq=mp.seq;
			vote.lt=mp.lt.clone(mp.lt);
			vote.kind = "Vote";
			vote.action = "normal";
			vote.ms = MutexState.VOTE;
			mp.send(vote);
		}
		private void insert(LinkedList<Message> linkedList, Message mes) {
		
		// TODO Auto-generated method stub
		if(linkedList.isEmpty())
		{

			Message tmp = mes.clone(mes);
			tmp.lt=mes.lt.clone(mes.lt);

			linkedList.add(tmp);
			System.out.println("===========");
			System.out.println(linkedList);
			System.out.println("===========");
			return;
		}
		
		for(int i = 0; i < linkedList.size();i++)
		{
			Message tmp = linkedList.get(i);
	
			
			if(mes.lt.getLogical() < tmp.lt.getLogical())
			{

				Message tp = mes.clone(mes);
				tp.lt=mes.lt.clone(mes.lt);

				linkedList.add(i,tp);
				
				break;
			}else if(mes.lt.getLogical() >= tmp.lt.getLogical())
			{
				if(i != tmp.multicastVector.length-1)
				{

					Message tp = mes.clone(mes);
					tp.lt=mes.lt.clone(mes.lt);

					linkedList.add(i+1,tp);
					
					break;
				}else{

					Message tp = mes.clone(mes);
					tp.lt=mes.lt.clone(mes.lt);

					linkedList.addLast(tp);
					
					break;
				}
					
			}
			
			
		}
		System.out.println("===========");
		System.out.println(linkedList);
		System.out.println("===========");
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
		Message message = new Message(mp.username,"","RELEASE", "RELEASE","ME");
		message.multicast=true;
		message.mutex=true;
		message.ms=MutexState.RELEASE;
		message.logicalTime=true;
		message.seq=mp.seq;
		message.groupName="Group_"+mp.username;
		message.groupSize=groupSize;
		message.lt=mp.lt.clone(mp.lt);
		st=MutexState.RELEASE;
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
			System.out.println("in queue: "+tmp.toString());
			if(tmp.src.equals(mp.username))
				recVote(tmp);
			else
			this.sendVote(tmp);
			voted = true;
		}
		
		
	}
	
	private void recVote(Message mes) {
		// TODO Auto-generated method stub
		vote++;
		System.out.println("get vote from: "+ mes.src+"\t"+mes.toString());
		if(!votes.contains(mes.src)&&voteMem.contains(mes.src))
			votes.add(mes.src);

		System.out.println("have votes: "+ this.vote+" total need: "+this.groupSize);
		
		if(vote==groupSize)
		{
			enter++;
			//System.out.println("enter CS");
			vote=0;
			votes.clear();
			st=MutexState.HOLD;
		}
	}

	public synchronized void receive(Message mes) {
		switch(mes.ms)
		{
		case REQUEST:
			try {
				this.recRequest(mes);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				System.out.println("request rec error");
			}   //TODO:
			break;
		case  RELEASE:
			try {
				this.recRelease(mes);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				System.out.println("release rec error");
			}
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
		String s="request send: "+req+"\trequest rec: "+reqrec+"\trelease send: "+rel+"\trelease rec: "+relrec+"\trec vote num: "+vote+"\tenter cs: "+enter;
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

	public String check() {
		// TODO Auto-generated method stub
		if(this.voted)
			return "voted and "+st.toString();
		else
			return st.toString();
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
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Enter in CS");
		wait = false;
	}
}