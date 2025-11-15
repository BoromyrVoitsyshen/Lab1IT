package org.example.desktop.ui;

import org.example.desktop.api.RemoteStorageClient;
import org.example.desktop.model.RemoteFileMetadata;
import org.example.desktop.sync.SyncManager;
import org.example.desktop.util.FileQuery;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MainFrame extends JFrame {
    private final RemoteStorageClient client;
    private final String username;

    private final FilesTableModel tableModel = new FilesTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<FilesTableModel> sorter = new TableRowSorter<>(tableModel);
    private final PreviewPanel preview;
    private final JComboBox<FileQuery.TypeFilter> filterBox = new JComboBox<>(FileQuery.TypeFilter.values());
    private final Map<Integer, TableColumn> hiddenCols = new HashMap<>();
    private SyncManager sync;

    public MainFrame(RemoteStorageClient client, String username) {
        super("Light Drive — " + username);
        this.client = client; this.username = username; this.preview = new PreviewPanel(client);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1000, 600);

        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> updatePreview());

        JToolBar tb = new JToolBar(); tb.setFloatable(false);
        JButton btnRefresh = new JButton("Refresh");
        JButton btnUpload = new JButton("Upload");
        JButton btnDownload = new JButton("Download");
        JButton btnDelete = new JButton("Delete");
        JButton btnSortUp = new JButton("Created ↑");
        JButton btnSortDown = new JButton("Created ↓");
        JButton btnSyncNow = new JButton("Sync now");
        JButton btnChooseSync = new JButton("Choose sync folder");

        tb.add(btnRefresh); tb.add(btnUpload); tb.add(btnDownload); tb.add(btnDelete);
        tb.addSeparator(); tb.add(new JLabel("Filter:")); tb.add(filterBox);
        tb.addSeparator(); tb.add(btnSortUp); tb.add(btnSortDown);
        tb.addSeparator(); tb.add(btnChooseSync); tb.add(btnSyncNow);

        JMenuBar mb = new JMenuBar();
        JMenu menuCols = new JMenu("Columns");
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            final int idx = i; final String name = tableModel.getColumnName(i);
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(name, true);
            if (i == 0) { item.setEnabled(false); }
            item.addActionListener(e -> toggleColumn(idx, item.isSelected()));
            menuCols.add(item);
        }
        mb.add(menuCols); setJMenuBar(mb);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(table), preview);
        split.setResizeWeight(0.6);
        add(tb, BorderLayout.NORTH); add(split, BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> refresh());
        filterBox.addActionListener(e -> applyFilter());
        btnSortUp.addActionListener(e -> sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(1, SortOrder.ASCENDING))));
        btnSortDown.addActionListener(e -> sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(1, SortOrder.DESCENDING))));
        btnUpload.addActionListener(e -> doUpload());
        btnDownload.addActionListener(e -> doDownload());
        btnDelete.addActionListener(e -> doDelete());
        btnChooseSync.addActionListener(e -> chooseSyncFolder());
        btnSyncNow.addActionListener(e -> doSyncNow());

        refresh();
    }

    private void toggleColumn(int index, boolean show) {
        TableColumnModel cm = table.getColumnModel();
        if (show) {
            if (hiddenCols.containsKey(index)) {
                TableColumn col = hiddenCols.remove(index);
                cm.addColumn(col);
                moveColumnTo(cm, index);
            }
        } else {
            TableColumn col = cm.getColumn(index);
            hiddenCols.put(index, col);
            cm.removeColumn(col);
        }
    }

    private void moveColumnTo(TableColumnModel cm, int targetIndex) {
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn c = cm.getColumn(i);
            if (c.getModelIndex() == targetIndex) { cm.moveColumn(i, targetIndex); return; }
        }
    }

    private void updatePreview() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { preview.showPreview(null); return; }
        int modelRow = table.convertRowIndexToModel(viewRow);
        preview.showPreview(tableModel.getAt(modelRow));
    }

    private void refresh() {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            java.util.List<RemoteFileMetadata> list;
            @Override protected Void doInBackground() {
                try {
                    list = client.listFiles();
                } catch (IOException e) {
                    // It's better to handle the exception here, e.g., by returning null and showing a dialog in done()
                    list = Collections.emptyList(); // Or handle error appropriately
                    JOptionPane.showMessageDialog(MainFrame.this, "Failed to list files: " + e.getMessage());
                }
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    tableModel.setFiles(list);
                    applyFilter();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, "Error refreshing files: " + ex.getMessage());
                }
            }
        };
        w.execute();
    }

    private void applyFilter() {
        FileQuery.TypeFilter selectedFilter = (FileQuery.TypeFilter) filterBox.getSelectedItem();
        if (selectedFilter == null || selectedFilter == FileQuery.TypeFilter.ALL) {
            sorter.setRowFilter(null);
            return;
        }
        String extensionToFilter;
        switch (selectedFilter) {
            case C: extensionToFilter = "c"; break;
            case JPG: extensionToFilter = "jpg"; break;
            default: sorter.setRowFilter(null); return;
        }
        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends FilesTableModel, ? extends Integer> entry) {
                String fileName = (String) entry.getValue(0);
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex >= 0) {
                    return extensionToFilter.equals(fileName.substring(dotIndex + 1).toLowerCase());
                }
                return false;
            }
        });
    }

    private void doUpload() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                client.upload(f.toPath(), username);
                refresh();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Upload failed: " + ex.getMessage());
            }
        }
    }

    private void doDownload() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int m = table.convertRowIndexToModel(row);
        RemoteFileMetadata meta = tableModel.getAt(m);
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(meta.getName()));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (InputStream is = client.download(meta.getId()); FileOutputStream fos = new FileOutputStream(fc.getSelectedFile())) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = is.read(buf)) != -1) {
                    fos.write(buf, 0, r);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Download failed: " + ex.getMessage());
            }
        }
    }

    private void doDelete() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int m = table.convertRowIndexToModel(row);
        RemoteFileMetadata meta = tableModel.getAt(m);
        if (JOptionPane.showConfirmDialog(this, "Delete " + meta.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                client.delete(meta.getId());
                refresh();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage());
            }
        }
    }

    private void chooseSyncFolder() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path dir = fc.getSelectedFile().toPath();
            if (sync != null) sync.stop();
            sync = new SyncManager(client, dir, username);
            sync.start();
            JOptionPane.showMessageDialog(this, "Sync watching started for: " + dir);
        }
    }

    private void doSyncNow() {
        if (sync == null) {
            JOptionPane.showMessageDialog(this, "Choose sync folder first");
            return;
        }
        try {
            sync.syncNow();
            refresh();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Sync error: " + ex.getMessage());
        }
    }
}