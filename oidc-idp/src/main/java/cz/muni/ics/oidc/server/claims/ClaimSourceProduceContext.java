package cz.muni.ics.oidc.server.claims;

import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.server.adapters.PerunAdapter;
import org.mitre.oauth2.model.ClientDetailsEntity;

import java.util.Map;

/**
 * Context in which the value of the claim is produced.
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class ClaimSourceProduceContext {

	private final long perunUserId;
	private final String sub;
	private final Map<String, PerunAttributeValue> attrValues;
	private final PerunAdapter perunAdapter;
	private final ClientDetailsEntity client;

	public ClaimSourceProduceContext(long perunUserId, String sub, Map<String, PerunAttributeValue> attrValues,
									 PerunAdapter perunAdapter, ClientDetailsEntity client)
	{
		this.perunUserId = perunUserId;
		this.sub = sub;
		this.attrValues = attrValues;
		this.perunAdapter = perunAdapter;
		this.client = client;
	}

	public Map<String, PerunAttributeValue> getAttrValues() {
		return attrValues;
	}

	public long getPerunUserId() {
		return perunUserId;
	}

	public String getSub() {
		return sub;
	}

	public PerunAdapter getPerunAdapter() {
		return perunAdapter;
	}

	public ClientDetailsEntity getClient() {
		return client;
	}

	@Override
	public String toString() {
		return "ClaimSourceProduceContext{" +
				"perunUserId=" + perunUserId +
				", sub='" + sub + '\'' +
				'}';
	}
}
