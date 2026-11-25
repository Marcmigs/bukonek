package ui;

import static app.Settings.tr;
import app.Theme;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import model.ContactRequest;
import service.ContactBoard;

// builds the two customer pop-up dialogs: "Contact a Clerk" and "Rate a Clerk" (validation and storage live in ContactBoard)
public class ContactDialogs {
	private final ContactBoard board; // backend service that validates submissions, generates reference codes, and stores ratings
	private final Random random = new Random(); // used to shuffle the clerk list so different clerks rotate into the top 3

	// connects the dialog builder to the shared ContactBoard service
	public ContactDialogs(ContactBoard board) {
		this.board = board;
	}

	// opens the "Contact a Clerk" pop-up: displays up to 3 randomly chosen clerks and submits a customer request
	public void showContact(List<String> allClerks) {
		if (allClerks.isEmpty()) { // stop early with a warning if no clerks exist in UserDirectory
			new Alert(Alert.AlertType.WARNING, tr("No clerks are available right now.")).showAndWait();
			return;
		}
		List<String> pool = new ArrayList<>(allClerks); // copy list before shuffling so the caller's list stays untouched
		Collections.shuffle(pool, random); // randomize order so the same 3 clerks aren't always shown first

		// build up to 3 side-by-side selectable clerk cards inside a single-selection ToggleGroup
		ToggleGroup clerkGroup = new ToggleGroup(); // ensures only one clerk card can be selected at a time
		HBox clerkBox = new HBox(10);
		for (String c : pool.subList(0, Math.min(3, pool.size()))) {
			ToggleButton card = new ToggleButton(c + "\n" + Formats.clerkRating(board, c)); // shows username + average rating
			card.setToggleGroup(clerkGroup);
			card.setUserData(c); // stores the raw clerk username on the button for retrieval on submit
			card.setMinWidth(110);
			card.setPadding(new Insets(10, 14, 10, 14));
			card.setTextAlignment(TextAlignment.CENTER);
			// updates card border and tint whenever selected state changes
			// (appending "33" to a 6-digit hex color adds ~20% opacity for a soft accent fill)
			Runnable look = () -> card.setStyle(
				(card.isSelected()
					? "-fx-background-color: " + Theme.green() + "33;-fx-border-color: " + Theme.green() + ";-fx-border-width: 2;-fx-font-weight: bold;"
					: "-fx-background-color: transparent;-fx-border-color: #cfcfcf;-fx-border-width: 1;")
				+ "-fx-background-radius: 10;-fx-border-radius: 10;-fx-cursor: hand;");
			card.selectedProperty().addListener((o, was, now) -> look.run()); // repaint when toggled on or off
			look.run(); // apply initial unselected styling
			clerkBox.getChildren().add(card);
		}
		// prevent unselecting the active card when clicked a second time so one clerk is always selected
		clerkGroup.selectedToggleProperty().addListener((o, was, now) -> {
			if (now == null && was != null) clerkGroup.selectToggle(was);
		});
		clerkGroup.selectToggle(clerkGroup.getToggles().get(0)); // pre-select the first clerk card by default

		// customer contact inputs
		TextField nameField = new TextField();
		TextField emailField = new TextField();
		emailField.setPromptText("name@example.com");
		TextField phoneField = new TextField();
		phoneField.setPromptText("09xx xxx xxxx");

		// reason dropdown: stores canonical English values for ContactBoard while displaying translated labels in the UI
		ChoiceBox<String> reasonBox = new ChoiceBox<>();
		reasonBox.getItems().addAll(ContactBoard.REASONS);
		reasonBox.setValue(ContactBoard.REASONS[0]);
		reasonBox.setConverter(Widgets.converter(app.Settings::tr));

		// optional message box with a hard character limit and live character counter
		int max = ContactBoard.MAX_MESSAGE_LENGTH;
		TextArea messageArea = new TextArea();
		messageArea.setWrapText(true);
		messageArea.setPrefRowCount(4);
		messageArea.setPrefColumnCount(24);
		// rejects any keystroke or paste (by returning null) that would push total length past max
		messageArea.setTextFormatter(new TextFormatter<String>(change ->
			change.getControlNewText().length() <= max ? change : null));
		Label counter = new Label("0 / " + max);
		counter.setStyle("-fx-text-fill: " + Theme.textMuted() + ";");
		messageArea.textProperty().addListener((o, was, now) -> counter.setText(now.length() + " / " + max)); // live count update

		// 1-element array workaround: lets the onConfirm lambda save the returned ContactRequest outside the lambda
		ContactRequest[] sent = {null};
		FormDialog dialog = new FormDialog(tr("Contact a Clerk"),
				tr("Pick one of the available clerks and tell us how we can help."), tr("Send"))
			.content(FormDialog.form(
				new String[]{tr("Clerk:"), tr("Your name:"), tr("Email:"), tr("Phone number:"), tr("Reason:"), tr("Message (optional):")},
				clerkBox, nameField, emailField, phoneField, reasonBox, new VBox(4, messageArea, counter)))
			.onConfirm(tr("Failed to send! "), () -> sent[0] = board.submit(
				(String) clerkGroup.getSelectedToggle().getUserData(), nameField.getText(),
				emailField.getText(), phoneField.getText(), reasonBox.getValue(), messageArea.getText()));

		// once submitted successfully, show the generated BK-XXXXXX reference number in a read-only copyable TextField
		if (dialog.showAndConfirm() && sent[0] != null) {
			TextField refField = new TextField(sent[0].getReference());
			refField.setEditable(false); // prevents editing while still allowing the customer to highlight and copy the code
			refField.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
			Alert done = new Alert(Alert.AlertType.INFORMATION);
			done.setTitle(tr("Contact a Clerk"));
			done.setHeaderText(tr("Request sent!"));
			done.getDialogPane().setContent(new VBox(10,
				new Label(tr("Your request was sent to") + " " + sent[0].getClerkUsername() + "."),
				refField,
				new Label(tr("Keep this reference number to rate your clerk later."))));
			done.showAndWait();
		}
	}

	// opens the "Rate a Clerk" pop-up: validates a single-use BK-XXXXXX reference code and submits a 1-to-5 star rating
	public void showRate() {
		TextField refField = new TextField();
		refField.setPromptText("BK-XXXXXX");

		// interactive 5-star picker built from clickable Labels; chosen[0] stores the selected rating (0 = none selected yet)
		int[] chosen = {0};
		Label[] star = new Label[5];
		HBox stars = new HBox(4);
		// fills stars 0..chosen[0]-1 with solid stars and leaves the remaining ones as outlines
		Runnable paint = () -> {
			for (int i = 0; i < 5; i++) {
				star[i].setText(i < chosen[0] ? "★" : "☆");
				star[i].setStyle("-fx-font-size: 26px; -fx-text-fill: #e0a100; -fx-cursor: hand;");
			}
		};
		for (int i = 0; i < 5; i++) {
			final int value = i + 1; // 1 to 5; captured as final so each star's click handler sets its own score
			star[i] = new Label("☆");
			star[i].setOnMouseClicked(e -> { chosen[0] = value; paint.run(); }); // record rating and repaint stars
			stars.getChildren().add(star[i]);
		}
		paint.run(); // render all 5 stars as empty outlines initially

		// validate reference code + star rating via ContactBoard before closing the pop-up
		FormDialog dialog = new FormDialog(tr("Rate a Clerk"),
				tr("Enter the reference number from your contact request."), tr("Submit"))
			.content(FormDialog.form(new String[]{tr("Reference number:"), tr("Rating:")}, refField, stars))
			.onConfirm(tr("Failed to rate! "), () -> board.rate(refField.getText(), chosen[0]));

		if (dialog.showAndConfirm()) {
			new Alert(Alert.AlertType.INFORMATION, tr("Thank you! Your rating was submitted.")).showAndWait();
		}
	}
}