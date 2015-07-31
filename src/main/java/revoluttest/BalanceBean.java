package revoluttest;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class BalanceBean {
	
	public BalanceBean() {
	}
	
	public BalanceBean(long balance) {
		this.balance = balance;
	}

	public long balance;
	
	public long getBalance() {
		return balance;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (balance ^ (balance >>> 32));
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
		BalanceBean other = (BalanceBean) obj;
		if (balance != other.balance)
			return false;
		return true;
	}

}
