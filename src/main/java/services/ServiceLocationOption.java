package services;

import entity.LocationOption;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceLocationOption {

    private final Connection connection = MyDataBase.getInstance().getCnx();

    public void add(LocationOption opt) throws SQLException {
        String sql = "INSERT INTO LocationOption (name) VALUES (?)";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, opt.getName() != null ? opt.getName().trim() : null);
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) opt.setId(rs.getInt(1));
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM LocationOption WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<LocationOption> getAll() throws SQLException {
        List<LocationOption> list = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM LocationOption ORDER BY name")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public boolean isUsedByAnimal(String locationName) throws SQLException {
        if (locationName == null) return false;
        String sql = "SELECT COUNT(*) FROM Animal WHERE location = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, locationName);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    private LocationOption mapRow(ResultSet rs) throws SQLException {
        LocationOption o = new LocationOption();
        o.setId(rs.getInt("id"));
        o.setName(rs.getString("name"));
        return o;
    }
}
