package mcmo;

import java.util.ArrayList;
import java.util.Arrays;

import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.Solver;
import net.sf.javailp.SolverFactory;
import net.sf.javailp.SolverFactoryCPLEX;
import net.sf.javailp.SolverFactoryLpSolve;

public class MckpSolver {

	private ArrayList <ArrayList<Option>> channelOptions=new ArrayList <ArrayList<Option>> ();	
	private int budget;	

	public MckpSolver(ArrayList <ArrayList<Option>> chanels, int budget) {		
		this.channelOptions=chanels;
		this.budget=budget;
	}

	/**
	 * Solve MCKP using Java ILP (http://javailp.sourceforge.net/) that support multiple ILP solvers
	 * @return
	 */
	public double solveByJavaILP(){
		//double value=0;

		//Construct problem
		//SolverFactory factory = new SolverFactoryLpSolve(); // use lp_solve
		SolverFactory factory = new SolverFactoryCPLEX(); // use CPLEX

		factory.setParameter(Solver.VERBOSE, 0);
		factory.setParameter(Solver.TIMEOUT, 100); // set timeout to 100 seconds		
		Problem problem = new Problem(); //create a new problem instance		
		Linear linearObj = new Linear(); //linear expression for objective function
		Linear linearBgt = new Linear(); //linear expression for budget constraint


		for(int i=0; i<channelOptions.size(); i++){ //for each marketing channel
			ArrayList<Option> cos=channelOptions.get(i);
			Linear linearDsj = new Linear(); //linear expression for disjoint constraint
			for(int j=0; j<cos.size(); j++){ //for each marketing option
				Option opt=cos.get(j);
				String varName="x_"+i+j;				
				linearObj.add(opt.getProfit(), varName);
				linearBgt.add(opt.getCost(), varName);
				linearDsj.add(1,varName);
				problem.setVarType(varName, Boolean.class);
			}
			problem.add(linearDsj, "=", 1);			
			
		}
				
		problem.setObjective(linearObj, OptType.MAX);
		problem.add(linearBgt, "<=", budget);

		//Solve it
		Solver solver = factory.get(); 
		Result result = solver.solve(problem);	

		//System.out.println(result);
		
		//Output optimal solution for a channel, 0:dtd; 1:oad; 2:dml; 3:brc
		displayChannelOptimalSolution(result,0);
		displayChannelOptimalSolution(result,1);
		displayChannelOptimalSolution(result,2);
		displayChannelOptimalSolution(result,3);
		
		return (Double) result.getObjective();	
	}
	
	private void displayChannelOptimalSolution(Result result, int cid) {
		ArrayList <Integer> sol=new ArrayList <Integer> ();
		Driver.CHANNEL_EXPD[cid]=0;
		//System.out.print(Driver.CHANNEL_NAMES[cid]+":");
		for(int j=0; j<channelOptions.get(cid).size();j++){
			String vs="x_"+cid+j;
			//System.out.println(vs+":"+result.getPrimalValue(vs));	
			//if(j<4)
			Integer sol_x=(Integer) result.getPrimalValue(vs);
			sol.add(sol_x);
			if (sol_x==1) {
				channelOptions.get(cid).get(j).select();		
				//assign channel expenditure
				Driver.CHANNEL_EXPD[cid]=channelOptions.get(cid).get(j).getCost();
			}
			else channelOptions.get(cid).get(j).unselect();
		}
		//System.out.println(Arrays.toString(sol.toArray()));
		
	}

	/**
	 * Solve MCKP using CPLEX java
	 * @return
	 */
	public double solveByCPLEX(){			
		return 0;		
	}
	
	
	

}
