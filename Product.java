package project;

import java.io.Serializable;
import java.time.LocalDate;

public class Product implements Serializable {
 private static final long serialVersionUID = 1L;

 private int id;
 private String name;
 private double price;
 private int stock;
 private String category; 
 private LocalDate expiryDate;

 public Product(int id, String name, double price, int stock, String category, LocalDate expiryDate) {
     this.id       = id;
     this.name     = name;
     this.category = category;
     setPrice(price);
     setStock(stock);
     setExpiryDate(expiryDate);
 }

 public int    getId()       { return id; }
 public String getName()     { return name; }
 public double getPrice()    { return price; }
 public int    getStock()    { return stock; }
 public String getCategory() { return category; }
 public LocalDate getExpiryDate() {
	    return expiryDate;
	}

 public void setPrice(double price) {
     if (price > 0) this.price = price;
     else throw new IllegalArgumentException("Price must be positive.");
 }

 public void setStock(int stock) {
     if (stock >= 0) this.stock = stock;
     else throw new IllegalArgumentException("Stock cannot be negative.");
 }

 public void setName(String name) {
     if (name != null && !name.trim().isEmpty()) this.name = name;
 }
 
 public void setExpiryDate(LocalDate expiryDate) {
	    if (expiryDate == null) {
	        throw new IllegalArgumentException("Expiry date cannot be empty.");
	    }
	    this.expiryDate = expiryDate;
	}

 public boolean sell(int qty) {
     if (qty <= stock) { stock -= qty; return true; }
     return false;
 }

 public boolean isExpired() {
	    return expiryDate.isBefore(LocalDate.now());
	}

public boolean isExpiringSoon() {
	    return !isExpired() && !expiryDate.isAfter(LocalDate.now().plusDays(7));
	}
	
 public void restock(int qty) {
     if (qty > 0) stock += qty;
 }

 public double getGstRate() {
     switch (category.toUpperCase()) {
         case "ELECTRONICS": return 18.0;
         case "CLOTHING":    return 12.0;
         case "GROCERY":     return 5.0;
         case "DAIRY":       return 0.0;
         default:            return 5.0;
     }
 }

 public double getPriceWithGst() {
     return price + (price * getGstRate() / 100.0);
 }

 @Override
 public String toString() {
     return String.format("[%d] %s | Rs.%.2f | Stock: %d | %s | Expiry: %s", id, name, price, stock, category, expiryDate);
 }
}
