package model;

import java.time.LocalDateTime;

public class Transaction implements java.io.Serializable {
	private static final long serialVersionUID = 1L; // unchanged on purpose : older save files still load

	private final int id;                  // running number, assigned by TransactionLog
	private final String clerkUsername;    // clerk this transaction is assigned to (a purchase -> the manager who bought)
	private final String item;             // what was sold or bought
	private final int quantity;            // how many units (always 1 for a purchase)
	private final double amount;           // total amount in pesos
	private final String recordedBy;       // username of whoever recorded it
	private final LocalDateTime timestamp; // set automatically when created
	private final boolean purchase;        // true = equipment the manager bought (money out), false = a sale (money in)
	private final String customerName;     // who bought (sales only)

	public Transaction(int id, String clerkUsername, String item, int quantity, double amount, String recordedBy, String customerName) {
		// purchase = false, and the customer name is validated and trimmed first
		this(id, clerkUsername, item, quantity, amount, recordedBy, requireCustomer(customerName), false);
	}

	public static Transaction purchase(int id, String managerUsername, String item, double amount) {
		return new Transaction(id, managerUsername, item, 1, amount, managerUsername, null, true);
	}

	// Validates the customer name for a sale (returns it trimmed)
	private static String requireCustomer(String customerName) {
		if (customerName == null || customerName.isBlank()) {
			throw new IllegalArgumentException("Please enter the customer's name.");
		}
		return customerName.trim();
	}

	// Shared constructor behind the sale constructor and the purchase factory.
	// Does the validation, so no invalid Transaction can ever exist.
	private Transaction(int id, String clerkUsername, String item, int quantity, 
			double amount, String recordedBy, String customerName, boolean purchase) {
		if (clerkUsername == null || clerkUsername.isBlank()) {
			throw new IllegalArgumentException("A transaction must be assigned to a clerk.");
		}
		if (item == null || item.isBlank()) {
			throw new IllegalArgumentException("Item cannot be empty.");
		}
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be more than zero.");
		}
		if (amount < 0) {
			throw new IllegalArgumentException("Amount cannot be negative.");
		}
		this.id = id;
		this.clerkUsername = clerkUsername;
		this.item = item.trim();
		this.quantity = quantity;
		this.amount = amount;
		this.recordedBy = recordedBy;
		this.timestamp = LocalDateTime.now(); // the time of creation
		this.purchase = purchase;
		this.customerName = customerName;
	}

	// getters (transaction never changes once recorded)
	public int getId() { return id; }
	public String getClerkUsername() { return clerkUsername; }
	public String getItem() { return item; }
	public int getQuantity() { return quantity; }
	public double getAmount() { return amount; }
	public String getRecordedBy() { return recordedBy; }
	public LocalDateTime getTimestamp() { return timestamp; }
	public boolean isPurchase() { return purchase; }

	// "-" for equipment purchases and for transactions saved before customers were required
	public String getCustomerName() {
		return (customerName == null || customerName.isBlank()) ? "-" : customerName;
	}
}
