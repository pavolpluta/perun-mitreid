package cz.muni.ics.oidc.server.connectors;

public class Affiliation {

	private String source;
	private String value;
	private long asserted;

	public Affiliation(String source, String value, long asserted) {
		this.source = source;
		this.value = value;
		this.asserted = asserted;
	}

	public String getSource() {
		return source;
	}

	public String getValue() {
		return value;
	}

	public long getAsserted() {
		return asserted;
	}

	@Override
	public String toString() {
		return "Affiliation{" +
				"source='" + source + '\'' +
				", value='" + value + '\'' +
				", asserted=" + asserted +
				'}';
	}
}
