//Commented by Carmie Shaine A. Decena
package model;

public class ProductOption implements java.io.Serializable {
	private static final long serialVersionUID = 1L; // version number used when saving / loading the file
	private final String label; // e.g. "500g" (fixed once created)
	private final double price; // price in pesos (fixed once created)
	private int stock;          // units available (never below zero)

	// Creates a variant with label, price, and starting stock
	public ProductOption(String label, double price, int stock) {
		this.label = label;
		this.price = price;
		this.stock = stock;
	}

	public String getLabel() { return label; }	// Label of the variant (used in inventory)
	public double getPrice() { return price; }	// Price of the variant (used in inventory)
	public int getStock() { return stock; }		// Units currently in stock (used in inventory)

	public void addStock(int amount) {
		// check before changing anything
		// so a failed call leaves doesn't change the stock
		if (this.stock + amount < 0) {
			throw new IllegalArgumentException("Stock cannot go below zero.");
		}
		this.stock += amount;
	}
}
