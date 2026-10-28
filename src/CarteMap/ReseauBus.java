package CarteMap;

import org.jxmapviewer.viewer.GeoPosition;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/** Contient tous les arrêts et toutes les lignes, et calcule les itinéraires. */
public class ReseauBus {

    private final List<BusStop> arrets = new ArrayList<>();
    private final List<LigneBus> lignes = new ArrayList<>();

    public ReseauBus() {
        // Arrêts de bus (Gatineau)
        BusStop centrale  = ajouterArret(45.4773, -75.7016, "Station Centrale");
        BusStop parc      = ajouterArret(45.4865, -75.7125, "Parc de la Gatineau");
        BusStop uqo       = ajouterArret(45.4225, -75.7383, "Université du Québec (UQO)");
        BusStop musee     = ajouterArret(45.4301, -75.7090, "Musée canadien de l'histoire");
        BusStop casino    = ajouterArret(45.4436, -75.7206, "Casino du Lac-Leamy");
        BusStop portage   = ajouterArret(45.4270, -75.7160, "Place du Portage");
        BusStop promenade = ajouterArret(45.4776, -75.6598, "Les Promenades Gatineau");

        // Lignes : arrêts, minutes entre arrêts, premier départ, dernier départ, fréquence
        lignes.add(new LigneBus("Ligne 11", new Color(220, 50, 47),
                List.of(parc, centrale, casino, portage, musee),
                new int[] {6, 9, 7, 4}, heure(6, 0), heure(22, 30), 15));
        lignes.add(new LigneBus("Ligne 22", new Color(38, 110, 210),
                List.of(promenade, centrale, uqo),
                new int[] {8, 14}, heure(6, 10), heure(22, 10), 20));
        lignes.add(new LigneBus("Ligne 33", new Color(40, 160, 70),
                List.of(uqo, portage, musee, casino),
                new int[] {5, 4, 8}, heure(6, 5), heure(21, 35), 30));
    }

    private BusStop ajouterArret(double lat, double lon, String nom) {
        BusStop arret = new BusStop(new GeoPosition(lat, lon), nom);
        arrets.add(arret);
        return arret;
    }

    public static int heure(int h, int m) {
        return h * 60 + m;
    }

    public List<BusStop> getArrets() {
        return arrets;
    }

    public List<LigneBus> getLignes() {
        return lignes;
    }

    /**
     * Cherche le trajet qui arrive le plus tôt (algorithme de Dijkstra sur les heures d'arrivée).
     * Les correspondances entre lignes sont permises.
     *
     * @return le trajet, ou null s'il n'y a plus de bus pour s'y rendre aujourd'hui
     */
    public Trajet chercherTrajet(BusStop depart, BusStop arrivee, int heureDepart) {
        if (depart.equals(arrivee)) {
            return null;
        }

        Map<BusStop, Integer> meilleureHeure = new HashMap<>();
        Map<BusStop, Trajet.Etape> etapePrecedente = new HashMap<>();
        Set<BusStop> visites = new HashSet<>();
        PriorityQueue<BusStop> file = new PriorityQueue<>(
                (a, b) -> Integer.compare(meilleureHeure.get(a), meilleureHeure.get(b)));

        meilleureHeure.put(depart, heureDepart);
        file.add(depart);

        while (!file.isEmpty()) {
            BusStop courant = file.poll();
            if (!visites.add(courant)) {
                continue;
            }
            if (courant.equals(arrivee)) {
                break;
            }
            int heureCourante = meilleureHeure.get(courant);

            for (LigneBus ligne : lignes) {
                int index = ligne.indexDe(courant);
                if (index < 0) {
                    continue;
                }
                for (boolean sensAller : new boolean[] {true, false}) {
                    int passage = ligne.prochainPassage(index, sensAller, heureCourante);
                    if (passage < 0) {
                        continue;
                    }
                    // On reste dans le bus et on regarde chaque arrêt suivant
                    int pas = sensAller ? 1 : -1;
                    for (int j = index + pas; j >= 0 && j < ligne.getArrets().size(); j += pas) {
                        BusStop suivant = ligne.getArrets().get(j);
                        int heureArrivee = passage + ligne.getDecalage(j, sensAller) - ligne.getDecalage(index, sensAller);
                        Integer connue = meilleureHeure.get(suivant);
                        if (!visites.contains(suivant) && (connue == null || heureArrivee < connue)) {
                            meilleureHeure.put(suivant, heureArrivee);
                            etapePrecedente.put(suivant,
                                    new Trajet.Etape(ligne, sensAller, courant, suivant, passage, heureArrivee));
                            file.remove(suivant);
                            file.add(suivant);
                        }
                    }
                }
            }
        }

        if (!etapePrecedente.containsKey(arrivee)) {
            return null;
        }

        // Reconstituer le trajet en remontant de l'arrivée vers le départ
        List<Trajet.Etape> etapes = new ArrayList<>();
        BusStop arret = arrivee;
        while (!arret.equals(depart)) {
            Trajet.Etape etape = etapePrecedente.get(arret);
            etapes.add(etape);
            arret = etape.getDepart();
        }
        Collections.reverse(etapes);
        return new Trajet(etapes);
    }
}
