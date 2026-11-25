package service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import model.Message;

// what NoticeBoard and ContactBoard share : numbered items, restore from file, mark read, clear read
// they differ only in WHAT "clear" does to an item (see discard)
public abstract class MessageBoard<T extends Message> extends Store {
	protected final List<T> items = new ArrayList<>(); // every message, oldest first
	private int nextId = 1;                            // id the next message will get

	// used by DataStore
	public int getNextId() { return nextId; }

	// called by the child board right after it adds an item that used getNextId()
	protected void advanceId() { nextId++; }

	/** A copy of all messages, so callers cannot change the board's own list. */
	public List<T> getAll() { return new ArrayList<>(items); }

	// puts saved items back ; null = older save file without them
	public void restore(List<T> saved, int savedNextId) {
		items.clear(); // clears the whole item list
		if (saved != null) { // checks that saved items are provided
			items.addAll(saved); // recovers all the saved into the list
		}
		nextId = Math.max(savedNextId, 1); // ids never start below 1
	}

	public int unreadCount() {
		int count = 0; //initializing 
		for (T item : items) { // loops through the items
			if (!item.isRead()) { // checks if the item is read already  
				count++; // increment if not read yet 
			}
		}
		return count;
	}

	protected void markReadWhere(Predicate<T> scope) {
		for (T item : items) {
			if (scope.test(item)) {
				item.markRead();
			}
		}
		changed(); // save
	}

	// Discards every message that is already read and matches the scope, then saves. */
	protected void clearReadWhere(Predicate<T> scope) {
		// loop over a copy : discard() may remove the item from the real list
		for (T item : new ArrayList<>(items)) { // loops over a copy of items
			if (item.isRead() && scope.test(item)) { // checks both that the item is already read and matches
				discard(item); // delegates how item is discarded to the subclass implementation
			}
		}
		changed(); // save
	}

	// what clearing one read item means (notices are deleted, contact requests are only hidden)
	protected abstract void discard(T item);
}