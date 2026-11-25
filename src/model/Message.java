//Commented by Carmie Shaine A. Decena
package model;

import java.time.LocalDateTime;

/**
 * For any message that lands on a board (notice/contact request)
 * Anything on a board has: [1] a number, [2] a time, [3] a read flag
 * MessageBoard and MessageTablePanel, can handle either one the same way
 * Extends Serializable so every message can be saved to a file
 */
public interface Message extends java.io.Serializable {

	int getId(); 					// Running number of the message (assigned by the board that stores it)
	LocalDateTime getTimestamp(); 	// When the message was created
	boolean isRead(); 				// True once the receiver (manager or clerk) has marked it as read
	void markRead();				// Marks the message as read
}
