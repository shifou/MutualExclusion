
import java.io.*;
import java.util.*;

//import java.lang.*;
import org.yaml.snakeyaml.Yaml;

public class configFileParse {
		
		private List<LinkedHashMap<String ,Object>> NodeInfo;
		private ArrayList<LinkedHashMap<String,Object>> sendRules;
		private ArrayList<LinkedHashMap<String,Object>> recvRules;
		private ArrayList<LinkedHashMap<String,Object>> groups;
		
		@SuppressWarnings("unchecked")
		public configFileParse(String configFile) throws FileNotFoundException {
			  
			    NodeInfo = new ArrayList<LinkedHashMap<String,Object>>();
			    sendRules = new ArrayList<LinkedHashMap<String,Object>>();
			    recvRules = new ArrayList<LinkedHashMap<String,Object>>();
			    groups = new ArrayList<LinkedHashMap<String,Object>>();
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
			  
			}
			
		
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
