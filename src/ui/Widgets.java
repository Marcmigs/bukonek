package ui;

import app.Theme;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import model.Equipment;
import service.EquipmentStore;

public final class Widgets {
	public static final int LOW_STOCK_LIMIT = 5; // stock at or below this count displays as "Low Stock"

	private Widgets() {} // utility class: prevents creating Widgets instances since all methods are static

	public static void styleButton(Button b) { // button styling
		b.setFont(Font.font(Theme.BODY_FONT, FontWeight.BOLD, 12));
		b.setStyle(
			"-fx-background-color: " + Theme.green() + ";" +
			"-fx-text-fill: " + Theme.white() + ";" +
			"-fx-background-radius: 10;" +
			"-fx-padding: 7 14 7 14;" +
			"-fx-cursor: hand;"
		);
	}

	public static ImageView circularLogo(double diameter) { // circular logo styling
		ImageView view = new ImageView();
		var url = Widgets.class.getResource("/bukonek_logo.png"); // looks up logo on the classpath; null if missing
		if (url != null) {
			Image img = new Image(url.toExternalForm()); // load image at its natural resolution
			view.setImage(img);
			if (img.getWidth() > 0 && img.getHeight() > 0) {
				// crop the picture to the largest centered square so fitting it into diameter x diameter won't distort it
				double side = Math.min(img.getWidth(), img.getHeight());
				view.setViewport(new Rectangle2D((img.getWidth() - side) / 2.0, (img.getHeight() - side) / 2.0, side, side));
			}
		}
		view.setFitWidth(diameter);
		view.setFitHeight(diameter);
		view.setPreserveRatio(true);
		view.setSmooth(true);
		view.setClip(new Circle(diameter / 2.0, diameter / 2.0, diameter / 2.0)); // masks the square view into a circle
		return view;
	}

	public static VBox card(String heading, Node content, String accentColor) { // reusable table card for equipments, transactions, etc.
		Label cardTitle = new Label(heading);
		cardTitle.setFont(Font.font(Theme.HEADING_FONT, FontWeight.BOLD, 14));
		cardTitle.setStyle("-fx-text-fill: " + accentColor + ";");

		VBox card = new VBox(10, cardTitle, content);
		card.setPadding(new Insets(16));
		card.setStyle(
			"-fx-background-color: " + Theme.surface() + ";" +
			"-fx-background-radius: 14;" +
			"-fx-border-color: " + Theme.borderLight() + ";" +
			"-fx-border-radius: 14;" +
			"-fx-effect: dropshadow(gaussian, rgba(35, 31, 32, 0.05), 10, 0, 0, 2);"
		);
		VBox.setVgrow(content, Priority.ALWAYS); // lets content fill remaining vertical height when cards sit side-by-side
		return card;
	}

	public static void stockPill(Label pill, int qty) { // checks quantity and updates the status (out of stock, low stock, in stock
		if (qty <= 0) {
			pill.setText("Out of Stock");
			pill.setStyle("-fx-background-color: " + Theme.blush() + ";-fx-text-fill: " + Theme.brown() + ";-fx-background-radius: 999;");
		} else if (qty <= LOW_STOCK_LIMIT) {
			pill.setText("Low Stock (" + qty + ")");
			pill.setStyle("-fx-background-color: " + Theme.blush2() + ";-fx-text-fill: " + Theme.brown()
				+ ";-fx-border-color: " + Theme.tan() + ";-fx-border-radius: 999;-fx-background-radius: 999;");
		} else {
			availablePill(pill, "In Stock (" + qty + ")");
		}
	}

	public static void availablePill(Label pill, String... text) { // sets the pill as available
		pill.setText(text.length > 0 ? text[0] : "Available");
		pill.setStyle("-fx-background-color: " + Theme.greenSoft() + ";-fx-text-fill: " + Theme.green() + ";-fx-background-radius: 999;");
	}

	// creates a TableView configured so columns fill the full width proportionally and show a placeholder when empty
	public static <T> TableView<T> table(double height, String emptyText) {
		TableView<T> t = new TableView<>();
		t.setPrefHeight(height);
		t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS); // stretches columns to eliminate blank trailing space
		t.setPlaceholder(new Label(emptyText)); // message shown when the table has 0 rows
		return t;
	}

	// creates a single-line text column; width is a proportional weight and sorting is disabled to keep newest-first order
	public static <T> TableColumn<T, String> col(String title, double width, Function<T, String> text) {
		TableColumn<T, String> c = new TableColumn<>(title);
		c.setCellValueFactory(cd -> new SimpleStringProperty(text.apply(cd.getValue()))); // extracts cell text from row object T
		c.setPrefWidth(width);
		c.setSortable(false); // preserves insertion / newest-first ordering
		return c;
	}

	// creates a multi-line wrapping column using a Text node so long messages wrap onto new lines instead of clipping
	public static <T> TableColumn<T, String> wrapCol(String title, double width, Function<T, String> text) {
		TableColumn<T, String> c = col(title, width, text);
		c.setCellFactory(column -> new TableCell<T, String>() {
			private final Text label = new Text(); // Text supports line wrapping, unlike standard clipped table labels
			{
				label.setStyle("-fx-fill: -fx-text-background-color;"); // matches the table's theme text color
				label.wrappingWidthProperty().bind(column.widthProperty().subtract(14)); // wraps text dynamically as column resizes
				setPrefHeight(USE_COMPUTED_SIZE); // allows the table row height to grow with wrapped lines
			}

			@Override
			protected void updateItem(String s, boolean empty) {
				super.updateItem(s, empty);
				if (empty || s == null) {
					setGraphic(null); // clear cell graphic on empty rows
				} else {
					label.setText(s);
					setGraphic(label); // render wrapped Text node inside the cell
				}
			}
		});
		return c;
	}

	// builds a read-only StringConverter for ChoiceBox / ComboBox so any model object T displays using the given formatter
	public static <T> StringConverter<T> converter(Function<T, String> text) {
		return new StringConverter<T>() {
			@Override public String toString(T item) { return item == null ? "" : text.apply(item); } // formats item for display
			@Override public T fromString(String s) { return null; } // unused because dropdowns are selection-only (not editable)
		};
	}

	// formats owned Equipment items in dropdowns (e.g. "Grater (uses left: 18/20)")
	public static StringConverter<Equipment> equipmentConverter() {
		return converter(e -> e.getDisplayName() + " (uses left: " + e.getUsesLeft() + "/" + e.getMaxUses() + ")");
	}

	// formats shop EquipmentStore.Model items in the "Buy Equipment" dropdown (e.g. "Presser Lv.2 - ₱1,500.00 (40 uses)")
	public static StringConverter<EquipmentStore.Model> shopConverter() {
		return converter(m -> m.type() + " Lv." + m.level() + " - " + Formats.money(m.price()) + " (" + m.uses() + " uses)");
	}
}