package cz.muni.ics.oidc.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import java.io.IOException;

/**
 * AUP object model.
 *
 * @author Dominik Baranek <0Baranek.dominik0@gmail.com>
 */
public class Aup {

    public static final String SIGNED_ON = "signed_on";

    private String version;
    private String date;
    private String link;
    private String text;

    @JsonProperty(SIGNED_ON)
    private String signedOn = null;

    public Aup() {
    }

    public Aup(String aupAsString) throws IOException {
        this(new ObjectMapper().readTree(aupAsString));
    }

    public Aup(JsonNode node) {
        this.version = node.get("version").asText();
        this.date = node.get("date").asText();
        this.link = node.get("link").asText();
        this.text = node.get("text").asText();
        if (node.get(SIGNED_ON) != null && !(node.get(SIGNED_ON) instanceof NullNode)) {
            this.signedOn = node.get(SIGNED_ON).asText();
        }
    }

    public Aup(String version, String date, String link, String text, String signedOn) {
        this.version = version;
        this.date = date;
        this.link = link;
        this.text = text;
        this.signedOn = signedOn;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }


    public String getSignedOn() {
        return signedOn;
    }

    public void setSignedOn(String signedOn) {
        this.signedOn = signedOn;
    }

    @Override
    public String toString() {
        return "Aup{" +
                "version='" + version + '\'' +
                ", date='" + date + '\'' +
                ", link='" + link + '\'' +
                ", text='" + text + '\'' +
                ", signedOn='" + signedOn + '\'' +
                '}';
    }
}
