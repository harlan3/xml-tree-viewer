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
import java.io.File;
import java.util.Enumeration;

public class XmlTreeViewer {

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
        File xmlFile = new File(fileValue);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        // Build tree
        DefaultMutableTreeNode rootTreeNode = createTreeNode(doc.getDocumentElement());
        JTree tree = new JTree(rootTreeNode);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(tree);

        // === Search Controls ===
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Find");

        // === Font Size Slider ===
        JSlider fontSlider = new JSlider(10, 30, 14);
        fontSlider.setMajorTickSpacing(5);
        fontSlider.setMinorTickSpacing(1);
        fontSlider.setPaintTicks(true);
        fontSlider.setPaintLabels(true);
        fontSlider.setToolTipText("Adjust font size");

        fontSlider.addChangeListener(e -> {
            int size = fontSlider.getValue();
            Font currentFont = tree.getFont();
            tree.setFont(new Font(currentFont.getName(), currentFont.getStyle(), size));
        });

        // === Search Button Action ===
        searchButton.addActionListener(e -> {
            String query = searchField.getText().trim().toLowerCase();
            if (!query.isEmpty()) {
                boolean found = searchAndSelect(tree, rootTreeNode, query);
                if (!found) {
                    JOptionPane.showMessageDialog(null, "No match found.");
                }
            }
        });

        // === Top Panel with Search & Font Size Slider ===
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        topPanel.add(new JLabel("Search:"), gbc);

        gbc.gridx = 1;
        topPanel.add(searchField, gbc);

        gbc.gridx = 2;
        topPanel.add(searchButton, gbc);

        gbc.gridx = 3;
        topPanel.add(new JLabel("Font size:"), gbc);

        gbc.gridx = 4;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(fontSlider, gbc);

        // === Expand / Collapse Buttons ===
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

        // === Frame Layout ===
        JFrame frame = new JFrame("XML Viewer with Search, Font Size, Expand/Collapse");
        frame.setLayout(new BorderLayout());
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    // === Helper Methods ===

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

    private static boolean searchAndSelect(JTree tree, DefaultMutableTreeNode root, String query) {
        Enumeration<TreeNode> enumeration = root.depthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            String nodeText = node.getUserObject().toString().toLowerCase();
            if (nodeText.contains(query)) {
                TreePath path = new TreePath(node.getPath());
                tree.scrollPathToVisible(path);
                tree.setSelectionPath(path);
                return true;
            }
        }
        return false;
    }
}
        
        
        
        
        
        
        
        
       