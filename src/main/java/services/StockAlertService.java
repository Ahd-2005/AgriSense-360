package services;

import entity.Produit;
import entity.Stock;
import javafx.application.Platform;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StockAlertService {

    private static StockAlertService instance;
    private final ServiceStockStock   serviceStock   = new ServiceStockStock();
    private final ServiceStockProduit serviceProduit = new ServiceStockProduit();

    private ScheduledExecutorService scheduler;
    private static final int INTERVAL_MINUTES = 5;

    // IDs des produits déjà notifiés — évite les notifications répétées
    private final Set<Integer> dejaNotifies = new HashSet<>();

    private AlertCallback alertCallback;

    public interface AlertCallback {
        void onAlertsUpdated(List<StockAlert> alerts);
    }

    public static class StockAlert {
        public final Stock  stock;
        public final String nomProduit;
        public final String categorie;

        public StockAlert(Stock stock, String nomProduit, String categorie) {
            this.stock      = stock;
            this.nomProduit = nomProduit;
            this.categorie  = categorie;
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

    public void demarrer() {
        // Au démarrage : badge sidebar seulement, AUCUNE notification Windows ni popup.
        // Les alertes déjà présentes sont marquées "déjà vues" dans dejaNotifies.
        // Seuls les produits qui passent sous le seuil APRÈS le lancement déclencheront une notif.
        initialiserSansNotifier();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "StockAlertThread");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(
                this::verifierEtNotifier,
                INTERVAL_MINUTES, INTERVAL_MINUTES, TimeUnit.MINUTES
        );
    }

    /**
     * Appelé UNE SEULE FOIS au démarrage.
     * Met à jour le badge sidebar et pré-remplit dejaNotifies
     * sans envoyer aucune notification ni popup.
     */
    private void initialiserSansNotifier() {
        List<StockAlert> alertesExistantes = getStocksEnAlerte();
        // Toutes les alertes existantes sont marquées "déjà vues"
        for (StockAlert a : alertesExistantes) {
            dejaNotifies.add(a.stock.getProduitId());
        }
        // Badge uniquement — pas de toast Windows, pas de barre in-app, pas de popup
        Platform.runLater(() -> {
            if (alertCallback != null) alertCallback.onAlertsUpdated(alertesExistantes);
        });
    }

    public void arreter() {
        if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdownNow();
    }

    /**
     * Vérification périodique (toutes les 5 min).
     * Notification Windows + barre in-app SEULEMENT pour les NOUVELLES alertes.
     */
    public void verifierEtNotifier() {
        List<StockAlert> toutesAlertes = getStocksEnAlerte();

        List<StockAlert> nouvellesAlertes = new ArrayList<>();
        Set<Integer> idsActuels = new HashSet<>();

        for (StockAlert a : toutesAlertes) {
            idsActuels.add(a.stock.getProduitId());
            if (!dejaNotifies.contains(a.stock.getProduitId())) {
                nouvellesAlertes.add(a);
                dejaNotifies.add(a.stock.getProduitId());
            }
        }

        // Produits revenus au-dessus du seuil → sortent de dejaNotifies
        // Ils pourront être notifiés à nouveau s'ils repassent sous le seuil
        dejaNotifies.retainAll(idsActuels);

        Platform.runLater(() -> {
            // Badge = toutes les alertes actives
            if (alertCallback != null) alertCallback.onAlertsUpdated(toutesAlertes);

            // Toast Windows + barre in-app = NOUVELLES alertes uniquement
            if (!nouvellesAlertes.isEmpty()) {
                NotificationService.getInstance().notifierStocksEnAlerte(nouvellesAlertes);
            }
        });
    }

    public List<StockAlert> getStocksEnAlerte() {
        List<StockAlert> alertes = new ArrayList<>();
        try {
            for (Stock s : serviceStock.afficher()) {
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
        } catch (SQLException e) { e.printStackTrace(); }
        return alertes;
    }
}
