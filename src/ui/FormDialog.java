package ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class FormDialog {

	@FunctionalInterface
	public interface Action { void run() throws Exception; } // allows the form to have custom error messages

	public static class Abort extends RuntimeException { // stops the pop up from closing
		private static final long serialVersionUID = 1L;
	}

	public static void fail(String message) { // if running a code fails, show alert and throw abort
		new Alert(Alert.AlertType.ERROR, message).showAndWait();
		throw new Abort();
	}

	public static GridPane form(String[] labels, Node... fields) { // builds a 2-column form layout for pop-ups; pairs labels[i] on the left with fields[i] on the right
		GridPane grid = new GridPane();
		grid.setHgap(10); // horizontal spacing between the label column and the input column
		grid.setVgap(8);  // vertical spacing between each form row
		grid.setPadding(new Insets(10)); // outer padding around the form grid
		for (int i = 0; i < fields.length; i++) { // fills in each item in the grid
			grid.add(new Label(labels[i]), 0, i); // label in column 0, row i
			grid.add(fields[i], 1, i); // matching input field in column 1, row i
		}
		return grid;
	}

	private final Dialog<ButtonType> dialog = new Dialog<>(); // pop up window itself
	private final ButtonType confirmType; // custom confirm button whose label is chosen by the caller (e.g. "Send", "Log In")

	public FormDialog(String title, String header, String confirmText) { // creates the dialog with a window title, header message, custom confirm button, and standard cancel button
		dialog.setTitle(title);
		dialog.setHeaderText(header);
		confirmType = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE); // marks button as the primary confirm action
		dialog.getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL); // adds both buttons to the dialog footer
	}

	public FormDialog content(Node content) { // sets the form grid as the dialog's center content and returns this for method chaining
		dialog.getDialogPane().setContent(content);
		return this;
	}

	public FormDialog onConfirm(String errorPrefix, Action action) { // attaches the confirm button before pop up is allowed to close
		Button confirmBtn = (Button) dialog.getDialogPane().lookupButton(confirmType);
		confirmBtn.addEventFilter(ActionEvent.ACTION, event -> { // runs before JavaFX's built-in close handler
			try {
				action.run(); // run validation and service logic; if this finishes without throwing, the dialog closes
			} catch (Abort a) {
				event.consume(); // keep the pop-up open silently (error was already shown or user cancelled)
			} catch (Exception ex) {
				new Alert(Alert.AlertType.ERROR, errorPrefix + ex.getMessage()).showAndWait(); // show prefixed error alert
				event.consume(); // cancel the button click event so the pop-up stays open with user input intact
			}
		});
		return this;
	}

	public boolean showAndConfirm() { // opens the modal pop-up and waits for user input
		return dialog.showAndWait().filter(b -> b == confirmType).isPresent();
	}
}