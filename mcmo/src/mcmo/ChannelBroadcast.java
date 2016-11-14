package mcmo;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;

public class ChannelBroadcast implements Channel{
	double marketSize;// market size
	int numOptions=11; //number of options, including (0,0)
	double [] cost;//={0, 2000, 4000, 6000, 8000, 10000}; // A list (5) of pricing packages
	double [] reponseRate;//={0, 1.0/5.0, 2.0/5.0, 3.0/5.0, 4.0/5.0, 5.0/5.0}; // A list (5) of estimated repose rates
	double converRate=0.1;//conversion rate
	double parCurve=0.0002*1; //parameter of right-half s curve, default=1, [0.75, 1.25]
	double numWeeks=13;//number of repeated campaign weeks: 
	double MAX_BUDGET=10000; // maximum budget
	Random ran=new Random(2016);
	
	//IMPLEMENT AS A SAMPLE FORM SOME parameterized S CURVE	

	public ChannelBroadcast(Set<String> set) {
		marketSize=set.size();
		cost=new double[numOptions];
		reponseRate=new double[numOptions];
		sampleCost(cost, numOptions, this.MAX_BUDGET);
		sampleResponseRate(parCurve, reponseRate);
		//System.out.println(Arrays.toString(reponseRate));	
		
		System.out.println("CHANNEL: broadcast");
		System.out.println(">>Conversion Rate:"+converRate);
		System.out.println(">>Curve Parameter:"+parCurve);
		
	}

	private void sampleCost_1(double[] c, double no, double b) {		
		for(int i=0; i<no; i++){
			c[i]=i*b/(no-1);			
		}		
	}
	
	private void sampleCost(double[] c, double no, double b) {		
		c[0]=0;		
		for(int i=1; i<no; i++){
			double c_ran=ran.nextDouble();
			c[i]=c_ran*b;
		}		
	}	
	

	private void sampleResponseRate(double par, double[] rr) {
		for(int i=0; i<cost.length; i++){
			rr[i]=2*(1/(1+Math.exp(-par*cost[i]))-0.5);
			//System.out.println(cost[i]+","+rr[i]);
		}		
		

	}

	//	public double getAdoption(double queryBudget){
	//		double adoption=0;
	//		double response=0;		
	//		int pos=-1;
	//		
	//		//Locate level
	//		for(double c: cost){
	//			if(c<=queryBudget)
	//				pos++;
	//		}		
	//		
	//		response=marketSize*reponseRate[pos];
	//		adoption=response*converRate;
	//		return adoption;		
	//	}	

	public double getAdoptionWeek(double weekLybudget){
		double adoption=0;
		double response=0;		
		int pos=-1;
		double cost_max=Double.NEGATIVE_INFINITY;

		//Locate level
//		for(double c: cost){
//			if(c<=weekLybudget)
//				pos++;
//		}		

		
		for(int i=0; i<cost.length; i++){
			if(weekLybudget>=cost[i]&&cost[i]>=cost_max){
				cost_max=cost[i];
				pos=i;
			}
		}		
		
		if (pos==-1) {
			System.out.println("Wrong!");
		}
		
		response=marketSize*reponseRate[pos];
		adoption=response*converRate;
		return adoption;		

	}

	public double getAdoption(double queryBudget){
		//Choose optimal number of weeks		
		int numWeekBest=0;
		double adoptionBest=0;

		for(int week=1; week<=numWeeks; week++){
			double adoption=0;

			for(int d=0;d<week;d++){			
				adoption+=getAdoptionWeek(queryBudget/week);	
			}			

			//System.out.println(week+":"+adoption);
			if(adoption>adoptionBest){
				numWeekBest=week;
				adoptionBest=adoption;
			}			
		}	

		return adoptionBest;		
	}
}
