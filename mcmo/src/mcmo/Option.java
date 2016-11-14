package mcmo;
/**
 * This class represents a marketing option, i.e., (cost, profit) for a channel.
 * @author zhangh24
 *
 */
public class Option {
	private double cost;
	private double profit;
	private boolean selected;

	public Option(double newCost, double newProfit) {
		this.cost=newCost;
		this.profit=newProfit;
		this.selected=false;
	}
	public double getCost(){
		return this.cost;
	}
	public double getProfit(){
		return this.profit;
	}

	public String toString() { 
		return "("+cost+","+profit+")";
	} 	
	public boolean isSelected(){
		return selected;
	}
	public void select() {
		selected=true;		
	}
	public void unselect(){
		selected=false;
	}
	public void setCost(double c) {
		cost=c;	
	}
}
