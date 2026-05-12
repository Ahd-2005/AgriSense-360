package services;

import entity.farm;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class farmservice {

    private Connection cnx;

    public farmservice() throws SQLException {
        cnx = MyDataBase.getInstance().getCnx();
    }

    // ===============================
    // GET ALL FARMS (for the list page visible to ROLE_PENDING)
    // ===============================
    public List<farm> getAllFarms() throws SQLException {
        List<farm> list = new ArrayList<>();
        String sql = "SELECT * FROM farm ORDER BY created_at DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    // ===============================
    // GET FARM BY ID
    // ===============================
    public farm getById(int id) throws SQLException {
        String sql = "SELECT * FROM farm WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    // ===============================
    // GET FARMS OWNED BY A USER
    // ===============================
    public List<farm> getFarmsByOwner(int ownerId) throws SQLException {
        List<farm> list = new ArrayList<>();
        String sql = "SELECT * FROM farm WHERE owner_id = ? ORDER BY created_at DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, ownerId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    // ===============================
    // ADD A NEW FARM  ← NEW
    // ===============================
    public void addFarm(farm f) throws SQLException {
        String sql = "INSERT INTO farm (farm_id, name, location, surface, description, image, owner_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, f.getFarmId());
        ps.setString(2, f.getName());
        ps.setString(3, f.getLocation());
        ps.setDouble(4, f.getSurface());
        ps.setString(5, f.getDescription());
        ps.setString(6, f.getImage());
        if (f.getOwnerId() != null) ps.setInt(7, f.getOwnerId());
        else ps.setNull(7, Types.INTEGER);
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) f.setId(keys.getInt(1));
        System.out.println("✅ Farm added, id=" + f.getId());
    }

    // ===============================
    // GET FARM IDs OWNED BY A USER  ← NEW (used for filtering ouvriers)
    // ===============================
    public List<Integer> getFarmIdsByOwner(int ownerId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM farm WHERE owner_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, ownerId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) ids.add(rs.getInt("id"));
        return ids;
    }

    // ===============================
    // PRIVATE HELPER
    // ===============================
    private farm mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : null;
        Integer ownerId = rs.getObject("owner_id") != null ? rs.getInt("owner_id") : null;

        return new farm(
                rs.getInt("id"),
                rs.getString("farm_id"),
                rs.getString("name"),
                rs.getString("location"),
                rs.getDouble("surface"),
                rs.getString("description"),
                rs.getString("image"),
                createdAt,
                ownerId
        );
    }
}