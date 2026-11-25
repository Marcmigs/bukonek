package ui;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import model.Equipment;
import model.ProductOption;
import model.Products;
import service.Inventory;

public class InventoryDialogs {
	private final Inventory inventory; // the products + the rules

	public InventoryDialogs(Inventory inventory) {
		this.inventory = inventory;
	}

	// one entry of a dropdown = a product + one of its variants 
	// option == null means "the whole product" (delete)
	// text = what the dropdown shows for this entry
	private record Pick(Products product, ProductOption option, String text) {}

	// true (after warning the manager) when there are no products
	// so the pop-up should not open
	private boolean nothingToPick() {
		if (inventory.getProducts().isEmpty()) {
			new Alert(Alert.AlertType.WARNING,
				"There are no " + inventory.getTitle().toLowerCase() + " yet. Add one first.").showAndWait();
			return true;
		}
		return false;
	}

	// every variant of every product (used by Adjust Stock)
	private List<Pick> stockPicks() {
		List<Pick> picks = new ArrayList<>();
		for (Products p : inventory.getProducts()) {
			for (ProductOption o : p.getOptions()) {
				picks.add(new Pick(p, o, p.getName() + " - " + o.getLabel() + " (₱" + o.getPrice() + ", stock: " + o.getStock() + ")"));
			}
		}
		return picks;
	}

	// whole products + each variant (used by Delete)
	private List<Pick> deletePicks() {
		List<Pick> picks = new ArrayList<>();
		for (Products p : inventory.getProducts()) {
			picks.add(new Pick(p, null, "ENTIRE PRODUCT: " + p.getName() + " (" + p.getOptions().size() + " variants)")); // option = null -> whole product
			for (ProductOption o : p.getOptions()) {
				picks.add(new Pick(p, o, "      " + p.getName() + " - " + o.getLabel() + " (variant only)")); // indented so it looks nested under its product
			}
		}
		return picks;
	}

	// a dropdown of the given picks with the first one selected
	// callers must make sure the list is not empty
	private ChoiceBox<Pick> pickBox(List<Pick> picks) {
		ChoiceBox<Pick> box = new ChoiceBox<>();
		box.setConverter(Widgets.converter(Pick::text)); // show each entry's text
		box.getItems().addAll(picks);
		box.setValue(box.getItems().get(0)); // would crash on an empty list, hence the note above
		return box;
	}
	
	// add a new product (variants are collected in a small second pop-up)
	public void showAddProduct() {
		String itemName = inventory.getItemName();
		// a product needs equipment
		// one per product, picked from what the manager owns and has not linked yet
		boolean needsEquipment = inventory.requiresEquipment();
		ChoiceBox<Equipment> equipBox = new ChoiceBox<>();
		equipBox.setConverter(Widgets.equipmentConverter());
		if (needsEquipment) {
			equipBox.getItems().addAll(inventory.availableEquipment());
			// no free equipment
			// stop here, before the manager types anything
			if (equipBox.getItems().isEmpty()) {
				new Alert(Alert.AlertType.WARNING,
					"You need an unlinked equipment to make a product. Buy one in the Equipment panel first.").showAndWait();
				return;
			}
			equipBox.setValue(equipBox.getItems().get(0));
		}

		TextField nameField = new TextField();
		nameField.setPromptText("Product Name (e.g. Coconut Juice)");
		TextField descField = new TextField();
		descField.setPromptText("Description (e.g. Pure, naturally refreshing)");
		List<ProductOption> tempOptions = new ArrayList<>(); // variants added through the second pop-up

		// read-only box that lists the variants added so far
		TextArea variantsDisplay = new TextArea();
		variantsDisplay.setEditable(false);
		variantsDisplay.setPrefHeight(120);
		variantsDisplay.setPromptText("No variants added yet.");

		// rewrites the box from tempOptions
		// runs after each variant is added
		Runnable refreshVariantsBox = () -> {
			if (tempOptions.isEmpty()) {
				variantsDisplay.setText("No variants added yet.");
				return;
			}
			// builds one numbered entry per variant
			// "1. Variant: 500g | Price: ₱100 | Stock: 10"
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < tempOptions.size(); i++) {
				ProductOption o = tempOptions.get(i);
				sb.append(i + 1).append(". Variant: ").append(o.getLabel())
				  .append(" | Price: ₱").append(o.getPrice())
				  .append(" | Stock: ").append(o.getStock()).append("\n");
			}
			variantsDisplay.setText(sb.toString());
		};

		Button addVariantBtn = new Button("Add Variant");
		Widgets.styleButton(addVariantBtn);
		addVariantBtn.setOnAction(e -> showVariantDialog(tempOptions, refreshVariantsBox));

		// the Equipment row appears only when the inventory requires equipment
		Node content = needsEquipment
			? FormDialog.form(new String[]{"Name:", "Description:", "Equipment:"}, nameField, descField, equipBox)
			: FormDialog.form(new String[]{"Name:", "Description:"}, nameField, descField);
		VBox layout = new VBox(10, content, new Label("Variants:"), addVariantBtn, variantsDisplay);
		layout.setPadding(new Insets(10));

		new FormDialog("Add New " + itemName, "Enter the " + itemName.toLowerCase() + " details", "Confirm")
			.content(layout)
			// the code below runs when Confirm is pressed
			// if it throws, the error is shown and the pop-up stays open
			.onConfirm("Failed to save product: ", () -> {
				String name = nameField.getText().trim();
				String desc = descField.getText().trim();
				if (name.isEmpty()) {
					throw new IllegalArgumentException("Product name cannot be empty.");
				}
				if (tempOptions.isEmpty()) {
					throw new IllegalArgumentException("Please add at least one variant.");
				}
				Equipment chosen = equipBox.getValue();
				if (needsEquipment && chosen == null) {
					throw new IllegalArgumentException("Please pick the equipment this product uses.");
				}
				// only used for the confirmation message below
				// the real total is computed again inside Inventory
				int totalInitialStock = 0;
				for (ProductOption o : tempOptions) {
					totalInitialStock += o.getStock();
				}
				// adds the product (and links the equipment)
				// returns a sentence about the equipment uses spent, or null
				String usage = inventory.addLinkedProduct(new Products(name, desc, tempOptions), needsEquipment ? chosen : null);
				if (usage != null) {
					new Alert(Alert.AlertType.INFORMATION,
						"Added \"" + name + "\" (initial stock: " + totalInitialStock + ")." + usage).showAndWait();
				}
			})
			.showAndConfirm();
	}

	// second pop-up
	// one variant (name, price, stock) goes into the temporary list
	private void showVariantDialog(List<ProductOption> tempOptions, Runnable afterAdd) {
		TextField vLabel = new TextField();
		vLabel.setPromptText("Variant Name (e.g. 500g)");
		TextField vPrice = new TextField();
		vPrice.setPromptText("Price (e.g. 100)");
		TextField vStock = new TextField();
		vStock.setPromptText("Stock (e.g. 10)");

		new FormDialog("Add Product Variant", "Enter Variant Details", "OK")
			.content(FormDialog.form(new String[]{"Variant Name:", "Price:", "Stock:"}, vLabel, vPrice, vStock))
			.onConfirm("Invalid variant input: ", () -> {
				String lbl = vLabel.getText().trim();
				// a non-number throws NumberFormatException, which FormDialog shows as an error
				double prc = Double.parseDouble(vPrice.getText().trim());
				int stk = Integer.parseInt(vStock.getText().trim());
				if (lbl.isEmpty()) throw new IllegalArgumentException("Variant name cannot be empty.");
				if (prc < 0 || stk < 0) throw new IllegalArgumentException("Price and stock cannot be negative.");
				tempOptions.add(new ProductOption(lbl, prc, stk));
			})
			.showAndConfirm();
		afterAdd.run(); // update the list in the first pop-up (also runs if this one was cancelled, which is harmless)
	}

	// restock / reduce stock of one variant
	public void showAdjustStock() {
		if (nothingToPick()) return;
		ChoiceBox<Pick> pickBox = pickBox(stockPicks()); // only existing items can be picked
		TextField amountField = new TextField();
		amountField.setPromptText("Stock change amount (e.g. 10 or -5)");

		new FormDialog("Adjust Stock (" + inventory.getTitle() + ")",
				"Pick the product variant and enter the stock change", "Confirm")
			.content(FormDialog.form(new String[]{"Variant:", "Stock change:"}, pickBox, amountField))
			.onConfirm("Failed to update stock! ", () -> {
				Pick pick = pickBox.getValue();
				int change;
				try {
					change = Integer.parseInt(amountField.getText().trim());
				} catch (NumberFormatException ex) {
					FormDialog.fail("Stock change must be a valid whole number."); // throws, so the code below is skipped
					return; // never reached
				}
				// a positive change is a restock and spends equipment uses
				// usage is the sentence about it (or null)
				String usage = inventory.adjustStock(pick == null ? null : pick.product(), pick == null ? null : pick.option(), change);
				if (usage != null) {
					new Alert(Alert.AlertType.INFORMATION, "Restocked +" + change + ".\n" + usage).showAndWait();
				}
			})
			.showAndConfirm();
	}

	// add a new variant to an existing product
	public void showAddOption() {
		if (nothingToPick()) return;

		// dropdown of the existing products
		// each showing how many variants it has
		ChoiceBox<Products> productBox = new ChoiceBox<>();
		productBox.setConverter(Widgets.converter(p -> p.getName() + " (" + p.getOptions().size() + " variants)"));
		productBox.getItems().addAll(inventory.getProducts());
		productBox.setValue(inventory.getProducts().get(0));

		TextField labelField = new TextField();
		labelField.setPromptText("Variant (e.g. 2kg)");
		TextField priceField = new TextField();
		priceField.setPromptText("Price (e.g. 300)");
		TextField stockField = new TextField();
		stockField.setPromptText("Stock (e.g. 5)");

		new FormDialog("Add Variant (" + inventory.getTitle() + ")", "Add a new variant to an existing product", "Confirm")
			.content(FormDialog.form(new String[]{"Product:", "Variant:", "Price:", "Stock:"}, productBox, labelField, priceField, stockField))
			.onConfirm("Failed to add variant! ", () -> {
				double price;
				int stock;
				try {
					price = Double.parseDouble(priceField.getText().trim());
					stock = Integer.parseInt(stockField.getText().trim());
				} catch (NumberFormatException ex) {
					FormDialog.fail("Price and stock must be valid numbers."); // throws, so the code below is skipped
					return;
				}
				// Inventory checks the other rules 
				// (empty label, negative values, equipment uses for the starting stock)
				String usage = inventory.addOptionToProduct(productBox.getValue(), labelField.getText().trim(), price, stock);
				if (usage != null) {
					new Alert(Alert.AlertType.INFORMATION, "Variant added.\n" + usage).showAndWait();
				}
			})
			.showAndConfirm();
	}

	// one pop-up for both deletes
	// the dropdown entry decides product or variant
	// asks "are you sure?" first
	public void showDelete() {
		if (nothingToPick()) return;
		ChoiceBox<Pick> pickBox = pickBox(deletePicks());

		new FormDialog("Delete (" + inventory.getTitle() + ")",
				"Pick an ENTIRE PRODUCT to delete it with all its variants,\nor pick a single variant to delete only that one.", "Confirm")
			.content(FormDialog.form(new String[]{"Delete:"}, pickBox))
			.onConfirm("Failed to delete! ", () -> {
				Pick pick = pickBox.getValue();
				if (pick == null) {
					throw new IllegalArgumentException("Please pick something to delete.");
				}
				// wording for the confirmation question
				// depending on what was picked
				String what = (pick.option() == null)
					? "the ENTIRE product \"" + pick.product().getName() + "\" and all its variants"
					: "variant \"" + pick.option().getLabel() + "\" of " + pick.product().getName();

				// last chance to back out
				// nothing deleted yet
				Alert sure = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + what + "?");
				if (sure.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
					throw new FormDialog.Abort(); // keep the pop-up open
				}
				// option == null means the whole product was picked
				if (pick.option() == null) {
					inventory.deleteProduct(pick.product());
				} else {
					inventory.deleteOption(pick.product(), pick.option());
				}
			})
			.showAndConfirm();
	}
}
