import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PBL: Bank Management System
 * GUI: Java Swing
 * Storage: persistent local file (bank_accounts.dat)
 *
 * Compile: javac BankManagementSystem.java
 * Run:     java BankManagementSystem
 */
public class BankManagementSystem extends JFrame {
    private static final String DATA_FILE = "bank_accounts.dat";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, BankAccount> accounts = new LinkedHashMap<String, BankAccount>();
    private final JTextField nameField = new JTextField(18);
    private final JTextField accountField = new JTextField(18);
    private final JTextField amountField = new JTextField(12);
    private final JTextField searchField = new JTextField(12);
    private final JLabel balanceLabel = new JLabel("Balance: ₹0.00");
    private final JLabel statusLabel = new JLabel("Ready.");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Account No.", "Account Holder", "Balance (₹)"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable accountTable = new JTable(tableModel);
    private final JTextArea historyArea = new JTextArea(8, 40);

    public BankManagementSystem() {
        super("Bank Management System - Java Swing PBL");
        loadData();
        buildUi();
        refreshTable();
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                saveData();
                dispose();
            }
        });
        setSize(980, 690);
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        JLabel heading = new JLabel("BANK MANAGEMENT SYSTEM", SwingConstants.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 24));
        heading.setForeground(new Color(25, 65, 110));
        root.add(heading, BorderLayout.NORTH);

        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setPreferredSize(new Dimension(390, 500));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Account Operations"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        addFormRow(form, gbc, 0, "Account holder:", nameField);
        addFormRow(form, gbc, 1, "Account number:", accountField);
        addFormRow(form, gbc, 2, "Amount (₹):", amountField);

        JPanel buttons = new JPanel(new GridLayout(0, 2, 6, 6));
        JButton createButton = new JButton("Create Account");
        JButton depositButton = new JButton("Deposit");
        JButton withdrawButton = new JButton("Withdraw");
        JButton balanceButton = new JButton("Check Balance");
        JButton detailsButton = new JButton("Account Details");
        JButton historyButton = new JButton("Transaction History");
        JButton clearButton = new JButton("Clear Fields");
        JButton saveButton = new JButton("Save Data");
        buttons.add(createButton);
        buttons.add(depositButton);
        buttons.add(withdrawButton);
        buttons.add(balanceButton);
        buttons.add(detailsButton);
        buttons.add(historyButton);
        buttons.add(clearButton);
        buttons.add(saveButton);

        JPanel formContainer = new JPanel(new BorderLayout(5, 8));
        formContainer.add(form, BorderLayout.NORTH);
        formContainer.add(buttons, BorderLayout.CENTER);
        left.add(formContainer, BorderLayout.NORTH);

        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("Transaction / Account Messages"));
        historyArea.setEditable(false);
        historyArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);
        historyPanel.add(new JScrollPane(historyArea), BorderLayout.CENTER);
        left.add(historyPanel, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(BorderFactory.createTitledBorder("Saved Bank Accounts"));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Find account:"));
        searchPanel.add(searchField);
        JButton searchButton = new JButton("Find");
        JButton showAllButton = new JButton("Show All");
        searchPanel.add(searchButton);
        searchPanel.add(showAllButton);
        right.add(searchPanel, BorderLayout.NORTH);
        accountTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountTable.setRowHeight(25);
        right.add(new JScrollPane(accountTable), BorderLayout.CENTER);
        balanceLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        balanceLabel.setForeground(new Color(20, 100, 65));
        right.add(balanceLabel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.42);
        root.add(split, BorderLayout.CENTER);

        statusLabel.setBorder(new EmptyBorder(4, 2, 0, 2));
        root.add(statusLabel, BorderLayout.SOUTH);

        createButton.addActionListener(e -> createAccount());
        depositButton.addActionListener(e -> performTransaction(true));
        withdrawButton.addActionListener(e -> performTransaction(false));
        balanceButton.addActionListener(e -> checkBalance());
        detailsButton.addActionListener(e -> showDetails());
        historyButton.addActionListener(e -> showHistory());
        clearButton.addActionListener(e -> clearFields());
        saveButton.addActionListener(e -> {
            if (saveData()) showMessage("Account data saved to " + DATA_FILE);
        });
        searchButton.addActionListener(e -> searchAccount());
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });
        accountTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) copySelectedAccountToFields();
        });
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row,
                            String label, JTextField field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
    }

    private void createAccount() {
        String name = nameField.getText().trim();
        String number = accountField.getText().trim();
        if (name.isEmpty() || number.isEmpty()) {
            showError("Enter both account holder name and account number.");
            return;
        }
        if (!number.matches("[A-Za-z0-9-]{3,20}")) {
            showError("Account number must be 3-20 characters (letters, digits, or hyphen).");
            return;
        }
        if (accounts.containsKey(number)) {
            showError("That account number already exists.");
            return;
        }
        BankAccount account = new BankAccount(number, name);
        account.addTransaction("ACCOUNT CREATED", BigDecimal.ZERO);
        accounts.put(number, account);
        saveData();
        refreshTable();
        amountField.setText("");
        log("Created account " + number + " for " + name + ".");
        statusLabel.setText("Account created successfully.");
    }

    private void performTransaction(boolean deposit) {
        BankAccount account = getSelectedOrEnteredAccount();
        if (account == null) return;
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountField.getText().trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            showError("Enter a valid numeric amount, for example 500 or 1250.50.");
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            showError("Amount must be greater than zero.");
            return;
        }

        if (deposit) {
            account.setBalance(account.getBalance().add(amount));
            account.addTransaction("DEPOSIT", amount);
            log("Deposited ₹" + money(amount) + " into account " + account.getAccountNumber() + ".");
            statusLabel.setText("Deposit successful.");
        } else {
            if (account.getBalance().compareTo(amount) < 0) {
                showError("Insufficient balance. Withdrawal was not completed.");
                return;
            }
            account.setBalance(account.getBalance().subtract(amount));
            account.addTransaction("WITHDRAWAL", amount);
            log("Withdrew ₹" + money(amount) + " from account " + account.getAccountNumber() + ".");
            statusLabel.setText("Withdrawal successful.");
        }
        saveData();
        refreshTable();
        balanceLabel.setText("Balance: ₹" + money(account.getBalance()));
        amountField.setText("");
    }

    private void checkBalance() {
        BankAccount account = getSelectedOrEnteredAccount();
        if (account == null) return;
        balanceLabel.setText("Balance: ₹" + money(account.getBalance()));
        log("Balance for " + account.getAccountNumber() + ": ₹" + money(account.getBalance()));
        statusLabel.setText("Balance displayed.");
    }

    private void showDetails() {
        BankAccount account = getSelectedOrEnteredAccount();
        if (account == null) return;
        String details = "Account Number: " + account.getAccountNumber()
                + "\nAccount Holder: " + account.getHolderName()
                + "\nCurrent Balance: ₹" + money(account.getBalance())
                + "\nTransactions: " + account.getTransactions().size();
        JOptionPane.showMessageDialog(this, details, "Account Details", JOptionPane.INFORMATION_MESSAGE);
        log("Displayed account details for " + account.getAccountNumber() + ".");
    }

    private void showHistory() {
        BankAccount account = getSelectedOrEnteredAccount();
        if (account == null) return;
        StringBuilder sb = new StringBuilder("Transaction history for ")
                .append(account.getAccountNumber()).append(" (")
                .append(account.getHolderName()).append(")\n\n");
        for (Transaction tx : account.getTransactions()) {
            sb.append(tx.getTimestamp()).append(" | ")
              .append(tx.getType()).append(" | ₹")
              .append(money(tx.getAmount())).append("\n");
        }
        JTextArea text = new JTextArea(sb.toString(), 18, 60);
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(text), "Transaction History",
                JOptionPane.INFORMATION_MESSAGE);
        log("Displayed transaction history for " + account.getAccountNumber() + ".");
    }

    private void searchAccount() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            refreshTable();
            return;
        }
        tableModel.setRowCount(0);
        for (BankAccount a : accounts.values()) {
            if (a.getAccountNumber().toLowerCase().contains(query)
                    || a.getHolderName().toLowerCase().contains(query)) {
                tableModel.addRow(new Object[]{a.getAccountNumber(), a.getHolderName(), money(a.getBalance())});
            }
        }
        statusLabel.setText(tableModel.getRowCount() + " matching account(s).");
    }

    private BankAccount getSelectedOrEnteredAccount() {
        String number = accountField.getText().trim();
        int selectedRow = accountTable.getSelectedRow();
        if (selectedRow >= 0) {
            number = String.valueOf(tableModel.getValueAt(selectedRow, 0));
        }
        if (number.isEmpty()) {
            showError("Select an account from the table or enter its account number.");
            return null;
        }
        BankAccount account = accounts.get(number);
        if (account == null) {
            showError("Account not found. Create the account first or select it from the table.");
            return null;
        }
        accountField.setText(account.getAccountNumber());
        nameField.setText(account.getHolderName());
        return account;
    }

    private void copySelectedAccountToFields() {
        int row = accountTable.getSelectedRow();
        if (row < 0) return;
        String number = String.valueOf(tableModel.getValueAt(row, 0));
        BankAccount account = accounts.get(number);
        if (account != null) {
            accountField.setText(account.getAccountNumber());
            nameField.setText(account.getHolderName());
            balanceLabel.setText("Balance: ₹" + money(account.getBalance()));
        }
    }

    private void clearFields() {
        nameField.setText("");
        accountField.setText("");
        amountField.setText("");
        accountTable.clearSelection();
        balanceLabel.setText("Balance: ₹0.00");
        statusLabel.setText("Fields cleared.");
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (BankAccount a : accounts.values()) {
            tableModel.addRow(new Object[]{a.getAccountNumber(), a.getHolderName(), money(a.getBalance())});
        }
        statusLabel.setText("Loaded " + accounts.size() + " account(s). Data file: " + DATA_FILE);
    }

    private void log(String message) {
        historyArea.append("[" + LocalDateTime.now().format(TIME_FORMAT) + "] " + message + "\n");
        historyArea.setCaretPosition(historyArea.getDocument().getLength());
    }

    private boolean saveData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(new ArrayList<BankAccount>(accounts.values()));
            return true;
        } catch (IOException ex) {
            showError("Could not save data file:\n" + ex.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            Object data = in.readObject();
            if (data instanceof List<?>) {
                for (Object item : (List<?>) data) {
                    if (item instanceof BankAccount) {
                        BankAccount account = (BankAccount) item;
                        accounts.put(account.getAccountNumber(), account);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(null,
                    "Could not load saved bank data. A new in-memory session will start.\n"
                            + "File: " + DATA_FILE + "\nReason: " + ex.getMessage(),
                    "Data Load Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
        statusLabel.setText(message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input / Operation Error", JOptionPane.ERROR_MESSAGE);
        statusLabel.setText(message.replace('\n', ' '));
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankManagementSystem().setVisible(true));
    }
}

class BankAccount implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String accountNumber;
    private final String holderName;
    private BigDecimal balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private final List<Transaction> transactions = new ArrayList<Transaction>();

    BankAccount(String accountNumber, String holderName) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
    }

    String getAccountNumber() { return accountNumber; }
    String getHolderName() { return holderName; }
    BigDecimal getBalance() { return balance; }
    void setBalance(BigDecimal balance) { this.balance = balance.setScale(2, RoundingMode.HALF_UP); }
    List<Transaction> getTransactions() { return transactions; }

    void addTransaction(String type, BigDecimal amount) {
        transactions.add(new Transaction(type, amount));
    }
}

class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String type;
    private final BigDecimal amount;
    private final String timestamp;

    Transaction(String type, BigDecimal amount) {
        this.type = type;
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    String getType() { return type; }
    BigDecimal getAmount() { return amount; }
    String getTimestamp() { return timestamp; }
}
