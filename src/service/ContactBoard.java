package service;

import static app.Settings.tr;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.ContactRequest;

// customer "Contact a Clerk" requests : customers send them, the chosen clerk reads them ; also holds the star ratings
public class ContactBoard extends MessageBoard<ContactRequest> {
	public static final String[] REASONS = {"Inquiry", "Create Order", "Refunds and Returns", "Report an Issue", "Other"};  // different options why to contact a clerk
	public static final int MAX_MESSAGE_LENGTH = 300; // most characters a customer can type in the message box
	private static final String REF_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I to avoid mix-ups

	private final Random random = new Random(); // used to build reference numbers

	// finds a request by its reference number (ignores upper/lower case) ; null if none
	private ContactRequest findByReference(String ref) {
		for (ContactRequest r : items) { // goes through all the request 
			if (r.getReference().equalsIgnoreCase(ref)) { // checks if the the reference matches any of the request
				return r;
			}
		}
		return null;
	}

	// builds a reference like "BK-7K3Q9X" : the prefix plus 6 random characters from REF_CHARS
	private String newReference() {
		StringBuilder sb = new StringBuilder("BK-"); // everything starts at BK
		for (int i = 0; i < 6; i++) {
			sb.append(REF_CHARS.charAt(random.nextInt(REF_CHARS.length()))); // generates 6 digits random characters
		}
		return sb.toString();
	}

	// customer sends a request to a clerk
	public ContactRequest submit(String clerkUsername, String name, String email, String phone, String reason, String message) {
		if (clerkUsername == null || clerkUsername.isBlank()) { // there are no existing clerks to contact
			throw new IllegalArgumentException(tr("No clerks are available right now."));
		}
		// if there are available clerks
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException(tr("Please enter your name.")); // makes the user enter their name
		}
		// null becomes "" so the checks below do not need null handling
		String em = (email == null) ? "" : email.trim(); // enter email
		String ph = (phone == null) ? "" : phone.trim(); // enter number
		if (em.isEmpty() && ph.isEmpty()) { // checks if email and number has been filled up
			throw new IllegalArgumentException(tr("Please enter an email or a phone number.")); 
		}
		// simple email shape : something@something.something, no spaces
		if (!em.isEmpty() && !em.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) { // checks if the entered email is in correct format 
			throw new IllegalArgumentException(tr("That email address doesn't look right.")); // error handling
		}
		// phone : optional +, then 7 to 20 digits, spaces, brackets or dashes
		if (!ph.isEmpty() && !ph.matches("[+]?[0-9 ()\\-]{7,20}")) { // checks if the number entered is valid
			throw new IllegalArgumentException(tr("That phone number doesn't look right.")); //error handling
		}
		String msg = (message == null) ? "" : message.trim(); // msg is initially blank
		if (msg.length() > MAX_MESSAGE_LENGTH) { // keeps track of length of messages
			throw new IllegalArgumentException(tr("Message is too long."));
		}
		// keep generating until the reference is not used by another request
		String ref;
		do {
			ref = newReference();
		} while (findByReference(ref) != null);

		ContactRequest r = new ContactRequest(getNextId(), ref, clerkUsername, name, em, ph, reason, msg); // instantiate a request
		items.add(r); // add request to the list 
		advanceId(); // moves to next id
		changed(); // save
		return r;
	}

	// customer rates the clerk of a request ; a reference number only works once
	public String rate(String reference, int stars) {
		if (reference == null || reference.isBlank()) { // checks if the reference number has been entered 
			throw new IllegalArgumentException(tr("Please enter your reference number.")); 
		}
		if (stars < 1 || stars > 5) {
			throw new IllegalArgumentException(tr("Please pick a star rating.")); // if the user didnt rate / havent clicked any star yet
		}
		ContactRequest r = findByReference(reference.trim()); //  find the request through the element
		if (r == null) {
			throw new IllegalArgumentException(tr("Reference number not found."));
		}
		if (r.isRated()) { // check if that reference number was already used to rate
			throw new IllegalArgumentException(tr("This reference number was already used to rate."));
		}
		r.setStars(stars); // update the value of the star for that reference number
		changed(); // save
		return r.getClerkUsername();
	}

	// ratings this clerk received (the text "★ 4.5 (3)" is built by ui.Formats)
	public int ratingCount(String clerkUsername) {
		int count = 0;
		for (ContactRequest r : items) {
			if (r.isRated() && r.getClerkUsername().equals(clerkUsername)) {
				count++;
			}
		}
		return count;
	}

	public double averageRating(String clerkUsername) {
		int sum = 0, count = 0;
		for (ContactRequest r : items) {
			if (r.isRated() && r.getClerkUsername().equals(clerkUsername)) {
				sum += r.getStars(); // adds all the starts rated 
				count++;
			}
		}
		return count == 0 ? 0 : (double) sum / count; // cast so the division keeps its decimals
	}
	
	public void markAllRead(String clerkUsername) {
		markReadWhere(r -> r.getClerkUsername().equals(clerkUsername));
	}

	// hides read requests from the board (kept in the data so ratings and reference numbers stay valid)
	public void clearRead(String clerkUsername) {
		clearReadWhere(r -> r.getClerkUsername().equals(clerkUsername));
	}

	// for requests, "clear" only hides them : deleting would lose the rating and free the reference number
	@Override
	protected void discard(ContactRequest request) { request.hide(); }

	// this clerk's requests that are not cleared, newest first
	public List<ContactRequest> visibleFor(String clerkUsername) {
		List<ContactRequest> result = new ArrayList<>();
		// walk backwards through the list, so the newest request comes first
		for (int i = items.size() - 1; i >= 0; i--) {
			ContactRequest r = items.get(i);
			if (r.getClerkUsername().equals(clerkUsername) && !r.isHidden()) { // request is not hidden (cleared)
				result.add(r); // add to list
			}
		}
		return result;
	}
}