package revoluttest;

public class TransactionIdBean {	

	public TransactionIdBean() {
	}

	public TransactionIdBean(long transactionId) {
		this.transactionId = transactionId;
	}
	
	public long transactionId;

	public long getTransactionId() {
		return transactionId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (transactionId ^ (transactionId >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TransactionIdBean other = (TransactionIdBean) obj;
		if (transactionId != other.transactionId)
			return false;
		return true;
	}

}
