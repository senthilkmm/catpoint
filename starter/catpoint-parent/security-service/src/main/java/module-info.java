module com.udacity.catpoint.security {
    requires com.google.common;
    requires java.desktop;
    requires com.google.gson;
    requires java.prefs;
    requires miglayout;
    requires com.udacity.catpoint.image;

    opens com.udacity.catpoint.security.data to com.google.gson;
}