
import java.io.*;
import java.util.*;

//import java.lang.*;
import org.yaml.snakeyaml.Yaml;

public class configFileParse {
		
		private List<LinkedHashMap<String ,Object>> NodeInfo;
		private ArrayList<LinkedHashMap<String,Object>> sendRules;
		private ArrayList<LinkedHashMap<String,Object>> recvRules;
		private ArrayList<LinkedHashMap<String,Object>> groups;
		private boolean properOne;    // same node in a row
		private boolean properTwo;    // same node in a column
		private boolean properThree;  // diagonal 
		private boolean properFour;   // two group has one intersection node
		@SuppressWarnings("unchecked")
		public configFileParse(String configFile) throws FileNotFoundException {
			  
			    NodeInfo = new ArrayList<LinkedHashMap<String,Object>>();
			    sendRules = new ArrayList<LinkedHashMap<String,Object>>();
			    recvRules = new ArrayList<LinkedHashMap<String,Object>>();
			    groups = new ArrayList<LinkedHashMap<String,Object>>();
			    properOne = true;
			    properTwo = true;
			    properThree = true;
			    properFour = true;
			    InputStream input = new FileInputStream(new File(configFile));
			    Yaml yaml = new Yaml();
			    LinkedHashMap<String,Object> data = (LinkedHashMap<String, Object>)yaml.load(input);
			    
			    if(data.get("configuration") != null)
				{
				
			    	for(LinkedHashMap<String, Object> p :(ArrayList<LinkedHashMap<String, Object>>)data.get("configuration"))
			    	{
			    		LinkedHashMap<String, Object> tmp = new LinkedHashMap<String, Object>();
			    		tmp.putAll(p);
			    		NodeInfo.add(tmp);
			    	}
				}
			   

			    if(data.get("sendRules") != null)
				{
				
			    	for(LinkedHashMap<String, Object> p :(ArrayList<LinkedHashMap<String, Object>>)data.get("sendRules"))
			    	{
			    		LinkedHashMap<String, Object> tmp = new LinkedHashMap<String, Object>();
			    		tmp.putAll(p);
			    		sendRules.add(tmp);	    	
			    	
			    	}
				}
			   
		    if(data.get("receiveRules") != null)
				{
			    	
			    	for(LinkedHashMap<String, Object> p :(ArrayList<LinkedHashMap<String, Object>>)data.get("receiveRules"))
			    	{
			    	LinkedHashMap<String, Object> tmp = new LinkedHashMap<String, Object>();
			    	tmp.putAll(p);
			    	recvRules.add(tmp);	    	 	
			    	}
				}
			    
			    if(data.get("groups") != null)
				{
			    	for(LinkedHashMap<String, Object> p : (ArrayList<LinkedHashMap<String, Object>>)data.get("groups"))
			    	{
			    		LinkedHashMap<String, Object> tmp = new LinkedHashMap<String, Object>();
			    		tmp.putAll(p);			    		
			    		groups.add(tmp);
			    		
			    	}
				}
			    
			    
			    // check if the group members accord with the four properties
			    
			    
			  
			}
		
		@SuppressWarnings("unchecked")
		private void checkGroups()
		{
			int numOfnode = NodeInfo.size();
			int numOfgroup = groups.size();
			
			if(numOfnode == 0 || numOfnode != numOfgroup || numOfgroup == 0)
			{
				this.properOne = false;
				this.properTwo = false;
				this.properThree = false;
				this.properFour = false;
				return;
			}
			
			int[][] groupMap = new int[numOfnode][numOfnode];
			Arrays.fill(groupMap,0);
			
			// 1. each row has the same number of X
			int lenOfone = groups.get(0).size();
			for(int i =0; i < numOfgroup; i++)
			{
				
				if(lenOfone != groups.get(i).size())
				{
					this.properOne = false;
					return;
				}
			}
			
			int memOf = ((ArrayList<String>)NodeInfo.get(0).get("memberOf")).size();
			// 2. each column has the same number of X
			for(int i=0; i < numOfnode; i++)
			{
				// add memberOf
				if(((ArrayList<String>)NodeInfo.get(i).get("memberOf")).size() != memOf)
				{
					this.properTwo = false;
					return;
				}
			}
			
			// add node into the group map
			LinkedHashMap<String, Integer> nameAndId = new LinkedHashMap<String, Integer>();
			nameAndId = getAllID();
			for(int i = 0; i<numOfnode; i++){
				for(int j =0; j<memOf; j++)
				{
					int id = nameAndId.get(((ArrayList<String>)NodeInfo.get(i).get("memberOf")).get(j)).intValue();
					groupMap[i][id] = 1;
				}
			}
			// 3. diagonal has X
			for(int i = 0; i< numOfnode; i++)
			{
				if(groupMap[i][i] != 1)
				{
					this.properThree = false;
					return;
				}
			}
			
			// 4. for two rows, there are two X in the same column (IS A MUST)
			for(int i=0; i< numOfnode; i++)
			{
				for(int j=i; j<numOfnode;j++)
				{
					int flag = 0;
					for(int k =0; k< numOfnode; k++)
					{
						if(groupMap[i][k] == 1 && groupMap[j][k] ==1)
						{
							flag = 1;
						}
					}
					if(flag == 0)
					{
						this.properFour = false;
						return;
					}
				}
			}
	
		}
		
		
		/*public ArrayList<String> memberOf(String user)
		{
			for(LinkedHashMap<String,Object> t : NodeInfo)
			{
				if(((String)t.get("name")).equals(user))
				{
					t.get("memberOf")
				}
			}
		}*/
		public List<LinkedHashMap<String, Object>> get_config()
		{
				return NodeInfo;
		}
		
		
		// return groups
		@SuppressWarnings("unchecked")
		public LinkedHashMap<String, ArrayList<String>> getGroups()
		{
			if(groups.isEmpty())
			{
				return null;
			}
			LinkedHashMap<String, ArrayList<String>> tmp = new LinkedHashMap<String, ArrayList<String>>();
			for(LinkedHashMap<String, Object> g : groups)
			{
				if(g.get("name") != null)
				{
					tmp.put((String) g.get("name"), (ArrayList<String>)g.get("members"));	

				}
			}
			return tmp;
			
		}
		
		public LinkedHashMap<String, Object> findByName(String name)
			{
				if(NodeInfo.isEmpty())
				{
					return null;
				}
				for(LinkedHashMap<String, Object> t : NodeInfo)
				{
					if(name.equals(t.get("name")))
					{
						return t;
					}
				}
				return null;
			}	

		public LinkedHashMap<String, nodeInfo> getNetMap(String username)
		{
			if(NodeInfo.isEmpty())
			{
				return null;
			}
			LinkedHashMap<String,nodeInfo> tmp = new LinkedHashMap<String,nodeInfo>();
			
			for(LinkedHashMap<String, Object> t : NodeInfo){
				if(!username.equals(t.get("name")))
				{
					nodeInfo nod = new nodeInfo(((String)t.get("ip")),((Integer)t.get("port")).intValue());
					tmp.put((String) t.get("name"), nod);
				}
			}
			
			return tmp;
		}
		
		public int getPortbyName(String name)
		{	
			if(NodeInfo.isEmpty())
			{
				return -1;
			}
			
			for(LinkedHashMap<String, Object> t : NodeInfo)
				{
					
					if(name.equals(t.get("name")))
					{
						if(t.get("port") != null)
						{
							return ((Integer)t.get("port")).intValue();
						}
					}
				}

				return -1;
			}
			
			public boolean itemExist(String item, LinkedHashMap<String, Object> t)
			{
				if(t.get(item) == null)
				{
					return false;
				}else{
					return true;
				}
			}
			
			public String sendRule(Message sendMsg)
			{
				//System.out.println(sendMsg.toString());
				if(sendRules.isEmpty())
				{
					return "ok";
				}
				for(LinkedHashMap<String, Object> t : sendRules)
				{
					boolean targetRule = true;
					if(itemExist("seqNum",t))
					{
						if(((Integer)t.get("seqNum")).intValue() == sendMsg.seq)
						{
							targetRule = (targetRule && true);
						}else{
							continue;
						}
					}

					if(itemExist("dest",t))
					{
						if(((String)t.get("dest")).equals(sendMsg.des))
						{
							targetRule = (targetRule && true);
						}else{
							//System.out.println(sendMsg.des+"???");
							continue;
						}
					}

					if(itemExist("src",t))
					{
						if(((String)t.get("src")).equals(sendMsg.src))
						{
							targetRule = (targetRule && true);
						}else{
							continue;
						}
					}
					if(itemExist("kind",t))
					{
						if(((String)t.get("kind")).equals(sendMsg.kind))
						{
							targetRule = (targetRule && true);
						}else{
							continue;
						}
					}
					if(itemExist("duplicate",t))
					{
						if(((boolean)t.get("duplicate")) && sendMsg.duplicate)
						{
							targetRule = (targetRule && true);
						}else{
							continue;
						}
					}
					
					if(itemExist("duplicate",t))
					{
						if(((boolean)t.get("duplicate")) && sendMsg.duplicate)
						{
							targetRule = (targetRule && true);
						}else{
							continue;
						}
					}

					if(targetRule == true)
					{
						return ((String)t.get("action"));
					}
					
				}
				return "ok";   // no rule need to apply on this message
			}
			
			public String recvRule(Message recvMsg)
			{
				//System.out.println(recvMsg.toString());
				if(recvRules.isEmpty())
				{
					
					return "ok";
				}
				for(LinkedHashMap<String, Object> t : recvRules)
				{
					boolean targetRule = true;
					if(itemExist("seqNum",t))
					{
						if(((Integer)t.get("seqNum")).intValue() == recvMsg.seq)
						{
							targetRule = (targetRule && true);
						}else{
							continue;
						}
					}

					if(itemExist("dest",t))
					{
						if(((String)t.get("dest")).equals(recvMsg.des))
						{
							targetRule = (targetRule && true);
						}else{
							continue;
						}
					}

					if(itemExist("src",t))
					{
						if(((String)t.get("src")).equals(recvMsg.src))
						{
							targetRule = (targetRule && true);
						}else{
							continue;
						}
					}
					if(itemExist("kind",t))
					{
						if(((String)t.get("kind")).equals(recvMsg.kind))
						{
							targetRule = (targetRule && true);
						}else{
							continue;
						}
					}

					if(itemExist("duplicate",t))
					{
						if(((boolean)t.get("duplicate")) == recvMsg.duplicate)
						{
							targetRule = (targetRule && true);
						}else{
							continue;
						}
					}
					if(targetRule == true)
					{
						System.out.println(((String)t.get("action")));
						return ((String)t.get("action"));
					}
					
				}
				//System.out.println(recvMsg.action);
				return "ok";   // no rule need to apply on this message
			}
			
			
			public static void main(String[] arg) throws FileNotFoundException{
				configFileParse a = new configFileParse("/Users/Moon/Desktop/example.yaml");
				Message t = new Message("alice","alice","process","Lookup",null);
				t.set_seqNum(3);
				t.set_src("charlie");
				System.out.println(a.recvRule(t));
				System.out.println(a.sendRule(t));
			}


			public LinkedHashMap<String, Integer> getAllID() {
				// TODO Auto-generated method stub
				if(NodeInfo==null)
				{
					return null;
				}
				LinkedHashMap<String, Integer> nameID = new LinkedHashMap<String, Integer>();
				int i = 0;
				for(LinkedHashMap<String,Object> tmp : NodeInfo)
				{
					if(!tmp.get("name").equals("logger")){
						nameID.put((String) tmp.get("name"), Integer.valueOf(i++));
					}
				}
				return nameID;
			}
			
			public int getId(String username) {
				// TODO Auto-generated method stub
				if(NodeInfo==null)
				{
					return -1;
				}
				int i = 0;
				for(LinkedHashMap<String,Object> tmp : NodeInfo)
				{
					
					if(tmp.get("name").equals(username)){
						return i;
					}
					i++;
				}
				return -1;
			}
			public int getSize()    // Number of nodes without Logger
			{
				if(NodeInfo == null)
				{
					return 0;
				}
				return NodeInfo.size()-1;
			}
			

}
