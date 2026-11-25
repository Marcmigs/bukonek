package service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

// base of every class that owns data : after each change it calls the hook Main set (Main saves everything)
public abstract class Store {
	private Runnable onChange = null; // the save action given by Main ; null until Main sets it

	// Registers the action to run after every change (Main passes "save everything"). */
	public void setOnChange(Runnable onChange) { // sets the callback that runs whenever changed() is called
		this.onChange = onChange; // assigns the passed runnable to the onChange field
	}

	// redraws the panels currently on screen (a panel binds when it is built)
	// every panel binds under its own name, so a rebuilt panel replaces its old one instead of piling up
	private final Map<String, Runnable> viewRefreshers = new LinkedHashMap<>(); // name -> refresher ; insertion order is kept

	public void bindView(Runnable viewRefresher) { // using main as panel key
		bindView("main", viewRefresher); // delegates the bindView with key "main"
	}

	public void bindView(String name, Runnable viewRefresher) {
		viewRefreshers.put(name, viewRefresher);
	}

	public void refreshDisplay() { // triggers all registered UI refreshers
		// iterate over a copy : a refresher may bind a new view while running
		for (Runnable r : new ArrayList<>(viewRefreshers.values())) { // create snapshot list of current refreshers
			r.run(); // execute refresher callback to update UI
		}
	}

	protected void changed() { // protected helper for subclasses to signal a state change
		if (onChange != null) { // checks that a save callback was registered
			onChange.run(); // run the save callback
		}
	}
}