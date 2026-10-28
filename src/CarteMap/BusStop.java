package CarteMap;

import org.jxmapviewer.viewer.GeoPosition;

public class BusStop {
    private final GeoPosition position;
    private final String name;

    public BusStop(GeoPosition position, String name) {
        this.position = position;
        this.name = name;
    }

    public GeoPosition getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    // Utilisé par les JComboBox pour afficher le nom de l'arrêt
    @Override
    public String toString() {
        return name;
    }
}
