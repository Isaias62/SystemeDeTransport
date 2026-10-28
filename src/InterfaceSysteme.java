import javax.swing.*;

import CarteMap.BusStop;
import CarteMap.LigneBus;
import CarteMap.MapViewer;
import CarteMap.ReseauBus;
import CarteMap.Trajet;

import java.awt.*;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class InterfaceSysteme {

    private final ReseauBus reseau = new ReseauBus();
    private final MapViewer carte = new MapViewer(reseau);

    // Onglet "Trajet"
    private final JComboBox<BusStop> departBox = new JComboBox<>();
    private final JComboBox<BusStop> arriveeBox = new JComboBox<>();
    private final JSpinner heureSpinner = new JSpinner(new SpinnerDateModel());
    private final JTextArea resultatArea = new JTextArea(14, 28);

    // Onglet "Horaires"
    private final JComboBox<LigneBus> ligneBox = new JComboBox<>();
    private final JComboBox<String> sensBox = new JComboBox<>();
    private final JComboBox<BusStop> arretBox = new JComboBox<>();
    private final DefaultListModel<String> horairesModel = new DefaultListModel<>();
    private final JList<String> horairesList = new JList<>(horairesModel);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InterfaceSysteme().afficher());
    }

    private void afficher() {
        JFrame frame = new JFrame("Système de transport - Gatineau");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(carte, BorderLayout.CENTER);

        JTabbedPane onglets = new JTabbedPane();
        onglets.addTab("Trajet", creerPanneauTrajet());
        onglets.addTab("Horaires", creerPanneauHoraires());
        onglets.addChangeListener(e -> {
            if (onglets.getSelectedIndex() == 1) {
                carte.afficherLigne((LigneBus) ligneBox.getSelectedItem());
            } else {
                carte.afficherLigne(null);
            }
        });
        frame.getContentPane().add(onglets, BorderLayout.EAST);

        frame.setSize(1150, 700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel creerPanneauTrajet() {
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (BusStop arret : reseau.getArrets()) {
            departBox.addItem(arret);
            arriveeBox.addItem(arret);
        }
        arriveeBox.setSelectedIndex(reseau.getArrets().size() > 1 ? 1 : 0);

        // Heure de départ (par défaut : maintenant)
        heureSpinner.setEditor(new JSpinner.DateEditor(heureSpinner, "HH:mm"));
        heureSpinner.setValue(new Date());

        // Panneau pour l'arrêt de départ
        inputPanel.add(ligneFormulaire("Arrêt de départ :", departBox));
        // Panneau pour l'arrêt d'arrivée
        inputPanel.add(ligneFormulaire("Arrêt d'arrivée :", arriveeBox));
        inputPanel.add(ligneFormulaire("Partir après :", heureSpinner));

        // Boutons
        JButton submitButton = new JButton("Rechercher");
        JButton inverserButton = new JButton("⇅ Inverser");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(submitButton);
        buttonPanel.add(inverserButton);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputPanel.add(buttonPanel);

        resultatArea.setEditable(false);
        resultatArea.setLineWrap(true);
        resultatArea.setWrapStyleWord(true);
        resultatArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(resultatArea);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputPanel.add(scroll);

        submitButton.addActionListener(e -> rechercherTrajet());
        inverserButton.addActionListener(e -> {
            Object depart = departBox.getSelectedItem();
            departBox.setSelectedItem(arriveeBox.getSelectedItem());
            arriveeBox.setSelectedItem(depart);
        });
        return inputPanel;
    }

    private JPanel creerPanneauHoraires() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (LigneBus ligne : reseau.getLignes()) {
            ligneBox.addItem(ligne);
        }
        panel.add(ligneFormulaire("Ligne :", ligneBox));
        panel.add(ligneFormulaire("Direction :", sensBox));
        panel.add(ligneFormulaire("Arrêt :", arretBox));

        horairesList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        horairesList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        horairesList.setVisibleRowCount(-1);
        JScrollPane scroll = new JScrollPane(horairesList);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setPreferredSize(new Dimension(300, 400));
        panel.add(scroll);

        ligneBox.addActionListener(e -> {
            remplirSensEtArrets();
            carte.afficherLigne((LigneBus) ligneBox.getSelectedItem());
        });
        sensBox.addActionListener(e -> afficherHoraires());
        arretBox.addActionListener(e -> afficherHoraires());
        remplirSensEtArrets();
        return panel;
    }

    private JPanel ligneFormulaire(String texte, JComponent champ) {
        JPanel ligne = new JPanel(new BorderLayout(5, 0));
        JLabel label = new JLabel(texte);
        label.setPreferredSize(new Dimension(115, 20));
        ligne.add(label, BorderLayout.WEST);
        ligne.add(champ, BorderLayout.CENTER);
        ligne.setAlignmentX(Component.LEFT_ALIGNMENT);
        ligne.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        ligne.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        return ligne;
    }

    private void rechercherTrajet() {
        BusStop depart = (BusStop) departBox.getSelectedItem();
        BusStop arrivee = (BusStop) arriveeBox.getSelectedItem();
        if (depart == null || arrivee == null) {
            return;
        }
        if (depart.equals(arrivee)) {
            resultatArea.setText("Le départ et l'arrivée sont le même arrêt.");
            carte.afficherTrajet(null);
            return;
        }

        int heure = heureChoisie();
        Trajet trajet = reseau.chercherTrajet(depart, arrivee, heure);
        if (trajet == null) {
            resultatArea.setText("Aucun bus disponible après " + LigneBus.formaterHeure(heure)
                    + " pour ce trajet.\nEssayez une heure plus tôt (service de 6h à 23h).");
            carte.afficherTrajet(null);
            return;
        }

        resultatArea.setText(depart.getName() + "  →  " + arrivee.getName() + "\n"
                + "Départ demandé : " + LigneBus.formaterHeure(heure) + "\n\n"
                + trajet.getDescription(heure));
        resultatArea.setCaretPosition(0);
        carte.afficherTrajet(trajet);
    }

    private int heureChoisie() {
        Calendar cal = Calendar.getInstance();
        cal.setTime((Date) heureSpinner.getValue());
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    private void remplirSensEtArrets() {
        LigneBus ligne = (LigneBus) ligneBox.getSelectedItem();
        if (ligne == null) {
            return;
        }
        // Remplir sans déclencher afficherHoraires() à chaque ajout
        sensBox.removeAllItems();
        arretBox.removeAllItems();
        sensBox.addItem("Vers " + ligne.getTerminus(true).getName());
        sensBox.addItem("Vers " + ligne.getTerminus(false).getName());
        for (BusStop arret : ligne.getArrets()) {
            arretBox.addItem(arret);
        }
        afficherHoraires();
    }

    private void afficherHoraires() {
        horairesModel.clear();
        LigneBus ligne = (LigneBus) ligneBox.getSelectedItem();
        BusStop arret = (BusStop) arretBox.getSelectedItem();
        if (ligne == null || arret == null || sensBox.getSelectedIndex() < 0) {
            return;
        }
        boolean sensAller = sensBox.getSelectedIndex() == 0;
        int index = ligne.indexDe(arret);

        if (arret.equals(ligne.getTerminus(sensAller))) {
            horairesModel.addElement("Terminus : aucun départ dans cette direction.");
            return;
        }

        LocalTime maintenant = LocalTime.now();
        int minutesMaintenant = maintenant.getHour() * 60 + maintenant.getMinute();
        List<Integer> horaires = ligne.getHoraires(index, sensAller);
        int prochain = -1;
        for (int i = 0; i < horaires.size(); i++) {
            horairesModel.addElement(" " + LigneBus.formaterHeure(horaires.get(i)) + " ");
            if (prochain < 0 && horaires.get(i) >= minutesMaintenant) {
                prochain = i;
            }
        }
        // Sélectionner le prochain passage
        if (prochain >= 0) {
            horairesList.setSelectedIndex(prochain);
            horairesList.ensureIndexIsVisible(prochain);
        }
    }
}
