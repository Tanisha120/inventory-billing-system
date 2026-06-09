package project;

public class CartItem {
 private Product product;
 private int     quantity;

 public CartItem(Product product, int quantity) {
     this.product  = product;
     this.quantity = quantity;
 }

 public Product getProduct()  { return product; }
 public int     getQuantity() { return quantity; }
 public void    setQuantity(int q) { if (q > 0) quantity = q; }

 // Subtotal without GST
 public double getSubtotal() {
     return product.getPrice() * quantity;
 }

 public double getGstAmount() {
     return product.getPrice() * product.getGstRate() / 100.0 * quantity;
 }

 // Total including GST
 public double getTotal() {
     return getSubtotal() + getGstAmount();
 }

 @Override
 public String toString() {
     return String.format("%s x%d = Rs.%.2f (GST: Rs.%.2f)",
             product.getName(), quantity, getSubtotal(), getGstAmount());
 }
}

