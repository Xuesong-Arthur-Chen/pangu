package pinetree.pangu.rest;

import java.sql.Timestamp;

public class TransactionBean {

	public TransactionBean() {
	}
	
	public TransactionBean(Timestamp timestamp, long from, long to, long amount) {
		this.timestamp = timestamp;
		this.from = from;
		this.to = to;
		this.amount = amount;
	}

	private Timestamp timestamp;
	private long from;
	private long to;
	private long amount;
	
	public Timestamp getTimestamp() {
		return timestamp;
	}

	public long getFrom() {
		return from;
	}

	public long getTo() {
		return to;
	}

	public long getAmount() {
		return amount;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}

	public void setFrom(long from) {
		this.from = from;
	}

	public void setTo(long to) {
		this.to = to;
	}

	public void setAmount(long amount) {
		this.amount = amount;
	}
}
