package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.Equipment;
import model.ProductOption;
import model.Products;

// the product list and every rule about changing it
public class Inventory extends Store {
	private final List<Products> productsList = new ArrayList<>(); // array of list for all the products

	private final String title;       // "Products" / "Equipment"
	private final String itemName;    // "Product" / "Equipment"

	// equipment the products are made with (set by Main, only the products inventory uses it)
	private EquipmentStore equipmentStore = null;

	public Inventory(String title, String itemName) { // initializes the item name
		this.title = title;
		this.itemName = itemName;
	}

	public void setEquipmentStore(EquipmentStore equipmentStore) { this.equipmentStore = equipmentStore; } // public setter 
	public String getTitle() { return title; } // getters
	public String getItemName() { return itemName; } // getters

	public boolean requiresEquipment() { return equipmentStore != null; } // checks if there are available equipments that hasnt been linked to any product yet 

	/** Equipment that can still be given to a new product ; empty when there is no equipment store. */
	public List<Equipment> availableEquipment() { // returns list of available equipments
		return equipmentStore == null ? new ArrayList<>() : equipmentStore.available(); // returns an empty list if no stores exists, otherwise delegates to equipmentStore.available()
	}

	// basic list functions //
	// Read-only view of the products : changes must go through the methods of this class
	public List<Products> getProducts() { return Collections.unmodifiableList(productsList); } // wraps productList so callers cannot directly add/remove elements outside class methods

	// used by DataStore : puts saved products back
	public void restore(List<Products> saved) { // replaces current products with saved list
		productsList.clear(); // removes all existing products from productList
		if (saved != null) { //guards against a null saved list
			productsList.addAll(saved); // appends all saved products into productsList
		}
	}

	// true when an item with this name already exists (ignores upper/lower case and extra spaces)
	private boolean nameTaken(String name) {
		String wanted = name.trim();
		for (Products p : productsList) { // goes through each product
			if (p.getName().trim().equalsIgnoreCase(wanted)) { // if a product has a similar name with  wanted
				return true;
			}
		}
		return false;
	}


	 //Adds a product to the list (does not save ; the public methods that call it do).
	 // @throws IllegalArgumentException if the name is already taken
	public void addProduct(Products product) {
		if (nameTaken(product.getName())) { // checks first if the name is valid/does not exist yet 
			throw new IllegalArgumentException("A " + itemName.toLowerCase() + " named \""
				+ product.getName().trim() + "\" already exists."); // error catch if a name exists already
		}
		productsList.add(product); // if the name is accepted, it is added to the productList
	}

	// raw removal from the list (no save) ; also used to undo an add that failed
	public void removeProduct(Products product) {
		productsList.remove(product); // removing a product from the list
	}

	// add items //
	/**
	 * @throws NumberFormatException if a price or stock is not a number
	 * @throws ArrayIndexOutOfBoundsException if the price or stock list is shorter than the label list
	 */
	// add a product with variants from the manager's text input ("x, y, z" lists ; same index goes together)
	public void addProductFromInput(String name, String desc, String optRaw, String priceRaw, String stockRaw) {
		String[] optArr = optRaw.split(","); // splits comma separated variants into a string array
		String[] priceArr = priceRaw.split(","); // splits comma separated prices into a string array
		String[] stockArr = stockRaw.split(","); // splits comma separated stock quantities into a string array

		List<ProductOption> optsList = new ArrayList<>(); // create list to hold the parsed productOoption variants
		for (int i = 0; i < optArr.length; i++) { // loops across each variant index based on optArr length
			String label = optArr[i].trim(); // trims white space around variant label
			double price = Double.parseDouble(priceArr[i].trim()); // trims and converts price string to double
			int stock = Integer.parseInt(stockArr[i].trim()); // trims and converts stock string to integer
			optsList.add(new ProductOption(label, price, stock)); // create productOption list and add the details there
		}
		addProduct(new Products(name, desc, optsList)); // constructs product object with all the variants and adds it to productsList
		changed(); // save
	}

	// adds a finished product ; when equipment is given it is linked and its uses are spent (1 per unit of starting stock)
	// returns a short sentence about the equipment used, or null ; if the equipment step fails the product is not kept
	public String addLinkedProduct(Products product, Equipment equipment) { // add product, links equipment if provided, and rolls back on failure
		// the starting stock of all variants together decides how many uses are spent
		int totalInitialStock = 0; // accumulates total starting stock across all products
		for (ProductOption o : product.getOptions()) { // loops through every variant option on the product
			totalInitialStock += o.getStock(); // adds the stock value to the total
		}
		addProduct(product); // adds product to List
		String usage = null; // holds the equipment usage message, defaulting to null if none occurs
		try {
			if (equipment != null && equipmentStore != null) { // only links and consumes when both equipment and equipmentStore exists
				usage = equipmentStore.linkAndConsume(equipment, product, totalInitialStock); // links equipment to product and deducts uses to totalInitialStock
			}
		} catch (RuntimeException ex) { // catches runtime failures (ex. insufficient equipment uses)
			// the equipment step failed (e.g. not enough uses) : undo the add so no half-made product stays
			removeProduct(product); // remove product to the list
			throw ex;
		}
		changed(); // save
		return usage;
	}

	// add another variant to an existing product ; its starting stock takes 1 use per unit from the product's equipment
	// returns a short sentence about the equipment used, or null when no equipment was used
	public String addOptionToProduct(Products product, String label, double price, int stock) {
		if (product == null) { // checks that a target product was provided
			throw new IllegalArgumentException("Please pick a product."); // throws if product is null
		}
		if (label.isEmpty()) { // checks whether the variant label is blank
			throw new IllegalArgumentException("Variant label cannot be empty.");
		}
		if (price < 0) { // checks if the price is valid
			throw new IllegalArgumentException("Price cannot be negative.");
		}
		if (stock < 0) { // checks if the stock value is valid
			throw new IllegalArgumentException("Stock cannot be negative.");
		}
		// all checks are done above, so equipment uses are never spent on a variant that then fails to be added
		String usage = null; // initially set to null
		if (equipmentStore != null && stock > 0) { // Consumes equipment uses only when an equipmentStore exists and initial stock is positive
			usage = equipmentStore.consumeForRestock(product, stock); // throws = blocked, nothing changes // deducts uses for new stocks; throws if blocked before mutating state
		}
		product.addOption(new ProductOption(label, price, stock)); // adds new product variant
		changed(); // save
		if (usage != null) { // check if equipment uses were consumed
			equipmentStore.refreshDisplay(); // the equipment panel shows the uses that were just spent
		}
		return usage;
	}

	// update items ///////////////////////////////////////
	// restock (positive) or reduce (negative) one variant ; a restock spends equipment uses
	// returns a short sentence about the equipment used, or null
	public String adjustStock(Products product, ProductOption option, int changeAmount) {
		if (option == null) { // checks if an product option has been chosen
			throw new IllegalArgumentException("Please pick an item.");
		}
		String usage = null; // set usage to null
		// only a restock (positive change) spends uses ; reducing stock is free
		if (equipmentStore != null && changeAmount > 0 && product != null) {
			usage = equipmentStore.consumeForRestock(product, changeAmount); // throws = blocked, nothing changes // deducts equipment uses for the restocked quantity
		}
		option.addStock(changeAmount); // throws if it would go below zero // apply stock change
		changed(); // save
		if (usage != null) {
			equipmentStore.refreshDisplay(); // the equipment panel shows the uses that were just spent // refreshes equipment display 
		}
		return usage;
	}

	// removes the product and all its variants
	public void deleteProduct(Products product) {
		if (product == null) { // checks if a product has been chosen
			throw new IllegalArgumentException("Please pick something to delete.");
		}
		removeProduct(product); // deletes the whole product with its variants
		changed(); // save
	}

	// removes just one variant ; the product stays
	public void deleteOption(Products product, ProductOption option) {
		if (product == null || option == null) { // checks if no product has been chosen or there is no variant of that product
			throw new IllegalArgumentException("Please pick something to delete.");
		}
		// product must always keep 1 variant ; else nothing left to hold stock / price
		if (product.getOptions().size() == 1) { // theres only one variant so you are deleting the whole product itself
			throw new IllegalArgumentException("That's the product's last variant. Delete the whole product instead.");
		}
		product.removeOption(option); // delete the product variant
		changed(); // save
	}
}