package revoluttest;

public class TransferReqBean {

	public TransferReqBean() {
	}
	
	public TransferReqBean(long from, long to, long amount) {
		this.from = from;
		this.to = to;
		this.amount = amount;
	}

	public long from;
	public long to;
	public long amount;
	
	public long getFrom() {
		return from;
	}

	public long getTo() {
		return to;
	}

	public long getAmount() {
		return amount;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (amount ^ (amount >>> 32));
		result = prime * result + (int) (from ^ (from >>> 32));
		result = prime * result + (int) (to ^ (to >>> 32));
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
		TransferReqBean other = (TransferReqBean) obj;
		if (amount != other.amount)
			return false;
		if (from != other.from)
			return false;
		if (to != other.to)
			return false;
		return true;
	}

}
