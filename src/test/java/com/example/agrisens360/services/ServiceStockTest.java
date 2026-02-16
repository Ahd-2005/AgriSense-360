package com.example.agrisens360.services;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import com.example.agrisens360.entity.Produit;
import com.example.agrisens360.entity.Stock;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceStockTest {
    static ServiceStock serviceStock;  // Instance de la vraie classe ServiceStock
    static ServiceProduit serviceProduit;  // Pour créer un produit lié

    @BeforeAll
    static void setup() {
        serviceStock = new ServiceStock();  // Instancier la vraie classe
        serviceProduit = new ServiceProduit();
    }

    static int idProduitTest;  // Produit lié au stock
    static int idStockTest;

    @Test
    @Order(1)
    void testAjouterStock() throws SQLException {
        // D'abord, ajouter un produit lié
        Produit p = new Produit();
        p.setAgriculteurId(1);
        p.setCategorie("Intrant");
        p.setNom("ProduitPourStock");
        p.setPrixUnitaire(new BigDecimal("40.00"));
        serviceProduit.ajouter(p);
        List<Produit> produits = serviceProduit.afficher();
        idProduitTest = produits.get(produits.size() - 1).getId();

        // Maintenant, ajouter le stock
        Stock s = new Stock();
        s.setProduitId(idProduitTest);
        s.setQuantiteActuelle(new BigDecimal("100.00"));
        s.setSeuilAlerte(new BigDecimal("10.00"));
        s.setUniteMesure("kg");
        serviceStock.ajouter(s);  // Appel sur l'instance de la vraie classe
        List<Stock> stocks = serviceStock.afficher();
        assertFalse(stocks.isEmpty());
        assertTrue(
                stocks.stream().anyMatch(stk -> stk.getProduitId() == idProduitTest)
        );
        idStockTest = stocks.get(stocks.size() - 1).getId();
        System.out.println("ID Stock ajouté : " + idStockTest);
    }

    @Test
    @Order(2)
    void testModifierStock() throws SQLException {
        Stock s = new Stock();
        s.setId(idStockTest);
        s.setQuantiteActuelle(new BigDecimal("200.00"));
        serviceStock.modifier(s);
        List<Stock> stocks = serviceStock.afficher();
        boolean trouve = stocks.stream()
                .anyMatch(stk -> stk.getQuantiteActuelle().compareTo(new BigDecimal("200.00")) == 0);
        assertTrue(trouve);
    }

    @Test
    @Order(3)
    void testSupprimerStock() throws SQLException {
        serviceStock.supprimer(idStockTest);
        List<Stock> stocks = serviceStock.afficher();
        boolean existe = stocks.stream()
                .anyMatch(s -> s.getId() == idStockTest);
        assertFalse(existe);
    }

    @AfterEach
    void cleanUp() throws SQLException {
        // Nettoyer le stock
        List<Stock> stocks = serviceStock.afficher();
        if (!stocks.isEmpty()) {
            Stock lastStock = stocks.get(stocks.size() - 1);
            serviceStock.supprimer(lastStock.getId());
        }
        // Nettoyer le produit lié
        List<Produit> produits = serviceProduit.afficher();
        if (!produits.isEmpty()) {
            Produit lastProduit = produits.get(produits.size() - 1);
            serviceProduit.supprimer(lastProduit.getId());
        }
    }
}