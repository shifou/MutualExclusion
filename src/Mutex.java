
public class Mutex {
	MessagePasser mp;
	MutexState st=MutexState.HOLD;
	int req, rel, vote;
	public boolean voted;
	public Mutex(MessagePasser messagePasser){
		mp = messagePasser;
		req=rel=vote=0;
		voted=false;
	}
	
	public void request(int seq, boolean logical) {
		// TODO Auto-generated method stub
		
	}
	public void recRequst(Message mes)
	{
		
	}
	public void release() {
		// TODO Auto-generated method stub
		
	}
	public void recRelease(Message mes)
	{
		
	}
	public String stat() {
		// TODO Auto-generated method stub
		return null;
	}
}
