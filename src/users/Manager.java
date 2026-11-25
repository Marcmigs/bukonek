package users;

public class Manager extends User { // a Manager is-a User
    private static final long serialVersionUID = 1L;

	public Manager(String username, String password) {
    	super(username, password); // inherits the username and password instantiation from the superclass
    }
    
    @Override public String getRole() { // abstract method application getRole (will return Manager)
    	return "Manager";
    }
}