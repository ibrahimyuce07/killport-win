import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class PortKillerGUI extends JFrame {
    private static final int PORT_COLUMN_INDEX = 2;
    private static final int PID_COLUMN_INDEX = 4;

    public PortKillerGUI() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Port Killer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(600, 400));
        setLocationRelativeTo(null);

        // Table initialization
        var model = new DefaultTableModel(new Object[][]{}, new String[]{"Protocol", "Local Address", "Port", "State", "PID"});


        var table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

        // Button panel initialization
        var buttonPanel = new JPanel();

        // Kill button initialization
        buttonPanel.add(createKillButton(model, table));

        // Refresh button initialization
        buttonPanel.add(createRefreshButton(model));

        // Add main panel to frame
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Search panel initialization
        getContentPane().add(createSearchPanel(table), BorderLayout.NORTH);

        refreshTable(model);
        setVisible(true);
    }

    private JButton createKillButton(DefaultTableModel model, JTable table) {
        var killButton = new JButton("Kill");
        killButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                int pid = Integer.parseInt((String) model.getValueAt(selectedRow, PID_COLUMN_INDEX));
                killPort(pid);
                refreshTable(model);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a port to kill!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        return killButton;
    }

    private JButton createRefreshButton(DefaultTableModel model) {
        var refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshTable(model));
        return refreshButton;
    }

    private JPanel createSearchPanel(JTable table) {
        var searchPanel = new JPanel();
        var searchField = new JTextField(10);
        searchPanel.add(searchField);
        var searchButton = new JButton("Search");
        searchButton.addActionListener(e -> {
            String searchText = searchField.getText();
            if (!searchText.isEmpty()) {
                for (int row = 0; row < table.getRowCount(); row++) {
                    String port = (String) table.getValueAt(row, PORT_COLUMN_INDEX);
                    if (port.equals(searchText)) {
                        table.getSelectionModel().setSelectionInterval(row, row);
                        table.scrollRectToVisible(table.getCellRect(row, 0, true));
                        break;
                    }
                }
            }
        });
        searchPanel.add(searchButton);
        return searchPanel;
    }

    private void refreshTable(DefaultTableModel model) {
        System.out.println("Refreshing table...");
        model.setRowCount(0); // Clear the table
        List<PortModel> openPorts = PortKillerGUI.getNetstatPorts(); // Get the open ports
        System.out.println("Found " + openPorts.size() + " open ports.");
        for (PortModel portInfo : openPorts) {
            model.addRow(new Object[]{
                    portInfo.protocol(),
                    portInfo.localAddress(),
                    portInfo.port(),
                    portInfo.state(),
                    portInfo.pid()
            });
        }
    }

    private void killPort(int pid) {
        try {
            System.out.println("Killing port with PID: " + pid);
            String[] command = {"taskkill", "/F", "/PID", String.valueOf(pid)};
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "An error occurred while killing the selected port.", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        System.out.println("Port killed.");
    }

    public static List<PortModel> getNetstatPorts() {
        List<PortModel> portsList = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("netstat -a -n -o");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            Pattern pattern = Pattern.compile("\\s*(TCP|UDP)\\s+(\\S+):(\\d+)\\s+(\\S+):(\\d+)\\s+(\\S+)\\s+(\\d+)\\s*");
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    String protocol = matcher.group(1);
                    String localAddress = matcher.group(2);
                    String localPort = matcher.group(3);
                    String state = matcher.group(6);
                    String pid = matcher.group(7);
                    PortModel portInfo = new PortModel(protocol, localAddress, localPort, state, pid);
                    if (state.contains("ESTABLISHED")) {
                        portsList.add(portInfo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return portsList;
    }


}
