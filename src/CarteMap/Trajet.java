package CarteMap;

import java.util.ArrayList;
import java.util.List;

/** Résultat d'une recherche d'itinéraire : une ou plusieurs étapes en bus. */
public class Trajet {

    /** Une étape = un trajet dans un seul bus, sans correspondance. */
    public static class Etape {
        private final LigneBus ligne;
        private final boolean sensAller;
        private final BusStop depart;
        private final BusStop arrivee;
        private final int heureDepart;
        private final int heureArrivee;

        public Etape(LigneBus ligne, boolean sensAller, BusStop depart, BusStop arrivee,
                     int heureDepart, int heureArrivee) {
            this.ligne = ligne;
            this.sensAller = sensAller;
            this.depart = depart;
            this.arrivee = arrivee;
            this.heureDepart = heureDepart;
            this.heureArrivee = heureArrivee;
        }

        public LigneBus getLigne() {
            return ligne;
        }

        public BusStop getDepart() {
            return depart;
        }

        public BusStop getArrivee() {
            return arrivee;
        }

        public int getHeureDepart() {
            return heureDepart;
        }

        public int getHeureArrivee() {
            return heureArrivee;
        }

        public List<BusStop> getArretsTraverses() {
            return ligne.getArretsEntre(ligne.indexDe(depart), ligne.indexDe(arrivee));
        }

        @Override
        public String toString() {
            return LigneBus.formaterHeure(heureDepart) + "  " + ligne.getNom()
                    + " (direction " + ligne.getTerminus(sensAller).getName() + ")\n"
                    + "        " + depart.getName() + "  →  " + arrivee.getName()
                    + "  (arrivée " + LigneBus.formaterHeure(heureArrivee) + ")";
        }
    }

    private final List<Etape> etapes;

    public Trajet(List<Etape> etapes) {
        this.etapes = new ArrayList<>(etapes);
    }

    public List<Etape> getEtapes() {
        return etapes;
    }

    public int getHeureDepart() {
        return etapes.get(0).getHeureDepart();
    }

    public int getHeureArrivee() {
        return etapes.get(etapes.size() - 1).getHeureArrivee();
    }

    public String getDescription(int heureDemandee) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < etapes.size(); i++) {
            if (i > 0) {
                sb.append("\n   Correspondance à ").append(etapes.get(i).getDepart().getName()).append("\n");
            }
            sb.append("• ").append(etapes.get(i)).append("\n");
        }
        sb.append("\nDurée totale : ").append(getHeureArrivee() - heureDemandee).append(" min")
          .append(" (attente comprise)");
        return sb.toString();
    }
}
