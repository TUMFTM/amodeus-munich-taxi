// code by jph
// https://stackoverflow.com/questions/23714383/what-are-all-the-possible-values-for-http-content-type-header
package amodeus.amodeus.util.io;

/** used in repository amod for downloading scenarios */
public enum ContentType {
    APPLICATION_JSON("application/json"), //
    APPLICATION_PDF("application/pdf"), //
    APPLICATION_OCTETSTREAM("application/octet-stream"), //
    APPLICATION_XML("application/xml"), //
    APPLICATION_ZIP("application/zip"), //
    IMAGE_GIF("image/gif"), //
    IMAGE_JPEG("image/jpeg"), //
    IMAGE_PNG("image/png"), //
    IMAGE_XICON("image/x-icon"), //
    TEXT_HTML("text/html"), //
    TEXT_PLAIN("text/plain"), //
    TEXT_XML("text/xml"), //
    VIDEO_MPEG("video/mpeg"), //
    VIDEO_MP4("video/mp4"), //
    VIDEO_WEBM("video/webm"), //
    ;

    private final String expression;

    ContentType(String expression) {
        this.expression = expression;
    }

    public void require(String string) {
        if (!matches(string))
            throw new IllegalArgumentException(string);
    }

    private boolean matches(String string) {
        // text/html; charset=UTF-8
        String first = string.split(";")[0];
        return expression.equalsIgnoreCase(first);
    }
}
