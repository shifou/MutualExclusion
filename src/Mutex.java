import java.util.concurrent.ConcurrentLinkedQueue;


public class Mutex {
	MessagePasser mp;
	MutexState st=MutexState.HOLD;
	int req, reqrec,rel, relrec,vote,enter;
	public boolean voted;
	public ConcurrentLinkedQueue<Message> delayQueue=new ConcurrentLinkedQueue<Message>();
	
	public Mutex(MessagePasser messagePasser){
		mp = messagePasser;
		req=reqrec=rel=relrec=vote=enter=0;
		voted=false;
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
