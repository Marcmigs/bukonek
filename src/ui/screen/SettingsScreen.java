package ui.screen;

import app.Theme;
import ui.Widgets;

import app.Settings;
import static app.Settings.tr; // tr("text") = translates the text to the chosen language
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class SettingsScreen extends Screen {
	private final Runnable onApply; // set by Main, re-applies the theme and redraws the page

	public SettingsScreen(Runnable onApply) {
		this.onApply = onApply;
	}

	@Override
	public Node build() {
		// grid
		// column 0 = the label, column 1 = the dropdown
		GridPane g = new GridPane();
		g.setHgap(16); // space between columns
		g.setVgap(12); // space between rows
		int row = 0;   // current row ; row++ moves down after each add

		// --- Appearance ---
		g.add(section(tr("Appearance")), 0, row++, 2, 1); // heading spans both columns
		g.add(rowLabel(tr("Mode")), 0, row);
		g.add(box(new String[]{"Light", "Dark"}, Settings.dark ? "Dark" : "Light", v -> Settings.dark = v.equals("Dark")), 1, row++);
		g.add(rowLabel(tr("Accent color")), 0, row);
		g.add(box(new String[]{"Green", "Brown", "Blue"}, Settings.accent, v -> Settings.accent = v), 1, row++);

		// --- Language ---
		g.add(section(tr("Language")), 0, row++, 2, 1);
		g.add(rowLabel(tr("Display language")), 0, row);
		g.add(box(new String[]{"English", "Tagalog"}, Settings.language, v -> Settings.language = v), 1, row++);

		// --- More ---
		g.add(section(tr("More")), 0, row++, 2, 1);
		g.add(rowLabel(tr("Text size")), 0, row);
		g.add(box(new String[]{"Small", "Normal", "Large"}, Settings.textSize, v -> Settings.textSize = v), 1, row++);

		// puts every setting back to its default, saves, then redraws the page
		Button reset = new Button(tr("Reset to defaults"));
		Widgets.styleButton(reset);
		reset.setOnAction(e -> {
			Settings.reset();
			Settings.save();
			onApply.run();
		});
		g.add(reset, 0, row, 2, 1);

		Label note = new Label(tr("Settings are saved automatically."));
		note.setStyle("-fx-text-fill: " + Theme.textMuted() + ";");

		// white rounded card that holds the grid and the note
		VBox card = new VBox(14, g, note);
		card.setPadding(new Insets(20));
		card.setStyle(
			"-fx-background-color: " + Theme.surface() + ";" +
			"-fx-background-radius: 14;" +
			"-fx-border-color: " + Theme.borderLight() + ";" +
			"-fx-border-radius: 14;" +
			"-fx-effect: dropshadow(gaussian, rgba(35, 31, 32, 0.05), 10, 0, 0, 2);" // soft shadow
		);

		VBox layout = new VBox(14, title(tr("SETTINGS")), card);
		layout.setPadding(new Insets(10));
		layout.setStyle("-fx-background-color: " + Theme.creamBg() + ";");
		return layout;
	}

	// green heading that starts each group of settings
	private Label section(String text) {
		Label l = new Label(text);
		l.setFont(Font.font(Theme.HEADING_FONT, FontWeight.BOLD, 14));
		l.setStyle("-fx-text-fill: " + Theme.green() + ";");
		return l;
	}

	// label shown beside a dropdown (e.g. "Mode")
	private Label rowLabel(String text) {
		Label l = new Label(text);
		l.setFont(Font.font(Theme.BODY_FONT, 13));
		l.setStyle("-fx-text-fill: " + Theme.textDark() + ";");
		return l;
	}

	private ChoiceBox<String> box(String[] keys, String current, Consumer<String> setter) {
		ChoiceBox<String> b = new ChoiceBox<>();
		b.getItems().addAll(keys);
		b.setValue(current);
		b.setConverter(Widgets.converter(Settings::tr)); // the dropdown displays the translated text, but the value stays English
		// runs whenever the user picks a different choice
		b.valueProperty().addListener((obs, old, v) -> {
			if (v != null && !v.equals(old)) { // ignore null and "same choice picked again"
				setter.accept(v);
				Settings.save();
				onApply.run();
			}
		});
		return b;
	}
}