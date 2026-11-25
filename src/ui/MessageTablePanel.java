package ui;

import java.util.Collections;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.ContactRequest;
import model.Message;
import model.StockNotice;
import service.ContactBoard;
import service.NoticeBoard;

// reusable table panel with "Mark All as Read" and "Clear Read ..." buttons (Template Method pattern)
// shared by the manager's NoticeBoard and the clerk's ContactBoard; subclasses only define extra columns and data calls
public abstract class MessageTablePanel<T extends Message> {
	private final double height;      // preferred table height in pixels
	private final String emptyText;   // placeholder message shown when there are no messages
	private final String clearLabel;  // text on the clear button (e.g. "Clear Read Notices")

	// stores layout settings passed by each subclass
	protected MessageTablePanel(double height, String emptyText, String clearLabel) {
		this.height = height;
		this.emptyText = emptyText;
		this.clearLabel = clearLabel;
	}

	// abstract hooks implemented by each subclass to customize columns and connect to its backend board
	protected abstract void addColumns(TableView<T> table); // adds domain-specific columns after Status, #, and Date
	protected abstract List<T> items();                      // fetches messages to display, ordered newest-first
	protected abstract void markAllRead();                   // marks all visible messages as read in the backend
	protected abstract void clearRead();                     // removes or hides read messages in the backend

	// template method: builds the shared button bar, table columns, bold unread styling, and live refresh logic
	public Node build() {
		VBox layout = new VBox(10);
		layout.setPadding(new Insets(10));

		// build the table with the 3 standard columns shared by every Message type
		TableView<T> table = Widgets.table(height, emptyText);
		table.getColumns().add(Widgets.<T>col("Status", 55, m -> m.isRead() ? "Read" : "NEW"));
		table.getColumns().add(Widgets.<T>col("#", 45, m -> Formats.id(m.getId())));
		table.getColumns().add(Widgets.<T>col("Date", 110, m -> Formats.time(m.getTimestamp())));
		addColumns(table); // let the subclass append its own specific columns

		// highlight unread rows in bold text
		table.setRowFactory(tv -> new TableRow<T>() {
			@Override
			protected void updateItem(T m, boolean empty) {
				super.updateItem(m, empty);
				setStyle((empty || m == null || m.isRead()) ? "" : "-fx-font-weight: bold;");
			}
		});

		// reloads rows from items() and calls table.refresh() so rows re-evaluate their bold unread style immediately
		Runnable refresh = () -> {
			table.getItems().setAll(items());
			table.refresh();
		};
		refresh.run(); // load initial rows

		// top action buttons: mark everything read or clear already-read items, then repaint the table
		Button readBtn = new Button("Mark All as Read");
		Widgets.styleButton(readBtn);
		readBtn.setOnAction(e -> { markAllRead(); refresh.run(); });

		Button clearBtn = new Button(clearLabel);
		Widgets.styleButton(clearBtn);
		clearBtn.setOnAction(e -> { clearRead(); refresh.run(); });

		layout.getChildren().addAll(new HBox(10, readBtn, clearBtn), table);
		return layout;
	}

	// low-stock notice feature: provides the manager's notice table (build()) and the clerk's "Notify Manager" button
	public static class NoticePanel extends MessageTablePanel<StockNotice> {
		private final NoticeBoard board; // shared notice board service

		public NoticePanel(NoticeBoard board) {
			super(200, "No stock notices.", "Clear Read Notices");
			this.board = board;
		}

		// adds Clerk and multi-line wrapping Message columns for the manager's table
		@Override
		protected void addColumns(TableView<StockNotice> table) {
			table.getColumns().add(Widgets.<StockNotice>col("Clerk", 80, n -> n.getClerkUsername()));
			table.getColumns().add(Widgets.<StockNotice>wrapCol("Message", 320, n -> n.getMessage()));
		}

		// fetches all notices and reverses the copy so the newest notice appears at the top
		@Override
		protected List<StockNotice> items() {
			List<StockNotice> newestFirst = board.getAll(); // getAll() returns a copy, so reversing is safe
			Collections.reverse(newestFirst);
			return newestFirst;
		}

		@Override protected void markAllRead() { board.markAllRead(); }
		@Override protected void clearRead() { board.clearRead(); }

		// builds the clerk-side panel containing the button that opens the low-stock alert pop-up
		public Node buildNotifyPanel(String clerkUsername) {
			Button notifyBtn = new Button("Notify Manager: Low Stock");
			Widgets.styleButton(notifyBtn);
			notifyBtn.setOnAction(e -> showNotifyDialog(clerkUsername));
			HBox box = new HBox(10, notifyBtn);
			box.setPadding(new Insets(10));
			return box;
		}

		// opens the pop-up where a clerk writes a low-stock message; NoticeBoard.send() validates length and blank text
		private void showNotifyDialog(String clerkUsername) {
			TextArea messageArea = new TextArea();
			messageArea.setPromptText("e.g. Only 2 Coconut Juice (500g) left, please restock.");
			messageArea.setWrapText(true);
			messageArea.setPrefRowCount(4);
			messageArea.setPrefColumnCount(28);

			FormDialog dialog = new FormDialog("Notify Manager", "Tell the manager what is running low", "Send")
				.content(FormDialog.form(new String[]{"Message:"}, messageArea))
				.onConfirm("Failed to send! ", () -> board.send(clerkUsername, messageArea.getText()));

			if (dialog.showAndConfirm()) {
				new Alert(Alert.AlertType.INFORMATION,
					"Your manager has been notified. They will see it the next time they log in.").showAndWait();
			}
		}
	}

	// clerk-side table of customer inquiries and orders; shows only requests assigned to this clerk
	public static class ContactPanel extends MessageTablePanel<ContactRequest> {
		private final ContactBoard board;         // shared customer contact and rating service
		private final String clerkUsername;       // filters requests to only those sent to this clerk

		public ContactPanel(ContactBoard board, String clerkUsername) {
			super(220, "No customer requests.", "Clear Read Requests");
			this.board = board;
			this.clerkUsername = clerkUsername;
		}

		// adds reference code, customer details, reason, wrapping message, and star rating columns
		@Override
		protected void addColumns(TableView<ContactRequest> table) {
			table.getColumns().add(Widgets.<ContactRequest>col("Ref", 90, r -> r.getReference()));
			table.getColumns().add(Widgets.<ContactRequest>col("Customer", 100, r -> r.getCustomerName()));
			table.getColumns().add(Widgets.<ContactRequest>wrapCol("Contact", 150, r -> Formats.contactText(r)));
			table.getColumns().add(Widgets.<ContactRequest>col("Reason", 110, r -> r.getReason()));
			table.getColumns().add(Widgets.<ContactRequest>wrapCol("Message", 220, r -> r.getMessage()));
			table.getColumns().add(Widgets.<ContactRequest>col("Rating", 80, r -> Formats.stars(r.getStars())));
		}

		// returns active (non-cleared) requests for this clerk; ContactBoard already orders them newest-first
		@Override protected List<ContactRequest> items() { return board.visibleFor(clerkUsername); }
		@Override protected void markAllRead() { board.markAllRead(clerkUsername); }
		@Override protected void clearRead() { board.clearRead(clerkUsername); }
	}
}