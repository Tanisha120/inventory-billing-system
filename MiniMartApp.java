package project;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.time.LocalDate;

public class MiniMartApp extends Application {

private final Store store = new Store();

// ── Tab 1: Inventory ──────────────────────────────────────────────────────
private TableView<PRow>          invTable;
private ObservableList<PRow>     invData  = FXCollections.observableArrayList();
private Label                    invStatus;

// ── Tab 2: Billing ────────────────────────────────────────────────────────
private TableView<CRow>          cartTable;
private ObservableList<CRow>     cartData = FXCollections.observableArrayList();
private Label                    lblSub, lblGst, lblTotal, billStatus;
private ComboBox<String>         cbProduct;
private TextField                tfQty;
private TextArea                 taReceipt;

@Override
public void start(Stage stage) {

    TabPane tabs = new TabPane(
        buildInventoryTab(),
        buildBillingTab(),
        buildReceiptLogTab()
    );
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    tabs.setStyle("-fx-tab-min-width:160;-fx-font-size:13;");

    tabs.getSelectionModel().selectedIndexProperty().addListener(
        (obs, o, n) -> { if (n.intValue() == 1) refreshProductCombo(); }
    );

    Label title = new Label("MiniMart — Inventory & Billing System");
    title.setStyle("-fx-font-size:17;-fx-font-weight:bold;-fx-text-fill:#1a5276;");
    HBox header = new HBox(title);
    header.setPadding(new Insets(10, 16, 10, 16));
    header.setStyle("-fx-background-color:#eaf4fb;-fx-border-color:#aed6f1;-fx-border-width:0 0 1 0;");

    BorderPane root = new BorderPane();
    root.setTop(header);
    root.setCenter(tabs);

    stage.setScene(new Scene(root, 1050, 680));
    stage.setTitle("MiniMart");
    stage.setMinWidth(820);
    stage.setMinHeight(520);
    stage.show();
}

// TAB 1 — INVENTORY
private Tab buildInventoryTab() {
    Tab tab = new Tab("Inventory");

    TextField tfSearch = new TextField();
    tfSearch.setPromptText("Search by name...");
    tfSearch.setPrefWidth(180);

    ComboBox<String> cbCatFilter = new ComboBox<>();
    cbCatFilter.getItems().addAll("ALL", "GROCERY", "DAIRY", "ELECTRONICS", "CLOTHING");
    cbCatFilter.setValue("ALL");

    ComboBox<String> cbSort = new ComboBox<>();
    cbSort.getItems().addAll("Default", "Name A-Z", "Increasing Price");
    cbSort.setValue("Default");

    Button btnSearch  = new Button("Search");
    Button btnClear   = new Button("Clear");
    Button btnRefresh = new Button("Refresh");

    btnSearch.setOnAction(e -> applyInventoryFilter(
            tfSearch.getText(), cbCatFilter.getValue(), cbSort.getValue()));
    btnClear.setOnAction(e -> {
        tfSearch.clear(); cbCatFilter.setValue("ALL"); cbSort.setValue("Default");
        refreshInventory(store.getAllProducts());
    });
    btnRefresh.setOnAction(e -> refreshInventory(store.getAllProducts()));
    cbSort.setOnAction(e -> applyInventoryFilter(
            tfSearch.getText(), cbCatFilter.getValue(), cbSort.getValue()));
    cbCatFilter.setOnAction(e -> applyInventoryFilter(
            tfSearch.getText(), cbCatFilter.getValue(), cbSort.getValue()));

    invStatus = new Label("");
    invStatus.setStyle("-fx-text-fill:#27ae60;-fx-font-weight:bold;");

    HBox toolbar = new HBox(8,
        new Label("Search:"), tfSearch,
        new Label("Category:"), cbCatFilter,
        new Label("Sort:"), cbSort,
        btnSearch, btnClear, btnRefresh, invStatus
    );
    toolbar.setAlignment(Pos.CENTER_LEFT);
    toolbar.setPadding(new Insets(0, 0, 8, 0));

    invTable = new TableView<>();
    invTable.setItems(invData);

    invTable.getColumns().addAll(
        pCol("ID",       "id",       55),
        pCol("Name",     "name",    180),
        pCol("Category", "category",110),
        pCol("Price ₹",  "price",    90),
        pCol("GST %",    "gst",      70),
        pCol("Net Price","net",      95),
        pCol("Stock",    "stock",    65),
        pCol("Expiry Date", "expiry", 110),
        pCol("Status", "expiryStatus", 120)
    );

    TableColumn<PRow, Integer> stockCol =
        (TableColumn<PRow, Integer>) invTable.getColumns().get(6);
    stockCol.setCellFactory(col -> new TableCell<>() {
        @Override protected void updateItem(Integer v, boolean empty) {
            super.updateItem(v, empty);
            if (empty || v == null) { setText(null); setStyle(""); return; }
            setText(String.valueOf(v));
            if      (v == 0) setStyle("-fx-background-color:#fde8e8;-fx-text-fill:#c0392b;-fx-font-weight:bold;");
            else if (v <= 3) setStyle("-fx-background-color:#fef9e7;-fx-text-fill:#d35400;-fx-font-weight:bold;");
            else             setStyle("-fx-background-color:#eafaf1;-fx-text-fill:#1e8449;");
        }
    });
    
    TableColumn<PRow, String> statusCol =
            (TableColumn<PRow, String>) invTable.getColumns().get(8);

    statusCol.setCellFactory(col -> new TableCell<>() {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);

            if (empty || status == null) {
                setText(null);
                setStyle("");
                return;
            }

            setText(status);

            if (status.equals("Expired")) {
                setStyle("-fx-background-color:#fde8e8;-fx-text-fill:#c0392b;-fx-font-weight:bold;");
            } else if (status.equals("Expiring Soon")) {
                setStyle("-fx-background-color:#fef9e7;-fx-text-fill:#d35400;-fx-font-weight:bold;");
            } else {
                setStyle("-fx-background-color:#eafaf1;-fx-text-fill:#1e8449;-fx-font-weight:bold;");
            }
        }
    });

    TableColumn<PRow, Void> actCol = new TableColumn<>("Actions");
    actCol.setPrefWidth(170);
    actCol.setCellFactory(col -> new TableCell<>() {
        final Button btnRemove  = new Button("Remove");
        final Button btnRestock = new Button("+Stock");
        final HBox box = new HBox(5, btnRestock, btnRemove);
        {
            btnRemove.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-size:11;");
            btnRestock.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;-fx-font-size:11;");
            btnRemove.setOnAction(e -> {
                PRow row = getTableView().getItems().get(getIndex());
                Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                        "Remove \"" + row.getName() + "\"?", ButtonType.OK, ButtonType.CANCEL);
                a.setHeaderText(null);
                a.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
                    store.removeProduct(row.getId());
                    refreshInventory(store.getAllProducts());
                    invStatus.setText("Removed: " + row.getName());
                });
            });
            btnRestock.setOnAction(e -> {
                PRow row = getTableView().getItems().get(getIndex());
                TextInputDialog d = new TextInputDialog("10");
                d.setHeaderText("Restock — " + row.getName());
                d.setContentText("Quantity to add:");
                d.showAndWait().ifPresent(v -> {
                    try {
                        store.restockProduct(row.getId(), Integer.parseInt(v));
                        refreshInventory(store.getAllProducts());
                        invStatus.setText("Restocked: " + row.getName());
                    } catch (NumberFormatException ex) {
                        showError("Enter a valid number.");
                    }
                });
            });
        }
        @Override protected void updateItem(Void v, boolean empty) {
            super.updateItem(v, empty); setGraphic(empty ? null : box);
        }
    });
    invTable.getColumns().add(actCol);
    invTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    refreshInventory(store.getAllProducts());

    TextField tfName  = new TextField(); tfName.setPromptText("Product Name");
    TextField tfPrice = new TextField(); tfPrice.setPromptText("Price (Rs.)");
    TextField tfStock = new TextField(); tfStock.setPromptText("Stock");
    DatePicker dpExpiry = new DatePicker();dpExpiry.setPromptText("Expiry Date");
    ComboBox<String> cbCat = new ComboBox<>();
    cbCat.getItems().addAll("GROCERY", "DAIRY", "ELECTRONICS", "CLOTHING");
    cbCat.setValue("GROCERY");

    Button btnAdd = new Button("+ Add Product");
    btnAdd.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;-fx-font-size:13;-fx-padding:6 16;");
    btnAdd.setOnAction(e -> {
        try {
            String name  = tfName.getText().trim();
            double price = Double.parseDouble(tfPrice.getText().trim());
            int    stock = Integer.parseInt(tfStock.getText().trim());
            if (name.isEmpty()) { showError("Name cannot be empty."); return; }

            LocalDate expiry = dpExpiry.getValue();

            if (expiry == null) {
                showError("Please select expiry date.");
                return;
            }

            store.addProduct(new Product(store.getNextId(), name, price, stock, cbCat.getValue(), expiry));
            
            refreshInventory(store.getAllProducts());
            refreshProductCombo();
            tfName.clear(); tfPrice.clear(); tfStock.clear();dpExpiry.setValue(null);
            invStatus.setText("Added: " + name);
        } catch (NumberFormatException ex) {
            showError("Enter valid numbers for Price and Stock.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    });

    GridPane form = new GridPane();
    form.setHgap(10); form.setVgap(6); form.setPadding(new Insets(10, 0, 0, 0));
    form.addRow(0, new Label("Name:"), tfName, new Label("Price:"), tfPrice,
                   new Label("Stock:"), tfStock, new Label("Category:"), cbCat,new Label("Expiry:"), dpExpiry, btnAdd);

    VBox content = new VBox(8, toolbar, invTable, new Separator(), form);
    content.setPadding(new Insets(14));
    VBox.setVgrow(invTable, Priority.ALWAYS);

    tab.setContent(content);
    return tab;
}

// TAB 2 — BILLING
private Tab buildBillingTab() {
    Tab tab = new Tab("Billing");

    cbProduct = new ComboBox<>();
    cbProduct.setPromptText("Select Product");
    cbProduct.setPrefWidth(300);
    refreshProductCombo();

    tfQty = new TextField("1"); tfQty.setPrefWidth(65);

    Button btnAddCart = new Button("+ Add to Cart");
    btnAddCart.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;-fx-font-size:13;-fx-padding:6 14;");
    btnAddCart.setOnAction(e -> addToCart());

    HBox addRow = new HBox(10, new Label("Product:"), cbProduct,
                               new Label("Qty:"), tfQty, btnAddCart);
    addRow.setAlignment(Pos.CENTER_LEFT);

    cartTable = new TableView<>();
    cartTable.setItems(cartData);
    cartTable.setPrefHeight(230);

    cartTable.getColumns().addAll(
        cCol("Product",    "name",     190),
        cCol("Qty",        "qty",       50),
        cCol("Price ₹",    "price",     90),
        cCol("Subtotal ₹", "subtotal",  90),
        cCol("GST ₹",      "gst",       80),
        cCol("Total ₹",    "total",     90)
    );

    TableColumn<CRow, Void> rmCol = new TableColumn<>("");
    rmCol.setPrefWidth(70);
    rmCol.setCellFactory(col -> new TableCell<>() {
        final Button btn = new Button("Remove");
        { btn.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-size:10;");
          btn.setOnAction(e -> { store.removeFromCart(getIndex()); refreshCart(); }); }
        @Override protected void updateItem(Void v, boolean e2) {
            super.updateItem(v, e2); setGraphic(e2 ? null : btn);
        }
    });
    cartTable.getColumns().add(rmCol);
    cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

    lblSub   = new Label("Rs.0.00");
    lblGst   = new Label("Rs.0.00");
    lblTotal = new Label("Rs.0.00");
    lblTotal.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:#1a5276;");

    GridPane totals = new GridPane();
    totals.setHgap(20); totals.setVgap(5); totals.setPadding(new Insets(6, 0, 0, 0));
    totals.addRow(0, bold("Subtotal:"), lblSub);
    totals.addRow(1, bold("Total GST:"), lblGst);
    totals.addRow(2, bold("GRAND TOTAL:"), lblTotal);

    TextField tfCustomer = new TextField(); tfCustomer.setPromptText("Customer Name");
    tfCustomer.setPrefWidth(200);

    billStatus = new Label("");
    billStatus.setStyle("-fx-font-weight:bold;-fx-text-fill:#27ae60;");

    Button btnBill  = new Button("Generate Bill");
    Button btnClear = new Button("Clear Cart");
    btnBill.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;-fx-font-size:13;-fx-padding:7 18;");
    btnClear.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-size:13;-fx-padding:7 18;");

    btnBill.setOnAction(e -> {
        String customer = tfCustomer.getText().trim();
        if (customer.isEmpty()) { showError("Enter customer name."); return; }
        String receipt = store.generateBill(customer);
        if (receipt == null) { showError("Cart is empty."); return; }
        taReceipt.setText(receipt);
        refreshCart();
        refreshInventory(store.getAllProducts());
        refreshProductCombo();
        tfCustomer.clear();
        billStatus.setText("Bill generated!");
        new Alert(Alert.AlertType.INFORMATION, "Bill saved to receipts.txt", ButtonType.OK) {{
            setHeaderText("Bill generated for " + customer); showAndWait();
        }};
    });
    btnClear.setOnAction(e -> { store.clearCart(); refreshCart(); billStatus.setText("Cart cleared."); });

    HBox billRow = new HBox(10, new Label("Customer:"), tfCustomer, btnBill, btnClear, billStatus);
    billRow.setAlignment(Pos.CENTER_LEFT);

    taReceipt = new TextArea("Generate a bill to see the receipt here.");
    taReceipt.setEditable(false);
    taReceipt.setPrefHeight(180);
    taReceipt.setStyle("-fx-font-family:monospace;-fx-font-size:12;");

    VBox content = new VBox(10, addRow, cartTable, totals, new Separator(), billRow, taReceipt);
    content.setPadding(new Insets(14));
    VBox.setVgrow(cartTable, Priority.ALWAYS);
    tab.setContent(content);
    return tab;
}

// TAB 3 — RECEIPT LOG  
private Tab buildReceiptLogTab() {
    Tab tab = new Tab("Receipt Log");

    TextArea taLog = new TextArea();
    taLog.setEditable(false);
    taLog.setStyle("-fx-font-family:monospace;-fx-font-size:12;");

    Button btnLoad = new Button("Load receipts.txt  (FileReader — Week 5)");
    btnLoad.setStyle("-fx-background-color:#1a5276;-fx-text-fill:white;-fx-font-size:13;-fx-padding:7 16;");
    btnLoad.setOnAction(e -> taLog.setText(store.readReceiptLog()));

    Label note = new Label("Receipts are saved automatically using FileWriter (append mode) every time a bill is generated. "
            + "Inventory is persisted using Java Serialization (ObjectOutputStream / ObjectInputStream).");
    note.setWrapText(true);
    note.setStyle("-fx-text-fill:#555;-fx-font-style:italic;-fx-font-size:12;");

    VBox content = new VBox(10, btnLoad, note,
            new ScrollPane(taLog) {{ setFitToWidth(true); setFitToHeight(true); VBox.setVgrow(this, Priority.ALWAYS); }});
    content.setPadding(new Insets(14));
    VBox.setVgrow(content.getChildren().get(2), Priority.ALWAYS);
    tab.setContent(content);
    tab.setOnSelectionChanged(e -> { if (tab.isSelected()) taLog.setText(store.readReceiptLog()); });
    return tab;
}


private void addToCart() {
    String selected = cbProduct.getValue();
    if (selected == null) { showError("Select a product."); return; }
    try {
        int start = selected.indexOf('[') + 1;
        int end   = selected.indexOf(']');
        int id    = Integer.parseInt(selected.substring(start, end).trim());

        int qty = Integer.parseInt(tfQty.getText().trim());
        if (qty <= 0) { showError("Quantity must be at least 1."); return; }
        String err = store.addToCart(id, qty);
        if (err != null) { showError(err); return; }
        refreshCart();
        tfQty.setText("1");
    } catch (NumberFormatException ex) {
        showError("Invalid quantity.");
    }
}

private void applyInventoryFilter(String keyword, String cat, String sort) {
    List<Product> list;
    if (!keyword.trim().isEmpty())   list = store.searchByName(keyword);
    else if (!"ALL".equals(cat))     list = store.filterByCategory(cat);
    else if ("Name A-Z".equals(sort)) list = store.getAllSortedByName();
    else if ("Increasing Price ".equals(sort))  list = store.getAllSortedByPrice();
    else                              list = store.getAllProducts();
    refreshInventory(list);
}

private void refreshInventory(List<Product> list) {
    invData.clear();
    for (Product p : list) invData.add(new PRow(p));
    if (invStatus != null) invStatus.setText(list.size() + " products");
}

private void refreshCart() {
    cartData.clear();
    for (CartItem item : store.getCart()) cartData.add(new CRow(item));
    if (lblSub != null) {
        lblSub.setText(String.format("Rs.%.2f", store.getCartSubtotal()));
        lblGst.setText(String.format("Rs.%.2f", store.getCartGst()));
        lblTotal.setText(String.format("Rs.%.2f", store.getCartTotal()));
    }
}

private void refreshProductCombo() {
    if (cbProduct == null) return;
    cbProduct.getItems().clear();
    for (Product p : store.getAllProducts()) {
        if (p.getStock() > 0 && !p.isExpired())
            cbProduct.getItems().add(String.format("[%d] %s | Rs.%.2f | Stock: %d | Expiry: %s",
                    p.getId(), p.getName(), p.getPrice(), p.getStock(), p.getExpiryDate()));
    }
}

private void showError(String msg) {
    new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK) {{
        setHeaderText(null); showAndWait();
    }};
}

private Label bold(String text) {
    Label l = new Label(text); l.setStyle("-fx-font-weight:bold;"); return l;
}

private <T> TableColumn<PRow, T> pCol(String title, String prop, int w) {
    TableColumn<PRow, T> c = new TableColumn<>(title);
    c.setCellValueFactory(new PropertyValueFactory<>(prop)); c.setPrefWidth(w); return c;
}
private <T> TableColumn<CRow, T> cCol(String title, String prop, int w) {
    TableColumn<CRow, T> c = new TableColumn<>(title);
    c.setCellValueFactory(new PropertyValueFactory<>(prop)); c.setPrefWidth(w); return c;
}

public static void main(String[] args) { launch(args); }



public static class PRow {
    private final SimpleIntegerProperty id, stock;
    private final SimpleStringProperty name, category, expiry, expiryStatus;
    private final SimpleDoubleProperty price, gst, net;

    public PRow(Product p) {
        id = new SimpleIntegerProperty(p.getId());
        name = new SimpleStringProperty(p.getName());
        category = new SimpleStringProperty(p.getCategory());
        price = new SimpleDoubleProperty(Math.round(p.getPrice() * 100.0) / 100.0);
        gst = new SimpleDoubleProperty(p.getGstRate());
        net = new SimpleDoubleProperty(Math.round(p.getPriceWithGst() * 100.0) / 100.0);
        stock = new SimpleIntegerProperty(p.getStock());

        expiry = new SimpleStringProperty(p.getExpiryDate().toString());

        if (p.isExpired()) {
            expiryStatus = new SimpleStringProperty("Expired");
        } else if (p.isExpiringSoon()) {
            expiryStatus = new SimpleStringProperty("Expiring Soon");
        } else {
            expiryStatus = new SimpleStringProperty("Safe");
        }
    }

    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getCategory() { return category.get(); }
    public double getPrice() { return price.get(); }
    public double getGst() { return gst.get(); }
    public double getNet() { return net.get(); }
    public int getStock() { return stock.get(); }
    public String getExpiry() { return expiry.get(); }
    public String getExpiryStatus() { return expiryStatus.get(); }
}

public static class CRow {
    private final SimpleStringProperty  name;
    private final SimpleIntegerProperty qty;
    private final SimpleDoubleProperty  price, subtotal, gst, total;

    public CRow(CartItem i) {
        name     = new SimpleStringProperty(i.getProduct().getName());
        qty      = new SimpleIntegerProperty(i.getQuantity());
        price    = new SimpleDoubleProperty(Math.round(i.getProduct().getPrice() * 100.0) / 100.0);
        subtotal = new SimpleDoubleProperty(Math.round(i.getSubtotal() * 100.0) / 100.0);
        gst      = new SimpleDoubleProperty(Math.round(i.getGstAmount() * 100.0) / 100.0);
        total    = new SimpleDoubleProperty(Math.round(i.getTotal() * 100.0) / 100.0);
    }
    public String getName()      { return name.get(); }
    public int    getQty()       { return qty.get(); }
    public double getPrice()     { return price.get(); }
    public double getSubtotal()  { return subtotal.get(); }
    public double getGst()       { return gst.get(); }
    public double getTotal()     { return total.get(); }
}
}