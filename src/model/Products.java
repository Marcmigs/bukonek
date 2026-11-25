//Commented by Carmie Shaine A. Decena
package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Products implements java.io.Serializable {
	private static final long serialVersionUID = 1L; // version number used when saving / loading the file
	private final String name;         // e.g. "Coconut Juice"
	private final String description;  // short text about the product
	private final List<ProductOption> options = new ArrayList<>(); // the variants (edited only through addOption / removeOption)

	public Products(String name, String description, List<ProductOption> options) {
		this.name = name;
		this.description = description;
		if (options != null) { //allows a null list
			this.options.addAll(options); //creates a copy instead of making two variables point to the same object
		}
	}

	// getters
	public String getName() { return name; }
	public String getDescription() { return description; }

	// read-only view
	// callers change variants through addOption / removeOption
	public List<ProductOption> getOptions() { return Collections.unmodifiableList(options); }

	// Adds a new variant to this product
	public void addOption(ProductOption option) { options.add(option); }

	// Removes a variant from this product (does nothing if it is not there)
	public void removeOption(ProductOption option) { options.remove(option); }
}
