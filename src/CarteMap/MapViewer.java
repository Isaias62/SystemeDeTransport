package CarteMap;


import javax.swing.event.MouseInputListener;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCenter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.*;

public class MapViewer extends JPanel {

    private final ArretBus arretBus;

    // Constructeur du JPanel personnalisé
    public MapViewer(ReseauBus reseau) {

        setLayout(new BorderLayout()); // Utiliser BorderLayout
        // Créer une instance de JXMapViewer
        JXMapViewer mapViewer = new JXMapViewer();

        // OpenStreetMap refuse les requêtes sans "User-Agent" identifiable
        System.setProperty("http.agent", "SystemeDeTransport/1.0 (projet etudiant)");

        // Configuration de la tuile OpenStreetMap.
        // L'URL par défaut de la librairie est en http, qui redirige vers https :
        // Java ne suit pas cette redirection, donc la carte restait vide.
        TileFactoryInfo info = new OSMTileFactoryInfo("OpenStreetMap", "https://tile.openstreetmap.org");
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        tileFactory.setThreadPoolSize(4);
        mapViewer.setTileFactory(tileFactory);

        // Définir la position de départ
        GeoPosition startPosition = new GeoPosition(45.455, -75.705); // Coordonées géographique de la ville de Gatineau
        mapViewer.setZoom(5); // Avec JXMapViewer, plus le nombre est petit, plus on est près
        mapViewer.setAddressLocation(startPosition);

        // Configurer le mouvement de souris
        MouseInputListener mouse = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mouse);
        mapViewer.addMouseMotionListener(mouse);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCenter(mapViewer));

        // Dessiner les lignes et les arrêts de bus sur la carte
        arretBus = new ArretBus(mapViewer, reseau);

        // Ajouter le JXMapViewer à ce JPanel
        this.add(mapViewer);
        this.setPreferredSize(new Dimension(700, 600)); // Taille du JPanel
    }

    public void afficherTrajet(Trajet trajet) {
        arretBus.afficherTrajet(trajet);
    }

    public void afficherLigne(LigneBus ligne) {
        arretBus.afficherLigne(ligne);
    }
}
