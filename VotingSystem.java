import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class VotingSystem {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/VotingSystem";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    private JFrame frame;
    private JTextField voterIdField;
    private JTextField nameField;
    private JComboBox<String> candidateBox;
    private JButton voteButton;
    private JLabel messageLabel;
    private boolean votingEnded = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(VotingSystem::new);
    }

    public VotingSystem() {
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("E-Voting System");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(7, 1));

        // Voter ID input
        frame.add(new JLabel("Enter Voter ID:"));
        voterIdField = new JTextField();
        frame.add(voterIdField);

        // Name input
        frame.add(new JLabel("Enter Name:"));
        nameField = new JTextField();
        frame.add(nameField);

        // Candidate selection
        frame.add(new JLabel("Select Candidate:"));
        candidateBox = new JComboBox<>(fetchCandidates());
        frame.add(candidateBox);

        // Vote button
        voteButton = new JButton("Cast Vote");
        frame.add(voteButton);

        // Message label
        messageLabel = new JLabel("", SwingConstants.CENTER);
        frame.add(messageLabel);

        // Admin panel button
        JButton adminButton = new JButton("Admin Panel");
        frame.add(adminButton);

        voteButton.addActionListener(new VoteButtonListener());
        adminButton.addActionListener(e -> new AdminPanel());

        frame.setVisible(true);
    }

    private class VoteButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (votingEnded) {
                messageLabel.setText("Voting has ended. Please check the results.");
                return;
            }

            int voterId = Integer.parseInt(voterIdField.getText());
            String name = nameField.getText();
            String candidateName = (String) candidateBox.getSelectedItem();

            if (authenticateVoter(voterId, name)) {
                if (castVote(voterId, candidateName)) {
                    messageLabel.setText("Vote successfully cast!");
                } else {
                    messageLabel.setText("You have already voted.");
                }
            } else {
                messageLabel.setText("Invalid voter details.");
            }
        }
    }

    private boolean authenticateVoter(int voterId, String name) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("SELECT has_voted FROM Voters WHERE voter_id = ? AND name = ?")) {

            stmt.setInt(1, voterId);
            stmt.setString(2, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return !rs.getBoolean("has_voted");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean castVote(int voterId, String candidateName) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.setAutoCommit(false);

            // Get candidate ID
            PreparedStatement candidateStmt = conn.prepareStatement("SELECT candidate_id FROM Candidates WHERE name = ?");
            candidateStmt.setString(1, candidateName);
            ResultSet candidateRs = candidateStmt.executeQuery();

            if (!candidateRs.next()) return false;

            int candidateId = candidateRs.getInt("candidate_id");

            // Update vote count for candidate
            PreparedStatement voteStmt = conn.prepareStatement("UPDATE Candidates SET votes = votes + 1 WHERE candidate_id = ?");
            voteStmt.setInt(1, candidateId);
            voteStmt.executeUpdate();

            // Mark voter as having voted
            PreparedStatement voterStmt = conn.prepareStatement("UPDATE Voters SET has_voted = TRUE WHERE voter_id = ?");
            voterStmt.setInt(1, voterId);
            voterStmt.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String[] fetchCandidates() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM Candidates")) {

            rs.last();
            String[] candidates = new String[rs.getRow()];
            rs.beforeFirst();

            int index = 0;
            while (rs.next()) {
                candidates[index++] = rs.getString("name");
            }
            return candidates;

        } catch (SQLException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    private class AdminPanel extends JFrame {

        public AdminPanel() {
            setTitle("Admin Control Panel");
            setSize(400, 400);
            setLayout(new GridLayout(6, 1));

            JButton registerVoterButton = new JButton("Register New Voter");
            registerVoterButton.addActionListener(e -> registerNewVoter());
            add(registerVoterButton);

            JButton addCandidateButton = new JButton("Add Candidate");
            addCandidateButton.addActionListener(e -> addNewCandidate());
            add(addCandidateButton);

            JButton removeCandidateButton = new JButton("Remove Candidate");
            removeCandidateButton.addActionListener(e -> removeCandidate());
            add(removeCandidateButton);

            JButton endVotingButton = new JButton("End Voting and Display Results");
            endVotingButton.addActionListener(e -> endVotingAndDisplayResults());
            add(endVotingButton);

            setVisible(true);
        }

        private void registerNewVoter() {
            String voterId = JOptionPane.showInputDialog(this, "Enter Voter ID:");
            String name = JOptionPane.showInputDialog(this, "Enter Name:");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO Voters (voter_id, name) VALUES (?, ?)")) {

                stmt.setInt(1, Integer.parseInt(voterId));
                stmt.setString(2, name);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Voter registered successfully!");

            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error registering voter.");
            }
        }

        private void addNewCandidate() {
            String candidateName = JOptionPane.showInputDialog(this, "Enter Candidate Name:");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO Candidates (name) VALUES (?)")) {

                stmt.setString(1, candidateName);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Candidate added successfully!");

            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error adding candidate.");
            }
        }

        private void removeCandidate() {
            String candidateName = JOptionPane.showInputDialog(this, "Enter Candidate Name to Remove:");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM Candidates WHERE name = ?")) {

                stmt.setString(1, candidateName);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Candidate removed successfully!");

            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error removing candidate.");
            }
        }

        private void endVotingAndDisplayResults() {
            votingEnded = true;
            StringBuilder results = new StringBuilder("Voting Results:\n");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name, votes FROM Candidates")) {

                while (rs.next()) {
                    results.append(rs.getString("name")).append(": ").append(rs.getInt("votes")).append(" votes\n");
                }

                JOptionPane.showMessageDialog(this, results.toString());

            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error retrieving results.");
            }
        }
    }
}
