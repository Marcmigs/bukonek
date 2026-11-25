package ui;

import app.Theme;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.ProductOption;
import model.Products;
import service.Inventory;

public class InventoryPanel {
	private final Inventory inventory;       // the products + the rules
	private final InventoryDialogs dialogs;  // the manager's pop-ups

	public InventoryPanel(Inventory inventory) {
		this.inventory = inventory;
		this.dialogs = new InventoryDialogs(inventory);
	}

	// who is looking at a panel
	// decides what it shows and whether it can be edited
	public enum ViewMode {
		MANAGER, CLERK, DEFAULT; // DEFAULT = the customer catalog

		public boolean canEdit() { return this == MANAGER; } // only the manager gets the buttons
	}

	/**
	 * Builds the panel
	 * Manager = can edit
	 * other modes = view only.
	 *
	 * @param mode who is looking at the panel
	 */
	public Node build(ViewMode mode) {
		VBox layout = new VBox(12);
		layout.setPadding(new Insets(10));

		FlowPane cardsGrid = new FlowPane(14, 14); // cards sit side by side and wrap to the next row
		cardsGrid.setPadding(new Insets(4, 0, 4, 0));

		// clears the grid and draws one card per product 
		// (or a message when there are none)
		Runnable rebuildCards = () -> {
			cardsGrid.getChildren().clear();

			if (inventory.getProducts().isEmpty()) {
				Label emptyMsg = new Label(emptyText(mode));
				emptyMsg.setFont(Font.font(Theme.BODY_FONT, FontWeight.BOLD, 13));
				emptyMsg.setStyle("-fx-text-fill: " + Theme.brown() + ";");
				cardsGrid.getChildren().add(emptyMsg);
				return; // nothing else to draw
			}
			for (Products p : inventory.getProducts()) {
				cardsGrid.getChildren().add(createProductCard(p));
			}
		};
		rebuildCards.run();
		inventory.bindView(rebuildCards); // lets other screens refresh it after a sale

		// the manager's buttons 
		// (clerks and customers never see them)
		if (mode.canEdit()) {
			String item = inventory.getItemName();

			// each button opens a dialog
			// then redraws the cards
			Button addBtn = actionButton("Add New " + item, () -> dialogs.showAddProduct(), rebuildCards);
			Button addOptionBtn = actionButton("Add Variant to " + item, () -> dialogs.showAddOption(), rebuildCards);
			Button stockBtn = actionButton("Adjust Stock of Product Variant", () -> dialogs.showAdjustStock(), rebuildCards);
			Button deleteBtn = actionButton("Delete Product / Variant", () -> dialogs.showDelete(), rebuildCards);
			layout.getChildren().add(new FlowPane(10, 10, addBtn, addOptionBtn, stockBtn, deleteBtn));
		}

		layout.getChildren().addAll(cardsGrid);
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

	// message shown when there are no products 
	// (clerks are told to notify the manager)
	private String emptyText(ViewMode mode) {
		return mode == ViewMode.CLERK ? inventory.getTitle() + " stock is empty. Notify the manager." : "Out of stock.";
	}

	// one product card 
	// name, total stock, description, stock pill and a chip per variant
	private VBox createProductCard(Products p) {
		VBox card = new VBox(8);
		card.setPrefWidth(250);
		card.setPadding(new Insets(14));
		card.setStyle(
			"-fx-background-color: " + Theme.creamBg() + ";" +
			"-fx-background-radius: 14;" +
			"-fx-border-color: " + Theme.tan() + ";" +
			"-fx-border-width: 1.2;" +
			"-fx-border-radius: 14;" +
			"-fx-effect: dropshadow(gaussian, rgba(88, 42, 20, 0.06), 8, 0, 0, 2);" // soft shadow
		);

		List<ProductOption> opts = p.getOptions();
		// the stock pill shows the stock of ALL variants added together
		int totalStock = 0;
		for (ProductOption o : opts) {
			totalStock += o.getStock();
		}

		Label nameLbl = new Label(p.getName());
		nameLbl.setFont(Font.font(Theme.HEADING_FONT, FontWeight.BOLD, 15));
		nameLbl.setStyle("-fx-text-fill: " + Theme.brown() + ";");
		nameLbl.setWrapText(true);

		// top-right of the card, the FIRST variant's price 
		// (only a summary, every price is in the chips below)
		// a product with no variants shows its total stock instead
		Label priceOrStockLbl = new Label(!opts.isEmpty() ? "₱" + opts.get(0).getPrice() : "Stock: " + totalStock);
		priceOrStockLbl.setFont(Font.font(Theme.HEADING_FONT, FontWeight.BOLD, 13));
		priceOrStockLbl.setStyle("-fx-text-fill: " + Theme.green() + ";");

		// the spacer pushes the price to the right edge of the title row
		Region topSpacer = new Region();
		HBox.setHgrow(topSpacer, Priority.ALWAYS);
		HBox titleRow = new HBox(8, nameLbl, topSpacer, priceOrStockLbl);
		titleRow.setAlignment(Pos.TOP_LEFT);

		Label descLbl = new Label(p.getDescription());
		descLbl.setFont(Font.font(Theme.BODY_FONT, 12));
		descLbl.setStyle("-fx-text-fill: " + Theme.textMuted() + ";");
		descLbl.setWrapText(true);

		Label stockPill = new Label();
		stockPill.setFont(Font.font(Theme.BODY_FONT, FontWeight.BOLD, 11));
		stockPill.setPadding(new Insets(3, 10, 3, 10));
		Widgets.stockPill(stockPill, totalStock); // sets the text and colour (Out of Stock / Low Stock / In Stock)

		card.getChildren().addAll(titleRow, descLbl, stockPill);

		// variants section
		// skipped when the product has no variants
		if (!opts.isEmpty()) {
			Label variantHeading = new Label("Variants");
			variantHeading.setFont(Font.font(Theme.BODY_FONT, FontWeight.BOLD, 11));
			variantHeading.setStyle("-fx-text-fill: " + Theme.brown() + ";");

			// one rounded "chip" per variant
			// label • price (stock)
			VBox chips = new VBox(6);
			for (ProductOption opt : opts) {
				Label chip = new Label(opt.getLabel() + " • ₱" + opt.getPrice() + " (" + opt.getStock() + ")");
				chip.setFont(Font.font(Theme.BODY_FONT, FontWeight.NORMAL, 11));
				chip.setPadding(new Insets(4, 10, 4, 10));
				chip.setMaxWidth(Double.MAX_VALUE); // stretch to the full width of the card
				chip.setStyle(
					"-fx-background-color: " + Theme.surface() + ";" +
					"-fx-text-fill: " + Theme.brown() + ";" +
					"-fx-border-color: " + Theme.tan() + ";" +
					"-fx-border-radius: 999;" +     // very large radius = pill shape
					"-fx-background-radius: 999;"
				);
				chips.getChildren().add(chip);
			}
			card.getChildren().addAll(variantHeading, chips);
		}
		return card;
	}
}
