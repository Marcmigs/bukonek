package service;


import java.util.ArrayList;
import java.util.List;
import users.Clerk;
import users.Manager;
import users.User;

// every account in the system : logs people in and lets a manager hire clerks
public class UserDirectory extends Store {
	private static final int MIN_USERNAME_LENGTH = 3; // constants for user name
	public static final int MIN_PASSWORD_LENGTH = 6;
	public static final int MAX_CLERKS = 10; // most clerks one manager can have

	private final List<User> users = new ArrayList<>(); // the manager and all clerks


	// pre-built accounts ; used until a save file with its own accounts replaces them
	public UserDirectory() {
		users.add(new Manager("manager", "123"));
		users.add(new Clerk("clerk01", "123", "manager"));
		users.add(new Clerk("clerk02", "123", "manager"));
		users.add(new Clerk("clerk03", "123", "manager"));
		users.add(new Clerk("clerk04", "123", "manager"));
		users.add(new Clerk("clerk05", "123", "manager"));
	}

	// used by DataStore
	public List<User> getAll() {
		return new ArrayList<>(users); // a copy, so callers cannot change the real list
	}

	// puts saved accounts back ; no saved accounts (older save file) = keep the pre-built ones
	public void restore(List<User> saved) {
		if (saved == null || saved.isEmpty()) { // check if there's existing saved 
			return;
		}
		users.clear(); // if theres existing, clear the users 
		users.addAll(saved); // restore the ones from saved
	}

	// returns the matching user, or null when the username / password is wrong
	public User authenticate(String username, String password) { // authenticate when a user logs in 
		for (User u : users) { // goes through all the users
			if (u.matches(username, password)) { // find if any user matches the passsword
				return u;
			}
		}
		return null;
	}

	public User find(String username) { // checks if theres an existing username
		for (User u : users) { // loops through all the users
			if (u.getUsername().equals(username)) { // compares to each username
				return u;
			}
		}
		return null;
	}

	// usernames of every clerk (customers pick from these in "Contact a Clerk")
	public List<String> getAllClerkUsernames() {
		List<String> result = new ArrayList<>(); // create a new array
		for (User u : users) { // loops through all the users
			if (u instanceof Clerk) {
				result.add(u.getUsername()); // adds the username to the array
			}
		}
		return result;
	}

	// usernames of the clerks assigned to this manager
	public List<String> getClerkUsernames(String managerUsername) {
		List<String> result = new ArrayList<>(); // create new array
		for (User u : users) { // goes through all the users
			if (u instanceof Clerk && ((Clerk) u).getManagerUsername().equals(managerUsername)) { // check if its a clerk and if that clerk is chosen by a manager
				result.add(u.getUsername()); // add the username to the list
			}
		}
		return result;
	}

	// false once the manager already has MAX_CLERKS clerks
	public boolean canHire(String managerUsername) {
		return getClerkUsernames(managerUsername).size() < MAX_CLERKS; // compares the value of the clerks and the max clerks allowed
	}

	// manager hires a new clerk : creates the account and assigns it to that manager
	public Clerk hireClerk(String managerUsername, String username, String password) { //  validates the parameters needed to create and save a new Clerk
		if (!(find(managerUsername) instanceof Manager)) { // looks up a manager username and checks if the matching user is a manager
			throw new IllegalArgumentException("Only a manager can hire clerks."); // throws if username is not a manager
		}
		if (!canHire(managerUsername)) { // check if the count of current clerk is less than the max clerk
			throw new IllegalArgumentException("You already have the maximum of " + MAX_CLERKS + " clerks.");
		}

		String name = (username == null) ? "" : username.trim(); // converts null username to empty string, otherwise trims leading whitespace
		if (name.length() < MIN_USERNAME_LENGTH) { // checks if length is less than the minimum length
			throw new IllegalArgumentException("Username must be at least " + MIN_USERNAME_LENGTH + " characters."); // throws message 
		}
		if (!name.matches("[A-Za-z0-9_]+")) { // checks if the name uses valid characters 
			throw new IllegalArgumentException("Username can only use letters, numbers and underscores."); // throws message if not valid
		}
		for (User u : users) { // goes through all the users
			if (u.getUsername().equalsIgnoreCase(name)) { // compares if an existing user already has that username
				throw new IllegalArgumentException("That username is already taken."); // throws message if taken already
			}
		}
		if (password == null || password.length() < MIN_PASSWORD_LENGTH) { // checks if password length if valid
			throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters."); // throws message if invalid
		}

		Clerk clerk = new Clerk(name, password, managerUsername); // instantiate a new clerk
		users.add(clerk); // add to the user list
		changed(); // save
		return clerk;
	}
}