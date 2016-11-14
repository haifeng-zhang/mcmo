package mcmo;

public class CustomerDirectMail {

	private String customerId;//customer id
	private double responseRate;//response rate
	private boolean target;//target this customer?

	public CustomerDirectMail(String id, double rr) {
		customerId=id;
		responseRate=rr;
		target=false;
	}

	public double getResponseRate() {
		return responseRate;
	}
	
	public void setAsTarget(){
		target=true;
	}
}
