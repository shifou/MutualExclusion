import java.io.Serializable;


public class LogicalTimeStamp implements Serializable{

	private static final long serialVersionUID = 3185670617192875966L;
		private int Logical;

		
		public LogicalTimeStamp(){
			setLogical(0);	
		}

		public int getLogical() {
			return Logical;
		}
		public void setLogical(int logical) {
			Logical = logical;
		}
		
		public void Increment()
		{
			Logical++;
		}
		
		public void updateTimeStamp(LogicalTimeStamp base)
		{
			Logical = Math.max(Logical, base.getLogical());
		}
		
		public int issueTimeStamp()
		{
			return Logical+1;
		}
		
		public String Compared(Message eventA, Message eventB)
		{
			if(eventA.lt.getLogical() < eventB.lt.getLogical())
			{
				return "happend before";
			}else{
				return null;
			}
		}
		public String toString()
		{
			return Integer.toString(Logical);
		}
}
