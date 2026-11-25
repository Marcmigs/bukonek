package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.ProductOption;
import model.Products;
import model.Transaction;

// every sale and the rules around them (no screens here : see ui.TransactionPanel / ui.RankingDashboard)
public class TransactionLog extends Store {
	public static final double REFUND_RATE = 0.5; // a refund gives back this fraction of the money (and no stock)

	// one entry of the product dropdown = a product + one of its variants
	public record SaleChoice(Products product, ProductOption option) {}

	// sales totals for one clerk (built by computeRanking)
	public static class ClerkStats {
		private final String name;
		private double amount = 0; // total pesos sold
		private int units = 0;     // total quantity sold
		private int sales = 0;     // number of transactions
		private int rank = 0;      // 1 = best

		ClerkStats(String name) { this.name = name; } // initialize the name

		// adds one sale to this clerk's totals
		void add(Transaction t) {
			amount += t.getAmount(); // add transaction money to total amount
			units += t.getQuantity(); // add transaction quantity to total units
			sales++; // increment on completed sale 
		}
		void setRank(int rank) { this.rank = rank; } // assigning computed rank

		public String getName() { return name; }
		public double getAmount() { return amount; }    // getters
		public int getUnits() { return units; }
		public int getSales() { return sales; }
		public int getRank() { return rank; }
	}

	private final List<Transaction> transactions = new ArrayList<>(); // oldest first
	private final Inventory productInventory; // where the sellable products come from
	private final Wallet wallet;              // every sale adds to it
	private int nextId = 1;                   // id the next transaction will get

	public TransactionLog(Inventory productInventory, Wallet wallet) {
		this.productInventory = productInventory; // initialize data 
		this.wallet = wallet;
	}

	// used by DataStore
	public int getNextId() { return nextId; } // return next transaction id

	// puts saved transactions back (same list object stays in use)
	public void restore(List<Transaction> saved, int savedNextId) {
		transactions.clear(); // remove any existing transaction in place
		transactions.addAll(saved); // append restores transactions
		nextId = savedNextId; // restore nextId counter from saved state
	}

	// every variant of every product, rebuilt each time so it matches the current inventory
	public List<SaleChoice> sellableChoices() { // builds a flat list of all product-variant combinations in inventory
		List<SaleChoice> sellable = new ArrayList<>(); // create array
		for (Products p : productInventory.getProducts()) { // loops through every product
			for (ProductOption o : p.getOptions()) { // loops through every variant
				sellable.add(new SaleChoice(p, o)); // adds SaleChoice pairing product p with variant option o
			}
		}
		return sellable;
	}

	// customerName is required : every sale must say who bought
	public Transaction recordSale(String clerkUsername, SaleChoice choice, int quantity, String recordedBy, String customerName) {
		// validate everything first
		if (customerName == null || customerName.isBlank()) { // checks if customer entered a name
			throw new IllegalArgumentException("Please enter the customer's name."); // throw message if no name is entered
		}
		if (choice == null) { // checks if a choice has been selected
			throw new IllegalArgumentException("Please pick a product."); // throws message if none has been selected
		}
		if (quantity <= 0) { // the quantity must be greater than 0 
			throw new IllegalArgumentException("Quantity must be more than zero."); // throws message if invalid value 
		}
		ProductOption option = choice.option(); // extracts the selected ProductionOption variant
		if (quantity > option.getStock()) { // checks whether requested quantity exceeds avail stock
			throw new IllegalArgumentException("Not enough stock! Only " // throw message 
				+ option.getStock() + " left for " + choice.product().getName()
				+ " - " + option.getLabel() + ".");
		}

		String item = choice.product().getName() + " (" + option.getLabel() + ")"; // same text findOption() matches on later
		double amount = option.getPrice() * quantity; // total is computed, never typed

		// saved first ; if it throws, stock is untouched
		Transaction t = new Transaction(nextId, clerkUsername, item, quantity, amount, recordedBy, customerName.trim()); // construct new sale Transaction
		transactions.add(t); // append new transaction to the list
		nextId++; // increment id for future transactions

		option.addStock(-quantity); // stock already checked above // deduct sold quantity
		wallet.deposit(amount);     // the full sale amount goes into the wallet
		productInventory.refreshDisplay(); // inventory list on screen shows the new stock
		refreshDisplay();                  // ranking charts show the new sale
		changed();                         // save transaction + updated stock
		return t;
	}
	
	// the manager bought equipment : shows in the store transactions as money going out (not a sale)
	// the caller saves (EquipmentStore.buy)
	public Transaction recordPurchase(String managerUsername, String itemName, double amount) { // record equipment purchase transaction
		Transaction t = Transaction.purchase(nextId, managerUsername, "Bought: " + itemName, amount); // create purchase transaction
		transactions.add(t); // add transaction to list
		nextId++; // increment id for future transaction
		refreshDisplay(); // history and charts show it
		return t;
	}

	// true when at least one real sale exists (purchases do not count)
	public boolean hasSales() {
		for (Transaction t : transactions) { // loops through all recorded transactions
			if (!t.isPurchase()) return true; // true for non-purchase transaction
		}
		return false;
	}

	// finds the product variant a transaction sold (the item text is "Product (Variant)") ; null if it no longer exists
	private ProductOption findOption(Transaction t) { // Locates the ProductOption matching a transaction's formatted item string
		for (Products p : productInventory.getProducts()) { // Iterates over all products in inventory
			for (ProductOption o : p.getOptions()) { // Iterates over all variants of each product
				if ((p.getName() + " (" + o.getLabel() + ")").equals(t.getItem())) { // Compares formatted "Product (Variant)" text with transaction's item string
					return o;
				}
			}
		}
		return null;
	}

	// how much money leaves the wallet : void = all of it, refund = REFUND_RATE of it
	public static double moneyBack(Transaction t, boolean voided) {
		return voided ? t.getAmount() : t.getAmount() * REFUND_RATE; // Full amount if voided; REFUND_RATE fraction if refunded
	}

	// removes one transaction from the log (manager only)
	// void   : the sale never happened -> stock goes back in full, the full amount leaves the wallet
	// refund : the customer keeps the product -> stock is NOT put back, only REFUND_RATE of the amount leaves the wallet
	public void deleteTransaction(Transaction t, boolean voided) { // Voids or refunds a sale transaction and updates wallet, stock, UI, and save state
		if (t.isPurchase()) { // Prevents voiding or refunding equipment purchase records
			throw new IllegalArgumentException("Equipment purchases cannot be voided or refunded."); // Throws if transaction is a purchase
		} 
		ProductOption option = null; // Holds target variant if voiding, stays null for refund
		if (voided) { // Checks whether operation is a void rather than a refund
			// a void must put stock back, so the variant has to still exist // Explains why void requires the variant to still exist
			option = findOption(t); // Looks up the variant to restore stock to
			if (option == null) { // Checks if the variant was deleted from inventory
				throw new IllegalArgumentException("Cannot void: \"" + t.getItem() // Starts error message explaining missing variant
					+ "\" is no longer in the inventory, so its stock cannot be returned. Use Refund instead."); // Suggests refund instead since refund doesn't return stock
			} 
		} 
		double back = moneyBack(t, voided); // Computes amount to withdraw from wallet (full for void, REFUND_RATE for refund)
		wallet.withdraw(back); // throws if the wallet cannot cover it, nothing has changed yet // Withdraws money from wallet; throws if insufficient balance before modifying log/stock
		if (!transactions.remove(t)) { // Attempts to remove transaction t from transactions list; false if already removed
			wallet.deposit(back); // it was not there : undo // Rolls back wallet withdrawal if transaction wasn't present in log
			throw new IllegalArgumentException("That transaction no longer exists."); // Throws exception because transaction was missing
		} 
		if (voided) { // Checks again if this was a void
			option.addStock(t.getQuantity()); // stock comes back in full // Restores sold quantity back to the variant's stock
		} // Closes stock restoration check
		productInventory.refreshDisplay(); // inventory list on screen shows the returned stock // Refreshes inventory UI to display updated stock
		refreshDisplay(); // ranking charts drop the transaction // Refreshes TransactionLog UI views (history and rankings)
		changed(); // save 
	} 

	// getAll / getForClerk return copies, so callers cannot change the real log
	public List<Transaction> getAll() { return new ArrayList<>(transactions); }
	public boolean isEmpty() { return transactions.isEmpty(); }

	/** Every transaction assigned to this clerk, oldest first. */
	public List<Transaction> getForClerk(String clerkUsername) {
		List<Transaction> result = new ArrayList<>(); // create list
		for (Transaction t : transactions) { // loop through each transaction
			if (t.getClerkUsername().equals(clerkUsername)) { //  check if transaction belongs to clerk
				result.add(t); // adds matching transaction to result
			}
		}
		return result;
	}

	// newest first ; clerkUsername null = everyone's transactions
	public List<Transaction> historyFor(String clerkUsername) {
		List<Transaction> list = (clerkUsername == null) ? getAll() : getForClerk(clerkUsername); // fetches all transactions if clerkUsername is null, else only that clerk's
		Collections.reverse(list); // reverse order of the list so newest appear first
		return list;
	}

	// highest total pesos first ; ties broken by units, then name ; tied clerks share a rank
	// every clerk is listed, even with no sales yet
	public List<ClerkStats> computeRanking(List<String> clerkUsernames) { // Computes sorted clerk sales stats and competition-style ranks
		// LinkedHashMap : one entry per clerk, keeps insertion order // Explains map choice for clerk lookup and deterministic order
		Map<String, ClerkStats> stats = new LinkedHashMap<>(); // Maps clerk username to their ClerkStats object
		for (String clerk : clerkUsernames) { // Iterates over provided list of clerk usernames
			stats.put(clerk, new ClerkStats(clerk)); // Initializes zeroed ClerkStats entry for each clerk
		} 
		for (Transaction t : transactions) { // Iterates over all transactions in the log
			if (t.isPurchase()) continue; // the manager is not a clerk // Skips equipment purchases so they don't count as clerk sales
			// a clerk who is not in the list (e.g. no longer hired) still gets an entry // Explains computeIfAbsent behavior for former/unlisted clerks
			stats.computeIfAbsent(t.getClerkUsername(), ClerkStats::new).add(t); // Gets or creates ClerkStats for the clerk and adds this sale
		} 

		// sort : amount (high to low), then units (high to low), then name (A to Z) // Explains multi-key sorting order
		List<ClerkStats> ranked = new ArrayList<>(stats.values()); // Copies ClerkStats values into a sortable list
		ranked.sort(Comparator.comparingDouble(ClerkStats::getAmount).reversed() // Sorts by sales amount descending (highest pesos first)
			.thenComparing(Comparator.comparingInt(ClerkStats::getUnits).reversed()) // Breaks ties by units sold descending (most units first)
			.thenComparing(ClerkStats::getName)); // Breaks remaining ties alphabetically by clerk name (A to Z)

		// assign ranks : 1, 2, 2, 4 style (clerks tied on amount and units share a rank) // Explains standard competition ranking ("1224" ranking)
		int rank = 0; // Current assigned rank value
		ClerkStats prev = null; // Previous clerk in sorted order for tie comparison
		for (int i = 0; i < ranked.size(); i++) { // Iterates through sorted clerks with index i
			ClerkStats s = ranked.get(i); // Gets clerk stats at position i
			if (prev == null || Double.compare(prev.getAmount(), s.getAmount()) != 0 || prev.getUnits() != s.getUnits()) { // True for first clerk or when amount/units differ from previous
				rank = i + 1; // new rank unless exactly tied with the clerk above // Sets competition rank based on 1-based position
			} 
			s.setRank(rank); // Assigns rank to current clerk's stats
			prev = s; // Updates prev reference to current clerk for next iteration
		} 
		return ranked; 
	} 
} 