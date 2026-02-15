package services;

import entity.Animal;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceAnimal implements IService<Animal> {

    private final Connection connection;

    public ServiceAnimal() {
        connection = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Animal animal) throws SQLException {
        String sql = "INSERT INTO Animal (earTag, type, gender, weight, healthStatus, birthDate, entryDate, origin, vaccinated, location) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setObject(1, animal.getEarTag());
        ps.setString(2, toDbEnum(animal.getType()));
        ps.setString(3, toDbEnum(animal.getGender()));
        ps.setObject(4, animal.getWeight());
        ps.setString(5, animal.getHealthStatus());
        ps.setObject(6, animal.getBirthDate());
        ps.setObject(7, animal.getEntryDate());
        ps.setString(8, toDbEnum(animal.getOrigin()));
        ps.setBoolean(9, animal.getVaccinated() != null && animal.getVaccinated());
        ps.setString(10, toDbEnum(animal.getLocation()));
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            animal.setId(rs.getInt(1));
        }
    }

    @Override
    public void update(Animal animal) throws SQLException {
        String sql = "UPDATE Animal SET earTag=?, type=?, gender=?, weight=?, healthStatus=?, birthDate=?, entryDate=?, origin=?, vaccinated=?, location=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setObject(1, animal.getEarTag());
        ps.setString(2, toDbEnum(animal.getType()));
        ps.setString(3, toDbEnum(animal.getGender()));
        ps.setObject(4, animal.getWeight());
        ps.setString(5, animal.getHealthStatus());
        ps.setObject(6, animal.getBirthDate());
        ps.setObject(7, animal.getEntryDate());
        ps.setString(8, toDbEnum(animal.getOrigin()));
        ps.setBoolean(9, animal.getVaccinated() != null && animal.getVaccinated());
        ps.setString(10, toDbEnum(animal.getLocation()));
        ps.setInt(11, animal.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM Animal WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Animal> getAll() throws SQLException {
        List<Animal> list = new ArrayList<>();
        String sql = "SELECT * FROM Animal";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    public Animal getById(int id) throws SQLException {
        String sql = "SELECT * FROM Animal WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapRow(rs);
        }
        return null;
    }

    public void updateHealthAndWeight(int animalId, String healthStatus, Double weight) throws SQLException {
        String sql = "UPDATE Animal SET healthStatus=?, weight=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, healthStatus);
        ps.setObject(2, weight);
        ps.setInt(3, animalId);
        ps.executeUpdate();
    }

    private Animal mapRow(ResultSet rs) throws SQLException {
        Animal a = new Animal();
        a.setId(rs.getInt("id"));
        a.setEarTag(rs.getObject("earTag") != null ? rs.getInt("earTag") : null);
        a.setType(fromDbAnimalType(rs.getString("type")));
        a.setGender(fromDbGender(rs.getString("gender")));
        a.setWeight(rs.getObject("weight") != null ? rs.getDouble("weight") : null);
        a.setHealthStatus(rs.getString("healthStatus"));
        Date bd = rs.getDate("birthDate");
        a.setBirthDate(bd != null ? bd.toLocalDate() : null);
        Date ed = rs.getDate("entryDate");
        a.setEntryDate(ed != null ? ed.toLocalDate() : null);
        a.setOrigin(fromDbOrigin(rs.getString("origin")));
        a.setVaccinated(rs.getBoolean("vaccinated"));
        a.setLocation(fromDbLocation(rs.getString("location")));
        return a;
    }

    private static String toDbEnum(Enum<?> e) {
        return e != null ? e.name().toLowerCase() : null;
    }

    private static Animal.AnimalType fromDbAnimalType(String s) {
        if (s == null) return null;
        return Animal.AnimalType.valueOf(s.toUpperCase());
    }

    private static Animal.Gender fromDbGender(String s) {
        if (s == null) return null;
        return Animal.Gender.valueOf(s.toUpperCase());
    }

    private static Animal.Origin fromDbOrigin(String s) {
        if (s == null) return null;
        return Animal.Origin.valueOf(s.toUpperCase().replace(" ", "_"));
    }

    private static Animal.Location fromDbLocation(String s) {
        if (s == null) return null;
        return Animal.Location.valueOf(s.toUpperCase());
    }
}
