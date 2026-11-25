package ui;

import static app.Settings.tr;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import model.ContactRequest;
import model.StockNotice;
import model.Transaction;
import service.ContactBoard;

// every piece of text built from model data (moved out of Transaction, ContactRequest, StockNotice, ContactBoard)
public final class Formats {
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"); // e.g. 2026-11-24 14:05
	private Formats() {} // utility class : no objects

	// date and time as "yyyy-MM-dd HH:mm".
	public static String time(LocalDateTime t) { return t.format(TIME); }

	// money with the peso sign, thousands separators and 2 decimals, e.g. "₱1,250.00"
	public static String money(double v) { return String.format("₱%,.2f", v); }

	// running number padded to 3 digits with a "#", e.g. 7 becomes "#007".
	public static String id(int id) { return String.format("#%03d", id); }

	// rating as filled and empty stars, e.g. 4 becomes "★★★★☆" ; 0 becomes "Not rated".
	public static String stars(int stars) { return stars > 0 ? "★".repeat(stars) + "☆".repeat(5 - stars) : "Not rated"; }

	// e.g. "★ 4.5 (3)" ; average of every rating this clerk received
	public static String clerkRating(ContactBoard board, String clerkUsername) {
		int count = board.ratingCount(clerkUsername);
		if (count == 0) return tr("No ratings yet");
		return String.format("★ %.1f (%d)", board.averageRating(clerkUsername), count);
	}

	// "Email: ..." and/or "Phone: ..." (whichever the customer gave), one per line
	public static String contactText(ContactRequest r) {
		// the middle part adds a line break only when BOTH an email and a phone were given
		return (r.getEmail().isEmpty() ? "" : "Email: " + r.getEmail())
			+ (!r.getEmail().isEmpty() && !r.getPhone().isEmpty() ? "\n" : "")
			+ (r.getPhone().isEmpty() ? "" : "Phone: " + r.getPhone());
	}

	// one line of text for the history display
	public static String transactionLine(Transaction t, boolean showClerk) {
		String line = String.format("#%03d | %s | %s x%d | ₱%.2f",
			t.getId(), time(t.getTimestamp()), t.getItem(), t.getQuantity(), t.getAmount());
		if (showClerk) line += " | Clerk: " + t.getClerkUsername();
		if (!t.getRecordedBy().equals(t.getClerkUsername())) line += " | Added by: " + t.getRecordedBy();
		return line;
	}

	public static String noticeLine(StockNotice n) {
		return (n.isRead() ? "" : "[NEW] ")
			+ String.format("#%03d | %s | Clerk: %s", n.getId(), time(n.getTimestamp()), n.getClerkUsername())
			+ "\n      " + n.getMessage().replace("\n", "\n      "); // indent every line of the message
	}

	public static String contactRequestLine(ContactRequest r) {
		String contact = contactText(r).replace("\n", " | "); // email and phone on one line here
		return (r.isRead() ? "" : "[NEW] ")
			+ String.format("#%03d | %s | Ref: %s", r.getId(), time(r.getTimestamp()), r.getReference())
			+ "\n      Customer: " + r.getCustomerName()
			+ "\n      " + contact
			+ "\n      Reason: " + r.getReason()
			+ (r.getMessage().isEmpty() ? "" : "\n      Message: " + r.getMessage().replace("\n", "\n      "))
			+ (r.isRated() ? "\n      Rating: " + stars(r.getStars()) : "");
	}
}
