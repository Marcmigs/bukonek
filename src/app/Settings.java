package app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

// app settings (appearance, language, text size) + the Tagalog word list (saved to its own small file)
public final class Settings {
	public static boolean dark = false;
	public static String accent = "Green";      // Green, Brown, Blue
	public static String language = "English";  // English, Tagalog
	public static String textSize = "Normal";   // Small, Normal, Large

	private static final Path FILE = Paths.get("bukonek-settings.properties");
	private static final Map<String, String> TL = new HashMap<>(); // English text -> Tagalog text

	// runs once when the class loads : fills the translation map from the pairs below
	static {
		String[][] pairs = {
			// each pair is { English, Tagalog } ; the English text must match the text passed to tr() exactly
			// header + sidebar
			{"CUSTOMER VIEW", "TANAW NG BISITA"}, {"LOG IN", "MAG-LOG IN"}, {"LOG OUT", "MAG-LOG OUT"}, {"SAVE", "I-SAVE"},
			{"Search catalog or view...", "Maghanap sa katalogo..."},
			{"Product Catalog", "Katalogo ng Produkto"}, {"Contact a Clerk", "Kontakin ang Clerk"},
			{"Rate a Clerk", "I-rate ang Clerk"}, {"Download Catalog", "I-download ang Katalogo"},
			{"Settings", "Mga Setting"}, {"My Workspace", "Aking Workspace"},
			{"Fresh coconut & local catalog", "Sariwang niyog at lokal na katalogo"},
			// settings page
			{"SETTINGS", "MGA SETTING"}, {"Appearance", "Hitsura"}, {"Mode", "Mode"}, {"Light", "Maliwanag"}, {"Dark", "Madilim"},
			{"Accent color", "Kulay ng accent"}, {"Green", "Berde"}, {"Brown", "Kayumanggi"}, {"Blue", "Asul"},
			{"Language", "Wika"}, {"Display language", "Wika ng display"}, {"More", "Higit pa"},
			{"Text size", "Laki ng teksto"}, {"Small", "Maliit"}, {"Normal", "Katamtaman"}, {"Large", "Malaki"},
			{"Reset to defaults", "Ibalik sa default"}, {"Settings are saved automatically.", "Awtomatikong sine-save ang mga setting."},
			// contact a clerk
			{"Pick one of the available clerks and tell us how we can help.", "Pumili ng available na clerk at sabihin kung paano kami makakatulong."},
			{"Clerk:", "Clerk:"}, {"Your name:", "Pangalan mo:"}, {"Email:", "Email:"}, {"Phone number:", "Numero ng telepono:"}, {"Reason:", "Dahilan:"},
			{"Send", "Ipadala"}, {"Inquiry", "Katanungan"}, {"Create Order", "Gumawa ng Order"},
			{"Message (optional):", "Mensahe (opsyonal):"}, {"Message is too long.", "Masyadong mahaba ang mensahe."},			{"Refunds and Returns", "Refund at Pagbabalik"}, {"Report an Issue", "Mag-ulat ng Isyu"}, {"Other", "Iba pa"},
			{"No clerks are available right now.", "Walang available na clerk ngayon."},
			{"Please enter your name.", "Pakilagay ang iyong pangalan."},
			{"Please enter an email or a phone number.", "Maglagay ng email o numero ng telepono."},
			{"That email address doesn't look right.", "Mukhang mali ang email address."},
			{"That phone number doesn't look right.", "Mukhang mali ang numero ng telepono."},
			{"Failed to send! ", "Hindi naipadala! "}, {"Request sent!", "Naipadala na!"},
			{"Your request was sent to", "Naipadala ang iyong request kay"},
			{"Keep this reference number to rate your clerk later.", "Itago ang reference number na ito para ma-rate ang clerk mamaya."},
			{"No ratings yet", "Wala pang rating"},
			// rate a clerk
			{"Enter the reference number from your contact request.", "Ilagay ang reference number ng iyong contact request."},
			{"Reference number:", "Reference number:"}, {"Rating:", "Rating:"}, {"Submit", "Isumite"},
			{"Please enter your reference number.", "Pakilagay ang iyong reference number."},
			{"Please pick a star rating.", "Pumili ng bituin para sa rating."},
			{"Reference number not found.", "Hindi nahanap ang reference number."},
			{"This reference number was already used to rate.", "Nagamit na ang reference number na ito sa pag-rate."},
			{"Thank you! Your rating was submitted.", "Salamat! Naisumite na ang iyong rating."},
			{"Failed to rate! ", "Hindi na-rate! "},
			// download catalog
			{"Save catalog as PDF", "I-save ang katalogo bilang PDF"}, {"Catalog saved to:", "Na-save ang katalogo sa:"},
			{"Could not create the PDF.", "Hindi magawa ang PDF."},
			{"There are no products to export.", "Walang produktong ma-e-export."}
		};
		for (String[] p : pairs) {
			TL.put(p[0], p[1]);
		}
	}

	private Settings() {} // utility class, no objects

	// translate an English text
	public static String tr(String english) {
		if (!"Tagalog".equals(language)) return english;
		return TL.getOrDefault(english, english);
	}

	// text size in pixels
	public static int fontPx() {
		return switch (textSize) { case "Small" -> 11; case "Large" -> 14; default -> 12; };
	}

	// sets main's color fields for the chosen mode + accent (screens read them every time they are built)
	public static void applyColors() {
		Theme.apply(dark, accent); // Theme owns the colours now
	}

	// puts every setting back to its default
	public static void reset() {
		dark = false; accent = "Green"; language = "English"; textSize = "Normal";
	}

	// reads the settings configurations file if it exists
	public static void load() {
		if (Files.exists(FILE)) {
			Properties p = new Properties();
			try (InputStream in = Files.newInputStream(FILE)) {
				p.load(in);
				dark = Boolean.parseBoolean(p.getProperty("dark", "false"));
				accent = p.getProperty("accent", "Green");
				language = p.getProperty("language", "English");
				textSize = p.getProperty("textSize", "Normal");
			} catch (IOException e) {
				System.err.println("Could not load settings: " + e.getMessage());
			}
		}
	}

	// saves settings
	public static void save() {
		Properties p = new Properties();
		p.setProperty("dark", String.valueOf(dark));
		p.setProperty("accent", accent);
		p.setProperty("language", language);
		p.setProperty("textSize", textSize);
		try (OutputStream out = Files.newOutputStream(FILE)) {
			p.store(out, "Bukonek settings");
		} catch (IOException e) {
			System.err.println("Could not save settings: " + e.getMessage());
		}
	}
}