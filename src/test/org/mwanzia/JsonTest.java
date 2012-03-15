package org.mwanzia;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonBackReference;
import org.codehaus.jackson.annotate.JsonManagedReference;
import org.codehaus.jackson.annotate.JsonProperty;
import org.junit.Assert;
import org.junit.Test;

public class JsonTest {
	@Test
	public void testSerialize() throws Exception {
		Container container = new Container("Test Container");
		for (int i = 0; i < 10; i++) {
			Child child = new Child(container, "Some string " + i,
					new BigDecimal(i * 5.78), i % 2 == 0, new Timestamp(System
							.currentTimeMillis()));
			if (container.child == null)
				container.child = child;
			container.simpleList.add(child);
			container.simpleSet.add(child);
			container.map.put("child" + i, child);
		}
		Call call = new Call(container, null, null, null);
		String json = JSON.serialize(call, true);
		System.out.println(json);
		Call deserializedCall = JSON.deserialize(json, Call.class);
		Container deserialized = (Container) call.getTarget();
		Assert.assertEquals(container.getSimpleSet().size(), deserialized
				.getSimpleSet().size());
		Assert.assertEquals(container.getSimpleList().size(), deserialized
				.getSimpleList().size());
		for (int i = 0; i < container.getSimpleList().size(); i++) {
			assertChildEquals(container.getSimpleList().get(i), container
					.getSimpleList().get(i));
		}
		assertChildEquals(container.getChild(), deserialized.getChild());
	}

	private void assertChildEquals(Child originalChild, Child deserializedChild)
			throws Exception {
		Assert.assertEquals(originalChild.getString(), deserializedChild
				.getString());
		Assert.assertEquals(originalChild.getBigDecimal(), deserializedChild
				.getBigDecimal());
		Assert.assertEquals(originalChild.isBool(), deserializedChild.isBool());
		Assert.assertEquals(originalChild.getDate().getTime(),
				deserializedChild.getDate().getTime());
	}

	public static class Container {
		private String name;
		private Map<String, Object> map = new HashMap<String, Object>();
		private List<Child> simpleList = new ArrayList<Child>();
		private Set<Child> simpleSet = new HashSet<Child>();
		private Child child;

		public Container() {
		}

		public Container(String name) {
			super();
			this.name = name;
		}

		@JsonProperty
	    public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@JsonProperty
	    @JsonManagedReference
		public Map<String, Object> getMap() {
			return map;
		}

		public void setMap(Map<String, Object> map) {
			this.map = map;
		}

		@JsonProperty
	    @JsonManagedReference
		public List<Child> getSimpleList() {
			return simpleList;
		}

		public void setSimpleList(List<Child> simpleList) {
			this.simpleList = simpleList;
		}

		@JsonProperty
	    @JsonManagedReference
		public Set<Child> getSimpleSet() {
			return simpleSet;
		}

		public void setSimpleSet(Set<Child> simpleSet) {
			this.simpleSet = simpleSet;
		}

		@JsonProperty
	    @JsonManagedReference
		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
		}

	}

	public static class Child {
		private Container container;
		private String string;
		private BigDecimal bigDecimal;
		private boolean bool;
		private Date date;

		public Child() {
		}

		public Child(Container container, String string, BigDecimal bigDecimal,
				boolean bool, Date date) {
			super();
			this.container = container;
			this.string = string;
			this.bigDecimal = bigDecimal;
			this.bool = bool;
			this.date = date;
		}

		@JsonProperty
	    @JsonBackReference
		public Container getContainer() {
			return container;
		}

		public void setContainer(Container container) {
			this.container = container;
		}

		@JsonProperty
	    public String getString() {
			return string;
		}

		public void setString(String string) {
			this.string = string;
		}

		@JsonProperty
	    public BigDecimal getBigDecimal() {
			return bigDecimal;
		}

		public void setBigDecimal(BigDecimal bigDecimal) {
			this.bigDecimal = bigDecimal;
		}

		@JsonProperty
	    public boolean isBool() {
			return bool;
		}

		public void setBool(boolean bool) {
			this.bool = bool;
		}

		@JsonProperty
	    public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

	}

}
