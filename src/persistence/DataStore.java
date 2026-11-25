package persistence;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import model.ContactRequest;
import model.Equipment;
import model.ProductOption;
import model.Products;
import model.StockNotice;
import model.Transaction;
import service.ContactBoard;
import service.EquipmentStore;
import service.Inventory;
import service.NoticeBoard;
import service.TransactionLog;
import service.UserDirectory;
import service.Wallet;
import users.User;

// saving and loading : everything is written to one file ; on the first run (or an unreadable file) the
// store's starting catalog is used instead (the default data is at the bottom of this class)
public class DataStore {
	private static final Path FILE = Paths.get("bukonek-data.ser"); // saved next to where the app is run

	// every default variant starts with this much stock ; change here to rebalance
	public static final int STARTING_STOCK = 20;

	// everything that gets written, bundled into one object
	// (keep its serialVersionUID and only ADD fields : that is how older save files still load)
	private static class Snapshot implements Serializable {
		
		// serialize all the needed array lists and  values
		private static final long serialVersionUID = 1L;
		ArrayList<Products> products;
		ArrayList<Transaction> transactions;
		int nextTransactionId;
		// added later : older save files simply load these as null / 0 (handled in load)
		ArrayList<StockNotice> notices;
		int nextNoticeId;
		ArrayList<User> users;
		ArrayList<ContactRequest> contacts; // added later : null in older files
		int nextContactId;
		// equipment + wallet added later : null in older files
		ArrayList<Equipment> ownedEquipment;
		int nextEquipmentId;
		Double walletBalance; // Double (not double) so "missing in an old file" can be null
	}

	// returns true when the file was written ; false = something went wrong (Save button tells the user)
	public static boolean save(Inventory products, EquipmentStore equipment, TransactionLog log, NoticeBoard notices,
			UserDirectory directory, ContactBoard contacts, Wallet wallet) {
		// copy every list into the snapshot (copies, so the live lists are not tied to the file object)
		Snapshot s = new Snapshot();
		s.products = new ArrayList<>(products.getProducts());
		s.transactions = new ArrayList<>(log.getAll());
		s.nextTransactionId = log.getNextId();
		s.notices = new ArrayList<>(notices.getAll());
		s.nextNoticeId = notices.getNextId();
		s.users = new ArrayList<>(directory.getAll());
		s.contacts = new ArrayList<>(contacts.getAll());
		s.nextContactId = contacts.getNextId();
		s.ownedEquipment = new ArrayList<>(equipment.getAll());
		s.nextEquipmentId = equipment.getNextId();
		s.walletBalance = wallet.getBalance();

		// step 1 : write to "bukonek-data.ser.tmp"
		Path temp = Paths.get(FILE.toString() + ".tmp");
		try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temp))) {
			out.writeObject(s); // serialize and write object to temp file // closes stream automatically
		} catch (IOException e) { // catches I/O
			System.err.println("Could not save data: " + e.getMessage());
			return false;
		}
		// step 2 : only now replace the real file (the old one is never half-written)
		try {
			Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING); // moves/replaces
		} catch (IOException e) {
			System.err.println("Could not save data: " + e.getMessage());
			return false; // reports failure
		}
		return true; // save succeeded
	}

	// full path of the save file ; shown in the Save button message
	public static String getFilePath() {
		return FILE.toAbsolutePath().toString(); // Converts relative Path to absolute path and returns as String
	}

	public static void load(Inventory products, EquipmentStore equipment, TransactionLog log, NoticeBoard notices,
			UserDirectory directory, ContactBoard contacts, Wallet wallet) {
		if (!Files.exists(FILE)) { // Checks if the save file does not exist yet
			// first run : no save file yet, so start with the store's default catalog
			loadDefaults(products, equipment); // Populates default products and starter equipment
			return;
		}
		try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(FILE))) {
			Snapshot s = (Snapshot) in.readObject(); // Deserializes object from stream and casts it to Snapshot

			// each restore() handles the "null = older save file" case itself (except log.restore, which expects a list)
			products.restore(s.products);
			equipment.restore(s.ownedEquipment, s.nextEquipmentId);
			wallet.restore(s.walletBalance);
			log.restore(s.transactions, s.nextTransactionId);
			notices.restore(s.notices, s.nextNoticeId);
			contacts.restore(s.contacts, s.nextContactId);
			directory.restore(s.users);
		} catch (IOException | ClassNotFoundException e) {
			// unreadable file (e.g. classes changed) : fall back to the default catalog instead of starting empty
			System.err.println("Could not load saved data: " + e.getMessage());
			loadDefaults(products, equipment);
		}
	}

	// default data ///////////////////////////////////////
	// the store's starting catalog (only the single-unit sizes ; bundles are not part of the default data)

	// puts the default products + starter equipment into the stores (does not trigger a save)
	private static void loadDefaults(Inventory products, EquipmentStore equipment) {
		products.restore(buildProducts()); // Loads built default products into product inventory
		List<Equipment> starter = buildEquipment(); // Builds starter list of level-1 equipment
		equipment.restore(starter, starter.size() + 1); // next id continues after the starter equipment
	}

	// builds the 5 default products, each with its sizes and prices (stock is added by product())
	private static List<Products> buildProducts() {
		List<Products> list = new ArrayList<>();

		list.add(product("Coconut Juice",
			"Pure, naturally refreshing coconut juice packed with essential electrolytes to keep you hydrated throughout the day.",
			new String[]{"500 mL", "1L"},
			new double[]{85, 170}));

		list.add(product("Coconut Meat Chunks",
			"Thick, rich, and naturally flavored, fresh coconut meat loaded with healthy fats, fiber, and essential nutrients. "
			+ "Delicious snack on its own or an ingredient for smoothies, desserts, and savory dishes.",
			new String[]{"500 g", "1 kg"},
			new double[]{165, 325}));

		list.add(product("Coconut Husks",
			"Multi-purpose, coconut husk is a sustainable fiber for gardening, soil enrichment, and crafting. "
			+ "It has high moisture retention and a durable texture.",
			new String[]{"2 kg", "5 kg"},
			new double[]{80, 175}));

		list.add(product("Virgin Coconut Oil",
			"Rich in nourishing fats, versatile coconut oil is a healthy addition to your daily cooking and baking routines.",
			new String[]{"250 mL", "500 mL", "1000 mL"},
			new double[]{200, 340, 580}));

		list.add(product("Raw Coconut Shell",
			"Durable and organic coconut shells for charming and eco-friendly crafts and home decor. "
			+ "Each shell features unique natural textures and offers zero waste alternatives.",
			new String[]{"1 kg", "5 kg"},
			new double[]{30, 135}));

		return list;
	}

	// one of each equipment type at level 1, taken from the shop catalog (free : no wallet deduction)
	private static List<Equipment> buildEquipment() {
		List<Equipment> list = new ArrayList<>(); // initialize
		int id = 1;
		for (EquipmentStore.Model m : EquipmentStore.catalog()) { // Iterates over all entries in EquipmentStore's catalog
			if (m.level() == 1) { // Selects only level-1 model
				list.add(new Equipment(id++, m.type(), m.level(), m.price(), m.uses())); // Creates Equipment with incremented id and adds to list
			}
		}
		return list;
	}

	// labels[i] goes with prices[i] ; every variant starts with STARTING_STOCK
	private static Products product(String name, String description, String[] labels, double[] prices) {
		List<ProductOption> options = new ArrayList<>(); // initialize
		for (int i = 0; i < labels.length; i++) { // loops across each variant index
			options.add(new ProductOption(labels[i], prices[i], STARTING_STOCK)); // Creates a variant with label, price, and STARTING_STOCK and adds to options
		}
		return new Products(name, description, options);
	}
}