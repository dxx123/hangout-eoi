package com.ctrip.ops.sysdev.filters;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONArray;

public class Split extends BaseFilter {
	private Map<String, String> fields;

	public Split(Map config) {
		super(config);
	}

	protected void prepare() {
		this.fields = (Map<String, String>) config.get("fields");
	}

	@Override
	protected Map filter(final Map event) {
		Iterator<Entry<String, String>> it = fields.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> entry = it.next();

			String field = entry.getKey();
			String regex = entry.getValue();

			if (event.containsKey(field)) {
				String message = event.get(field).toString();
				String[] tempArray = message.split(regex);
				event.put(field + "_split", JSONArray.toJSONString(tempArray));
			}
		}
		return event;
	}
}
