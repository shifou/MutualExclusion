import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;


public class Manager {
	public static void main(String[] args) {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		int seq=0;
		boolean flag=true,vec=false;;
		while(flag)
		{
			System.out.println("input vector or logical: ");
			try {
				switch(in.readLine())
				{
				case "vector":
					 flag=false;
					 break;
				case "logical":
					vec=true;
					flag=false;
					break;
				default:
					System.out.println("wrong input format");
					break;
				}
			} catch (IOException e) {
				System.out.println("exit");
			}
		}
		MessagePasser messagePasser=null;
		try {
			messagePasser = new MessagePasser(args[0], args[1],vec);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block

			System.out.println("error exit in init");
		}
		while(true){
			System.out.println("Enter the command : send, rec, lrec, print, issue, multicast");
			String cm="";
			try {
				cm = in.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block

				System.out.println("error exit in read cmd");
					}
			String[] hold = cm.split("#");
			messagePasser.log=false;
			switch(hold[0]){
				case "send":
					if(hold.length!=6)
					{
						System.err.println("wrong send command!\n");
						System.out.println("usage: send#bob#Action#kind#what is your name#y");
						break;
					}
					Message message = new Message(args[1],hold[1],hold[2], hold[3],hold[4]);
					message.set_seqNum(seq++);
					if(hold[5].equals("y"))
						messagePasser.log=true;
				try {
					messagePasser.send(message);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block

					System.out.println("exit in send");
				}
					break;
				case "rec":
				try {
					System.out.println(messagePasser.receive().toString());
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block

					System.out.println("exit in rec");
				}
					break;
				case "lrec":
					messagePasser.log=true;
					//System.out.println(messagePasser.log);
				try {
					System.out.println(messagePasser.receive().toString());
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					System.out.println("error exit in lrec");
					
				}
					break;
				case "print":
					System.out.println(messagePasser.printTimestamp());
					break;
				case "issue":
					messagePasser.issueTimestamp();
					break;
				case "multicast":
					//multicast#Group1#abc#abc#haha#n
					if(hold.length!=6)
					{
						//System.err.println("wrong send command!\n");
						System.out.println("usage: multicast#GroupName#Action#kind#data#n/y");
						break;
					}
					message = new Message(args[1],"",hold[2], hold[3],hold[4]);
					message.multicast=true;
					message.groupName=hold[1];
					message.logicalTime=vec;
					if(messagePasser.groups.containsKey(hold[1])==false)
					{
						System.out.println("no such group");
						break;
					}
					message.groupSize=messagePasser.groups.get(hold[1]).size();
					message.set_seqNum(seq++);
					if(hold[5].equals("y"))
						messagePasser.log=true;
				try {
					messagePasser.multicast.send(message);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					System.out.println("error exit in multi");
					
				}
					break;
				default:
					System.err.println("Illegal input format! Please enter again!");
				}
		}
	}
}
