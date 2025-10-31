import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;

import org.json.JSONObject;

public class CryptoPortfolioGUI extends JFrame {

    private static final String BASE_URL = "https://api.coinbase.com/v2/prices";

    private static class Cryptocurrency {
        String symbol;
        String pair; 
        double quantity;
        double price;
        double marketValue;

        Cryptocurrency(String symbol, String pair, double quantity) {
            this.symbol = symbol;
            this.pair = pair;
            this.quantity = quantity;
            this.price = 0.0;
            this.marketValue = 0.0;
        }
    }

    private static class Portfolio {

        Map<String, Cryptocurrency> assets = new HashMap<>();

        public void addAsset(Cryptocurrency asset) {
            assets.put(asset.symbol.toUpperCase(), asset);
        }

        public Cryptocurrency getAsset(String symbol) {
            return assets.get(symbol.toUpperCase());
        }
    }

   
    private final Portfolio portfolio = new Portfolio();
    private final DefaultTableModel tableModel;
    private final JTable portfolioTable;
    private final JLabel totalValueLabel;
    private final JTextField symbolField, pairField, quantityField;

    public CryptoPortfolioGUI() {
        super("Crypto Portfolio Analyzer");

 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650); 
        setLayout(new BorderLayout(10, 10));

        String[] columnNames = {"Symbol", "Pair", "Quantity", "Price (USD)", "Market Value"};
        tableModel = new DefaultTableModel(columnNames, 0);
        portfolioTable = new JTable(tableModel);
        portfolioTable.setFillsViewportHeight(true); 
        portfolioTable.setRowHeight(25); 

        JTableHeader header = portfolioTable.getTableHeader();
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));

        JScrollPane scrollPane = new JScrollPane(portfolioTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10), 
            BorderFactory.createLineBorder(Color.GRAY) 
        ));
        add(scrollPane, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setBorder(new EmptyBorder(10, 10, 0, 10));
        totalValueLabel = new JLabel("Total Portfolio Value: $0.00");
        totalValueLabel.setFont(new Font("Arial", Font.BOLD, 18)); 
        topPanel.add(totalValueLabel);
        add(topPanel, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridLayout(2, 4, 10, 10)); 
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Manage Assets"),
            new EmptyBorder(10, 10, 10, 10) 
        ));
        
        symbolField = new JTextField("BTC");
        pairField = new JTextField("BTC-USD");
        quantityField = new JTextField("0.5");
        JButton addButton = new JButton("Add Asset");
        JButton sellButton = new JButton("Sell Asset");
        JButton refreshButton = new JButton("Refresh Prices");

        Font buttonFont = new Font("Arial", Font.PLAIN, 14);
        addButton.setFont(buttonFont);
        sellButton.setFont(buttonFont);
        refreshButton.setFont(buttonFont);

        inputPanel.add(new JLabel("Symbol:"));
        inputPanel.add(new JLabel("Trading Pair (e.g., BTC-USD):"));
        inputPanel.add(new JLabel("Quantity:"));
        inputPanel.add(new JLabel(""));
        inputPanel.add(symbolField);
        inputPanel.add(pairField);
        inputPanel.add(quantityField);
        inputPanel.add(addButton);
        
        JPanel southWrapper = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 10)); 
        buttonPanel.setBorder(new EmptyBorder(0, 10, 10, 10)); 
        buttonPanel.add(sellButton);
        buttonPanel.add(refreshButton);

        southWrapper.add(inputPanel, BorderLayout.CENTER);
        southWrapper.add(buttonPanel, BorderLayout.SOUTH);
        add(southWrapper, BorderLayout.SOUTH);

        addButton.addActionListener(this::addAssetAction);
        sellButton.addActionListener(this::sellAssetAction);
        refreshButton.addActionListener(this::refreshPricesAction);

        portfolio.addAsset(new Cryptocurrency("BTC", "BTC-USD", 0.5));
        portfolio.addAsset(new Cryptocurrency("ETH", "ETH-USD", 10));
        portfolio.addAsset(new Cryptocurrency("SOL", "SOL-USD", 50));
        portfolio.addAsset(new Cryptocurrency("DOGE", "DOGE-USD", 1000));
        portfolio.addAsset(new Cryptocurrency("XRP", "XRP-USD", 200)); 
        updateTable();

        setLocationRelativeTo(null);
    }

    private void addAssetAction(ActionEvent e) {
        try {
            String symbol = symbolField.getText().trim().toUpperCase();
            String pair = pairField.getText().trim().toUpperCase();
            double quantity = Double.parseDouble(quantityField.getText().trim());

            if (symbol.isEmpty() || pair.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Symbol and Trading Pair cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be positive.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Cryptocurrency existingAsset = portfolio.getAsset(symbol);
            if (existingAsset != null) {
                existingAsset.quantity += quantity;
            } else {

                portfolio.addAsset(new Cryptocurrency(symbol, pair, quantity));
            }
            updateTable();
            symbolField.setText("");
            pairField.setText("");
            quantityField.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for quantity.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sellAssetAction(ActionEvent e) {
        try {
            String symbol = JOptionPane.showInputDialog(this, "Enter symbol to sell:").trim().toUpperCase();
            if (symbol == null || symbol.isEmpty()) return; 

            String quantityStr = JOptionPane.showInputDialog(this, "Enter quantity to sell for " + symbol + ":");
            if (quantityStr == null || quantityStr.isEmpty()) return; 
            double quantityToSell = Double.parseDouble(quantityStr.trim());

            if (quantityToSell <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity to sell must be positive.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Cryptocurrency asset = portfolio.getAsset(symbol);
            if (asset == null) {
                JOptionPane.showMessageDialog(this, "Asset not found in portfolio.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (asset.quantity < quantityToSell) {
                JOptionPane.showMessageDialog(this, "Insufficient quantity to sell. You own " + asset.quantity + ".", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            asset.quantity -= quantityToSell;
            if (asset.quantity == 0) {
                portfolio.assets.remove(symbol);
            }
            updateTable();
            JOptionPane.showMessageDialog(this, quantityToSell + " " + symbol + " sold successfully.", "Sale Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for quantity.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "An unexpected error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshPricesAction(ActionEvent e) {

        new PriceRefreshWorker().execute();
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        double totalValue = 0;
        for (Cryptocurrency asset : portfolio.assets.values()) {
            totalValue += asset.marketValue;
            tableModel.addRow(new Object[]{
                asset.symbol,
                asset.pair,
                String.format("%.6f", asset.quantity),
                String.format("$%,.2f", asset.price),
                String.format("$%,.2f", asset.marketValue)
            });
        }
        totalValueLabel.setText(String.format("Total Portfolio Value: $%,.2f", totalValue));
        portfolioTable.repaint();
        portfolioTable.revalidate();
    }

    private class PriceRefreshWorker extends SwingWorker<Map<String, Double>, Void> {
        private final HttpClient httpClient = HttpClient.newHttpClient();

        @Override
        protected Map<String, Double> doInBackground() throws Exception {
            Map<String, Double> newPrices = new HashMap<>();
            for (Cryptocurrency asset : portfolio.assets.values()) {
                try {
                    String url = String.format("%s/%s/spot", BASE_URL, asset.pair.toUpperCase());
                    HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        if (json.has("data") && json.getJSONObject("data").has("amount")) {
                            double price = json.getJSONObject("data").getDouble("amount");
                            newPrices.put(asset.symbol, price);
                        } else {
                            System.err.println("ERROR: 'data.amount' field not found in JSON response for " + asset.symbol);
                        }
                    } else {
                        System.err.println("ERROR: API returned non-200 status code for " + asset.symbol + ": " + response.statusCode());
                    }
                } catch (Exception e) {
                    System.err.println("ERROR: Exception while fetching price for " + asset.symbol + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            }
            return newPrices;
        }

        @Override
        protected void done() {
            try{
                Map<String, Double> newPrices = get();
                for (Map.Entry<String, Double> entry : newPrices.entrySet()) {
                    Cryptocurrency asset = portfolio.getAsset(entry.getKey());
                    if (asset != null) {
                        asset.price = entry.getValue();
                        asset.marketValue = asset.quantity * asset.price;
                    }
                }
            }
        }
private void updateTable() {
    tableModel.setRowCount(0);
    double totalValue = 0;
    for (Cryptocurrency asset : portfolio.assets.values()) {
        totalValue += asset.marketValue;
        tableModel.addRow(new Object[]{
            asset.symbol,
            asset.pair,
            String.format("%.6f", asset.quantity),
            String.format("$%,.2f", asset.price),
            String.format("$%,.2f", asset.marketValue)
        });
    }
    totalValueLabel.setText(String.format("Total Portfolio Value: $%,.2f", totalValue));
}

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {

        }


        SwingUtilities.invokeLater(() -> {
            new CryptoPortfolioGUI().setVisible(true);
        });
    }
}