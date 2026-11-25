package ui;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import service.UserDirectory;
import users.User;

public class AccountDialogs {
	private final UserDirectory directory; // all accounts (does the real checking)

	public AccountDialogs(UserDirectory directory) {
		this.directory = directory;
	}

	public User logIn() {
		TextField u = new TextField();
		PasswordField p = new PasswordField(); // hides what is typed
		// simple grid
		// label on the left, input on the right
		GridPane g = new GridPane();
		g.setHgap(8); g.setVgap(8); g.setPadding(new Insets(10));
		g.addRow(0, new Label("Username"), u);
		g.addRow(1, new Label("Password"), p);

		// a plain Dialog (not FormDialog) because the check happens after it closes, not before
		Dialog<ButtonType> d = new Dialog<>();
		d.setTitle("Log in");
		d.getDialogPane().setContent(g);
		d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		if (d.showAndWait().filter(b -> b == ButtonType.OK).isEmpty()) return null; // cancelled (anything but OK)

		User user = directory.authenticate(u.getText().trim(), p.getText()); // the password is not trimmed
		if (user == null) new Alert(Alert.AlertType.ERROR, "Invalid username or password.").showAndWait();
		return user; // null if the login was wrong
	}

	public void hireClerk(String managerUsername, Runnable onHired) {
		// check the limit first so the manager does not fill in a form that cannot be saved
		if (!directory.canHire(managerUsername)) {
			new Alert(Alert.AlertType.WARNING, "You already have the maximum of " + UserDirectory.MAX_CLERKS + " clerks.").showAndWait();
			return;
		}
		TextField userField = new TextField();
		userField.setPromptText("Username (e.g. clerk06)");
		PasswordField passField = new PasswordField();
		passField.setPromptText("At least " + UserDirectory.MIN_PASSWORD_LENGTH + " characters");
		PasswordField confirmField = new PasswordField();
		confirmField.setPromptText("Type the password again");

		// FormDialog shows any exception thrown inside onConfirm and keeps the pop-up open
		FormDialog dialog = new FormDialog("Hire Clerk", "Create a clerk profile assigned to " + managerUsername, "Hire")
			.content(FormDialog.form(new String[]{"Username:", "Password:", "Confirm password:"}, userField, passField, confirmField))
			.onConfirm("Failed to hire! ", () -> {
				// the "passwords match" check is the screen's job
				// every other rule is in UserDirectory.hireClerk
				if (!passField.getText().equals(confirmField.getText())) {
					throw new IllegalArgumentException("Passwords do not match.");
				}
				directory.hireClerk(managerUsername, userField.getText(), passField.getText());
			});

		// showAndConfirm() is true only when the clerk was really saved
		if (dialog.showAndConfirm()) {
			onHired.run();
		}
	}
}