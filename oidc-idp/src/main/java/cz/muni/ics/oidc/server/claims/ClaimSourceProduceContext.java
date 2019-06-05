package cz.muni.ics.oidc.server.claims;

import cz.muni.ics.oidc.models.RichUser;
import cz.muni.ics.oidc.server.connectors.PerunConnector;

public class ClaimSourceProduceContext {

	private final long perunUserId;
	private final String sub;
	private final RichUser richUser;
	private final PerunConnector perunConnector;

	public ClaimSourceProduceContext(long perunUserId, String sub, RichUser richUser, PerunConnector perunConnector) {
		this.perunUserId = perunUserId;
		this.sub = sub;
		this.richUser = richUser;
		this.perunConnector = perunConnector;
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

	@Override
	public String toString() {
		return "ClaimSourceProduceContext{" +
				"perunUserId=" + perunUserId +
				", sub='" + sub + '\'' +
				//", richUser=" + richUser +
				'}';
	}
}
