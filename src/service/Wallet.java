package service;


import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;

// the store's money : every sale adds to it, only the manager spends it (on equipment)
public class Wallet {
	public static final double STARTING_BALANCE = 5000; // starter fund so a new store can buy its first equipment

	// writable inside this class only ; the outside gets a read-only view (see balanceProperty)
	private final ReadOnlyDoubleWrapper balance = new ReadOnlyDoubleWrapper(STARTING_BALANCE);

	public double getBalance() { // getter that returns the balance
		return balance.get();
	}

	// labels bind to this so they update by themselves after every sale / purchase
	public ReadOnlyDoubleProperty balanceProperty() { 
		return balance.getReadOnlyProperty();
	}

	// used by DataStore ; null = older save file without a wallet (keeps the starting balance)
	public void restore(Double saved) { // restores balance from boxed double saved value 
		if (saved != null) { // updates balance if non null saved val exists
			balance.set(Math.max(saved, 0)); // never restore a negative balance // restore balance to at least 0 and update property
		}
	}

	public void deposit(double amount) {
		if (amount < 0) { // checks if the amount to deposit is negative
			throw new IllegalArgumentException("Amount cannot be negative.");
		}
		balance.set(balance.get() + amount); // add amount to current balance
	}

	public void withdraw(double amount) {
		if (amount < 0) { // checks if the amount to deposit is negative
			throw new IllegalArgumentException("Amount cannot be negative.");
		}
		// the 0.005 tolerance avoids false "not enough" errors from floating-point rounding
		if (amount > balance.get() + 0.005) { // checks whether requested withdrawal exceeds balance even with rounding tolerance
			throw new IllegalArgumentException(String.format(
				"Not enough balance in the wallet (needs ₱%,.2f, has ₱%,.2f).", amount, balance.get()));
		}
		balance.set(Math.max(balance.get() - amount, 0)); // Math.max keeps a rounding leftover from going below zero // subtract amount and clamps at 0 to avoid tiny negative values
	}
}