package app.space.notes.domain;

/** Portrait panel budget in pixels. Insets/chrome are removed by the caller. */
public final class FormattingViewport {
    private FormattingViewport(){}
    public static int height(int available,int readableBody,int minimumControls,int preferredMaximum){
        if(available<0 || readableBody<0 || minimumControls<0 || preferredMaximum<minimumControls)
            throw new IllegalArgumentException("Invalid formatting viewport budget");
        // On a very short viewport prioritise reachable, scrollable controls;
        // normal viewports reserve the requested prose space before growing the panel.
        return Math.min(available,Math.min(preferredMaximum,Math.max(minimumControls,available-readableBody)));
    }
}
