package ui;

import app.Theme;
import java.util.List;
import java.util.function.Supplier;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import model.Equipment;
import model.Products;
import service.EquipmentStore;
import service.Wallet;

public class EquipmentPanel {
	private final EquipmentStore store; // the equipment the manager owns + the rules
	private final Wallet wallet;        // the manager's money (used for the balance label and buying)

	public EquipmentPanel(EquipmentStore store) {
		this.store = store;
		this.wallet = store.getWallet();
	}

	// clerk side 
	// just the list, nothing to press
	public Node buildView() { return build(false, null); }

	// manager side
	// wallet balance + buy / link buttons + the list
	// currentUser = gives the name of the manager who is logged in (recorded when buying)
	public Node buildManager(Supplier<String> currentUser) { return build(true, currentUser); }

	// builds the table for both sides
	// the manager version adds the balance label and the buttons above it
	private Node build(boolean manager, Supplier<String> currentUser) {
		VBox layout = new VBox(10);
		layout.setPadding(new Insets(10));

		// the table is a little taller for the manager because the buttons are above it
		TableView<Equipment> table = Widgets.table(manager ? 220 : 200, "No equipment yet. Buy one to be able to add products.");
		table.getColumns().add(Widgets.<Equipment>col("Equipment", 100, e -> e.getDisplayName()));
		table.getColumns().add(Widgets.<Equipment>col("Uses Left", 100, e -> e.getUsesLeft() + " / " + e.getMaxUses()));
		table.getColumns().add(Widgets.<Equipment>col("Linked Product", 100, e -> {
			Products p = store.linkedProduct(e); // null when not linked (or its product was deleted)
			return p == null ? "Not linked" : p.getName();
		}));

		// equipment that is low on uses gets a tan row with bold brown text (clerks see it too)
		table.setRowFactory(tv -> new TableRow<Equipment>() {
			@Override
			protected void updateItem(Equipment e, boolean empty) {
				super.updateItem(e, empty);
				if (empty || e == null || !e.isLowOnUses()) {
					setStyle(""); // normal row, remove any old highlight (rows get reused)
				} else {
					setStyle("-fx-background-color: " + Theme.tan() + ";"
						+ "-fx-text-background-color: " + Theme.brown() + ";"
						+ "-fx-font-weight: bold;");
				}
			}
		});

		// reloads the rows from the store (sorted by name)
		Runnable refresh = () -> {
			table.getItems().setAll(store.getSortedByName());
			table.refresh(); // makes the rows re-check their highlight after uses change
		};
		refresh.run();
		store.bindView("equipment", refresh); // the store redraws this table whenever equipment changes

		if (manager) {
			// the label is BOUND to the wallet, so it updates by itself after every sale or purchase
			Label balance = new Label();
			balance.setStyle("-fx-font-weight: bold;");
			balance.textProperty().bind(Bindings.createStringBinding(
				() -> "Wallet balance: " + Formats.money(wallet.getBalance()), wallet.balanceProperty()));

			// each button opens its dialog, then refreshes the table
			Button buyBtn = actionButton("Buy Equipment", () -> showBuyDialog(currentUser), refresh);
			Button linkBtn = actionButton("Link Equipment to Product", this::showLinkDialog, refresh);
			Button removeBtn = actionButton("Remove Equipment", this::showRemoveDialog, refresh);
			layout.getChildren().addAll(balance, new FlowPane(10, 10, buyBtn, linkBtn, removeBtn));
		}
		layout.getChildren().add(table);
		return layout;
	}

	// a styled button that runs a dialog and then a refresh 
	// (the dialog blocks, so the refresh runs after it closes)
	private Button actionButton(String text, Runnable dialog, Runnable afterwards) {
		Button btn = new Button(text);
		Widgets.styleButton(btn);
		btn.setOnAction(e -> { dialog.run(); afterwards.run(); });
		return btn;
	}

	// pick one item from the fixed shop list and pay for it with the wallet
	private void showBuyDialog(Supplier<String> currentUser) {
		ChoiceBox<EquipmentStore.Model> box = new ChoiceBox<>(); // only the fixed list can be picked
		box.setConverter(Widgets.shopConverter()); // how each shop item is written in the dropdown
		box.getItems().addAll(EquipmentStore.catalog());
		box.setValue(box.getItems().get(0));

		// showAndConfirm() is true only when the purchase really went through
		boolean bought = new FormDialog("Buy Equipment", "Wallet balance: " + Formats.money(wallet.getBalance()), "Buy")
			.content(FormDialog.form(new String[]{"Equipment:"}, box))
			.onConfirm("Failed to buy! ", () -> store.buy(box.getValue(), currentUser.get())) // throws if the wallet cannot pay
			.showAndConfirm();
		if (bought) {
			new Alert(Alert.AlertType.INFORMATION,
				"Bought! Link it to a product with \"Link Equipment to Product\" (or pick it when adding a new product).").showAndWait();
		}
	}

	// give a product its equipment
	// linking replaces the product's current equipment
	private void showLinkDialog() {
		// two things must exist first
		// a product, and a free equipment with uses left
		if (store.getProductInventory().getProducts().isEmpty()) {
			new Alert(Alert.AlertType.WARNING, "There are no products yet. Add a product first.").showAndWait();
			return;
		}
		List<Equipment> free = store.available(); // not linked and still has uses left
		if (free.isEmpty()) {
			new Alert(Alert.AlertType.WARNING, "No unlinked equipment with uses left. Buy equipment first.").showAndWait();
			return;
		}
		ChoiceBox<Equipment> equipBox = new ChoiceBox<>();
		equipBox.setConverter(Widgets.equipmentConverter());
		equipBox.getItems().addAll(free);
		equipBox.setValue(free.get(0));

		ChoiceBox<Products> productBox = new ChoiceBox<>();
		// each product shows its current equipment, so the manager sees what will be replaced
		productBox.setConverter(Widgets.converter(p -> {
			Equipment now = store.findFor(p); // null when the product has no equipment
			return p.getName() + (now == null ? " (no equipment)" : " (now: " + now.getDisplayName() + ")");
		}));
		productBox.getItems().addAll(store.getProductInventory().getProducts());
		productBox.setValue(productBox.getItems().get(0));

		new FormDialog("Link Equipment", "A product uses one equipment. Linking replaces the product's current one.", "Link")
			.content(FormDialog.form(new String[]{"Equipment:", "Product:"}, equipBox, productBox))
			.onConfirm("Failed to link! ", () -> store.link(equipBox.getValue(), productBox.getValue()))
			.showAndConfirm();
	}

	// throw away a piece of equipment (no refund)
	// asks "are you sure?" first
	private void showRemoveDialog() {
		if (store.isEmpty()) {
			new Alert(Alert.AlertType.WARNING, "There is no equipment to remove.").showAndWait();
			return;
		}
		ChoiceBox<Equipment> equipBox = new ChoiceBox<>();
		// each entry shows the uses left and the linked product, so the manager knows what is being removed
		equipBox.setConverter(Widgets.converter(e -> {
			Products p = store.linkedProduct(e);
			return e.getDisplayName() + " (uses left: " + e.getUsesLeft() + "/" + e.getMaxUses() + ")"
				+ (p == null ? "" : " - linked to " + p.getName());
		}));
		equipBox.getItems().addAll(store.getSortedByName());
		equipBox.setValue(equipBox.getItems().get(0));

		new FormDialog("Remove Equipment", "Pick the equipment to remove. This cannot be undone.", "Remove")
			.content(FormDialog.form(new String[]{"Equipment:"}, equipBox))
			.onConfirm("Failed to remove! ", () -> {
				Equipment pick = equipBox.getValue();
				if (pick == null) {
					throw new IllegalArgumentException("Please pick the equipment to remove.");
				}
				Products p = store.linkedProduct(pick);
				// the warning also tells the manager when a product will be left without equipment
				String warning = "Remove " + pick.getDisplayName() + "? It will not be refunded."
					+ (p == null ? "" : "\n\n\"" + p.getName() + "\" will have no equipment and cannot be restocked until you link new equipment.");
				if (new Alert(Alert.AlertType.CONFIRMATION, warning).showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
					throw new FormDialog.Abort(); // backed out, keep the pop-up open
				}
				store.remove(pick);
			})
			.showAndConfirm();
	}
}