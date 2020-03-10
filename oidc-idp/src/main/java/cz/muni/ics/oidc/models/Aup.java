package cz.muni.ics.oidc.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AUP object model.
 *
 * @author Dominik Baranek <baranek@ics.muni.cz>
 */
public class Aup {

    public static final String SIGNED_ON = "signed_on";

    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

    @JsonIgnore
    public LocalDate getDateAsLocalDate() {
        return LocalDate.parse(date, format);
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
