package mcmo;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;

public class ChannelOnlineAds implements Channel{

	private static int NUM_KEYWORDS=10;
	double [] cpc=new double[NUM_KEYWORDS];//{1,2,3,4,5,6,7,8,9,10};//CPC (cost-per-click) for each keyword (learned from data)
	double [] numClickFrac=new double[NUM_KEYWORDS];//{0.001, 0.002, 0.003, 0.004, 0.005, 0.006, 0.007, 0.008, 0.009, 0.010};//{4.0, 2.0, 2.0, 1.0, 10.0};//fraction of distinct clicks for each keyword (learned from data)
	double [] x=new double [NUM_KEYWORDS]; //decision variables
	double converRate=0.05; //conversion rate, i.e., #(conversion)/#(click) [+magnitude]
	double numDays=7*13; //number of days of online ads campaign, i.e., x weeks [-duration]
	//NOTE: for each budget we may have to decide optimal number of camapin weeks.
	double popSize;//size of population
	Random random=new Random(2016);//set random seed to replicate result
	double alphaAdjust=0.5;//adjustment for parameter alpha [0.25, 0.75]

	public ChannelOnlineAds(Set<String> set) {
		System.out.println("Channel: onine ads");
		System.out.println(">>Conversion Rate:"+converRate);
		System.out.println("alphaAdjust:"+alphaAdjust);

		popSize=set.size();
		//random generate cpc and numCLickFrac
		//sampleKeywords(cpc, numClickFrac);
		//System.out.println("cpc:"+Arrays.toString(cpc));
		//System.out.println("numClickFrac:"+Arrays.toString(numClickFrac));
	}

	private void sampleKeywords(double[] cpc2, double[] numClickFrac2) {
		for(int i=0; i<this.NUM_KEYWORDS; i++){
			cpc[i]=random.nextDouble()+i;
			numClickFrac[i]=(random.nextDouble()+i)*0.001*alphaAdjust;//adjust [0.5]
		}

		//Assign a static configuration
		//		for(int i=0; i<this.NUM_KEYWORDS; i++){
		//			cpc[i]=1+i;
		//			numClickFrac[i]=(1+i)*0.001;
		//		}

	}

	public double getAdoption(double queryBudget){
		//SELECT optimal number of days		
		int numDayBest=0;
		double adoptionBest=0;

		for(int day=1; day<=numDays; day++){
			double adoption=0;

			//AGGREGATE adoption by day
			for(int d=0;d<day;d++){			
				adoption+=getAdoptionDay(queryBudget/day);	
			}			

			//System.out.println(day+":"+adoption);
			
			//TRACK maximum
			if(adoption>adoptionBest){
				numDayBest=day;
				adoptionBest=adoption;
			}			
		}	

		return adoptionBest;
	}

	public double getAdoptionDay(double dailyBudget){
		double adoption=0;
		double clicks=0;
		double budget=dailyBudget;//Daily budget

		//SAMPLE cpc and numCLickFrac
		sampleKeywords(cpc, numClickFrac);
		//System.out.println("cpc:"+Arrays.toString(cpc));
		//System.out.println("numClickFrac:"+Arrays.toString(numClickFrac));


		//COMPUTE costs
		double [] costs=new double [cpc.length]; 
		//double [] costs={12.0, 1.0, 2.0, 1.0, 4.0}; 

		for(int i=0; i<cpc.length; i++){
			costs[i]=cpc[i]*numClickFrac[i]*popSize;
		}	

		double [] numClick=new double[numClickFrac.length];
		for(int i=0; i<numClick.length; i++){
			numClick[i]=numClickFrac[i]*popSize;
		}

		//Each day, solve a fractional/continuous knapsack problem based on estimated CPC, # of clicks and budget
		//Same campaign runs for "numDays"		
		clicks=fractionalKnapsackSolver(numClick, costs, budget);	

		//Compute estimated conversion
		double conversion=converRate*clicks;
		adoption=(conversion>popSize)?popSize:conversion;		

		return adoption;		
	}



	public double fractionalKnapsackSolver(double [] value, double [] cost, double budget ){
		double optVal=0;
		Double [] efficiencies=new Double [value.length];

		//Compute efficiencies
		for(int i=0; i<value.length;i++){
			efficiencies[i]=-1*value[i]/cost[i];
			//System.out.println(efficiencies[i]);
		}

		//Sort efficiencies and obtain index		
		ArrayIndexComparator comparator = new ArrayIndexComparator(efficiencies);
		Integer[] index = comparator.createIndexArray();
		Arrays.sort(index, comparator);

		//Compute solution using Greedy algorithm
		double [] x = new double[index.length];
		//initialization
		for(int i=0; i<x.length; i++){
			x[i]=0;
		}
		double b=budget;

		for(int i=0; i<index.length; i++){
			if(cost[index[i]]<=b)
				x[index[i]]=1;
			else
				if(cost[index[i]]<=budget) //feasible
					x[index[i]]=b/cost[index[i]];
			b-=x[index[i]]*cost[index[i]];
		}

		for(int i=0; i<x.length; i++){
			optVal+=x[i]*value[i];
			//System.out.println(x[i]);
		}		

		return optVal;	
	}



	//	public double getAdoption(double queryBudget) {
	//		double adoption=0;
	//		double clicks=0;
	//		double budget=queryBudget/numDays;//Daily budget
	//
	//		//COMPUTE costs
	//		double [] costs=new double [cpc.length]; 
	//		//double [] costs={12.0, 1.0, 2.0, 1.0, 4.0}; 
	//
	//		for(int i=0; i<cpc.length; i++){
	//			costs[i]=cpc[i]*numClickFrac[i]*popSize;
	//		}	
	//
	//		double [] numClick=new double[numClickFrac.length];
	//		for(int i=0; i<numClick.length; i++){
	//			numClick[i]=numClickFrac[i]*popSize;
	//		}
	//
	//		//Each day, solve a fractional/continuous knapsack problem based on estimated CPC, # of clicks and budget
	//		//Same campaign runs for "numDays"		
	//		clicks=numDays*fractionalKnapsackSolver(numClick, costs, budget);	
	//
	//		//Compute estimated conversion
	//		double conversion=converRate*clicks;
	//		adoption=(conversion>popSize)?popSize:conversion;		
	//
	//		return adoption;
	//	}

}
