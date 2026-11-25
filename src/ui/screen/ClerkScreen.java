package ui.screen;

import ui.InventoryPanel;

import ui.InventoryPanel.ViewMode;
import ui.EquipmentPanel;
import ui.TransactionPanel;

import app.Theme;
import ui.Widgets;
import ui.Formats;
import ui.MessageTablePanel.NoticePanel;
import ui.MessageTablePanel.ContactPanel;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import service.ContactBoard;
import service.EquipmentStore;
import service.Inventory;
import service.NoticeBoard;
import service.TransactionLog;

public class ClerkScreen extends Screen { // class that has the attributes and methods of Screen (superclass)
    private final Inventory products; 		// ensures that the products cannot be edited since a clerk has an access
    private final EquipmentStore equipment; // ensures that equipments cannot be edited here since a clerk has an access
    private final TransactionLog log;		// 
    private final ContactBoard contacts; 	// customer contact requests + ratings
    private final NoticeBoard notices; 		// where low stock notices to the manager are sent
    private final String username; 			// the clerk whose dashboard this is
    private final String recordedBy; 		// who records new transactions (the clerk, or the manager when viewing)
    private final Runnable onBack; 			// null = the clerk themself, not null = manager viewing this clerk

    // the clerk's own dashboard
    public ClerkScreen(Inventory products, EquipmentStore equipment, TransactionLog log, NoticeBoard notices, 
    		ContactBoard contacts, String username) {
        this(products, equipment, log, notices, contacts, username, username, null);
    }

    // manager viewing a clerk's dashboard ; onBack returns to the manager screen
    public ClerkScreen(Inventory products, EquipmentStore equipment, TransactionLog log, NoticeBoard notices, 
    		ContactBoard contacts, String clerkUsername, String managerUsername, Runnable onBack) { // constructor 
        this.products = products; // initializes the attributes for the current clerk 
        this.equipment = equipment;
        this.log = log;
        this.notices = notices;
        this.contacts = contacts;
        this.username = clerkUsername;
        this.recordedBy = managerUsername;
        this.onBack = onBack;
    }

    @Override
	public Node build() { // overrides the abstract method in the superclass (Screen)
		boolean managerView = (onBack != null); // true when a manager is looking at this clerk

		VBox layout = new VBox(14); // vertical box, 14 spacing between sections
		layout.setPadding(new Insets(10)); // so content doesn't touch the border of the window
		layout.setStyle("-fx-background-color: " + Theme.creamBg() + ";");

		// small pill showing the clerk's name and average star rating
		Label roleBadge = new Label("CLERK WORKSPACE • " + username.toUpperCase() + "  |  " + Formats.clerkRating(contacts, username));
		roleBadge.setFont(Font.font(Theme.BODY_FONT, FontWeight.BOLD, 11));
		roleBadge.setPadding(new Insets(5, 12, 5, 12));
		roleBadge.setStyle(
			"-fx-background-color: " + Theme.greenSoft() + ";" +
			"-fx-text-fill: " + Theme.green() + ";" +
			"-fx-background-radius: 999;" // very large radius = pill shape
		);

		// top row
		// a manager gets a back button, the clerk does not
		HBox topRow;
		if (managerView) {
			Button back = new Button("< Back to Manager View");
			Widgets.styleButton(back);
			back.setOnAction(e -> onBack.run());
			topRow = new HBox(12, back, title("CLERK DASHBOARD: " + username), roleBadge);
		} else {
			topRow = new HBox(12, title("CLERK - INVENTORY VIEW"), roleBadge);
		}
		topRow.setAlignment(Pos.CENTER_LEFT);

		layout.getChildren().add(topRow);

		// the products and equipment are only shown to the clerk themself (the manager already has them in the manager view)
		if (!managerView) {
			Node left = new InventoryPanel(products).build(ViewMode.CLERK); // view-only version of the inventory
			Node right = new EquipmentPanel(equipment).buildView();         // view-only version of the equipment

			VBox leftCard = wrapCard("Products Inventory", left, Theme.green());
			VBox rightCard = wrapCard("Equipment Inventory", right, Theme.green());

			HBox columns = new HBox(15, leftCard, rightCard); // two columns side by side
			HBox.setHgrow(leftCard, Priority.ALWAYS); // both cards grow equally to fill the width
			HBox.setHgrow(rightCard, Priority.ALWAYS);

			layout.getChildren().add(columns);
		}

		// only the clerk themself can notify the manager (the manager is the one who receives it)
		if (!managerView) {
			layout.getChildren().add(wrapCard("Notify Manager", new NoticePanel(notices).buildNotifyPanel(username), Theme.brown()));
		}

		// customer requests sent to this clerk (shown in both views)
		layout.getChildren().add(wrapCard("Customer Contact Requests", new ContactPanel(contacts, username).build(), Theme.brown())); // same layout as the manager notice board

		// this clerk's transactions
		// the card title changes depending on who is looking
		Node transactions = new TransactionPanel(log).buildClerkPanel(username, recordedBy);
		VBox txCard = wrapCard(managerView ? "Recorded Transactions of " + username : "My Recorded Transactions",
			transactions, Theme.brown());

		layout.getChildren().addAll(
			title(managerView ? "TRANSACTIONS OF " + username.toUpperCase() : "MY TRANSACTIONS"),
			txCard);
		return layout;
	}
}