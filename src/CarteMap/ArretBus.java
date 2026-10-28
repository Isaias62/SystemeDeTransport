package CarteMap;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Dessine le réseau de bus (lignes, arrêts) et le trajet choisi sur la carte. */
public class ArretBus {

    private final JXMapViewer mapViewer;
    private final ReseauBus reseau;
    private Trajet trajetAffiche;
    private LigneBus ligneSelectionnee;

    public ArretBus(JXMapViewer mapViewer, ReseauBus reseau) {
        this.mapViewer = mapViewer;
        this.reseau = reseau;
        mapViewer.setOverlayPainter(new ReseauPainter());
        mapViewer.repaint();
    }

    /** Met le trajet en évidence et centre la carte dessus (null pour effacer). */
    public void afficherTrajet(Trajet trajet) {
        trajetAffiche = trajet;
        ligneSelectionnee = null;
        if (trajet != null) {
            List<BusStop> arrets = new ArrayList<>();
            for (Trajet.Etape etape : trajet.getEtapes()) {
                arrets.addAll(etape.getArretsTraverses());
            }
            centrerSur(arrets);
        }
        mapViewer.repaint();
    }

    /** Met une ligne en évidence (null pour effacer). */
    public void afficherLigne(LigneBus ligne) {
        ligneSelectionnee = ligne;
        trajetAffiche = null;
        if (ligne != null) {
            centrerSur(ligne.getArrets());
        }
        mapViewer.repaint();
    }

    private void centrerSur(List<BusStop> arrets) {
        Set<GeoPosition> positions = new HashSet<>();
        for (BusStop arret : arrets) {
            positions.add(arret.getPosition());
        }
        if (positions.size() > 1) {
            mapViewer.zoomToBestFit(positions, 0.7);
        } else if (positions.size() == 1) {
            mapViewer.setAddressLocation(positions.iterator().next());
        }
    }

    // Painter qui dessine tout le réseau
    private class ReseauPainter implements Painter<JXMapViewer> {

        @Override
        public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
            g = (Graphics2D) g.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Les coordonnées de geoToPixel sont celles du monde entier :
            // il faut les décaler selon la partie de la carte visible.
            Rectangle vue = map.getViewportBounds();
            g.translate(-vue.x, -vue.y);

            boolean miseEnEvidence = trajetAffiche != null || ligneSelectionnee != null;

            // 1. Toutes les lignes (pâles si un trajet ou une ligne est mis en évidence)
            for (LigneBus ligne : reseau.getLignes()) {
                boolean active = ligne == ligneSelectionnee;
                Color couleur = miseEnEvidence && !active ? transparent(ligne.getCouleur(), 70) : ligne.getCouleur();
                dessinerChemin(g, map, ligne.getArrets(), couleur, active ? 6 : 4);
            }

            // 2. Le trajet trouvé, par-dessus
            if (trajetAffiche != null) {
                for (Trajet.Etape etape : trajetAffiche.getEtapes()) {
                    dessinerChemin(g, map, etape.getArretsTraverses(), Color.BLACK, 9);
                    dessinerChemin(g, map, etape.getArretsTraverses(), etape.getLigne().getCouleur(), 6);
                }
            }

            // 3. Les arrêts avec leur nom
            g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
            for (BusStop arret : reseau.getArrets()) {
                Color remplissage = Color.WHITE;
                if (trajetAffiche != null) {
                    if (arret.equals(trajetAffiche.getEtapes().get(0).getDepart())) {
                        remplissage = new Color(40, 180, 70);
                    } else if (arret.equals(trajetAffiche.getEtapes().get(trajetAffiche.getEtapes().size() - 1).getArrivee())) {
                        remplissage = new Color(220, 50, 47);
                    }
                }
                dessinerArret(g, map, arret, remplissage);
            }
            g.dispose();
        }

        private void dessinerChemin(Graphics2D g, JXMapViewer map, List<BusStop> arrets, Color couleur, float epaisseur) {
            g.setColor(couleur);
            g.setStroke(new BasicStroke(epaisseur, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < arrets.size() - 1; i++) {
                Point2D p1 = versPixel(map, arrets.get(i).getPosition());
                Point2D p2 = versPixel(map, arrets.get(i + 1).getPosition());
                g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
            }
        }

        private void dessinerArret(Graphics2D g, JXMapViewer map, BusStop arret, Color remplissage) {
            Point2D p = versPixel(map, arret.getPosition());
            int x = (int) p.getX();
            int y = (int) p.getY();
            int r = 7;

            g.setColor(remplissage);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
            g.setColor(Color.DARK_GRAY);
            g.setStroke(new BasicStroke(2));
            g.drawOval(x - r, y - r, 2 * r, 2 * r);

            // Nom de l'arrêt sur fond blanc pour rester lisible
            String nom = arret.getName();
            int largeur = g.getFontMetrics().stringWidth(nom);
            int hauteur = g.getFontMetrics().getHeight();
            g.setColor(new Color(255, 255, 255, 210));
            g.fillRoundRect(x + r + 3, y - hauteur / 2 - 2, largeur + 8, hauteur + 2, 6, 6);
            g.setColor(Color.BLACK);
            g.drawString(nom, x + r + 7, y + g.getFontMetrics().getAscent() / 2 - 1);
        }

        private Point2D versPixel(JXMapViewer map, GeoPosition position) {
            return map.getTileFactory().geoToPixel(position, map.getZoom());
        }

        private Color transparent(Color c, int alpha) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }
    }
}
