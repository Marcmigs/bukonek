//Commented on by Carmie Shaine A. Decena
package model;

import java.time.LocalDateTime;

public class ContactRequest implements Message {
	private static final long serialVersionUID = 1L; // version number used when saving / loading the file

	private final int id;                  // running number, assigned by ContactBoard
	private final String reference;        // reference number the customer uses to rate (e.g. BK-7K3Q9X)
	private final String clerkUsername;    // clerk the request was sent to
	private final String customerName;     // trimmed in the constructor
	private final String email;            // may be empty (phone given instead)
	private final String phone;            // may be empty (email given instead)
	private final String reason;           // why the customer is contacting the clerk
	private final String message;          // what the customer typed to elaborate (may be empty)
	private final LocalDateTime timestamp; // set automatically when the request is created
	private boolean read = false;          // clerk marked it as read
	private boolean hidden = false;        // clerk cleared it from the board (kept so the rating is not lost)
	private int stars = 0;                 // 0 = not rated yet OR 1-5 once rated (reference number is then used up)

	public ContactRequest(int id, String reference, String clerkUsername, String customerName, 
							String email, String phone, String reason, String message) {
		this.id = id;
		this.reference = reference;
		this.clerkUsername = clerkUsername;
		this.customerName = customerName.trim();
		this.email = email;
		this.phone = phone;
		this.reason = reason;
		this.message = (message == null) ? "" : message.trim(); // null message -> empty string
		this.timestamp = LocalDateTime.now(); // time of creation, the caller cannot set it
	}

	// methods below come from the Message interface
	@Override public int getId() { return id; }
	@Override public LocalDateTime getTimestamp() { return timestamp; }
	@Override public boolean isRead() { return read; }
	@Override public void markRead() { this.read = true; } // one way only

	// getters for what the customer sent
	public String getReference() { return reference; }
	public String getClerkUsername() { return clerkUsername; }
	public String getCustomerName() { return customerName; }
	public String getEmail() { return email; }
	public String getPhone() { return phone; }
	public String getReason() { return reason; }
	public String getMessage() { return message == null ? "" : message; } 

	// hiding 
	// the request disappears from the clerk's board but stays stored, so its rating still counts
	public boolean isHidden() { return hidden; }
	public void hide() { this.hidden = true; }

	// rating
	// 0 means not rated yet
	public int getStars() { return stars; }
	public boolean isRated() { return stars > 0; } // any value above 0 means a rating was given
	public void setStars(int stars) { this.stars = stars; } // note : does not check the 1-5 range, the caller must
}
