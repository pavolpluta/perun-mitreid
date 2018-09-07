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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Model)) return false;

		Model model = (Model) o;

		return getId() != null ? getId().equals(model.getId()) : model.getId() == null;
	}

	@Override
	public int hashCode() {
		return getId() != null ? getId().hashCode() : 0;
	}
}
