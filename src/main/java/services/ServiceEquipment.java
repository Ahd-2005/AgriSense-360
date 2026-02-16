package services;

import entity.Equipment;
import utils.MyDataBase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceEquipment implements IService<Equipment> {

    private final Connection connection;

    public ServiceEquipment() {
        connection = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Equipment equipment) throws SQLException {
        String sql = "INSERT INTO Equipments (name, type, status, purchase_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindFields(ps, equipment);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    equipment.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Equipment equipment) throws SQLException {
        String sql = "UPDATE Equipments SET name=?, type=?, status=?, purchase_date=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindFields(ps, equipment);
            ps.setInt(5, equipment.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM Equipments WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Equipment> getAll() throws SQLException {
        List<Equipment> list = new ArrayList<>();
        String sql = "SELECT * FROM Equipments ORDER BY id";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Equipment getById(int id) throws SQLException {
        String sql = "SELECT * FROM Equipments WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    private void bindFields(PreparedStatement ps, Equipment equipment) throws SQLException {
        ps.setString(1, equipment.getName());
        ps.setString(2, equipment.getType());
        ps.setString(3, equipment.getStatus());
        LocalDate purchaseDate = equipment.getPurchaseDate();
        ps.setDate(4, purchaseDate != null ? Date.valueOf(purchaseDate) : null);
    }

    private Equipment mapRow(ResultSet rs) throws SQLException {
        Equipment e = new Equipment();
        e.setId(rs.getInt("id"));
        e.setName(rs.getString("name"));
        e.setType(rs.getString("type"));
        e.setStatus(rs.getString("status"));
        Date pd = rs.getDate("purchase_date");
        e.setPurchaseDate(pd != null ? pd.toLocalDate() : null);
        return e;
    }
}
