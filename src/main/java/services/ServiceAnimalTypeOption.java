package services;

import entity.AnimalTypeOption;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceAnimalTypeOption {

    private final Connection connection = MyDataBase.getInstance().getCnx();

    public void add(AnimalTypeOption opt) throws SQLException {
        String sql = "INSERT INTO AnimalTypeOption (name) VALUES (?)";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, opt.getName() != null ? opt.getName().trim() : null);
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) opt.setId(rs.getInt(1));
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM AnimalTypeOption WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<AnimalTypeOption> getAll() throws SQLException {
        List<AnimalTypeOption> list = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM AnimalTypeOption ORDER BY name")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public boolean isUsedByAnimal(String typeName) throws SQLException {
        if (typeName == null) return false;
        String sql = "SELECT COUNT(*) FROM Animal WHERE type = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, typeName);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    private AnimalTypeOption mapRow(ResultSet rs) throws SQLException {
        AnimalTypeOption o = new AnimalTypeOption();
        o.setId(rs.getInt("id"));
        o.setName(rs.getString("name"));
        return o;
    }
}
