package export;

import model.ProductOption;
import model.Products;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CatalogPdf {
	private static final double W = 595, H = 842, M = 48, BOTTOM = 50; // page size for A4 (72 points to make an inch)

	// palette in normalized RGB [0.0 - 1.0] as required by PDF color operators
	private static final double[] GREEN = {0.420, 0.604, 0.122}, BROWN = {0.345, 0.165, 0.078},
		TAN = {0.851, 0.702, 0.549}, CREAM = {0.957, 0.961, 0.941}, MUTED = {0.431, 0.416, 0.388},
		DARK = {0.137, 0.122, 0.125}, WHITE = {1, 1, 1};

	private final List<StringBuilder> pages = new ArrayList<>(); // raw PDF drawing commands for each page
	private StringBuilder cur; // active page stream currently being drawn onto
	private double y; // current vertical cursor position on the active page

	// generates the catalog PDF for the given products and writes the finished file to disk
	public static void export(List<Products> products, Path file) throws IOException {
		CatalogPdf pdf = new CatalogPdf();
		pdf.layout(products); // lay out banners, product cards, and footers onto pages
		Files.write(file, pdf.build()); // wrap pages into PDF 1.4 structure and save bytes
	}

	// formats a number to 2 decimal places using Locale.ROOT so decimal points are always '.' (never ',')
	private static String n(double v) {
		return String.format(Locale.ROOT, "%.2f", v);
	}

	// sets the active RGB color; op is "rg" for fill color or "RG" for outline/stroke color
	private void color(double[] c, String op) {
		cur.append(n(c[0])).append(' ').append(n(c[1])).append(' ').append(n(c[2])).append(' ').append(op).append('\n');
	}

	// draws a rectangle from bottom-left corner (x, yy); filled = true fills solid ("re f"), false draws border ("re S")
	private void rect(double x, double yy, double w, double h, double[] c, boolean filled) {
		color(c, filled ? "rg" : "RG");
		cur.append(n(x)).append(' ').append(n(yy)).append(' ').append(n(w)).append(' ').append(n(h))
			.append(filled ? " re f\n" : " re S\n");
	}

	// renders one line of text at baseline (x, yy); /F2 = Helvetica-Bold, /F1 = Helvetica (defined in build())
	private void text(boolean bold, double size, double x, double yy, double[] c, String s) {
		color(c, "rg");
		cur.append("BT /").append(bold ? "F2" : "F1").append(' ').append(n(size)).append(" Tf ")
			.append(n(x)).append(' ').append(n(yy)).append(" Td (").append(esc(s)).append(") Tj ET\n");
	}

	// sanitizes text for PDF's built-in WinAnsiEncoding (Latin-1) and escapes special PDF string characters
	private static String esc(String s) {
		StringBuilder b = new StringBuilder();
		for (char ch : s.toCharArray()) {
			if (ch == '\u20B1') b.append("PHP "); // standard Helvetica lacks the ₱ glyph
			else if (ch == '\\' || ch == '(' || ch == ')') b.append('\\').append(ch); // escape PDF string delimiters
			else if (ch < 32) b.append(' '); // replace newlines/tabs with spaces
			else if (ch > 255) b.append('?'); // replace unsupported non-Latin-1 chars
			else b.append(ch);
		}
		return b.toString();
	}

	// word-wraps text into lines of at most max characters without breaking words mid-word
	private static List<String> wrap(String text, int max) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.trim().split("\\s+")) {
			if (word.isEmpty()) continue;
			if (line.length() > 0 && line.length() + 1 + word.length() > max) {
				lines.add(line.toString()); // current line is full; commit it and start a new line
				line.setLength(0);
			}
			if (line.length() > 0) line.append(' ');
			line.append(word);
		}
		if (line.length() > 0) lines.add(line.toString());
		return lines;
	}
	
	// starts a new page: page 1 gets the full green header banner with export date; later pages get a slim running header
	private void newPage(boolean first) {
		cur = new StringBuilder();
		pages.add(cur);
		if (first) {
			rect(0, H - 92, W, 92, GREEN, true); // full-width green banner across the top of page 1
			text(true, 26, M, H - 48, WHITE, "BUKONEK");
			text(false, 11, M, H - 68, WHITE, "Product Catalog");
			text(false, 10, W - M - 105, H - 68, WHITE, LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)));
			y = H - 92 - 24; // position cursor 24pt below the banner
		} else {
			text(true, 10, M, H - 34, BROWN, "BUKONEK - Product Catalog");
			rect(M, H - 42, W - 2 * M, 2, GREEN, true); // thin green divider line
			y = H - 42 - 22; // position cursor 22pt below the divider
		}
	}

	// draws one card per product from top to bottom, spilling onto a new page whenever a card would cross BOTTOM
	private void layout(List<Products> products) {
		newPage(true);
		if (products.isEmpty()) {
			text(false, 12, M, y - 10, MUTED, "No products available.");
		}
		for (Products p : products) {
			List<String> desc = wrap(p.getDescription() == null ? "" : p.getDescription(), 88);
			int k = Math.max(p.getOptions().size(), 1); // reserve at least 1 row even if no options exist ("Price on request")
			double h = 42 + 13 * desc.size() + 15 * k;  // calculate total card height from title + description lines + variant rows
			if (y - h < BOTTOM) newPage(false);         // start a new page if this card won't fit above the footer margin

			// draw card background, border, and left green accent strip (y - h is the card's bottom-left y-coordinate)
			rect(M, y - h, W - 2 * M, h, CREAM, true);
			rect(M, y - h, W - 2 * M, h, TAN, false);
			rect(M, y - h, 4, h, GREEN, true);

			double ty = y - 26; // text baseline cursor moving downward inside the card
			text(true, 13, M + 16, ty, BROWN, p.getName());
			ty -= 17;
			for (String line : desc) {
				text(false, 10, M + 16, ty, MUTED, line);
				ty -= 13;
			}
			ty -= 2;
			if (p.getOptions().isEmpty()) {
				text(false, 10, M + 16, ty, DARK, "Price on request");
			}
			for (ProductOption o : p.getOptions()) {
				String label = (o.getLabel() == null || o.getLabel().isBlank()) ? "Standard" : o.getLabel();
				text(false, 10, M + 16, ty, DARK, label);
				text(true, 10, W - M - 130, ty, GREEN, String.format(Locale.US, "PHP %,.2f", o.getPrice())); // right-aligned price
				ty -= 15;
			}
			y -= h + 10; // advance cursor below this card plus a 10pt vertical gap
		}

		// draw footers in a second pass now that pages.size() is known for "Page X of N"
		for (int i = 0; i < pages.size(); i++) {
			cur = pages.get(i);
			rect(0, 0, W, 26, BROWN, true); // bottom brown bar matching the app footer
			text(true, 8, M, 9, TAN, "DECENA, DE GUZMAN, ESPIRITU");
			text(false, 8, W - M - 55, 9, TAN, "Page " + (i + 1) + " of " + pages.size());
		}
	}

	// packages all page command streams into a complete PDF 1.4 byte array with object table and byte-offset xref
	// object numbering: 1 = Catalog, 2 = Pages root, 3 = Helvetica, 4 = Helvetica-Bold,
	// and for each page i: (5 + 2i) = Page dictionary, (6 + 2i) = Page content stream
	private byte[] build() {
		StringBuilder sb = new StringBuilder("%PDF-1.4\n");
		List<Integer> offsets = new ArrayList<>(); // records exact byte offset of each object for the xref table
		int count = pages.size();
		StringBuilder kids = new StringBuilder(); // builds "/Kids [5 0 R 7 0 R ...]" list of page object references
		for (int i = 0; i < count; i++) kids.append(5 + 2 * i).append(" 0 R ");

		obj(sb, offsets, "<< /Type /Catalog /Pages 2 0 R >>"); // object 1: document root
		obj(sb, offsets, "<< /Type /Pages /Kids [" + kids + "] /Count " + count + " >>"); // object 2: page tree
		obj(sb, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"); // object 3: regular font (/F1)
		obj(sb, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"); // object 4: bold font (/F2)
		for (int i = 0; i < count; i++) {
			// page dictionary linking fonts (/F1, /F2) and its content stream object (6 + 2i)
			obj(sb, offsets, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
				+ "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + (6 + 2 * i) + " 0 R >>");
			String content = pages.get(i).toString();
			obj(sb, offsets, "<< /Length " + content.length() + " >>\nstream\n" + content + "endstream"); // page drawing stream
		}

		// cross-reference (xref) table: gives PDF readers the exact 10-digit byte position of every object
		int xref = sb.length();
		sb.append("xref\n0 ").append(offsets.size() + 1).append("\n0000000000 65535 f \n"); // object 0 is always the free head
		for (int off : offsets) sb.append(String.format("%010d 00000 n \n", off));
		sb.append("trailer\n<< /Size ").append(offsets.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
			.append(xref).append("\n%%EOF\n");
		return sb.toString().getBytes(StandardCharsets.ISO_8859_1); // 1 char = 1 byte in ISO-8859-1, matching sb.length() offsets
	}

	// records the starting byte offset of a new PDF object and appends "N 0 obj ... endobj"
	private static void obj(StringBuilder sb, List<Integer> offsets, String body) {
		offsets.add(sb.length());
		sb.append(offsets.size()).append(" 0 obj\n").append(body).append("\nendobj\n");
	}
}