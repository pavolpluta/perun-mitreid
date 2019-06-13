package cz.muni.ics.oidc.server.claims;

import cz.muni.ics.oidc.models.RichUser;
import cz.muni.ics.oidc.server.connectors.PerunConnector;
import org.mitre.oauth2.model.ClientDetailsEntity;

public class ClaimSourceProduceContext {

	private final long perunUserId;
	private final String sub;
	private final RichUser richUser;
	private final PerunConnector perunConnector;
	private final ClientDetailsEntity client;

	public ClaimSourceProduceContext(long perunUserId, String sub, RichUser richUser, PerunConnector perunConnector, ClientDetailsEntity client) {
		this.perunUserId = perunUserId;
		this.sub = sub;
		this.richUser = richUser;
		this.perunConnector = perunConnector;
		this.client = client;
	}

	public RichUser getRichUser() {
		return richUser;
	}

	public long getPerunUserId() {
		return perunUserId;
	}

	public String getSub() {
		return sub;
	}

	public PerunConnector getPerunConnector() {
		return perunConnector;
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
