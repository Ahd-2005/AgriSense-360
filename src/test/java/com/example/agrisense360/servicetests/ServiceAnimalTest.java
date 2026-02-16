package com.example.agrisense360.servicetests;

import org.junit.jupiter.api.*;
import com.example.agrisense360.entity.Animal;
import com.example.agrisense360.services.ServiceAnimal;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServiceAnimalTest {

    static ServiceAnimal service;
    static int idAnimalTest;

    @BeforeAll
    static void setup() {
        service = new ServiceAnimal();
    }

    @Test
    @Order(1)
    void testAddAnimal() throws SQLException {
        Animal a = new Animal(9999, Animal.AnimalType.COW, Animal.Gender.FEMALE, 450.0,
                null, LocalDate.of(2020, 3, 15), LocalDate.of(2020, 4, 1),
                Animal.Origin.BORN_IN_FARM, true);
        service.add(a);
        assertNotNull(a.getId());
        idAnimalTest = a.getId();
        List<Animal> animals = service.getAll();
        assertTrue(animals.stream().anyMatch(an -> an.getEarTag() != null && an.getEarTag() == 9999));
    }

    @Test
    @Order(2)
    void testGetAllAnimals() throws SQLException {
        List<Animal> animals = service.getAll();
        assertFalse(animals.isEmpty());
        assertTrue(animals.stream().anyMatch(a -> a.getId() == idAnimalTest));
    }

    @Test
    @Order(3)
    void testGetById() throws SQLException {
        Animal found = service.getById(idAnimalTest);
        assertNotNull(found);
        assertEquals(idAnimalTest, found.getId());
        assertEquals(9999, found.getEarTag());
        assertEquals(Animal.AnimalType.COW, found.getType());
        assertEquals(Animal.Gender.FEMALE, found.getGender());
    }

    @Test
    @Order(4)
    void testUpdateAnimal() throws SQLException {
        Animal a = service.getById(idAnimalTest);
        assertNotNull(a);
        a.setEarTag(9998);
        a.setWeight(460.0);
        service.update(a);
        Animal updated = service.getById(idAnimalTest);
        assertNotNull(updated);
        assertEquals(9998, updated.getEarTag());
        assertEquals(460.0, updated.getWeight());
    }

    @Test
    @Order(5)
    void testDeleteAnimal() throws SQLException {
        service.delete(idAnimalTest);
        Animal deleted = service.getById(idAnimalTest);
        assertNull(deleted);
        assertFalse(service.getAll().stream().anyMatch(a -> a.getId() == idAnimalTest));
    }
}
