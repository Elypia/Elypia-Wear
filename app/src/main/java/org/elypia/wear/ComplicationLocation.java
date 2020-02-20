package org.elypia.wear;

import android.support.wearable.complications.ComplicationData;

public enum ComplicationLocation {

    /**
     * The top of the watchface next
     * to the time on the left side.
     */
    HEAD_LEFT(0, ComplicationData.TYPE_ICON, ComplicationData.TYPE_SHORT_TEXT),

    /**
     * The top of the watchface next to the time
     * on the right side, aligned with the end
     * of the date above.
     */
    HEAD_RIGHT(1, ComplicationData.TYPE_ICON, ComplicationData.TYPE_SHORT_TEXT),

    /**
     * The largest open space on the screen
     * in the center.
     */
    CENTER(2, ComplicationData.TYPE_ICON, ComplicationData.TYPE_LONG_TEXT),

    /**
     * The complication anchored to the bottom of the watch.
     */
    BOTTOM(3, ComplicationData.TYPE_ICON, ComplicationData.TYPE_SHORT_TEXT);

    /**
     * The complication ID, if this is below 0 then it will not be
     * modifiable by users.
     */
    private final int ID;

    /**
     * The types of data this complication requires and will display.
     */
    private final int[] TYPES;

    ComplicationLocation(final int ID, final int... TYPES) {
        this.ID = ID;
        this.TYPES = TYPES;
    }

    public int getId() {
        return ID;
    }

    public int[] getTypes() {
        return TYPES;
    }

    public static int[] getComplicationIds() {
        ComplicationLocation[] locations = values();
        int length = locations.length;
        int[] ids = new int[length];

        for (int i = 0; i < length; i++)
            ids[i] = locations[i].getId();

        return ids;
    }

    public static int[][] getSupportedTypes() {
        ComplicationLocation[] locations = values();
        int length = locations.length;
        int[][] supportedTypes = new int[length][];

        for (int i = 0; i < length; i++)
            supportedTypes[i] = locations[i].getTypes();

        return supportedTypes;
    }
}
