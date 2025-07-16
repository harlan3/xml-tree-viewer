/*
 *  XML Tree Viewer
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package orbisoftware.xml_tree_viewer;

import javax.swing.*;
import javax.swing.tree.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import jargs.gnu.CmdLineParser;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class XmlTreeViewer {

	private static List<TreePath> searchMatches = new ArrayList<>();
	private static int currentMatchIndex = -1;

	private static void printUsage() {

		System.out.println("Usage: XmlTreeViewer [OPTION]...");
		System.out.println("View XML tree.");
		System.out.println();
		System.out.println("   -f, --file         XML file for viewing");
		System.out.println("   -h, --help         Show this help message");

	}

	public static void main(String[] args) throws Exception {

		CmdLineParser parser = new CmdLineParser();

		CmdLineParser.Option fileOption = parser.addStringOption('f', "file");
		CmdLineParser.Option helpOption = parser.addBooleanOption('h', "help");

		try {
			parser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			System.out.println(e.getMessage());
			printUsage();
			System.exit(0);
		}

		String fileValue = (String) parser.getOptionValue(fileOption);
		Boolean helpValue = (Boolean) parser.getOptionValue(helpOption);

		if ((helpValue != null) || (fileValue == null)) {
			printUsage();
			System.exit(0);
		}

		// Load XML file
		File xmlFile = new File(fileValue); // Replace with your file
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.parse(xmlFile);
		doc.getDocumentElement().normalize();

		// Build tree
		DefaultMutableTreeNode rootTreeNode = createTreeNode(doc.getDocumentElement());
		JTree tree = new JTree(rootTreeNode);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		JScrollPane scrollPane = new JScrollPane(tree);

		// Path Display
		JTextField pathField = new JTextField();
		pathField.setEditable(false);
		pathField.setForeground(Color.DARK_GRAY);
		pathField.setFont(new Font("Monospaced", Font.PLAIN, 12));

		JButton copyPathButton = new JButton("Copy Path");
		copyPathButton.addActionListener(e -> {
			String path = pathField.getText();
			if (!path.isEmpty()) {
				StringSelection selection = new StringSelection(path);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(selection, null);
			}
		});

		// Update path when selection changes
		tree.addTreeSelectionListener(e -> {
			TreePath selectedPath = tree.getSelectionPath();
			if (selectedPath != null) {
				String dotPath = IntStream.range(0, selectedPath.getPathCount())
						.mapToObj(i -> selectedPath.getPathComponent(i).toString().split(" = ")[0])
						.collect(Collectors.joining("."));
				pathField.setText(dotPath);
			}
		});

		// Search Controls
		JTextField searchField = new JTextField(20);
		JButton searchButton = new JButton("Find");
		JButton nextButton = new JButton("Next");
		JButton prevButton = new JButton("Previous");
		String[] fontSizes = { "10", "15", "20", "25" };
		JComboBox<String> fontComboBox = new JComboBox<>(fontSizes);
		fontComboBox.setPreferredSize(new Dimension(100, fontComboBox.getPreferredSize().height));
		fontComboBox.setSelectedItem("15");
		fontComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {

				if (e.getStateChange() == ItemEvent.SELECTED) {

					Font currentFont = tree.getFont();
					int fontSize = Integer.parseInt((String) e.getItem());

					tree.setFont(new Font(currentFont.getName(), currentFont.getStyle(), fontSize));
				}
			}
		});

		nextButton.setEnabled(false);
		prevButton.setEnabled(false);

		// Search Logic
		searchButton.addActionListener(e -> {
			String query = searchField.getText().trim().toLowerCase();
			searchMatches.clear();
			currentMatchIndex = -1;

			if (!query.isEmpty()) {
				Enumeration<TreeNode> enumeration = rootTreeNode.depthFirstEnumeration();
				while (enumeration.hasMoreElements()) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
					String nodeText = node.getUserObject().toString().toLowerCase();
					if (nodeText.contains(query)) {
						searchMatches.add(new TreePath(node.getPath()));
					}
				}

				if (!searchMatches.isEmpty()) {
					currentMatchIndex = 0;
					highlightCurrentMatch(tree);
					nextButton.setEnabled(true);
					prevButton.setEnabled(true);
				} else {
					JOptionPane.showMessageDialog(null, "No matches found.");
					nextButton.setEnabled(false);
					prevButton.setEnabled(false);
				}
			}
		});

		nextButton.addActionListener(e -> {
			if (!searchMatches.isEmpty()) {
				currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size();
				highlightCurrentMatch(tree);
			}
		});

		prevButton.addActionListener(e -> {
			if (!searchMatches.isEmpty()) {
				currentMatchIndex = (currentMatchIndex - 1 + searchMatches.size()) % searchMatches.size();
				highlightCurrentMatch(tree);
			}
		});

		// Layout
		JPanel topPanel = new JPanel(new BorderLayout());

		JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
		pathPanel.add(new JLabel("Path: "), BorderLayout.WEST);
		pathPanel.add(pathField, BorderLayout.CENTER);
		pathPanel.add(copyPathButton, BorderLayout.EAST);

		JPanel searchFontPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;

		gbc.gridx = 0;
		searchFontPanel.add(new JLabel("Search:"), gbc);
		gbc.gridx = 1;
		searchFontPanel.add(searchField, gbc);
		gbc.gridx = 2;
		searchFontPanel.add(searchButton, gbc);
		gbc.gridx = 3;
		searchFontPanel.add(prevButton, gbc);
		gbc.gridx = 4;
		searchFontPanel.add(nextButton, gbc);
		gbc.gridx = 5;
		searchFontPanel.add(new JLabel("Font size:"), gbc);
		gbc.gridx = 6;
		searchFontPanel.add(fontComboBox, gbc);
		gbc.weightx = 1.0;

		topPanel.add(pathPanel, BorderLayout.NORTH);
		topPanel.add(searchFontPanel, BorderLayout.SOUTH);

		// Expand/Collapse Panel
		JButton expandButton = new JButton("Expand All Below Selected Node");
		JButton collapseButton = new JButton("Collapse All Below Selected Node");

		expandButton.addActionListener(e -> {
			TreePath selectedPath = tree.getSelectionPath();
			if (selectedPath != null) {
				expandAll(tree, selectedPath);
			} else {
				JOptionPane.showMessageDialog(null, "Please select a node first.");
			}
		});

		collapseButton.addActionListener(e -> {
			TreePath selectedPath = tree.getSelectionPath();
			if (selectedPath != null) {
				collapseAll(tree, selectedPath);
			} else {
				JOptionPane.showMessageDialog(null, "Please select a node first.");
			}
		});

		JPanel buttonPanel = new JPanel(new FlowLayout());
		buttonPanel.add(expandButton);
		buttonPanel.add(collapseButton);

		JFrame frame = new JFrame("XML Viewer");
		frame.setLayout(new BorderLayout());
		frame.add(topPanel, BorderLayout.NORTH);
		frame.add(scrollPane, BorderLayout.CENTER);
		frame.add(buttonPanel, BorderLayout.SOUTH);
		frame.setSize(900, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	// Highlight search result
	private static void highlightCurrentMatch(JTree tree) {

		if (!searchMatches.isEmpty() && currentMatchIndex >= 0) {
			TreePath path = searchMatches.get(currentMatchIndex);
			tree.setSelectionPath(path);
			tree.scrollPathToVisible(path);
		}
	}

	// Recursively build tree
	private static DefaultMutableTreeNode createTreeNode(Node xmlNode) {

		String displayText = xmlNode.getNodeName();

		if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
			String textContent = xmlNode.getTextContent().trim();
			boolean hasChildElements = false;
			NodeList children = xmlNode.getChildNodes();

			for (int i = 0; i < children.getLength(); i++) {
				if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
					hasChildElements = true;
					break;
				}
			}

			if (!hasChildElements && !textContent.isEmpty()) {
				displayText += " = " + textContent;
			}
		}

		DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(displayText);

		NodeList childNodes = xmlNode.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				treeNode.add(createTreeNode(child));
			}
		}

		return treeNode;
	}

	// Expand helper
	private static void expandAll(JTree tree, TreePath parent) {

		TreeNode node = (TreeNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0) {
			for (Enumeration<?> e = node.children(); e.hasMoreElements();) {
				TreeNode n = (TreeNode) e.nextElement();
				TreePath path = parent.pathByAddingChild(n);
				expandAll(tree, path);
			}
		}
		tree.expandPath(parent);
	}

	// Collapse helper
	private static void collapseAll(JTree tree, TreePath parent) {

		TreeNode node = (TreeNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0) {
			for (Enumeration<?> e = node.children(); e.hasMoreElements();) {
				TreeNode n = (TreeNode) e.nextElement();
				TreePath path = parent.pathByAddingChild(n);
				collapseAll(tree, path);
			}
		}
		tree.collapsePath(parent);
	}
}
        
        
        
        
        
       