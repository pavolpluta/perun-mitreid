package cz.muni.ics.oidc.models;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Model representing value of attribute from Perun.
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class PerunAttributeValue extends PerunAttributeValueAwareModel {

    private String attrName;

    public PerunAttributeValue(String attrName, String type, JsonNode value) {
        super(type, value);
        this.setAttrName(attrName);
    }

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }



}
