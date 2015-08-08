package pinetree.rest;

public class BalanceBean {
	
	public BalanceBean() {
	}
	
	public BalanceBean(long balance) {
		this.balance = balance;
	}

	private long balance;
	
	public long getBalance() {
		return balance;
	}

	public void setBalance(long balance) {
		this.balance = balance;
	}

}
