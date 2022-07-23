package org.matsim.amodeus.config.modal;

import org.matsim.amodeus.components.router.DefaultAmodeusRouter;
import org.matsim.core.config.ReflectiveConfigGroup;

public class LinkSpeedsConfig extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "linkSpeeds";

    static public final String LINKSPEEDFILE = "linkSpeedFile";

    static public final String DEFAULT_LINK_SPEED_FILE = "linkSpeedData";
    private String linkSpeedFile = DEFAULT_LINK_SPEED_FILE;

    public LinkSpeedsConfig() {
        super(GROUP_NAME, true);
    }

    @StringGetter(LINKSPEEDFILE)
    public String getLinkSpeedFile() {
        return linkSpeedFile;
    }

    @StringSetter(LINKSPEEDFILE)
    public void setLinkSpeedFile(String type) {
        this.linkSpeedFile = type;
    }
}
