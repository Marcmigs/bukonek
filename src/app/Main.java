/**

Bukonek is a small local store in Los Baños, Laguna
that sells fresh coconut products, such as coconut
juice in different sizes, taking its name from buko,
the Filipino word for young coconut. To support daily
store operations, the project introduces the Bukonek
application, a desktop inventory and store-management
system built around three interconnected roles: customers,
clerks, and managers.

Developed by: Decena, De Guzman, and Espiritu
In Fulfillment of the Project Requirements of: CMSC 22

Default user logins:

manager->123, 
clerk01 // 123
clerk02 // 123
clerk03 // 123
clerk04 // 123
clerk05 // 123

*/

package app;

import java.util.List;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import persistence.DataStore;
import service.ContactBoard;
import service.EquipmentStore;
import service.Inventory;
import service.NoticeBoard;
import service.Store;
import service.TransactionLog;
import service.UserDirectory;
import service.Wallet;
import ui.AppShell;
import export.CatalogPdf;
import java.io.File;
import ui.ContactDialogs;
import ui.AccountDialogs;
import ui.screen.ClerkScreen;
import ui.screen.DefaultScreen;
import ui.screen.ManagerScreen;
import ui.screen.Screen;
import ui.screen.SettingsScreen;
import users.Manager;
import users.User;

// starts the app, creates the data classes, and switches between screens ; the look is in ui.*, the rules in service.*
public class Main extends Application implements AppShell.Controller {
	// creates the variables to be used by the entire app
	private final Inventory productInventory = new Inventory("Products", "Product"); // which section does it belong (main product, variants), individual products itself
	private final Wallet wallet = new Wallet(); // balance of the entire business, clerk sales (+), manager equipment expenses (-)
	private final EquipmentStore equipmentStore = new EquipmentStore(productInventory, wallet); // owned equipment (separate from products)
	private final UserDirectory directory = new UserDirectory(); // pre-built login credentials until a save file brings back the saved accounts
	private final TransactionLog transactionLog = new TransactionLog(productInventory, wallet); // all recorded transactions
	private final ContactBoard contactBoard = new ContactBoard(); // customer contact requests + clerk ratings
	private final NoticeBoard noticeBoard = new NoticeBoard(); // low stock notices from clerks to the manager

	// the window parts and the current state
	private final AppShell shell = new AppShell(this); // header, sidebar, footer
	private final ContactDialogs contactDialogs = new ContactDialogs(contactBoard); // contact a clerk/rate a clerk popup
	private final BorderPane root = new BorderPane(); // layout containers
	private Stage window; // primary desktop window
	private User current = null; // current user tracker; null = nobody logged in which opens the customer view
	private String activeNavKey = "CATALOG"; // which sidebar button is highlighted
	private Runnable redraw = null; // redraws whichever screen is open (used by the save button), prevents it from going back to homescreen

	public static void main(String[] args) { launch(args); }
 
	@Override // called by JavaFX
	public void start(Stage stage) {
		window = stage; // saves a reference to the primary desktop window (so that popups can be attached)
		Settings.load(); // load saved appearance / language / text size
		applyTheme(); // applies the matching css styles to root 
		stage.setScene(new Scene(root, 1020, 840)); // creates a window canvas around root
		stage.setTitle("Bukonek - Inventory System"); // main title
		DataStore.load(productInventory, equipmentStore, transactionLog, noticeBoard, directory, contactBoard, wallet); // bring back saved data (if any)

		// save everything after every change (every data class is a Store, so one loop covers them all)
		Runnable save = () -> DataStore.save(productInventory, equipmentStore, transactionLog, noticeBoard, directory, contactBoard, wallet);
		for (Store s : List.of(productInventory, equipmentStore, transactionLog, noticeBoard, directory, contactBoard)) { // loops and saves all
			s.setOnChange(save);
		}
		// done after load on purpose: loading must not spend equipment uses or trigger saves
		productInventory.setEquipmentStore(equipmentStore); // connects products to equipment, creating or restocking products deducts equipment uses
		equipmentStore.setTransactionLog(transactionLog); // equipment purchases now show up in the transaction log

		showDefault();
		stage.show();
	}

	// showscreens
	
	// each showXxx() method does 3 things: 
	// (1) remembers which sidebar button is active (CATALOG, WORKSPACE, SETTINGS)
	// (2) remembers how to redraw itself (used by the Save button)
	// (3) and shows its screen, passes it to show(...)

	private void showDefault() { // public logged out view
		current = null; // no current user
		activeNavKey = "CATALOG";
		redraw = this::showDefault;
		show(new DefaultScreen(productInventory));
	}

	private void showManager() {
		activeNavKey = "WORKSPACE";
		redraw = this::showManager; // showManager again = redraw after a hire
		show(new ManagerScreen(productInventory, equipmentStore, transactionLog, noticeBoard, directory, contactBoard,
			current.getUsername(), this::showClerkAsManager, this::showManager));
	}

	private void showClerk() {
		activeNavKey = "WORKSPACE";
		redraw = this::showClerk;
		show(new ClerkScreen(productInventory, equipmentStore, transactionLog, noticeBoard, contactBoard, current.getUsername()));
	}

	private void showClerkAsManager(String clerkUsername) { // manager opens one clerk's dashboard
		activeNavKey = "WORKSPACE";
		redraw = () -> showClerkAsManager(clerkUsername);
		show(new ClerkScreen(productInventory, equipmentStore, transactionLog, noticeBoard, contactBoard,
			clerkUsername, current.getUsername(), this::showManager)); // back button returns to manager view
	}

	private void showSettings() {
		activeNavKey = "SETTINGS";
		redraw = this::showSettings;
		show(new SettingsScreen(() -> { applyTheme(); showSettings(); })); // any change : re-apply colours + redraw
	}

	// puts a screen in the window. the screen's content goes in a scrollable centre area,
	// and the header, sidebar and footer are rebuilt around it (so they follow the current theme and language)
	private void show(Screen screen) {
		VBox box = new VBox(12, screen.build()); // calls build method to generate ui, wraps the screen inside a vertical box
		box.setPadding(new Insets(14, 18, 20, 18)); // adds padding
		box.setStyle("-fx-background-color: " + Theme.creamBg() + ";"); // background color set

		ScrollPane sp = new ScrollPane(box); // making the screen scrollable
		sp.setFitToWidth(true); // sets fit to width to true
		sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // only scroll up and down
		sp.setStyle( // set styling
			"-fx-background: " + Theme.creamBg() + ";" +
			"-fx-background-color: " + Theme.creamBg() + ";" +
			"-fx-border-color: transparent;"
		);

		// place each section into its corresponding region on the borderframe
		root.setTop(shell.header());
		root.setLeft(shell.sidebar());
		root.setCenter(sp);
		root.setBottom(shell.footer());
	}

	// colors, dark mode and text size for the whole window
	private void applyTheme() {
		Settings.applyColors(); // recalculates the color palette based on users chosen color and appearance setting
		String s = "-fx-background-color: " + Theme.creamBg() + ";-fx-font-size: " + Settings.fontPx() + "px;"; // main background color and font size
		if (Settings.dark) s += "-fx-base: #3a3b36;-fx-control-inner-background: #2f302b;"; // dark text areas / lists
		root.setStyle(s); // apply style sheet
	}

	// (these are the methods required by AppShell.Controller; most of them just switch screens)
	@Override public User currentUser() { return current; } // show login/logout
	@Override public String activeNavKey() { return activeNavKey; } // returns catalog, workspace, settings
	@Override public void goCatalog() { showDefault(); } // reset the login details
	@Override public void goSettings() { showSettings(); } // open appearance settings
	
	@Override public void goWorkspace() { if (current instanceof Manager) showManager(); else showClerk(); } // the workspace button opens the manager or the clerk view, depending on who is logged in
	@Override public void logOut() { showDefault(); } // logs out 
	@Override public void contactClerk() { contactDialogs.showContact(directory.getAllClerkUsernames()); } // pulls the list of hired clerk usernames
	@Override public void rateClerk() { contactDialogs.showRate(); } // opens contact dialog (allows the user to enter their BK-XXXXXX

	// asks where to save, then exports the product catalog as a PDF (with a message for success or failure).
	
	@Override
	public void downloadCatalog() {
		if (productInventory.getProducts().isEmpty()) { // empty inventory check
			new Alert(Alert.AlertType.WARNING, Settings.tr("There are no products to export.")).showAndWait();
			return;
		}
		
		FileChooser fc = new FileChooser();
		fc.setTitle(Settings.tr("Save catalog as PDF"));
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
		fc.setInitialFileName("Bukonek-Catalog.pdf");
		File f = fc.showSaveDialog(window);
		if (f == null) return; // the user cancelled
		try {
			CatalogPdf.export(productInventory.getProducts(), f.toPath());
			new Alert(Alert.AlertType.INFORMATION, Settings.tr("Catalog saved to:") + "\n" + f.getAbsolutePath()).showAndWait();
		} catch (Exception ex) {
			new Alert(Alert.AlertType.ERROR, Settings.tr("Could not create the PDF.") + "\n" + ex.getMessage()).showAndWait();
		}
	}

	@Override
	public void logIn() { // login prompts and checking of the credentials
		User user = new AccountDialogs(directory).logIn();
		if (user == null) return; // cancelled or wrong login
		current = user;
		goWorkspace(); // navigates to the correct screen manager/clerk
	}

	@Override
	public void save() { // manual save button
		if (!DataStore.save(productInventory, equipmentStore, transactionLog, noticeBoard, directory, contactBoard, wallet)) {
			new Alert(Alert.AlertType.ERROR, "Could not save! Check the console for details.").showAndWait();
			return;
		}
		if (redraw != null) redraw.run(); // redraw whichever screen is open so every panel shows the current data
		new Alert(Alert.AlertType.INFORMATION, "Saved to:\n" + DataStore.getFilePath()).showAndWait();
	}
}