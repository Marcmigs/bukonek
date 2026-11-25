package ui.screen;

import ui.InventoryPanel;
import ui.InventoryPanel.ViewMode;

import app.Theme;
import ui.Widgets;

// This is the default screen that everyone will see once the window is opened 
import service.Inventory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultScreen extends Screen { // subclass of Screen
    private final Inventory inventory; // this screen only shows the inventory

    // finds "stock: 12", "Stock - 12" or "stock 12" in a product's text and captures the number
    // (?i) = ignore upper / lower case, \\s* = optional spaces, (\\d+) = the number
    private static final Pattern STOCK_PATTERN = Pattern.compile("(?i)stock\\s*[:\\-]?\\s*(\\d+)");

    public DefaultScreen(Inventory inventory) {
        this.inventory = inventory; // initializing the inventory
    }

    @Override
    public Node build() { // overriding the abstract method from parent class
    	// 1. Cover photo banner (/bukonek_cover.png) that fades as the user scrolls
        ImageView coverView = createCoverBanner();
        StackPane coverContainer = new StackPane(coverView);
        coverContainer.setMaxWidth(Double.MAX_VALUE);
        coverContainer.setPrefHeight(180);
        coverContainer.setMinHeight(120);
        coverContainer.setStyle(
            "-fx-background-color: " + Theme.green() + ";" + // green shows if the picture is missing
            "-fx-background-radius: 14;"
        );

        coverView.setPreserveRatio(true); // keep the picture's real proportions
        coverView.setSmooth(true);
        coverView.fitWidthProperty().bind(coverContainer.widthProperty()); // picture always as wide as the banner
        // the banner is a fixed-height strip : crop what does not fit instead of squashing the picture
        Rectangle cropArea = new Rectangle();
        cropArea.widthProperty().bind(coverContainer.widthProperty());
        cropArea.heightProperty().bind(coverContainer.heightProperty());
        coverContainer.setClip(cropArea);

        // gradient at the bottom of the banner that blends it into the page background
        Region bottomFadeOverlay = new Region();
        bottomFadeOverlay.setMaxHeight(55);
        bottomFadeOverlay.setStyle(
            "-fx-background-color: linear-gradient(to bottom, transparent, " + Theme.creamBg() + ");"
        );
        StackPane.setAlignment(bottomFadeOverlay, Pos.BOTTOM_CENTER);
        coverContainer.getChildren().add(bottomFadeOverlay);

        // 2. Catalog header card with circular logo (brand beside stock status removed)
        ImageView circleLogoView = createCircularLogo(56);

        Label brandTitle = new Label("CUSTOMER - PRODUCT CATALOG");
        brandTitle.setFont(Font.font(Theme.HEADING_FONT, FontWeight.BOLD, 21));
        brandTitle.setStyle("-fx-text-fill: " + Theme.brown() + ";");

        Label brandSubtitle = new Label("Browse available products, variants, and stock status");
        brandSubtitle.setFont(Font.font(Theme.BODY_FONT, 12));
        brandSubtitle.setStyle("-fx-text-fill: " + Theme.textMuted() + ";");

        VBox titleStack = new VBox(2, brandTitle, brandSubtitle); // title above subtitle
        titleStack.setAlignment(Pos.CENTER_LEFT);

        HBox headerCard = new HBox(14, circleLogoView, titleStack); // logo on the left, text on the right
        headerCard.setAlignment(Pos.CENTER_LEFT);
        headerCard.setPadding(new Insets(14, 18, 14, 18));
        headerCard.setStyle(
            "-fx-background-color: " + Theme.surface() + ";" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: " + Theme.borderLight() + ";" +
            "-fx-border-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(35, 31, 32, 0.05), 12, 0, 0, 3);" // soft shadow
        );

        // 3. Product catalog panel with styled cards & per-product stock status pills
        Label activeFilterChip = new Label("All Products"); // display only, it does not filter anything
        activeFilterChip.setFont(Font.font(Theme.BODY_FONT, FontWeight.BOLD, 12));
        activeFilterChip.setPadding(new Insets(5, 14, 5, 14));
        activeFilterChip.setStyle(
            "-fx-background-color: " + Theme.green() + ";" +
            "-fx-text-fill: " + Theme.white() + ";" +
            "-fx-background-radius: 999;"
        );

        HBox toolbarRow = new HBox(10, activeFilterChip);
        toolbarRow.setAlignment(Pos.CENTER_LEFT);

        // builds the JavaFX interface for the customer (DEFAULT view), then restyles it
        Node inventoryControlPanel = new InventoryPanel(inventory).build(ViewMode.DEFAULT);
        enhanceProductPresentation(inventoryControlPanel);

        VBox catalogCard = new VBox(12, toolbarRow, inventoryControlPanel);
        catalogCard.setPadding(new Insets(18));
        catalogCard.setStyle(
            "-fx-background-color: " + Theme.surface() + ";" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: " + Theme.borderLight() + ";" +
            "-fx-border-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(35, 31, 32, 0.05), 12, 0, 0, 3);"
        );
        VBox.setVgrow(inventoryControlPanel, Priority.ALWAYS); // the product list takes the leftover height

        VBox rootLayout = new VBox(14, coverContainer, headerCard, catalogCard); // compiles everything into the layout
        rootLayout.setStyle("-fx-background-color: " + Theme.creamBg() + ";");

        // 4. Fade cover banner as enclosing ScrollPane scrolls
        // the page is not inside a ScrollPane yet when it is built, so wait until it gets a parent
        rootLayout.parentProperty().addListener((obs, oldParent, newParent) -> {
            Parent p = newParent;
            while (p != null) { // walk upward until a ScrollPane is found
                if (p instanceof ScrollPane sp) {
                    sp.vvalueProperty().addListener((vObs, oldV, newV) -> {
                        double v = newV.doubleValue(); // scroll position : 0 = top, 1 = bottom
                        double progress = Math.min(Math.max(v / 0.35, 0.0), 1.0); // 0 to 1 over the first 35% of the scroll
                        coverContainer.setOpacity(1.0 - (progress * 0.85)); // fades to 15% visible
                        coverContainer.setTranslateY(progress * 18); // moves down slightly while fading
                    });
                    break;
                }
                p = p.getParent();
            }
        });

        return rootLayout;
    }

    private void enhanceProductPresentation(Node node) {
        if (node == null) return;

        if (node instanceof ListView<?> rawList) {
            // the product list
        	// cast so we can set a custom cell design on it
            @SuppressWarnings("unchecked")
            ListView<Object> listView = (ListView<Object>) rawList;
            listView.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-control-inner-background: transparent;" +
                "-fx-background-insets: 0;" +
                "-fx-padding: 4 0 4 0;"
            );
            
            // each row of the list is drawn as a card
            // title + details on the left, stock pill on the right
            listView.setCellFactory(lv -> new ListCell<>() {
                private final Label titleLabel = new Label();   // first line of the product's text
                private final Label detailLabel = new Label();  // the rest of the text
                private final Label statusPill = new Label();   // stock status (e.g. in stock / low)
                private final VBox textCol = new VBox(3, titleLabel, detailLabel);
                private final Region rowSpacer = new Region();  // pushes the pill to the far right
                private final HBox cardRow = new HBox(12, textCol, rowSpacer, statusPill);

                { 
                // initializer block 
                // runs once per cell, sets the fixed styling
                    titleLabel.setFont(Font.font(Theme.HEADING_FONT, FontWeight.BOLD, 14));
                    titleLabel.setStyle("-fx-text-fill: " + Theme.brown() + ";");

                    detailLabel.setFont(Font.font(Theme.BODY_FONT, 12));
                    detailLabel.setStyle("-fx-text-fill: " + Theme.textMuted() + ";");
                    detailLabel.setWrapText(true);

                    statusPill.setFont(Font.font(Theme.BODY_FONT, FontWeight.BOLD, 11));
                    statusPill.setPadding(new Insets(4, 12, 4, 12));

                    HBox.setHgrow(textCol, Priority.ALWAYS);
                    HBox.setHgrow(rowSpacer, Priority.ALWAYS);
                    cardRow.setAlignment(Pos.CENTER_LEFT);
                    cardRow.setPadding(new Insets(12, 16, 12, 16));
                    setPadding(new Insets(5, 2, 5, 2));
                }

                // called by JavaFX whenever this row must be (re)drawn with an item
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { // blank row (show nothing)
                        setGraphic(null);
                        setText(null);
                        setStyle("-fx-background-color: transparent;");
                        return;
                    }

                    // the product's text
                    // first line = title, everything after = details
                    String raw = item.toString();
                    String[] lines = raw.split("\\R", 2); // \\R = any line break, split in 2 parts at most
                    titleLabel.setText(lines[0].trim());
                    detailLabel.setText(lines.length > 1 ? lines[1].trim() : "");
                    detailLabel.setVisible(lines.length > 1 && !lines[1].isBlank());
                    detailLabel.setManaged(detailLabel.isVisible()); // hidden details also take no space

                    applyStockBadge(statusPill, raw);

                    // the selected row gets a green highlight
                    boolean selected = isSelected();
                    cardRow.setStyle(
                        "-fx-background-color: " + (selected ? Theme.greenSoft() : Theme.creamBg()) + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + (selected ? Theme.green() : Theme.tan()) + ";" +
                        "-fx-border-width: 1.2;" +
                        "-fx-border-radius: 12;"
                    );
                    setStyle("-fx-background-color: transparent;");
                    setGraphic(cardRow);
                    setText(null);
                }
            });
        } else if (node instanceof Label label) {
            label.setFont(Font.font(Theme.BODY_FONT, FontWeight.NORMAL, 13));
            // labels containing "---" are treated as section headings
            if (label.getText() != null && label.getText().contains("---")) {
                label.setFont(Font.font(Theme.HEADING_FONT, FontWeight.BOLD, 13));
                label.setStyle("-fx-text-fill: " + Theme.brown() + ";");
            }
        } else if (node instanceof Button button) {
            button.setFont(Font.font(Theme.BODY_FONT, FontWeight.BOLD, 12));
            button.setStyle(
                "-fx-background-color: " + Theme.green() + ";" +
                "-fx-text-fill: " + Theme.white() + ";" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 6 14 6 14;" +
                "-fx-cursor: hand;"
            );
        }

        // keep going down
        // restyle everything inside this node too
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                enhanceProductPresentation(child);
            }
        }
    }

    // reads the stock number out of the product's text and colors the pill
    // No number found = "available"
    private void applyStockBadge(Label pill, String rawText) {
        Matcher m = STOCK_PATTERN.matcher(rawText);
        if (m.find()) {
            Widgets.stockPill(pill, Integer.parseInt(m.group(1))); // group(1) = the digits
        } else {
            Widgets.availablePill(pill);
        }
    }

    // loads the banner picture from the resources
    // If the file is missing the banner stays empty (green background)
    private ImageView createCoverBanner() {
        var url = getClass().getResource("/bukonek_cover.png");
        ImageView view = new ImageView();
        if (url != null) {
            view.setImage(new Image(url.toExternalForm()));
        }
        return view;
    }

    private ImageView createCircularLogo(double diameter) {
        return Widgets.circularLogo(diameter); // same proportion-keeping logo as the header
    }
}