package com.example.agrisens360.services;

import entity.Produit;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceProduitTest {
    static ServiceStockProduit service;

    @BeforeAll
    static void setup() {
        service = new ServiceStockProduit();
    }

    static int idProduitTest;

    @Test
    @Order(1)
    void testAjouterProduit() throws SQLException {
        Produit p = new Produit();
        p.setAgriculteurId(1);
        p.setCategorie("Fertilisants");
        p.setNom("TestNom");
        p.setDescription("TestDescription");
        p.setPrixUnitaire(new BigDecimal("50.00"));
        p.setPhotoUrl("C:\\Users\\Super User\\Documents\\Esprit\\3A\\PI\\Agritech\\src\\main\\resources\\images\\default_product.png");

        service.ajouter(p);
        List<Produit> produits = service.getAllProduits();
        assertFalse(produits.isEmpty());
        assertTrue(
                produits.stream().anyMatch(prod -> prod.getNom().equals("TestNom"))
        );
        idProduitTest = produits.get(produits.size() - 1).getId();
        System.out.println("ID du produit test : " + idProduitTest);
    }

    @Test
    @Order(2)
    void testModifierProduit() throws SQLException {
        Produit p = new Produit();
        p.setId(idProduitTest);
        p.setNom("NomModifie");
        p.setDescription("DescriptionModifiee");
        p.setPrixUnitaire(new BigDecimal("60.00"));

        service.modifier(p);
        List<Produit> produits = service.getAllProduits();
        boolean trouve = produits.stream()
                .anyMatch(prod -> prod.getNom().equals("NomModifie"));
        assertTrue(trouve);
    }

    @Test
    @Order(3)
    void testSupprimerProduit() throws SQLException {
        service.supprimer(idProduitTest);
        List<Produit> produits = service.getAllProduits();
        boolean existe = produits.stream()
                .anyMatch(p -> p.getId() == idProduitTest);
        assertFalse(existe);
    }

    @AfterEach
    void cleanUp() throws SQLException {
        List<Produit> produits = service.getAllProduits();
        if (!produits.isEmpty()) {
            Produit last = produits.get(produits.size() - 1);
            service.supprimer(last.getId());
        }
    }
}