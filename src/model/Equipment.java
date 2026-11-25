package model;

public class Equipment implements java.io.Serializable { // Serializable = can be saved to a file
	private static final long serialVersionUID = 1L; // version number used when saving / loading the file
	public static final double LOW_USES_RATIO = 0.2; // highlighted when this fraction (or less) of the uses is left

	private final int id;          // running number, assigned by EquipmentStore
	private final String name;     // e.g. "Coconut Juicer"
	private final int level;       // 1 - 3
	private final double price;    // what it cost
	private final int maxUses;     // uses when brand new
	private int usesLeft;          // goes down with use(), starts at maxUses
	private Products product = null; // the product it makes (null = not linked)

	// Creates a new equipment with full uses
	public Equipment(int id, String name, int level, double price, int maxUses) {
		this.id = id;
		this.name = name;
		this.level = level;
		this.price = price;
		this.maxUses = maxUses;
		this.usesLeft = maxUses; // new equipment starts full
	}

	// getters
	public int getId() { return id; }
	public String getName() { return name; }
	public int getLevel() { return level; }
	public double getPrice() { return price; }
	public int getMaxUses() { return maxUses; }
	public int getUsesLeft() { return usesLeft; }
	public Products getProduct() { return product; } // null when not linked

	// Name shown in the UI, e.g. "Coconut Juicer Lv.2"
	public String getDisplayName() { return name + " Lv." + level; }

	// True when the equipment is running out of uses (20% or less left, LOW_USES_RATIO)
	public boolean isLowOnUses() { return usesLeft <= maxUses * LOW_USES_RATIO; }

	/**
	 * Deducts uses from this equipment.
	 *
	 * @param uses how many uses to spend
	 * @throws IllegalArgumentException if uses is negative or more than what is left
	 */
	public void use(int uses) {
		if (uses < 0 || uses > usesLeft) {
			throw new IllegalArgumentException("Not enough uses left.");
		}
		usesLeft -= uses;
	}

	// Links this equipment to the product it makes
	public void link(Products p) { this.product = p; }

	// Removes the link to the product (the equipment becomes unlinked)
	public void unlink() { this.product = null; }
}
