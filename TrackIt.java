package expensetracker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class TrackIt extends JFrame{
	//Components
	private JTable transactionTable;
	private JLabel totalLabel, topCatLabel;
	private JComboBox<String> monthSelector;
	private JButton setLimitsButton, summaryButton;

	private ArrayList<String[]> transactions = new ArrayList<>();
	private Map<String, Double> categoryLimits = new HashMap<>();
	
	//constructor for title in frame
	public TrackIt() {
		setTitle("TrackIt - Expense Tracker");
		setSize(800, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null); // to control pos of window and doesn't make it visible
		
		//Top Panel
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // creates panel and aligns components to left
		monthSelector = new JComboBox<>(new String[] {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"});
		
		summaryButton = new JButton("View Summary");
		setLimitsButton = new JButton("Set Limits");
		
		topPanel.add(new JLabel("select Month:"));
		topPanel.add(monthSelector);
		topPanel.add(summaryButton);
		topPanel.add(setLimitsButton);
		add(topPanel, BorderLayout.NORTH); // added to top of the frame
		
		//Center Panel - Table
		String[] columns = {"Date", "Description", "Amount(Rs.)", "Category"}; // column names
		transactionTable = new JTable(new Object[0][columns.length], columns); // table with 4 columns
		add(new JScrollPane(transactionTable), BorderLayout.CENTER); // added to center of the frame & as a scrollable area
		
		//Bottom Panel
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		totalLabel = new JLabel("Total: Rs.0.00");
		topCatLabel = new JLabel("Top Category: None");
		bottomPanel.add(topCatLabel);
		bottomPanel.add(totalLabel);
		add(bottomPanel, BorderLayout.SOUTH);
		
		//Event - Load CSV when month changes
		monthSelector.addActionListener(e -> {
			String month = (String) monthSelector.getSelectedItem();
			loadMonthData(month);
		});
		
		summaryButton.addActionListener(e -> openSummaryWindow());
		
		setLimitsButton.addActionListener(e -> openSetLimitsDialog());
		
		setVisible(true);
	}
	
	// Load & categorize transactions from CSV based on month
	private void loadMonthData(String month) {
		String filePath = "data/" + month + ".csv";
		transactions.clear();
		categoryLimits.clear();
		
		File csvFile = new File(filePath);
		if(!csvFile.exists()) {
			JOptionPane.showMessageDialog(this, "No data found for " + month + ".");
			refreshTable();
			totalLabel.setText("Total: Rs.0.00");
			topCatLabel.setText("Top Category: None");
			return;
		}
		
		try(BufferedReader br = new BufferedReader(new FileReader(csvFile))){
			String line;
			while((line = br.readLine()) != null) {
				String[] data = line.split(",");
				if(data.length < 3) continue;
				
				String date = data[0].trim();
				String desc = data[1].trim().toLowerCase();
				String amount = data[2].trim();
				String category = categorize(desc);
				
				transactions.add(new String[] {date, data[1].trim(), amount, category});
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Error reading file: " +ex.getMessage());
		}
		
		//Load category limits
		loadCategoryLimits(month);
		
		//Refresh table & calculate summary
		refreshTable();
		calculateSummary();
	}
	
	//Automatically categorize the data
	private String categorize(String desc) {
		if(desc.contains("swiggy") || desc.contains("zomato") || desc.contains("dominos"))
			return "Food";
		else if (desc.contains("rapido") || desc.contains("ola") || desc.contains("uber"))
			return "Transport";
		else if (desc.contains("bill") || desc.contains("recharge") || desc.contains("electricity") || desc.contains("jio") || desc.contains("airtel"))
            return "Bills & Utilities";
        else if (desc.contains("rent") || desc.contains("room"))
            return "Rent/Housing";
        else if (desc.contains("amazon") || desc.contains("myntra"))
            return "Shopping";
        else if (desc.contains("netflix") || desc.contains("prime") || desc.contains("hotstar"))
            return "Entertainment";
        else if (desc.contains("school") || desc.contains("fee") || desc.contains("course"))
            return "Education";
        else if (desc.contains("loan") || desc.contains("emi"))
            return "Loans & EMIs";
        else if (desc.contains("insurance"))
            return "Insurance";
        else if (desc.contains("trip") || desc.contains("travel") || desc.contains("vacation"))
            return "Vacation";
        else
            return "Others";
	}
	
	// Refresh table with new data
	private void refreshTable() {
		String[] columns = {"Date", "Description", "Amount(Rs.)", "Category"};
		Object[][] data = new Object[transactions.size()][4];
		
		for(int i = 0; i < transactions.size(); i++) {
			data[i] = transactions.get(i);
		}
		
		transactionTable.setModel(new javax.swing.table.DefaultTableModel(data, columns));
	}
	
	private void loadCategoryLimits(String month) {
		File file = new File("data/limits.csv");
		if(!file.exists()) return;
		
		try(BufferedReader br = new BufferedReader(new FileReader(file))){
			String line;
			br.readLine();
			while((line = br.readLine()) != null) {
				String[] parts = line.split(",");
				if(parts.length < 3) continue;
				if(!parts[0].trim().equalsIgnoreCase(month)) continue;
				
				String category = parts[1].trim();
				double limit = Double.parseDouble(parts[2].trim());
				categoryLimits.put(category, limit);
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Error reading limits.csv: " + ex.getMessage());
		}
	}
	
	//Calculate summary
	private void calculateSummary() {
		double total = 0;
		HashMap<String, Double> categoryTotals = new HashMap<>();
		
		for(String[] t : transactions) {
			double amt = Double.parseDouble(t[2]);
			total += amt;
			categoryTotals.put(t[3], categoryTotals.getOrDefault(t[3], 0.0) + amt);
		}
		
		//Find top category
		String topCategory = "None";
		double max = 0;
		for(var entry : categoryTotals.entrySet()) {
			if (entry.getValue() > max) {
				max = entry.getValue();
				topCategory = entry.getKey();
			}
		}
		
		totalLabel.setText("Total: Rs." + String.format("%.2f", total));
		topCatLabel.setText("Top Category: " + topCategory);
		
		//Check limits
		for(var entry : categoryTotals.entrySet()) {
			String cat = entry.getKey();
			double spent = entry.getValue();
			if(categoryLimits.containsKey(cat)) {
				double limit = categoryLimits.get(cat);
				if(spent > limit) {
					JOptionPane.showMessageDialog(this, cat + "exceeded its limit(" + limit + ") by Rs." + String.format("%.2f", spent - limit));
				}
			}
		}
	}
	
	// Summary Window
	private void openSummaryWindow() {
	    JDialog summaryDialog = new JDialog(this, "Monthly Summary", true);
	    summaryDialog.setSize(500, 600);
	    summaryDialog.setLayout(new BorderLayout());
	    summaryDialog.setLocationRelativeTo(this);

	    JTextArea summaryArea = new JTextArea();
	    summaryArea.setEditable(false);
	    summaryArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));

	    StringBuilder sb = new StringBuilder();

	    sb.append("===== Monthly Summary =====\n\n");

	    // Total Spent
	    double total = 0;
	    Map<String, Double> catTotals = new HashMap<>();

	    for (String[] t : transactions) {
	        double amt = Double.parseDouble(t[2]);
	        total += amt;
	        catTotals.put(t[3], catTotals.getOrDefault(t[3], 0.0) + amt);
	    }

	    sb.append("Total Spent: ₹").append(String.format("%.2f", total)).append("\n\n");

	    // Category-wise spending
	    sb.append("Category Breakdown:\n");
	    for (String cat : catTotals.keySet()) {
	        sb.append(" - ").append(cat)
	          .append(": ₹").append(String.format("%.2f", catTotals.get(cat)));

	        // If limit exists, show limit & remaining
	        if (categoryLimits.containsKey(cat)) {
	            double limit = categoryLimits.get(cat);
	            double spent = catTotals.get(cat);
	            sb.append(" (Limit: ₹").append(limit).append(")");

	            if (spent > limit) {
	                sb.append("  **Exceeded by ₹")
	                  .append(String.format("%.2f", spent - limit))
	                  .append("**");
	            } else {
	                sb.append("  | Remaining: ₹")
	                  .append(String.format("%.2f", limit - spent));
	            }
	        }

	        sb.append("\n");
	    }

	    // Top category
	    String topCat = "None";
	    double max = 0;

	    for (var e : catTotals.entrySet()) {
	        if (e.getValue() > max) {
	            max = e.getValue();
	            topCat = e.getKey();
	        }
	    }

	    sb.append("\nTop Category: ").append(topCat)
	      .append(" (₹").append(String.format("%.2f", max)).append(")\n");

	    summaryArea.setText(sb.toString());

	    summaryDialog.add(new JScrollPane(summaryArea), BorderLayout.CENTER);

	    summaryDialog.setVisible(true);
	}

	
	//Open dialog to set limits
	private void openSetLimitsDialog() {
		JDialog dialog = new JDialog(this, "Set Category Limit", true);
        dialog.setSize(400, 250);
        dialog.setLayout(new GridLayout(5, 2, 10, 10));
        dialog.setLocationRelativeTo(this);

        JComboBox<String> monthBox = new JComboBox<>(new String[]{
            "January","February","March","April","May","June",
            "July","August","September","October","November","December"
        });
        JComboBox<String> categoryBox = new JComboBox<>(new String[]{
            "Food","Transport","Bills & Utilities","Rent/Housing","Shopping",
            "Entertainment","Education","Loans & EMIs","Insurance","Vacation","Others"
        });
        JTextField limitField = new JTextField();

        dialog.add(new JLabel("Select Month:"));
        dialog.add(monthBox);
        dialog.add(new JLabel("Select Category:"));
        dialog.add(categoryBox);
        dialog.add(new JLabel("Set Limit (₹):"));
        dialog.add(limitField);

        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        dialog.add(saveBtn);
        dialog.add(cancelBtn);

        saveBtn.addActionListener(e -> {
            String month = (String) monthBox.getSelectedItem();
            String category = (String) categoryBox.getSelectedItem();
            double limit;
            try {
                limit = Double.parseDouble(limitField.getText().trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Enter a valid number for limit!");
                return;
            }
            saveLimit(month, category, limit);
            dialog.dispose();
            if (month.equals(monthSelector.getSelectedItem()))
                loadMonthData(month); // reload if current month
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    // Save/update limit in limits.csv
    private void saveLimit(String month, String category, double limit) {
        File file = new File("data/limits.csv");
        ArrayList<String> lines = new ArrayList<>();
        boolean updated = false;

        // read existing lines
        try {
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String header = br.readLine();
                lines.add(header != null ? header : "Month,Category,Limit");

                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length < 3) continue;
                    if (parts[0].equalsIgnoreCase(month) && parts[1].equalsIgnoreCase(category)) {
                        lines.add(month + "," + category + "," + limit);
                        updated = true;
                    } else {
                        lines.add(line);
                    }
                }
                br.close();
            } else {
                lines.add("Month,Category,Limit");
            }

            if (!updated) {
                lines.add(month + "," + category + "," + limit);
            }

            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for (String l : lines) {
                bw.write(l);
                bw.newLine();
            }
            bw.close();
            JOptionPane.showMessageDialog(this, "Limit saved successfully!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving limit: " + ex.getMessage());
        }
    }

	
	//Main
	public static void main(String[] args) {
		SwingUtilities.invokeLater(TrackIt::new);
	}
}