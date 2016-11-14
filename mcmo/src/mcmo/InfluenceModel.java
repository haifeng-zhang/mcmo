package mcmo;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import mcmo.Driver_testConcurrent.Sum;

public class InfluenceModel<V> {

	Graph<V> socialNetwork; //Underlying social network structure
	private final int MAX_STAGES=13; //max number of stages, higher and larger variance of expected influence, 1stage=1day
	private int MAX_RUNS; //max number of sample model runs, higher and smaller variance of expected influence: [10000]
	private final double ACT_PROB=0.1; //activation probability [default=0.2]
	//private Random random = new Random(2016); //set random seed to replicate result
	public static int seed_public=2000;
	//NOTE: 10 is never enough/robust
	//AAAI2016:100
	//jair: 1000 [compare]
	//jair: [opt k]dtd:10000, rg:5000
	//aaai2017: act_prob:[0.1, 0.2, 0.3, 0.4, 0.5]; max_runs:[2000, 2000, 650, 500, 400]

	public InfluenceModel(Graph <V> graph) {
		MAX_RUNS=20000;

		System.out.println(">>MAX_RUNS:"+MAX_RUNS);
		System.out.println(">>ACT_PROB:"+ACT_PROB);

		socialNetwork=graph;

		//MAX_RUNS=2000+(Driver.BUDGET-2000)*(50000-2000)/(10000-2000); //Variational Runs: [2000, 2000], [10000, 50000]
	}

	//	public int getInfluence_v1(HashSet <V> active_nodes){
	//		int influence=active_nodes.size();
	//
	//		//System.out.println("~~~~~~~~Stage:0~~~~~~~~~");
	//		//System.out.println("Seeds:"+active_nodes.toString());
	//
	//		//ASSIGN states of all vertices according the list of active nodes
	//		for (Vertex vertex : socialNetwork.getVertexList().values()) {			
	//			if(active_nodes.contains(vertex.getID())) vertex.activate();
	//			else vertex.reset();
	//		}		
	//
	//		//IMPLEMENT the independent cascade process
	//		for (int t=0; t<this.MAX_STAGES; t++){	
	//
	//			//System.out.println("ITR:"+t);
	//
	//
	//			//UPDATE states stage by stage discretely
	//			for (Vertex vertex : socialNetwork.getVertexList().values()) {			
	//				vertex.updateState();
	//				//System.out.println(vertex.getID()+":"+vertex.is_activated_curr()+","+vertex.is_activated_next()+","+vertex.is_capable());
	//
	//			}	
	//
	//			//System.out.println("~~~~~~~~Stage:"+(t+1)+"~~~~~~~~~");
	//
	//			for (Vertex vertex : socialNetwork.getVertexList().values()) {
	//
	//				//System.out.println("Current Vertex:"+vertex.getID());
	//
	//				if(!vertex.is_activated_curr()) { // if not been activated
	//
	//					ArrayList <V> nb_IDs= socialNetwork.getAdjacentVertices((V) vertex.getID());
	//					//System.out.println(nb_IDs.toString());
	//
	//					for (V v_id: nb_IDs){ //for each adjacent vertex
	//						//System.out.println(">>Neighbor:"+v_id);
	//
	//						Vertex v=socialNetwork.getVertexList().get(v_id); //get actual node object thru its id
	//						//System.out.println(v.is_activated_curr()+","+v.is_activated_next()+","+v.is_capable());
	//
	//
	//						double prob;// = socialNetwork.getDistanceBetween(v_id, (V) vertex.getID()); //get edge weight, i.e. activation probability
	//						//NEED A FAST REPRENTATION to get weight...
	//						//TEST ONLY prob=0.5, 0.8
	//						prob=ACT_PROB;//0.1[SAVED]
	//
	//						//System.out.println("Activation Prob:"+prob);
	//
	//						if (v.is_capable()) { // if its neighbor is active
	//
	//							//Get activated at a chance determined by the weight
	//							//Random rn = new Random(System.nanoTime());
	//							double rn_db=random.nextDouble();
	//							//System.out.println("Random Draw:"+rn_db);
	//
	//							if (rn_db<=prob) {
	//								//System.out.println("ACTIVATED!!!");
	//								if(!vertex.is_activated_next()) influence++; 
	//								vertex.activate();
	//								//System.out.println(vertex.is_activated_curr()+","+vertex.is_activated_next()+","+vertex.is_capable());
	//
	//								//influence++;
	//							}						
	//						}					
	//					}			
	//				}			
	//			}
	//
	//			//MAKE newly activated vertices inactive
	//			for (Vertex vertex : socialNetwork.getVertexList().values()) {			
	//				if (vertex.is_capable()) vertex.makeIncapable(); // its neighbor becomes inactive once acts	
	//			}		
	//
	//		}
	//
	//		return influence;
	//	}


	//	public int getInfluence_v2(HashSet <V> active_nodes){
	//		int influence=active_nodes.size();
	//
	//		//System.out.println("~~~~~~~~Stage:0~~~~~~~~~");
	//		//System.out.println("Seeds:"+active_nodes.toString());
	//
	//		//ASSIGN states of all vertices according the list of active nodes
	//		for (Vertex vertex : socialNetwork.getVertexList().values()) {			
	//			if(active_nodes.contains(vertex.getID())) {
	//				vertex.activate();
	//			}
	//			else vertex.reset();
	//		}		
	//
	//		//IMPLEMENT the independent cascade process
	//		for (int t=0; t<this.MAX_STAGES; t++){	
	//			//System.out.println("ITR:"+t);
	//
	//			ArrayList <Vertex> actors=new ArrayList(); //List of active nodes that are capable to active neighbors
	//
	//			//UPDATE states stage by stage discretely
	//			for (Vertex vertex : socialNetwork.getVertexList().values()) {			
	//				vertex.updateState();
	//				if(vertex.is_capable()) actors.add(vertex);
	//			}
	//
	//			//System.out.println("ACTr:"+actors);
	//
	//
	//			//IF NO MORE CAPABLE NODES: BREAK
	//			if(actors.size()==0) break;
	//
	//			//System.out.println("~~~~~~~~Stage:"+(t+1)+"~~~~~~~~~");
	//			for(Vertex actor:actors){ //each actor activates its neighbors
	//				//System.out.println("Current actor:"+actor.getID());
	//
	//
	//				ArrayList <V> nb_IDs= socialNetwork.getAdjacentVertices((V) actor.getID());//Neighbors
	//
	//				for (V nb_id: nb_IDs){ //for each adjacent vertex
	//					//System.out.println(">>Neighbor:"+nb_id);
	//
	//					Vertex nb=socialNetwork.getVertexList().get(nb_id); //get actual node object thru its id
	//					//System.out.println("is_capable:"+v.is_capable());
	//					//System.out.println(nb.is_activated_curr()+","+nb.is_activated_next()+","+nb.is_capable());					
	//
	//					if(!nb.is_activated_curr()&&!nb.is_activated_next()){
	//
	//						double prob;// = socialNetwork.getDistanceBetween(v_id, (V) vertex.getID()); //get edge weight, i.e. activation probability
	//						//NOTE: A FAST METHOD/representation may be needed to get weight...
	//						//TEST ONLY prob=0.5, 0.8
	//						prob=ACT_PROB;
	//
	//						//Get activated at a chance determined by the weight
	//						//Random rn = new Random(System.nanoTime());
	//						double rn_db=random.nextDouble();
	//						//System.out.println("Random Draw:"+rn_db);
	//
	//						if (rn_db<=prob) {
	//							//System.out.println("ACTIVATED!!!");
	//							influence++; 
	//							nb.activate();
	//							//System.out.println(nb.is_activated_curr()+","+nb.is_activated_next()+","+nb.is_capable());
	//						}						
	//					}										
	//				}
	//			}		
	//
	//			//MAKE newly activated vertices inactive
	//			for (Vertex actor:actors) {			
	//				actor.makeIncapable(); // its neighbor becomes inactive once acts
	//				//System.out.println(actor);
	//			}		
	//
	//		}
	//
	//		return influence;
	//	}

	/**
	 * An implementation of the IC model, which is expected to be more efficient than v1 and v2 and designed to be called in a parallel fashion.
	 * @param active_nodes list of names of active node
	 * @return influence simulated by the IC model
	 */
	//	public int getInfluence_v3(HashSet <V> active_nodes){
	//		int influence=active_nodes.size();
	//		HashSet <V> activeNodes=new HashSet (active_nodes);//active nodes
	//		HashSet <V> removedNodes=new HashSet(); //nodes no longer active
	//		
	//		for (int t=0; t<this.MAX_STAGES; t++){	//Each step, newly active nodes infect uninfected neighbors stochastically
	//			HashSet <V> newInfected=new HashSet();//newly infected nodes			
	//			for(V v: activeNodes){
	//				ArrayList <V> nbs= socialNetwork.getAdjacentVertices(v);//Neighbors
	//				for (V nb: nbs){ //for each neighboring vertex
	//					//that is not infected
	//					if(!(activeNodes.contains(nb)||removedNodes.contains(nb))){
	//						double ran=random.nextDouble();
	//						if(ran<=this.ACT_PROB){
	//							influence++; 
	//							newInfected.add(nb);
	//						}	
	//					}								
	//				}				
	//			}
	//			removedNodes.addAll(activeNodes);
	//			activeNodes.clear();
	//			activeNodes.addAll(newInfected);
	//		}	
	//		
	//		
	//		return influence;
	//	}


	/**
	 * Compute expected influence of multiple model runs
	 * @param active_nodes active nodes initially specified
	 * @return expected influence of multiple model runs
	 */
	//	public double getExpectedInfluence_old(HashSet <V> active_nodes){
	//		double expectedInfluence=0;
	//		HashSet <V> social_active_nodes=this.filter(active_nodes,socialNetwork);
	//		//System.out.println(social_active_nodes);
	//		for (int r=0; r<this.MAX_RUNS; r++){
	//			//int delta_inf=this.getInfluence_v2(social_active_nodes); //[Note: AIJ uses this!]
	//			int delta_inf=this.getInfluence_v3(social_active_nodes);
	//
	//			//System.out.println(r+":"+delta_inf);
	//			expectedInfluence+=delta_inf;		
	//		}
	//
	//		//System.out.println(active_nodes);
	//		return expectedInfluence/this.MAX_RUNS;		
	//	}

	/**
	 * A multi-threading implementation to compute expected influence
	 * @param active_nodes
	 * @return
	 * @throws Exception
	 */
	public double getExpectedInfluence(HashSet <V> active_nodes) throws Exception{
		//MAX_RUNS=Driver.max_run_infl;//[TEST] varying runs test
		//System.out.println(MAX_RUNS);

		double expectedInfluence=0;
		int numThreads=4;//adjust with reference to local CPU cores [local: 4; server: 16/16]
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		ArrayList taskList=new ArrayList();
		int nt=4; //number of tasks

		for(int i=0; i<nt; i++){
			taskList.add(new Sum(active_nodes, MAX_RUNS/nt));
		}		

		List <Future<Double>> results = executor.invokeAll(taskList);
		executor.shutdown();

		for (Future<Double> result : results) {
			expectedInfluence+=result.get();
		}		

		return expectedInfluence/this.MAX_RUNS;	
	}

	public class Sum implements Callable<Double> {
		private final HashSet activeNodes;
		private final int maxRun;
		private final Random random;//each use own random
		Sum(HashSet as, int mr) {
			this.activeNodes = as;
			this.maxRun = mr;
			//this.random=new Random(System.nanoTime());
			seed_public++;
			this.random=new Random(seed_public);
		}

		@Override
		public Double call() {
			//System.out.println(Thread.currentThread().getId());
			Double acc = 0.0;            
			for (int r=0; r<this.maxRun; r++){
				acc+=this.getInfluence_v3c(activeNodes);            	
			}           
			return acc;
		}

		private Integer getInfluence_v3c(HashSet ans) {			
			int influence=ans.size();
			HashSet <V> activeNodes=new HashSet (ans);//active nodes
			HashSet <V> removedNodes=new HashSet(); //nodes no longer active

			for (int t=0; t<MAX_STAGES; t++){	//Each step, newly active nodes infect uninfected neighbors stochastically
				HashSet <V> newInfected=new HashSet();//newly infected nodes			
				for(V v: activeNodes){
					ArrayList <V> nbs= socialNetwork.getAdjacentVertices(v);//Neighbors
					for (V nb: nbs){ //for each neighboring vertex
						//that is not infected
						if(!(activeNodes.contains(nb)||removedNodes.contains(nb))){
							double ran=this.random.nextDouble();
							//System.out.println(this+":"+ran+":"+t);
							if(ran<=ACT_PROB){
								influence++; 
								newInfected.add(nb);
							}	
						}								
					}				
				}
				removedNodes.addAll(activeNodes);
				activeNodes.clear();
				activeNodes.addAll(newInfected);
			}			
			return influence;				
		}                
	}		


	/**
	 * Filter out nodes which are not socially connected, i.e., way points but not houses
	 * @param active_nodes 
	 * @param socialNet
	 * @return nodes in social network
	 */
	private HashSet<V> filter(HashSet<V> active_nodes, Graph<V> socialNet) {
		HashSet<V> social_nodes=new HashSet<V>();

		for (V v: active_nodes){
			if (socialNet.getVertexList().containsKey(v)) 
				social_nodes.add(v);			
		}
		return social_nodes;
	}

	public Graph<V> getSocialNetWork(){
		return 	socialNetwork;	
	}

}
