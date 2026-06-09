package project;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.LocalDate;

public class Store {

 // ── Inventory ─────────────────────────────────────────────────────────────
 private List<Product>          products   = new ArrayList<>();   
 private Map<Integer, Product>  productMap = new HashMap<>();     
 private int nextId = 1;

 // ── Cart ──────────────────────────────────────────────────────────────────
 private List<CartItem> cart = new ArrayList<>();

 // ── Bill counter ──────────────────────────────────────────────────────────
 private int billCount = 0;

 // ── File names ────────────────────────────────────────────────────────────
 private static final String INVENTORY_FILE = "inventory.dat";
 private static final String RECEIPT_LOG    = "receipts.txt";

 public Store() {
     loadInventory(); 
     if (products.isEmpty()) loadSampleData();
 }

 // INVENTORY METHODS

 public void addProduct(Product p) {
     products.add(p);
     productMap.put(p.getId(), p);
     if (p.getId() >= nextId) nextId = p.getId() + 1;
     saveInventory(); 
 }

 public boolean removeProduct(int id) {
     Product p = productMap.get(id);
     if (p == null) return false;
     products.remove(p);
     productMap.remove(id);
     saveInventory();
     return true;
 }

 public Product findById(int id) {
     return productMap.get(id); 
 }

 public List<Product> getAllSortedByName() {
     List<Product> sorted = new ArrayList<>(products);
     Collections.sort(sorted, Comparator.comparing(Product::getName));
     return sorted;
 }

 public List<Product> getAllSortedByPrice() {
     List<Product> sorted = new ArrayList<>(products);
     Collections.sort(sorted, Comparator.comparingDouble(Product::getPrice));
     return sorted;
 }

 public List<Product> filterByCategory(String category) {
     List<Product> result = new ArrayList<>();
     Iterator<Product> it = products.iterator();  
     while (it.hasNext()) {
         Product p = it.next();
         if (p.getCategory().equalsIgnoreCase(category)) result.add(p);
     }
     return result;
 }

 public List<Product> searchByName(String keyword) {
     List<Product> result = new ArrayList<>();
     for (Product p : products) {
         if (p.getName().toLowerCase().contains(keyword.toLowerCase()))
             result.add(p);
     }
     return result;
 }

 public List<Product> getLowStock() {
     List<Product> low = new ArrayList<>();
     for (Product p : products) if (p.getStock() <= 3) low.add(p);
     return low;
 }

 public List<Product> getAllProducts() { return products; }

 public int getNextId() { return nextId++; }

 public void restockProduct(int id, int qty) {
     Product p = productMap.get(id);
     if (p != null) { p.restock(qty); saveInventory(); }
 }

 // CART METHODS

 public String addToCart(int productId, int qty) {
     Product p = productMap.get(productId);
     if (p == null)          return "Product not found.";
     if (p.isExpired()) return "Cannot sell expired product: " + p.getName();
     if (p.getStock() < qty) return "Only " + p.getStock() + " in stock.";

     // Check if already in cart
     for (CartItem item : cart) {
         if (item.getProduct().getId() == productId) {
             int newQty = item.getQuantity() + qty;
             if (newQty > p.getStock()) return "Not enough stock for " + newQty + " units.";
             item.setQuantity(newQty);
             return null;
         }
     }
     cart.add(new CartItem(p, qty));
     return null;
 }

 public void removeFromCart(int index) {
     if (index >= 0 && index < cart.size()) cart.remove(index);
 }

 public void clearCart() { cart.clear(); }

 public List<CartItem> getCart() { return cart; }

 public double getCartSubtotal() {
     double t = 0;
     for (CartItem i : cart) t += i.getSubtotal();
     return t;
 }

 public double getCartGst() {
     double t = 0;
     for (CartItem i : cart) t += i.getGstAmount();
     return t;
 }

 public double getCartTotal() { return getCartSubtotal() + getCartGst(); }

 // BILLING

 public synchronized String generateBill(String customerName) {
     if (cart.isEmpty()) return null;

     for (CartItem item : cart) {
         item.getProduct().sell(item.getQuantity());
     }

     billCount++;
     String timestamp = LocalDateTime.now()
             .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

     StringBuilder sb = new StringBuilder();
     sb.append("========================================\n");
     sb.append("        MINIMART — BILL RECEIPT         \n");
     sb.append("========================================\n");
     sb.append(String.format("Bill No   : #%04d%n", billCount));
     sb.append(String.format("Customer  : %s%n", customerName));
     sb.append(String.format("Date/Time : %s%n", timestamp));
     sb.append("----------------------------------------\n");
     sb.append(String.format("%-20s %4s %10s%n", "Item", "Qty", "Amount"));
     sb.append("----------------------------------------\n");
     for (CartItem item : cart) {
         sb.append(String.format("%-20s %4d %10.2f%n",
                 item.getProduct().getName(), item.getQuantity(), item.getSubtotal()));
         sb.append(String.format("  GST %.0f%%:%29.2f%n",
                 item.getProduct().getGstRate(), item.getGstAmount()));
     }
     sb.append("----------------------------------------\n");
     sb.append(String.format("%-24s %10.2f%n", "Subtotal:", getCartSubtotal()));
     sb.append(String.format("%-24s %10.2f%n", "Total GST:", getCartGst()));
     sb.append("========================================\n");
     sb.append(String.format("%-24s %10.2f%n", "GRAND TOTAL:", getCartTotal()));
     sb.append("========================================\n");
     sb.append("    Thank you for shopping with us!     \n");
     sb.append("========================================\n");

     String receipt = sb.toString();

     saveReceiptToFile(receipt);

     saveInventory(); 
     clearCart();
     return receipt;
 }

 // FILE I/O

 private void saveReceiptToFile(String receipt) {
     try (FileWriter fw = new FileWriter(RECEIPT_LOG, true)) {
         fw.write(receipt + "\n");
     } catch (IOException e) {
         System.out.println("Could not save receipt: " + e.getMessage());
     }
 }

 public String readReceiptLog() {
     StringBuilder sb = new StringBuilder();
     try (FileReader fr = new FileReader(RECEIPT_LOG)) {
         int ch;
         while ((ch = fr.read()) != -1) sb.append((char) ch);
     } catch (IOException e) {
         return "No receipts yet. Generate a bill first.";
     }
     return sb.toString();
 }

 private void saveInventory() {
     try (ObjectOutputStream oos = new ObjectOutputStream(
             new FileOutputStream(INVENTORY_FILE))) {
         oos.writeObject(products);
     } catch (IOException e) {
         System.out.println("Save error: " + e.getMessage());
     }
 }

 @SuppressWarnings("unchecked")
 private void loadInventory() {
     try (ObjectInputStream ois = new ObjectInputStream(
             new FileInputStream(INVENTORY_FILE))) {
         products = (List<Product>) ois.readObject();
         for (Product p : products) {
             productMap.put(p.getId(), p);
             if (p.getId() >= nextId) nextId = p.getId() + 1;
         }
     } catch (Exception e) {
         // First run - no file yet
     }
 }

 // SAMPLE DATA
 private void loadSampleData() {
     addProduct(new Product(nextId++, "Amul Milk 1L",      60,  20, "DAIRY", LocalDate.now().plusDays(5)));
     addProduct(new Product(nextId++, "Basmati Rice 5kg",  350, 15, "GROCERY", LocalDate.now().plusMonths(12)));
     addProduct(new Product(nextId++, "Whole Wheat Bread",  45, 10, "GROCERY", LocalDate.now().plusDays(3)));
     addProduct(new Product(nextId++, "Cheddar Cheese",    180,  3, "DAIRY", LocalDate.now().plusDays(10)));
     addProduct(new Product(nextId++, "Samsung Earbuds",  1999,  8, "ELECTRONICS", LocalDate.now().plusYears(3)));
     addProduct(new Product(nextId++, "Philips LED Bulb",  249, 25, "ELECTRONICS", LocalDate.now().plusYears(2)));
     addProduct(new Product(nextId++, "Cotton T-Shirt",    499, 12, "CLOTHING", LocalDate.now().plusYears(2)));
     addProduct(new Product(nextId++, "Denim Jeans",       999,  6, "CLOTHING", LocalDate.now().plusYears(2)));
     addProduct(new Product(nextId++, "Orange Juice 1L",   120,  2, "GROCERY", LocalDate.now().plusDays(2)));
     addProduct(new Product(nextId++, "USB-C Cable",        299, 30, "ELECTRONICS", LocalDate.now().plusYears(3)));
 }
}

