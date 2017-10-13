package cz.muni.ics.oidc;

import com.google.gson.JsonObject;
import org.mitre.openid.connect.model.DefaultUserInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implements UserInfo by inheriting from DefaultUserInfo and adding more claims.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunUserInfo extends DefaultUserInfo {

	private Map<String,String> customClaims = new LinkedHashMap<>();

	public Map<String, String> getCustomClaims() {
		return customClaims;
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		for(Map.Entry<String,String> entry : customClaims.entrySet()) {
			obj.addProperty(entry.getKey(),entry.getValue());
		}
		return obj;
	}

}
