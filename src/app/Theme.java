package app;

public final class Theme {
	// global font families used across headings and body controls
	public static final String HEADING_FONT = "Trebuchet MS";
	public static final String BODY_FONT = "Segoe UI";

	// active CSS hex color tokens (initialized to Light Mode + Green accent defaults; overwritten by apply())
	private static String green = "#6b9a1f", greenSoft = "#eef5e3", brown = "#582a14", tan = "#d9b38c",
		creamBg = "#f4f5f0", white = "#ffffff", borderLight = "#e4e6de", textDark = "#231f20",
		textMuted = "#6e6a63", surface = "#ffffff", blush = "#f7ede6", blush2 = "#fbf3e4";

	// read-only color getters called by UI builders when assembling CSS styles
	public static String green() { return green; }             // primary accent color (buttons, active nav tabs, links)
	public static String greenSoft() { return greenSoft; }     // soft accent tint (role pills, "In Stock" badges)
	public static String brown() { return brown; }             // brand title, section headings, and warning pill text
	public static String tan() { return tan; }                 // warm secondary border and card accent
	public static String creamBg() { return creamBg; }         // main window and page background color
	public static String white() { return white; }             // high-contrast text on solid accent buttons
	public static String borderLight() { return borderLight; } // subtle card, header, and sidebar divider lines
	public static String textDark() { return textDark; }       // primary body text color
	public static String textMuted() { return textMuted; }     // secondary helper labels and captions
	public static String surface() { return surface; }         // elevated card, header, and sidebar surface fill
	public static String blush() { return blush; }             // "Out of Stock" pill background
	public static String blush2() { return blush2; }           // "Low Stock" pill background

	public static void apply(boolean dark, String accent) {
		switch (accent) { // dark or light mode
			case "Brown" -> {
				green = dark ? "#b5703f" : "#8a4b2a";
				greenSoft = dark ? "#4a3426" : "#f5ebe3";
			}
			case "Blue" -> {
				green = dark ? "#4a8fd4" : "#2f6fb0";
				greenSoft = dark ? "#243649" : "#e6eef8";
			}
			default -> { // Green, and the fallback for any unknown accent
				green = dark ? "#7aab2a" : "#6b9a1f";
				greenSoft = dark ? "#33402a" : "#eef5e3";
			}
		}
		
		// accent colors
		creamBg = dark ? "#1d1e1b" : "#f4f5f0";
		surface = dark ? "#2a2b27" : "#ffffff";
		white = "#ffffff";
		borderLight = dark ? "#42443c" : "#e4e6de";
		textDark = dark ? "#ece9e2" : "#231f20";
		textMuted = dark ? "#a9a59c" : "#6e6a63";
		brown = dark ? "#e6c3a0" : "#582a14";
		tan = dark ? "#6b5440" : "#d9b38c";
		blush = dark ? "#4a3426" : "#f7ede6";
		blush2 = dark ? "#4a4030" : "#fbf3e4";
	}
}