package mcmo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

/**
 * This class implements the simple greedy algorithm ignoring cost (Zhang 2016)
 * @author zhangh24
 *
 */
public class Algorithm5 {

	private static final boolean USE_CHRISTOFIDES=true;

	public Algorithm5() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Single source Dijkstra algorithm
	 * @param graph graph
	 * @param source source vertex
	 * @return list of shortest distance and previous node
	 */
	public static <V> WrappedObject <V> dijkstraShortestPath (Graph<V> graph,V source) {
		/*System.out.println("Shortest Path:"+source+"->ALL");*/

		HashMap<V, Double> dist = new HashMap<V, Double>(); //distance to all vertices
		HashMap<V, V> prev = new HashMap<V, V>(); //predecessor list

		PriorityQueue<Vertex<V>> Q = new PriorityQueue<>();


		graph.getVertexList().get(source).min_dist=0;
		dist.put(source, 0.0);

		graph.getVertexList().get(source).pre_node=null;
		prev.put(source, null);

		//Initialization
		for (V v: graph.getVertexList().keySet()){
			if (!v.equals(source)) {
				graph.getVertexList().get(v).min_dist=Double.POSITIVE_INFINITY;
				dist.put(v, Double.POSITIVE_INFINITY);				

				//prev.put(v, null);
				graph.getVertexList().get(v).pre_node=null;
				prev.put(v, null);

			}
			Q.offer(graph.getVertexList().get(v));			
		}	

		//Main Procedure
		while (!Q.isEmpty()){
			Vertex u=Q.poll();

			for (V v : graph.getAdjacentVertices((V) u.getID())){
				//double alt = dist.get(u) + graph.getDistanceBetween(u, v);
				double alt = u.min_dist + graph.getDistanceBetween((V) u.getID(), v);
				if (alt<graph.getVertexList().get(v).min_dist){
					Q.remove(graph.getVertexList().get(v));	

					graph.getVertexList().get(v).min_dist=alt;
					dist.put(v, alt);											

					graph.getVertexList().get(v).pre_node=u.getID();
					prev.put(v, (V) u.getID());

					Q.add(graph.getVertexList().get(v));					
				}				
			}
		}

		/*System.out.println("Dist:"+dist);*/
		return new WrappedObject(dist, prev);
	}

	/**
	 * Compute optimal walk using simple greedy
	 * @param road_network routing network incorporating houses and way points 
	 * @param social_network social diffusion network
	 * @param start start vertex, i.e., a way point
	 * @param target end vertex, i.e., a way point
	 * @param budget budget
	 * @param iniActiveNodes initially active vertex, i.e., adopters
	 * @return a walk as sequence of vertices in road_network
	 * @throws Exception 
	 */
	public static <V> ArrayList <V> greedyWalk(int mode, Graph<V> road_network, InfluenceModel<V> social_network, 
			V start, V target, double budget, HashSet <V> iniActiveNodes) throws Exception {
		//DEFINED two lists: one for visited set, one for unvisited set
		HashSet<V> visited_set=new HashSet<V>();
		HashSet<V> unvisited_set=new HashSet<V>();

		//ADD all social nodes to unvisited set except initially active nodes
		for (V v:social_network.getSocialNetWork().getVertexList().keySet()){
			if(iniActiveNodes.contains(v)) visited_set.add(v);
			else unvisited_set.add(v);			
		}

		//BEST walk tracked so far
		ArrayList <V> walk_best = new ArrayList <V> ();

		double f_bs=social_network.getExpectedInfluence(visited_set); //influence before choosing a new vertex

		//PICK node in unvisited set with max marginal influence and ADD to the visited set
		while (!unvisited_set.isEmpty()){
			V node_best_iter = null;
			ArrayList <V> walk_best_iter = null; //BEST walk for this iteration

			//double fc_max_iter=0;
			double df_max_iter=0;
			double cost_best_iter=0;

			//double f_cur=social_network.getExpectedInfluence(visited_set); //influence before choosing a new vertex
			double f_new=0;
			double df=0;

			for(V v:unvisited_set){	
				HashSet <V> u=union(v, visited_set);

				//Walk and cost
				WrapWalkWithLength walk_new;
				if(USE_CHRISTOFIDES)
					walk_new=Graph.shortestCoverCostChristofides(start,road_network,exclude(u,iniActiveNodes));
				else
					walk_new=Graph.shortestCoverCostGreedy(start,road_network,exclude(u,iniActiveNodes));
								
				//System.out.println(walk_new.length+","+walk_new_1.length);					

				double cost_new=walk_new.length;
				if(mode==1) cost_new+=ChannelDoorToDoor.visit_cost*exclude(u,iniActiveNodes).size(); //cost of adding the new vertex, i.e., approximate shortest walk covering all nodes
				else if(mode==2) cost_new+=Driver_rg.visit_cost*exclude(u,iniActiveNodes).size();

				//OPTIMIZE PERFORMANCE: method 1
				//SKIP infeasible nodes to boost speed
				//Twice fast

				if(ChannelDoorToDoor.prune_mode||Driver_rg.prune_mode){ 
					//System.out.println(cost_new);
					if(cost_new>budget) continue;				
				}
				//Inflence				
				f_new=social_network.getExpectedInfluence(u); //influence when adding the new vertex				
				df=f_new-f_bs; //influence changes

				if (df>=df_max_iter){
					f_bs=f_new;
					node_best_iter=v;
					df_max_iter=df;
					cost_best_iter=cost_new;
					walk_best_iter=walk_new.walk;									
				}else{
					//System.out.println("worse than:"+fc_max);
				}
			}

			if (node_best_iter!=null&&cost_best_iter<=budget){	//Method 0
				//if (node_best_iter!=null){	//Method 1
				walk_best=walk_best_iter;
				visited_set.add(node_best_iter); //ADD the node to visited set
				unvisited_set.remove(node_best_iter); //REMOVE the node from unvisited set
				if(mode==1){
					//Driver_dtd.max_influe+=delta_f_max;
					ChannelDoorToDoor.budget=cost_best_iter;//Track budget usage
					ChannelDoorToDoor.visit_set=exclude(visited_set, iniActiveNodes);
				}else if(mode==2){
					//Driver_rg.max_influe+=delta_f_max;
					Driver_rg.budget=cost_best_iter;//Track budget usage
					Driver_rg.visit_set=exclude(visited_set, iniActiveNodes);					
				}
				//System.out.println(Driver_dtd.visit_set+";"+f_cur+";"+f_new+";"+fc_max_iter+";"+delta_f_max);
			}else break;			
		}

		//Adjusted Influence[jair]
		double influAdj=(visited_set.size()==iniActiveNodes.size())?0:
			social_network.getExpectedInfluence(visited_set)-social_network.getExpectedInfluence(iniActiveNodes);
		
		//System.out.println(influAdj);

		
		if(mode==1) ChannelDoorToDoor.max_influe=influAdj;
		else if(mode==2){
			Driver_rg.max_influe=influAdj;
		}
		//System.out.println(social_network.getExpectedInfluence(visited_set)-social_network.getExpectedInfluence(iniActiveNodes));
		return walk_best;		
	}

	public static <V> HashSet union(V new_node, HashSet <V> active_nodes){
		HashSet <V> union=new HashSet <V> (active_nodes);		
		union.add(new_node);
		return union;		
	}

	public static <V> HashSet union( HashSet <V> node_set_1, HashSet <V> node_set_2){
		HashSet <V> union=new HashSet <V> (node_set_1);
		//		for (V v: node_set_2){
		//			union.add(v);		
		//		}
		union.addAll(node_set_2);
		return union;			
	}	
	public static <V> HashSet<V> exclude( HashSet <V> node_set_1, HashSet <V> node_set_2){
		HashSet <V> exclude=new HashSet <V> ();
		for(V v: node_set_1){
			if(!node_set_2.contains(v)) exclude.add(v);  
		}		
		return exclude;			
	}
}
