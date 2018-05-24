package de.zipperle.cowtracker.db.listener;

import java.util.List;

import de.zipperle.cowtracker.db.datastructure.Locations;

public interface LocationQueryListener {
    public void updateCowLocations(List<Locations> locations);
}
