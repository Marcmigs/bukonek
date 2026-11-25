package service;

import model.StockNotice;

// low stock notices : clerks send them, the manager reads them
public class NoticeBoard extends MessageBoard<StockNotice> {
	public static final int MAX_MESSAGE_LENGTH = 300; // most characters a notice can have

	// clerk sends a notice to the manager
	public StockNotice send(String clerkUsername, String message) {
		if (message == null || message.isBlank()) {
			throw new IllegalArgumentException("Please type a message for the manager.");
		}
		if (message.trim().length() > MAX_MESSAGE_LENGTH) {
			throw new IllegalArgumentException("Message is too long (max " + MAX_MESSAGE_LENGTH + " characters).");
		}
		StockNotice n = new StockNotice(getNextId(), clerkUsername, message);
		items.add(n);
		advanceId(); // the id is used up, so move to the next one
		changed(); // save
		return n;
	}

	public void markAllRead() { markReadWhere(n -> true); }

	// removes notices the manager already read
	public void clearRead() { clearReadWhere(n -> true); }

	// for notices, "clear" means really deleting them (unlike ContactBoard, which only hides)
	@Override
	protected void discard(StockNotice notice) { items.remove(notice); }
}