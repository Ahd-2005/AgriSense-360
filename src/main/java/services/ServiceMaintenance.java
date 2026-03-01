package services;

import entity.Equipment;
import entity.Maintenance;
import utils.MyDataBase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceMaintenance implements IService<Maintenance> {

    private final Connection connection;
    private final ServiceEquipment serviceEquipment;

    public ServiceMaintenance() {
        connection = MyDataBase.getInstance().getCnx();
        serviceEquipment = new ServiceEquipment();
    }

    @Override
    public void add(Maintenance maintenance) throws SQLException {
        ensureEquipmentExists(maintenance.getEquipmentId());
        String sql = "INSERT INTO Maintenance (equipment_id, maintenance_date, maintenance_type, cost) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindFields(ps, maintenance);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    maintenance.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Maintenance maintenance) throws SQLException {
        ensureEquipmentExists(maintenance.getEquipmentId());
        String sql = "UPDATE Maintenance SET equipment_id=?, maintenance_date=?, maintenance_type=?, cost=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindFields(ps, maintenance);
            ps.setInt(5, maintenance.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM Maintenance WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Maintenance> getAll() throws SQLException {
        List<Maintenance> list = new ArrayList<>();
        String sql = "SELECT * FROM Maintenance ORDER BY maintenance_date DESC";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Maintenance getById(int id) throws SQLException {
        String sql = "SELECT * FROM Maintenance WHERE id=?";
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

    public List<Maintenance> getByEquipmentId(int equipmentId) throws SQLException {
        List<Maintenance> list = new ArrayList<>();
        String sql = "SELECT * FROM Maintenance WHERE equipment_id=? ORDER BY maintenance_date DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public void deleteByEquipmentId(int equipmentId) throws SQLException {
        String sql = "DELETE FROM Maintenance WHERE equipment_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            ps.executeUpdate();
        }
    }

    private void ensureEquipmentExists(Integer equipmentId) throws SQLException {
        if (equipmentId == null) {
            throw new SQLException("Equipment is required");
        }
        Equipment equipment = serviceEquipment.getById(equipmentId);
        if (equipment == null) {
            throw new SQLException("Equipment with id " + equipmentId + " not found");
        }
    }

    private void bindFields(PreparedStatement ps, Maintenance maintenance) throws SQLException {
        ps.setInt(1, maintenance.getEquipmentId());
        ps.setDate(2, maintenance.getMaintenanceDate() != null ? Date.valueOf(maintenance.getMaintenanceDate()) : null);
        ps.setString(3, maintenance.getMaintenanceType());
        BigDecimal cost = maintenance.getCost();
        ps.setBigDecimal(4, cost);
    }

    private Maintenance mapRow(ResultSet rs) throws SQLException {
        Maintenance m = new Maintenance();
        m.setId(rs.getInt("id"));
        m.setEquipmentId(rs.getInt("equipment_id"));
        Date md = rs.getDate("maintenance_date");
        m.setMaintenanceDate(md != null ? md.toLocalDate() : null);
        m.setMaintenanceType(rs.getString("maintenance_type"));
        m.setCost(rs.getBigDecimal("cost"));
        return m;
    }
}
