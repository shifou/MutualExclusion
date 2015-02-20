import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Multicast {
	MessagePasser mp;
	HashMap<String, int[]> vectorMap = new HashMap<>();
	HashMap<String,LinkedList<Message>> holdBackQueueList = new HashMap<String,LinkedList<Message>>();
	public ConcurrentLinkedQueue<Message> delayQueue=new ConcurrentLinkedQueue<Message>();
	public Multicast(MessagePasser messagePasser){
		this.mp = messagePasser;
		for(String hold : mp.groups.keySet()){
			int len=mp.groups.get(hold).size();
			int[] groupVector = new int[len];
			for(int j=0; j<len; j++){
				groupVector[j] = 0;
			}
			vectorMap.put(hold, groupVector);
			holdBackQueueList.put(hold,new LinkedList<Message>());
			
		}
	}
	public void send(Message message) throws FileNotFoundException {
		//mp.reconfig();
		
		int length = vectorMap.get(message.groupName).length;
		int[] tmp = new int[length];
		for(int i=0; i<length; i++){
			if(i==mp.gid.get(message.groupName).get(message.src))
				 (vectorMap.get(message.groupName))[i]+=1;
			tmp[i] =  (vectorMap.get(message.groupName))[i];
		}
		message.setMulticastVector(tmp);
		for(String dest : mp.groups.get(message.groupName)){
			if(!dest.equalsIgnoreCase(message.src)){
				Message hold = message.clone(message);
				hold.des = dest;
				mp.send(hold);
			}else{
				mp.messages.offer(message);
			}
		}
	}
	
	public synchronized void receive(Message mes) throws FileNotFoundException {
		// TODO Auto-generated method stub
		//mp.reconfig();
		if(!mp.logicalTime)
		{
			mp.vt.updateTimeStamp(mes.vt);
			mp.vt.Increment(mp.id);
		}else{
			mp.lt.updateTimeStamp(mes.lt);
			mp.lt.Increment();
		}
		
		int[] recVec = mes.multicastVector;
		int length = mes.groupSize;
		int[] curVec = new int[length];
		System.out.print("rec: "+mes.getMultiVector()+" \nnow: ");
		for(int i=0; i<length; i++){
			curVec[i] =  (vectorMap.get(mes.groupName))[i];

			System.out.print(curVec[i]+" ");
		}
		String check = judge(curVec, recVec);
		System.out.println("------");
		//System.out.println("\n"+check);
		switch(check){
		case "rec":
			//System.out.println("receive multicast"+ "size of queue is" +this.holdBackQueueList.get(mes.groupName).size());
			mp.messages.offer(mes);
			// TODO: 
			vectorMap.get(mes.groupName)[mp.gid.get(mes.groupName).get(mes.src)] = mes.multicastVector[mp.gid.get(mes.groupName).get(mes.src)];
			forward(mes);
			int len = this.holdBackQueueList.get(mes.groupName).size();
			int j = 0;
			int flag = 0;
			//System.out.println("---"+len);
			while(j < len)
			{
				Message tmp = this.holdBackQueueList.get(mes.groupName).get(j);
				/*for(int i=0; i<tmp.multicastVector.length; i++){
					curVec[i] =  (vectorMap.get(mes.groupName))[i];
					System.out.print(curVec[i]+" ");
				}
				for(int i=0; i<tmp.multicastVector.length; i++){
					//tmp.multicastVector[i] =  (vectorMap.get(mes.groupName))[i];
					System.out.print(tmp.multicastVector[i]+" ");
				}*/
				for( int k =0; k < tmp.multicastVector.length; k++)
				{
					//System.out.println("----"+k+"\t"+tmp.multicastVector[k]+"\t"+curVec[k]);
					if(k != mp.gid.get(mes.groupName).get(mes.src))
					{
						if(tmp.multicastVector[k] > curVec[k])
						{
							flag = 1;
							break;
						}
					}else{
						if(tmp.multicastVector[k] != curVec[k] + 1)
						{
							flag = 1;
							break;
						}
					}
				}
				if(flag ==1)
				{
					break;
				}else{
					System.out.println("accept message from buffer");
					mp.messages.offer(tmp);
					vectorMap.get(tmp.groupName)[mp.gid.get(tmp.groupName).get(tmp.src)] = tmp.multicastVector[mp.gid.get(tmp.groupName).get(tmp.src)];
					forward(tmp);
					this.holdBackQueueList.get(tmp.groupName).removeFirst();
				}
				len= this.holdBackQueueList.get(mes.groupName).size();
				j=0;
				flag=0;
			}
			break;
		case "drop":
			System.out.println("drop multicast");
			break;
		case "hold":
			System.out.println("holdback multicast");
			insert(this.holdBackQueueList.get(mes.groupName),mes);
			break;
		}
		
		
	}
	
	private void forward(Message mes) throws FileNotFoundException
	{
		
		ArrayList<String> group = mp.groups.get(mes.groupName);
		for(int i = 0; i < group.size(); i ++)
		{
			if(!group.get(i).equals(mp.username))
			{
				Message hold = mes.clone(mes);
				hold.src = mp.username;
				hold.des = group.get(i);
				mp.send(hold);
			}
		}
	}
	
	private void insert(LinkedList<Message> linkedList, Message mes) {
		
		// TODO Auto-generated method stub
		if(linkedList.isEmpty())
		{
			linkedList.add(mes);
			return;
		}
		for(int i = 0; i < linkedList.size();i++)
		{
			Message tmp = linkedList.get(i);
			int largeCount = 0;
			
			for(int j = 0; j< tmp.multicastVector.length; j++)
			{
				if(tmp.multicastVector[j] >= mes.multicastVector[j])
				{
					largeCount ++;
				}
			}
			
			if(largeCount != 0)
			{
				linkedList.add(i, mes);
				break;
			}
			if(i == tmp.multicastVector.length-1)
			{
				linkedList.addLast(mes);
				break;
			}
		}
	}
	
	private String judge(int[] curVec, int[] recVec) {
		// TODO Auto-generated method stub
		int len = curVec.length;
		
			int counterLarg = 0;
			
			int diffOne = 0;
			for(int i=0; i < len; i++)
			{
				
					if(curVec[i] >= recVec[i])
					{
						counterLarg ++;
					}if(curVec[i] < recVec[i])
					{
						if(recVec[i] - curVec[i] ==1)
						{
							diffOne ++;
						}
					}
			}
			if(counterLarg == len-1 && diffOne ==1) // when i != j && recVec[i] <= curVec[i]
			{
				return "rec";
			}else if(counterLarg == len){
				return "drop";
			}else{
				return "hold";
			}
		

		
	}
}
