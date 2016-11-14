package mcmo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Driver program to implement the Iterative Budgeting (IB) algorithm
 * 
 * @author zhangh24
 * 
 */
public class Driver {
	public static int MAX_ITER; //maximum number of iterations
	public static int BUDGET; //total marketing budget: small(5000), median(10000), large(20000)
	public static String [] CHANNEL_NAMES={"DTD", "OAD", "DML", "BRC"};	
	public static int QUERY_MODE=0; //query mode: 0 [binary], 1[local]
	public static double [] CHANNEL_EXPD=new double [4]; //channel expenditure by channel []
	public static int mode=0;
	public static Random random=new Random(2016);
	public static int max_run_infl=0;

	public static void main(String[] args) throws Exception {
		BUDGET=1000*Integer.valueOf(args[0]); //first parameter budget[2, 4, 6, 8, 10]				
		System.out.println("Budget:"+BUDGET);

		mode=Integer.valueOf(args[1]); //second parameter mode
		MAX_ITER=Integer.valueOf(args[2]); //third  parameter mode
		System.out.println("MAX_ITER:"+MAX_ITER);


		if(mode==0) System.out.println("MODE: Full Option");
		else if (mode==1) System.out.println("MODE: Single Option");
		else if (mode==2) System.out.println("MODE: Adaptive Option");

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


		//Query results: lists of options, i.e., cost-profit pairs
		//		ArrayList<Option> optionsDtd=new ArrayList <Option> ();
		//		ArrayList<Option> optionsOad=new ArrayList <Option> ();
		//		ArrayList<Option> optionsDml=new ArrayList <Option> ();
		//		ArrayList<Option> optionsBrc=new ArrayList <Option> ();

		//Query results: list of thresholds, i.e., two lists: one for Lower Bounds, and one for Upper Bounds.
		ArrayList<Option> optionsDtdLb=new ArrayList <Option> ();
		ArrayList<Option> optionsOadLb=new ArrayList <Option> ();
		ArrayList<Option> optionsDmlLb=new ArrayList <Option> ();
		ArrayList<Option> optionsBrcLb=new ArrayList <Option> ();	

		ArrayList<Option> optionsDtdUb=new ArrayList <Option> ();
		ArrayList<Option> optionsOadUb=new ArrayList <Option> ();
		ArrayList<Option> optionsDmlUb=new ArrayList <Option> ();
		ArrayList<Option> optionsBrcUb=new ArrayList <Option> ();	


		//Timer-start:
		long t0 = System.nanoTime();

		//SETUP initial set of queries
		sendInitialQuery(optionsDtdLb, optionsDtdUb, channelDoorToDoor);
		//System.out.println("Door-to-door[LB]:\n"+Arrays.toString(optionsDtdLb.toArray()));
		//System.out.println("Door-to-door[UB]:\n"+Arrays.toString(optionsDtdUb.toArray()));

		//test max_run
		//for(int i=0; i<20; i+=1){
		//	max_run_infl+=2000;
		//	sendInitialQuery(optionsDtdLb, optionsDtdUb, channelDoorToDoor);
		//}


		sendInitialQuery(optionsOadLb, optionsOadUb, channelOnlineAds);
		//System.out.println("Online Ads[LB]:\n"+Arrays.toString(optionsOadLb.toArray()));
		//System.out.println("Online Ads[UB]:\n"+Arrays.toString(optionsOadUb.toArray()));

		sendInitialQuery(optionsDmlLb, optionsDmlUb, channelDirectMail);
		//System.out.println("Direct Mail[LB]:\n"+Arrays.toString(optionsDmlLb.toArray()));
		//System.out.println("Direct Mail[UB]:\n"+Arrays.toString(optionsDmlUb.toArray()));

		sendInitialQuery(optionsBrcLb, optionsBrcUb, channelBroadcast);
		//System.out.println("Broadcast[LB]:\n"+Arrays.toString(optionsBrcLb.toArray()));
		//System.out.println("Broadcast[UB]:\n"+Arrays.toString(optionsBrcUb.toArray()));

		System.out.println("iteration, payoff, payoff_next, t1, t2, expd_dtd, expd_oad, expd_dml, expd_brc");


		// MAIN Loop: iterative solver
		for(int i=0; i<MAX_ITER; i++){
			//SOLVE an instance:using branch and bound algorithm

			//list of channels, each is a list of options, i.e., upper bounds
			ArrayList <ArrayList<Option>> chanelOptions=new ArrayList <ArrayList<Option>> ();	

			chanelOptions.add(optionsDtdUb);
			chanelOptions.add(optionsOadUb);
			chanelOptions.add(optionsDmlUb);
			chanelOptions.add(optionsBrcUb);

			MckpSolver solver = new MckpSolver(chanelOptions,BUDGET);
			double curOptimum=solver.solveByJavaILP();			
			long t1=System.nanoTime();

			String channel_expd=new String();
			for(int c=0; c<4; c++){
				channel_expd+=(","+CHANNEL_EXPD[c]);
			}

			//System.out.println(i+":"+solver.solveByJavaILP()+","+(System.nanoTime()-t0)/1e+9);
			//System.out.println(i+":"+solver.solveByCPLEX());

			//UPDATE Queries: seek cheaper option for given profit level
			//Door-to-Door:0
			//if(QUERY_MODE==0) updateOptions(optionsDtd, channelDoorToDoor);
			//else updateOptionsLocal(optionsDtd, channelDoorToDoor);
			//			updateThresholds(optionsDtdLb, optionsDtdUb, channelDoorToDoor);
			//			System.out.println("Door-to-door:\n"+Arrays.toString(optionsDtdLb.toArray()));
			//			System.out.println("Door-to-door:\n"+Arrays.toString(optionsDtdUb.toArray()));	
			//System.out.println("Door-to-door:\n"+Arrays.toString(optionsDtd.toArray()));

			//Online Ads:1
			//			if(QUERY_MODE==0) updateOptions(optionsOad, channelOnlineAds);
			//			else updateOptionsLocal(optionsOad, channelOnlineAds);

			//System.out.println("Online Ads:\n"+Arrays.toString(optionsOad.toArray()));

			//Direct Mail:2
			//			if(QUERY_MODE==0) updateOptions(optionsDml, channelDirectMail);
			//			else updateOptionsLocal(optionsDml, channelDirectMail);
			//System.out.println("Direct Mail:\n"+Arrays.toString(optionsDml.toArray()));

			//Broadcast:3
			//			if(QUERY_MODE==0) updateOptions(optionsBrc, channelBroadcast);
			//			else updateOptionsLocal(optionsBrc, channelBroadcast);
			//System.out.println("Broadcast:\n"+Arrays.toString(optionsBrc.toArray()));		

			//Forward Checking to obtain an upper bound
			//New upper bounds
			ArrayList<Option> optionsDtdUbNew=desireUpperBounds(optionsDtdUb, optionsDtdLb);
			ArrayList<Option> optionsOadUbNew=desireUpperBounds(optionsOadUb, optionsOadLb);
			ArrayList<Option> optionsDmlUbNew=desireUpperBounds(optionsDmlUb, optionsDmlLb);
			ArrayList<Option> optionsBrcUbNew=desireUpperBounds(optionsBrcUb, optionsBrcLb);
			ArrayList <ArrayList<Option>> chanelOptionsDesired=new ArrayList <ArrayList<Option>> ();
			chanelOptionsDesired.add(optionsDtdUbNew);
			chanelOptionsDesired.add(optionsOadUbNew);
			chanelOptionsDesired.add(optionsDmlUbNew);
			chanelOptionsDesired.add(optionsBrcUbNew);

			MckpSolver solverNew = new MckpSolver(chanelOptionsDesired,BUDGET);	
			double besOptimum=solverNew.solveByJavaILP();	//best optimum next iteration 		
			long t2=System.nanoTime();

			//System.out.println("("+(i+1)+"):"+solverNew.solveByJavaILP()+","+(System.nanoTime()-t0)/1e+9);			
			System.out.print(i+","+curOptimum+","+besOptimum+","+(t1-t0)/1e+9+","+(t2-t0)/1e+9);
			System.out.println(channel_expd);

			//Difference between current value and lookahead value
			double deltaVal=besOptimum-curOptimum;
			double coef=1;//show by sensitive analysis.
			double slopeRate=deltaVal/(coef*(i+1));//slope
			//System.out.println(deltaVal+","+slopeRate);			

			//UPDATE queries
			//ONE
			//System.out.println("dtd->");
			if(mode==0){
				updateThresholds(optionsDtdLb, optionsDtdUb, channelDoorToDoor);
			}else if(mode==1){
				updateThresholdsSelected(optionsDtdLb, optionsDtdUb, channelDoorToDoor, optionsDtdUbNew);
			}else if(mode==2){
				updateThresholdsAdaptive(optionsDtdLb, optionsDtdUb, channelDoorToDoor, optionsDtdUbNew, slopeRate);
			}
			//System.out.println("Door-to-Door:\n"+Arrays.toString(optionsDtdLb.toArray()));
			//System.out.println("Door-to-Door:\n"+Arrays.toString(optionsDtdUb.toArray()));

			//TWO
			//System.out.println("oad->");
			if(mode==0){
				updateThresholds(optionsOadLb, optionsOadUb, channelOnlineAds);
			}else if(mode==1){
				updateThresholdsSelected(optionsOadLb, optionsOadUb, channelOnlineAds, optionsOadUbNew);
			}else if(mode==2){
				updateThresholdsAdaptive(optionsOadLb, optionsOadUb, channelOnlineAds, optionsOadUbNew, slopeRate);
			}
			//System.out.println("Online Ads:\n"+Arrays.toString(optionsOadLb.toArray()));
			//System.out.println("Online Ads:\n"+Arrays.toString(optionsOadUb.toArray()));

			//THREE
			//System.out.println("dml->");
			if(mode==0){
				updateThresholds(optionsDmlLb, optionsDmlUb, channelDirectMail);
			}else if(mode==1){
				updateThresholdsSelected(optionsDmlLb, optionsDmlUb, channelDirectMail, optionsDmlUbNew);
			}else if(mode==2){
				updateThresholdsAdaptive(optionsDmlLb, optionsDmlUb, channelDirectMail, optionsDmlUbNew, slopeRate);
			}
			//System.out.println("Direct Mail:\n"+Arrays.toString(optionsDmlLb.toArray()));
			//System.out.println("Direct Mail:\n"+Arrays.toString(optionsDmlUb.toArray()));			

			//FOUR
			//System.out.println("brc->");
			if(mode==0){
				updateThresholds(optionsBrcLb, optionsBrcUb, channelBroadcast);
			}else if(mode==1){
				updateThresholdsSelected(optionsBrcLb, optionsBrcUb, channelBroadcast, optionsBrcUbNew);				
			}else if(mode==2){
				updateThresholdsAdaptive(optionsBrcLb, optionsBrcUb, channelBroadcast, optionsBrcUbNew, slopeRate);
			}
			//System.out.println("Broadcast:\n"+Arrays.toString(optionsBrcLb.toArray()));
			//System.out.println("Broadcast:\n"+Arrays.toString(optionsBrcUb.toArray()));

			//TERMINATE when solutions are sufficiently close	

		}
	}



	private static ArrayList<Option> desireUpperBounds(ArrayList<Option> ubs, ArrayList<Option> lbs) {
		ArrayList<Option> ubsNew=new ArrayList<Option>();
		//Construct upper bounds, i.e., w_new=0.5*(w_l+w_u), p_new=p_u
		for(int i=0; i<ubs.size(); i++){
			double lbc=lbs.get(i).getCost();
			double ubc=ubs.get(i).getCost();
			if(mode==1) 
				ubsNew.add(new Option((lbc+ubc)*0.5, ubs.get(i).getProfit())); //[HBQ]
			else if(mode==0)
				ubsNew.add(new Option(lbc, ubs.get(i).getProfit())); //Use lower cost and upper payoff [GBQ]
		}		

		return ubsNew;
	}

	private static void updateThresholds(ArrayList<Option> lbs, ArrayList<Option> ubs, Channel channel) throws Exception {
		int pos=1; //position, skip first threshold. 

		while(pos<lbs.size()){//process lbs an ubs simultaneously
			double newCost=0.5*(lbs.get(pos).getCost()+ubs.get(pos).getCost());
			long t0 = System.nanoTime();
			double newProfit=(int) channel.getAdoption(newCost);	
			long t1 = System.nanoTime();
			//System.out.println(newCost+","+newProfit+","+(t1-t0)/1.0e+9);

			//System.out.println("["+lbs.get(pos).getProfit()+","+ubs.get(pos).getProfit()+"]:"+newProfit+"("+newCost+")");

			if (newProfit==lbs.get(pos).getProfit()) {//Case 1: update lower bound
				lbs.remove(pos);
				lbs.add(pos,new Option(newCost, newProfit));
				pos++;
			}else if (newProfit==ubs.get(pos).getProfit()){//Case 2: update upper bound
				ubs.remove(pos);
				ubs.add(pos, new Option(newCost, newProfit));
				pos++;
			}else{ //Case 3: new threshold, [f(lb), f(ub)]
				ubs.add(pos,new Option(newCost, newProfit));
				lbs.add(pos+1,new Option(newCost, newProfit));
				pos=pos+2;
			}			
		}		
	}


	private static void updateThresholdsAdaptive(ArrayList<Option> lbs, ArrayList<Option> ubs, Channel channel, ArrayList<Option> ubsNew, double slopeRate) throws Exception {
		int pos=1; //position, skip first threshold. 
		double prob=Math.exp(-slopeRate); //map rate of slope falling to a probability

		while(pos<lbs.size()){//process lbs an ubs simultaneously
			//System.out.println(pos);
			//Only update threshold that seems promising with a prob determined by slopeRate!
			double draw=random.nextDouble();
			System.out.println(draw+"<"+prob+"?");
			if(!(ubsNew.get(pos).isSelected())&&draw<prob){
				pos++;
				continue;
			}

			//&&random.nextDouble()<prob)

			double newCost=0.5*(lbs.get(pos).getCost()+ubs.get(pos).getCost());
			long t0 = System.nanoTime();
			double newProfit=(int) channel.getAdoption(newCost);	
			long t1 = System.nanoTime();
			System.out.println(newCost+","+newProfit+","+(t1-t0)/1.0e+9);

			if (newProfit==lbs.get(pos).getProfit()) {//Case 1: update lower bound
				lbs.remove(pos);
				lbs.add(pos,new Option(newCost, newProfit));
				pos++;
			}else if (newProfit==ubs.get(pos).getProfit()){//Case 2: update upper bound
				ubs.remove(pos);
				ubs.add(pos, new Option(newCost, newProfit));
				pos++;
			}else { //if (newProfit>lbs.get(pos).getProfit()&&newProfit<ubs.get(pos).getProfit()) { //Case 3: new threshold
				ubs.add(pos,new Option(newCost, newProfit));
				lbs.add(pos+1,new Option(newCost, newProfit));
				ubsNew.add(pos, new Option(-1, -1)); //ADD a dummy node intentionally to make it same length as the other two lists. 
				pos=pos+2;
			}
		}	

	}


	private static void updateThresholdsSelected(ArrayList<Option> lbs, ArrayList<Option> ubs, Channel channel, ArrayList<Option> ubsNew) throws Exception {
		int pos=1; //position, skip first threshold. 

		while(pos<lbs.size()){//process lbs an ubs simultaneously
			//System.out.println(pos);
			//Only update threshold that seems promising!
			if(!ubsNew.get(pos).isSelected()){
				pos++;
				continue;
			}

			double newCost=0.5*(lbs.get(pos).getCost()+ubs.get(pos).getCost());
			long t0 = System.nanoTime();
			double newProfit=(int) channel.getAdoption(newCost);	
			long t1 = System.nanoTime();
			//System.out.println(newCost+","+newProfit+","+(t1-t0)/1.0e+9);

			if (newProfit==lbs.get(pos).getProfit()) {//Case 1: update lower bound
				lbs.remove(pos);
				lbs.add(pos,new Option(newCost, newProfit));
				pos++;
			}else if (newProfit==ubs.get(pos).getProfit()){//Case 2: update upper bound
				ubs.remove(pos);
				ubs.add(pos, new Option(newCost, newProfit));
				pos++;
			}else {// if (newProfit>lbs.get(pos).getProfit()&&newProfit<ubs.get(pos).getProfit()){ //Case 3: new threshold
				ubs.add(pos,new Option(newCost, newProfit));
				lbs.add(pos+1,new Option(newCost, newProfit));
				ubsNew.add(pos, new Option(-1, -1)); //ADD a dummy node intentionally to make it same length as the other two lists. 
				pos=pos+2;
			}			
		}			
	}


	private static void sendInitialQuery(ArrayList<Option> lbs, ArrayList<Option> ubs, Channel channel) throws Exception {
		for(int b=0; b<=BUDGET;b+=BUDGET){//modified [b==0]
			long t0 = System.nanoTime();
			double r=(int) channel.getAdoption(b);
			long t1 = System.nanoTime();
			System.out.println(b+","+r+","+(t1-t0)/1.0e+9);
			if(b==0) { //Modified [b==0]
				lbs.add(new Option(b, (int)r));
				ubs.add(new Option(b, (int)r));
			}
			if(r>ubs.get(ubs.size()-1).getProfit()){
				lbs.add(ubs.get(ubs.size()-1));
				ubs.add(new Option(b, (int)r));
			}				

			//optionsDtd.add(new Option(b, (int)r));
		}
	}

	private static void updateOptions(ArrayList<Option> options, Channel channel) throws Exception {
		int pos=0; //position

		while(pos<(options.size()-1)){
			double newCost=0.5*(options.get(pos).getCost()+options.get(pos+1).getCost());
			long t0 = System.nanoTime();
			double newProfit=(int) channel.getAdoption(newCost);	
			long t1 = System.nanoTime();
			System.out.println(newCost+","+newProfit+","+(t1-t0)/1.0e+9);
			options.add(pos+1,new Option(newCost, newProfit));
			pos=pos+2;
		}
	}


	private static void updateOptionsLocal(ArrayList<Option> options, Channel channel) throws Exception {
		int pos=0; //position

		while(pos<(options.size()-1)){
			//create new queries only around current best option 
			if(options.get(pos).isSelected()||options.get(pos+1).isSelected()){
				double newCost=0.5*(options.get(pos).getCost()+options.get(pos+1).getCost());
				long t0 = System.nanoTime();
				double newProfit=(int) channel.getAdoption(newCost);	
				long t1 = System.nanoTime();
				//System.out.println(newCost+","+newProfit+","+(t1-t0)/1.0e+9);
				options.add(pos+1,new Option(newCost, newProfit));
				pos=pos+2;
			}else	pos++;
		}
	}	
}
