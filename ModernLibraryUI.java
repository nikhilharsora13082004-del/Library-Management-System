import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
//  COLOUR PALETTE & THEME
// Primary  : #1A1A2E  (dark navy)
// Accent   : #E94560  (vivid red-pink)
// Card     : #16213E  (deep blue card)
// Surface  : #0F3460  (medium blue)
// Text     : #EAEAEA  (light)
// Muted    : #A0A3B1  (grey)
// Success  : #4CAF50
// Warning  : #FF9800

public class ModernLibraryUI extends JFrame {

    // ── Colors ──────────────────────────────────
    static final Color BG        = new Color(0x1A1A2E);
    static final Color CARD      = new Color(0x16213E);
    static final Color SURFACE   = new Color(0x0F3460);
    static final Color ACCENT    = new Color(0xE94560);
    static final Color TEXT      = new Color(0xEAEAEA);
    static final Color MUTED     = new Color(0xA0A3B1);
    static final Color SUCCESS   = new Color(0x4CAF50);
    static final Color WARNING   = new Color(0xFF9800);

    // ── Fonts ────────────────────────────────────
    static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 28);
    static final Font FONT_HEAD   = new Font("Segoe UI", Font.BOLD, 16);
    static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 14);
    static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 12);
    static final Font FONT_BADGE  = new Font("Segoe UI", Font.BOLD, 11);

    Connection conn;
    JTabbedPane tabs;
    CardLayout sideLayout;

    // Dashboard counters
    JLabel lblTotalBooks, lblAvailBooks, lblStudents, lblIssued;

    public ModernLibraryUI() {
        setTitle("Library Management System");
        setSize(1200, 760);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);
        getContentPane().setBackground(BG);

        // Try to connect – won't crash if DB not ready
        try { conn = DBConnection.getConnection(); } catch (Exception ignored) {}

        buildUI();
        addWindowDragSupport();
        setVisible(true);
    }

    // ════════════════════════════════════════════
    //  MAIN LAYOUT
    // ════════════════════════════════════════════
    void buildUI() {
        setLayout(new BorderLayout());

        // Left sidebar
        JPanel sidebar = buildSidebar();
        add(sidebar, BorderLayout.WEST);

        // Main content area with CardLayout
        JPanel content = new JPanel(new CardLayout());
        content.setBackground(BG);
        sideLayout = (CardLayout) content.getLayout();

        content.add(buildDashboard(),   "dashboard");
        content.add(buildAddBook(),     "addbook");
        content.add(buildAllBooks(),    "allbooks");
        content.add(buildStudents(),    "students");
        content.add(buildIssueReturn(), "issue");

        add(content, BorderLayout.CENTER);

        // Store reference so sidebar buttons can switch panels
        for (Component c : sidebar.getComponents()) {
            if (c instanceof JPanel) {
                for (Component btn : ((JPanel) c).getComponents()) {
                    if (btn instanceof SidebarButton sb) {
                        sb.setContent(content);
                    }
                }
            }
        }
    }
    //  SIDEBAR
    JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(CARD);
        sidebar.setLayout(new BorderLayout());
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, SURFACE));

        // Logo area
        JPanel logo = new JPanel(new BorderLayout());
        logo.setBackground(CARD);
        logo.setPreferredSize(new Dimension(220, 90));
        logo.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel icon = new JLabel("📚");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 30));
        JPanel textGroup = new JPanel(new GridLayout(2, 1));
        textGroup.setOpaque(false);
        JLabel lib = new JLabel("LibraryOS");
        lib.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lib.setForeground(TEXT);
        JLabel ver = new JLabel("v2.0 Pro");
        ver.setFont(FONT_SMALL);
        ver.setForeground(ACCENT);
        textGroup.add(lib);
        textGroup.add(ver);
        logo.add(icon, BorderLayout.WEST);
        logo.add(textGroup, BorderLayout.CENTER);

        // Nav buttons
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBackground(CARD);
        nav.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[][] navItems = {
                {"🏠", "Dashboard",  "dashboard"},
                {"➕", "Add Book",   "addbook"},
                {"📖", "All Books",  "allbooks"},
                {"👤", "Students",   "students"},
                {"🔄", "Issue/Return","issue"},
        };

        for (String[] item : navItems) {
            SidebarButton btn = new SidebarButton(item[0], item[1], item[2]);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            nav.add(btn);
            nav.add(Box.createVerticalStrut(6));
        }

        // Bottom close
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setBackground(CARD);
        bottom.setBorder(new EmptyBorder(10, 10, 20, 10));
        JButton close = makeButton("✕  Close App", ACCENT, TEXT);
        close.addActionListener(e -> System.exit(0));
        bottom.add(close);

        sidebar.add(logo, BorderLayout.NORTH);
        sidebar.add(nav,  BorderLayout.CENTER);
        sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
    }

    // ════════════════════════════════════════════
    //  DASHBOARD
    // ════════════════════════════════════════════
    JPanel buildDashboard() {
        JPanel p = new JPanel(new BorderLayout(0, 20));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(30, 30, 30, 30));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Dashboard Overview");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT);
        JLabel date = new JLabel(new SimpleDateFormat("EEEE, dd MMMM yyyy").format(new Date()));
        date.setFont(FONT_BODY);
        date.setForeground(MUTED);
        header.add(title, BorderLayout.WEST);
        header.add(date,  BorderLayout.EAST);

        // Stat cards row
        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        lblTotalBooks = new JLabel("0");
        lblAvailBooks = new JLabel("0");
        lblStudents   = new JLabel("0");
        lblIssued     = new JLabel("0");
        cards.add(statCard("Total Books",     lblTotalBooks, "📚", SURFACE, new Color(0x5C6BC0)));
        cards.add(statCard("Available",       lblAvailBooks, "✅", SURFACE, SUCCESS));
        cards.add(statCard("Students",        lblStudents,   "👥", SURFACE, new Color(0x26C6DA)));
        cards.add(statCard("Books Issued",    lblIssued,     "🔄", SURFACE, WARNING));

        // Recent activity table
        JPanel recent = buildRecentTable();

        // Refresh dashboard data
        refreshDashboard();

        p.add(header,  BorderLayout.NORTH);
        p.add(cards,   BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(recent, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }

    JPanel statCard(String label, JLabel valLabel, String emoji, Color bg, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // accent bar at top
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), 4, 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel emojiLbl = new JLabel(emoji);
        emojiLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        valLabel.setForeground(TEXT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(MUTED);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(emojiLbl, BorderLayout.WEST);
        top.add(valLabel, BorderLayout.EAST);

        card.add(top, BorderLayout.CENTER);
        card.add(lbl, BorderLayout.SOUTH);
        return card;
    }

    JPanel buildRecentTable() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        JLabel title = new JLabel("Recent Activity");
        title.setFont(FONT_HEAD);
        title.setForeground(TEXT);

        String[] cols = {"Book Title", "Student", "Action", "Date"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = styleTable(model);

        // Load recent issued books if connected
        if (conn != null) {
            try {
                String sql = "SELECT b.title, s.name, ib.issue_date FROM issued_books ib " +
                        "JOIN books b ON ib.book_id=b.id " +
                        "JOIN students s ON ib.student_id=s.id " +
                        "ORDER BY ib.id DESC LIMIT 5";
                ResultSet rs = conn.createStatement().executeQuery(sql);
                while (rs.next())
                    model.addRow(new Object[]{rs.getString(1), rs.getString(2), "Issued", rs.getString(3)});
            } catch (Exception ignored) {}
        }

        JScrollPane scroll = styledScroll(table);
        scroll.setPreferredSize(new Dimension(0, 200));

        panel.add(title,  BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    void refreshDashboard() {
        if (conn == null) return;
        try {
            ResultSet rs;
            rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM books");
            if (rs.next()) lblTotalBooks.setText(String.valueOf(rs.getInt(1)));

            rs = conn.createStatement().executeQuery("SELECT SUM(available) FROM books");
            if (rs.next()) lblAvailBooks.setText(String.valueOf(rs.getInt(1)));

            rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM students");
            if (rs.next()) lblStudents.setText(String.valueOf(rs.getInt(1)));

            rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM issued_books WHERE return_date IS NULL");
            if (rs.next()) lblIssued.setText(String.valueOf(rs.getInt(1)));
        } catch (Exception ignored) {}
    }

    // ════════════════════════════════════════════
    //  ADD BOOK  (with photo upload)
    // ════════════════════════════════════════════
    JPanel buildAddBook() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Add New Book");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT);
        outer.add(title, BorderLayout.NORTH);

        // Two columns: form left, photo right
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 0, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Form fields
        JTextField fTitle   = styledField("Enter book title");
        JTextField fAuthor  = styledField("Enter author name");
        JTextField fGenre   = styledField("Genre  e.g. Fiction");
        JTextField fISBN    = styledField("ISBN number");
        JTextField fQty     = styledField("Quantity");
        JTextField fYear    = styledField("Publication year");

        addFormRow(body, gbc, "Book Title *",  fTitle,  0);
        addFormRow(body, gbc, "Author *",      fAuthor, 1);
        addFormRow(body, gbc, "Genre",         fGenre,  2);
        addFormRow(body, gbc, "ISBN",          fISBN,   3);
        addFormRow(body, gbc, "Quantity *",    fQty,    4);
        addFormRow(body, gbc, "Year",          fYear,   5);

        // Buttons row
        gbc.gridx=0; gbc.gridy=6; gbc.gridwidth=1;
        JButton btnSave  = makeButton("💾  Save Book", ACCENT, TEXT);
        JButton btnClear = makeButton("🗑  Clear", SURFACE, MUTED);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnSave);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(btnClear);
        gbc.gridwidth = 2;
        body.add(btnRow, gbc);

        // Photo panel (right side)
        JPanel photoPanel = buildPhotoUploadPanel();

        // Combine form + photo
        JPanel combined = new JPanel(new BorderLayout(30, 0));
        combined.setOpaque(false);
        combined.add(body,       BorderLayout.CENTER);
        combined.add(photoPanel, BorderLayout.EAST);

        outer.add(combined, BorderLayout.CENTER);

        // Save action
        btnSave.addActionListener(e -> {
            String t = fTitle.getText().trim();
            String a = fAuthor.getText().trim();
            String q = fQty.getText().trim();
            if (t.isEmpty() || a.isEmpty() || q.isEmpty()) {
                toast(outer, "Title, Author and Quantity are required!", ACCENT); return;
            }
            try {
                int qty = Integer.parseInt(q);
                if (conn != null) {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO books (title,author,genre,quantity,available) VALUES(?,?,?,?,?)");
                    ps.setString(1, t); ps.setString(2, a); ps.setString(3, fGenre.getText());
                    ps.setInt(4, qty);  ps.setInt(5, qty);
                    ps.executeUpdate();
                }
                toast(outer, "✅  Book added successfully!", SUCCESS);
                for (JTextField f : new JTextField[]{fTitle,fAuthor,fGenre,fISBN,fQty,fYear})
                    f.setText("");
                refreshDashboard();
            } catch (NumberFormatException ex) {
                toast(outer, "Quantity must be a number!", WARNING);
            } catch (SQLException ex) {
                toast(outer, "DB error: " + ex.getMessage(), ACCENT);
            }
        });
        btnClear.addActionListener(e -> {
            for (JTextField f : new JTextField[]{fTitle,fAuthor,fGenre,fISBN,fQty,fYear})
                f.setText("");
        });

        return outer;
    }

    JPanel buildPhotoUploadPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(240, 0));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel heading = new JLabel("Book Cover Photo");
        heading.setFont(FONT_HEAD);
        heading.setForeground(TEXT);

        // Photo display area
        JLabel photoHolder = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getIcon() == null) {
                    g2.setColor(SURFACE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setColor(MUTED);
                    g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
                    FontMetrics fm = g2.getFontMetrics();
                    String em = "📷";
                    g2.drawString(em, (getWidth()-fm.stringWidth(em))/2, getHeight()/2+15);
                } else {
                    super.paintComponent(g);
                }
                g2.dispose();
            }
        };
        photoHolder.setPreferredSize(new Dimension(200, 220));
        photoHolder.setHorizontalAlignment(SwingConstants.CENTER);

        JButton upload = makeButton("📁  Choose Photo", SURFACE, TEXT);
        upload.setFont(FONT_SMALL);

        JLabel hint = new JLabel("JPG, PNG supported");
        hint.setFont(FONT_SMALL);
        hint.setForeground(MUTED);
        hint.setHorizontalAlignment(SwingConstants.CENTER);

        upload.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg","jpeg","png","gif"));
            if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                try {
                    BufferedImage img = ImageIO.read(chooser.getSelectedFile());
                    Image scaled = img.getScaledInstance(200, 220, Image.SCALE_SMOOTH);
                    photoHolder.setIcon(new ImageIcon(scaled));
                    photoHolder.repaint();
                } catch (IOException ex) {
                    toast(panel, "Could not load image!", ACCENT);
                }
            }
        });

        panel.add(heading,     BorderLayout.NORTH);
        panel.add(photoHolder, BorderLayout.CENTER);
        JPanel btmPanel = new JPanel(new GridLayout(2,1,0,6));
        btmPanel.setOpaque(false);
        btmPanel.add(upload);
        btmPanel.add(hint);
        panel.add(btmPanel,    BorderLayout.SOUTH);
        return panel;
    }

    // ════════════════════════════════════════════
    //  ALL BOOKS  (table with search)
    // ════════════════════════════════════════════
    JPanel buildAllBooks() {
        JPanel outer = new JPanel(new BorderLayout(0, 20));
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(30, 30, 30, 30));

        // Header + search bar
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("All Books");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT);

        JTextField search = styledField("🔍  Search by title or author...");
        search.setPreferredSize(new Dimension(280, 40));

        JButton btnRefresh = makeButton("↻  Refresh", SURFACE, TEXT);
        JButton btnDelete  = makeButton("🗑  Delete", ACCENT, TEXT);

        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBar.setOpaque(false);
        rightBar.add(search);
        rightBar.add(btnRefresh);
        rightBar.add(btnDelete);

        header.add(title,    BorderLayout.WEST);
        header.add(rightBar, BorderLayout.EAST);

        // Table
        String[] cols = {"ID", "Title", "Author", "Genre", "Total", "Available", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = styleTable(model);

        // Custom renderer for Status column (colored badge)
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                JLabel lbl = new JLabel(val != null ? val.toString() : "");
                lbl.setOpaque(true);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(FONT_BADGE);
                lbl.setBorder(new EmptyBorder(4,10,4,10));
                String v = val != null ? val.toString() : "";
                if (v.equals("Available")) {
                    lbl.setBackground(new Color(0x1B5E20)); lbl.setForeground(new Color(0xA5D6A7));
                } else {
                    lbl.setBackground(new Color(0x7B0000)); lbl.setForeground(new Color(0xEF9A9A));
                }
                return lbl;
            }
        });

        JScrollPane scroll = styledScroll(table);
        loadBooks(model, null);

        // Search live filter
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate (javax.swing.event.DocumentEvent e) { loadBooks(model, search.getText()); }
            public void removeUpdate (javax.swing.event.DocumentEvent e) { loadBooks(model, search.getText()); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { loadBooks(model, search.getText()); }
        });

        btnRefresh.addActionListener(e -> loadBooks(model, null));
        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { toast(outer,"Select a book to delete!", WARNING); return; }
            int id = (int) model.getValueAt(row, 0);
            if (JOptionPane.showConfirmDialog(outer,
                    "Delete this book permanently?", "Confirm", JOptionPane.YES_NO_OPTION) == 0) {
                try {
                    if (conn != null) {
                        conn.createStatement().executeUpdate("DELETE FROM books WHERE id=" + id);
                        loadBooks(model, null);
                        refreshDashboard();
                        toast(outer, "Book deleted.", SUCCESS);
                    }
                } catch (SQLException ex) { toast(outer, ex.getMessage(), ACCENT); }
            }
        });

        outer.add(header, BorderLayout.NORTH);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    void loadBooks(DefaultTableModel model, String keyword) {
        model.setRowCount(0);
        if (conn == null) return;
        try {
            String sql = "SELECT id,title,author,genre,quantity,available FROM books";
            if (keyword != null && !keyword.isBlank())
                sql += " WHERE title LIKE '%" + keyword + "%' OR author LIKE '%" + keyword + "%'";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                int avail = rs.getInt("available");
                model.addRow(new Object[]{
                        rs.getInt("id"), rs.getString("title"), rs.getString("author"),
                        rs.getString("genre"), rs.getInt("quantity"), avail,
                        avail > 0 ? "Available" : "Issued Out"
                });
            }
        } catch (Exception ignored) {}
    }

    // ════════════════════════════════════════════
    //  STUDENTS  (with photo)
    // ════════════════════════════════════════════
    JPanel buildStudents() {
        JPanel outer = new JPanel(new BorderLayout(0, 20));
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Student Management");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT);
        outer.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(24, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 0, 0, 0));

        // ── Registration form card ──
        JPanel formCard = new JPanel(new BorderLayout(0, 16)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                g2.dispose();
            }
        };
        formCard.setOpaque(false);
        formCard.setPreferredSize(new Dimension(360, 0));
        formCard.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel formTitle = new JLabel("Register Student");
        formTitle.setFont(FONT_HEAD);
        formTitle.setForeground(TEXT);

        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 12));
        fields.setOpaque(false);

        JTextField fName  = styledField("Full name");
        JTextField fRoll  = styledField("Roll number");
        JTextField fEmail = styledField("Email address");
        JTextField fPhone = styledField("Phone number");

        // Student photo section inside form
        JLabel photoLbl = new JLabel("Photo");
        photoLbl.setFont(FONT_BODY);
        photoLbl.setForeground(MUTED);

        JLabel studentPhoto = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Circle clip
                g2.setClip(new Ellipse2D.Float(0, 0, getWidth(), getHeight()));
                if (getIcon() == null) {
                    g2.setColor(SURFACE);
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.setColor(MUTED);
                    g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
                    FontMetrics fm = g2.getFontMetrics();
                    String em = "👤";
                    g2.drawString(em, (getWidth()-fm.stringWidth(em))/2, getHeight()/2+12);
                } else {
                    super.paintComponent(g);
                }
                g2.dispose();
            }
        };
        studentPhoto.setPreferredSize(new Dimension(80, 80));

        JButton uploadPhoto = makeButton("📷 Upload Photo", SURFACE, TEXT);
        uploadPhoto.setFont(FONT_SMALL);
        uploadPhoto.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Images","jpg","jpeg","png"));
            if (chooser.showOpenDialog(outer) == JFileChooser.APPROVE_OPTION) {
                try {
                    BufferedImage img = ImageIO.read(chooser.getSelectedFile());
                    Image scaled = img.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                    studentPhoto.setIcon(new ImageIcon(scaled));
                    studentPhoto.repaint();
                } catch (IOException ex) { toast(outer,"Cannot load image",ACCENT); }
            }
        });

        JPanel photoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        photoRow.setOpaque(false);
        photoRow.add(studentPhoto);
        photoRow.add(uploadPhoto);

        addLabeledField(fields, "Full Name *",  fName);
        addLabeledField(fields, "Roll Number *",fRoll);
        addLabeledField(fields, "Email",        fEmail);
        addLabeledField(fields, "Phone",        fPhone);

        JButton btnAdd = makeButton("✚  Register Student", ACCENT, TEXT);
        btnAdd.setPreferredSize(new Dimension(Integer.MAX_VALUE, 42));

        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel mid = new JPanel(new BorderLayout(0,12));
        mid.setOpaque(false);
        mid.add(photoRow, BorderLayout.NORTH);
        mid.add(fields,   BorderLayout.CENTER);
        formCard.add(mid, BorderLayout.CENTER);
        formCard.add(btnAdd, BorderLayout.SOUTH);

        // ── Students table ──
        String[] cols = {"ID","Name","Roll Number","Email","Phone"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        JTable table = styleTable(model);
        loadStudents(model);

        btnAdd.addActionListener(e -> {
            String n = fName.getText().trim(), r = fRoll.getText().trim();
            if (n.isEmpty() || r.isEmpty()) { toast(outer,"Name and Roll are required!",ACCENT); return; }
            try {
                if (conn != null) {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO students(name,roll_number,email,phone) VALUES(?,?,?,?)");
                    ps.setString(1,n); ps.setString(2,r);
                    ps.setString(3,fEmail.getText()); ps.setString(4,fPhone.getText());
                    ps.executeUpdate();
                }
                toast(outer,"✅  Student registered!",SUCCESS);
                fName.setText(""); fRoll.setText(""); fEmail.setText(""); fPhone.setText("");
                loadStudents(model);
                refreshDashboard();
            } catch (SQLException ex) { toast(outer,"Error: "+ex.getMessage(),ACCENT); }
        });

        body.add(formCard,         BorderLayout.WEST);
        body.add(styledScroll(table), BorderLayout.CENTER);
        outer.add(body, BorderLayout.CENTER);
        return outer;
    }

    void loadStudents(DefaultTableModel model) {
        model.setRowCount(0);
        if (conn == null) return;
        try {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM students");
            while (rs.next())
                model.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)});
        } catch (Exception ignored) {}
    }

    // ════════════════════════════════════════════
    //  ISSUE / RETURN
    // ════════════════════════════════════════════
    JPanel buildIssueReturn() {
        JPanel outer = new JPanel(new BorderLayout(0, 20));
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Issue & Return Books");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT);
        outer.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(24, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 0, 0, 0));

        // ── Issue card ──
        JPanel issueCard = cardPanel();
        issueCard.setPreferredSize(new Dimension(340, 0));
        issueCard.setLayout(new GridLayout(0, 1, 0, 14));
        issueCard.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel h1 = new JLabel("Issue a Book");
        h1.setFont(FONT_HEAD); h1.setForeground(TEXT);

        JTextField fBookId    = styledField("Book ID");
        JTextField fBookName  = styledField("Book name (auto-fill)");
        fBookName.setEditable(false);
        JTextField fStudentId = styledField("Student ID");
        JTextField fDueDate   = styledField("Due date  dd/MM/yyyy");

        JTextField fIssueDate = styledField("");
        fIssueDate.setEditable(false);
        fIssueDate.setText(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

        JButton btnSearch = makeButton("🔍  Search Book", SURFACE, TEXT);
        JButton btnIssue  = makeButton("✔  Issue Book",  ACCENT,   TEXT);

        issueCard.add(h1);
        issueCard.add(labeledPair("Book ID", fBookId));
        issueCard.add(btnSearch);
        issueCard.add(labeledPair("Book Name", fBookName));
        issueCard.add(labeledPair("Student ID", fStudentId));
        issueCard.add(labeledPair("Issue Date", fIssueDate));
        issueCard.add(labeledPair("Due Date",   fDueDate));
        issueCard.add(btnIssue);

        btnSearch.addActionListener(e -> {
            try {
                if (conn == null) return;
                PreparedStatement ps = conn.prepareStatement("SELECT title FROM books WHERE id=?");
                ps.setString(1, fBookId.getText().trim());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) fBookName.setText(rs.getString(1));
                else { fBookName.setText(""); toast(outer,"Book not found!",WARNING); }
            } catch (SQLException ex) { toast(outer,ex.getMessage(),ACCENT); }
        });

        btnIssue.addActionListener(e -> {
            if (fBookName.getText().isEmpty()) { toast(outer,"Search the book first!",WARNING); return; }
            if (fStudentId.getText().isEmpty()) { toast(outer,"Enter Student ID!",WARNING); return; }
            if (fDueDate.getText().isEmpty())   { toast(outer,"Enter Due Date!",WARNING); return; }
            try {
                if (conn != null) {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO issued_books(book_id,student_id,issue_date) VALUES(?,?,CURDATE())");
                    ps.setString(1,fBookId.getText()); ps.setString(2,fStudentId.getText());
                    ps.executeUpdate();
                    conn.createStatement().executeUpdate(
                            "UPDATE books SET available=available-1 WHERE id="+fBookId.getText());
                }
                toast(outer,"✅  Book issued successfully!",SUCCESS);
                fBookId.setText(""); fBookName.setText(""); fStudentId.setText(""); fDueDate.setText("");
                refreshDashboard();
            } catch (SQLException ex) { toast(outer,"Error: "+ex.getMessage(),ACCENT); }
        });

        // ── Issued books table ──
        String[] cols = {"ID","Book","Student","Roll No.","Issue Date","Return Date"};
        DefaultTableModel issuedModel = new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        JTable issuedTable = styleTable(issuedModel);

        JButton btnReturn  = makeButton("↩  Return Selected", SUCCESS, TEXT);
        JButton btnRefresh = makeButton("↻", SURFACE, TEXT);
        btnRefresh.setPreferredSize(new Dimension(40,36));

        loadIssued(issuedModel);

        btnRefresh.addActionListener(e -> loadIssued(issuedModel));
        btnReturn.addActionListener(e -> {
            int row = issuedTable.getSelectedRow();
            if (row < 0) { toast(outer,"Select a record to return!",WARNING); return; }
            int issueId = (int) issuedModel.getValueAt(row, 0);
            try {
                if (conn != null) {
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT book_id FROM issued_books WHERE id=? AND return_date IS NULL");
                    ps.setInt(1,issueId);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) { toast(outer,"Already returned!",WARNING); return; }
                    int bookId = rs.getInt(1);
                    conn.createStatement().executeUpdate(
                            "UPDATE issued_books SET return_date=CURDATE() WHERE id="+issueId);
                    conn.createStatement().executeUpdate(
                            "UPDATE books SET available=available+1 WHERE id="+bookId);
                }
                toast(outer,"✅  Book returned!",SUCCESS);
                loadIssued(issuedModel);
                refreshDashboard();
            } catch (SQLException ex) { toast(outer,"Error: "+ex.getMessage(),ACCENT); }
        });

        JPanel tableHeader = new JPanel(new BorderLayout(8,0));
        tableHeader.setOpaque(false);
        JLabel tblTitle = new JLabel("Issued Books");
        tblTitle.setFont(FONT_HEAD); tblTitle.setForeground(TEXT);
        JPanel tblBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        tblBtns.setOpaque(false);
        tblBtns.add(btnReturn); tblBtns.add(btnRefresh);
        tableHeader.add(tblTitle,  BorderLayout.WEST);
        tableHeader.add(tblBtns,   BorderLayout.EAST);

        JPanel rightSide = new JPanel(new BorderLayout(0,12));
        rightSide.setOpaque(false);
        rightSide.add(tableHeader,          BorderLayout.NORTH);
        rightSide.add(styledScroll(issuedTable), BorderLayout.CENTER);

        body.add(issueCard, BorderLayout.WEST);
        body.add(rightSide, BorderLayout.CENTER);
        outer.add(body, BorderLayout.CENTER);
        return outer;
    }

    void loadIssued(DefaultTableModel model) {
        model.setRowCount(0);
        if (conn == null) return;
        try {
            String sql = "SELECT ib.id,b.title,s.name,s.roll_number,ib.issue_date,ib.return_date " +
                    "FROM issued_books ib JOIN books b ON ib.book_id=b.id " +
                    "JOIN students s ON ib.student_id=s.id ORDER BY ib.id DESC";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next())
                model.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),
                        rs.getString(4),rs.getString(5),
                        rs.getString(6)!=null?rs.getString(6):"Not returned"});
        } catch (Exception ignored) {}
    }

    // ════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════

    JTable styleTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(CARD);
        table.setForeground(TEXT);
        table.setFont(FONT_BODY);
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setBackground(SURFACE);
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setFont(FONT_BADGE);
        table.getTableHeader().setBorder(new EmptyBorder(8,12,8,12));
        table.setSelectionBackground(new Color(ACCENT.getRed(),ACCENT.getGreen(),ACCENT.getBlue(),80));
        table.setSelectionForeground(TEXT);
        // Alternate row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                lbl.setBorder(new EmptyBorder(0,12,0,12));
                if (!sel) lbl.setBackground(r%2==0 ? CARD : new Color(0x1a2540));
                lbl.setForeground(TEXT);
                return lbl;
            }
        });
        return table;
    }

    JScrollPane styledScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(CARD);
        scroll.getViewport().setBackground(CARD);
        scroll.setBorder(BorderFactory.createLineBorder(SURFACE, 1));
        return scroll;
    }

    JTextField styledField(String placeholder) {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(MUTED);
                    g2.setFont(FONT_BODY.deriveFont(Font.ITALIC));
                    g2.drawString(placeholder, 10, getHeight()/2+5);
                }
            }
        };
        f.setBackground(SURFACE);
        f.setForeground(TEXT);
        f.setCaretColor(ACCENT);
        f.setFont(FONT_BODY);
        f.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2A4070), 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        f.setPreferredSize(new Dimension(0, 42));
        // Focus highlight
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                f.setBorder(new CompoundBorder(
                        BorderFactory.createLineBorder(ACCENT, 1),
                        new EmptyBorder(8,10,8,10)));
            }
            @Override public void focusLost(FocusEvent e) {
                f.setBorder(new CompoundBorder(
                        BorderFactory.createLineBorder(new Color(0x2A4070),1),
                        new EmptyBorder(8,10,8,10)));
            }
        });
        return f;
    }

    JButton makeButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() :
                        getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width+20, 38));
        return btn;
    }

    JPanel cardPanel() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                g2.dispose();
            }
        };
    }

    void addFormRow(JPanel p, GridBagConstraints gbc, String label, JTextField field, int row) {
        gbc.gridx=0; gbc.gridy=row; gbc.gridwidth=1;
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_BODY); lbl.setForeground(MUTED);
        p.add(lbl, gbc);
        gbc.gridx=1; gbc.gridwidth=1;
        p.add(field, gbc);
    }

    void addLabeledField(JPanel p, String label, JTextField field) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL); lbl.setForeground(MUTED);
        p.add(lbl); p.add(field);
    }

    JPanel labeledPair(String label, JTextField field) {
        JPanel pair = new JPanel(new BorderLayout(0,4));
        pair.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL); lbl.setForeground(MUTED);
        pair.add(lbl, BorderLayout.NORTH);
        pair.add(field, BorderLayout.CENTER);
        return pair;
    }

    /** Small animated toast notification */
    void toast(JPanel parent, String msg, Color color) {
        JWindow toast = new JWindow(this);
        JLabel lbl = new JLabel("  " + msg + "  ");
        lbl.setFont(FONT_BODY); lbl.setForeground(Color.WHITE);
        lbl.setOpaque(true); lbl.setBackground(color);
        lbl.setBorder(new EmptyBorder(10,20,10,20));
        toast.add(lbl);
        toast.pack();
        // Position at bottom-center of main window
        Point loc = getLocation();
        toast.setLocation(loc.x + getWidth()/2 - toast.getWidth()/2,
                loc.y + getHeight() - 80);
        toast.setVisible(true);
        new Timer(2200, e -> toast.dispose()) {{ setRepeats(false); }}.start();
    }

    /** Allow dragging the undecorated window */
    void addWindowDragSupport() {
        final Point[] start = {null};
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { start[0] = e.getPoint(); }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (start[0] != null) {
                    Point now = e.getLocationOnScreen();
                    setLocation(now.x - start[0].x, now.y - start[0].y);
                }
            }
        });
    }

    // ════════════════════════════════════════════
    //  SIDEBAR BUTTON  (inner class)
    // ════════════════════════════════════════════
    class SidebarButton extends JButton {
        final String cardName;
        JPanel contentPanel;

        SidebarButton(String emoji, String label, String card) {
            this.cardName = card;
            setLayout(new FlowLayout(FlowLayout.LEFT, 12, 0));
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(200, 44));
            setPreferredSize(new Dimension(200, 44));

            JLabel e = new JLabel(emoji);
            e.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            JLabel l = new JLabel(label);
            l.setFont(FONT_BODY); l.setForeground(TEXT);
            add(e); add(l);

            addActionListener(ev -> {
                if (contentPanel != null)
                    ((CardLayout) contentPanel.getLayout()).show(contentPanel, cardName);
            });

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setBackground(SURFACE); repaint(); }
                @Override public void mouseExited(MouseEvent e)  { setBackground(CARD);    repaint(); }
            });
        }

        void setContent(JPanel p) { this.contentPanel = p; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? SURFACE : CARD);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            if (getModel().isRollover()) {
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, 8, 4, 28, 4, 4);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ════════════════════════════════════════════
    //  ENTRY POINT
    // ════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ModernLibraryUI::new);
    }
}
