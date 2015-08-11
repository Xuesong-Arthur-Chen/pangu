package snowpine.pangu.rest;

public class TransferRes {	

	public TransferRes() {
	}

	public TransferRes(long transactionId) {
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
