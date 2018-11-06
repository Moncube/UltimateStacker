package com.songoda.ultimatestacker.storage.types;

import com.songoda.ultimatestacker.UltimateStacker;
import com.songoda.ultimatestacker.storage.Storage;
import com.songoda.ultimatestacker.storage.StorageItem;
import com.songoda.ultimatestacker.storage.StorageRow;
import com.songoda.ultimatestacker.utils.MySQLDatabase;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageMysql extends Storage {

    private MySQLDatabase database;
    private static List<String> toSave = new ArrayList<>();

    public StorageMysql(UltimateStacker instance) {
        super(instance);
        this.database = new MySQLDatabase(instance);
    }

    @Override
    public boolean containsGroup(String group) {
        try {
            DatabaseMetaData dbm = database.getConnection().getMetaData();
            ResultSet rs = dbm.getTables(null, null, instance.getConfig().getString("Database.Prefix") + group, null);
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;

    }

    @Override
    public List<StorageRow> getRowsByGroup(String group) {
        List<StorageRow> rows = new ArrayList<>();
        try {
            ResultSet set = database.getConnection().createStatement().executeQuery(String.format("SELECT * FROM `" + instance.getConfig().getString("Database.Prefix") + "%s`", group));
            while (set.next()) {
                Map<String, StorageItem> items = new HashMap<>();

                String key = set.getString(1);
                for (int i = 2; i <= set.getMetaData().getColumnCount(); i++) {
                    if (set.getObject(i) == null || set.getObject(i) == "") continue;
                    StorageItem item = new StorageItem(set.getObject(i));
                    items.put(set.getMetaData().getColumnName(i), item);
                }
                StorageRow row = new StorageRow(key, items);
                rows.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }

    @Override
    public void prepareSaveItem(String group, StorageItem... items) {
            StringBuilder sql = new StringBuilder(String.format("INSERT INTO `" + instance.getConfig().getString("Database.Prefix") + "%s`", group));

            sql.append(" (");

            for (StorageItem item : items) {
                if (item == null || item.asObject() == null) continue;
                sql.append(String.format("`%s`, ", item.getKey()));
            }

            sql = new StringBuilder(sql.substring(0, sql.length() - 2));

            sql.append(") VALUES (");

            for (StorageItem item : items) {
                if (item == null || item.asObject() == null) continue;
                sql.append(String.format("'%s', ", item.asObject().toString()));
            }

            sql = new StringBuilder(sql.substring(0, sql.length() - 2));

            sql.append(");");

            toSave.add(sql.toString());
    }

    @Override
    public void doSave() {
        try {
            // Clear database
            database.getConnection().createStatement().execute("TRUNCATE `" + instance.getConfig().getString("Database.Prefix") + "entities`");
            database.getConnection().createStatement().execute("TRUNCATE `" + instance.getConfig().getString("Database.Prefix") + "spawners`");

            Statement stmt = database.getConnection().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            for (String line : toSave) {
                stmt.addBatch(line);
            }

            stmt.executeBatch();

            toSave.clear();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeConnection() {
        try {
            database.getConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

