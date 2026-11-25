package ui;

import app.Theme;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Transaction;
import service.TransactionLog;
import service.TransactionLog.SaleChoice;

// builds the transaction history tables and pop-ups ("Record Transaction" and "Void / Refund") for clerks and managers
// all business rules (stock updates, wallet math, refund rates) live in service.TransactionLog
public class TransactionPanel {
	private final TransactionLog log; // backend service storing sales history and executing sales/reversals

	// connects the UI panel to the shared TransactionLog service
	public TransactionPanel(TransactionLog log) {
		this.log = log;
	}

	// builds a transaction history table; includes the "Clerk" column only when showClerk is true (manager's global table)
	private TableView<Transaction> buildTable(boolean showClerk, double height) {
		TableView<Transaction> table = Widgets.table(height, "No transactions yet.");
		table.getColumns().add(Widgets.<Transaction>col("#", 45, tx -> Formats.id(tx.getId())));
		table.getColumns().add(Widgets.<Transaction>col("Date", 110, tx -> Formats.time(tx.getTimestamp())));
		table.getColumns().add(Widgets.<Transaction>col("Item", 190, tx -> tx.getItem()));
		table.getColumns().add(Widgets.<Transaction>col("Qty", 45, tx -> String.valueOf(tx.getQuantity())));
		table.getColumns().add(Widgets.<Transaction>col("Amount", 90, tx -> Formats.money(tx.getAmount())));
		table.getColumns().add(Widgets.<Transaction>col("Customer", 110, tx -> tx.getCustomerName()));
		if (showClerk) {
			table.getColumns().add(Widgets.<Transaction>col("Clerk", 80, tx -> tx.getClerkUsername())); // shown only in manager view
		}
		table.getColumns().add(Widgets.<Transaction>col("Added By", 80, tx -> tx.getRecordedBy())); // tracks who logged the sale
		return table;
	}

	// builds a single clerk's transaction panel; recordedBy differs from clerkUsername when a manager inspects a clerk's dashboard
	public Node buildClerkPanel(String clerkUsername, String recordedBy) {
		boolean own = recordedBy.equals(clerkUsername); // true = clerk viewing their own dashboard; false = manager inspecting

		VBox layout = new VBox(10);
		layout.setPadding(new Insets(10));

		TableView<Transaction> table = buildTable(false, 250); // omits Clerk column since every row belongs to clerkUsername
		Runnable refresh = () -> table.getItems().setAll(log.historyFor(clerkUsername)); // reloads this clerk's sales (newest first)
		refresh.run(); // load initial rows

		// button opens the record-sale pop-up with clerkUsername locked in (no clerk dropdown needed)
		Button recordBtn = new Button(own ? "Record Transaction" : "Record Transaction for " + clerkUsername);
		Widgets.styleButton(recordBtn);
		recordBtn.setOnAction(e -> showRecordDialog(clerkUsername, null, recordedBy, refresh));

		layout.getChildren().addAll(recordBtn, table);
		return layout;
	}

	// builds the manager's global transaction panel with buttons to log a sale for any clerk or void/refund an existing sale
	public Node buildManagerPanel(String managerUsername, List<String> clerkUsernames) {
		VBox layout = new VBox(10);
		layout.setPadding(new Insets(10));

		TableView<Transaction> table = buildTable(true, 250); // includes Clerk column to show who handled each sale
		// reloads all store transactions; adding or deleting also triggers TransactionLog's change listener so ranking charts update
		Runnable refresh = () -> table.getItems().setAll(log.historyFor(null)); // null = fetch everyone's transactions
		refresh.run(); // load initial rows
		log.bindView("transactions-manager", refresh); // redraws when an equipment purchase is added (EquipmentStore.buy)

		// opens the record-sale pop-up with fixedClerk = null so the manager chooses a clerk from clerkUsernames
		Button addBtn = new Button("Add Transaction for Clerk");
		Widgets.styleButton(addBtn);
		addBtn.setOnAction(e -> showRecordDialog(null, clerkUsernames, managerUsername, refresh));

		// opens the void/refund pop-up to reverse a past transaction
		Button deleteBtn = new Button("Void / Refund Transaction");
		Widgets.styleButton(deleteBtn);
		deleteBtn.setOnAction(e -> showDeleteDialog(refresh));

		layout.getChildren().addAll(new HBox(10, addBtn, deleteBtn), table);
		return layout;
	}

	// opens the "Record Transaction" pop-up; shows a clerk dropdown only when fixedClerk is null (manager mode)
	private void showRecordDialog(String fixedClerk, List<String> clerkChoices, String recordedBy, Runnable refresh) {
		List<SaleChoice> sellable = log.sellableChoices(); // fetched fresh each time so prices and stock counts are current
		if (sellable.isEmpty()) { // stop early if no products/variants have been created yet
			new Alert(Alert.AlertType.WARNING,
				"There are no products in the inventory yet. Ask the manager to add some first.").showAndWait();
			return;
		}

		// clerk selector (populated and displayed only when fixedClerk is null)
		ChoiceBox<String> clerkBox = new ChoiceBox<>();
		if (fixedClerk == null) {
			clerkBox.getItems().addAll(clerkChoices);
			if (!clerkChoices.isEmpty()) {
				clerkBox.setValue(clerkChoices.get(0));
			}
		}

		// customer name input (required)
		TextField customerField = new TextField();
		customerField.setPromptText("Customer name");

		// product/variant dropdown formatted with name, option label, price, and available stock
		ChoiceBox<SaleChoice> productBox = new ChoiceBox<>();
		productBox.setConverter(Widgets.converter(c -> c.product().getName() + " - " + c.option().getLabel()
				+ " (₱" + c.option().getPrice() + ", stock: " + c.option().getStock() + ")"));
		productBox.getItems().addAll(sellable);
		productBox.setValue(sellable.get(0));

		TextField qtyField = new TextField();
		qtyField.setPromptText("Quantity (e.g. 2)");

		// read-only live total label (unit price x quantity)
		Label totalLabel = new Label("₱0.00");
		totalLabel.setStyle("-fx-font-weight:bold");
		Runnable updateTotal = () -> {
			SaleChoice c = productBox.getValue();
			double total = 0;
			try {
				int q = Integer.parseInt(qtyField.getText().trim());
				if (c != null && q > 0) {
					total = c.option().getPrice() * q; // compute total only for valid positive integers
				}
			} catch (NumberFormatException ignored) {
				// leave total at ₱0.00 while quantity is blank or non-numeric
			}
			totalLabel.setText(String.format("₱%.2f", total));
		};
		// recalculate total immediately whenever the user types a quantity or switches products
		qtyField.textProperty().addListener((obs, o, n) -> updateTotal.run());
		productBox.valueProperty().addListener((obs, o, n) -> updateTotal.run());

		// include the "Clerk:" row only when the manager is assigning the sale
		Node content = (fixedClerk == null)
			? FormDialog.form(new String[]{"Clerk:", "Customer:", "Product:", "Quantity:", "Total:"}, clerkBox, customerField, productBox, qtyField, totalLabel)
			: FormDialog.form(new String[]{"Customer:", "Product:", "Quantity:", "Total:"}, customerField, productBox, qtyField, totalLabel);

		// validate inputs before closing the pop-up; stock deduction, wallet credit, and autosave happen inside log.recordSale()
		new FormDialog("Record Transaction",
				fixedClerk != null ? "Enter the transaction details" : "Enter the details and pick the clerk it is assigned to", "Confirm")
			.content(content)
			.onConfirm("Failed to record! ", () -> {
				String customer = customerField.getText().trim();
				if (customer.isEmpty()) {
					FormDialog.fail("Please enter the customer's name."); // keeps pop-up open without error prefix
					return;
				}
				int qty;
				try {
					qty = Integer.parseInt(qtyField.getText().trim());
				} catch (NumberFormatException ex) {
					FormDialog.fail("Quantity must be a valid whole number."); // keeps pop-up open without error prefix
					return;
				}
				String clerk = (fixedClerk != null) ? fixedClerk : clerkBox.getValue();
				log.recordSale(clerk, productBox.getValue(), qty, recordedBy, customer); // throws on insufficient stock or invalid clerk
				refresh.run(); // reload the transaction table to display the newly recorded sale
			})
			.showAndConfirm();
	}

	// opens the manager's "Void / Refund Transaction" pop-up with a live impact preview and confirmation prompt
	private void showDeleteDialog(Runnable refresh) {
		List<Transaction> newestFirst = log.historyFor(null);
		if (newestFirst.isEmpty()) { // stop early if there are no recorded transactions to reverse
			new Alert(Alert.AlertType.WARNING, "There are no transactions to void or refund.").showAndWait();
			return;
		}

		// dropdown listing existing transactions newest-first
		ChoiceBox<Transaction> box = new ChoiceBox<>();
		box.setConverter(Widgets.converter(t -> Formats.transactionLine(t, true)));
		box.getItems().addAll(newestFirst);
		box.setValue(box.getItems().get(0));

		// radio button group explaining the difference between Void (full reversal) and Refund (partial money back, no restock)
		ToggleGroup group = new ToggleGroup(); // ensures only Void or Refund can be selected at once
		RadioButton voidBtn = new RadioButton("Void");
		voidBtn.setToggleGroup(group);
		voidBtn.setStyle("-fx-font-weight: bold;");
		// \u2022 renders a bullet point (•)
		Label voidInfo = new Label("The sale is cancelled, as if it never happened.\n"
			+ "\u2022 Stock is returned in full\n"
			+ "\u2022 100% of the money is taken back from the wallet");
		RadioButton refundBtn = new RadioButton("Refund");
		refundBtn.setToggleGroup(group);
		refundBtn.setStyle("-fx-font-weight: bold;");
		Label refundInfo = new Label("The customer is paid back part of the money, but the product is not restocked.\n"
			+ "\u2022 Stock is NOT returned\n"
			+ "\u2022 Only " + Math.round(TransactionLog.REFUND_RATE * 100) + "% of the money is taken back from the wallet");
		for (Label l : new Label[]{voidInfo, refundInfo}) {
			l.setWrapText(true);
			l.setMaxWidth(380);
			l.setStyle("-fx-text-fill: " + Theme.textMuted() + ";");
			l.setPadding(new Insets(0, 0, 0, 22)); // indents description text to align neatly under the radio button label
		}
		voidBtn.setSelected(true); // default to Void
		VBox choice = new VBox(4, voidBtn, voidInfo, refundBtn, refundInfo);
		VBox.setMargin(refundBtn, new Insets(8, 0, 0, 0));

		// live preview showing the exact wallet deduction and stock change for the selected transaction and mode
		Label effect = new Label();
		effect.setWrapText(true);
		effect.setMaxWidth(380);
		effect.setStyle("-fx-font-weight: bold;");
		Runnable updateEffect = () -> {
			Transaction t = box.getValue();
			if (t == null) {
				effect.setText("");
				return;
			}
			boolean voided = voidBtn.isSelected();
			effect.setText("Wallet: -" + Formats.money(TransactionLog.moneyBack(t, voided))
				+ "   |   Stock: " + (voided ? "+" + t.getQuantity() + " returned" : "not returned"));
		};
		// recalculate the preview whenever the selected transaction or Void/Refund radio button changes
		box.valueProperty().addListener((obs, o, n) -> updateEffect.run());
		group.selectedToggleProperty().addListener((obs, o, n) -> updateEffect.run());
		updateEffect.run(); // render initial preview

		new FormDialog("Void / Refund Transaction", "Pick the transaction, then choose what to do with it", "Confirm")
			.content(new VBox(12, FormDialog.form(new String[]{"Transaction:"}, box), choice, effect))
			.onConfirm("Failed! ", () -> {
				Transaction t = box.getValue();
				if (t == null) {
					throw new IllegalArgumentException("Please pick a transaction.");
				}
				boolean voided = voidBtn.isSelected();
				// ask for final confirmation before modifying wallet balance or inventory stock
				Alert sure = new Alert(Alert.AlertType.CONFIRMATION,
					(voided ? "Void" : "Refund") + " transaction #" + String.format("%03d", t.getId()) + "?\n\n"
					+ (voided
						? "Stock (" + t.getQuantity() + ") will be returned to the inventory.\n"
						: "Stock will NOT be returned to the inventory.\n")
					+ Formats.money(TransactionLog.moneyBack(t, voided)) + " will be taken out of the wallet.");
				if (sure.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
					throw new FormDialog.Abort(); // user clicked Cancel; keep the main dialog open without showing an error
				}
				log.deleteTransaction(t, voided); // reverses the transaction in TransactionLog, updates wallet/stock, and saves
				refresh.run(); // removes the reversed transaction from the table
			})
			.showAndConfirm();
	}
}