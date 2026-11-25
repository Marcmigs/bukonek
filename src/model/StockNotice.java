//Commented by Carmie Shaine A. Decena
package model;

import java.time.LocalDateTime;

public class StockNotice implements Message {
	private static final long serialVersionUID = 1L; // version number used when saving / loading the file

	private final int id;                  // running number, assigned by NoticeBoard
	private final String clerkUsername;    // clerk who sent the notice
	private final String message;          // what the clerk typed
	private final LocalDateTime timestamp; // set automatically when created
	private boolean read = false;          // false until the manager marks it as read

	public StockNotice(int id, String clerkUsername, String message) {
		
		// validate first, so an invalid notice can never exist
		if (clerkUsername == null || clerkUsername.isBlank()) {
			throw new IllegalArgumentException("A notice must come from a clerk.");
		}
		if (message == null || message.isBlank()) {
			throw new IllegalArgumentException("Message cannot be empty.");
		}
		
		
		this.id = id;
		this.clerkUsername = clerkUsername;
		this.message = message.trim();
		this.timestamp = LocalDateTime.now();
	}

	// methods below come from the Message interface
	@Override public int getId() { return id; }
	@Override public LocalDateTime getTimestamp() { return timestamp; }
	@Override public boolean isRead() { return read; }
	@Override public void markRead() { this.read = true; } // one way only

	// getters specific to a stock notice
	public String getClerkUsername() { return clerkUsername; }
	public String getMessage() { return message; }
}
