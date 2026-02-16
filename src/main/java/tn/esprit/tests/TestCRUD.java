package tn.esprit.tests;

import tn.esprit.entities.Produit;
import tn.esprit.entities.Stock;
import tn.esprit.services.ServiceProduit;
import tn.esprit.services.ServiceStock;

import java.math.BigDecimal;

public class TestCRUD {
    public static void main(String[] args) {
        ServiceProduit sp = new ServiceProduit();
        ServiceStock ss = new ServiceStock();

        // 1. Créer un produit
        Produit p = new Produit();
        p.setAgriculteurId(1); // suppose un utilisateur ID=1 existe
        p.setCategorie("Intrant");
        p.setNom("Engrais bio test");
        p.setPrixUnitaire(new BigDecimal("89.90"));
        try {
            sp.ajouter(p);
            System.out.println("Produit ajouté : " + p.getNom() + " (ID = " + p.getId() + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Créer son stock
        Stock s = new Stock();
        s.setProduitId(p.getId());
        s.setQuantiteActuelle(new BigDecimal("150.000"));
        s.setSeuilAlerte(new BigDecimal("20.000"));
        s.setUniteMesure("sac");
        try {
            ss.ajouter(s);
            System.out.println("Stock ajouté pour produit ID " + p.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Lister tous les produits
        try {
            System.out.println("\nListe des produits :");
            sp.afficher().forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}