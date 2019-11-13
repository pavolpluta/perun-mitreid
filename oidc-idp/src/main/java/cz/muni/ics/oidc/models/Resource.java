package cz.muni.ics.oidc.models;

import java.util.Objects;

/**
 * Resource object model.
 *
 * @author Peter Jancus jancus@ics.muni.cz
 */
public class Resource extends Model {

	private Long voId;
	private String name;
	private String description;
	private Vo vo;

    public Resource(Long id, Long voId, String name, String description) {
        super(id);
        this.voId = voId;
        this.name = name;
        this.description = description;
    }

    /**
     * Should be used when RichResource is obtained from Perun
     */
    public Resource(Long id, Long voId, String name, String description, Vo vo) {
		this(id, voId, name, description);
		this.vo = vo;
	}

	public Long getVoId() {
		return voId;
	}

	public void setVoId(Long voId) {
		this.voId = voId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Vo getVo() {
		return vo;
	}

	public void setVo(Vo vo) {
		this.vo = vo;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		Resource resource = (Resource) o;
		return Objects.equals(voId, resource.voId) &&
				Objects.equals(name, resource.name) &&
				Objects.equals(description, resource.description) &&
				Objects.equals(vo, resource.vo);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), voId, name, description, vo);
	}

	@Override
	public String toString() {
		return "Resource{" +
				"id=" + getId() +
				", voId=" + voId +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				", vo='" + vo + '\'' +
				'}';
	}
}
