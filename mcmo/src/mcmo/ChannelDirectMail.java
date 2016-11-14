package mcmo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;

public class ChannelDirectMail implements Channel {
	ArrayList <CustomerDirectMail> customers=new ArrayList <CustomerDirectMail>(); //customers
	double converRate=0.05;//conversion rate
	double numWeeks=13;//number of repeated campaign weeks: 
	double RESPONSE_RATE=0.037*3; //average response rate
	//Note: for each budget we might need to decide optimal number of campaign weeks

	//Use a uniform distribution generate customer response rate with range [0, 0.037*2]  
	Random ran = new Random(2016);
	double cost=1;//cost to mail

	public ChannelDirectMail(Set<String> set) {
		for(String id: set){
			CustomerDirectMail newCustomer=new CustomerDirectMail(id, ran.nextDouble()*RESPONSE_RATE*2);
			customers.add(newCustomer);
		}
		
		System.out.println("CHANNEL: direct mail");
		System.out.println(">>Conversion Rate:"+converRate);
		System.out.println(">>Response Rate:"+RESPONSE_RATE);			
		
	}

	//	public double getAdoption(double budget){
	//		double response=0;
	//		double adoption=0;
	//		double rmbgt=budget/numCamps; //remaining budget
	//
	//		//SORT response rates and obtain index		
	//		ArrayIndexComparator comparator = new ArrayIndexComparator(this.getProbList());
	//		Integer[] index = comparator.createIndexArray();
	//		Arrays.sort(index, comparator);
	//
	//		//Response
	//		for(int i=0; i<index.length; i++){
	//			if(rmbgt>=cost)	{				
	//				//customers.get(index[i]).setAsTarget();
	//				rmbgt-=cost;
	//				response+=customers.get(index[i]).getResponseRate(); //estimated response
	//			}
	//		}		
	//
	//		//Adoption
	//		adoption=response*numCamps*converRate;		
	//
	//		return adoption;		
	//	}

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

	public double getAdoptionWeek(double weeklyBudget){
		double response=0;
		double adoption=0;
		double rmbgt=weeklyBudget; //remaining budget

		//SORT response rates and obtain index		
		ArrayIndexComparator comparator = new ArrayIndexComparator(this.getProbList());
		Integer[] index = comparator.createIndexArray();
		Arrays.sort(index, comparator);

		//Response
		for(int i=0; i<index.length; i++){
			if(rmbgt>=cost)	{				
				//customers.get(index[i]).setAsTarget();
				rmbgt-=cost;
				response+=customers.get(index[i]).getResponseRate(); //estimated response
			}
		}		

		//Adoption
		adoption=response*converRate;		

		return adoption;		
	}



	private Double[] getProbList() {
		Double [] probs=new Double [customers.size()];
		for(int c=0; c<customers.size(); c++){
			probs[c]=customers.get(c).getResponseRate();
		}		
		return probs;
	}	

}
