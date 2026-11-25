package ui;

import app.Theme;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import service.TransactionLog.ClerkStats;
import service.TransactionLog;

public class RankingDashboard {
	private final TransactionLog log; // where the sales are recorded

	public RankingDashboard(TransactionLog log) {
		this.log = log;
	}

	public Node build(List<String> clerkUsernames) {
		VBox holder = new VBox(10); // stays the same, only its contents are swapped on redraw
		holder.setPadding(new Insets(10));
		Runnable draw = () -> holder.getChildren().setAll(buildCharts(clerkUsernames)); // replaces the old charts with new ones
		draw.run();
		log.bindView(draw); // the log calls draw again whenever the records change
		return holder;
	}

	// the two charts side by side, or a message when nothing was sold yet
	private Node buildCharts(List<String> clerkUsernames) {
		if (log.isEmpty()) {
			Label none = new Label("No sales recorded yet.");
			none.setStyle("-fx-text-fill: " + Theme.textMuted() + ";");
			return none;
		}
		List<ClerkStats> ranked = log.computeRanking(clerkUsernames); // already sorted, best first
		BarChart<String, Number> pesos = rankChart("Total Sales (₱)", "₱", ranked, true);
		BarChart<String, Number> units = rankChart("Units Sold", "Units", ranked, false);
		HBox charts = new HBox(15, pesos, units);
		HBox.setHgrow(pesos, Priority.ALWAYS); // both charts share the width equally
		HBox.setHgrow(units, Priority.ALWAYS);
		return charts;
	}

	private BarChart<String, Number> rankChart(String heading, String axisLabel, List<ClerkStats> ranked, boolean pesos) {
		CategoryAxis clerkAxis = new CategoryAxis(); // horizontal (clerk names)
		NumberAxis valueAxis = new NumberAxis();     // vertical (pesos or units)
		valueAxis.setLabel(axisLabel);
		valueAxis.setForceZeroInRange(true); // bars always start from 0

		BarChart<String, Number> chart = new BarChart<>(clerkAxis, valueAxis);
		chart.setTitle(heading);
		chart.setLegendVisible(false); // only one series, a legend is not needed
		chart.setAnimated(false); // redraws instantly after a sale
		chart.setCategoryGap(14);
		chart.setPrefHeight(260);
		chart.setMaxWidth(Double.MAX_VALUE);
		chart.setStyle("-fx-text-background-color: " + Theme.textDark() + ";"); // axis and title text follow light / dark mode

		XYChart.Series<String, Number> series = new XYChart.Series<>();
		for (ClerkStats s : ranked) {
			// bar label looks like "#1 clerk01"
			// bar height is either the amount or the units
			XYChart.Data<String, Number> d = new XYChart.Data<>("#" + s.getRank() + " " + s.getName(),
				pesos ? s.getAmount() : s.getUnits());
			String fill = (s.getRank() == 1) ? Theme.brown() : Theme.green();
			// the bar's visual node does not exist yet when the data is added, so wait until JavaFX creates it, then color it
			d.nodeProperty().addListener((o, oldNode, node) -> {
				if (node != null) {
					node.setStyle("-fx-bar-fill: " + fill + ";");
				}
			});
			series.getData().add(d);
		}
		chart.getData().add(series);
		return chart;
	}
}