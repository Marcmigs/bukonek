package users;

public abstract class User implements java.io.Serializable { // Serializable = can be saved to a file
    private static final long serialVersionUID = 1L;
    private final String username; // username and password variables
    private final String password; // kept as plain text, so anyone who can open the save file can read it

    protected User(String username, String password) { // creates the constructors username/password
    	this.username = username;
    	this.password = password;
    }
    
    public String getUsername() { return username; }
       
    public boolean matches(String username, String password) { // function for checking username and password
        if (this.username.equals(username) && this.password.equals(password)) {
        	return true;
        }
		return false;
    }

    public abstract String getRole(); // abstract method for getRole (will return Clerk or Manager)
}