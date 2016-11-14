package mcmo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Driver program to implement a Simulated Annealing (SA) algorithm
 * 
 * @author zhangh24
 * 
 */
public class Driver_SA {
	//public static int MAX_ITER; //maximum number of iterations
	public static int BUDGET; //total marketing budget: small(5000), median(10000), large(20000)
	public static String [] CHANNEL_NAMES={"DTD", "OAD", "DML", "BRC"};	
	public static int QUERY_MODE=0; //query mode: 0 [binary], 1[local]
	public static double [] CHANNEL_EXPD=new double [4]; //channel expenditure by channel []
	//public static int mode=0;
	public static Random random;//=new Random(2016);
	public static int max_run_infl=10000;
	public static int visitedMatrix [][]=new int [BUDGET][BUDGET]; 
	public static int iter_max=1; //inner loop
	public static int [] seeds={28,378,53,18,982,880,315,515,253,581,
		9529,4926,5707,5605,1632,3869,6651,3753,1163,9513};

	public static void main(String[] args) throws Exception {
		BUDGET=1000*Integer.valueOf(args[0]); //first parameter budget[2, 4, 6, 8, 10]				
		System.out.println("Budget:"+BUDGET);

		iter_max=Integer.valueOf(args[1]); //second parameter

		//mode=Integer.valueOf(args[1]); //second parameter mode
		//MAX_ITER=Integer.valueOf(args[2]); //third  parameter mode
		//System.out.println("MAX_ITER:"+MAX_ITER);


		//if(mode==0) System.out.println("MODE: Full Option");
		//else if (mode==1) System.out.println("MODE: Single Option");
		//else if (mode==2) System.out.println("MODE: Adaptive Option");

		//System.out.println("MODE: Single Option");
		//System.out.println("MODE: Adaptive Option");


		// LOAD targeted population (from a geographical area)
		Graph<String> roadNet = new Graph<String>(false);
		Graph<String> sociNet = new Graph<String>(true);

		// USE test case (i.e., a simple graph with 7 nodes) or not
		boolean test_case = false;		

		// BUILD a routing network, an undirected graph, using edge-list and node-list files
		if (test_case)
			DataLoader.loadNetwork(roadNet, false, "edges_road_test.csv",
					"nodes_road_test.csv");
		else
			DataLoader.loadNetwork(roadNet, false, "edges_road.csv",
					"nodes_road.csv");

		// BUILD a social network by spatial closeness
		if (test_case)
			DataLoader.loadNetwork(sociNet, true, "edges_soci_test.csv",
					"nodes_soci_test.csv");
		else
			DataLoader.loadNetwork(sociNet, true, "edges_soci.csv",
					"nodes_soci.csv");

		//Verify DataLoader
		//System.out.println(roadNet.toString());
		//System.out.println(sociNet.toString());	


		//Thresholds

		// SET UP channels
		//Channel 1: Door-to-door marketing
		ChannelDoorToDoor channelDoorToDoor=new ChannelDoorToDoor(sociNet, roadNet, test_case);		

		//Channel 2: online advertising, i.e., search ads
		ChannelOnlineAds channelOnlineAds=new ChannelOnlineAds(sociNet.getVertexList().keySet());

		//Channel 3: Direct Mailing Marketing
		ChannelDirectMail channelDirectMail=new ChannelDirectMail(sociNet.getVertexList().keySet());

		//Channel 4: Broadcast Marketing, i.e., tv or radio
		ChannelBroadcast channelBroadcast=new ChannelBroadcast(sociNet.getVertexList().keySet());			


		//Timer-start:
		long t0 = System.nanoTime();

		int ws=Integer.valueOf(args[2]); //0-9
		random=new Random(seeds[ws]);
		System.out.println("SA seed:"+ws+"-"+seeds[ws]);		

		//Generate a random solution s0=(b1, b2, b3, b4)
		double payoff_max=0; //payoff of best so far
		//double [] alloc_max=new double [4]; //state of best payoff so far

		double payoff=0; //payoff of current state
		double [] alloc=new double [4];  //current state

		//Initialization
		generateRandomSolution(alloc, BUDGET);
		payoff=(int)( (channelDoorToDoor.getAdoption(alloc[0])+
				channelOnlineAds.getAdoption(alloc[1])+
				channelDirectMail.getAdoption(alloc[2])+
				channelBroadcast.getAdoption(alloc[3])));

		payoff_max=payoff;		
		CHANNEL_EXPD[0]=alloc[0];
		CHANNEL_EXPD[1]=alloc[1];
		CHANNEL_EXPD[2]=alloc[2];
		CHANNEL_EXPD[3]=alloc[3];
		
		System.out.println("Starting state:"+Arrays.toString(alloc));		
		System.out.println("Starting payoff:"+payoff);	
		
		//System.out.println(Arrays.toString(CHANNEL_EXPD));
		//System.out.println(payoff);

		double tem=1; //starting temperature
		double tem_min=0.00001; //minimum temperature
		double alpha=0.94; //rate of cooling-down [0.9, 0.92, [0.94], 0.96, 0.98]
		int iter=0;
		double adjust=sociNet.getVertexList().keySet().size();

		System.out.println("SA alpha:"+alpha);				
		System.out.println("Max Inner Loop:"+iter_max);
		System.out.println("Temperature adjustment:"+adjust);
		System.out.println("iteration, payoff, time, expd_dtd, expd_oad, expd_dml, expd_brc");

		
		while(tem>tem_min){
			double i=0;
			while (i<iter_max){ //inner loop
				double [] alloc_new=new double [4];				
				alloc_new=getNeighbor_uniform(alloc, BUDGET);
				
				//new_alloc=getNeighbor_skew(alloc, BUDGET);
				//System.out.println(Arrays.toString(new_alloc));	

				double payoff_new=(int)( (channelDoorToDoor.getAdoption(alloc_new[0])+
						channelOnlineAds.getAdoption(alloc_new[1])+
						channelDirectMail.getAdoption(alloc_new[2])+
						channelBroadcast.getAdoption(alloc_new[3])));
				
				double acceptanceRate=Math.exp((payoff_new-payoff)/(tem*adjust));
				//System.out.println("L_a:"+acceptanceRate);
				
				if(payoff_new>=payoff){						
					alloc=alloc_new;
					payoff=payoff_new;
					//System.out.print("Better Move:");
					
					if(payoff>payoff_max){
						payoff_max=payoff;						
						CHANNEL_EXPD[0]=alloc[0];
						CHANNEL_EXPD[1]=alloc[1];
						CHANNEL_EXPD[2]=alloc[2];
						CHANNEL_EXPD[3]=alloc[3];						
					}
											
				}else if(random.nextDouble()<acceptanceRate){
					alloc=alloc_new;
					payoff=payoff_new;
					//System.out.print("Worse Move:");
				}else{
					//System.out.print("Not Move:");
				}
				
				//System.out.println(Arrays.toString(alloc));

				i+=1;
			}

			long t2=System.nanoTime();			
			System.out.println(iter+"," +payoff_max+","+ (t2-t0)/1e+9 +","+ CHANNEL_EXPD[0]+","+ CHANNEL_EXPD[1]+"," +CHANNEL_EXPD[2]+"," +CHANNEL_EXPD[3]);

			tem*=alpha;
			iter++;			
		}
	}


	private static double[] getNeighbor_uniform(double[] alloc, int bgt) {
		double [] new_nb=new double [4];
		generateRandomSolution(new_nb, bgt);	//equal chance to pick any neighbor		
		return new_nb;
	}

	private static double[] getNeighbor_skew(double[] alloc, int bgt) {
		double [] new_nb=new double [4];
		double coef=1; //coefficient
		while(true){//the nearer the more likely to be picked
			generateRandomSolution(new_nb, bgt);	
			double dist=Math.sqrt(((Math.pow(Math.log(new_nb[0]/alloc[0]),2)+ //log distance of two vectors
					Math.pow(Math.log(new_nb[1]/alloc[1]),2)+
					Math.pow(Math.log(new_nb[2]/alloc[2]), 2)+
					Math.pow(Math.log(new_nb[3]/alloc[3]), 2))/4));
			double thres=Math.exp(-coef*dist);
			//System.out.println(dist+","+thres);
			double ran=random.nextDouble();
			//System.out.println(ran+"<"+thres+"?");
			if(ran<thres)
				break;
		}
		return new_nb;
	}


	private static void generateRandomSolution(double[] alloc, int bgt) {
		double [] allocPlan =alloc;			

		int budget=BUDGET;

		for(int i=0; i<alloc.length; i++){
			if(i==alloc.length-1){
				allocPlan[i]=budget;
			}else{
				allocPlan[i]=(int) (budget*random.nextDouble());
				budget-=allocPlan[i];
			}				
		}			
	}	
}
