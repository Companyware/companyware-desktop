package platform.api.theme;

import java.awt.Color;
import java.awt.Font;

/** Semantic presentation tokens; plugins must not depend on concrete brand constants. */
public interface ThemeTokens {
    Color color(ThemeColor token);
    Font font(ThemeFont token);
    int spacing(ThemeSpacing token);
}
