package users;

public class Clerk extends User { // a Clerk is-a User
    private static final long serialVersionUID = 1L;
    private final String managerUsername; // the manager this clerk is assigned to

    public Clerk(String username, String password, String managerUsername) {
    	super(username, password); // inherits the username and password instantiation from the superclass
    	this.managerUsername = managerUsername;
    }

    public String getManagerUsername() { return managerUsername; }
    
    @Override public String getRole() { // abstract method application getRole (will return Clerk)
    	return "Clerk";
    }
}