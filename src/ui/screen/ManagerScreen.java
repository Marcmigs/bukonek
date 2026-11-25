package ui.screen;

import ui.InventoryPanel;
import ui.InventoryPanel.ViewMode;
import ui.EquipmentPanel;
import ui.TransactionPanel;
import ui.RankingDashboard;

import app.Theme;
import ui.Widgets;
import ui.Formats;
import ui.MessageTablePanel.NoticePanel;
import ui.AccountDialogs;

import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import service.ContactBoard;
import service.EquipmentStore;
import service.Inventory;
import service.NoticeBoard;
import service.TransactionLog;
import service.UserDirectory;

public class ManagerScreen extends Screen { 	// another subclass of screen
	private final Inventory products; 			// attributes are final and cannot be edited 
	private final EquipmentStore equipment;		//
	private final TransactionLog log; 			// shared transaction records
	private final NoticeBoard notices; 			// low stock notices sent by clerks
	private final ContactBoard contacts; 		// clerk ratings
	private final UserDirectory directory; 		// all accounts, gives this manager's clerks and hires new ones
	private final String username; 				// the manager currently logged in
	private final Consumer<String> onViewClerk; // opens the dashboard of the clerk picked (set by Main)
	private final Runnable onHired; 			// redraws this screen after a clerk is hired (set by Main)

	// stores everything the screen needs, the page itself is only drawn later in build()
	public ManagerScreen(Inventory products, EquipmentStore equipment, TransactionLog log, NoticeBoard notices,
			UserDirectory directory, ContactBoard contacts, String username, Consumer<String> onViewClerk, Runnable onHired) {
		this.products = products; // initializing the attributes for the current manager
		this.equipment = equipment;
		this.log = log;
		this.notices = notices;
		this.directory = directory;
		this.contacts = contacts;
		this.username = username;
		this.onViewClerk = onViewClerk;
		this.onHired = onHired;
	}

	@Override
	public Node build() { // overriding the abstract method in the parent class
		VBox layout = new VBox(14); // creating new vertical box layout 
		layout.setPadding(new Insets(10)); // set paddings so content doesnt touch border of window
		layout.setStyle("-fx-background-color: " + Theme.creamBg() + ";");

		Label roleBadge = new Label("MANAGER CONSOLE • " + username.toUpperCase());
		roleBadge.setFont(Font.font(Theme.BODY_FONT, FontWeight.BOLD, 11));
		roleBadge.setPadding(new Insets(5, 12, 5, 12));
		roleBadge.setStyle(
			"-fx-background-color: " + Theme.blush() + ";" +
			"-fx-text-fill: " + Theme.brown() + ";" +
			"-fx-background-radius: 999;"
		);

		HBox topRow = new HBox(12, title("MANAGER - INVENTORY CONTROL"), roleBadge);
		topRow.setAlignment(Pos.CENTER_LEFT);

		// only the clerks assigned to this manager
		List<String> clerkUsernames = directory.getClerkUsernames(username);

		// pick a clerk and open their dashboard
		TilePane clerkButtons = new TilePane(Orientation.VERTICAL, 10, 10); // fills down, then starts a new column
		clerkButtons.setPrefRows(5); // buttons per column before wrapping
		for (String clerk : clerkUsernames) {
			Button clerkBtn = new Button(clerk + "   " + Formats.clerkRating(contacts, clerk));
			clerkBtn.setMaxWidth(Double.MAX_VALUE); // all buttons the same width
			Widgets.styleButton(clerkBtn);
			clerkBtn.setOnAction(e -> onViewClerk.accept(clerk));
			clerkButtons.getChildren().add(clerkBtn);
		}

		// hire a new clerk (assigned to this manager)
		Button hireBtn = new Button("Hire Clerk (" + clerkUsernames.size() + "/" + UserDirectory.MAX_CLERKS + ")");
		Widgets.styleButton(hireBtn);
		hireBtn.setDisable(!directory.canHire(username)); // a manager can only have 10 clerks
		hireBtn.setOnAction(e -> new AccountDialogs(directory).hireClerk(username, onHired));

		VBox clerkViewer = new VBox(10, new Label("View clerk dashboard:"), clerkButtons, hireBtn);
		VBox clerkCard = wrapCard("My Clerks", clerkViewer, Theme.green());

		// low stock notices from clerks
		VBox noticeCard = wrapCard("Low Stock Notices", new NoticePanel(notices).build(), Theme.brown());
		HBox clerkRow = new HBox(15, clerkCard, noticeCard);
		HBox.setHgrow(noticeCard, Priority.ALWAYS); // notices take the remaining width

		// this calls the buildControlPanel to build the interactive javaFX interface that shows the inventory
		Node left = new InventoryPanel(products).build(ViewMode.MANAGER); // calls the function that builds the layout of products for a manager
		Node right = new EquipmentPanel(equipment).buildManager(() -> username); // username = the manager who buys equipment
		VBox leftCard = wrapCard("Products Management", left, Theme.green());
		VBox rightCard = wrapCard("Equipment Management", right, Theme.green());

		HBox columns = new HBox(15, leftCard, rightCard); // create column to place the left and right contents
		// horizontal box that fills up the space in the window for the left and right contents
		HBox.setHgrow(leftCard, Priority.ALWAYS);
		HBox.setHgrow(rightCard, Priority.ALWAYS);

		Node transactions = new TransactionPanel(log).buildManagerPanel(username, clerkUsernames); // history + clerk sales ranking
		VBox txCard = wrapCard("All Store Transactions", transactions, Theme.brown());

		// bar charts
		// clerks ranked by total sales and units sold
		VBox dashboardCard = wrapCard("Clerk Sales Ranking", new RankingDashboard(log).build(clerkUsernames), Theme.brown());

		layout.getChildren().addAll(topRow, clerkRow, dashboardCard, columns,
				title("TRANSACTIONS"), txCard);
		return layout;
	}
}