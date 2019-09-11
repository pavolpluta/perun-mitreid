package cz.muni.ics.oidc.server.elixir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
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

public class GA4GHTokenParser {

	static ObjectMapper jsonMapper = new ObjectMapper();

	public static void main(String[] args) throws IOException, ParseException, JOSEException {
		String userinfo = "/tmp/ga4gh_userinfo.json";
		JsonNode doc = jsonMapper.readValue(new File(userinfo), JsonNode.class);
		JsonNode ga4gh = doc.get("ga4gh_passport_v1");
		long startx = System.currentTimeMillis();
		System.out.println();
		for (JsonNode jwtString : ga4gh) {
			String s = jwtString.asText();
			if(!verifyJWT(s)) {
				System.out.println("signature broken in "+s);
				System.exit(1);
			}
		}
		long endx = System.currentTimeMillis();
		System.out.println("signature verification time: " + (endx - startx));

	}

	private static Map<URL, RemoteJWKSet<SecurityContext>> remoteJwkSets = new HashMap<>();

	private static boolean verifyJWT(String jwtString) throws ParseException, MalformedURLException, JOSEException {
//		System.out.println();
//		System.out.println("verifying a JWT ...");

		JWT parsedJWT = JWTParser.parse(jwtString);
		if (!(parsedJWT instanceof SignedJWT)) {
			throw new RuntimeException("JWT is not SignedJWT");
		}
		SignedJWT signedJWT = (SignedJWT) parsedJWT;
		JWSHeader header = signedJWT.getHeader();
		String keyID = header.getKeyID();
		JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
		String payload = signedJWT.getPayload().toString();
		prettyPrintPayload(payload);
		URL jwks;
		URI jwkURL = header.getJWKURL();
		if(jwkURL!=null) {
			jwks = jwkURL.toURL();
		} else {
			String iss = jwtClaimsSet.getIssuer();
			RestTemplate restTemplate = new RestTemplate();
			JsonNode metadata = restTemplate.getForObject(iss + ".well-known/openid-configuration", JsonNode.class);
			jwks = new URL(metadata.path("jwks_uri").asText());
		}
		RemoteJWKSet<SecurityContext> remoteJWKSet = remoteJwkSets.computeIfAbsent(jwks, s -> new RemoteJWKSet<>(jwks));
		List<JWK> keys = remoteJWKSet.get(new JWKSelector(new JWKMatcher.Builder().keyID(keyID).build()), null);
		RSAPublicKey key = ((RSAKey) keys.get(0)).toRSAPublicKey();
		RSASSAVerifier verifier = new RSASSAVerifier(key);
		return signedJWT.verify(verifier);
	}

	private static String isoDateTime(long linuxTime) {
		return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.ofEpochSecond(linuxTime), ZoneId.systemDefault()));
	}
	private static void prettyPrintPayload(String payload) {
		try {
			JsonNode doc = jsonMapper.readValue(payload, JsonNode.class);
//			System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc));
			long iat = doc.get("iat").asLong();
			long exp = doc.get("exp").asLong();
			JsonNode visa = doc.get("ga4gh_visa_v1");
			long asserted = visa.get("asserted").asLong();
            String type = visa.get("type").asText();
			String value = visa.get("value").asText();
			String source = visa.get("source").asText();
			String by = visa.get("by").asText();
			System.out.println("type: "+type+", value: "+value+", by: "+by+", source: "+source+", asserted: "+isoDateTime(asserted));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
