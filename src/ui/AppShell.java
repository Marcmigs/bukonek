package ui;

import static app.Settings.tr;
import app.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import users.User;

// builds the persistent outer frame around every center screen: top header, left sidebar, and bottom footer
public class AppShell {

	// contract implemented by Main so the header and sidebar can query state and trigger actions without depending on Main directly
	public interface Controller {
		User currentUser();          // returns the logged-in Manager/Clerk, or null when in public Customer View
		String activeNavKey();       // returns "CATALOG", "WORKSPACE", or "SETTINGS" to highlight the active sidebar tab
		void goCatalog();            // switches to the public product catalog
		void goWorkspace();          // opens ManagerScreen or ClerkScreen based on the logged-in user's role
		void goSettings();           // opens the appearance, language, and text-size screen
		void logIn();                // opens the login dialog and routes to the user's workspace on success
		void logOut();               // clears the active user session and returns to Customer View
		void save();                 // manually saves all store data to disk and refreshes the open screen
		void downloadCatalog();      // prompts for a file location and exports the product catalog as a PDF
		void contactClerk();         // opens the customer inquiry/order request dialog
		void rateClerk();            // opens the customer clerk-rating dialog
	}

	private final Controller ctl; // reference to Main (via the Controller interface) for routing all button actions

	// stores the controller callback bridge when Main creates the shell
	public AppShell(Controller ctl) {
		this.ctl = ctl;
	}

	// builds the top navigation bar with the circular logo, brand title, active role badge, Save button, and Log In/Out button
	public HBox header() {
		User current = ctl.currentUser(); // null if browsing as a customer, otherwise the logged-in staff user
		ImageView miniLogo = Widgets.circularLogo(36);

		Label logo = new Label("BUKONEK");
		logo.setFont(Font.font(Theme.HEADING_FONT, FontWeight.BOLD, 21));
		logo.setStyle("-fx-text-fill: " + Theme.brown() + ";");

		// rounded status pill next to the logo: displays "CUSTOMER VIEW" or the logged-in username
		Label rolePill = new Label(current == null ? tr("CUSTOMER VIEW") : current.getUsername().toUpperCase());
		rolePill.setFont(Font.font(Theme.BODY_FONT, FontWeight.BOLD, 11));
		rolePill.setPadding(new Insets(4, 10, 4, 10));
		rolePill.setStyle(
			"-fx-background-color: " + Theme.greenSoft() + ";" +
			"-fx-text-fill: " + Theme.green() + ";" +
			"-fx-background-radius: 999;"
		);

		HBox brandGroup = new HBox(10, miniLogo, logo, rolePill);
		brandGroup.setAlignment(Pos.CENTER_LEFT);

		// stretchy empty spacer that pushes the Save and Log In/Out buttons to the far right edge
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		// single toggle button that switches label and behavior between "LOG IN" and "LOG OUT"
		Button loginBtn = new Button(tr(current == null ? "LOG IN" : "LOG OUT"));
		Widgets.styleButton(loginBtn);
		loginBtn.setOnAction(e -> { if (ctl.currentUser() == null) ctl.logIn(); else ctl.logOut(); });

		// Save button: forces a save to disk and redraws the open screen so all panels reflect latest data
		Button saveBtn = new Button(tr("SAVE"));
		Widgets.styleButton(saveBtn);
		saveBtn.setOnAction(e -> ctl.save());

		HBox h = new HBox(12, brandGroup, spacer, saveBtn, loginBtn);
		h.setAlignment(Pos.CENTER_LEFT);
		h.setPadding(new Insets(10, 18, 10, 18));
		h.setStyle(
			"-fx-background-color: " + Theme.surface() + ";" +
			"-fx-border-color: " + Theme.borderLight() + ";" +
			"-fx-border-width: 0 0 1 0;" // draws a 1px divider line on the bottom edge only
		);
		return h;
	}

	// builds the left navigation rail; displays customer actions when logged out, or "My Workspace" when staff is logged in
	public VBox sidebar() {
		String active = ctl.activeNavKey();

		Label menuSection = new Label("MENU");
		menuSection.setFont(Font.font(Theme.BODY_FONT, FontWeight.BOLD, 10));
		menuSection.setStyle("-fx-text-fill: " + Theme.textMuted() + ";");

		Label badgeTitle = new Label("Version Developers:");
		badgeTitle.setFont(Font.font(Theme.HEADING_FONT, FontWeight.BOLD, 12));
		badgeTitle.setStyle("-fx-text-fill: " + Theme.brown() + ";");

		Label badgeSub = new Label(tr("Carmie Shaine Decena\nMishael De Guzman\nMarco Miguel Espiritu"));
		badgeSub.setWrapText(true);
		badgeSub.setFont(Font.font(Theme.BODY_FONT, 11));
		badgeSub.setStyle("-fx-text-fill: " + Theme.textMuted() + ";");

		VBox infoCard = new VBox(4, badgeTitle, badgeSub);
		infoCard.setPadding(new Insets(12));
		infoCard.setStyle(
			"-fx-background-color: " + Theme.creamBg() + ";" +
			"-fx-background-radius: 10;" +
			"-fx-border-color: " + Theme.tan() + ";" +
			"-fx-border-radius: 10;"
		);

		Region grow = new Region();
		VBox.setVgrow(grow, Priority.ALWAYS);

		VBox rail = new VBox(8, menuSection);
		if (ctl.currentUser() != null) {
			// staff view: only My Workspace (no Product Catalog)
			rail.getChildren().add(navButton(tr("My Workspace"), "WORKSPACE".equals(active), ctl::goWorkspace));
		} else {
			// customer view: Product Catalog, Contact a Clerk, Rate a Clerk
			rail.getChildren().addAll(
				navButton(tr("Product Catalog"), "CATALOG".equals(active), ctl::goCatalog),
				navButton(tr("Contact a Clerk"), false, ctl::contactClerk),
				navButton(tr("Rate a Clerk"), false, ctl::rateClerk));
		}
		rail.getChildren().addAll(
			navButton(tr("Download Catalog"), false, ctl::downloadCatalog),
			navButton(tr("Settings"), "SETTINGS".equals(active), ctl::goSettings),
			grow, infoCard);
		rail.setPrefWidth(200);
		rail.setPadding(new Insets(16, 12, 16, 12));
		rail.setStyle(
			"-fx-background-color: " + Theme.surface() + ";" +
			"-fx-border-color: " + Theme.borderLight() + ";" +
			"-fx-border-width: 0 1 0 0;"
		);
		return rail;
	}

	// builds the bottom status bar with fixed brand colors that stay identical in both Light and Dark mode
	public HBox footer() {
		Label l = new Label("Copyright @ Bukonek 2026");
		l.setFont(Font.font(Theme.BODY_FONT, FontWeight.BOLD, 10));
		l.setStyle("-fx-text-fill: #d9b38c;"); // warm tan text on dark coconut brown background
		HBox f = new HBox(l);
		f.setPadding(new Insets(10, 18, 10, 18));
		f.setStyle("-fx-background-color: #582a14;");
		return f;
	}

	// builds a full-width sidebar navigation button; highlights with the accent fill and bold text when active is true
	private Button navButton(String text, boolean active, Runnable action) {
		Button btn = new Button(text);
		btn.setMaxWidth(Double.MAX_VALUE); // stretches button across the full width of the sidebar
		btn.setAlignment(Pos.CENTER_LEFT);
		btn.setFont(Font.font(Theme.BODY_FONT, active ? FontWeight.BOLD : FontWeight.MEDIUM, 13));
		btn.setPadding(new Insets(9, 12, 9, 12));
		btn.setStyle(
			"-fx-background-color: " + (active ? Theme.green() : "transparent") + ";" +
			"-fx-text-fill: " + (active ? Theme.white() : Theme.textDark()) + ";" +
			"-fx-background-radius: 10;" +
			"-fx-cursor: hand;"
		);
		btn.setOnAction(e -> action.run()); // triggers the navigation or dialog callback when clicked
		return btn;
	}
}