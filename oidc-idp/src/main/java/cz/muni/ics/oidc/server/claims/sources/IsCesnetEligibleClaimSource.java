package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import cz.muni.ics.oidc.server.claims.ClaimUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * This source checks if the timestamp is within 12 months from now. If so, returns TRUE, FALSE otherwise.
 *
 * Configuration (replace [claimName] with the name of the claim):
 * <ul>
 *     <li><b>custom.claim.[claimName].source.attribute</b> - attribute containing the isCesnetEligible timestamp</li>
 *     <li><b>custom.claim.[claimName].source.valueFormat</b> - format of the value (i.e. yyyy-MM-dd HH:mm:ss</li>
 * </ul>
 *
 * @author Pavol Pluta <pavol.pluta1@gmail.com>
 */
public class IsCesnetEligibleClaimSource extends ClaimSource {

    public static final Logger log = LoggerFactory.getLogger(IsCesnetEligibleClaimSource.class);

    private static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final int VALIDITY_PERIOD = 12; // 12 months

    private static final String SOURCE_ATTR_NAME = "attribute";
    private static final String VALUE_FORMAT = "valueFormat";

    private final String sourceAttr;
    private String valueFormat;
    private final String claimName;

    public IsCesnetEligibleClaimSource(ClaimSourceInitContext ctx) {
        super(ctx);
        this.claimName = ctx.getClaimName();
        this.sourceAttr = ClaimUtils.fillStringPropertyOrNoVal(SOURCE_ATTR_NAME, ctx);
        if (!ClaimUtils.isPropSet(sourceAttr)) {
            throw new IllegalArgumentException(claimName + " - missing mandatory configuration option: "
                    + SOURCE_ATTR_NAME);
        }
        this.valueFormat = ClaimUtils.fillStringPropertyOrNoVal(VALUE_FORMAT, ctx);
        if (!ClaimUtils.isPropSet(valueFormat)) {
            this.valueFormat = DEFAULT_FORMAT;
        }
        log.debug("{} - sourceAttr: '{}', valueFormat: '{}'", claimName, sourceAttr, valueFormat);
    }

    @Override
    public JsonNode produceValue(ClaimSourceProduceContext pctx) {
        JsonNode result;
        if (ClaimUtils.isPropSetAndHasAttribute(sourceAttr, pctx)) {
            String lastSeen = pctx.getAttrValues().get(sourceAttr).valueAsString();
            result = JsonNodeFactory.instance.booleanNode(this.isCesnetEligible(lastSeen));
        } else {
            result = JsonNodeFactory.instance.booleanNode(false);
        }
        log.debug("{} - produced value for user({}): '{}'", claimName, pctx.getPerunUserId(), result);
        return result;
    }

    private boolean isCesnetEligible(String attrValue) {
        if (!StringUtils.hasText(attrValue)) {
            return false;
        }
        LocalDate timeStampLastSeen;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(valueFormat);
            timeStampLastSeen = LocalDate.parse(attrValue, formatter);
        } catch (DateTimeParseException e) {
            log.warn("{} - could not parse timestamp (for format: {}) value: '{}'", claimName, valueFormat, attrValue);
            return false;
        }

        LocalDate now = LocalDateTime.now().toLocalDate();
        if (timeStampLastSeen.isBefore(now.minusMonths(VALIDITY_PERIOD))) {
            log.trace("{} - timestamp '{}' is after the defined period of '{} months'",
                    claimName, timeStampLastSeen, VALIDITY_PERIOD);
            return false;
        } else {
            log.trace("{} - timestamp '{}' is within the defined period of '{} months'",
                    claimName, timeStampLastSeen, VALIDITY_PERIOD);
            return true;
        }
    }

}
