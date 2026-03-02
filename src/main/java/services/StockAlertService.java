package services;

import controllers.MainLayoutController;
import entity.Produit;
import entity.Stock;
import javafx.application.Platform;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StockAlertService {

    private static StockAlertService instance;
    private final ServiceStockStock serviceStock = new ServiceStockStock();
    private final ServiceStockProduit serviceProduit = new ServiceStockProduit();

    // IDs des produits déjà notifiés (persistant dans la session)
    private final Set<Integer> dejaNotifies = new HashSet<>();

    private AlertCallback alertCallback;

    public interface AlertCallback {
        void onAlertsUpdated(List<StockAlert> alerts);
    }

    public static class StockAlert {
        public final Stock stock;
        public final String nomProduit;
        public final String categorie;

        public StockAlert(Stock stock, String nomProduit, String categorie) {
            this.stock = stock;
            this.nomProduit = nomProduit;
            this.categorie = categorie;
        }
    }

    private StockAlertService() {}

    public static StockAlertService getInstance() {
        if (instance == null) instance = new StockAlertService();
        return instance;
    }

    public void setAlertCallback(AlertCallback callback) {
        this.alertCallback = callback;
    }

    /**
     * À appeler une seule fois au démarrage de l'application (ex: dans MainLayoutController.initialize())
     * Charge les alertes existantes, les marque "déjà vues", met à jour le badge sans envoyer de notification.
     */
    public void initialiser() {
        List<StockAlert> alertesExistantes = getStocksEnAlerte();
        System.out.println("[AlertService] Initialisation — alertes existantes : " + alertesExistantes.size());

        // Marquer tous les produits en alerte comme déjà notifiés
        for (StockAlert a : alertesExistantes) {
            dejaNotifies.add(a.stock.getProduitId());
            System.out.println("[AlertService] Marqué déjà vu : produitId=" + a.stock.getProduitId() + " (" + a.nomProduit + ")");
        }

        // Mettre à jour le badge et la sidebar immédiatement
        Platform.runLater(() -> {
            if (alertCallback != null) {
                alertCallback.onAlertsUpdated(alertesExistantes);
            }
            MainLayoutController ctrl = MainLayoutController.getInstance();
            if (ctrl != null) {
                ctrl.signalerNouvelleAlerte(alertesExistantes.size());
            }
        });
    }

    /**
     * À appeler après CHAQUE modification de stock (ajout, modif, suppression).
     * Détecte les **nouvelles** alertes uniquement et envoie une notification Windows.
     */
    public void notifierSiNouvelleAlerte() {
        List<StockAlert> toutesAlertes = getStocksEnAlerte();
        List<StockAlert> nouvelles = new ArrayList<>();

        for (StockAlert a : toutesAlertes) {
            int id = a.stock.getProduitId();
            if (!dejaNotifies.contains(id)) {
                nouvelles.add(a);
                dejaNotifies.add(id);
                System.out.println("[AlertService] Nouvelle alerte détectée : produitId=" + id + " (" + a.nomProduit + ")");
            }
        }

        // Nettoyage : retire les IDs qui ne sont plus en alerte
        Set<Integer> idsActuels = toutesAlertes.stream()
                .map(a -> a.stock.getProduitId())
                .collect(Collectors.toSet());
        dejaNotifies.retainAll(idsActuels);

        System.out.println("[AlertService] Total alertes : " + toutesAlertes.size() + " | Nouvelles : " + nouvelles.size());

        Platform.runLater(() -> {
            // Toujours mettre à jour le badge/sidebar
            if (alertCallback != null) {
                alertCallback.onAlertsUpdated(toutesAlertes);
            }
            MainLayoutController ctrl = MainLayoutController.getInstance();
            if (ctrl != null) {
                ctrl.signalerNouvelleAlerte(toutesAlertes.size());
            }

            // Notification Windows + barre in-app UNIQUEMENT si nouvelles alertes
            if (!nouvelles.isEmpty()) {
                NotificationService.getInstance().notifierStocksEnAlerte(nouvelles);
            }
        });
    }

    public List<StockAlert> getStocksEnAlerte() {
        List<StockAlert> alertes = new ArrayList<>();
        try {
            List<Stock> stocks = serviceStock.afficher();
            for (Stock s : stocks) {
                if (s.getQuantiteActuelle() != null && s.getSeuilAlerte() != null
                        && s.getQuantiteActuelle().compareTo(s.getSeuilAlerte()) < 0) {
                    String nom = "Produit #" + s.getProduitId();
                    String cat = "";
                    try {
                        Produit p = serviceProduit.recupererParId(s.getProduitId());
                        if (p != null) {
                            nom = p.getNom();
                            cat = p.getCategorie() != null ? p.getCategorie() : "";
                        }
                    } catch (SQLException ignored) {}
                    alertes.add(new StockAlert(s, nom, cat));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alertes;
    }
}