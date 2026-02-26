package services;

import entity.Animal;
import entity.AnimalHealthRecord;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceAnimalHealthRecord implements IService<AnimalHealthRecord> {

    private final Connection connection;
    private final ServiceAnimal serviceAnimal;

    public ServiceAnimalHealthRecord() {
        connection = MyDataBase.getInstance().getCnx();
        serviceAnimal = new ServiceAnimal();
    }

    @Override
    public void add(AnimalHealthRecord record) throws SQLException {
        Animal animal = serviceAnimal.getById(record.getAnimalId());
        if (animal == null) {
            throw new SQLException("Animal id " + record.getAnimalId() + " not found");
        }
        record = clearProductionFieldsByType(record, animal.getType());

        String sql = "INSERT INTO AnimalHealthRecord (animal, recordDate, weight, appetite, conditionStatus, milkYield, eggCount, woolLength, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        setRecordParams(ps, record);
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            record.setId(rs.getInt(1));
        }
        serviceAnimal.updateHealthAndWeight(record.getAnimalId(), record.getConditionStatus().name().toLowerCase(), record.getWeight());
    }

    @Override
    public void update(AnimalHealthRecord record) throws SQLException {
        Animal animal = serviceAnimal.getById(record.getAnimalId());
        if (animal == null) {
            throw new SQLException("Animal id " + record.getAnimalId() + " not found");
        }
        record = clearProductionFieldsByType(record, animal.getType());

        String sql = "UPDATE AnimalHealthRecord SET animal=?, recordDate=?, weight=?, appetite=?, conditionStatus=?, milkYield=?, eggCount=?, woolLength=?, notes=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        setRecordParams(ps, record);
        ps.setInt(10, record.getId());
        ps.executeUpdate();
        serviceAnimal.updateHealthAndWeight(record.getAnimalId(), record.getConditionStatus().name().toLowerCase(), record.getWeight());
    }

    @Override
    public void delete(int id) throws SQLException {
        AnimalHealthRecord record = getById(id);
        if (record == null) return;
        int animalId = record.getAnimalId();

        String sql = "DELETE FROM AnimalHealthRecord WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();

        AnimalHealthRecord latest = getLatestRecordForAnimal(animalId);
        if (latest != null) {
            serviceAnimal.updateHealthAndWeight(animalId, latest.getConditionStatus().name().toLowerCase(), latest.getWeight());
        } else {
            serviceAnimal.updateHealthAndWeight(animalId, null, null);
        }
    }

    @Override
    public List<AnimalHealthRecord> getAll() throws SQLException {
        List<AnimalHealthRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM AnimalHealthRecord";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    public AnimalHealthRecord getById(int id) throws SQLException {
        String sql = "SELECT * FROM AnimalHealthRecord WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapRow(rs);
        }
        return null;
    }

    public List<AnimalHealthRecord> getRecordsByAnimalId(int animalId) throws SQLException {
        List<AnimalHealthRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM AnimalHealthRecord WHERE animal=? ORDER BY recordDate DESC";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, animalId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    private AnimalHealthRecord getLatestRecordForAnimal(int animalId) throws SQLException {
        String sql = "SELECT * FROM AnimalHealthRecord WHERE animal=? ORDER BY recordDate DESC LIMIT 1";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, animalId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapRow(rs);
        }
        return null;
    }

    private AnimalHealthRecord clearProductionFieldsByType(AnimalHealthRecord record, String type) {
        if (type == null) return record;
        String t = type.toLowerCase();
        switch (t) {
            case "cow":
            case "goat":
                record.setEggCount(null);
                record.setWoolLength(null);
                break;
            case "chicken":
                record.setMilkYield(null);
                record.setWoolLength(null);
                break;
            case "sheep":
                record.setMilkYield(null);
                record.setEggCount(null);
                break;
            default:
                break;
        }
        return record;
    }

    private void setRecordParams(PreparedStatement ps, AnimalHealthRecord record) throws SQLException {
        ps.setInt(1, record.getAnimalId());
        ps.setDate(2, record.getRecordDate() != null ? Date.valueOf(record.getRecordDate()) : null);
        ps.setObject(3, record.getWeight());
        ps.setString(4, record.getAppetite() != null ? record.getAppetite().name().toLowerCase() : null);
        ps.setString(5, record.getConditionStatus() != null ? record.getConditionStatus().name().toLowerCase() : null);
        ps.setObject(6, record.getMilkYield());
        ps.setObject(7, record.getEggCount());
        ps.setObject(8, record.getWoolLength());
        ps.setString(9, record.getNotes());
    }

    private AnimalHealthRecord mapRow(ResultSet rs) throws SQLException {
        AnimalHealthRecord r = new AnimalHealthRecord();
        r.setId(rs.getInt("id"));
        r.setAnimalId(rs.getInt("animal"));
        Date rd = rs.getDate("recordDate");
        r.setRecordDate(rd != null ? rd.toLocalDate() : null);
        r.setWeight(rs.getObject("weight") != null ? rs.getDouble("weight") : null);
        r.setAppetite(fromDbAppetite(rs.getString("appetite")));
        r.setConditionStatus(fromDbConditionStatus(rs.getString("conditionStatus")));
        r.setMilkYield(rs.getObject("milkYield") != null ? rs.getDouble("milkYield") : null);
        r.setEggCount(rs.getObject("eggCount") != null ? rs.getInt("eggCount") : null);
        r.setWoolLength(rs.getObject("woolLength") != null ? rs.getDouble("woolLength") : null);
        r.setNotes(rs.getString("notes"));
        return r;
    }

    private static AnimalHealthRecord.Appetite fromDbAppetite(String s) {
        if (s == null) return null;
        return AnimalHealthRecord.Appetite.valueOf(s.toUpperCase());
    }

    private static AnimalHealthRecord.ConditionStatus fromDbConditionStatus(String s) {
        if (s == null) return null;
        return AnimalHealthRecord.ConditionStatus.valueOf(s.toUpperCase());
    }
}
