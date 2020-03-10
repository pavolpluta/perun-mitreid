package cz.muni.ics.oidc.server.elixir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cz.muni.ics.oidc.server.elixir.GA4GHClaimSource.parseAndVerifyVisa;

/**
 * This class is a command-line debugging tool. It parses JSON in GA4GH Passport format,
 * verifies signatures on Passport Visas (JWT tokens), and prints them in human-readable format.
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class GA4GHTokenParser {

	static ObjectMapper jsonMapper = new ObjectMapper();

	public static void main(String[] args) throws IOException, ParseException, JOSEException {
		String userinfo = "/tmp/ga4gh.json";
		JsonNode doc = jsonMapper.readValue(new File(userinfo), JsonNode.class);
		JsonNode ga4gh = doc.get("ga4gh_passport_v1");
		long startx = System.currentTimeMillis();
		System.out.println();
		for (JsonNode jwtString : ga4gh) {
			String s = jwtString.asText();
			GA4GHClaimSource.PassportVisa visa = parseAndVerifyVisa(s);
			if(!visa.isVerified()) {
				System.out.println("visa not verified: "+s);
				System.out.println("visa = " + visa.getPrettyString());

//				System.exit(1);
			} else {
				System.out.println("OK: "+visa.getPrettyString());
			}
			JsonNode visadoc = jsonMapper.readValue(((SignedJWT) JWTParser.parse(visa.getJwt())).getPayload().toString(), JsonNode.class);
			System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(visadoc));
		}
		long endx = System.currentTimeMillis();
		System.out.println("signature verification time: " + (endx - startx));

	}

	private static String isoDateTime(long linuxTime) {
		return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.ofEpochSecond(linuxTime), ZoneId.systemDefault()));
	}
}
