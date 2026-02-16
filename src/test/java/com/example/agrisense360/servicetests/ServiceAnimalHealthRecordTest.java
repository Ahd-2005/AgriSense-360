package com.example.agrisense360.servicetests;

import org.junit.jupiter.api.*;
import com.example.agrisense360.entity.Animal;
import com.example.agrisense360.entity.AnimalHealthRecord;
import com.example.agrisense360.services.ServiceAnimal;
import com.example.agrisense360.services.ServiceAnimalHealthRecord;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServiceAnimalHealthRecordTest {

    static ServiceAnimal serviceAnimal;
    static ServiceAnimalHealthRecord serviceRecord;
    static int idAnimalTest;
    static int idRecordTest;

    @BeforeAll
    static void setup() throws SQLException {
        serviceAnimal = new ServiceAnimal();
        serviceRecord = new ServiceAnimalHealthRecord();
        Animal a = new Animal(8888, Animal.AnimalType.COW, Animal.Gender.FEMALE, 400.0,
                null, LocalDate.of(2021, 1, 10), LocalDate.of(2021, 2, 1),
                Animal.Origin.BORN_IN_FARM, false);
        serviceAnimal.add(a);
        idAnimalTest = a.getId();
    }

    @AfterAll
    static void cleanup() throws SQLException {
        if (idAnimalTest > 0) {
            serviceAnimal.delete(idAnimalTest);
        }
    }

    @Test
    @Order(1)
    void testAddRecord() throws SQLException {
        AnimalHealthRecord r = new AnimalHealthRecord(idAnimalTest, LocalDate.now(), 405.0,
                AnimalHealthRecord.Appetite.NORMAL, AnimalHealthRecord.ConditionStatus.HEALTHY,
                25.5, null, null, "test test");
        serviceRecord.add(r);
        assertNotNull(r.getId());
        idRecordTest = r.getId();
        List<AnimalHealthRecord> records = serviceRecord.getRecordsByAnimalId(idAnimalTest);
        assertTrue(records.stream().anyMatch(rec -> rec.getId() == idRecordTest));
        assertEquals(25.5, records.get(0).getMilkYield());
    }

    @Test
    @Order(2)
    void testGetRecordsByAnimalId() throws SQLException {
        List<AnimalHealthRecord> records = serviceRecord.getRecordsByAnimalId(idAnimalTest);
        assertFalse(records.isEmpty());
        assertTrue(records.stream().anyMatch(r -> r.getAnimalId() == idAnimalTest));
    }

    @Test
    @Order(3)
    void testGetById() throws SQLException {
        AnimalHealthRecord found = serviceRecord.getById(idRecordTest);
        assertNotNull(found);
        assertEquals(idRecordTest, found.getId());
        assertEquals(idAnimalTest, found.getAnimalId());
        assertEquals(AnimalHealthRecord.ConditionStatus.HEALTHY, found.getConditionStatus());
    }

    @Test
    @Order(4)
    void testUpdateRecord() throws SQLException {
        AnimalHealthRecord r = serviceRecord.getById(idRecordTest);
        assertNotNull(r);
        r.setWeight(410.0);
        r.setConditionStatus(AnimalHealthRecord.ConditionStatus.SICK);
        r.setMilkYield(20.0);
        serviceRecord.update(r);
        AnimalHealthRecord updated = serviceRecord.getById(idRecordTest);
        assertNotNull(updated);
        assertEquals(410.0, updated.getWeight());
        assertEquals(AnimalHealthRecord.ConditionStatus.SICK, updated.getConditionStatus());
        assertEquals(20.0, updated.getMilkYield());
    }

    @Test
    @Order(5)
    void testDeleteRecord() throws SQLException {
        serviceRecord.delete(idRecordTest);
        AnimalHealthRecord deleted = serviceRecord.getById(idRecordTest);
        assertNull(deleted);
        List<AnimalHealthRecord> records = serviceRecord.getRecordsByAnimalId(idAnimalTest);
        assertFalse(records.stream().anyMatch(r -> r.getId() == idRecordTest));
    }
}
