package com.ctrip.ops.sysdev.inputs;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import com.ctrip.ops.sysdev.outputs.BaseOutput;
import com.ctrip.ops.sysdev.decoder.IDecode;
import com.ctrip.ops.sysdev.decoder.JsonDecoder;
import com.ctrip.ops.sysdev.decoder.PlainDecoder;
import com.ctrip.ops.sysdev.filters.BaseFilter;

public abstract class BaseInput {
	private static final Logger logger = Logger.getLogger(BaseInput.class.getName());

	protected Map<String, Object> inputConfig;
	protected IDecode decoder;
	protected BaseFilter[] filterProcessors;
	protected BaseOutput[] outputProcessors;
	protected ArrayList<Map> filterList;
	protected ArrayList<Map> outputList;

	public BaseFilter[] createFilterProcessors() {
		if (filterList != null) {
			filterProcessors = new BaseFilter[filterList.size()];

			int idx = 0;
			for (Map filter : filterList) {
				Iterator<Entry<String, Map>> filterIT = filter.entrySet().iterator();

				while (filterIT.hasNext()) {
					Map.Entry<String, Map> filterEntry = filterIT.next();
					String filterType = filterEntry.getKey();
					Map filterConfig = filterEntry.getValue();

					try {
						logger.info("begin to build filter " + filterType);
						Class<?> filterClass = Class.forName("com.ctrip.ops.sysdev.filters." + filterType);
						Constructor<?> ctor = filterClass.getConstructor(Map.class);

						BaseFilter filterInstance = (BaseFilter) ctor.newInstance(filterConfig);
						filterProcessors[idx] = filterInstance;
						logger.info("build filter " + filterType + " done");
					} catch (Exception e) {
						logger.error(e);
						System.exit(1);
					}
					idx++;
				}
			}
		} else {
			filterProcessors = null;
		}
		return filterProcessors;
	}

	public BaseOutput[] createOutputProcessors() {
		outputProcessors = new BaseOutput[outputList.size()];
		int idx = 0;
		for (Map output : outputList) {
			Iterator<Entry<String, Map>> outputIT = output.entrySet().iterator();

			while (outputIT.hasNext()) {
				Map.Entry<String, Map> outputEntry = outputIT.next();
				String outputType = outputEntry.getKey();
				Map outputConfig = outputEntry.getValue();
				Class<?> outputClass;
				try {
					logger.info("begin to build output " + outputType);
					outputClass = Class.forName("com.ctrip.ops.sysdev.outputs." + outputType);
					Constructor<?> ctor = outputClass.getConstructor(Map.class);

					outputProcessors[idx] = (BaseOutput) ctor.newInstance(outputConfig);
					logger.info("build output " + outputType + " done");
					idx++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return outputProcessors;
	}

	public IDecode createDecoder() {
		String codec = (String) this.inputConfig.get("codec");
		if (codec != null && codec.equalsIgnoreCase("plain")) {
			return new PlainDecoder();
		} else {
			return new JsonDecoder();
		}
	}

	public BaseInput(Map inputConfig, ArrayList<Map> filterList, ArrayList<Map> outputList) throws Exception {
		this.inputConfig = inputConfig;
		this.filterList = filterList;
		this.outputList = outputList;
	}

	// 供子类实现业务逻辑
	protected abstract void prepare();

	// 供子类实现业务逻辑
	public abstract void emit();

	public void process(String message) {
		Map<String, Object> event = this.decoder.decode(message);

		if (this.filterProcessors != null) {
			for (BaseFilter bf : filterProcessors) {
				if (event == null) {
					break;
				}
				event = bf.process(event);
			}
		}
		if (event != null) {
			for (BaseOutput bo : outputProcessors) {
				bo.process(event);
			}
		}
	}
}
