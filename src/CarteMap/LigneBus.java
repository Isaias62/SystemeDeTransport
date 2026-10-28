package CarteMap;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Une ligne de bus : une suite d'arrêts parcourue dans les deux sens.
 * Les heures sont exprimées en minutes depuis minuit (ex. 6h30 = 390).
 */
public class LigneBus {
    private final String nom;
    private final Color couleur;
    private final List<BusStop> arrets;
    private final int[] tempsCumule;   // minutes depuis le premier arrêt (sens aller)
    private final int premierDepart;   // premier départ du terminus
    private final int dernierDepart;   // dernier départ du terminus
    private final int frequence;       // minutes entre deux bus

    /**
     * @param minutesEntreArrets durée entre l'arrêt i et l'arrêt i+1 (taille = nombre d'arrêts - 1)
     */
    public LigneBus(String nom, Color couleur, List<BusStop> arrets, int[] minutesEntreArrets,
                    int premierDepart, int dernierDepart, int frequence) {
        if (minutesEntreArrets.length != arrets.size() - 1) {
            throw new IllegalArgumentException("Il faut une durée entre chaque paire d'arrêts consécutifs");
        }
        this.nom = nom;
        this.couleur = couleur;
        this.arrets = List.copyOf(arrets);
        this.premierDepart = premierDepart;
        this.dernierDepart = dernierDepart;
        this.frequence = frequence;

        tempsCumule = new int[arrets.size()];
        for (int i = 1; i < arrets.size(); i++) {
            tempsCumule[i] = tempsCumule[i - 1] + minutesEntreArrets[i - 1];
        }
    }

    public String getNom() {
        return nom;
    }

    public Color getCouleur() {
        return couleur;
    }

    public List<BusStop> getArrets() {
        return arrets;
    }

    public int indexDe(BusStop arret) {
        return arrets.indexOf(arret);
    }

    /** Terminus vers lequel roule le bus dans le sens donné. */
    public BusStop getTerminus(boolean sensAller) {
        return sensAller ? arrets.get(arrets.size() - 1) : arrets.get(0);
    }

    /** Minutes entre le départ du terminus et le passage à l'arrêt index. */
    public int getDecalage(int index, boolean sensAller) {
        return sensAller ? tempsCumule[index] : tempsCumule[arrets.size() - 1] - tempsCumule[index];
    }

    /** Toutes les heures de passage à un arrêt dans un sens. */
    public List<Integer> getHoraires(int index, boolean sensAller) {
        List<Integer> horaires = new ArrayList<>();
        int decalage = getDecalage(index, sensAller);
        for (int depart = premierDepart; depart <= dernierDepart; depart += frequence) {
            horaires.add(depart + decalage);
        }
        return horaires;
    }

    /** Prochain passage à l'arrêt à partir de l'heure donnée, ou -1 s'il n'y en a plus. */
    public int prochainPassage(int index, boolean sensAller, int apres) {
        for (int passage : getHoraires(index, sensAller)) {
            if (passage >= apres) {
                return passage;
            }
        }
        return -1;
    }

    /** Liste des arrêts traversés entre deux index (inclus), dans l'ordre du trajet. */
    public List<BusStop> getArretsEntre(int indexDepart, int indexArrivee) {
        List<BusStop> resultat = new ArrayList<>();
        int pas = indexArrivee >= indexDepart ? 1 : -1;
        for (int i = indexDepart; i != indexArrivee + pas; i += pas) {
            resultat.add(arrets.get(i));
        }
        return resultat;
    }

    public static String formaterHeure(int minutes) {
        return String.format("%02d:%02d", (minutes / 60) % 24, minutes % 60);
    }

    @Override
    public String toString() {
        return nom;
    }
}
