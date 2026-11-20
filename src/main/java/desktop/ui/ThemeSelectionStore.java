package desktop.ui;

/** Small persistence seam so theme selection can be tested without user preferences. */
interface ThemeSelectionStore {
    String load();
    void save(String themeId);
}
