package services;

import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ServiceEnumManagement {

    private static final Pattern ENUM_PATTERN = Pattern.compile("enum\\((.+)\\)", Pattern.CASE_INSENSITIVE);

    private Connection conn() {
        return MyDataBase.getInstance().getCnx();
    }


    public List<String> getEnumValues(String tableName, String columnName) throws SQLException {
        Connection c = conn();
        if (c == null) throw new SQLException("No database connection");
        String schema = c.getCatalog();
        if (schema == null) schema = "agrisense-360";
        String sql = "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, schema);
        ps.setString(2, tableName);
        ps.setString(3, columnName);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) return new ArrayList<>();
        String columnType = rs.getString("COLUMN_TYPE");
        return parseEnumValues(columnType);
    }


    public void addEnumValue(String tableName, String columnName, String newValue) throws SQLException {
        if (newValue == null || newValue.trim().isEmpty()) throw new SQLException("Can't be empty");
        String normalized = newValue.trim().toLowerCase().replace(' ', '_');
        List<String> current = getEnumValues(tableName, columnName);
        if (current.contains(normalized)) throw new SQLException("Already exists: " + normalized);
        current.add(normalized);
        alterEnumColumn(tableName, columnName, current);
    }


    public void removeEnumValue(String tableName, String columnName, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) throw new SQLException("Can't empty");
        String normalized = value.trim().toLowerCase();
        List<String> current = getEnumValues(tableName, columnName);
        if (!current.contains(normalized)) throw new SQLException("Not found: " + normalized);
        if ("Animal".equalsIgnoreCase(tableName)) {
            if ("type".equalsIgnoreCase(columnName) && countAnimalsWithType(normalized) > 0)
                throw new SQLException("Can't remove it, some animals still use it");
            if ("location".equalsIgnoreCase(columnName) && countAnimalsWithLocation(normalized) > 0)
                throw new SQLException("Can't remove it, some animals still use it");
        }
        current.remove(normalized);
        if (current.isEmpty()) throw new SQLException("Can't remove");
        alterEnumColumn(tableName, columnName, current);
    }

    public int countAnimalsWithType(String type) throws SQLException {
        Connection c = conn();
        if (c == null) throw new SQLException("No database connection");
        String sql = "SELECT COUNT(*) FROM Animal WHERE type = ?";
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, type);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    public int countAnimalsWithLocation(String location) throws SQLException {
        Connection c = conn();
        if (c == null) throw new SQLException("No database connection");
        String sql = "SELECT COUNT(*) FROM Animal WHERE location = ?";
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, location);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    private void alterEnumColumn(String tableName, String columnName, List<String> values) throws SQLException {
        Connection c = conn();
        if (c == null) throw new SQLException("No database connection");
        StringBuilder enumDef = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) enumDef.append(",");
            enumDef.append("'").append(values.get(i).replace("'", "''")).append("'");
        }
        String sql = "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " ENUM(" + enumDef + ")";
        Statement st = c.createStatement();
        st.executeUpdate(sql);
    }

    private static List<String> parseEnumValues(String columnType) {
        List<String> list = new ArrayList<>();
        if (columnType == null) return list;
        Matcher m = ENUM_PATTERN.matcher(columnType.trim());
        if (!m.matches()) return list;
        String inner = m.group(1);
        String[] parts = inner.split(",");
        for (String p : parts) {
            String s = p.trim().replaceAll("^'|'$", "").replace("''", "'");
            if (!s.isEmpty()) list.add(s);
        }
        return list;
    }
}
