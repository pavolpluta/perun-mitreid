package cz.muni.ics.oidc.models;

/**
 * Basic model object, which is extended by another specific models with specific variables.
 *
 * Created as replacement of JsonNode for PerunConnector methods return types.
 */
public abstract class Model {

	private Long id;

	public Model() {
	}

	public Model(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
