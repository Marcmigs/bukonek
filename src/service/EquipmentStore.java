package service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import model.Equipment;
import model.Products;

// equipment the manager owns : bought from a fixed list with the wallet, linked one-to-one to products,
// and used up a little every time that product is restocked (no screens here : see ui.EquipmentPanel)
public class EquipmentStore extends Store {
	// the fixed shop : 5 types x 3 levels = 15 items (sample numbers, change them here to rebalance)
	private static final String[] TYPES = {"Coconut Juicer", "Husk Shredder", "Oil Press", "Meat Grater", "Shell Cutter"};
	// PRICES and USES are tables : row = type (same order as TYPES), column = level 1 / 2 / 3
	private static final double[][] PRICES = {
		{500, 1200, 2500},   // Coconut Juicer  Lv.1 / 2 / 3
		{400, 1000, 2200},   // Husk Shredder
		{800, 1800, 3500},   // Oil Press
		{450, 1100, 2300},   // Meat Grater
		{350, 900, 2000}     // Shell Cutter
	};
	private static final int[][] USES = { // how many times u can use the equipment
		{200, 550, 1300},
		{250, 700, 1600},
		{200, 550, 1300},
		{220, 600, 1400},
		{250, 700, 1600}
	};

	// one item of the shop (how it is written on screen is up to Widgets.shopConverter)
	public record Model(String type, int level, double price, int uses) {} // object that contains all the details of an equipment 

	private static final int USES_PER_UNIT = 1; // restocking 1 unit takes 1 use (linear) ; change here to rebalance

	private static final List<Model> CATALOG = new ArrayList<>(); // create an array for the equipments
	// runs once when the class loads : builds the 15 shop items from the tables above
	static {
		for (int t = 0; t < TYPES.length; t++) {
			for (int l = 0; l < 3; l++) {
				CATALOG.add(new Model(TYPES[t], l + 1, PRICES[t][l], USES[t][l])); // adds all the equipments to the list 
			}
		}
	}

	// alphabetical by name (then level) ; used by the equipment list and the link dropdown
	private static final Comparator<Equipment> BY_NAME_THEN_LEVEL =
		Comparator.comparing((Equipment e) -> e.getName(), String.CASE_INSENSITIVE_ORDER)
			.thenComparingInt(Equipment::getLevel);

	private final List<Equipment> owned = new ArrayList<>();
	private final Inventory productInventory; // to know which products still exist
	private final Wallet wallet;              // pays for purchases
	private TransactionLog transactionLog = null; // set by Main : equipment purchases are recorded here
	private int nextId = 1;                   // id the next bought equipment will get

	public EquipmentStore(Inventory productInventory, Wallet wallet) {
		this.productInventory = productInventory;
		this.wallet = wallet;
	}

	public Wallet getWallet() { return wallet; }
	public Inventory getProductInventory() { return productInventory; }

	public static List<Model> catalog() { return new ArrayList<>(CATALOG); }

	// the log that shows every purchase in the store transactions (set by Main, like Inventory.setEquipmentStore)
	public void setTransactionLog(TransactionLog transactionLog) { this.transactionLog = transactionLog; }

	// used by DataStore
	public int getNextId() { return nextId; }

	public List<Equipment> getAll() { return new ArrayList<>(owned); }
	public boolean isEmpty() { return owned.isEmpty(); }

	// alphabetical by name (then level) ; no numbering
	public List<Equipment> getSortedByName() {
		List<Equipment> list = new ArrayList<>(owned);
		list.sort(BY_NAME_THEN_LEVEL);
		return list;
	}

	// puts saved equipment back ; null = older save file without equipment
	public void restore(List<Equipment> saved, int savedNextId) {
		owned.clear();
		if (saved != null) {
			owned.addAll(saved);
		}
		nextId = Math.max(savedNextId, 1);
		purgeUsedUp(); // older saves may still hold used-up equipment
	}

	// equipment with no uses left is thrown away and unlinked from its product
	private void purgeUsedUp() {
		owned.removeIf(e -> {
			if (e.getUsesLeft() <= 0) {
				e.unlink();
				return true; // removeIf deletes the items for which this returns true
			}
			return false;
		});
	}
	
	// the product this equipment makes ; null when not linked (or its product was deleted)
	public Products linkedProduct(Equipment e) {
		Products p = e.getProduct();
		// the contains() check catches a product that was deleted from the inventory after being linked
		return (p != null && productInventory.getProducts().contains(p)) ? p : null;
	}

	// the equipment a product uses ; null = none
	public Equipment findFor(Products product) {
		for (Equipment e : owned) {
			if (e.getProduct() == product && linkedProduct(e) != null) {
				return e;
			}
		}
		return null;
	}

	// equipment that can still be given to a product : not linked and not used up
	public List<Equipment> available() {
		List<Equipment> result = new ArrayList<>();
		for (Equipment e : owned) {
			if (linkedProduct(e) == null && e.getUsesLeft() > 0) {
				result.add(e);
			}
		}
		result.sort(BY_NAME_THEN_LEVEL);
		return result;
	}

	// the checks both link() and linkAndConsume() need
	private void checkLinkable(Equipment e, Products p) {
		if (e == null) {
			throw new IllegalArgumentException("Please pick the equipment.");
		}
		if (p == null) {
			throw new IllegalArgumentException("Please pick a product.");
		}
	}

	// the equipment must have uses left and must not already belong to another product
	private void checkFree(Equipment e) {
		if (e.getUsesLeft() <= 0) {
			throw new IllegalArgumentException("That equipment has no uses left.");
		}
		if (linkedProduct(e) != null) {
			throw new IllegalArgumentException("That equipment is already linked to " + linkedProduct(e).getName() + ".");
		}
	}

	// a product's old equipment is unlinked (and thrown away if it was used up)
	// this keeps the rule "a product has at most one equipment" when a new one is linked
	private void releaseOldEquipmentOf(Products p) {
		Equipment old = findFor(p);
		if (old != null) {
			old.unlink();
			if (old.getUsesLeft() <= 0) {
				owned.remove(old);
			}
		}
	}

	// gives a product its equipment
	public void link(Equipment e, Products p) {
		checkLinkable(e, p);
		checkFree(e);
		releaseOldEquipmentOf(p);
		e.link(p);
		changed(); // save
		refreshDisplay();
	}

	// manager throws away a piece of equipment ; it is unlinked from its product first
	public void remove(Equipment e) {
		if (e == null || !owned.contains(e)) {
			throw new IllegalArgumentException("Please pick the equipment to remove.");
		}
		e.unlink();
		owned.remove(e);
		changed(); // save
		refreshDisplay();
	}

	// links equipment to a newly created product and deducts uses for its initial stock
	// throws without changing anything if the equipment does not have enough uses left
	public String linkAndConsume(Equipment e, Products p, int initialStock) {
		// all checks first, so a failure leaves everything untouched
		checkLinkable(e, p);
		if (initialStock < 0) {
			throw new IllegalArgumentException("Initial stock cannot be negative.");
		}
		checkFree(e);
		int cost = initialStock * USES_PER_UNIT;
		if (cost > e.getUsesLeft()) {
			throw new IllegalArgumentException("Initial stock needs " + cost + " uses (" + initialStock + " x " + USES_PER_UNIT
				+ ") but " + e.getDisplayName() + " only has " + e.getUsesLeft()
				+ " left. Reduce the initial stock, or buy equipment with enough uses first.");
		}
		releaseOldEquipmentOf(p);
		e.link(p);
		if (cost > 0) {
			e.use(cost);
		}
		String summary = usageSummary(e, p, cost);
		changed(); // save
		refreshDisplay();
		return summary;
	}

	// boughtBy = the manager who pays ; the purchase also shows in the store transactions (as money out)
	public void buy(Model m, String boughtBy) {
		if (m == null) {
			throw new IllegalArgumentException("Please pick an equipment.");
		}
		if (boughtBy == null || boughtBy.isBlank()) {
			throw new IllegalArgumentException("Who is buying this equipment?");
		}
		wallet.withdraw(m.price()); // throws when the wallet cannot pay ; nothing is bought then
		owned.add(new Equipment(nextId, m.type(), m.level(), m.price(), m.uses()));
		nextId++;
		if (transactionLog != null) {
			transactionLog.recordPurchase(boughtBy, m.type() + " Lv." + m.level(), m.price());
		}
		changed(); // save (equipment, wallet and the new transaction together)
	}

	// uses taken = restock amount x USES_PER_UNIT (linear) ; throws (nothing changes) when blocked
	// equipment that runs out is removed and unlinked right away ; returns a short sentence for the manager
	public String consumeForRestock(Products product, int amount) {
		Equipment e = findFor(product);
		if (e == null) {
			throw new IllegalArgumentException("\"" + product.getName() + "\" has no equipment linked. "
				+ "Use \"Link Equipment to Product\" in the equipment panel first.");
		}
		int cost = amount * USES_PER_UNIT;
		if (cost > e.getUsesLeft()) {
			throw new IllegalArgumentException("This restock needs " + cost + " uses (" + amount + " x " + USES_PER_UNIT
				+ ") but " + e.getDisplayName() + " only has " + e.getUsesLeft()
				+ " left. Restock a smaller amount, or buy and link new equipment first.");
		}
		e.use(cost);
		return usageSummary(e, product, cost);
	}

	// sentence about the uses just spent ; equipment that ran out is thrown away here
	private String usageSummary(Equipment e, Products product, int cost) {
		String name = e.getDisplayName();
		if (e.getUsesLeft() <= 0) {
			purgeUsedUp(); // removes it from the owned list and unlinks it
			return name + " used " + cost + " uses and is now used up, so it was removed and unlinked from \""
				+ product.getName() + "\". Buy and link new equipment before the next restock.";
		}
		return name + " used " + cost + " uses (" + e.getUsesLeft() + "/" + e.getMaxUses() + " left).";
	}
}