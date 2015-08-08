package snowpine.pangu.rest;

public class TransactionIdBean {	

	public TransactionIdBean() {
	}

	public TransactionIdBean(long transactionId) {
		this.transactionId = transactionId;
	}
	
	private long transactionId;

	public long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}

}
