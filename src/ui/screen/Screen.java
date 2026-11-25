package ui.screen;

import app.Theme;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import ui.Widgets;

// Base of every page (DefaultScreen, ClerkScreen, ManagerScreen, SettingsScreen)
// Subclasses only say what the page contains (build)
// The shared design (page titles and cards)
public abstract class Screen {

	// Builds the whole page and returns it
	// Must be available for every subclass
	public abstract Node build();

	// Page Title
	// Bold text with underline
	protected Label title(String text) {
		Label label = new Label(text);
		label.setFont(Font.font(Theme.HEADING_FONT, FontWeight.BOLD, 19));
		label.setStyle(
			"-fx-text-fill: " + Theme.brown() + ";" +
			"-fx-padding: 2 0 6 0;" +
			"-fx-border-color: " + Theme.green() + ";" +
			"-fx-border-width: 0 0 2 0;" // border on the bottom side only
		);
		return label;
	}

	// White rounded card with a heading (copied in ClerkScreen and ManagerScreen)
	protected VBox wrapCard(String heading, Node content, String accentColor) {
		return Widgets.card(heading, content, accentColor);
	}
}
